package com.oddlyclever.sundries

import androidx.compose.ui.graphics.Color
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/**
 * Pins colour text: six digits are opaque, eight carry their alpha, including alphas past 0x7F, the `#` is optional,
 * anything else is Unspecified from [toColor] and an error from [toColorInt], and [toColorString] reads back as the
 * same colour.
 */
class ColorsTest {

    @Test
    fun sixDigitsAreOpaque() {
        assertEquals(Color(0xFFFF0000), "#FF0000".toColor())
        assertEquals(Color(0xFF00FF00), "00ff00".toColor())
    }

    @Test
    fun eightDigitsCarryTheirAlpha() {
        assertEquals(Color(0x800000FF), "#800000FF".toColor())
        assertEquals(Color(0xFF123456), "FF123456".toColor())
    }

    @Test
    fun anythingElseIsUnspecifiedOrAnError() {
        for (bad in listOf("", "#", "red", "#FFF", "#GG0000", "0xFF0000", "#FF00000")) {
            assertEquals(Color.Unspecified, bad.toColor(), "\"$bad\" read as a colour")
            assertFailsWith<IllegalArgumentException>("\"$bad\" read as an int") { bad.toColorInt() }
        }
    }

    @Test
    fun aColourStringReadsBackAsTheSameColour() {
        for (colour in listOf(Color(0xFFFF0000), Color(0x00000000), Color(0x80123456))) {
            val text = colour.toColorString()
            assertEquals(9, text.length, "\"$text\" is not #AARRGGBB")
            assertEquals(colour, text.toColor(), "\"$text\" read back differently")
        }
    }
}
