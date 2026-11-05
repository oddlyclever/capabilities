package com.oddlyclever.wayfinder

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.coderwise.libs.mapview.LatLon
import com.coderwise.libs.mapview.MapOverlayScope
import com.coderwise.libs.mapview.MapState
import com.coderwise.libs.mapview.MapView
import com.coderwise.libs.mapview.Polyline
import com.coderwise.libs.mapview.TileKey
import com.coderwise.libs.mapview.mapGestures
import com.oddlyclever.wayfinder.controls.CompassTape
import com.oddlyclever.wayfinder.controls.CourseUp
import com.oddlyclever.wayfinder.controls.NorthButton
import com.oddlyclever.wayfinder.controls.ScaleBar
import com.oddlyclever.wayfinder.controls.ZoomButtons

/**
 * A map, with map-view drawing it: tiles from [layers], [polylines] and [markers] over them, and the ornaments a map
 * screen needs around them.
 *
 * Layers come from the wayfinder modules that draw them, `wayfinder-vector` and `wayfinder-raster` for basemaps, each a
 * [MapLayer]; they are drawn in order. [overlay] draws anything else placed by coordinate, over the lines and
 * under the markers: a GeoJSON layer, a track, the user's position.
 *
 * Everything the app reads or drives is on [state]: the camera, whether the user has hold of the map, north-up or
 * course-up, and the visible bounds, which this keeps current as the map moves or is resized.
 *
 * The credits of the layers' sources are shown as [MapOrnaments.attributionStyle] says, collapsing to an "i" by
 * default. [onAttributionClick] runs when they are tapped, to open the page behind them, such as
 * openstreetmap.org/copyright.
 *
 * The camera is kept inside [limits]: a range of zoom, and a box for the middle of the map when one is given. Neither
 * a gesture nor a move on [state] takes it out of them for longer than a frame.
 *
 * With [clustering], markers that would cover each other at the current zoom are drawn as one marker for the group,
 * regrouped as the map zooms. Tapping a group runs [onClusterClick], or without one zooms the map in on the group;
 * [onMarkerClick] is only for single markers. Without [clustering], every marker is drawn where it is.
 */
@Composable
fun WayfinderMap(
    state: WayfinderMapState,
    modifier: Modifier = Modifier,
    layers: List<MapLayer> = emptyList(),
    markers: List<MapMarker> = emptyList(),
    polylines: List<MapPolyline> = emptyList(),
    gestures: MapGestures = MapGestures(),
    ornaments: MapOrnaments = MapOrnaments(),
    limits: CameraLimits = CameraLimits(),
    onTap: ((LatLon) -> Unit)? = null,
    onMarkerClick: ((MapMarker) -> Unit)? = null,
    onAttributionClick: (() -> Unit)? = null,
    clustering: MarkerClustering? = null,
    onClusterClick: ((PointCluster<MapMarker>) -> Unit)? = null,
    overlay: @Composable MapOverlayScope.() -> Unit = {},
) {
    val placed = layers.map { layer -> key(layer) { layer.place(state) } }
    // Grouped again only when the zoom crosses a step, or reaches the closest zoom, where nothing is grouped. The zoom
    // is read through derived state, so a pinch recomposes the map once per step rather than once per frame; and the
    // groups are keyed on what decides them, so a MarkerClustering built inline does not regroup at every composition.
    val closest = limits.effectiveZoom.endInclusive
    val currentClustering by rememberUpdatedState(clustering)
    val groupingZoom by remember(state, closest) {
        derivedStateOf { currentClustering?.let { if (state.zoom >= closest) closest else it.groupingZoom(state.zoom) } }
    }
    val drawn = remember(markers, clustering?.radius, clustering?.step, closest, groupingZoom) {
        clusteredMarkers(markers, groupingZoom ?: closest, clustering, limits)
    }
    val density = LocalDensity.current
    TrackInteraction(state)
    HoldCamera(state, limits)
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
                    for (group in drawn) {
                        val marker = group.items.first()
                        if (!group.isCluster || clustering == null) {
                            key(marker.id) {
                                Box(
                                    Modifier
                                        .at(marker.position, marker.anchor)
                                        .then(if (onMarkerClick != null) Modifier.clickable { onMarkerClick(marker) } else Modifier)
                                        .then(marker.description?.let { text -> Modifier.semantics { contentDescription = text } } ?: Modifier)
                                ) { marker.content() }
                            }
                        } else {
                            // Keyed on its first marker, so a group that only gains or loses members keeps its state.
                            key(ClusterKey(marker.id)) {
                                Box(
                                    Modifier
                                        .at(group.position, Alignment.Center)
                                        .clickable {
                                            if (onClusterClick != null) onClusterClick(group)
                                            else state.moveTo(group.position, zoomIntoCluster(state.zoom, clustering, limits))
                                        }
                                        .then(clustering.description(group)?.let { text -> Modifier.semantics { contentDescription = text } } ?: Modifier)
                                ) { clustering.content(group) }
                            }
                        }
                    }
                }
            }
        }
        Ornaments(state, ornaments, placed.mapNotNull { it.attribution }.distinct(), onAttributionClick)
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

