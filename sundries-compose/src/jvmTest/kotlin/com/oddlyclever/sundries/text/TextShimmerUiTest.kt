package com.oddlyclever.sundries.text

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.runComposeUiTest
import kotlin.test.Test

/**
 * Pins TextShimmer as an accessibility service meets it: its text, and an indeterminate progress bar while it runs,
 * which it stops being once inactive.
 */
@OptIn(ExperimentalTestApi::class)
class TextShimmerUiTest {

    private val inProgress = SemanticsMatcher.expectValue(SemanticsProperties.ProgressBarRangeInfo, ProgressBarRangeInfo.Indeterminate)

    @Test
    fun whileActiveItIsAnIndeterminateProgressBarWithItsText() = runComposeUiTest {
        mainClock.autoAdvance = false
        var active by mutableStateOf(true)
        setContent { TextShimmer("Thinking", active = active) }
        mainClock.advanceTimeByFrame()
        onNode(hasText("Thinking") and inProgress).assertExists()

        active = false
        mainClock.advanceTimeByFrame()
        onNode(hasText("Thinking")).assertExists()
        onNode(inProgress).assertDoesNotExist()
    }
}
