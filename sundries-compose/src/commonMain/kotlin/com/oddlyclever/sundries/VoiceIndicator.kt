package com.oddlyclever.sundries

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.min
import kotlin.math.sin

/** What a [VoiceIndicator] draws. */
enum class VoicePattern {
    /**
     * A line pinned at both ends that is almost flat in silence and swells with the voice, with a fainter line behind
     * it drifting the other way. The swell is widest in the middle, which is what keeps one line from reading as
     * something crawling.
     */
    Wave,

    /** Five upright bars, tallest in the middle, each on a beat of its own: a level meter. */
    Bars,

    /** Three dots that swell. */
    Dots,

    /** Rings that spread from the middle and fade, as from something sounding. */
    Rings,

    /** One circle that swells and settles, as breathing. */
    Breathing,
}

/**
 * A small drawn sign that a microphone is live, for the inside of a button or beside one.
 *
 * It either follows the sound or moves by itself. Given a [level], from 0 for silence to 1 for loud speech, it rests
 * near still in silence and moves with the voice, so a person sees both that it is listening and that it hears them.
 * Given `null`, it moves by itself for as long as it is shown, which says only that it is listening: for when an
 * app has no measure of the sound, or has not been given one yet. It knows nothing of where the level comes from; an
 * app passes it a number as often as it has one, and the indicator smooths it, rising quickly and falling slowly, so
 * a meter fed a dozen times a second moves without jumping.
 *
 * The level need not be fitted to the room or the microphone. The indicator learns the quietest and the loudest it
 * has been given and moves between them: the quietest is taken for the room's own sound, which rests it, and what is
 * louder moves it in proportion. So a soft voice at a weak microphone shows as fully as a loud one at a strong
 * microphone, and a measure whose range is not known can be passed as it comes.
 *
 * It draws in [color] and nothing else: no background and no outline, which are the button's. It fills the size it
 * is given and is meant for a square of about 18dp and up.
 *
 * Only drawing changes while it runs. It ignores the platform's reduced-motion setting, which Compose Multiplatform
 * does not expose on every target, so an app that honours it passes `animated = false`: the pattern is then drawn
 * once, at rest in a form that still reads as its own, and whether the microphone is live has to be told by the
 * button around it. It carries no semantics: the button says what it is.
 *
 * @param level how loud the sound is now, from 0 to 1, or null to move without following anything. Values outside
 *   the range are held to it.
 * In silence a still line is easy to miss, so by default the [VoicePattern.Wave] keeps rolling slowly while the
 * room is quiet, one period across its width, and grows into the voice's ripple as someone speaks. Pass
 * `rollsWhenQuiet = false` for a line that lies flat until there is a voice. [PulseRing] is the other sign for a
 * quiet room, drawn around the button; the two are separate so an app can use either or both.
 *
 * @param rollsWhenQuiet whether the wave keeps rolling in silence. Other patterns ignore it.
 * @param gate how far above the quietest sound heard, as a share of the way to the loudest, still counts as
 *   silence, so the room's own unsteadiness does not move it.
 */
@Composable
fun VoiceIndicator(
    level: Float?,
    color: Color,
    modifier: Modifier = Modifier,
    pattern: VoicePattern = VoicePattern.Wave,
    animated: Boolean = true,
    rollsWhenQuiet: Boolean = true,
    gate: Float = 0.1f,
) {
    val given by rememberUpdatedState(level)
    var frame by remember { mutableStateOf(VoiceFrame()) }
    if (animated) {
        LaunchedEffect(Unit) {
            var last = withFrameMillis { it }
            while (true) {
                withFrameMillis { now ->
                    frame = frame.advancedBy(now - last, given, gate)
                    last = now
                }
            }
        }
    }
    Canvas(modifier = modifier) {
        val shown = if (animated) frame else VoiceFrame.still
        when (pattern) {
            VoicePattern.Wave -> drawWave(shown, color, rollsWhenQuiet)
            VoicePattern.Bars -> drawBars(shown, color)
            VoicePattern.Dots -> drawDots(shown, color)
            VoicePattern.Rings -> drawRings(shown, color)
            VoicePattern.Breathing -> drawBreathing(shown, color)
        }
    }
}

