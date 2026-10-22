package com.oddlyclever.media.image

import com.oddlyclever.media.webp.RgbaPixels
import com.oddlyclever.media.webp.encodeToWebp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.Canvas
import org.jetbrains.skia.ColorAlphaType
import org.jetbrains.skia.ColorType
import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.FilterMipmap
import org.jetbrains.skia.FilterMode
import org.jetbrains.skia.Image
import org.jetbrains.skia.ImageInfo
import org.jetbrains.skia.MipmapMode
import org.jetbrains.skia.Rect
import java.io.File
import java.net.URI
import kotlin.math.roundToInt

/**
 * Decodes, resizes and encodes JPEG and PNG with Skia, and WebP with media-webp. Skia reads JPEG, PNG, WebP, GIF and
 * BMP, so a WebP can be resized as well as written. Skia stands a photo upright as it decodes it, from its EXIF
 * orientation; the EXIF is read here only for the metadata kept when asked.
 *
 * Skia's native library must be on the classpath at run time, as it is in a Compose desktop app; see the README.
 */
actual class ImageProcessor {

    actual suspend fun compressImage(
        uri: String,
        format: ImageFormat,
        maxDimension: Int,
        quality: Int,
        keepMetadata: Boolean,
    ): Result<ProcessedImage> = withContext(Dispatchers.IO) {
        val file = try {
            if (uri.startsWith("file://")) File(URI(uri)) else File(uri)
        } catch (e: Exception) {
            return@withContext Result.failure(ImageProcessingException.SourceUnreadable(uri, e))
        }
        val encoded = try {
            file.readBytes()
        } catch (e: Exception) {
            return@withContext Result.failure(ImageProcessingException.SourceUnreadable(uri, e))
        }
        val image = try {
            Image.makeFromEncoded(encoded)
        } catch (e: Exception) {
            return@withContext Result.failure(ImageProcessingException.DecodeFailed(uri, e))
        }

        try {
            // Only ever smaller: a thumbnail-sized source stays its own size.
            val scale = minOf(1.0, maxDimension.toDouble() / maxOf(image.width, image.height))
            val width = (image.width * scale).roundToInt().coerceAtLeast(1)
            val height = (image.height * scale).roundToInt().coerceAtLeast(1)

            val bitmap = Bitmap()
            bitmap.allocPixels(ImageInfo.makeN32Premul(width, height))
            // Mipmapped sampling, so a large reduction averages the pixels it drops instead of aliasing them.
            Canvas(bitmap).use { canvas ->
                canvas.drawImageRect(
                    image,
                    Rect.makeWH(image.width.toFloat(), image.height.toFloat()),
                    Rect.makeWH(width.toFloat(), height.toFloat()),
                    FilterMipmap(FilterMode.LINEAR, MipmapMode.LINEAR),
                    null,
                    true,
                )
            }

            val rgba = bitmap.readPixels(ImageInfo(width, height, ColorType.RGBA_8888, ColorAlphaType.UNPREMUL), width * 4, 0, 0)
                ?: return@withContext Result.failure(ImageProcessingException.EncodeFailed(format))

            if (!format.supportsAlpha && rgba.hasNonOpaquePixels()) {
                return@withContext Result.failure(ImageProcessingException.AlphaNotRepresentable(format))
            }

            val bytes = try {
                when (format) {
                    ImageFormat.Webp -> RgbaPixels(width, height, rgba).encodeToWebp(quality)
                    ImageFormat.Jpeg -> bitmap.encode(EncodedImageFormat.JPEG, quality)
                    ImageFormat.Png -> bitmap.encode(EncodedImageFormat.PNG, 100)
                }
            } catch (e: Exception) {
                return@withContext Result.failure(ImageProcessingException.EncodeFailed(format, e))
            }
            if (bytes == null || bytes.isEmpty()) {
                return@withContext Result.failure(ImageProcessingException.EncodeFailed(format))
            }

            val withMetadata = embedMetadata(bytes, format, if (keepMetadata) SourceExifReader.read(encoded).metadata else null)
            Result.success(ProcessedImage(bytes = withMetadata, contentType = format.contentType, width = width, height = height))
        } finally {
            image.close()
        }
    }

    private fun Bitmap.encode(format: EncodedImageFormat, quality: Int): ByteArray? =
        Image.makeFromBitmap(this).use { it.encodeToData(format, quality)?.use { data -> data.bytes } }

    /** True if any pixel is not fully opaque: every fourth byte of RGBA is alpha. */
    private fun ByteArray.hasNonOpaquePixels(): Boolean {
        for (i in 3 until size step 4) if (this[i] != (-1).toByte()) return true
        return false
    }
}
