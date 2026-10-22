package com.oddlyclever.media.image

import kotlinx.coroutines.suspendCancellableCoroutine
import org.w3c.dom.HTMLCanvasElement
import org.w3c.files.Blob
import kotlin.coroutines.resume

internal actual suspend fun HTMLCanvasElement.encodeToBlob(type: String, quality: Double): Blob? =
    suspendCancellableCoroutine { continuation ->
        toBlob(
            _callback = { blob -> continuation.resume(blob); null },
            type = type,
            quality = quality.toJsNumber()
        )
    }
