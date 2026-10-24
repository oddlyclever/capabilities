package com.oddlyclever.wayfinder.frame

import android.graphics.Bitmap
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import java.io.ByteArrayOutputStream

actual fun ImageBitmap.encodeToPng(): ByteArray = compress(Bitmap.CompressFormat.PNG, 100)

private fun ImageBitmap.compress(format: Bitmap.CompressFormat, quality: Int): ByteArray {
    val out = ByteArrayOutputStream()
    check(asAndroidBitmap().compress(format, quality, out)) { "Android could not encode the image as $format." }
    return out.toByteArray()
}
