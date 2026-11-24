package com.oddlyclever.media.webp

import com.oddlyclever.media.webp.libwebp.WebPEncodeLosslessRGBA
import com.oddlyclever.media.webp.libwebp.WebPEncodeRGBA
import com.oddlyclever.media.webp.libwebp.WebPFree
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

// Every native target encodes with the vendored libwebp through cinterop: iOS, macOS, Linux and Windows alike.
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
