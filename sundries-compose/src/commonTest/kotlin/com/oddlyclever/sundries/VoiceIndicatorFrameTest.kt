package com.oddlyclever.sundries

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Pins the arithmetic behind [VoiceIndicator]: that a room rests it and a voice over the room moves it, whatever
 * the measure's scale, quickly up and slowly down; that the wave's ends stay put and its reach is bounded; and that
 * nothing it draws leaves its bounds.
 */
class VoiceIndicatorFrameTest {
    private fun after(millis: Long, level: Float?, from: VoiceFrame = VoiceFrame(), gate: Float = 0.2f): VoiceFrame {
        var frame = from
        repeat((millis / 16).toInt()) { frame = frame.advancedBy(16, level, gate) }
        return frame
    }

    /** A frame that has heard a room at [room] for a second, so it knows what quiet is. */
    private fun settledIn(room: Float): VoiceFrame = after(1_000, room)

    @Test
    fun aSteadyRoomRestsItAndAVoiceOverItMovesIt() {
        val quiet = settledIn(0.38f)
        assertEquals(0f, quiet.moved, "moved by a steady room")
        val spoken = after(1_000, 0.7f, from = quiet).moved
        assertTrue(spoken > 0.9f, "a voice over the room moved it only $spoken")
    }

    @Test
    fun theScaleOfTheLevelDoesNotMatter() {
        // A voice on a measure a third the size still moves it well, where a fixed scale would hardly show it.
        val wide = after(500, 0.9f, from = settledIn(0.3f)).moved
        val narrow = after(500, 0.3f, from = settledIn(0.1f)).moved
        assertTrue(wide > 0.9f && narrow > 0.5f, "moved $wide on a wide measure and $narrow on a narrow one")
    }

    @Test
    fun aRoomsOwnUnsteadinessDoesNotMoveIt() {
        var frame = settledIn(0.38f)
        // As measured in a quiet room: the level wanders over about an eighth of the range.
        repeat(240) { step -> frame = frame.advancedBy(16, 0.38f + 0.13f * ((step * 7) % 10) / 10f, 0.2f) }
        assertTrue(frame.moved < 0.1f, "a room wavering by an eighth moved it ${frame.moved}")
        assertTrue(waveAmplitude(frame.moved) < 0.1f, "a wavering room swung the wave to ${waveAmplitude(frame.moved)}")
        assertEquals(0f, gated(0.2f, 0.2f))
        assertEquals(1f, gated(1f, 0.2f))
        assertEquals(1f, gated(7f, 0.2f), "a level over the range was not held to it")
        assertEquals(0f, gated(-3f, 0.2f), "a level under the range was not held to it")
    }

    @Test
    fun aQuietVoiceJustOverTheRoomIsSeenClearly() {
        val voice = after(1_000, 0.5f, from = settledIn(0.38f)).moved
        assertTrue(waveAmplitude(voice) > 0.3f, "a voice just over the room swung the wave only ${waveAmplitude(voice)}")
    }

    @Test
    fun aQuieterVoiceAfterALoudOneStillShows() {
        val shouted = after(300, 0.95f, from = settledIn(0.3f))
        val later = after(10_000, 0.3f, from = shouted)
        val soft = after(300, 0.55f, from = later).moved
        assertTrue(soft > 0.5f, "a soft voice ten seconds after a shout moved it only $soft")
    }

    @Test
    fun speechDoesNotPassForTheRoom() {
        val talking = after(5_000, 0.6f, from = settledIn(0.3f))
        assertTrue(talking.moved > 0.5f, "five seconds of steady speech and it is moved only ${talking.moved}")
    }

    @Test
    fun itRisesFasterThanItFalls() {
        val room = settledIn(0.3f)
        val risen = after(96, 0.7f, from = room).moved
        val loud = after(1_000, 0.7f, from = room)
        val fallen = loud.moved - after(96, 0.3f, from = loud).moved
        assertTrue(risen > fallen, "rose $risen and fell $fallen in the same time")
    }

    @Test
    fun withNoLevelItMovesByItself() {
        val frame = after(1_000, null)
        assertTrue(frame.moved > 0.5f, "moved only ${frame.moved} with nothing to follow")
        assertTrue(frame.turned > 0.5f, "turned only ${frame.turned} in a second")
    }

