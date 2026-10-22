package com.oddlyclever.media.image

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.util.Log
import com.oddlyclever.media.webp.encodeToWebp
import com.oddlyclever.platform.AppContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import kotlin.math.max

/**
 * Decodes with `BitmapFactory`, subsampling a large image as it decodes so a 50-megapixel photo never sits in memory
 * whole, and encodes WebP with media-webp and JPEG and PNG with `Bitmap.compress`.
 *
 * [context] opens `content://` URIs; it defaults to the application context the platform library captured.
 */
actual class ImageProcessor(private val context: Context) {

    actual constructor() : this(AppContext.get())

    private fun openSafeInputStream(uri: Uri): InputStream? {
        return try {
            if (uri.scheme == "file") {
                uri.path?.let { FileInputStream(File(it)) }
            } else {
                context.contentResolver.openInputStream(uri)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open input stream for $uri (ENXIO/Security)", e)
            null
        }
    }

    actual suspend fun compressImage(
        uri: String,
        format: ImageFormat,
        maxDimension: Int,
        quality: Int,
    ): Result<ProcessedImage> = withContext(Dispatchers.IO) {
        // The whole body is guarded: decode and native encode can throw (OOM in
        // particular), and previously those propagated to the caller on Android while
        // the JVM target swallowed them. Both now report through Result.
        try {
            val parsedUri = if (uri.startsWith("/")) {
                Uri.fromFile(File(uri))
            } else {
                Uri.parse(uri)
            }

            // 1. Decode bounds
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }

            val inputStream1 = openSafeInputStream(parsedUri)
                ?: return@withContext Result.failure(ImageProcessingException.SourceUnreadable(uri))
            inputStream1.use { BitmapFactory.decodeStream(it, null, options) }

            val originalWidth = options.outWidth
            val originalHeight = options.outHeight

            if (originalWidth <= 0 || originalHeight <= 0) {
                return@withContext Result.failure(ImageProcessingException.DecodeFailed(uri))
            }

            // 2. Calculate inSampleSize
            var inSampleSize = 1
            if (originalHeight > maxDimension || originalWidth > maxDimension) {
                val halfHeight: Int = originalHeight / 2
                val halfWidth: Int = originalWidth / 2
                while (halfHeight / inSampleSize >= maxDimension && halfWidth / inSampleSize >= maxDimension) {
                    inSampleSize *= 2
                }
            }

            // 3. Decode sub-sampled image
            options.inJustDecodeBounds = false
            options.inSampleSize = inSampleSize

            val inputStream2 = openSafeInputStream(parsedUri)
                ?: return@withContext Result.failure(ImageProcessingException.SourceUnreadable(uri))
            val sampledBitmap = inputStream2.use { BitmapFactory.decodeStream(it, null, options) }
                ?: return@withContext Result.failure(ImageProcessingException.DecodeFailed(uri))

            // 4. Scale precisely
            val finalBitmap = if (sampledBitmap.width > maxDimension || sampledBitmap.height > maxDimension) {
                val scale = maxDimension.toFloat() / max(sampledBitmap.width, sampledBitmap.height).toFloat()
                val matrix = Matrix().apply { postScale(scale, scale) }
                val scaled = Bitmap.createBitmap(sampledBitmap, 0, 0, sampledBitmap.width, sampledBitmap.height, matrix, true)
                if (scaled != sampledBitmap) sampledBitmap.recycle()
                scaled
            } else {
                sampledBitmap
            }

            if (!format.supportsAlpha && finalBitmap.hasNonOpaquePixels()) {
                finalBitmap.recycle()
                return@withContext Result.failure(ImageProcessingException.AlphaNotRepresentable(format))
            }

            // 5. Encode
            val width = finalBitmap.width
            val height = finalBitmap.height
            val bytes = try {
                when (format) {
                    ImageFormat.Webp -> finalBitmap.encodeToWebp(quality)
                    ImageFormat.Jpeg -> finalBitmap.compressToBytes(Bitmap.CompressFormat.JPEG, quality)
                    ImageFormat.Png -> finalBitmap.compressToBytes(Bitmap.CompressFormat.PNG, 100)
                }
            } finally {
                finalBitmap.recycle()
            }

            if (bytes == null || bytes.isEmpty()) {
                Log.e(TAG, "Encoder produced no data for ${format.contentType}")
                return@withContext Result.failure(ImageProcessingException.EncodeFailed(format))
            }

            Result.success(
                ProcessedImage(
                    bytes = bytes,
                    contentType = format.contentType,
                    width = width,
                    height = height,
                )
            )
        } catch (e: Throwable) {
            Log.e(TAG, "Image processing failed for $uri as ${format.contentType}", e)
            Result.failure(ImageProcessingException.EncodeFailed(format, e))
        }
    }

    private fun Bitmap.compressToBytes(format: Bitmap.CompressFormat, quality: Int): ByteArray? {
        val out = ByteArrayOutputStream()
        return if (compress(format, quality, out)) out.toByteArray() else null
    }

    /**
     * True if any pixel is not fully opaque.
     *
     * [Bitmap.hasAlpha] only reports whether an alpha channel exists, not whether it is
     * used, and images decoded to ARGB_8888 almost always report true. Treating that as
     * "has transparency" would rule out JPEG for ordinary opaque photos, so this scans
     * for real non-opaque pixels. Scanning one row at a time keeps the allocation at a
     * few KB rather than materialising the whole bitmap as an IntArray.
     */
    private fun Bitmap.hasNonOpaquePixels(): Boolean {
        if (!hasAlpha()) return false
        val row = IntArray(width)
        for (y in 0 until height) {
            getPixels(row, 0, width, 0, y, width, 1)
            for (pixel in row) {
                if ((pixel ushr 24) != 0xFF) return true
            }
        }
        return false
    }

    private companion object {
        const val TAG = "ImageProcessor"
    }
}
