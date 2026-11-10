package com.oddlyclever.media.image

/**
 * What an image is, told from its bytes by [imageKindOf] before anything decodes it.
 *
 * These are kinds of source, not targets: [ImageFormat] is what [ImageProcessor] writes. The kinds that matter most
 * are the two a caller should usually not convert at all. Rasterizing an [Svg] makes it larger and stops it scaling,
 * and [ImageProcessor.compressImage] keeps only the first frame of an [AnimatedGif]. `compressImage` does not refuse
 * either; telling them apart here is how a caller decides to store them as they are.
 *
 * [contentType] is the MIME type to store an unconverted image under. Store that, not the member's name.
 */
enum class ImageKind(val contentType: String) {
    Svg("image/svg+xml"),
    AnimatedGif("image/gif"),
    Gif("image/gif"),
    Webp("image/webp"),
    Png("image/png"),
    Jpeg("image/jpeg"),
}

/**
 * What [bytes] hold, from their signature, or null when they are none of the [ImageKind]s.
 *
 * Nothing is decoded. PNG, JPEG, WebP and GIF are known by their first bytes. A GIF is walked block by block, without
 * decompressing anything, until a second frame shows it is animated, so the whole file may be read. An SVG is known by
 * its root element: `<svg` after an optional byte-order mark, whitespace, XML declaration, comments and doctype,
 * within the first 64 KiB. A compressed SVG (`.svgz`) or one in UTF-16 is not recognised.
 */
fun imageKindOf(bytes: ByteArray): ImageKind? = when {
    bytes.startsWith(PngSignature) -> ImageKind.Png
    bytes.startsWith(JpegSignature) -> ImageKind.Jpeg
    bytes.startsWith(Riff) && bytes.size >= 12 && bytes.matchesAt(8, WebpFourCc) -> ImageKind.Webp
    bytes.startsWith(Gif87a) || bytes.startsWith(Gif89a) -> if (gifFrameCount(bytes, stopAt = 2) > 1) ImageKind.AnimatedGif else ImageKind.Gif
    isSvg(bytes) -> ImageKind.Svg
    else -> null
}

private val PngSignature = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A)
private val JpegSignature = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte())
private val Riff = "RIFF".encodeToByteArray()
private val WebpFourCc = "WEBP".encodeToByteArray()
private val Gif87a = "GIF87a".encodeToByteArray()
private val Gif89a = "GIF89a".encodeToByteArray()

private fun ByteArray.startsWith(prefix: ByteArray): Boolean = matchesAt(0, prefix)

private fun ByteArray.matchesAt(offset: Int, expected: ByteArray): Boolean {
    if (offset < 0 || offset + expected.size > size) return false
    for (i in expected.indices) if (this[offset + i] != expected[i]) return false
    return true
}

private fun ByteArray.u8(index: Int): Int = this[index].toInt() and 0xFF

/**
 * How many frames a GIF holds, counting image descriptors, up to [stopAt]. A file that ends early counts what it got
 * through; the blocks are skipped by their lengths, so no image data is decompressed.
 */
private fun gifFrameCount(bytes: ByteArray, stopAt: Int): Int {
    // The header is 6 bytes and the logical screen descriptor 7; its packed byte says whether a global colour table follows.
    if (bytes.size < 13) return 0
    var at = 13
    val screenPacked = bytes.u8(10)
    if (screenPacked and 0x80 != 0) at += 3 * (1 shl ((screenPacked and 0x07) + 1))
    var frames = 0
    while (at < bytes.size) {
        when (bytes.u8(at)) {
            0x21 -> {
                // Extension: introducer, label, then data sub-blocks.
                at = skipSubBlocks(bytes, at + 2) ?: return frames
            }
            0x2C -> {
                frames++
                if (frames >= stopAt) return frames
                // Image descriptor: 9 bytes after the separator, the last packed, then an optional local colour table,
                // the LZW minimum code size, and the image data's sub-blocks.
                if (at + 10 > bytes.size) return frames
                val packed = bytes.u8(at + 9)
                at += 10
                if (packed and 0x80 != 0) at += 3 * (1 shl ((packed and 0x07) + 1))
                at = skipSubBlocks(bytes, at + 1) ?: return frames
            }
            else -> return frames // the trailer, 0x3B, or bytes that are not a GIF block
        }
    }
    return frames
}

/** The index just past the zero-length block that ends a run of sub-blocks starting at [start], or null if it runs off the end. */
private fun skipSubBlocks(bytes: ByteArray, start: Int): Int? {
    var at = start
    while (at < bytes.size) {
        val length = bytes.u8(at)
        at += 1
        if (length == 0) return at
        at += length
    }
    return null
}

private const val SvgScanLimit = 64 * 1024

private fun isSvg(bytes: ByteArray): Boolean {
    val end = minOf(bytes.size, SvgScanLimit)
    var at = 0
    if (bytes.size >= 3 && bytes.u8(0) == 0xEF && bytes.u8(1) == 0xBB && bytes.u8(2) == 0xBF) at = 3
    while (true) {
        while (at < end && bytes.u8(at).isXmlSpace()) at++
        if (at >= end || bytes.u8(at) != '<'.code) return false
        when {
            bytes.matchesAscii(at, "<?") -> at = bytes.indexOfAscii("?>", at + 2, end)?.plus(2) ?: return false
            bytes.matchesAscii(at, "<!--") -> at = bytes.indexOfAscii("-->", at + 4, end)?.plus(3) ?: return false
            bytes.matchesAscii(at, "<!DOCTYPE") || bytes.matchesAscii(at, "<!doctype") -> at = bytes.endOfDoctype(at + 9, end) ?: return false
            bytes.matchesAscii(at, "<svg") -> {
                // The root element is svg itself, not one whose name merely starts with it.
                val next = if (at + 4 < bytes.size) bytes.u8(at + 4) else return false
                return next.isXmlSpace() || next == '>'.code || next == '/'.code
            }
            else -> return false
        }
    }
}

private fun Int.isXmlSpace(): Boolean = this == 0x20 || this == 0x09 || this == 0x0A || this == 0x0D

private fun ByteArray.matchesAscii(offset: Int, text: String): Boolean {
    if (offset + text.length > size) return false
    for (i in text.indices) if (u8(offset + i) != text[i].code) return false
    return true
}

private fun ByteArray.indexOfAscii(text: String, from: Int, end: Int): Int? {
    var at = from
    while (at + text.length <= end) {
        if (matchesAscii(at, text)) return at
        at++
    }
    return null
}

/** Past the `>` that closes a doctype, skipping an internal subset in square brackets, which may hold `>` of its own. */
private fun ByteArray.endOfDoctype(from: Int, end: Int): Int? {
    var at = from
    var inSubset = false
    while (at < end) {
        when (u8(at)) {
            '['.code -> inSubset = true
            ']'.code -> inSubset = false
            '>'.code -> if (!inSubset) return at + 1
        }
        at++
    }
    return null
}