/** How a map's compass is drawn. */
enum class CompassStyle {
    /** A strip of headings across the map, for a map someone navigates by. */
    Tape,

    /**
     * A small needle in a circle that always points north, for a map someone looks around on. Pressing it turns the
     * map back to north-up. It does not leave course-up, where the heading turns the map straight back.
     */
    Rosette,
}

/** The controls drawn over a map, each switched on or off and placed in a corner. */
@Immutable
data class MapOrnaments(
    val compass: Boolean = true,
    val compassAlignment: Alignment = Alignment.TopCenter,
    val compassStyle: CompassStyle = CompassStyle.Tape,
    /**
     * A button that switches between north-up and course-up. It shows a compass needle in north-up and a navigation
     * arrowhead in course-up. With [CompassStyle.Tape] at the top, which spans the map's width, put it somewhere other
     * than a top corner, or it sits on the tape's end. A screen that wants it in a layout of its own, such as a top bar,
     * leaves this off and draws [OrientationToggle] there.
     */
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
    /** In full throughout, or in full when the map appears and then behind an "i". */
    val attributionStyle: AttributionStyle = AttributionStyle.Collapsible,
    /** The credits' text size. Small is fine as long as it stays readable; 9 sp is about as small as reads. */
    val attributionTextSize: TextUnit = 9.sp,
    /** How far the ornaments sit from the map's edges, to clear an app bar or a sheet. */
    val padding: Dp = 8.dp,
    /**
     * The app's own words for the ornaments, over wayfinder's. Every field is null unless set, and wayfinder's own words
     * in the device's language fill the rest.
     */
    val labels: MapOrnamentLabels = MapOrnamentLabels(),
)

/**
 * Something placed on the map at [position]: a pin, a photo, a label. [content] is drawn with its [anchor] on the
 * position, the tip of a pin with [Alignment.BottomCenter], and is the same size at every zoom.
 *
 * [id] keeps a marker's state across recompositions and identifies it to [WayfinderMap]'s `onMarkerClick`.
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
 * A layer of tiles on a [WayfinderMap]. The wayfinder modules that draw tiles provide them: `vectorBasemap` in
 * `wayfinder-vector`, `rasterBasemap` in `wayfinder-raster`.
 *
 * Implementations remember their tile state in [place], which [WayfinderMap] calls during composition, once per layer.
 * The map keys that state on the layer itself, by `equals`, so a layer that compares equal across recompositions keeps
 * its tiles and one that does not starts again with none. The layers the wayfinder modules return are equal whenever
 * they were made from equal arguments; a layer of your own written as a lambda is equal only to itself, so make it
 * once.
 */
