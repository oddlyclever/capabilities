package com.oddlyclever.sundries

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Pins what a shimmer draws: the band in the pass's colour where it is, the content's own colour elsewhere and
 * during the pause, the next colour on the next pass, the start edge following the layout direction, and nothing
 * at all while inactive; and with a base, the base everywhere the band is not, inactive too.
 */
@OptIn(ExperimentalTestApi::class)
class ShimmerUiTest {

    private fun ComposeUiTest.show(
        colors: List<Color>,
        active: Boolean = true,
        direction: LayoutDirection = LayoutDirection.Ltr,
        base: Color = Color.Unspecified,
    ) {
        mainClock.autoAdvance = false
        setContent {
            CompositionLocalProvider(LocalLayoutDirection provides direction) {
                // A black block the width of the band's path, so a pixel is either the band or the content.
                Box(Modifier.size(100.dp, 20.dp).shimmer(colors, active = active, sweepMillis = 1_000, pauseMillis = 500, base = base).background(Color.Black))
            }
        }
        // The first frame is where the shimmer's time starts.
        mainClock.advanceTimeByFrame()
    }

    /** The colour across the middle row at [fraction] of the width, as 0xRRGGBB. */
    private fun ComposeUiTest.pixelAt(fraction: Float): Int {
        val pixels = onRoot().captureToImage().toPixelMap()
        val colour = pixels[((pixels.width - 1) * fraction).toInt(), pixels.height / 2]
        val r = (colour.red * 255).toInt()
        val g = (colour.green * 255).toInt()
        val b = (colour.blue * 255).toInt()
        return (r shl 16) or (g shl 8) or b
    }

    private fun isRed(rgb: Int) = rgb shr 16 and 0xff > 200 && rgb shr 8 and 0xff < 60 && rgb and 0xff < 60
    private fun isBlue(rgb: Int) = rgb and 0xff > 200 && rgb shr 16 and 0xff < 60 && rgb shr 8 and 0xff < 60
    private fun isBlack(rgb: Int) = rgb shr 16 and 0xff < 30 && rgb shr 8 and 0xff < 30 && rgb and 0xff < 30

    @Test
    fun theBandIsThePasssColourWhereItIsAndTheContentElsewhere() = runComposeUiTest {
        show(listOf(Color.Red))
        mainClock.advanceTimeBy(500)
        val middle = pixelAt(0.5f)
        assertTrue(isRed(middle), "the band's middle, halfway through the pass: ${middle.toString(16)}")
        val edge = pixelAt(0.02f)
        assertTrue(isBlack(edge), "the start edge, outside the band: ${edge.toString(16)}")
    }

    @Test
    fun thePauseShowsOnlyTheContent() = runComposeUiTest {
        show(listOf(Color.Red))
        mainClock.advanceTimeBy(1_250)
        for (fraction in listOf(0.1f, 0.5f, 0.9f)) {
            val pixel = pixelAt(fraction)
            assertTrue(isBlack(pixel), "at $fraction during the pause: ${pixel.toString(16)}")
        }
    }

    @Test
    fun theNextPassTakesTheNextColour() = runComposeUiTest {
        show(listOf(Color.Red, Color.Blue))
        mainClock.advanceTimeBy(2_000)
        val middle = pixelAt(0.5f)
        assertTrue(isBlue(middle), "the second pass, halfway: ${middle.toString(16)}")
    }

    @Test
    fun aRightToLeftSweepStartsFromTheRight() = runComposeUiTest {
        show(listOf(Color.Red), direction = LayoutDirection.Rtl)
        mainClock.advanceTimeBy(333)
        val right = pixelAt(0.75f)
        assertTrue(isRed(right), "a quarter in from the right, a third through: ${right.toString(16)}")
        val left = pixelAt(0.25f)
        assertTrue(isBlack(left), "a quarter in from the left, a third through: ${left.toString(16)}")
    }

    @Test
    fun anInactiveShimmerDrawsNoBand() = runComposeUiTest {
        show(listOf(Color.Red), active = false)
        mainClock.advanceTimeBy(500)
        val middle = pixelAt(0.5f)
        assertTrue(isBlack(middle), "the middle, halfway through where a pass would be: ${middle.toString(16)}")
    }

    @Test
    fun aBaseRecoloursEverythingTheBandDoesNotReach() = runComposeUiTest {
        show(listOf(Color.Red), base = Color.Blue)
        mainClock.advanceTimeBy(500)
        val middle = pixelAt(0.5f)
        assertTrue(isRed(middle), "the band's middle: ${middle.toString(16)}")
        val edge = pixelAt(0.02f)
        assertTrue(isBlue(edge), "outside the band: ${edge.toString(16)}")
        mainClock.advanceTimeBy(750)
        val paused = pixelAt(0.5f)
        assertTrue(isBlue(paused), "the middle during the pause: ${paused.toString(16)}")
    }

    @Test
    fun anInactiveShimmerWithABaseIsTheBase() = runComposeUiTest {
        show(listOf(Color.Red), active = false, base = Color.Blue)
        mainClock.advanceTimeBy(500)
        val middle = pixelAt(0.5f)
        assertTrue(isBlue(middle), "the middle: ${middle.toString(16)}")
    }
}
