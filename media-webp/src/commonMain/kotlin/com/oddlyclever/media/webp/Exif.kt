package com.oddlyclever.media.webp

import kotlin.math.abs
import kotlin.math.roundToLong
import kotlin.time.Instant

/**
 * [WebpMetadata] as an EXIF block: a little-endian TIFF structure with the primary image's tags, an Exif sub-IFD for
 * the capture time and a GPS sub-IFD for the location. This is the payload of a WebP's `EXIF` chunk, which starts
 * with the TIFF header and has no `Exif\0\0` prefix.
 *
 * Only the tags [WebpMetadata] has are written, so a reader sees nothing it would have to second-guess.
 */
internal object Exif {

    fun write(metadata: WebpMetadata): ByteArray {
        val captured = metadata.capturedAt?.let(::utc)
        val exifIfd = captured?.let {
            listOf(
                Entry.undefined(EXIF_VERSION, "0232".encodeToByteArray()),
                Entry.ascii(DATE_TIME_ORIGINAL, it.dateTime),
                Entry.ascii(OFFSET_TIME_ORIGINAL, "+00:00"),
                Entry.ascii(SUB_SEC_TIME_ORIGINAL, it.millis.toString().padStart(3, '0')),
            )
        }
        val gpsIfd = metadata.location?.let { location ->
            buildList {
                add(Entry.bytes(GPS_VERSION_ID, byteArrayOf(2, 3, 0, 0)))
                add(Entry.ascii(GPS_LATITUDE_REF, if (location.latitude < 0) "S" else "N"))
                add(Entry.rationals(GPS_LATITUDE, degreesMinutesSeconds(location.latitude)))
                add(Entry.ascii(GPS_LONGITUDE_REF, if (location.longitude < 0) "W" else "E"))
                add(Entry.rationals(GPS_LONGITUDE, degreesMinutesSeconds(location.longitude)))
                location.altitude?.let { altitude ->
                    add(Entry.bytes(GPS_ALTITUDE_REF, byteArrayOf((if (altitude < 0) 1 else 0).toByte())))
                    add(Entry.rationals(GPS_ALTITUDE, listOf((abs(altitude) * 1000).roundToLong() to 1000L)))
                }
                captured?.let {
                    add(Entry.rationals(GPS_TIME_STAMP, listOf(it.hour to 1L, it.minute to 1L, it.secondMillis to 1000L)))
                    add(Entry.ascii(GPS_DATE_STAMP, it.date))
                }
            }
        }

        // Each IFD's size does not depend on the offsets its pointers hold, so the layout is known before writing.
        val pointerPlaceholders = buildList {
            if (exifIfd != null) add(Entry.long(EXIF_IFD_POINTER, 0))
            if (gpsIfd != null) add(Entry.long(GPS_IFD_POINTER, 0))
        }
        val primaryWithout = buildList {
            metadata.software?.takeIf { it.isNotEmpty() }?.let { add(Entry.ascii(SOFTWARE, it)) }
        }
        val primarySize = size(primaryWithout + pointerPlaceholders)
        val exifOffset = TIFF_HEADER + primarySize
        val gpsOffset = exifOffset + (exifIfd?.let(::size) ?: 0)
        val primary = primaryWithout + buildList {
            if (exifIfd != null) add(Entry.long(EXIF_IFD_POINTER, exifOffset))
            if (gpsIfd != null) add(Entry.long(GPS_IFD_POINTER, gpsOffset))
        }

        val out = Bytes()
        out.ascii("II"); out.u16(42); out.u32(TIFF_HEADER)
        writeIfd(out, primary)
        exifIfd?.let { writeIfd(out, it) }
        gpsIfd?.let { writeIfd(out, it) }
        return out.toByteArray()
    }

    private class Entry(val tag: Int, val type: Int, val count: Int, val value: ByteArray) {
        companion object {
            fun bytes(tag: Int, value: ByteArray) = Entry(tag, BYTE, value.size, value)
            fun undefined(tag: Int, value: ByteArray) = Entry(tag, UNDEFINED, value.size, value)
            fun ascii(tag: Int, value: String): Entry {
                val bytes = value.encodeToByteArray() + 0.toByte()
                return Entry(tag, ASCII, bytes.size, bytes)
            }
            fun long(tag: Int, value: Int) = Entry(tag, LONG, 1, Bytes().apply { u32(value) }.toByteArray())
            fun rationals(tag: Int, values: List<Pair<Long, Long>>) = Entry(
                tag, RATIONAL, values.size,
                Bytes().apply { values.forEach { (n, d) -> u32(n.toInt()); u32(d.toInt()) } }.toByteArray(),
            )
        }
    }

