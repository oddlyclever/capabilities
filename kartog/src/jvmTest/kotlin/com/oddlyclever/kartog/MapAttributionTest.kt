package com.oddlyclever.kartog

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.sp
import com.coderwise.libs.mapview.MapCameraState
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Pins the attribution rule OpenStreetMap's guidelines allow on an interactive map: the credit is in full when the map
 * appears, shrinks to an "i" after five seconds, comes back when the "i" is tapped, and never shrinks in full style.
 */
@OptIn(ExperimentalTestApi::class)
class MapAttributionTest {

    private val credit = "© OpenStreetMap"

    @Test
    fun theCreditIsShownFirstThenShrinksToAnI() = runComposeUiTest {
        mainClock.autoAdvance = false
        setContent { MapAttribution(credit, MapCameraState(), AttributionStyle.Collapsible, 9.sp, onClick = null) }
        mainClock.advanceTimeByFrame()
        onNodeWithText(credit).assertExists("the credit on first display")
        mainClock.advanceTimeBy(ATTRIBUTION_SHOWN_MILLIS + 100)
        onNodeWithText(credit).assertDoesNotExist()
        onNodeWithContentDescription("Map credits").assertExists("the i after five seconds")
    }

    @Test
    fun tappingTheIShowsTheCreditAgain() = runComposeUiTest {
        mainClock.autoAdvance = false
        setContent { MapAttribution(credit, MapCameraState(), AttributionStyle.Collapsible, 9.sp, onClick = null) }
        mainClock.advanceTimeBy(ATTRIBUTION_SHOWN_MILLIS + 100)
        onNodeWithContentDescription("Map credits").performClick()
        mainClock.advanceTimeByFrame()
        onNodeWithText(credit).assertExists("the credit after tapping the i")
        mainClock.advanceTimeBy(ATTRIBUTION_SHOWN_MILLIS + 100)
        onNodeWithContentDescription("Map credits").assertExists("shrunk again by the same rule")
    }

    @Test
    fun fullStyleNeverShrinks() = runComposeUiTest {
        mainClock.autoAdvance = false
        setContent { MapAttribution(credit, MapCameraState(), AttributionStyle.Full, 9.sp, onClick = null) }
        mainClock.advanceTimeBy(ATTRIBUTION_SHOWN_MILLIS * 3)
        onNodeWithText(credit).assertExists("the credit in full style")
    }

    @Test
    fun tappingTheCreditRunsTheClickHandler() = runComposeUiTest {
        var clicks = 0
        setContent { MapAttribution(credit, MapCameraState(), AttributionStyle.Full, 9.sp, onClick = { clicks++ }) }
        onNodeWithText(credit).performClick()
        assertEquals(1, clicks, "clicks")
    }
}
