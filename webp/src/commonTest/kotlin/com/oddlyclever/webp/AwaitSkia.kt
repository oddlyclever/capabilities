package com.oddlyclever.webp

/** Waits until Skia can be used: at once except on the web, where its WebAssembly loads asynchronously. */
expect suspend fun awaitSkia()

/** [webp] decoded to straight-alpha RGBA by Skia, or null on a target this module has no decoder for. */
expect fun decodeForTest(webp: ByteArray): RgbaPixels?
