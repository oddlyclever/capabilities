package com.oddlyclever.webp

import org.jetbrains.skia.ColorAlphaType
import org.jetbrains.skia.ColorType
import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.Image
import org.jetbrains.skia.ImageInfo
import java.awt.image.BufferedImage

// Lossy through Skia, whose WebP encoder is libwebp. Skia offers no lossless WebP, so lossless is Vp8lEncoder's.
internal actual fun encodeRgba(pixels: RgbaPixels, quality: Int, lossless: Boolean): ByteArray? {
    if (lossless) return Vp8lEncoder.encode(pixels)
    val info = ImageInfo(pixels.width, pixels.height, ColorType.RGBA_8888, ColorAlphaType.UNPREMUL)
    val image = Image.makeRaster(info, pixels.bytes, pixels.rowBytes)
    try {
        val data = image.encodeToData(EncodedImageFormat.WEBP, quality) ?: return null
        try {
            return data.bytes
        } finally {
            data.close()
        }
    } finally {
        image.close()
    }
}

/** This image's pixels as straight-alpha RGBA, whatever its type. */
fun BufferedImage.toRgbaPixels(): RgbaPixels {
    val argb = getRGB(0, 0, width, height, null, 0, width)
    val bytes = ByteArray(argb.size * 4)
    for (i in argb.indices) {
        val c = argb[i]
        bytes[4 * i] = (c shr 16).toByte()
        bytes[4 * i + 1] = (c shr 8).toByte()
        bytes[4 * i + 2] = c.toByte()
        bytes[4 * i + 3] = (c ushr 24).toByte()
    }
    return RgbaPixels(width, height, bytes)
}
