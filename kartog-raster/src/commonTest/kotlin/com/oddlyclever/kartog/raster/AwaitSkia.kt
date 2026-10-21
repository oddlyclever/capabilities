package com.oddlyclever.kartog.raster

/**
 * Waits until Skia can decode and draw. On JS its wasm module loads in the background after the page starts, and a
 * test that decodes an image before then fails on a missing function; everywhere else it is ready at once.
 */
expect suspend fun awaitSkia()
