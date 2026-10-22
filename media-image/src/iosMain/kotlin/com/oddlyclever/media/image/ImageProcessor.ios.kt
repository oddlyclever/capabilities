package com.oddlyclever.media.image

import com.oddlyclever.media.webp.encodeToWebp
import kotlinx.cinterop.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import platform.CoreGraphics.*
import platform.Foundation.*
import platform.ImageIO.*
import platform.UIKit.UIImage
import platform.UIKit.UIImageJPEGRepresentation
import platform.UIKit.UIImagePNGRepresentation
import platform.posix.memcpy

/**
 * Decodes with UIKit, resizes with Core Graphics, and encodes WebP with media-webp and JPEG and PNG with UIKit.
 */
@OptIn(ExperimentalForeignApi::class)
actual class ImageProcessor {

    actual suspend fun compressImage(
        uri: String,
        format: ImageFormat,
        maxDimension: Int,
        quality: Int,
    ): Result<ProcessedImage> = withContext(Dispatchers.Default) {
        val url = if (uri.startsWith("file://")) NSURL(string = uri) else NSURL.fileURLWithPath(uri)
        val path = url.path
            ?: return@withContext Result.failure(ImageProcessingException.SourceUnreadable(uri))

        // UIKit answers a missing file and one that is not an image alike, so look for the file first.
        if (!NSFileManager.defaultManager.fileExistsAtPath(path)) {
            return@withContext Result.failure(ImageProcessingException.SourceUnreadable(uri))
        }

        // The class method, which can return nil: the initializer is bound as non-null and throws on a file that is
        // not an image.
        val cgImage = UIImage.imageWithContentsOfFile(path)?.CGImage
            ?: return@withContext Result.failure(ImageProcessingException.DecodeFailed(uri))

        val width = CGImageGetWidth(cgImage).toDouble()
        val height = CGImageGetHeight(cgImage).toDouble()

        if (width <= 0 || height <= 0) {
            return@withContext Result.failure(ImageProcessingException.DecodeFailed(uri))
        }

        // 1. Calculate new dimensions
        var newWidth = width
        var newHeight = height

        if (width > maxDimension || height > maxDimension) {
            val scale = maxDimension.toDouble() / maxOf(width, height)
            newWidth = width * scale
            newHeight = height * scale
        }

        // 2. Resize using Core Graphics, which draws only premultiplied alpha.
        val colorSpace = CGColorSpaceCreateDeviceRGB()
        val context = CGBitmapContextCreate(
            null,
            newWidth.toULong(),
            newHeight.toULong(),
            8uL,
            0uL,
            colorSpace,
            CGImageAlphaInfo.kCGImageAlphaPremultipliedLast.value
        )
        CGColorSpaceRelease(colorSpace)
        if (context == null) return@withContext Result.failure(ImageProcessingException.EncodeFailed(format))

        val resizedCGImage = try {
            CGContextSetInterpolationQuality(context, kCGInterpolationHigh)
            CGContextDrawImage(context, CGRectMake(0.0, 0.0, newWidth, newHeight), cgImage)
            // A declared alpha channel is often unused, so look at the pixels before refusing an alpha-less format.
            if (!format.supportsAlpha && cgImage.mayHaveAlpha() && context.hasNonOpaquePixels()) {
                return@withContext Result.failure(ImageProcessingException.AlphaNotRepresentable(format))
            }
            CGBitmapContextCreateImage(context)
        } finally {
            CGContextRelease(context)
        } ?: return@withContext Result.failure(ImageProcessingException.EncodeFailed(format))

        val resizedUIImage = UIImage.imageWithCGImage(resizedCGImage)
        CGImageRelease(resizedCGImage)

        val bytes: ByteArray? = try {
            when (format) {
                ImageFormat.Webp -> resizedUIImage.encodeToWebp(quality)
                ImageFormat.Jpeg -> UIImageJPEGRepresentation(resizedUIImage, quality / 100.0)?.toByteArray()
                ImageFormat.Png -> UIImagePNGRepresentation(resizedUIImage)?.toByteArray()
            }
        } catch (e: Exception) {
            return@withContext Result.failure(ImageProcessingException.EncodeFailed(format, e))
        }

        if (bytes == null || bytes.isEmpty()) {
            return@withContext Result.failure(ImageProcessingException.EncodeFailed(format))
        }

        Result.success(
            ProcessedImage(
                bytes = bytes,
                contentType = format.contentType,
                width = newWidth.toInt(),
                height = newHeight.toInt(),
            )
        )
    }

    /** Whether the image declares an alpha channel at all; when it does not, no pixel can be translucent. */
    private fun CGImageRef.mayHaveAlpha(): Boolean =
        when (CGImageGetAlphaInfo(this)) {
            CGImageAlphaInfo.kCGImageAlphaNone,
            CGImageAlphaInfo.kCGImageAlphaNoneSkipFirst,
            CGImageAlphaInfo.kCGImageAlphaNoneSkipLast -> false
            else -> true
        }

    /** True if any pixel the context holds is not fully opaque: the last of each pixel's four bytes is alpha. */
    private fun CGContextRef.hasNonOpaquePixels(): Boolean {
        val data = CGBitmapContextGetData(this)?.reinterpret<UByteVar>() ?: return true
        val rowBytes = CGBitmapContextGetBytesPerRow(this).toInt()
        val width = CGBitmapContextGetWidth(this).toInt()
        val height = CGBitmapContextGetHeight(this).toInt()
        for (y in 0 until height) {
            val row = y * rowBytes
            for (x in 0 until width) if (data[row + 4 * x + 3] != 0xFF.toUByte()) return true
        }
        return false
    }

    @OptIn(ExperimentalForeignApi::class)
    private fun NSData.toByteArray(): ByteArray {
        val bytes = ByteArray(this.length.toInt())
        if (bytes.isNotEmpty()) {
            bytes.usePinned { pinned ->
                memcpy(pinned.addressOf(0), this.bytes, this.length)
                Unit
            }
        }
        return bytes
    }
}
