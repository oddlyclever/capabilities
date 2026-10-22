package com.oddlyclever.media.image

import com.oddlyclever.media.webp.WebpMetadata
import com.oddlyclever.media.webp.toExif
import com.oddlyclever.media.webp.withWebpMetadata

/**
 * [encoded], an image [format] wrote with no metadata, with [metadata] written into it as EXIF. Every platform's
 * encoder writes none, so this is the only EXIF the result carries.
 *
 * - **WebP:** an `EXIF` chunk, through media-webp.
 * - **JPEG:** an APP1 segment straight after the start of the image, or after the JFIF header when there is one.
 * - **PNG:** an `eXIf` chunk straight after the header chunk, before the image data as the format requires.
 */
internal fun embedMetadata(encoded: ByteArray, format: ImageFormat, metadata: WebpMetadata?): ByteArray {
    if (metadata == null || metadata.isEmpty) return encoded
    return when (format) {
        ImageFormat.Webp -> encoded.withWebpMetadata(metadata)
        ImageFormat.Jpeg -> jpegWithExif(encoded, metadata.toExif())
        ImageFormat.Png -> pngWithExif(encoded, metadata.toExif())
    }
}

internal fun jpegWithExif(jpeg: ByteArray, tiff: ByteArray): ByteArray {
    val segment = ByteArray(10 + tiff.size)
    val length = 8 + tiff.size
    segment[0] = 0xFF.toByte(); segment[1] = 0xE1.toByte()
    segment[2] = (length ushr 8).toByte(); segment[3] = length.toByte()
    "Exif\u0000\u0000".encodeToByteArray().copyInto(segment, 4)
    tiff.copyInto(segment, 10)
    // JFIF requires its APP0 header to come first, so the EXIF goes after it when it is there.
    var at = 2
    if (jpeg.size > 4 && jpeg[2] == 0xFF.toByte() && jpeg[3] == 0xE0.toByte()) {
        at = 4 + (((jpeg[4].toInt() and 0xff) shl 8) or (jpeg[5].toInt() and 0xff))
    }
    return jpeg.copyOfRange(0, at) + segment + jpeg.copyOfRange(at, jpeg.size)
}

internal fun pngWithExif(png: ByteArray, tiff: ByteArray): ByteArray {
    // The signature is 8 bytes and IHDR, always first, is 25: a 13-byte payload in a 12-byte frame.
    val afterHeader = 8 + 25
    val type = "eXIf".encodeToByteArray()
    val chunk = ByteArray(12 + tiff.size)
    putBeU32(chunk, 0, tiff.size)
    type.copyInto(chunk, 4)
    tiff.copyInto(chunk, 8)
    putBeU32(chunk, 8 + tiff.size, crc32(type + tiff))
    return png.copyOfRange(0, afterHeader) + chunk + png.copyOfRange(afterHeader, png.size)
}

private fun putBeU32(b: ByteArray, at: Int, v: Int) {
    b[at] = (v ushr 24).toByte(); b[at + 1] = (v ushr 16).toByte(); b[at + 2] = (v ushr 8).toByte(); b[at + 3] = v.toByte()
}

/** The CRC-32 a PNG chunk ends with, over its type and payload. */
private fun crc32(bytes: ByteArray): Int {
    var crc = -1
    for (byte in bytes) {
        crc = crc xor (byte.toInt() and 0xff)
        repeat(8) { crc = if (crc and 1 != 0) (crc ushr 1) xor 0xEDB88320.toInt() else crc ushr 1 }
    }
    return crc.inv()
}
