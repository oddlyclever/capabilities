package com.oddlyclever.media.image

import kotlinx.cinterop.ExperimentalForeignApi
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
