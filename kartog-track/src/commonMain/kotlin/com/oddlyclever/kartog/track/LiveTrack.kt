package com.oddlyclever.kartog.track

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.coderwise.libs.mapview.LatLon
import com.coderwise.libs.mapview.MapOverlayScope
import com.oddlyclever.kartog.KartogMapState
import com.oddlyclever.kartog.projection

/**
 * A recorded track on a live map, drawn as [drawTrack] draws it on an image, in a `KartogMap`'s overlay. It follows
 * the map as it moves, and redraws as [points] grows during recording.
 */
@Composable
fun MapOverlayScope.Track(
    points: List<TrackPoint>,
    state: KartogMapState,
    style: TrackStyle = TrackStyle(),
    waypoints: List<LatLon> = emptyList(),
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val projection = remember(state, density) { projection(state.camera, density) }
    Canvas(modifier.fillMaxSize()) { drawTrack(points, projection, style, waypoints) }
}

/**
 * The user's position on a live map: a dot, an accuracy circle sized to the ground it covers, and a heading marker
 * pointing the way they face, turned with the map. [accuracy] is in metres and [heading] in degrees clockwise from
 * north; either may be unknown, and is then not drawn.
 */
@Composable
fun MapOverlayScope.LocationMarker(
    position: LatLon,
    state: KartogMapState,
    heading: Float? = null,
    accuracy: Double? = null,
    style: LocationStyle = LocationStyle(),
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val projection = remember(state, density) { projection(state.camera, density) }
    Canvas(modifier.fillMaxSize()) {
        val at = projection.project(position)
        if (accuracy != null && accuracy > 0) {
            drawCircle(style.accuracyColor, radius = (accuracy / projection.metresPerPixel).toFloat(), center = at)
        }
        if (heading != null && style.heading != HeadingMarker.None) {
            drawHeading(at, heading - projection.bearing, TrackStyle(markerColor = style.color, heading = style.heading))
        }
        drawCircle(style.ring, radius = style.radius.toPx() + 2.dp.toPx(), center = at)
        drawCircle(style.color, radius = style.radius.toPx(), center = at)
    }
}

/** How [LocationMarker] draws the user's position. */
@Immutable
data class LocationStyle(
    val color: Color = Color(0xFF2563EB),
    val ring: Color = Color.White,
    val radius: Dp = 7.dp,
    val accuracyColor: Color = Color(0x332563EB),
    val heading: HeadingMarker = HeadingMarker.Cone,
)
