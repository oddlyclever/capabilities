package com.oddlyclever.media.webp

actual suspend fun awaitSkia() = Unit

actual fun decodeForTest(webp: ByteArray): RgbaPixels? = null
