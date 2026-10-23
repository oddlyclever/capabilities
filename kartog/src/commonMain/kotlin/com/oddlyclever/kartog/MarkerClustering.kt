package com.oddlyclever.kartog

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.coderwise.libs.mapview.LatLon
import kotlin.math.floor
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * Points that would be drawn on top of each other at one zoom, gathered so that one marker can stand for them.
 *
 * @property items The points in the group, in the order they were given. A point with nothing near it is a group of
 * one, so every point given is in exactly one group.
 * @property position The middle of the group, where its marker is drawn.
 */
@Immutable
data class PointCluster<T>(val items: List<T>, val position: LatLon) {
    /** How many points the group holds. */
    val size: Int get() = items.size

    /** Whether more than one point is drawn as this group's marker. */
    val isGroup: Boolean get() = items.size > 1
}

/**
 * [items] gathered into the groups whose markers would cover each other at [zoom]: those whose positions fall within
 * [radius] of each other on the map.
 *
 * Distances are measured on the Web Mercator plane map-view draws, a tile 256 dp across at zoom 0, so [radius] is in
 * the same units as a marker's size and the answer does not depend on the screen's density. Greedy and in order: each
 * point not yet in a group starts one and takes every later point within [radius] of it, so the same points at the
 * same zoom always group the same way, and a group is never wider than twice [radius]. Points either side of the
 * antimeridian are measured the short way round. A group's position is the middle of its points, taken the same way.
 *
 * Pure, and quadratic in the number of points: meant for the hundreds of markers a screen can show, not for a dataset.
 */
fun <T> clusterPoints(
    items: List<T>,
    zoom: Float,
    radius: Dp = MarkerClustering.DEFAULT_RADIUS,
    position: (T) -> LatLon,
): List<PointCluster<T>> {
    val world = WebMercator.WORLD_TILE * 2.0.pow(zoom.toDouble())
    val units = items.map { item -> position(item).let { WebMercator.x(it.lon) to WebMercator.y(it.lat) } }
    val taken = BooleanArray(items.size)
    val groups = mutableListOf<PointCluster<T>>()
    for (i in items.indices) {
        if (taken[i]) continue
        taken[i] = true
        val (seedX, seedY) = units[i]
        val members = mutableListOf(i)
        for (j in i + 1 until items.size) {
            if (taken[j]) continue
            val (x, y) = units[j]
            val dx = wrapped(x - seedX) * world
            val dy = (y - seedY) * world
            if (sqrt(dx * dx + dy * dy) <= radius.value) {
                taken[j] = true
                members += j
            }
        }
        // The middle, measured from the first point the short way round, so a group across the antimeridian stays on it.
        val meanX = seedX + members.map { wrapped(units[it].first - seedX) }.average()
        val meanY = members.map { units[it].second }.average()
        groups += PointCluster(
            items = members.map { items[it] },
            position = LatLon(WebMercator.lat(meanY), WebMercator.lon(meanX)),
        )
    }
    return groups
}

/** [dx] across the unit square the short way round the world, in `[-0.5, 0.5)`. */
private fun wrapped(dx: Double): Double = dx - floor(dx + 0.5)

/**
 * Draws the [KartogMap] markers that would cover each other as one marker for the group, regrouping as the map zooms.
 *
 * Off unless a map is given one: a map of a few markers, or of markers the app has spaced itself, has nothing to group.
 * Grouping is worked out again at every [step] of zoom rather than every frame of a pinch, which keeps a map of
 * hundreds of markers smooth and stops groups flickering apart and back together while the zoom passes a boundary. At
 * the closest zoom the map allows, nothing is grouped, since zooming no further can part points at the same spot; they
 * are drawn on top of each other there, as they would be with no grouping at all.
 *
 * @property radius How close two markers' positions must be, in dp at the current zoom, to be drawn as one. About the
 * width of a marker and its label.
 * @property step How much the zoom must change before the groups are worked out again.
 * @property zoomStep How much closer the map zooms when a group is tapped and the app gives no `onClusterClick`.
 * @property description What a screen reader says for a group. The app's words, since a count of "markers" means
 * nothing to its users; null says nothing.
 * @property content How a group is drawn, centred on its position. [DefaultClusterMarker] unless the app draws its own.
 */
@Immutable
class MarkerClustering(
    val radius: Dp = DEFAULT_RADIUS,
    val step: Float = 0.5f,
    val zoomStep: Float = 2f,
    val description: (PointCluster<MapMarker>) -> String? = { null },
    val content: @Composable (PointCluster<MapMarker>) -> Unit = { DefaultClusterMarker(it.size) },
) {
    init {
        require(radius > 0.dp) { "A clustering radius must be positive, not $radius" }
        require(step > 0f) { "A regrouping step must be positive, not $step" }
        require(zoomStep > 0f) { "A group's zoom step must be positive, not $zoomStep" }
    }

    /** The zoom the groups are worked out at for a camera at [zoom]: [zoom] rounded down to a [step]. Pure. */
    fun groupingZoom(zoom: Float): Float = floor(zoom / step) * step

    companion object {
        /** About a marker and its label across, so markers closer than this cover each other. */
        val DEFAULT_RADIUS: Dp = 40.dp
    }
}

/**
 * The markers [KartogMap] draws for [markers] at [zoom] under [clustering]: every marker as it is when there is no
 * clustering or the map is at the closest zoom [limits] allow, and otherwise the groups [clusterPoints] finds. Pure.
 */
internal fun clusteredMarkers(
    markers: List<MapMarker>,
    zoom: Float,
    clustering: MarkerClustering?,
    limits: CameraLimits,
): List<PointCluster<MapMarker>> =
    if (clustering == null || zoom >= limits.effectiveZoom.endInclusive) markers.map { PointCluster(listOf(it), it.position) }
    else clusterPoints(markers, clustering.groupingZoom(zoom), clustering.radius) { it.position }

/** A group's composition key, named for its first marker. Its own type, so it never equals a single marker's id. */
internal data class ClusterKey(val firstMarkerId: String)

/** The zoom a tapped group moves the map to: [MarkerClustering.zoomStep] closer, but no closer than [limits]. Pure. */
internal fun zoomIntoCluster(zoom: Float, clustering: MarkerClustering, limits: CameraLimits): Float =
    (zoom + clustering.zoomStep).coerceAtMost(limits.effectiveZoom.endInclusive)

/** The group marker drawn when a [MarkerClustering] gives no content of its own: a white-ringed dot with the count. */
@Composable
fun DefaultClusterMarker(count: Int, color: Color = Color(0xFF2563EB), size: Dp = 28.dp) {
    Box(
        Modifier.size(size).clip(CircleShape).background(color).border(2.dp, Color.White, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        BasicText(
            text = if (count > 99) "99+" else count.toString(),
            style = TextStyle(color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold),
            maxLines = 1,
        )
    }
}
