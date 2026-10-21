package com.oddlyclever.kartog

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import com.coderwise.libs.mapview.LatLon
import com.coderwise.libs.mapview.MapCameraState
import com.coderwise.libs.mapview.flyTo
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest

/** Which way is up on a [KartogMap]. */
enum class Orientation {
    /** North stays at the top, unless the user turns the map. */
    NorthUp,

    /** The map turns so the direction of travel, [KartogMapState.heading], is at the top. */
    CourseUp,
}

/**
 * Everything about a [KartogMap] that the app reads or drives: where the camera is, whether the user has hold of it,
 * which way is up, and what is on screen.
 *
 * The camera itself is map-view's [MapCameraState], exposed as [camera] for anything kartog does not wrap. Make one
 * with [rememberKartogMapState], or construct it directly to keep it in a view model.
 */
@Stable
class KartogMapState(center: LatLon = LatLon(0.0, 0.0), zoom: Float = 3f, bearing: Float = 0f) {

    /** map-view's camera, for its own controls and helpers. */
    val camera = MapCameraState(center, zoom, bearing)

    /** The point at the middle of the map. */
    val center: LatLon get() = camera.center

    /** The current zoom, fractional. */
    val zoom: Float get() = camera.zoom

    /** What is at the top of the map, in degrees clockwise from north. */
    val bearing: Float get() = camera.bearing

    /**
     * Whether the user has hold of the map, or let go of it less than [SETTLE_MILLIS] ago.
     *
     * The settle is what keeps an app from snapping the map back to the user's position the instant a pan ends:
     * people often lift a finger, look, and carry on panning. Anything that moves the camera on the app's behalf
     * should wait while this is true.
     */
    var isInteracting by mutableStateOf(false)
        internal set

    /** North-up or course-up. In [Orientation.CourseUp] the map follows [heading] and the user cannot turn it. */
    var orientation by mutableStateOf(Orientation.NorthUp)

    /** The direction of travel in degrees clockwise from north, or null when it is not known. Course-up follows it. */
    var heading by mutableStateOf<Float?>(null)

    /** What the map shows, as the smallest box of latitude and longitude that holds all of it. Null until laid out. */
    var visibleBounds by mutableStateOf<GeoBounds?>(null)
        internal set

    /** Puts the camera at [center] and [zoom] at once. */
    fun moveTo(center: LatLon, zoom: Float = this.zoom) = camera.moveTo(center, zoom)

    /** Puts the camera at a fitted [position] at once, bearing included. */
    fun moveTo(position: CameraPosition) = camera.moveTo(position)

    /** Animates the camera to [center] and [zoom], the short way round the world. */
    suspend fun animateTo(center: LatLon, zoom: Float = this.zoom, animationSpec: AnimationSpec<Float> = spring()) =
        camera.flyTo(center, zoom, animationSpec)

    /** Animates the camera to a fitted [position], then turns it to the position's bearing. */
    suspend fun animateTo(position: CameraPosition, animationSpec: AnimationSpec<Float> = spring()) =
        camera.flyTo(position, animationSpec)

    /** One level closer, animated. */
    suspend fun zoomIn() = camera.zoomIn()

    /** One level farther, animated. */
    suspend fun zoomOut() = camera.zoomOut()

    companion object {
        /** How long after the user lets go [isInteracting] stays true. */
        const val SETTLE_MILLIS = 2_000L
    }
}

/** A [KartogMapState] that lives as long as the composition, starting at [center] and [zoom]. */
@Composable
fun rememberKartogMapState(center: LatLon = LatLon(0.0, 0.0), zoom: Float = 3f, bearing: Float = 0f): KartogMapState =
    remember { KartogMapState(center, zoom, bearing) }

/** Keeps [KartogMapState.isInteracting] following map-view's gestures, held for the settle after they end. */
@Composable
internal fun TrackInteraction(state: KartogMapState) {
    LaunchedEffect(state) {
        snapshotFlow { state.camera.isInteracting }.collectLatest { touching ->
            if (touching) {
                state.isInteracting = true
            } else {
                delay(KartogMapState.SETTLE_MILLIS)
                state.isInteracting = false
            }
        }
    }
}