    @Test
    fun itTurnsFasterTheMoreItIsMovedAndNeverJitters() {
        val room = settledIn(0.3f)
        val drifting = after(1_000, 0.3f, from = room).turned - room.turned
        val driven = after(1_000, 0.8f, from = room).turned - room.turned
        assertTrue(drifting in 0.3f..0.45f, "drifted $drifting turns in a second of silence")
        assertTrue(driven > drifting && driven <= 2.05f, "turned $driven in a second of loud speech")
    }

    @Test
    fun aLongGapBetweenFramesIsNotALeap() {
        val frame = VoiceFrame().advancedBy(60_000, null, 0.2f)
        assertTrue(frame.turned < 0.25f, "one late frame turned it ${frame.turned}")
    }

    @Test
    fun theWavesEndsStayWhereTheyAre() {
        for (turned in listOf(0f, 0.13f, 0.5f, 3.7f)) {
            assertTrue(abs(wavePoint(0f, turned, 0.19f, 2.25f)) < 1e-6f, "the start moved at $turned")
            assertTrue(abs(wavePoint(1f, turned, 0.19f, 2.25f)) < 1e-6f, "the end moved at $turned")
        }
    }

    @Test
    fun theWaveIsAlmostFlatAtRestAndStaysInsideItsBoundsWhenMoved() {
        assertTrue(waveAmplitude(0f) <= 0.04f, "at rest the wave reaches ${waveAmplitude(0f)} of the height")
        val full = waveAmplitude(1f)
        assertTrue(full > 4 * waveAmplitude(0f), "fully moved it reaches only $full")
        for (step in 0..100) {
            val reach = abs(wavePoint(step / 100f, 0.37f, full, 2.25f))
            assertTrue(reach <= full + 1e-6f && reach < 0.5f, "reached $reach of the height at $step")
        }
    }

    @Test
    fun barsStayLowAtRestAndInsideTheirBoundsWhenMoved() {
        for (index in 0 until 5) {
            assertTrue(barHeight(index, VoiceFrame(moved = 0f, turned = 0.4f)) <= 0.2f, "bar $index is tall at rest")
            for (step in 0..50) {
                val height = barHeight(index, VoiceFrame(moved = 1f, turned = step / 10f))
                assertTrue(height in 0f..1f, "bar $index is $height of the height")
            }
        }
    }

    @Test
    fun theMiddleBarIsTheTallest() {
        val still = VoiceFrame(moved = 0f)
        assertTrue(barHeight(2, still) > barHeight(0, still) && barHeight(2, still) > barHeight(4, still))
    }

    @Test
    fun inSilenceTheWaveRollsOnePeriodAndGrowsIntoTheVoicesRipple() {
        val (quietReach, quietCycles) = waveShape(0f, rolls = true)
        assertTrue(quietReach in 0.1f..0.2f, "rolling in silence it reaches $quietReach")
        assertEquals(1f, quietCycles, "rolling in silence it has $quietCycles cycles")
        val (loudReach, loudCycles) = waveShape(1f, rolls = true)
        assertEquals(waveAmplitude(1f), loudReach, "fully moved it reaches $loudReach")
        assertEquals(2.25f, loudCycles)
        // No jump: a little more voice is a little more wave.
        val (nearReach, _) = waveShape(0.02f, rolls = true)
        assertTrue(abs(nearReach - quietReach) < 0.02f, "a breath of voice jumped the reach from $quietReach to $nearReach")
    }

    @Test
    fun withoutRollingTheWaveLiesAlmostFlatInSilence() {
        assertTrue(waveShape(0f, rolls = false).first <= 0.04f, "without rolling it reaches ${waveShape(0f, rolls = false).first}")
    }

    @Test
    fun theRingSpreadsFromTheEdgeAndFades() {
        val (startScale, startAlpha) = pulseRing(0f, 0.3f)
        val (endScale, endAlpha) = pulseRing(0.999f, 0.3f)
        assertEquals(1f, startScale, "the ring started at $startScale of the edge")
        assertTrue(endScale in 1.29f..1.3f, "the ring ended at $endScale of the edge")
        assertTrue(startAlpha > 0.5f && endAlpha < 0.01f, "the ring was drawn at $startAlpha and ended at $endAlpha")
        assertEquals(0.5f, pulsePhase(1_300, 2_600))
        assertEquals(0f, pulsePhase(2_600, 2_600), "a whole period did not start the pulse again")
    }
}
