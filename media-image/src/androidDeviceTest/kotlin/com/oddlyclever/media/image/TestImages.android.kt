package com.oddlyclever.media.image

import android.graphics.Bitmap
import android.graphics.Color
import com.oddlyclever.platform.AppContext
import java.io.File

actual suspend fun awaitSkia() = Unit

actual suspend fun writeTestImage(width: Int, height: Int, alpha: Int): String {
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    for (x in 0 until width) for (y in 0 until height) {
        bitmap.setPixel(x, y, Color.argb(alpha, x * 255 / width, y * 255 / height, 128))
    }
    val file = File.createTempFile("media-image-test-", ".png", AppContext.get().cacheDir)
    file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    return file.absolutePath
}

actual fun missingSource(): String = File(AppContext.get().cacheDir, "media-image-missing.png").absolutePath

actual suspend fun notAnImage(): String =
    File.createTempFile("media-image-not-", ".png", AppContext.get().cacheDir).apply { writeText("not an image") }.absolutePath

actual val tellsUnreadableFromUndecodable: Boolean = true

actual suspend fun twoToneImage(width: Int, height: Int, jpeg: Boolean): ByteArray {
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    for (x in 0 until width) for (y in 0 until height) bitmap.setPixel(x, y, if (x < width / 2) Color.RED else Color.BLUE)
    return java.io.ByteArrayOutputStream().also {
        bitmap.compress(if (jpeg) Bitmap.CompressFormat.JPEG else Bitmap.CompressFormat.PNG, 95, it)
    }.toByteArray()
}

actual suspend fun writeSource(bytes: ByteArray, jpeg: Boolean): String =
    File.createTempFile("media-image-source-", if (jpeg) ".jpg" else ".png", AppContext.get().cacheDir)
        .apply { writeBytes(bytes) }.absolutePath

actual fun pixelAt(encoded: ByteArray, x: Int, y: Int): Int? =
    android.graphics.BitmapFactory.decodeByteArray(encoded, 0, encoded.size)?.getPixel(x, y)
