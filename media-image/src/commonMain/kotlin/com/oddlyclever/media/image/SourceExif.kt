package com.oddlyclever.media.image

import com.oddlyclever.media.webp.GpsLocation
import com.oddlyclever.media.webp.WebpMetadata
import kotlin.time.Instant

/**
 * What a source image's EXIF says that [ImageProcessor] acts on: how the stored pixels must be turned to stand upright,
 * and where and when the photo was taken.
 *
 * @property orientation the EXIF orientation, 1 to 8; 1 is already upright.
 * @property location where the photo was taken, when the source records it.
 * @property capturedAt when it was taken, when the source records the moment exactly: an original date and time with
 *   its offset from UTC, or a GPS date and time stamp. A local date and time with no offset names no moment, so it is
 *   left out rather than guessed.
 */
internal class SourceExif(
    val orientation: Int = 1,
    val location: GpsLocation? = null,
    val capturedAt: Instant? = null,
) {
    /** Whether the stored image is turned a quarter, so its width and height swap when it is stood upright. */
    val swapsSides: Boolean get() = orientation in 5..8

    /** Where and when, as the metadata to write into the result; null when the source records neither. */
    val metadata: WebpMetadata?
        get() = if (location == null && capturedAt == null) null else WebpMetadata(location = location, capturedAt = capturedAt)
}

/**
 * Reads the EXIF of a JPEG (its APP1 segment), a WebP (its `EXIF` chunk) or a PNG (its `eXIf` chunk), in either byte
 * order. Used where the platform offers no EXIF reader of its own; Android and iOS read with theirs, which also
 * understand HEIC.
 *
 * It never throws: a source with no EXIF, or EXIF it cannot make sense of, reads as upright with nothing recorded.
 */
internal object SourceExifReader {

    fun read(bytes: ByteArray): SourceExif = runCatching { tiffOf(bytes)?.let(::readTiff) }.getOrNull() ?: SourceExif()

    private fun tiffOf(b: ByteArray): ByteArray? = when {
        b.size > 4 && b[0] == 0xFF.toByte() && b[1] == 0xD8.toByte() -> jpegExif(b)
        b.size > 12 && b.decodeToString(0, 4) == "RIFF" && b.decodeToString(8, 12) == "WEBP" -> webpExif(b)
        b.size > 8 && b[0] == 0x89.toByte() && b.decodeToString(1, 4) == "PNG" -> pngExif(b)
        else -> null
    }

    private fun jpegExif(b: ByteArray): ByteArray? {
        var at = 2
        while (at + 4 <= b.size && b[at] == 0xFF.toByte()) {
            val marker = b[at + 1].toInt() and 0xff
            if (marker == 0xDA || marker == 0xD9) return null // the image data starts; no EXIF before it
            val length = beU16(b, at + 2)
            if (marker == 0xE1 && length >= 8 && at + 2 + length <= b.size && b.decodeToString(at + 4, at + 10) == "Exif\u0000\u0000") {
                return b.copyOfRange(at + 10, at + 2 + length)
            }
            at += 2 + length
        }
        return null
    }

    private fun webpExif(b: ByteArray): ByteArray? {
        var at = 12
        while (at + 8 <= b.size) {
            val length = leU32(b, at + 4)
            if (length < 0 || at + 8 + length > b.size) return null
            if (b.decodeToString(at, at + 4) == "EXIF") {
                val payload = b.copyOfRange(at + 8, at + 8 + length)
                // Some writers put JPEG's "Exif\0\0" in front of the TIFF header; the specification has none.
                return if (payload.size > 6 && payload.decodeToString(0, 6) == "Exif\u0000\u0000") payload.copyOfRange(6, payload.size) else payload
            }
            at += 8 + length + (length and 1)
        }
        return null
    }

    private fun pngExif(b: ByteArray): ByteArray? {
        var at = 8
        while (at + 12 <= b.size) {
            val length = beU32(b, at)
            if (length < 0 || at + 12 + length > b.size) return null
            when (b.decodeToString(at + 4, at + 8)) {
                "eXIf" -> return b.copyOfRange(at + 8, at + 8 + length)
                "IDAT", "IEND" -> return null
            }
            at += 12 + length
        }
        return null
    }

    private class Tiff(val b: ByteArray) {
        val little = b.decodeToString(0, 2) == "II"
        fun u16(at: Int) = if (little) (b[at].toInt() and 0xff) or ((b[at + 1].toInt() and 0xff) shl 8) else beU16(b, at)
        fun u32(at: Int) = if (little) leU32(b, at) else beU32(b, at)
    }

