package com.oddlyclever.sundries

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Pins the shimmer's timing: a sweep crosses at an even pace and then pauses with no band, each pass is counted so
 * the colours can rotate, and a bounce goes and returns, slowing into each edge.
 */
class ShimmerFrameTest {

    private fun at(millis: Long, motion: ShimmerMotion = ShimmerMotion.Sweep) =
        shimmerFrame(millis, motion, sweepMillis = 1_000, pauseMillis = 500)

    private fun assertNear(expected: Float, actual: Float?, what: String) {
        assertNotNull(actual, "$what: no band")
        assertTrue(abs(expected - actual) < 1e-4f, "$what: expected $expected, was $actual")
    }

    @Test
    fun aSweepCrossesAtAnEvenPace() {
        assertNear(0f, at(0)?.progress, "start")
        assertNear(0.25f, at(250)?.progress, "a quarter")
        assertNear(0.5f, at(500)?.progress, "half")
        assertNear(0.999f, at(999)?.progress, "the end")
    }

    @Test
    fun aPauseHasNoBand() {
        for (millis in listOf(1_000L, 1_250L, 1_499L)) assertNull(at(millis), "at $millis: ${at(millis)}")
    }

    @Test
    fun eachPassIsCountedSoTheColoursRotate() {
        assertEquals(0L, at(500)?.pass, "first pass")
        assertEquals(1L, at(1_500)?.pass, "second pass, as the pause ends")
        assertNear(0f, at(1_500)?.progress, "the second pass starts at the start edge")
        assertEquals(4L, at(6_100)?.pass, "fifth pass")
    }

    @Test
    fun aBounceGoesAndReturnsSlowingIntoEachEdge() {
        val bounce = ShimmerMotion.Bounce
        assertNear(0.5f, at(500, bounce)?.progress, "going, halfway")
        assertNear(0.5f, at(2_000, bounce)?.progress, "returning, halfway")
        val going = at(250, bounce)?.progress ?: 0f
        assertTrue(going < 0.25f, "going is slow near the start edge: $going at a quarter of the time")
        val returning = at(1_750, bounce)?.progress ?: 0f
        assertTrue(returning > 0.75f, "returning starts from the end edge, slowly: $returning at a quarter of the time")
        assertNull(at(2_600, bounce), "a pause after the return")
    }
}
