package com.oddlyclever.media.image

import java.awt.Color
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO

actual suspend fun awaitSkia() = Unit

actual suspend fun writeTestImage(width: Int, height: Int, alpha: Int): String {
    val image = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
    for (x in 0 until width) for (y in 0 until height) {
        image.setRGB(x, y, Color(x * 255 / width, y * 255 / height, 128, alpha).rgb)
    }
    val file = File.createTempFile("media-image-test-", ".png").apply { deleteOnExit() }
    ImageIO.write(image, "png", file)
    return file.absolutePath
}

actual fun missingSource(): String = File(System.getProperty("java.io.tmpdir"), "media-image-missing.png").absolutePath

actual suspend fun notAnImage(): String =
    File.createTempFile("media-image-not-", ".png").apply { deleteOnExit(); writeText("not an image") }.absolutePath

actual val tellsUnreadableFromUndecodable: Boolean = true

actual suspend fun twoToneImage(width: Int, height: Int, jpeg: Boolean): ByteArray {
    val image = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
    for (x in 0 until width) for (y in 0 until height) image.setRGB(x, y, if (x < width / 2) 0xFF0000 else 0x0000FF)
    return java.io.ByteArrayOutputStream().also { ImageIO.write(image, if (jpeg) "jpg" else "png", it) }.toByteArray()
}

actual suspend fun writeSource(bytes: ByteArray, jpeg: Boolean): String =
    File.createTempFile("media-image-source-", if (jpeg) ".jpg" else ".png").apply { deleteOnExit(); writeBytes(bytes) }.absolutePath

actual fun pixelAt(encoded: ByteArray, x: Int, y: Int): Int? {
    val image = org.jetbrains.skia.Image.makeFromEncoded(encoded)
    val bitmap = org.jetbrains.skia.Bitmap().apply { allocN32Pixels(image.width, image.height) }
    image.readPixels(bitmap, 0, 0)
    return bitmap.getColor(x, y)
}
