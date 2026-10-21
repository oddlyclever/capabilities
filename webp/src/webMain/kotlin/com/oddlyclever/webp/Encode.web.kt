package com.oddlyclever.webp

import org.jetbrains.skia.ColorAlphaType
import org.jetbrains.skia.ColorType
import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.Image
import org.jetbrains.skia.ImageInfo

// Lossy through Skia, whose WebP encoder is libwebp. Skia offers no lossless WebP, so lossless is Vp8lEncoder's.
// Skia's WebAssembly must have loaded first, as it has in a Compose app.
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
