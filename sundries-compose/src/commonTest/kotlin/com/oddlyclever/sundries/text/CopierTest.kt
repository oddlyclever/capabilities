package com.oddlyclever.sundries.text

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Pins what a copy reports: the text reaches the clipboard and the state says Copied for the time it was given, then
 * Ready; a clipboard that refuses, or throws, leaves it Refused until the next copy; and a second copy replaces the
 * first.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class CopierTest {

    private class Clipboard(var answer: () -> Boolean = { true }) : ClipboardWriter {
        val written = mutableListOf<String>()
        override suspend fun write(text: String): Boolean {
            written += text
            return answer()
        }
    }

    @Test
    fun aCopySaysCopiedForItsTimeThenReady() = runTest {
        val clipboard = Clipboard()
        val copier = Copier(clipboard, backgroundScope, shownMillis = 1_000)
        copier.copy("abc")
        runCurrent()
        assertEquals(listOf("abc"), clipboard.written)
        assertEquals(CopyState.Copied, copier.state)
        advanceTimeBy(999)
        assertEquals(CopyState.Copied, copier.state, "went back before its time")
        advanceTimeBy(2)
        assertEquals(CopyState.Ready, copier.state, "still ${copier.state} after its time")
    }

    @Test
    fun aRefusedCopyStaysRefusedUntilTheNext() = runTest {
        val clipboard = Clipboard { false }
        val copier = Copier(clipboard, backgroundScope, shownMillis = 1_000)
        copier.copy("abc")
        runCurrent()
        advanceTimeBy(10_000)
        assertEquals(CopyState.Refused, copier.state)
        clipboard.answer = { true }
        copier.copy("abc")
        runCurrent()
        assertEquals(CopyState.Copied, copier.state)
    }

    @Test
    fun aClipboardThatThrowsIsRefusedNotACrash() = runTest {
        val copier = Copier(Clipboard { error("no clipboard here") }, backgroundScope, shownMillis = 1_000)
        copier.copy("abc")
        runCurrent()
        assertEquals(CopyState.Refused, copier.state)
    }

    @Test
    fun aSecondCopyReplacesTheFirst() = runTest {
        val clipboard = Clipboard()
        val copier = Copier(clipboard, backgroundScope, shownMillis = 1_000)
        copier.copy("first")
        runCurrent()
        advanceTimeBy(900)
        copier.copy("second")
        runCurrent()
        advanceTimeBy(900)
        assertEquals(CopyState.Copied, copier.state, "the first copy's timer reset the second")
        assertEquals(listOf("first", "second"), clipboard.written)
    }
}
