package com.oddlyclever.media.image

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.CoreGraphics.CGBitmapContextCreate
import platform.CoreGraphics.CGBitmapContextCreateImage
import platform.CoreGraphics.CGColorSpaceCreateDeviceRGB
import platform.CoreGraphics.CGContextFillRect
import platform.CoreGraphics.CGContextSetRGBFillColor
import platform.CoreGraphics.CGImageAlphaInfo
import platform.CoreGraphics.CGRectMake
import platform.Foundation.NSString
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSUUID
import platform.Foundation.writeToFile
import platform.Foundation.create
import platform.UIKit.UIImage
import platform.UIKit.UIImagePNGRepresentation

actual suspend fun awaitSkia() = Unit

@OptIn(ExperimentalForeignApi::class)
actual suspend fun writeTestImage(width: Int, height: Int, alpha: Int): String {
    val context = CGBitmapContextCreate(
        null, width.toULong(), height.toULong(), 8u, 0u, CGColorSpaceCreateDeviceRGB(),
        CGImageAlphaInfo.kCGImageAlphaPremultipliedLast.value,
    )
    for (x in 0 until width) {
        CGContextSetRGBFillColor(context, x.toDouble() / width, 0.5, 0.5, alpha / 255.0)
        CGContextFillRect(context, CGRectMake(x.toDouble(), 0.0, 1.0, height.toDouble()))
    }
    val png = UIImagePNGRepresentation(UIImage.imageWithCGImage(CGBitmapContextCreateImage(context)))!!
    val path = temporaryPath()
    png.writeToFile(path, atomically = true)
    return path
}

actual fun missingSource(): String = NSTemporaryDirectory() + "media-image-missing.png"

@OptIn(kotlinx.cinterop.BetaInteropApi::class, ExperimentalForeignApi::class)
actual suspend fun notAnImage(): String {
    val path = temporaryPath()
    @Suppress("CAST_NEVER_SUCCEEDS")
    (("not an image" as NSString)).writeToFile(path, atomically = true, encoding = 4u, error = null)
    return path
}

actual val tellsUnreadableFromUndecodable: Boolean = true

private fun temporaryPath() = NSTemporaryDirectory() + "media-image-test-" + NSUUID().UUIDString + ".png"

@OptIn(ExperimentalForeignApi::class)
actual suspend fun twoToneImage(width: Int, height: Int, jpeg: Boolean): ByteArray {
    val context = CGBitmapContextCreate(
        null, width.toULong(), height.toULong(), 8u, 0u, CGColorSpaceCreateDeviceRGB(),
        CGImageAlphaInfo.kCGImageAlphaNoneSkipLast.value,
    )
    CGContextSetRGBFillColor(context, 1.0, 0.0, 0.0, 1.0)
    CGContextFillRect(context, CGRectMake(0.0, 0.0, width / 2.0, height.toDouble()))
    CGContextSetRGBFillColor(context, 0.0, 0.0, 1.0, 1.0)
    CGContextFillRect(context, CGRectMake(width / 2.0, 0.0, width / 2.0, height.toDouble()))
    val image = UIImage.imageWithCGImage(CGBitmapContextCreateImage(context))
    val data = (if (jpeg) platform.UIKit.UIImageJPEGRepresentation(image, 0.95) else UIImagePNGRepresentation(image))!!
    return ByteArray(data.length.toInt()).also { bytes ->
        bytes.usePinned { platform.posix.memcpy(it.addressOf(0), data.bytes, data.length) }
    }
}

@OptIn(ExperimentalForeignApi::class, kotlinx.cinterop.BetaInteropApi::class)
actual suspend fun writeSource(bytes: ByteArray, jpeg: Boolean): String {
    val path = NSTemporaryDirectory() + "media-image-source-" + NSUUID().UUIDString + if (jpeg) ".jpg" else ".png"
    bytes.usePinned { platform.Foundation.NSData.create(bytes = it.addressOf(0), length = bytes.size.toULong()).writeToFile(path, atomically = true) }
    return path
}

@OptIn(ExperimentalForeignApi::class, kotlinx.cinterop.BetaInteropApi::class)
actual fun pixelAt(encoded: ByteArray, x: Int, y: Int): Int? {
    val data = encoded.usePinned { platform.Foundation.NSData.create(bytes = it.addressOf(0), length = encoded.size.toULong()) }
    val image = UIImage.imageWithData(data)?.CGImage ?: return null
    val width = platform.CoreGraphics.CGImageGetWidth(image).toInt()
    val height = platform.CoreGraphics.CGImageGetHeight(image).toInt()
    val pixels = ByteArray(width * height * 4)
    pixels.usePinned { pinned ->
        val context = CGBitmapContextCreate(
            pinned.addressOf(0), width.toULong(), height.toULong(), 8u, (width * 4).toULong(), CGColorSpaceCreateDeviceRGB(),
            CGImageAlphaInfo.kCGImageAlphaPremultipliedLast.value,
        )
        platform.CoreGraphics.CGContextDrawImage(context, CGRectMake(0.0, 0.0, width.toDouble(), height.toDouble()), image)
    }
    // A bitmap context's memory starts at the image's top row, so (x, y) from the top is at y * width + x.
    val i = (y * width + x) * 4
    fun b(k: Int) = pixels[i + k].toInt() and 0xff
    return (b(3) shl 24) or (b(0) shl 16) or (b(1) shl 8) or b(2)
}
