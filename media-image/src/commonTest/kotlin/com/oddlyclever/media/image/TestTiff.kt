package com.oddlyclever.media.image

/**
 * An EXIF block as a camera writes it, in either byte order, with only the tags the tests need: so the reader is
 * tested against EXIF it did not write itself, including the big-endian kind iPhones produce.
 */
fun testTiff(
    bigEndian: Boolean,
    orientation: Int? = null,
    latitude: Double? = null,
    longitude: Double? = null,
    original: String? = null,
    offset: String? = null,
): ByteArray {
    class Entry(val tag: Int, val type: Int, val count: Int, val value: ByteArray)
    fun u16(v: Int) = if (bigEndian) byteArrayOf((v shr 8).toByte(), v.toByte()) else byteArrayOf(v.toByte(), (v shr 8).toByte())
    fun u32(v: Int) = if (bigEndian) u16(v ushr 16) + u16(v) else u16(v) + u16(v ushr 16)
    fun ascii(tag: Int, text: String) = (text.encodeToByteArray() + 0.toByte()).let { Entry(tag, 2, it.size, it) }
    fun dms(tag: Int, degrees: Double): Entry {
        val whole = degrees.toInt()
        val minutes = ((degrees - whole) * 60).toInt()
        val seconds = ((degrees - whole) * 3600 - minutes * 60) * 1_000_000
        val value = u32(whole) + u32(1) + u32(minutes) + u32(1) + u32(seconds.toLong().toInt()) + u32(1_000_000)
        return Entry(tag, 5, 3, value)
    }

    val exif = listOfNotNull(original?.let { ascii(0x9003, it) }, offset?.let { ascii(0x9011, it) })
    val gps = if (latitude == null || longitude == null) emptyList() else listOf(
        ascii(0x0001, if (latitude < 0) "S" else "N"), dms(0x0002, kotlin.math.abs(latitude)),
        ascii(0x0003, if (longitude < 0) "W" else "E"), dms(0x0004, kotlin.math.abs(longitude)),
    )
    fun size(entries: List<Entry>) = 2 + 12 * entries.size + 4 + entries.sumOf { if (it.value.size > 4) it.value.size + (it.value.size and 1) else 0 }
    val primaryCount = listOfNotNull(orientation, exif.ifEmpty { null }, gps.ifEmpty { null }).size
    val primarySize = 2 + 12 * primaryCount + 4
    val exifAt = 8 + primarySize
    val gpsAt = exifAt + if (exif.isEmpty()) 0 else size(exif)
    val primary = listOfNotNull(
        orientation?.let { Entry(0x0112, 3, 1, u16(it)) },
        exif.ifEmpty { null }?.let { Entry(0x8769, 4, 1, u32(exifAt)) },
        gps.ifEmpty { null }?.let { Entry(0x8825, 4, 1, u32(gpsAt)) },
    )

    var out = (if (bigEndian) "MM" else "II").encodeToByteArray() + u16(42) + u32(8)
    fun write(entries: List<Entry>) {
        var data = out.size + 2 + 12 * entries.size + 4
        out += u16(entries.size)
        val tail = mutableListOf<ByteArray>()
        for (e in entries.sortedBy { it.tag }) {
            out += u16(e.tag) + u16(e.type) + u32(e.count)
            if (e.value.size <= 4) {
                out += e.value + ByteArray(4 - e.value.size)
            } else {
                out += u32(data)
                data += e.value.size + (e.value.size and 1)
                tail += if (e.value.size % 2 == 1) e.value + 0.toByte() else e.value
            }
        }
        out += u32(0)
        tail.forEach { out += it }
    }
    write(primary)
    if (exif.isNotEmpty()) write(exif)
    if (gps.isNotEmpty()) write(gps)
    return out
}
