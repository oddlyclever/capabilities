package com.oddlyclever.media.webp

/**
 * An image to encode, as raw pixels: [height] rows of [width] pixels, four bytes each in red, green, blue, alpha
 * order, with alpha **not** premultiplied. Row `y` starts at `y * rowBytes`.
 *
 * Raw pixels rather than a platform image type, so the encoder is the same call on every target and needs no UI
 * toolkit. Each platform's image type converts to it: `Bitmap.toRgbaPixels()` on Android, `UIImage.toRgbaPixels()`
 * on iOS and `BufferedImage.toRgbaPixels()` on the JVM.
 *
 * The bytes are not copied, so changing them after construction changes what is encoded.
 */
class RgbaPixels(
    val width: Int,
    val height: Int,
    val bytes: ByteArray,
    val rowBytes: Int = width * 4,
) {
    init {
        require(width in 1..MAX_DIMENSION && height in 1..MAX_DIMENSION) {
            "A WebP is 1 to $MAX_DIMENSION pixels a side, not $width × $height"
        }
        require(rowBytes >= width * 4) { "rowBytes $rowBytes is less than four bytes a pixel for a width of $width" }
        require(bytes.size.toLong() >= rowBytes.toLong() * (height - 1) + width * 4L) {
            "${bytes.size} bytes cannot hold $height rows of $rowBytes bytes"
        }
    }

    companion object {
        /** The largest width or height a WebP can have. */
        const val MAX_DIMENSION: Int = 16383
    }
}

/**
 * Turns premultiplied RGBA bytes, which is what Core Graphics and Skia draw into, into the straight alpha [RgbaPixels]
 * holds, in place.
 */
internal fun unpremultiply(bytes: ByteArray) {
    for (i in 0 until bytes.size - 3 step 4) {
        val a = bytes[i + 3].toInt() and 0xff
        if (a == 0 || a == 255) continue
        for (c in i until i + 3) {
            val v = bytes[c].toInt() and 0xff
            bytes[c] = minOf(255, (v * 255 + a / 2) / a).toByte()
        }
    }
}