fun interface MapLayer {
    @Composable
    fun place(state: WayfinderMapState): PlacedLayer
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
private fun BoxScope.Ornaments(state: WayfinderMapState, ornaments: MapOrnaments, credits: List<String>, onAttributionClick: (() -> Unit)?) {
    val edge = Modifier.padding(ornaments.padding)
    val words = ornaments.labels.words()
    if (ornaments.compass) {
        when (ornaments.compassStyle) {
            CompassStyle.Tape -> MapCompassTape(state, edge.align(ornaments.compassAlignment), labels = ornaments.labels)
            CompassStyle.Rosette ->
                NorthButton(state.camera, edge.align(ornaments.compassAlignment), alwaysVisible = true, label = words.north)
        }
    }
    if (ornaments.orientationToggle) {
        OrientationToggle(state, edge.align(ornaments.orientationToggleAlignment), labels = ornaments.labels)
    }
    if (ornaments.scaleBar) ScaleBar(state.camera, edge.align(ornaments.scaleBarAlignment), units = ornaments.scaleUnits)
    if (ornaments.zoomButtons) {
        ZoomButtons(state.camera, edge.align(ornaments.zoomButtonsAlignment), words = words)
    }
    if (ornaments.attribution && credits.isNotEmpty()) {
        MapAttribution(
            credits.joinToString(" · "),
            state.camera,
            ornaments.attributionStyle,
            ornaments.attributionTextSize,
            onAttributionClick,
            edge.align(ornaments.attributionAlignment),
            creditsLabel = words.credits,
        )
    }
}

/**
 * The compass tape [MapOrnaments] draws, for a screen that places it in its own layout, such as between the buttons of
 * a top bar. It takes the width it is given. Tapping it turns the map back to north-up. [background] is the tape's
 * own plate; pass [Color.Transparent] to draw it on a plate of the screen's. Its name to a screen reader and its points
 * are wayfinder's own in the device's language, unless [labels] sets them; pass the same labels as the map's
 * [MapOrnaments].
 */
@Composable
fun MapCompassTape(
    state: WayfinderMapState,
    modifier: Modifier = Modifier,
    background: Color = Color.White.copy(alpha = 0.85f),
    labels: MapOrnamentLabels? = null,
) {
    val words = labels.words()
    CompassTape(
        state.camera,
        modifier,
        onClick = { state.orientation = Orientation.NorthUp; state.camera.rotateTo(0f) },
        background = background,
        label = words.compass,
        points = words.compassPoints,
    )
}

/**
 * The north-up/course-up button [MapOrnaments] draws, for a screen that places it in its own layout, such as at the
 * end of a top bar. It is [size] across; give it the size of the buttons beside it. What a screen reader says for it
 * is wayfinder's own in the device's language, unless [labels] sets it.
 */
@Composable
fun OrientationToggle(
    state: WayfinderMapState,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    labels: MapOrnamentLabels? = null,
) {
    val words = labels.words()
    val courseUp = state.orientation == Orientation.CourseUp
    Box(
        modifier
            .size(size)
            .clip(CircleShape)
            .background(if (courseUp) Color(0xFF2563EB) else Color.White)
            .clickable {
                state.orientation = if (courseUp) Orientation.NorthUp else Orientation.CourseUp
                if (courseUp) state.camera.rotateTo(0f)
            }
            .semantics { contentDescription = if (courseUp) words.courseUp else words.northUp },
        contentAlignment = Alignment.Center,
    ) {
        OrientationIcon(courseUp = courseUp, modifier = Modifier.size(size * 0.55f))
    }
}

/**
 * What the orientation toggle shows: in north-up, a compass needle, its north half blue and its south half grey; in
 * course-up, a white navigation arrowhead, the map turning with the heading. It replaced a bare dot, which said nothing
 * about what the button does. Drawn rather than taken from an icon set, so the module needs no icon dependency. Public
 * for a screen that draws its own button around it.
 */
@Composable
fun OrientationIcon(courseUp: Boolean, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        if (courseUp) {
            // A navigation arrowhead pointing up, with a notch at its base.
            val arrow = Path().apply {
                moveTo(w * 0.5f, h * 0.08f)
                lineTo(w * 0.86f, h * 0.9f)
                lineTo(w * 0.5f, h * 0.7f)
                lineTo(w * 0.14f, h * 0.9f)
                close()
            }
            drawPath(arrow, Color.White)
        } else {
            // A compass needle: north half blue, south half grey.
            val north = Path().apply {
                moveTo(w * 0.5f, h * 0.04f)
                lineTo(w * 0.72f, h * 0.5f)
                lineTo(w * 0.28f, h * 0.5f)
                close()
            }
            val south = Path().apply {
                moveTo(w * 0.28f, h * 0.5f)
                lineTo(w * 0.72f, h * 0.5f)
                lineTo(w * 0.5f, h * 0.96f)
                close()
            }
            drawPath(north, Color(0xFF2563EB))
            drawPath(south, Color(0xFF9CA3AF))
        }
    }
}
