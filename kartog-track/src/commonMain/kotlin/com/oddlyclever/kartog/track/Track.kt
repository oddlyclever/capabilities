package com.oddlyclever.kartog.track

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.coderwise.libs.mapview.LatLon
import com.oddlyclever.kartog.MapProjection
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

/**
 * One recorded fix of a track.
 *
 * @property speed metres per second, when the fix carries one.
 * @property accuracy the radius, in metres, the position is good to, when the fix carries one.
 * @property heading the direction of travel in degrees clockwise from north, when the fix carries one.
 */
class TrackPoint(
    val position: LatLon,
    val speed: Double? = null,
    val accuracy: Double? = null,
    val heading: Float? = null,
)

/** How a track's speed shows. */
enum class SpeedStyle {
    /** The line's colour runs from [TrackStyle.slowColor] to [TrackStyle.fastColor] with the speed. */
    Gradient,

    /** Dots along the track that strengthen toward its end, so the direction of travel reads at a glance. */
    Ghosting,

    /** Both. */
    Both,

    /** Neither: the line is [TrackStyle.lineColor]. */
    None,
}

/** How a track's line is drawn. */
enum class LineStyle { Solid, Dotted, None }

/** What marks the direction of travel at a track's last fix. */
enum class HeadingMarker {
    /** A translucent wedge, like a torch beam. */
    Cone,
    Arrow,

    /** A small dot just ahead of the position. */
    Dot,
    None,
}

/** How a track is drawn: an indigo line that runs from grey when slow, with green and red ends. */
data class TrackStyle(
    val lineColor: Color = Color(0xFF6366F1),
    val slowColor: Color = Color(0xFF9EA8A3),
    val fastColor: Color = Color(0xFF6366F1),
    val lineWidth: Dp = 4.dp,
    val line: LineStyle = LineStyle.Solid,
    val speed: SpeedStyle = SpeedStyle.Both,
    val markerColor: Color = Color(0xFF6366F1),
    val heading: HeadingMarker = HeadingMarker.Cone,
    val accuracyColor: Color = Color(0x336366F1),
    val showAccuracy: Boolean = true,
    val startColor: Color = Color(0xFF16A34A),
    val endColor: Color = Color(0xFFDC2626),
    val waypointColor: Color = Color(0xFF2563EB),
    val endMarkerRadius: Dp = 6.dp,
    /**
     * Whether the start and end are marked. Off for a track still being recorded, whose end is the user's position
     * and already has a marker of its own. Waypoints are marked either way.
     */
    val markEnds: Boolean = true,
)

/**
 * A recorded track drawn through [projection], on a live map ([Track]) or over a map image: the line, the speed along it, an accuracy circle and a heading marker at
 * the last fix, and markers at its ends and at [waypoints].
 *
 * **Only the ends and the waypoints are marked, never the track's own points**, and the ends not at all with
 * [TrackStyle.markEnds] off. A dense track marked point by point
 * becomes a band of rings that hides the line. The start is [TrackStyle.startColor] and the end
 * [TrackStyle.endColor]; when they would overlap, as on a loop, one marker shows both, the end with the start inside
 * it. Ends are drawn last, so nothing covers them.
 *
 * A single point, or none, with waypoints, has no line to have ends: each point is marked as a location.
 *
 * The accuracy circle and the heading marker are drawn only when the last fix carries an accuracy or a heading; an
 * unknown accuracy is not guessed at.
 */
