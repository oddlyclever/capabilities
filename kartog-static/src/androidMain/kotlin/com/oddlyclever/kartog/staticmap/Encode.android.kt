package com.oddlyclever.kartog.staticmap

import android.graphics.Bitmap
import android.os.Build
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import java.io.ByteArrayOutputStream

actual fun ImageBitmap.encodeToPng(): ByteArray = compress(Bitmap.CompressFormat.PNG, 100)

actual fun ImageBitmap.encodeToWebp(quality: Int): ByteArray = compress(
    // WEBP_LOSSY arrived in API 30; before it, WEBP at a quality below 100 is the same lossy encoding.
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) Bitmap.CompressFormat.WEBP_LOSSY
    else @Suppress("DEPRECATION") Bitmap.CompressFormat.WEBP,
    quality.coerceIn(0, 99),
)

private fun ImageBitmap.compress(format: Bitmap.CompressFormat, quality: Int): ByteArray {
    val out = ByteArrayOutputStream()
    check(asAndroidBitmap().compress(format, quality, out)) { "Android could not encode the image as $format." }
    return out.toByteArray()
}
