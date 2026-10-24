package com.oddlyclever.wayfinder

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import com.coderwise.libs.mapview.LatLon
import com.coderwise.libs.mapview.ZOOM_LIMITS
import kotlin.math.abs

/**
 * How far a [WayfinderMap]'s camera may go: a range of [zoom], and a box its [center] stays in, when there is one.
 *
 * map-view holds every camera to its own [ZOOM_LIMITS] and to nothing else, and its gestures move the camera
 * directly. So wayfinder holds a map to these by bringing the camera back inside them whenever it moves out, which can
 * show it past an edge for a frame before it is brought back. Holding the camera inside them in the first place has to
 * happen where map-view moves it, and is asked of map-view in coderwise/libs-mobile#5; until it lands, this is how
 * they are kept.
 *
 * The box limits where the middle of the map can go, as MapLibre's does, not what the map shows: zoomed out, the
 * edges of the screen can show ground outside it.
 *
 * @property zoom the zoom the map stays in, within map-view's [ZOOM_LIMITS].
 * @property center the box the middle of the map stays in, or null for anywhere. A box that crosses the antimeridian
 * has [GeoBounds.west] greater than [GeoBounds.east].
 */
@Immutable
data class CameraLimits(
    val zoom: ClosedFloatingPointRange<Float> = ZOOM_LIMITS,
    val center: GeoBounds? = null,
) {
    /** The zoom range kept to, [zoom] narrowed to map-view's [ZOOM_LIMITS]; map-view's own range when they miss. */
    val effectiveZoom: ClosedFloatingPointRange<Float>
        get() {
            val low = maxOf(zoom.start, ZOOM_LIMITS.start)
            val high = minOf(zoom.endInclusive, ZOOM_LIMITS.endInclusive)
            return if (low <= high) low..high else ZOOM_LIMITS
        }

    /** [zoom] kept inside the limits. Pure. */
    fun hold(zoom: Float): Float = zoom.coerceIn(effectiveZoom)

    /**
     * [point] kept inside [center], moved to the nearest edge when it is outside, or [point] itself when there is no
     * box. Outside a box that crosses the antimeridian, it goes to whichever side is nearer the short way round. Pure.
     */
    fun hold(point: LatLon): LatLon {
        val box = center ?: return point
        val lat = point.lat.coerceIn(box.south, box.north)
        val lon = when {
            !box.crossesAntimeridian -> point.lon.coerceIn(box.west, box.east)
            point.lon >= box.west || point.lon <= box.east -> point.lon
            degreesBetween(point.lon, box.west) <= degreesBetween(point.lon, box.east) -> box.west
            else -> box.east
        }
        return if (lat == point.lat && lon == point.lon) point else LatLon(lat, lon)
    }
}

/** The angle between two longitudes the short way round, in degrees. */
private fun degreesBetween(a: Double, b: Double): Double {
    val d = abs(a - b) % 360.0
    return if (d > 180.0) 360.0 - d else d
}

/** Brings [state]'s camera back inside [limits] whenever it moves out of them, or the limits change around it. */
@Composable
internal fun HoldCamera(state: WayfinderMapState, limits: CameraLimits) {
    val current by rememberUpdatedState(limits)
    LaunchedEffect(state) {
        snapshotFlow { Triple(state.center, state.zoom, current) }.collect { (center, zoom, held) ->
            val heldCenter = held.hold(center)
            val heldZoom = held.hold(zoom)
            if (heldCenter != center || heldZoom != zoom) state.camera.moveTo(heldCenter, heldZoom)
        }
    }
}
