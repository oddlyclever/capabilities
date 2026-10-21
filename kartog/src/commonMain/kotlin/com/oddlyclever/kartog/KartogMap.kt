package com.oddlyclever.kartog

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.coderwise.libs.mapview.LatLon
import com.coderwise.libs.mapview.MapOverlayScope
import com.coderwise.libs.mapview.MapState
import com.coderwise.libs.mapview.MapView
import com.coderwise.libs.mapview.Polyline
import com.coderwise.libs.mapview.TileKey
import com.coderwise.libs.mapview.controls.Attribution
import com.coderwise.libs.mapview.controls.CompassTape
import com.coderwise.libs.mapview.controls.CourseUp
import com.coderwise.libs.mapview.controls.ScaleBar
import com.coderwise.libs.mapview.controls.ScaleUnits
import com.coderwise.libs.mapview.controls.ZoomButtons
import com.coderwise.libs.mapview.mapGestures

/**
 * A map, with map-view drawing it: tiles from [layers], [polylines] and [markers] over them, and the ornaments a map
 * screen needs around them.
 *
 * Layers come from the kartog modules that draw them, `kartog-vector` and `kartog-raster` for basemaps, each a
 * [KartogLayer]; they are drawn in order. [overlay] draws anything else placed by coordinate, over the lines and
 * under the markers: a GeoJSON layer, a track, the user's position.
 *
 * Everything the app reads or drives is on [state]: the camera, whether the user has hold of the map, north-up or
 * course-up, and the visible bounds, which this keeps current as the map moves or is resized.
 */
@Composable
fun KartogMap(
    state: KartogMapState,
    modifier: Modifier = Modifier,
    layers: List<KartogLayer> = emptyList(),
    markers: List<MapMarker> = emptyList(),
    polylines: List<MapPolyline> = emptyList(),
    gestures: MapGestures = MapGestures(),
    ornaments: MapOrnaments = MapOrnaments(),
    onTap: ((LatLon) -> Unit)? = null,
    onMarkerClick: ((MapMarker) -> Unit)? = null,
    overlay: @Composable MapOverlayScope.() -> Unit = {},
) {
    val placed = layers.map { layer -> key(layer) { layer.place(state) } }
    val density = LocalDensity.current
    TrackInteraction(state)
    CourseUp(state.camera, state.heading, enabled = state.orientation == Orientation.CourseUp)

    val size = remember { mutableStateOf(IntSize.Zero) }
    val currentDensity = rememberUpdatedState(density)
    LaunchedEffect(state) {
        // Recomputed whenever the camera moves or the map is resized.
        snapshotFlow { listOf(state.center, state.zoom, state.bearing, size.value) }.collect {
            val measured = size.value
            if (measured != IntSize.Zero) state.visibleBounds = state.camera.visibleBounds(measured, currentDensity.value)
        }
    }

    Box(modifier) {
        // Gestures on a box around the map, as map-view's own sample does, so the map and every layer on it move
        // together and a tap that no marker or line took lands on the map.
        val gestureModifier = Modifier
            .matchParentSize()
            .onSizeChanged { size.value = it }
            .let { base ->
                if (!gestures.enabled) base
                else base.mapGestures(
                    state.camera,
                    // Course-up turns the map itself; a twist would fight it.
                    rotatable = gestures.rotate && state.orientation == Orientation.NorthUp,
                    onTap = onTap,
                )
            }
        Box(gestureModifier) {
            MapView(state.camera, Modifier.matchParentSize()) {
                for (layer in placed) for (content in layer.contents) layer(layer.state, content)
                overlay {
                    for (line in polylines) {
                        key(line.id) { Polyline(line.points, line.color, width = line.width, onClick = line.onClick) }
                    }
                    overlay()
                    for (marker in markers) {
                        key(marker.id) {
                            Box(
                                Modifier
                                    .at(marker.position, marker.anchor)
                                    .then(if (onMarkerClick != null) Modifier.clickable { onMarkerClick(marker) } else Modifier)
                                    .then(marker.description?.let { text -> Modifier.semantics { contentDescription = text } } ?: Modifier)
                            ) { marker.content() }
                        }
                    }
                }
            }
        }
        Ornaments(state, ornaments, placed.mapNotNull { it.attribution }.distinct())
    }
}

/** What the user can do to the map. */
@Immutable
data class MapGestures(
    /** Pan, pinch, fling, double-tap and the mouse wheel. Off makes a map the user can look at but not move. */
    val enabled: Boolean = true,
    /** Two-finger turning. Off in course-up regardless, since the heading turns the map then. */
    val rotate: Boolean = true,
)

