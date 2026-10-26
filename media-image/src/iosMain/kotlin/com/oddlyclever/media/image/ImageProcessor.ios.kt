package com.oddlyclever.media.image

import com.oddlyclever.media.webp.GpsLocation
import com.oddlyclever.media.webp.WebpMetadata
import com.oddlyclever.media.webp.encodeToWebp
import platform.CoreFoundation.CFDictionaryRef
import platform.CoreFoundation.CFRelease
import platform.CoreFoundation.CFRetain
import platform.CoreFoundation.CFStringRef
import platform.CoreFoundation.CFURLRef
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
 * Decodes, shrinks and turns upright with ImageIO, checks transparency with Core Graphics, and encodes WebP with
 * media-webp and JPEG and PNG with UIKit.
 */
@OptIn(ExperimentalForeignApi::class)
actual class ImageProcessor {

    actual suspend fun compressImage(
        uri: String,
        format: ImageFormat,
        maxDimension: Int,
        quality: Int,
        keepMetadata: Boolean,
    ): Result<ProcessedImage> = withContext(Dispatchers.Default) {
        val url = if (uri.startsWith("file://")) NSURL(string = uri) else NSURL.fileURLWithPath(uri)
        val path = url.path
            ?: return@withContext Result.failure(ImageProcessingException.SourceUnreadable(uri))

        // UIKit answers a missing file and one that is not an image alike, so look for the file first.
        if (!NSFileManager.defaultManager.fileExistsAtPath(path)) {
            return@withContext Result.failure(ImageProcessingException.SourceUnreadable(uri))
        }

        // ImageIO decodes, shrinks and stands the photo upright in one pass, from the file, so a 48-megapixel photo is
        // never decoded whole. UIImage would decode it whole, and its CGImage ignores the EXIF orientation.
        val urlRef = CFBridgingRetain(url)
        val source = CGImageSourceCreateWithURL(urlRef as CFURLRef?, null)
        CFRelease(urlRef)
        if (source == null || CGImageSourceGetCount(source) == 0uL) {
            source?.let { CFRelease(it) }
            return@withContext Result.failure(ImageProcessingException.DecodeFailed(uri))
        }

        val properties = CGImageSourceCopyPropertiesAtIndex(source, 0u, null)?.let { CFBridgingRelease(it) as? Map<*, *> }.orEmpty()
        val longest = maxOf(properties[key(kCGImagePropertyPixelWidth)].asDouble() ?: 0.0, properties[key(kCGImagePropertyPixelHeight)].asDouble() ?: 0.0)
        // Keyed by the constants' string values: a Core Foundation pointer is not a key an NSDictionary built from
        // Kotlin can hold, and a dictionary with such keys is silently ignored.
        val thumbnailOptions = CFBridgingRetain(
            mapOf(
                key(kCGImageSourceCreateThumbnailFromImageAlways) to true,
                key(kCGImageSourceCreateThumbnailWithTransform) to true,
                key(kCGImageSourceShouldCacheImmediately) to true,
                // Never larger than the source: the thumbnail call would otherwise scale a small image up.
                key(kCGImageSourceThumbnailMaxPixelSize) to minOf(maxDimension.toDouble(), longest),
            ),
        )
        val cgImage = if (longest > 0) CGImageSourceCreateThumbnailAtIndex(source, 0u, thumbnailOptions as CFDictionaryRef?) else null
        CFRelease(thumbnailOptions)
        CFRelease(source)
        if (cgImage == null) return@withContext Result.failure(ImageProcessingException.DecodeFailed(uri))
        val metadata = if (keepMetadata) metadataOf(properties) else null

        val newWidth = CGImageGetWidth(cgImage).toDouble()
        val newHeight = CGImageGetHeight(cgImage).toDouble()

        // Into a bitmap context, to look at the pixels' alpha and hand UIKit an image; Core Graphics draws only
        // premultiplied alpha.
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
        if (context == null) {
            CGImageRelease(cgImage)
            return@withContext Result.failure(ImageProcessingException.EncodeFailed(format))
        }

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
            CGImageRelease(cgImage)
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
                bytes = embedMetadata(bytes, format, metadata),
                contentType = format.contentType,
                width = newWidth.toInt(),
                height = newHeight.toInt(),
            )
        )
    }

    /** Where and when the photo was taken, from the properties ImageIO read with it. */
    private fun metadataOf(properties: Map<*, *>): WebpMetadata? {
        val gps = properties[key(kCGImagePropertyGPSDictionary)] as? Map<*, *>
        val exif = properties[key(kCGImagePropertyExifDictionary)] as? Map<*, *>
        val location = gps?.let {
            val latitude = it[key(kCGImagePropertyGPSLatitude)].asDouble() ?: return@let null
            val longitude = it[key(kCGImagePropertyGPSLongitude)].asDouble() ?: return@let null
            val lat = if (it[key(kCGImagePropertyGPSLatitudeRef)] == "S") -latitude else latitude
            val lon = if (it[key(kCGImagePropertyGPSLongitudeRef)] == "W") -longitude else longitude
            val altitude = it[key(kCGImagePropertyGPSAltitude)].asDouble()?.let { metres ->
                if (it[key(kCGImagePropertyGPSAltitudeRef)].asDouble() == 1.0) -metres else metres
            }
            if (lat in -90.0..90.0 && lon in -180.0..180.0) GpsLocation(lat, lon, altitude?.takeIf { a -> a.isFinite() }) else null
        }
        val gpsSeconds = (gps?.get(key(kCGImagePropertyGPSTimeStamp)) as? String)?.split(':')?.mapNotNull { it.toDoubleOrNull() }
            ?.takeIf { it.size == 3 }?.let { (h, m, sec) -> h * 3600 + m * 60 + sec }
        val capturedAt = exactCaptureTime(
            original = exif?.get(key(kCGImagePropertyExifDateTimeOriginal)) as? String,
            offset = exif?.get(key(kCGImagePropertyExifOffsetTimeOriginal)) as? String,
            gpsDate = gps?.get(key(kCGImagePropertyGPSDateStamp)) as? String,
            gpsSeconds = gpsSeconds,
        )
        return SourceExif(location = location, capturedAt = capturedAt).metadata
    }

    /** A Core Foundation string constant as the key it is in a bridged dictionary. */
    private fun key(constant: CFStringRef?): String = CFBridgingRelease(CFRetain(constant)) as String

    private fun Any?.asDouble(): Double? = when (this) {
        is NSNumber -> doubleValue
        is Number -> toDouble()
        is String -> toDoubleOrNull()
        else -> null
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