    /** One IFD's entries: tag to the bytes of its value, wherever they are stored. */
    private fun Tiff.ifd(at: Int): Map<Int, Pair<Int, ByteArray>> {
        if (at <= 0 || at + 2 > b.size) return emptyMap()
        val count = u16(at)
        val entries = mutableMapOf<Int, Pair<Int, ByteArray>>()
        for (i in 0 until count) {
            val entry = at + 2 + 12 * i
            if (entry + 12 > b.size) break
            val type = u16(entry + 2)
            val n = u32(entry + 4)
            val size = n.toLong() * when (type) { 3 -> 2; 4 -> 4; 5, 10 -> 8; else -> 1 }
            if (n < 0 || size > b.size) continue
            val from = if (size <= 4) entry + 8 else u32(entry + 8)
            if (from < 0 || from + size > b.size) continue
            entries[u16(entry)] = type to b.copyOfRange(from, from + size.toInt())
        }
        return entries
    }

    private fun readTiff(bytes: ByteArray): SourceExif? {
        if (bytes.size < 8) return null
        val t = Tiff(bytes)
        if (!t.little && bytes.decodeToString(0, 2) != "MM") return null
        if (t.u16(2) != 42) return null
        val primary = t.ifd(t.u32(4))
        val exif = primary[EXIF_POINTER]?.let { t.ifd(t.valueU32(it.second)) } ?: emptyMap()
        val gps = primary[GPS_POINTER]?.let { t.ifd(t.valueU32(it.second)) } ?: emptyMap()

        val orientation = primary[ORIENTATION]?.let { t.valueU16(it.second) }?.takeIf { it in 1..8 } ?: 1

        val location = run {
            val lat = gps[GPS_LATITUDE]?.let { t.degrees(it.second) } ?: return@run null
            val lon = gps[GPS_LONGITUDE]?.let { t.degrees(it.second) } ?: return@run null
            val south = gps[GPS_LATITUDE_REF]?.second?.ascii() == "S"
            val west = gps[GPS_LONGITUDE_REF]?.second?.ascii() == "W"
            val altitude = gps[GPS_ALTITUDE]?.let { t.rationals(it.second).firstOrNull() }?.let { metres ->
                if (gps[GPS_ALTITUDE_REF]?.second?.firstOrNull()?.toInt() == 1) -metres else metres
            }
            val latitude = if (south) -lat else lat
            val longitude = if (west) -lon else lon
            if (latitude !in -90.0..90.0 || longitude !in -180.0..180.0) null
            else GpsLocation(latitude, longitude, altitude?.takeIf { it.isFinite() })
        }

        val capturedAt = exactCaptureTime(
            original = exif[DATE_TIME_ORIGINAL]?.second?.ascii(),
            offset = exif[OFFSET_TIME_ORIGINAL]?.second?.ascii(),
            gpsDate = gps[GPS_DATE_STAMP]?.second?.ascii(),
            gpsSeconds = gps[GPS_TIME_STAMP]?.let { t.rationals(it.second) }?.takeIf { it.size == 3 }
                ?.let { (h, m, sec) -> h * 3600 + m * 60 + sec },
        )

        return SourceExif(orientation, location, capturedAt)
    }

    private fun Tiff.valueU16(v: ByteArray) =
        if (v.size < 2) null else if (little) (v[0].toInt() and 0xff) or ((v[1].toInt() and 0xff) shl 8) else beU16(v, 0)
    private fun Tiff.valueU32(v: ByteArray) = if (v.size >= 4) (if (little) leU32(v, 0) else beU32(v, 0)) else -1
    private fun Tiff.rationals(v: ByteArray): List<Double> = (0 until v.size / 8).mapNotNull { i ->
        val n = (if (little) leU32(v, 8 * i) else beU32(v, 8 * i)).toLong() and 0xffffffffL
        val d = (if (little) leU32(v, 8 * i + 4) else beU32(v, 8 * i + 4)).toLong() and 0xffffffffL
        if (d == 0L) null else n.toDouble() / d
    }
    private fun Tiff.degrees(v: ByteArray): Double? = rationals(v).takeIf { it.size == 3 }?.let { (d, m, s) -> d + m / 60 + s / 3600 }

    private fun ByteArray.ascii() = decodeToString().trimEnd('\u0000').trim()

    private fun beU16(b: ByteArray, at: Int) = ((b[at].toInt() and 0xff) shl 8) or (b[at + 1].toInt() and 0xff)
    private fun beU32(b: ByteArray, at: Int) = (beU16(b, at) shl 16) or beU16(b, at + 2)
    private fun leU32(b: ByteArray, at: Int) = (b[at].toInt() and 0xff) or ((b[at + 1].toInt() and 0xff) shl 8) or
        ((b[at + 2].toInt() and 0xff) shl 16) or ((b[at + 3].toInt() and 0xff) shl 24)