/** The controls drawn over a map, each switched on or off and placed in a corner. */
@Immutable
data class MapOrnaments(
    val compass: Boolean = true,
    val compassAlignment: Alignment = Alignment.TopCenter,
    /** A button that switches between north-up and course-up. */
    val orientationToggle: Boolean = false,
    val orientationToggleAlignment: Alignment = Alignment.TopEnd,
    val scaleBar: Boolean = true,
    val scaleBarAlignment: Alignment = Alignment.BottomStart,
    val scaleUnits: ScaleUnits = ScaleUnits.Metric,
    val zoomButtons: Boolean = false,
    val zoomButtonsAlignment: Alignment = Alignment.CenterEnd,
    /** The credits the layers' sources ask for. Leave it on wherever a source's licence requires attribution. */
    val attribution: Boolean = true,
    val attributionAlignment: Alignment = Alignment.BottomEnd,
    /** How far the ornaments sit from the map's edges, to clear an app bar or a sheet. */
    val padding: Dp = 8.dp,
)

/**
 * Something placed on the map at [position]: a pin, a photo, a label. [content] is drawn with its [anchor] on the
 * position, the tip of a pin with [Alignment.BottomCenter], and is the same size at every zoom.
 *
 * [id] keeps a marker's state across recompositions and identifies it to [KartogMap]'s `onMarkerClick`.
 * [description] is what a screen reader says for it.
 */
class MapMarker(
    val id: String,
    val position: LatLon,
    val anchor: Alignment = Alignment.Center,
    val description: String? = null,
    val content: @Composable () -> Unit = { DefaultMarker() },
)

/** A line through [points] in order, drawn [width] wide at every zoom. [onClick] makes it tappable. */
class MapPolyline(
    val id: String,
    val points: List<LatLon>,
    val color: Color,
    val width: Dp = 4.dp,
    val onClick: (() -> Unit)? = null,
)

/** The marker drawn when a [MapMarker] gives no content of its own: a white-ringed dot. */
@Composable
fun DefaultMarker(color: Color = Color(0xFF2563EB), size: Dp = 14.dp) {
    Box(Modifier.size(size).clip(CircleShape).background(Color.White).padding(2.dp).clip(CircleShape).background(color))
}

/**
 * A layer of tiles on a [KartogMap]. The kartog modules that draw tiles provide them: `vectorBasemap` in
 * `kartog-vector`, `rasterBasemap` in `kartog-raster`.
 *
 * Implementations remember their tile state in [place], which [KartogMap] calls during composition, once per layer.
 */
fun interface KartogLayer {
    @Composable
    fun place(state: KartogMapState): PlacedLayer
}

/**
 * A layer as placed on the map: the tiles in [state], drawn by each of [contents] as a layer of its own, in order. A
 * vector basemap is two, ground then names, so every name is drawn over every tile. [attribution] is the credit its
 * source asks for.
 */
class PlacedLayer(
    val state: MapState<*>,
    val contents: List<@Composable (TileKey) -> Unit>,
    val attribution: String? = null,
)

@Composable
private fun BoxScope.Ornaments(state: KartogMapState, ornaments: MapOrnaments, credits: List<String>) {
    val edge = Modifier.padding(ornaments.padding)
    if (ornaments.compass) {
        CompassTape(
            state.camera,
            edge.align(ornaments.compassAlignment),
            onClick = { state.orientation = Orientation.NorthUp; state.camera.rotateTo(0f) },
        )
    }
    if (ornaments.orientationToggle) {
        val courseUp = state.orientation == Orientation.CourseUp
        Box(
            edge.align(ornaments.orientationToggleAlignment)
                .size(40.dp)
                .clip(CircleShape)
                .background(if (courseUp) Color(0xFF2563EB) else Color.White)
                .clickable {
                    state.orientation = if (courseUp) Orientation.NorthUp else Orientation.CourseUp
                    if (courseUp) state.camera.rotateTo(0f)
                }
                .semantics { contentDescription = if (courseUp) "Course up: tap for north up" else "North up: tap for course up" },
            contentAlignment = Alignment.Center,
        ) {
            Box(Modifier.size(10.dp).clip(CircleShape).background(if (courseUp) Color.White else Color(0xFF2563EB)))
        }
    }
    if (ornaments.scaleBar) ScaleBar(state.camera, edge.align(ornaments.scaleBarAlignment), units = ornaments.scaleUnits)
    if (ornaments.zoomButtons) ZoomButtons(state.camera, edge.align(ornaments.zoomButtonsAlignment))
    if (ornaments.attribution && credits.isNotEmpty()) {
        Column(edge.align(ornaments.attributionAlignment)) { Attribution(credits.joinToString(" · ")) }
    }
}
