package com.oddlyclever.media.webp

import android.graphics.BitmapFactory

actual suspend fun awaitSkia() = Unit

// Android's own decoder, asked not to premultiply, so translucent pixels keep their colour.
actual fun decodeForTest(webp: ByteArray): RgbaPixels? {
    val options = BitmapFactory.Options().apply { inPremultiplied = false }
    return BitmapFactory.decodeByteArray(webp, 0, webp.size, options)?.toRgbaPixels()
}