    private const val ORIENTATION = 0x0112
    private const val EXIF_POINTER = 0x8769
    private const val GPS_POINTER = 0x8825
    private const val DATE_TIME_ORIGINAL = 0x9003
    private const val OFFSET_TIME_ORIGINAL = 0x9011
    private const val GPS_LATITUDE_REF = 0x0001
    private const val GPS_LATITUDE = 0x0002
    private const val GPS_LONGITUDE_REF = 0x0003
    private const val GPS_LONGITUDE = 0x0004
    private const val GPS_ALTITUDE_REF = 0x0005
    private const val GPS_ALTITUDE = 0x0006
    private const val GPS_TIME_STAMP = 0x0007
    private const val GPS_DATE_STAMP = 0x001D
}

/**
 * The affine transform, as `[scaleX, skewX, transX, skewY, scaleY, transY]`, that stands an image of EXIF [orientation]
 * upright: it maps a point of the stored image, already scaled to fit, onto an upright image [width] by [height]. One
 * table for every platform that draws the turn itself, so they cannot disagree on which way a sideways photo goes.
 */
internal fun uprightTransform(orientation: Int, width: Float, height: Float): FloatArray = when (orientation) {
    2 -> floatArrayOf(-1f, 0f, width, 0f, 1f, 0f) // mirrored left to right
    3 -> floatArrayOf(-1f, 0f, width, 0f, -1f, height) // upside down
    4 -> floatArrayOf(1f, 0f, 0f, 0f, -1f, height) // mirrored top to bottom
    5 -> floatArrayOf(0f, 1f, 0f, 1f, 0f, 0f) // mirrored across the diagonal
    6 -> floatArrayOf(0f, -1f, width, 1f, 0f, 0f) // turned a quarter: stand it up by turning clockwise
    7 -> floatArrayOf(0f, -1f, width, -1f, 0f, height) // mirrored across the other diagonal
    8 -> floatArrayOf(0f, 1f, 0f, -1f, 0f, height) // turned a quarter the other way
    else -> floatArrayOf(1f, 0f, 0f, 0f, 1f, 0f)
}

/**
 * When a photo was taken, from its EXIF as text, only where the moment is exact: the original date and time with its
 * offset from UTC, else the GPS date stamp with the GPS time of day in seconds, which is UTC by definition. A local time
 * with no offset names no moment, so it gives null rather than a guess.
 */
internal fun exactCaptureTime(original: String?, offset: String?, gpsDate: String?, gpsSeconds: Double?): Instant? {
    if (original != null && offset != null) {
        val local = localSeconds(original)
        val east = offsetSeconds(offset)
        if (local != null && east != null) return Instant.fromEpochSeconds(local - east)
    }
    if (gpsDate != null && gpsSeconds != null && gpsSeconds.isFinite()) {
        val midnight = localSeconds("$gpsDate 00:00:00") ?: return null
        return Instant.fromEpochMilliseconds(midnight * 1000 + (gpsSeconds * 1000).toLong())
    }
    return null
}

/** "YYYY:MM:DD HH:MM:SS" as seconds since 1970 in that wall-clock time, without a date library. */
private fun localSeconds(text: String): Long? {
    val parts = text.split(':', ' ').mapNotNull { it.toIntOrNull() }
    if (parts.size < 6) return null
    val (year, month, day) = parts
    val (hour, minute, second) = parts.drop(3)
    if (month !in 1..12 || day !in 1..31 || hour !in 0..23 || minute !in 0..59 || second !in 0..60) return null
    // Howard Hinnant's days_from_civil.
    val y = if (month <= 2) year - 1 else year
    val era = (if (y >= 0) y else y - 399) / 400
    val yoe = y - era * 400
    val doy = (153 * (if (month > 2) month - 3 else month + 9) + 2) / 5 + day - 1
    val doe = yoe * 365 + yoe / 4 - yoe / 100 + doy
    val days = era * 146097L + doe - 719468
    return days * 86400 + hour * 3600 + minute * 60 + second
}

/** "+HH:MM" or "-HH:MM" as seconds east of UTC. */
private fun offsetSeconds(text: String): Long? {
    val match = Regex("^([+-])(\\d{2}):(\\d{2})$").find(text) ?: return null
    val (sign, hours, minutes) = match.destructured
    val seconds = hours.toLong() * 3600 + minutes.toLong() * 60
    return if (sign == "-") -seconds else seconds
}
