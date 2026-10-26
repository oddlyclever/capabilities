package com.oddlyclever.media.image

import kotlinx.coroutines.suspendCancellableCoroutine
import org.w3c.dom.HTMLCanvasElement
import org.w3c.files.Blob
import kotlin.coroutines.resume
import kotlinx.coroutines.await
import org.khronos.webgl.get

internal actual suspend fun HTMLCanvasElement.encodeToBlob(type: String, quality: Double): Blob? =
    suspendCancellableCoroutine { continuation ->
        toBlob(
            _callback = { blob -> continuation.resume(blob); null },
            type = type,
            quality = quality.toJsNumber()
        )
    }

internal actual suspend fun fetchBytes(uri: String): ByteArray? = try {
    val response = kotlinx.browser.window.fetch(uri).await<org.w3c.fetch.Response>()
    if (!response.ok) {
        null
    } else {
        val array = org.khronos.webgl.Int8Array(response.arrayBuffer().await<org.khronos.webgl.ArrayBuffer>())
        ByteArray(array.length) { array[it] }
    }
} catch (e: Throwable) {
    null
}
