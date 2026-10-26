package com.oddlyclever.media.image

import com.oddlyclever.media.webp.RgbaPixels
import com.oddlyclever.media.webp.encodeToWebp
import kotlinx.browser.document
import kotlinx.coroutines.suspendCancellableCoroutine
import org.khronos.webgl.ArrayBuffer
import org.khronos.webgl.Int8Array
import org.khronos.webgl.get
import org.w3c.dom.CanvasRenderingContext2D
import org.w3c.dom.HTMLCanvasElement
import org.w3c.dom.HTMLImageElement
import org.w3c.files.Blob
import org.w3c.files.FileReader
import kotlin.coroutines.resume
import kotlin.math.roundToInt

/**
 * Decodes with an `<img>`, resizes on an offscreen `<canvas>`, and encodes WebP with media-webp and JPEG and PNG with
 * the canvas.
 */
actual class ImageProcessor {

    /**
     * Downsizes and encodes via an offscreen `<canvas>`.
     *
     * [uri] must be something an `<img>` can load: a `blob:`, `data:`, or same-origin URL.
     * A cross-origin URL taints the canvas, which makes both the alpha check and `toBlob`
     * throw a security error.
     */
    actual suspend fun compressImage(
        uri: String,
        format: ImageFormat,
        maxDimension: Int,
        quality: Int,
        keepMetadata: Boolean,
    ): Result<ProcessedImage> {
        val image = loadImage(uri)
            ?: return Result.failure(ImageProcessingException.SourceUnreadable(uri))

        val srcWidth = image.naturalWidth
        val srcHeight = image.naturalHeight
        if (srcWidth <= 0 || srcHeight <= 0) {
            return Result.failure(ImageProcessingException.DecodeFailed(uri))
        }

        // Fit inside the box while preserving aspect ratio; never upscale.
        val scale = minOf(1.0, maxDimension.toDouble() / maxOf(srcWidth, srcHeight).toDouble())
        val width = (srcWidth * scale).roundToInt().coerceAtLeast(1)
        val height = (srcHeight * scale).roundToInt().coerceAtLeast(1)

        val canvas = document.createElement("canvas") as HTMLCanvasElement
        canvas.width = width
        canvas.height = height

        val context = canvas.getContext("2d") as? CanvasRenderingContext2D
            ?: return Result.failure(ImageProcessingException.EncodeFailed(format))

        context.drawImage(image, 0.0, 0.0, width.toDouble(), height.toDouble())

        if (!format.supportsAlpha && context.hasNonOpaquePixels(width, height)) {
            return Result.failure(ImageProcessingException.AlphaNotRepresentable(format))
        }

        // The browser has already stood the photo upright: <img> applies the EXIF orientation. Only the metadata
        // needs the source's own bytes, so they are read only when it is kept.
        val metadata = if (keepMetadata) fetchBytes(uri)?.let { SourceExifReader.read(it).metadata } else null

        if (format == ImageFormat.Webp) return encodeWebp(context, width, height, quality).map { it.copy(bytes = embedMetadata(it.bytes, format, metadata)) }

        val blob = canvas.encode(format, quality)
            ?: return Result.failure(ImageProcessingException.EncodeFailed(format))

        // toBlob silently falls back to image/png when the requested type is unsupported,
        // so verify what came back instead of trusting the request. Reporting the wrong
        // contentType here is exactly the bug the typed contract exists to prevent.
        if (blob.type != format.contentType) {
            return Result.failure(ImageProcessingException.Unsupported(format, platform = "Web"))
        }

        val bytes = blob.toByteArray()
        if (bytes.isEmpty()) {
            return Result.failure(ImageProcessingException.EncodeFailed(format))
        }

        return Result.success(
            ProcessedImage(
                bytes = embedMetadata(bytes, format, metadata),
                contentType = format.contentType,
                width = width,
                height = height,
            )
        )
    }

    /**
     * WebP from the canvas's pixels through media-webp, not `toBlob`: Safari's canvas does not write WebP at all, and
     * the others write whatever their own libwebp makes of it. media-webp's lossy encoder is Skia's, so Skia's
     * WebAssembly must have loaded, as it has in a Compose app.
     */
    private fun encodeWebp(context: CanvasRenderingContext2D, width: Int, height: Int, quality: Int): Result<ProcessedImage> {
        val bytes = try {
            val data = context.getImageData(0.0, 0.0, width.toDouble(), height.toDouble()).data
            // getImageData gives straight-alpha RGBA, what RgbaPixels holds.
            RgbaPixels(width, height, ByteArray(data.length) { data[it] }).encodeToWebp(quality)
        } catch (e: Throwable) {
            // A tainted (cross-origin) canvas throws, as does an encoder that refuses the image.
            return Result.failure(ImageProcessingException.EncodeFailed(ImageFormat.Webp, e))
        }
        return Result.success(ProcessedImage(bytes, ImageFormat.Webp.contentType, width, height))
    }

    private suspend fun loadImage(uri: String): HTMLImageElement? =
        suspendCancellableCoroutine { continuation ->
            val image = document.createElement("img") as HTMLImageElement
            var settled = false
            // addEventListener rather than onload/onerror: the property setters have
            // different signatures on js and wasmJs, this does not.
            image.addEventListener("load", {
                if (!settled) { settled = true; continuation.resume(image) }
            })
            image.addEventListener("error", {
                if (!settled) { settled = true; continuation.resume(null) }
            })
            image.src = uri
        }

    private suspend fun HTMLCanvasElement.encode(format: ImageFormat, quality: Int): Blob? =
        try {
            encodeToBlob(format.contentType, quality / 100.0)
        } catch (e: Throwable) {
            // A tainted (cross-origin) canvas throws rather than returning null.
            null
        }

    /** True if any pixel is not fully opaque. */
    private fun CanvasRenderingContext2D.hasNonOpaquePixels(width: Int, height: Int): Boolean {
        val data = try {
            getImageData(0.0, 0.0, width.toDouble(), height.toDouble()).data
        } catch (e: Throwable) {
            // Tainted canvas: assume alpha may be present rather than risk flattening it.
            return true
        }
        // RGBA quads; alpha is every 4th byte. Masked rather than compared with -1: Kotlin/JS reads 255 from a
        // Uint8ClampedArray as an unnormalised byte holding 255, which is not equal to -1.
        var i = 3
        while (i < data.length) {
            if (data[i].toInt() and 0xFF != 0xFF) return true
            i += 4
        }
        return false
    }

    private suspend fun Blob.toByteArray(): ByteArray =
        suspendCancellableCoroutine { continuation ->
            val reader = FileReader()
            reader.onload = {
                val array = Int8Array(reader.result as ArrayBuffer)
                continuation.resume(ByteArray(array.length) { index -> array[index] })
                Unit
            }
            reader.onerror = {
                continuation.resume(ByteArray(0))
                Unit
            }
            reader.readAsArrayBuffer(this)
        }
}
