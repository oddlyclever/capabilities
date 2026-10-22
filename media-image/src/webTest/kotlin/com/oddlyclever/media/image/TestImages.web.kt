package com.oddlyclever.media.image

import kotlinx.browser.document
import org.w3c.dom.CanvasRenderingContext2D
import org.w3c.dom.HTMLCanvasElement

actual suspend fun writeTestImage(width: Int, height: Int, alpha: Int): String {
    val canvas = document.createElement("canvas") as HTMLCanvasElement
    canvas.width = width
    canvas.height = height
    val context = canvas.getContext("2d") as CanvasRenderingContext2D
    for (x in 0 until width) {
        context.fillStyle = "rgba(${x * 255 / width}, 128, 128, ${alpha / 255.0})".toJsString()
        context.fillRect(x.toDouble(), 0.0, 1.0, height.toDouble())
    }
    return canvas.toDataURL("image/png")
}

actual fun missingSource(): String = "blob:http://localhost/media-image-missing"

actual suspend fun notAnImage(): String = "data:image/png;base64,bm90IGFuIGltYWdl"

actual val tellsUnreadableFromUndecodable: Boolean = false