/**
 * Where an indicator is in its movement: how strongly it is [moved], from 0 to 1; how far its own slow motion has
 * [turned], in turns; and the [quiet]est and [loud]est levels it has been given, negative until it has been given
 * one. Kept apart from drawing so that the arithmetic can be tested without a screen.
 */
internal data class VoiceFrame(val moved: Float = 0f, val turned: Float = 0f, val quiet: Float = -1f, val loud: Float = -1f) {

    /**
     * The frame [millis] later, having heard [level], or moving by itself when [level] is null.
     *
     * The level is placed between the quietest and loudest heard. The quietest follows the sound down within half a
     * second, not at once, so that it is the room's usual low and not one stray dip, and up only over many seconds,
     * so a pause finds the room and speech does not pass for it. The loudest follows the sound up at once and eases
     * back over several seconds, so one shout does not flatten what follows. The two are never closer than a least
     * span, and a room's own sway above its quiet is taken off, or a room would fill the range by itself. What is
     * under [gate] of the way up counts as silence.
     *
     * The strength rises towards a louder level quickly and falls towards a quieter one slowly, as a meter's needle
     * does, so speech is seen at once and its gaps do not flatten it. The slow motion turns faster the more it is
     * moved, between a drift and about two turns a second, so it never jitters.
     */
    fun advancedBy(millis: Long, level: Float?, gate: Float): VoiceFrame {
        val step = millis.coerceIn(0L, 100L).toFloat()
        var quietest = quiet
        var loudest = loud
        // With no level to follow it holds a steady middle strength, and the patterns' own motion carries it.
        val target = if (level == null) {
            SELF_MOVED
        } else {
            val heard = level.coerceIn(0f, 1f)
            quietest = when {
                quietest < 0f -> heard
                else -> quietest + (heard - quietest) * part(step, if (heard < quietest) QUIET_FALL_MILLIS else QUIET_RISE_MILLIS)
            }
            val least = quietest + LEAST_SPAN
            loudest = when {
                heard > loudest -> heard
                else -> loudest + (least - loudest) * part(step, LOUD_FALL_MILLIS)
            }.coerceAtLeast(least)
            // A room is never quite steady, so what is only a little over its quiet is still the room.
            gated((heard - quietest - ROOM_SWAY) / (loudest - quietest - ROOM_SWAY), gate)
        }
        val next = moved + (target - moved) * part(step, if (target > moved) ATTACK_MILLIS else DECAY_MILLIS)
        val speed = DRIFT_TURNS_PER_SECOND + (FASTEST_TURNS_PER_SECOND - DRIFT_TURNS_PER_SECOND) * next
        return VoiceFrame(next, (turned + speed * step / 1000f) % WRAP_TURNS, quietest, loudest)
    }

    /** How much of the way to a value is covered in [step] milliseconds, easing with the time constant [millis]. */
    private fun part(step: Float, millis: Float): Float = 1f - exp(-step / millis)

    companion object {
        /** What is drawn with no animation: at rest, moved enough for the pattern to be told. */
        val still = VoiceFrame(moved = 0.45f, turned = 0f)

        const val ATTACK_MILLIS = 70f
        const val DECAY_MILLIS = 180f
        const val DRIFT_TURNS_PER_SECOND = 0.35f
        const val FASTEST_TURNS_PER_SECOND = 2f
        const val SELF_MOVED = 0.6f
        const val QUIET_FALL_MILLIS = 400f
        const val QUIET_RISE_MILLIS = 12_000f
        const val LOUD_FALL_MILLIS = 6_000f
        const val LEAST_SPAN = 0.12f
        const val ROOM_SWAY = 0.1f

        // Every pattern's motion repeats within this many turns, so wrapping here shows no jump.
        const val WRAP_TURNS = 840f
    }
}

/** [level] with everything under [gate] as silence and the rest stretched to fill 0 to 1. */
internal fun gated(level: Float, gate: Float): Float {
    val held = level.coerceIn(0f, 1f)
    val floor = gate.coerceIn(0f, 0.95f)
    return if (held <= floor) 0f else (held - floor) / (1f - floor)
}

