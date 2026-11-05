// Derived from map-view-geojson 0.3.0, part of map-view (https://github.com/coderwise/libs-mobile), licensed under
// the Apache License 2.0. Modified: moved into com.oddlyclever.wayfinder.geojson, renamed from GeoJson to GeoJsonLayer
// beside the layer that takes text, without its modifier, and drawing aughtone-types' GeoFeatureCollection in place of
// map-view-geojson's own model. See the module README.

package com.oddlyclever.wayfinder.geojson

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.coderwise.libs.mapview.LatLon
import com.coderwise.libs.mapview.MapOverlayScope
import com.coderwise.libs.mapview.toWorld
import io.github.aughtone.types.geo.GeoFeature
import io.github.aughtone.types.geo.GeoFeatureCollection
import kotlin.math.round
import kotlin.math.sqrt

/**
 * GeoJSON already in aughtone-types' model, drawn on a `WayfinderMap`, in its overlay: [data]'s points, lines and
 * polygons, in [style].
 *
 * ```
 * WayfinderMap(state, layers = listOf(basemap)) {
 *     GeoJsonLayer(coverage, style = GeoJsonStyle(fill = Color(0x330066FF), stroke = Color.Blue))
 *     GeoJsonLayer(trails, minZoom = 12f, onFeatureClick = { feature -> select(feature.stringProperty("name")) })
 * }
 * ```
 *
 * This is the overload for a collection an app built in code or decoded itself; GeoJSON as text goes to the other
 * `GeoJsonLayer`, which parses it and draws it here.
 *
 * [data] is projected once and kept for as long as it is the same, so only the camera's affine runs per frame, in the
 * draw pass: the map pans and pinches without recomposing this. Rings are filled by the even-odd rule, so a polygon's
 * holes are holes and a multi-polygon is its polygons; and a shape across the antimeridian stays in one piece. Altitude
 * is ignored.
 *
 * One call is one layer: one style, one `minZoom`, one `onFeatureClick`. Layers are drawn in the order they are
 * declared.
 *
 * @param minZoom the layer is not drawn, and cannot be touched, while the map is zoomed out further than this.
 * @param touchWidth how close a touch has to be to a feature to count as on it; a touch inside a polygon always is. A
 *   touch in a hole is not.
 * @param onFeatureClick gets the feature nearest the tap. Like every shape of the map's, it takes the tap from the
 *   nearest of them, which need not be the top one, and a tap it misses goes on to whatever is under it, the map
 *   included.
 */
@Composable
fun MapOverlayScope.GeoJsonLayer(
    data: GeoFeatureCollection,
    style: GeoJsonStyle = GeoJsonStyle(),
    minZoom: Float = 0f,
    touchWidth: Dp = 24.dp,
    onFeatureClick: ((GeoFeature) -> Unit)? = null,
) {
    val overlay = this
    val shapes = remember(data) { data.toShapes() }
    val scratch = remember { Scratch() }
    val projection = remember(overlay) { OverlayProjection(overlay) }

    // The tap is told how far a touch is, and later that it was one. What it was nearest to is worked out in the first
    // and read in the second.
    val touched = remember { Touched() }
    val gate = rememberUpdatedState(minZoom)
    val click = rememberUpdatedState(onFeatureClick)
    val distance = remember(overlay, shapes) {
        { at: Offset -> overlay.pixelsTo(shapes, at, gate.value, touched) }
    }
    val onClick = remember { { touched.feature?.let { click.value?.invoke(it) }; Unit } }

    val sized = Modifier.fillMaxSize()
    Canvas(if (onFeatureClick == null) sized else sized.mapTap(distance, touchWidth, onClick)) {
        if (shownAt(projection.zoom, minZoom)) drawShapes(shapes, style, projection, scratch)
    }
}

private class OverlayProjection(private val overlay: MapOverlayScope) : GeoJsonProjection {
    override val zoom get() = overlay.zoom
    override fun trace(path: Path, world: DoubleArray) = overlay.trace(path, world)
}

private class Touched {
    var feature: GeoFeature? = null
}

/** How far in pixels the nearest feature is from [at], remembering which it is in [touched]. */
private fun MapOverlayScope.pixelsTo(shapes: Shapes, at: Offset, minZoom: Float, touched: Touched): Float {
    touched.feature = null
    if (!shownAt(zoom, minZoom)) return Float.MAX_VALUE

    val here = world(unproject(at))
    val ahead = world(unproject(at + Offset(PROBE, 0f)))
    // The map is its world scaled and turned, so a few pixels along the screen are the same distance in the world
    // however it is turned, and that is all a pixel is.
    val across = ahead[0] - here[0]
    val perPixel = sqrt((across - round(across)).let { it * it } + (ahead[1] - here[1]).let { it * it }) / PROBE
    if (!(perPixel > 0.0)) return Float.MAX_VALUE

    val nearest = shapes.nearest(here[0], here[1]) ?: return Float.MAX_VALUE
    touched.feature = nearest.feature
    return (nearest.distance / perPixel).toFloat()
}

private fun world(at: LatLon) = listOf(at).toWorld()

/** Far enough that the difference is not lost to rounding, near enough to be the same place. */
private const val PROBE = 64f
