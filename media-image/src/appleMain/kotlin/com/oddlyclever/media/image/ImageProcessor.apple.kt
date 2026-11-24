package com.oddlyclever.media.image

import com.oddlyclever.media.webp.GpsLocation
import com.oddlyclever.media.webp.RgbaPixels
import com.oddlyclever.media.webp.WebpMetadata
import com.oddlyclever.media.webp.encodeToWebp
import platform.CoreFoundation.CFDataRef
import platform.CoreFoundation.CFDictionaryRef
import platform.CoreFoundation.CFRelease
import platform.CoreFoundation.CFURLRef
import kotlinx.cinterop.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import platform.CoreGraphics.*
import platform.Foundation.*
import platform.ImageIO.*

/**
 * Decodes, shrinks and turns upright with ImageIO, checks transparency with Core Graphics, and encodes WebP with
 * media-webp and JPEG and PNG with ImageIO. The same on iOS and macOS: nothing here is UIKit or AppKit.
 */
@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
actual class ImageProcessor {

    actual suspend fun compressImage(
        uri: String,
        format: ImageFormat,
        maxDimension: Int,
        quality: Int,
        keepMetadata: Boolean,
    ): Result<ProcessedImage> = withContext(Dispatchers.Default) {
        // ImageIO answers a missing file and one that is not an image alike, so look for the file first.
        val url = existingFile(uri)
            ?: return@withContext Result.failure(ImageProcessingException.SourceUnreadable(uri))

        // From the file rather than its bytes: ImageIO reads only what the thumbnail needs, so the file is never in
        // memory whole either.
        val urlRef = CFBridgingRetain(url)
        val source = CGImageSourceCreateWithURL(urlRef as CFURLRef?, null)
        CFRelease(urlRef)
        compress(source, uri, format, maxDimension, quality, keepMetadata)
    }

    actual suspend fun compressImage(
        bytes: ByteArray,
        format: ImageFormat,
        maxDimension: Int,
        quality: Int,
        keepMetadata: Boolean,
    ): Result<ProcessedImage> = withContext(Dispatchers.Default) {
        if (bytes.isEmpty()) return@withContext Result.failure(ImageProcessingException.DecodeFailed(BytesSource))
        val data = bytes.usePinned { NSData.create(bytes = it.addressOf(0), length = bytes.size.toULong()) }
        val dataRef = CFBridgingRetain(data)
        val source = CGImageSourceCreateWithData(dataRef as CFDataRef?, null)
        CFRelease(dataRef)
        compress(source, BytesSource, format, maxDimension, quality, keepMetadata)
    }

    actual suspend fun readSource(uri: String): Result<ByteArray> = withContext(Dispatchers.Default) {
        val data = existingFile(uri)?.path?.let { NSData.dataWithContentsOfFile(it) }
        if (data != null) Result.success(data.toByteArray()) else Result.failure(ImageProcessingException.SourceUnreadable(uri))
    }

    /** The file [uri] names, a `file://` URI or a path, or null when there is no such file. */
    private fun existingFile(uri: String): NSURL? {
        val url = if (uri.startsWith("file://")) NSURL(string = uri) else NSURL.fileURLWithPath(uri)
        val path = url.path ?: return null
        return if (NSFileManager.defaultManager.fileExistsAtPath(path)) url else null
    }

    /**
     * Decodes [source], which this releases, and does the rest; [label] is what a failure names.
     *
     * ImageIO decodes, shrinks and stands the photo upright in one pass, so a 48-megapixel photo is never decoded
     * whole. UIImage would decode it whole, and its CGImage ignores the EXIF orientation.
     */
    private fun compress(
        source: CGImageSourceRef?,
        label: String,
        format: ImageFormat,
        maxDimension: Int,
        quality: Int,
        keepMetadata: Boolean,
    ): Result<ProcessedImage> {
        if (source == null || CGImageSourceGetCount(source) == 0uL) {
            source?.let { CFRelease(it) }
            return Result.failure(ImageProcessingException.DecodeFailed(label))
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
        if (cgImage == null) return Result.failure(ImageProcessingException.DecodeFailed(label))
        val metadata = if (keepMetadata) metadataOf(properties) else null

        val newWidth = CGImageGetWidth(cgImage).toDouble()
        val newHeight = CGImageGetHeight(cgImage).toDouble()

        // Into a bitmap context, to look at the pixels' alpha and hand the encoders an image; Core Graphics draws only
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
            return Result.failure(ImageProcessingException.EncodeFailed(format))
        }

        val resizedCGImage = try {
            CGContextSetInterpolationQuality(context, kCGInterpolationHigh)
            CGContextDrawImage(context, CGRectMake(0.0, 0.0, newWidth, newHeight), cgImage)
            // A declared alpha channel is often unused, so look at the pixels before refusing an alpha-less format.
            if (!format.supportsAlpha && cgImage.mayHaveAlpha() && context.hasNonOpaquePixels()) {
                return Result.failure(ImageProcessingException.AlphaNotRepresentable(format))
            }
            CGBitmapContextCreateImage(context)
        } finally {
            CGContextRelease(context)
            CGImageRelease(cgImage)
        } ?: return Result.failure(ImageProcessingException.EncodeFailed(format))

        val bytes: ByteArray? = try {
            when (format) {
                ImageFormat.Webp -> resizedCGImage.toRgbaPixels().encodeToWebp(quality)
                ImageFormat.Jpeg -> resizedCGImage.encodeWithImageIO(JpegType, quality / 100.0)
                ImageFormat.Png -> resizedCGImage.encodeWithImageIO(PngType)
            }
        } catch (e: Exception) {
            return Result.failure(ImageProcessingException.EncodeFailed(format, e))
        } finally {
            CGImageRelease(resizedCGImage)
        }

        if (bytes == null || bytes.isEmpty()) {
            return Result.failure(ImageProcessingException.EncodeFailed(format))
        }

        return Result.success(
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

    /** This image's pixels as straight-alpha RGBA in sRGB, for media-webp, which takes them that way. */
    private fun CGImageRef.toRgbaPixels(): RgbaPixels {
        val width = CGImageGetWidth(this).toInt()
        val height = CGImageGetHeight(this).toInt()
        val bytes = ByteArray(width * height * 4)
        bytes.usePinned { pinned ->
            val space = CGColorSpaceCreateWithName(kCGColorSpaceSRGB)
            // Core Graphics draws only premultiplied alpha; R, G, B, A in memory with a big-endian 32-bit order.
            val context = CGBitmapContextCreate(
                pinned.addressOf(0), width.toULong(), height.toULong(), 8u, (width * 4).toULong(), space,
                CGImageAlphaInfo.kCGImageAlphaPremultipliedLast.value or kCGBitmapByteOrder32Big,
            )
            CGContextDrawImage(context, CGRectMake(0.0, 0.0, width.toDouble(), height.toDouble()), this)
            CGContextRelease(context)
            CGColorSpaceRelease(space)
        }
        // Back to straight alpha: each colour over its pixel's alpha, rounded.
        for (i in bytes.indices step 4) {
            val alpha = bytes[i + 3].toInt() and 0xFF
            if (alpha == 0 || alpha == 0xFF) continue
            for (c in 0..2) bytes[i + c] = (((bytes[i + c].toInt() and 0xFF) * 255 + alpha / 2) / alpha).coerceAtMost(255).toByte()
        }
        return RgbaPixels(width, height, bytes)
    }
}