/**
 * How far the wave line is from the middle at [across], from 0 at its start to 1 at its end, as a share of the
 * height it is drawn in; positive is down.
 *
 * It is a sine of [cycles] across the width, under an envelope that is nothing at both ends and whole in the middle,
 * so the ends stay where they are and the swell is in the middle. The envelope is a plain arch and not its square,
 * so the swell is broad and the peaks near the ends are not lost. [amplitude] is its greatest reach.
 */
internal fun wavePoint(across: Float, turned: Float, amplitude: Float, cycles: Float): Float {
    val bell = sin(PI.toFloat() * across)
    return amplitude * bell * sin(2f * PI.toFloat() * (cycles * across - turned))
}

/**
 * The wave's reach, as a share of the height: almost flat at rest and nearly to the edge when fully moved. It is
 * meant to be seen and not to be read as a measure, so a moderate voice is drawn well over half way: the curve
 * lifts the middle of the range and leaves the bottom low, so a room's own sound stays nearly flat.
 */
internal fun waveAmplitude(moved: Float): Float {
    val held = moved.coerceIn(0f, 1f)
    return WAVE_REST + (WAVE_FULL - WAVE_REST) * (1f - (1f - held) * (1f - held))
}

private const val WAVE_REST = 0.03f
private const val WAVE_FULL = 0.46f
private const val WAVE_CYCLES = 2.25f

/** The quiet roll's reach, as a share of the height: clearly moving, about a third of a voice's. */
private const val ROLL_REACH = 0.13f
private const val ROLL_CYCLES = 1f

/**
 * The wave's reach and how many cycles it has across its width, for a frame [moved] this much: with [rolls], a slow
 * roll of one period in silence that grows into the voice's ripple, the two blended so there is no jump between
 * them; without, the voice's ripple alone, almost flat in silence.
 */
internal fun waveShape(moved: Float, rolls: Boolean): Pair<Float, Float> {
    val held = moved.coerceIn(0f, 1f)
    if (!rolls) return waveAmplitude(held) to WAVE_CYCLES
    return (ROLL_REACH + (waveAmplitude(held) - ROLL_REACH) * held) to (ROLL_CYCLES + (WAVE_CYCLES - ROLL_CYCLES) * held)
}

private fun DrawScope.drawWave(frame: VoiceFrame, color: Color, rolls: Boolean) {
    val (amplitude, cycles) = waveShape(frame.moved, rolls)
    val steps = 40
    fun line(turned: Float, reach: Float, cycles: Float) = Path().apply {
        for (step in 0..steps) {
            val across = step / steps.toFloat()
            val x = size.width * across
            val y = size.height / 2f + size.height * wavePoint(across, turned, reach, cycles)
            if (step == 0) moveTo(x, y) else lineTo(x, y)
        }
    }
    // The fainter line is a harmonic drifting the other way, so the two braid as sound does and do not travel together.
    drawPath(line(-frame.turned * 0.7f + 0.3f, amplitude * 0.7f, cycles * 1.5f), color.copy(alpha = color.alpha * 0.38f), style = Stroke(size.minDimension / 16f, cap = StrokeCap.Round))
    drawPath(line(frame.turned, amplitude, cycles), color, style = Stroke(size.minDimension / 12f, cap = StrokeCap.Round))
}

/** Each bar: how tall it gets, of the whole height; how many times it beats in a turn; and where in a beat it starts. */
private val Bars = listOf(
    Triple(0.55f, 2.5f, 0.10f),
    Triple(0.80f, 1.5f, 0.55f),
    Triple(1.00f, 2.0f, 0.25f),
    Triple(0.80f, 3.5f, 0.80f),
    Triple(0.55f, 2.0f, 0.45f),
)

/** How tall bar [index] is, as a share of the height: a low rest, and its own beat scaled by how moved the frame is. */
internal fun barHeight(index: Int, frame: VoiceFrame): Float {
    val (tallest, beats, offset) = Bars[index]
    val beat = abs(sin(PI.toFloat() * (frame.turned * beats + offset)))
    return tallest * (BAR_REST + (1f - BAR_REST) * frame.moved.coerceIn(0f, 1f) * (0.45f + 0.55f * beat))
}

private const val BAR_REST = 0.18f

