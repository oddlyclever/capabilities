package com.oddlyclever.wayfinder

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.Density
import com.coderwise.libs.mapview.LatLon
import com.coderwise.libs.mapview.MapCameraState
import com.coderwise.libs.mapview.MapOverlayScope

/**
 * Where a coordinate falls on a map, live or drawn to an image, for drawing over it.
 *
 * Here in the base so that what draws over a map, a track or a location marker, draws the same way on a live map and
 * on an image without either module depending on the other.
 */
interface MapProjection {
    /** Where [point] falls, in pixels from the map's top left, with the bearing taken into account. */
    fun project(point: LatLon): Offset

    /** How many metres of ground a pixel covers at the middle of the map, for drawing a distance as a radius. */
    val metresPerPixel: Double

    /** What is at the top of the map, in degrees clockwise from north, for turning a heading onto the screen. */
    val bearing: Float
}

/**
 * This overlay's projection, read in the draw phase so whatever draws through it follows the map as it moves.
 * [camera] supplies the scale and the bearing, which the overlay scope does not.
 */
fun MapOverlayScope.projection(camera: MapCameraState, density: Density): MapProjection {
    val overlay = this
    return object : MapProjection {
        override fun project(point: LatLon): Offset = overlay.project(point)
        override val metresPerPixel: Double get() = camera.metersPerDp / density.density
        override val bearing: Float get() = camera.bearing
    }
}
