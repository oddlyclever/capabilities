package com.oddlyclever.webp

/** Reads back the tags [Exif] writes, so the tests check the bytes rather than trust the writer's own arithmetic. */
class ExifReader(private val tiff: ByteArray) {
    class Tag(val type: Int, val count: Int, val value: ByteArray)

    init {
        check(tiff.decodeToString(0, 2) == "II" && u16(2) == 42) { "not a little-endian TIFF header" }
    }

    val primary: Map<Int, Tag> = ifd(u32(4))
    val exif: Map<Int, Tag> = primary[0x8769]?.let { ifd(u32(it.value, 0)) } ?: emptyMap()
    val gps: Map<Int, Tag> = primary[0x8825]?.let { ifd(u32(it.value, 0)) } ?: emptyMap()

    private fun ifd(at: Int): Map<Int, Tag> = (0 until u16(at)).associate { i ->
        val entry = at + 2 + 12 * i
        val type = u16(entry + 2)
        val count = u32(entry + 4)
        val size = count * when (type) { 3 -> 2; 4 -> 4; 5 -> 8; else -> 1 }
        val from = if (size <= 4) entry + 8 else u32(entry + 8)
        u16(entry) to Tag(type, count, tiff.copyOfRange(from, from + size))
    }

    private fun u16(at: Int) = (tiff[at].toInt() and 0xff) or ((tiff[at + 1].toInt() and 0xff) shl 8)
    private fun u32(at: Int) = u32(tiff, at)

    companion object {
        fun u32(b: ByteArray, at: Int) = (b[at].toInt() and 0xff) or ((b[at + 1].toInt() and 0xff) shl 8) or
            ((b[at + 2].toInt() and 0xff) shl 16) or ((b[at + 3].toInt() and 0xff) shl 24)

        fun ascii(tag: Tag) = tag.value.decodeToString().trimEnd('\u0000')
        fun rationals(tag: Tag) = (0 until tag.count).map { u32(tag.value, 8 * it).toDouble() / u32(tag.value, 8 * it + 4) }

        /** Degrees from EXIF's degrees, minutes and seconds. */
        fun degrees(tag: Tag) = rationals(tag).let { (d, m, s) -> d + m / 60 + s / 3600 }
    }
}

/** The chunk ids of a WebP, in order. */
fun ByteArray.chunkIds(): List<String> {
    val ids = mutableListOf<String>()
    var at = 12
    while (at + 8 <= size) {
        ids += decodeToString(at, at + 4)
        val length = ExifReader.u32(this, at + 4)
        at += 8 + length + (length and 1)
    }
    return ids
}

/** The payload of this WebP's first chunk named [id]. */
fun ByteArray.chunk(id: String): ByteArray? {
    var at = 12
    while (at + 8 <= size) {
        val length = ExifReader.u32(this, at + 4)
        if (decodeToString(at, at + 4) == id) return copyOfRange(at + 8, at + 8 + length)
        at += 8 + length + (length and 1)
    }
    return null
}
