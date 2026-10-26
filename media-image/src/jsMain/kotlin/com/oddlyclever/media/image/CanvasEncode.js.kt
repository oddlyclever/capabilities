package com.oddlyclever.media.image

import kotlinx.coroutines.suspendCancellableCoroutine
import org.w3c.dom.HTMLCanvasElement
import org.w3c.files.Blob
import kotlin.coroutines.resume
import kotlinx.coroutines.await

internal actual suspend fun HTMLCanvasElement.encodeToBlob(type: String, quality: Double): Blob? =
    suspendCancellableCoroutine { continuation ->
        toBlob(
            _callback = { blob -> continuation.resume(blob) },
            type = type,
            quality = quality
        )
    }

internal actual suspend fun fetchBytes(uri: String): ByteArray? = try {
    // Through dynamic: the typed binding's RequestInit() fills every option with undefined, which fetch can refuse.
    val response = kotlinx.browser.window.asDynamic().fetch(uri).unsafeCast<kotlin.js.Promise<org.w3c.fetch.Response>>().await()
    if (!response.ok) null else org.khronos.webgl.Int8Array(response.arrayBuffer().await()).unsafeCast<ByteArray>()
} catch (e: Throwable) {
    null
}
