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

@OptIn(kotlin.io.encoding.ExperimentalEncodingApi::class)
actual suspend fun twoToneImage(width: Int, height: Int, jpeg: Boolean): ByteArray {
    val canvas = document.createElement("canvas") as HTMLCanvasElement
    canvas.width = width
    canvas.height = height
    val context = canvas.getContext("2d") as CanvasRenderingContext2D
    context.fillStyle = "#ff0000".toJsString()
    context.fillRect(0.0, 0.0, width / 2.0, height.toDouble())
    context.fillStyle = "#0000ff".toJsString()
    context.fillRect(width / 2.0, 0.0, width / 2.0, height.toDouble())
    val url = canvas.toDataURL(if (jpeg) "image/jpeg" else "image/png")
    return kotlin.io.encoding.Base64.decode(url.substringAfter(","))
}

@OptIn(kotlin.io.encoding.ExperimentalEncodingApi::class)
actual suspend fun writeSource(bytes: ByteArray, jpeg: Boolean): String =
    "data:image/${if (jpeg) "jpeg" else "png"};base64," + kotlin.io.encoding.Base64.encode(bytes)

// Decoding on the web is asynchronous; the size checks still run there, the pixel checks do not.
actual fun pixelAt(encoded: ByteArray, x: Int, y: Int): Int? = null
