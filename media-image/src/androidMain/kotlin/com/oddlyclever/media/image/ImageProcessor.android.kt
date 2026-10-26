package com.oddlyclever.media.image

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.util.Log
import androidx.exifinterface.media.ExifInterface
import com.oddlyclever.media.webp.GpsLocation
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
 * whole, and encodes WebP with media-webp and JPEG and PNG with `Bitmap.compress`. The EXIF is read with ExifInterface,
 * which also reads HEIC, for the orientation the decoder ignores and for the metadata kept when asked.
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
        keepMetadata: Boolean,
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

            // 4. Scale precisely, and stand the photo upright in the same pass: BitmapFactory ignores the EXIF
            // orientation, so a photo taken with the phone held sideways decodes sideways.
            val exif = openSafeInputStream(parsedUri)?.use { readExif(it) } ?: SourceExif()
            val scale = if (sampledBitmap.width > maxDimension || sampledBitmap.height > maxDimension) {
                maxDimension.toFloat() / max(sampledBitmap.width, sampledBitmap.height).toFloat()
            } else {
                1f
            }
            val finalBitmap = if (scale != 1f || exif.orientation != 1) {
                val t = uprightTransform(exif.orientation, 0f, 0f)
                // createBitmap places the turned image at the origin itself, so only the linear part is needed.
                val matrix = Matrix().apply {
                    setValues(floatArrayOf(t[0], t[1], 0f, t[3], t[4], 0f, 0f, 0f, 1f))
                    preScale(scale, scale)
                }
                val turned = Bitmap.createBitmap(sampledBitmap, 0, 0, sampledBitmap.width, sampledBitmap.height, matrix, true)
                if (turned != sampledBitmap) sampledBitmap.recycle()
                turned
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
                    bytes = embedMetadata(bytes, format, if (keepMetadata) exif.metadata else null),
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

    /** The orientation, location and capture time ExifInterface finds; it reads JPEG, PNG, WebP and HEIC. */
    private fun readExif(stream: InputStream): SourceExif = try {
        val exif = ExifInterface(stream)
        val orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
        val location = exif.latLong?.let { (latitude, longitude) ->
            val altitude = if (exif.getAttribute(ExifInterface.TAG_GPS_ALTITUDE) != null) exif.getAltitude(0.0) else null
            if (latitude in -90.0..90.0 && longitude in -180.0..180.0) GpsLocation(latitude, longitude, altitude) else null
        }
        val gpsSeconds = exif.getAttribute(ExifInterface.TAG_GPS_TIMESTAMP)?.split(':')?.mapNotNull { it.toDoubleOrNull() }
            ?.takeIf { it.size == 3 }?.let { (h, m, s) -> h * 3600 + m * 60 + s }
        val capturedAt = exactCaptureTime(
            original = exif.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL),
            offset = exif.getAttribute(ExifInterface.TAG_OFFSET_TIME_ORIGINAL),
            gpsDate = exif.getAttribute(ExifInterface.TAG_GPS_DATESTAMP),
            gpsSeconds = gpsSeconds,
        )
        SourceExif(orientation.takeIf { it in 1..8 } ?: 1, location, capturedAt)
    } catch (e: Exception) {
        Log.w(TAG, "Could not read the image's EXIF; treating it as upright with none", e)
        SourceExif()
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
