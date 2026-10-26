package com.oddlyclever.media.image

import org.w3c.dom.HTMLCanvasElement
import org.w3c.files.Blob

/**
 * Wraps `HTMLCanvasElement.toBlob`, whose Kotlin binding differs between the js and wasmJs
 * targets: on wasmJs the callback returns `JsAny?` and the quality argument is `JsAny?`
 * rather than `Double`. Everything else in the web ImageProcessor is shared.
 *
 * Returns null when the browser hands back no blob. Throws on a tainted (cross-origin)
 * canvas; the caller converts that to a failure.
 */
internal expect suspend fun HTMLCanvasElement.encodeToBlob(type: String, quality: Double): Blob?

/**
 * The bytes at [uri], fetched as the page would fetch them, or null when they cannot be had. `fetch`'s Kotlin binding
 * also differs between js and wasmJs.
 */
internal expect suspend fun fetchBytes(uri: String): ByteArray?
