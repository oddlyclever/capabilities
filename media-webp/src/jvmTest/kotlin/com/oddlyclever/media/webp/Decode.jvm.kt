package com.oddlyclever.media.webp

import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.ColorAlphaType
import org.jetbrains.skia.ColorType
import org.jetbrains.skia.Image
import org.jetbrains.skia.ImageInfo

actual fun decodeForTest(webp: ByteArray): RgbaPixels? {
    val image = Image.makeFromEncoded(webp)
    val bitmap = Bitmap()
    bitmap.allocPixels(ImageInfo(image.width, image.height, ColorType.RGBA_8888, ColorAlphaType.UNPREMUL))
    check(image.readPixels(bitmap, 0, 0)) { "Skia could not read the decoded pixels" }
    return RgbaPixels(image.width, image.height, checkNotNull(bitmap.readPixels()))
}
