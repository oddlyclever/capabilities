package com.oddlyclever.sundries

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.toRect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.invalidateDraw
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.unit.LayoutDirection
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos

/** How a [shimmer] band travels across what it covers. */
enum class ShimmerMotion {
    /** From the start edge to the end edge, then a pause, then again from the start: a "working on it" sweep. */
    Sweep,

    /** To the end edge, a pause, back to the start, a pause: a scanner, slowing as it turns at each edge. */
    Bounce,
}

/**
 * Sweeps a soft band of colour across whatever this modifier draws, over the drawn pixels only: across the letters
 * of a text and not the space around them, across an icon's shape and not its box.
 *
 * The band recolours what is drawn rather than drawing over it. Outside the band the content keeps its own colours,
 * unless [base] is set, which then recolours all of it, as long as the shimmer is applied, running or not. A band
 * never changes how opaque a pixel is, so [base] is how to get the muted "thinking" look: an opaque text with a
 * half-transparent [base] and an opaque band colour shows the text faint, and full where the band crosses it. Text
 * dimmed with its own colour's alpha would stay faint inside the band too. For a colour shimmer, pass the app's
 * highlight colour. With several [colors], each pass takes the next, in order, and starts again from the first, so a
 * pass is one colour and the colours rotate from pass to pass. An empty list draws no band.
 *
 * The band is [band] of the content's width, from transparent at its edges to the pass's colour at its middle. A
 * [ShimmerMotion.Sweep] enters at the start edge and leaves at the end edge in [sweepMillis], following the layout
 * direction, so it runs right to left in a right-to-left layout; [pauseMillis] then passes with no band. A
 * [ShimmerMotion.Bounce] goes and returns, each way a pass and each followed by the pause.
 *
 * Only drawing changes while it runs: the band moves each frame without recomposing or measuring anything. It runs
 * while [active] is true and the content is on screen. It ignores the platform's reduced-motion setting, which
 * Compose Multiplatform does not expose on every target, so an app that honours it passes `active = false` there.
 *
 * @throws IllegalArgumentException if [sweepMillis] is not positive, [pauseMillis] is negative, or [band] is not
 *   greater than 0 and at most 1.
 */
fun Modifier.shimmer(
    colors: List<Color>,
    active: Boolean = true,
    motion: ShimmerMotion = ShimmerMotion.Sweep,
    sweepMillis: Int = 1_000,
    pauseMillis: Int = 800,
    band: Float = 0.5f,
    base: Color = Color.Unspecified,
): Modifier {
    require(sweepMillis > 0) { "sweepMillis must be positive, was $sweepMillis" }
    require(pauseMillis >= 0) { "pauseMillis must not be negative, was $pauseMillis" }
    require(band > 0f && band <= 1f) { "band must be greater than 0 and at most 1, was $band" }
    return this then ShimmerElement(colors, active, motion, sweepMillis, pauseMillis, band, base)
}

/** A one-colour [shimmer]: every pass in [color]. */
fun Modifier.shimmer(
    color: Color,
    active: Boolean = true,
    motion: ShimmerMotion = ShimmerMotion.Sweep,
    sweepMillis: Int = 1_000,
    pauseMillis: Int = 800,
    band: Float = 0.5f,
    base: Color = Color.Unspecified,
): Modifier = shimmer(listOf(color), active, motion, sweepMillis, pauseMillis, band, base)

/**
 * Where a shimmer is at [elapsedMillis] after it started: which pass, and how far along its path from the start edge
 * to the end edge, 0 to 1, or null during a pause. A [ShimmerMotion.Bounce] pass on its way back runs from 1 to 0.
 */
internal fun shimmerFrame(elapsedMillis: Long, motion: ShimmerMotion, sweepMillis: Int, pauseMillis: Int): ShimmerFrame? {
    val period = (sweepMillis + pauseMillis).toLong()
    val pass = elapsedMillis / period
    val within = elapsedMillis % period
    if (within >= sweepMillis) return null
    val travelled = within.toFloat() / sweepMillis
    return when (motion) {
        ShimmerMotion.Sweep -> ShimmerFrame(pass, travelled)
        ShimmerMotion.Bounce -> {
            // Eased, so the band slows into each edge and turns there rather than bouncing off it.
            val eased = ((1 - cos(travelled * PI)) / 2).toFloat()
            ShimmerFrame(pass, if (pass % 2 == 0L) eased else 1f - eased)
        }
    }
}

