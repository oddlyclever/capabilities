package com.oddlyclever.sundries.text

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** How long [Copier.state] says [CopyState.Copied] before it goes back to [CopyState.Ready], unless told otherwise. */
const val COPIED_SHOWN_MILLIS: Long = 2_500L

/** What the last copy did, for a control to show and announce. */
enum class CopyState {
    /** Nothing copied lately: the control offers to copy. */
    Ready,

    /** The text is on the clipboard. Lasts a moment, then goes back to [Ready]. */
    Copied,

    /**
     * The platform would not take the text, and the person has to copy it themselves. Lasts until the next copy, so a
     * control can go on explaining what to do.
     */
    Refused,
}

/**
 * Copies text to the clipboard and remembers, for a moment, how that went.
 *
 * It is the shared part of every copy control, so an app's own copy button, a copyable id and an error dialog's Copy
 * all copy the same way, and say so the same way, while each looks like itself. [TextCopy] is one control built on
 * it. Get one with [rememberCopier]; there is no public constructor, because the clipboard belongs to the platform
 * the composition runs on.
 *
 * On the web a page served over plain http has no clipboard API, so a copy there falls back to the older way a
 * browser still allows from a click. Where neither works, or the platform has no clipboard to give, the state is
 * [CopyState.Refused] and nothing is thrown.
 */
@Stable
class Copier internal constructor(
    private val clipboard: ClipboardWriter,
    private val scope: CoroutineScope,
    private val shownMillis: Long,
) {
    /** What the last copy did. Observable: a composable that reads it recomposes when it changes. */
    var state: CopyState by mutableStateOf(CopyState.Ready)
        private set

    private var running: Job? = null

    /**
     * Puts [text] on the clipboard, then sets [state]. Call it from a click: some browsers copy only in answer to one.
     * A second copy before the first has settled replaces it.
     */
    fun copy(text: String) {
        running?.cancel()
        running = scope.launch {
            val copied = try {
                clipboard.write(text)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                false
            }
            if (!copied) {
                state = CopyState.Refused
                return@launch
            }
            state = CopyState.Copied
            delay(shownMillis)
            state = CopyState.Ready
        }
    }
}

/**
 * A [Copier] for this composition, writing to this platform's clipboard. Its [Copier.state] says
 * [CopyState.Copied] for [shownMillis] after each copy.
 */
@Composable
fun rememberCopier(shownMillis: Long = COPIED_SHOWN_MILLIS): Copier {
    val clipboard = rememberClipboardWriter()
    val scope = rememberCoroutineScope()
    return remember(clipboard, scope, shownMillis) { Copier(clipboard, scope, shownMillis) }
}

/** Puts text on one platform's clipboard: true when it is there, false when the platform would not take it. */
internal fun interface ClipboardWriter {
    suspend fun write(text: String): Boolean
}

/** This platform's clipboard, for the composition that asks. */
@Composable
internal expect fun rememberClipboardWriter(): ClipboardWriter
