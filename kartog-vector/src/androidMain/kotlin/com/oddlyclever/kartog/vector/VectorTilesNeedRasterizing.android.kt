package com.oddlyclever.kartog.vector

import android.os.Build

/**
 * True on the Pixel 10 family, whose Tensor G5 (PowerVR DXT-48-1536) Vulkan driver segfaults the render thread when
 * Compose draws complex paths. Matched on every name the platform gives the device, since each reports it differently:
 * the model says "Pixel 10", the board and hardware say "laguna" (the system on a chip), and the device and product
 * say "franklin" (the device family).
 */
actual fun vectorTilesNeedRasterizing(): Boolean {
    // The OpenGL renderer does not have the fault; only Vulkan does.
    if (hwuiRenderer == "skiagl") return false
    val model = Build.MODEL.orEmpty().lowercase()
    val board = Build.BOARD.orEmpty().lowercase()
    val hardware = Build.HARDWARE.orEmpty().lowercase()
    val device = Build.DEVICE.orEmpty().lowercase()
    val product = Build.PRODUCT.orEmpty().lowercase()
    return "pixel 10" in model || "laguna" in board || "laguna" in hardware ||
        "franklin" in device || "franklin" in product
}

/**
 * The renderer forced with `adb shell setprop debug.hwui.renderer`, or null. That is an Android system property, not
 * a Java one, so `System.getProperty` never sees it, and the API that reads it is hidden from apps; `getprop` is the
 * supported way to read it. Read once, since the renderer cannot change while the app runs.
 */
private val hwuiRenderer: String? by lazy {
    runCatching {
        val process = ProcessBuilder("getprop", "debug.hwui.renderer").redirectErrorStream(true).start()
        process.inputStream.bufferedReader().use { it.readText() }.trim().also { process.waitFor() }
    }.getOrNull()?.takeIf { it.isNotEmpty() }
}
