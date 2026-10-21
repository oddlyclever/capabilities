package com.oddlyclever.webp

import android.graphics.Bitmap
import android.os.Build

internal actual fun encodeRgba(pixels: RgbaPixels, quality: Int, lossless: Boolean): ByteArray? =
    NativeWebp.encodeRgba(pixels.bytes, pixels.width, pixels.height, pixels.rowBytes, quality.toFloat(), lossless)

/**
 * This bitmap's pixels, converted to straight-alpha RGBA whatever its config. A hardware bitmap, which cannot be
 * read directly, is copied to memory first.
 */
fun Bitmap.toRgbaPixels(): RgbaPixels {
    // Hardware bitmaps arrived in API 26; reading the constant on an older release fails.
    val hardware = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && config == Bitmap.Config.HARDWARE
    val readable = if (hardware) copy(Bitmap.Config.ARGB_8888, false) else this
    val argb = IntArray(width * height)
    // getPixels returns non-premultiplied ARGB, converting from any config.
    readable.getPixels(argb, 0, width, 0, 0, width, height)
    if (readable !== this) readable.recycle()
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

/**
 * This bitmap as a WebP, through the libwebp built into this library rather than `Bitmap.compress`, whose WebP
 * output depends on the Android release and the device and has been seen to come back empty without an error.
 *
 * @see encodeToWebp
 */
fun Bitmap.encodeToWebp(
    quality: Int = DEFAULT_WEBP_QUALITY,
    compression: WebpCompression = WebpCompression.Lossy,
    metadata: WebpMetadata? = null,
): ByteArray = toRgbaPixels().encodeToWebp(quality, compression, metadata)