fun DrawScope.drawTrack(
    points: List<TrackPoint>,
    projection: MapProjection,
    style: TrackStyle = TrackStyle(),
    waypoints: List<LatLon> = emptyList(),
) {
    val at = points.map { projection.project(it.position) }
    val last = points.lastOrNull()

    if (last != null && style.showAccuracy) {
        last.accuracy?.takeIf { it > 0 }?.let { metres ->
            drawCircle(style.accuracyColor, radius = (metres / projection.metresPerPixel).toFloat(), center = at.last())
        }
    }

    if (points.size > 1 && style.line != LineStyle.None) {
        val gradient = style.speed == SpeedStyle.Gradient || style.speed == SpeedStyle.Both
        val fastest = if (gradient) points.maxOf { it.speed ?: 0.0 } else 0.0
        val dash = if (style.line == LineStyle.Dotted) {
            PathEffect.dashPathEffect(floatArrayOf(10.dp.toPx(), 10.dp.toPx()), 0f)
        } else null
        for (i in 1 until points.size) {
            val color = if (gradient && fastest > 0) {
                lerp(style.slowColor, style.fastColor, ((points[i].speed ?: 0.0) / fastest).toFloat().coerceIn(0f, 1f))
            } else if (gradient) style.slowColor else style.lineColor
            drawLine(color, at[i - 1], at[i], strokeWidth = style.lineWidth.toPx(), cap = StrokeCap.Round, pathEffect = dash)
        }
    }

    if (points.size > 1 && (style.speed == SpeedStyle.Ghosting || style.speed == SpeedStyle.Both)) {
        for (i in 0 until points.size - 1) {
            // Strengthening toward the end, to 0.6 at the most recent.
            val alpha = (i + 1f) / points.size * 0.6f
            drawCircle(style.markerColor.copy(alpha = alpha), radius = 5.dp.toPx(), center = at[i])
        }
    }

    val heading = last?.heading
    if (heading != null && style.heading != HeadingMarker.None) {
        drawHeading(at.last(), heading - projection.bearing, style)
    }

    drawEnds(at, waypoints.map { projection.project(it) }, style)
}

/** The heading marker at [centre], [degrees] clockwise from the top of the image. */
internal fun DrawScope.drawHeading(centre: Offset, degrees: Float, style: TrackStyle) {
    val radians = (degrees - 90.0) * PI / 180.0
    fun ahead(distance: Float, turn: Double = 0.0) =
        Offset(centre.x + distance * cos(radians + turn).toFloat(), centre.y + distance * sin(radians + turn).toFloat())
    when (style.heading) {
        HeadingMarker.Cone -> {
            val reach = 32.dp.toPx()
            drawArc(
                color = style.markerColor.copy(alpha = 0.2f),
                startAngle = degrees - 90f - 20f,
                sweepAngle = 40f,
                useCenter = true,
                topLeft = Offset(centre.x - reach, centre.y - reach),
                size = Size(reach * 2, reach * 2),
            )
        }
        HeadingMarker.Arrow -> {
            val tip = ahead(16.dp.toPx())
            val width = 2.dp.toPx()
            drawLine(style.markerColor, centre, tip, strokeWidth = width, cap = StrokeCap.Round)
            val barb = 6.dp.toPx()
            for (side in listOf(PI - PI / 6, PI + PI / 6)) {
                val end = Offset(tip.x + barb * cos(radians + side).toFloat(), tip.y + barb * sin(radians + side).toFloat())
                drawLine(style.markerColor, tip, end, strokeWidth = width, cap = StrokeCap.Round)
            }
        }
        HeadingMarker.Dot -> drawCircle(style.markerColor, radius = 3.dp.toPx(), center = ahead(8.dp.toPx()))
        HeadingMarker.None -> Unit
    }
}

/** Waypoints first and the track's ends last, so the ends are never covered. */
private fun DrawScope.drawEnds(line: List<Offset>, waypoints: List<Offset>, style: TrackStyle) {
    val radius = style.endMarkerRadius.toPx()
    if (line.size <= 1) {
        // No line to have ends: every point is a location.
        for (point in waypoints + line) drawCircle(style.endColor, radius, point)
        return
    }
    for (point in waypoints) drawCircle(style.waypointColor, 4.dp.toPx(), point)
    if (!style.markEnds) return
    val start = line.first()
    val end = line.last()
    if (hypot(start.x - end.x, start.y - end.y) < radius * 2) {
        drawCircle(style.endColor, radius + 1.dp.toPx(), start)
        drawCircle(style.startColor, radius * 0.55f, start)
    } else {
        drawCircle(style.startColor, radius, start)
        drawCircle(style.endColor, radius, end)
    }
}