    /** The bytes an IFD takes: entry count, entries, next-IFD offset, then each value too long to sit in its entry. */
    private fun size(entries: List<Entry>): Int =
        2 + 12 * entries.size + 4 + entries.sumOf { if (it.value.size > 4) it.value.size + (it.value.size and 1) else 0 }

    private fun writeIfd(out: Bytes, entries: List<Entry>) {
        val sorted = entries.sortedBy { it.tag }
        var data = out.size + 2 + 12 * sorted.size + 4
        out.u16(sorted.size)
        for (entry in sorted) {
            out.u16(entry.tag); out.u16(entry.type); out.u32(entry.count)
            if (entry.value.size <= 4) {
                out.bytes(entry.value); repeat(4 - entry.value.size) { out.u8(0) }
            } else {
                out.u32(data)
                data += entry.value.size + (entry.value.size and 1)
            }
        }
        out.u32(0)
        for (entry in sorted) if (entry.value.size > 4) {
            out.bytes(entry.value)
            if (entry.value.size and 1 == 1) out.u8(0)
        }
    }

    /** Degrees, minutes and seconds to a ten-thousandth of a second, about 3 mm; rounded once, so never 60 seconds. */
    private fun degreesMinutesSeconds(degrees: Double): List<Pair<Long, Long>> {
        val tenThousandths = (abs(degrees) * 3600 * 10_000).roundToLong()
        val whole = tenThousandths / (3600 * 10_000)
        val minutes = tenThousandths / (60 * 10_000) % 60
        return listOf(whole to 1L, minutes to 1L, tenThousandths % (60 * 10_000) to 10_000L)
    }

    private class Utc(val date: String, val dateTime: String, val hour: Long, val minute: Long, val secondMillis: Long, val millis: Int)

    /** [instant] as a UTC calendar date and time, without a date library. */
    private fun utc(instant: Instant): Utc {
        val millis = instant.toEpochMilliseconds()
        val days = millis.floorDiv(86_400_000L)
        val ofDay = millis.mod(86_400_000L)
        // Days since 1970-01-01 to a proleptic Gregorian date (Howard Hinnant's civil_from_days).
        val z = days + 719_468
        val era = z.floorDiv(146_097)
        val doe = z - era * 146_097
        val yoe = (doe - doe / 1460 + doe / 36_524 - doe / 146_096) / 365
        val doy = doe - (365 * yoe + yoe / 4 - yoe / 100)
        val mp = (5 * doy + 2) / 153
        val day = doy - (153 * mp + 2) / 5 + 1
        val month = if (mp < 10) mp + 3 else mp - 9
        val year = yoe + era * 400 + if (month <= 2) 1 else 0
        val hour = ofDay / 3_600_000
        val minute = ofDay / 60_000 % 60
        val second = ofDay / 1000 % 60
        fun Long.two() = toString().padStart(2, '0')
        val date = "${year.toString().padStart(4, '0')}:${month.two()}:${day.two()}"
        return Utc(date, "$date ${hour.two()}:${minute.two()}:${second.two()}", hour, minute, ofDay % 60_000, (ofDay % 1000).toInt())
    }

    private class Bytes {
        private var buffer = ByteArray(256)
        var size = 0
            private set

        fun u8(v: Int) {
            if (size == buffer.size) buffer = buffer.copyOf(buffer.size * 2)
            buffer[size++] = v.toByte()
        }
        fun u16(v: Int) { u8(v); u8(v ushr 8) }
        fun u32(v: Int) { u16(v); u16(v ushr 16) }
        fun bytes(v: ByteArray) = v.forEach { u8(it.toInt()) }
        fun ascii(v: String) = bytes(v.encodeToByteArray())
        fun toByteArray() = buffer.copyOf(size)
    }

    private const val TIFF_HEADER = 8

    private const val BYTE = 1
    private const val ASCII = 2
    private const val LONG = 4
    private const val RATIONAL = 5
    private const val UNDEFINED = 7

    private const val SOFTWARE = 0x0131
    private const val EXIF_IFD_POINTER = 0x8769
    private const val GPS_IFD_POINTER = 0x8825

    private const val EXIF_VERSION = 0x9000
    private const val DATE_TIME_ORIGINAL = 0x9003
    private const val OFFSET_TIME_ORIGINAL = 0x9011
    private const val SUB_SEC_TIME_ORIGINAL = 0x9291

    private const val GPS_VERSION_ID = 0x0000
    private const val GPS_LATITUDE_REF = 0x0001
    private const val GPS_LATITUDE = 0x0002
    private const val GPS_LONGITUDE_REF = 0x0003
    private const val GPS_LONGITUDE = 0x0004
    private const val GPS_ALTITUDE_REF = 0x0005
    private const val GPS_ALTITUDE = 0x0006
    private const val GPS_TIME_STAMP = 0x0007
    private const val GPS_DATE_STAMP = 0x001D
}
