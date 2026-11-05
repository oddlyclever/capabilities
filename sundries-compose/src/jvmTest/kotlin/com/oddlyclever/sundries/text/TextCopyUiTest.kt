package com.oddlyclever.sundries.text

import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Pins TextCopy as a person and a screen reader meet it: one clickable node labelled with the app's own words, a tap
 * that copies [copies] rather than the shown text, and a label that changes to say it was copied, or that it was not.
 */
@OptIn(ExperimentalTestApi::class)
class TextCopyUiTest {

    private class Clipboard(private val takes: Boolean) : ClipboardWriter {
        val written = mutableListOf<String>()
        override suspend fun write(text: String): Boolean {
            written += text
            return takes
        }
    }

    private val strings = CopyStrings(copy = "Copy the handle", copied = "Copied the handle", refused = "Not copied")

    @Test
    fun aTapCopiesWhatItIsToldAndSaysSo() = runComposeUiTest {
        val clipboard = Clipboard(takes = true)
        setContent {
            val scope = rememberCoroutineScope()
            val copier = remember { Copier(clipboard, scope, shownMillis = 60_000) }
            TextCopy("Ada · @ada", copies = "@ada", strings = strings, copier = copier)
        }
        onNodeWithContentDescription("Copy the handle").assertHasClickAction().performClick()
        waitForIdle()
        assertEquals(listOf("@ada"), clipboard.written)
        onNode(SemanticsMatcher.expectValue(SemanticsProperties.ContentDescription, listOf("Copied the handle"))).assertExists()
    }

    @Test
    fun aRefusalSaysSoInTheAppsWords() = runComposeUiTest {
        setContent {
            val scope = rememberCoroutineScope()
            val copier = remember { Copier(Clipboard(takes = false), scope, shownMillis = 60_000) }
            TextCopy("@ada", strings = strings, copier = copier)
        }
        onNodeWithContentDescription("Copy the handle").performClick()
        waitForIdle()
        onNodeWithContentDescription("Not copied").assertExists()
    }
}
