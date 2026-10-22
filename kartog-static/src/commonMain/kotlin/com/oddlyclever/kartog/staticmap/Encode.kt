package com.oddlyclever.kartog.staticmap

import androidx.compose.ui.graphics.ImageBitmap
import com.oddlyclever.webp.RgbaPixels
import com.oddlyclever.webp.WebpException
import com.oddlyclever.webp.WebpMetadata
import com.oddlyclever.webp.encodeToWebp as encodePixelsToWebp

/**
 * This image as PNG bytes, for a file, an upload or an email attachment. Lossless, so a map's thin lines and small
 * text stay crisp, at the cost of size.
 *
 * @throws IllegalStateException when the platform's encoder refuses the image.
 */
expect fun ImageBitmap.encodeToPng(): ByteArray

/**
 * This image as lossy WebP bytes at [quality], 0 to 100: a fraction of the PNG's size, for an image that is sent or
 * saved. [metadata], such as the place the map shows, is written into the file as EXIF.
 *
 * Encoded by the webp library: with libwebp built in on Android and iOS, so the output does not depend on the
 * device's own encoder, and with Skia on the JVM and the web.
 *
 * @throws WebpException when the encoder refuses the image.
 * @throws IllegalArgumentException when [quality] is outside 0..100.
 */
fun ImageBitmap.encodeToWebp(quality: Int = MAP_WEBP_QUALITY, metadata: WebpMetadata? = null): ByteArray =
    toRgbaPixels().encodePixelsToWebp(quality, metadata = metadata)

/** High enough that roads and labels keep their edges; WebP's artefacts show first on thin lines. */
const val MAP_WEBP_QUALITY = 90

/** This image's pixels as the webp library takes them: R, G, B, A bytes with straight alpha. */
private fun ImageBitmap.toRgbaPixels(): RgbaPixels {
    // Compose reads pixels as straight-alpha ARGB on every platform.
    val argb = IntArray(width * height)
    readPixels(argb)
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
