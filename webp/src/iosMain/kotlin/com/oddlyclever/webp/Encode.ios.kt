package com.oddlyclever.webp

import com.oddlyclever.webp.libwebp.WebPEncodeLosslessRGBA
import com.oddlyclever.webp.libwebp.WebPEncodeRGBA
import com.oddlyclever.webp.libwebp.WebPFree
import kotlinx.cinterop.CPointerVar
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.UByteVar
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.readBytes
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.usePinned
import kotlinx.cinterop.value
import platform.CoreGraphics.CGBitmapContextCreate
import platform.CoreGraphics.CGColorSpaceCreateWithName
import platform.CoreGraphics.CGColorSpaceRelease
import platform.CoreGraphics.CGContextDrawImage
import platform.CoreGraphics.CGContextRelease
import platform.CoreGraphics.CGImageAlphaInfo
import platform.CoreGraphics.CGImageGetHeight
import platform.CoreGraphics.CGImageGetWidth
import platform.CoreGraphics.CGImageRef
import platform.CoreGraphics.CGRectMake
import platform.CoreGraphics.kCGBitmapByteOrder32Big
import platform.CoreGraphics.kCGColorSpaceSRGB
import platform.UIKit.UIGraphicsImageRenderer
import platform.UIKit.UIGraphicsImageRendererFormat
import platform.UIKit.UIImage
import platform.UIKit.UIImageOrientation

@OptIn(ExperimentalForeignApi::class)
internal actual fun encodeRgba(pixels: RgbaPixels, quality: Int, lossless: Boolean): ByteArray? = memScoped {
    val output = alloc<CPointerVar<UByteVar>>()
    val size = pixels.bytes.usePinned { pinned ->
        val rgba = pinned.addressOf(0).reinterpret<UByteVar>()
        if (lossless) {
            WebPEncodeLosslessRGBA(rgba, pixels.width, pixels.height, pixels.rowBytes, output.ptr)
        } else {
            WebPEncodeRGBA(rgba, pixels.width, pixels.height, pixels.rowBytes, quality.toFloat(), output.ptr)
        }
    }
    val data = output.value
    try {
        if (size == 0uL || data == null) null else data.readBytes(size.toInt())
    } finally {
        WebPFree(data)
    }
}

/**
 * This image's pixels as straight-alpha RGBA in sRGB, at its full pixel size and turned upright: a photo whose
 * orientation says it was taken sideways comes out the way it is shown.
 *
 * @throws WebpException for an image with no bitmap behind it, such as one made from a `CIImage`.
 */
@OptIn(ExperimentalForeignApi::class)
fun UIImage.toRgbaPixels(): RgbaPixels {
    val upright = if (imageOrientation == UIImageOrientation.UIImageOrientationUp) this else redrawnUpright()
    val image = upright.CGImage ?: throw WebpException("a UIImage with no bitmap behind it")
    return image.toRgbaPixels()
}

/**
 * This image as a WebP, through the libwebp built into this library.
 *
 * @see encodeToWebp
 */
fun UIImage.encodeToWebp(
    quality: Int = DEFAULT_WEBP_QUALITY,
    compression: WebpCompression = WebpCompression.Lossy,
    metadata: WebpMetadata? = null,
): ByteArray = toRgbaPixels().encodeToWebp(quality, compression, metadata)

@OptIn(ExperimentalForeignApi::class)
private fun UIImage.redrawnUpright(): UIImage {
    val format = UIGraphicsImageRendererFormat.defaultFormat().apply { scale = this@redrawnUpright.scale }
    return UIGraphicsImageRenderer(size, format).imageWithActions { drawAtPoint(platform.CoreGraphics.CGPointMake(0.0, 0.0)) }
}

@OptIn(ExperimentalForeignApi::class)
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
    unpremultiply(bytes)
    return RgbaPixels(width, height, bytes)
}
