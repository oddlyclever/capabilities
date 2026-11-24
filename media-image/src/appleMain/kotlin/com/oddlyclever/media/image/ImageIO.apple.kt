package com.oddlyclever.media.image

import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.CoreFoundation.CFDataRef
import platform.CoreFoundation.CFDictionaryRef
import platform.CoreFoundation.CFMutableDataRef
import platform.CoreFoundation.CFRelease
import platform.CoreFoundation.CFRetain
import platform.CoreFoundation.CFStringRef
import platform.CoreGraphics.CGImageRef
import platform.Foundation.CFBridgingRelease
import platform.Foundation.CFBridgingRetain
import platform.Foundation.NSData
import platform.Foundation.NSMutableData
import platform.Foundation.create
import platform.ImageIO.CGImageDestinationAddImage
import platform.ImageIO.CGImageDestinationCreateWithData
import platform.ImageIO.CGImageDestinationFinalize
import platform.ImageIO.CGImageSourceCreateImageAtIndex
import platform.ImageIO.CGImageSourceCreateWithData
import platform.ImageIO.kCGImageDestinationLossyCompressionQuality
import platform.posix.memcpy

// JPEG and PNG written with ImageIO rather than UIKit, which macOS does not have: the same Apple encoders UIKit's
// UIImageJPEGRepresentation and UIImagePNGRepresentation call, so iOS and macOS write the same files.

/** The uniform type identifier ImageIO writes a JPEG as. */
internal const val JpegType = "public.jpeg"

/** The uniform type identifier ImageIO writes a PNG as. */
internal const val PngType = "public.png"

/**
 * This image encoded as [type], a uniform type identifier such as [JpegType], at [quality] from 0 to 1 for a lossy
 * type, or null when ImageIO cannot write it.
 */
@OptIn(ExperimentalForeignApi::class)
internal fun CGImageRef.encodeWithImageIO(type: String, quality: Double? = null): ByteArray? {
    val data = NSMutableData()
    val dataRef = CFBridgingRetain(data)
    val typeRef = CFBridgingRetain(type)
    val destination = CGImageDestinationCreateWithData(dataRef as CFMutableDataRef?, typeRef as CFStringRef?, 1u, null)
    CFRelease(typeRef)
    CFRelease(dataRef)
    destination ?: return null
    // Keyed by the constant's string value: a Core Foundation pointer is not a key an NSDictionary built from Kotlin
    // can hold.
    val options = quality?.let { CFBridgingRetain(mapOf(key(kCGImageDestinationLossyCompressionQuality) to it)) }
    CGImageDestinationAddImage(destination, this, options as CFDictionaryRef?)
    options?.let { CFRelease(it) }
    val written = CGImageDestinationFinalize(destination)
    CFRelease(destination)
    return if (written) data.toByteArray() else null
}

/** The first image in [encoded], decoded by ImageIO, or null when it is not an image ImageIO reads. The caller releases it. */
@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
internal fun decodeWithImageIO(encoded: ByteArray): CGImageRef? {
    if (encoded.isEmpty()) return null
    val data = encoded.usePinned { NSData.create(bytes = it.addressOf(0), length = encoded.size.toULong()) }
    val dataRef = CFBridgingRetain(data)
    val source = CGImageSourceCreateWithData(dataRef as CFDataRef?, null)
    CFRelease(dataRef)
    source ?: return null
    val image = CGImageSourceCreateImageAtIndex(source, 0u, null)
    CFRelease(source)
    return image
}

/** A Core Foundation string constant as the key it is in a bridged dictionary. */
@OptIn(ExperimentalForeignApi::class)
internal fun key(constant: CFStringRef?): String = CFBridgingRelease(CFRetain(constant)) as String

@OptIn(ExperimentalForeignApi::class)
internal fun NSData.toByteArray(): ByteArray {
    val bytes = ByteArray(length.toInt())
    if (bytes.isNotEmpty()) bytes.usePinned { memcpy(it.addressOf(0), this.bytes, length) }
    return bytes
}
