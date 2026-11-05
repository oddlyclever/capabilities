package com.oddlyclever.sundries.text

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.await
import org.w3c.dom.HTMLElement
import org.w3c.dom.HTMLTextAreaElement

@Composable
internal actual fun rememberClipboardWriter(): ClipboardWriter = remember { ClipboardWriter(::writeToClipboard) }

// The clipboard API exists only in a secure context: https, or localhost. A page served over plain http, such as an
// app on a private network, has no navigator.clipboard at all, so calling it throws, and the text is copied the older
// way instead, from a selected text area, which browsers still allow in answer to a click. The same fallback covers
// a secure page whose write is refused, such as when it does not have focus.
private suspend fun writeToClipboard(text: String): Boolean {
    try {
        window.navigator.clipboard.writeText(text).await()
        return true
    } catch (e: CancellationException) {
        throw e
    } catch (e: Throwable) {
        return copyBySelecting(text)
    }
}

private fun copyBySelecting(text: String): Boolean {
    val focused = document.activeElement as? HTMLElement
    val field = document.createElement("textarea") as HTMLTextAreaElement
    field.value = text
    field.setAttribute("readonly", "")
    field.style.position = "fixed"
    field.style.top = "0"
    field.style.opacity = "0"
    val body = document.body ?: return false
    body.appendChild(field)
    return try {
        field.select()
        document.execCommand("copy")
    } catch (e: Throwable) {
        false
    } finally {
        body.removeChild(field)
        // A canvas app keeps the keyboard only if focus goes back where it was.
        focused?.focus()
    }
}
