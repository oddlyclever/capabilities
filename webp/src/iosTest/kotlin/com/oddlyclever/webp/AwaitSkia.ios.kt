package com.oddlyclever.webp

actual suspend fun awaitSkia() = Unit

actual fun decodeForTest(webp: ByteArray): RgbaPixels? = null