private fun DrawScope.drawBars(frame: VoiceFrame, color: Color) {
    val gap = size.width / (Bars.size * 2 - 1)
    for (index in Bars.indices) {
        val height = size.height * barHeight(index, frame)
        val x = gap * (index * 2) + gap / 2
        drawLine(color, Offset(x, (size.height - height) / 2), Offset(x, (size.height + height) / 2), strokeWidth = gap, cap = StrokeCap.Round)
    }
}

private fun DrawScope.drawDots(frame: VoiceFrame, color: Color) {
    val count = 3
    val cell = size.width / count
    for (index in 0 until count) {
        val beat = 0.5f + 0.5f * sin(2f * PI.toFloat() * (frame.turned - index * 0.18f))
        val radius = min(cell, size.height) * (0.16f + 0.26f * frame.moved * beat)
        drawCircle(color, radius, Offset(cell * (index + 0.5f), size.height / 2f))
    }
}

private fun DrawScope.drawRings(frame: VoiceFrame, color: Color) {
    val reach = size.minDimension / 2f
    // A steady dot in the middle is what the rings come from; with nothing moving it, it is all there is.
    drawCircle(color, reach * 0.22f)
    for (ring in 0 until 2) {
        val spread = (frame.turned * 0.6f + ring * 0.5f) % 1f
        val alpha = color.alpha * (1f - spread) * (0.15f + 0.6f * frame.moved)
        drawCircle(color.copy(alpha = alpha), reach * (0.3f + 0.7f * spread), style = Stroke(size.minDimension / 14f))
    }
}

private fun DrawScope.drawBreathing(frame: VoiceFrame, color: Color) {
    val reach = size.minDimension / 2f
    val breath = 0.5f + 0.5f * sin(2f * PI.toFloat() * frame.turned * 0.5f)
    val swell = 0.45f + 0.45f * (0.35f * breath + 0.65f * frame.moved)
    drawCircle(color.copy(alpha = color.alpha * 0.25f), reach * swell)
    drawCircle(color, reach * swell, style = Stroke(size.minDimension / 14f))
}

/**
 * A ring that spreads outward from the edge of what it is laid over and fades, again and again, as from something
 * sounding: the sign that a microphone is live, for a quiet room where a voice indicator has nothing to show. It
 * is meant to sit behind a round button the same size, in a `Box` with it, and draws past its own bounds, so
 * nothing between it and the window may clip it.
 *
 * It runs while [active] and draws nothing otherwise. As with [VoiceIndicator], reduced motion is the app's to
 * honour, by passing `active = false`.
 *
 * @param spread how far the ring spreads past the edge, as a share of the radius.
 */
@Composable
fun PulseRing(
    color: Color,
    active: Boolean,
    modifier: Modifier = Modifier,
    periodMillis: Int = 2_600,
    spread: Float = 0.3f,
    width: Dp = 1.5.dp,
) {
    require(periodMillis > 0) { "periodMillis must be positive, was $periodMillis" }
    var phase by remember { mutableStateOf(0f) }
    if (active) {
        LaunchedEffect(periodMillis) {
            val start = withFrameMillis { it }
            while (true) withFrameMillis { now -> phase = pulsePhase(now - start, periodMillis) }
        }
    }
    Canvas(modifier = modifier) {
        if (!active) return@Canvas
        val (scale, alpha) = pulseRing(phase, spread)
        drawCircle(color.copy(alpha = color.alpha * alpha), size.minDimension / 2f * scale, style = Stroke(width.toPx()))
    }
}

/** How far through a pulse of [periodMillis] it is after [elapsedMillis], from 0 to just under 1. */
internal fun pulsePhase(elapsedMillis: Long, periodMillis: Int): Float = (elapsedMillis.mod(periodMillis.toLong())).toFloat() / periodMillis

/** The ring at [phase] through a pulse: its radius as a share of the edge's, and how strongly it is drawn. */
internal fun pulseRing(phase: Float, spread: Float): Pair<Float, Float> {
    val held = phase.coerceIn(0f, 1f)
    // It eases out as it spreads, and fades as it goes, so it leaves the edge firmly and vanishes softly.
    val eased = 1f - (1f - held) * (1f - held)
    return (1f + spread * eased) to (0.6f * (1f - held))
}