/** A moment of a shimmer: the [pass] it is in, counted from 0, and the band's [progress] from start edge to end edge. */
internal data class ShimmerFrame(val pass: Long, val progress: Float)

private data class ShimmerElement(
    val colors: List<Color>,
    val active: Boolean,
    val motion: ShimmerMotion,
    val sweepMillis: Int,
    val pauseMillis: Int,
    val band: Float,
    val base: Color,
) : ModifierNodeElement<ShimmerNode>() {
    override fun create() = ShimmerNode(colors, active, motion, sweepMillis, pauseMillis, band, base)

    override fun update(node: ShimmerNode) = node.update(colors, active, motion, sweepMillis, pauseMillis, band, base)

    override fun InspectorInfo.inspectableProperties() {
        name = "shimmer"
        properties["colors"] = colors
        properties["active"] = active
        properties["motion"] = motion
        properties["sweepMillis"] = sweepMillis
        properties["pauseMillis"] = pauseMillis
        properties["band"] = band
        properties["base"] = base
    }
}

private class ShimmerNode(
    private var colors: List<Color>,
    private var active: Boolean,
    private var motion: ShimmerMotion,
    private var sweepMillis: Int,
    private var pauseMillis: Int,
    private var band: Float,
    private var base: Color,
) : Modifier.Node(), DrawModifierNode {

    // Read in draw, so a new frame redraws without recomposing. Equal frames during a pause do not redraw.
    private var frame by mutableStateOf<ShimmerFrame?>(null)
    private var job: Job? = null

    private val runs get() = active && colors.isNotEmpty()

    override fun onAttach() = restart()

    override fun onDetach() {
        job = null
        frame = null
    }

    fun update(
        colors: List<Color>,
        active: Boolean,
        motion: ShimmerMotion,
        sweepMillis: Int,
        pauseMillis: Int,
        band: Float,
        base: Color,
    ) {
        val timing = active != this.active || motion != this.motion || sweepMillis != this.sweepMillis ||
            pauseMillis != this.pauseMillis || colors.isEmpty() != this.colors.isEmpty()
        this.colors = colors
        this.active = active
        this.motion = motion
        this.sweepMillis = sweepMillis
        this.pauseMillis = pauseMillis
        this.band = band
        this.base = base
        if (timing) restart()
        invalidateDraw()
    }

    private fun restart() {
        job?.cancel()
        frame = null
        if (!isAttached || !runs) return
        job = coroutineScope.launch {
            val start = withFrameMillis { it }
            while (true) {
                withFrameMillis { now -> frame = shimmerFrame(now - start, motion, sweepMillis, pauseMillis) }
            }
        }
    }

    override fun ContentDrawScope.draw() {
        val frame = if (runs && size.width > 0f && size.height > 0f) frame else null
        if (frame == null && !base.isSpecified) {
            drawContent()
            return
        }
        // A layer of its own, so the blend recolours only what the content drew and not what lies beneath it.
        drawIntoCanvas { it.saveLayer(size.toRect(), Paint()) }
        drawContent()
        if (frame == null) {
            drawRect(SolidColor(base), blendMode = BlendMode.SrcIn)
        } else {
            val color = colors[(frame.pass % colors.size).toInt()]
            val width = size.width * band
            // The band's middle travels from half a band before the start edge to half a band past the end edge, so
            // it enters and leaves whole.
            val along = -width / 2 + frame.progress * (size.width + width)
            val middle = if (layoutDirection == LayoutDirection.Rtl) size.width - along else along
            // With a base, it fills everywhere the band does not reach, the gradient clamping to it at both ends;
            // without one, the band fades to nothing and SrcAtop leaves the content as it was.
            val edge = if (base.isSpecified) base else color.copy(alpha = 0f)
            val brush = Brush.horizontalGradient(listOf(edge, color, edge), startX = middle - width / 2, endX = middle + width / 2)
            drawRect(brush, blendMode = if (base.isSpecified) BlendMode.SrcIn else BlendMode.SrcAtop)
        }
        drawIntoCanvas { it.restore() }
    }
}
