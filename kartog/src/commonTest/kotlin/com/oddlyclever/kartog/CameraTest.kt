package com.oddlyclever.kartog

import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.coderwise.libs.mapview.LatLon
import com.coderwise.libs.mapview.MapCameraState
import com.coderwise.libs.mapview.ZOOM_LIMITS
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Pins kartog's camera helpers: that a fit frames every point inside its padding at any bearing and across the
 * antimeridian, that kartog's copy of Web Mercator agrees with map-view's, and that the screen position follows
 * map-view's conventions for bearing and size.
 */
class CameraTest {

    private val density = Density(2f)
    private val size = IntSize(800, 600)

    @Test
    fun webMercatorAgreesWithMapView() {
        // MapCameraState stores its centre in map-view's own Mercator and reads it back through it, so a round trip
        // through the camera and one through kartog's copy must land on the same coordinate exactly.
        for (point in listOf(LatLon(43.65, -79.38), LatLon(-33.87, 151.21), LatLon(84.9, 179.9), LatLon(-84.9, -179.9))) {
            val camera = MapCameraState(center = point)
            val ours = LatLon(WebMercator.lat(WebMercator.y(point.lat)), WebMercator.lon(WebMercator.x(point.lon)))
            assertEquals(camera.center, ours, "map-view read $point back as ${camera.center}, kartog as $ours")
        }
    }

    @Test
    fun fitFramesEveryPointInsideThePadding() {
        val points = listOf(LatLon(43.60, -79.50), LatLon(43.75, -79.20), LatLon(43.70, -79.45))
        for (bearing in listOf(0f, 37f, 90f, 200f)) {
            assertFramed(points, bearing)
        }
    }

    @Test
    fun fitCrossesTheAntimeridianTheShortWay() {
        val points = listOf(LatLon(-17.7, 178.4), LatLon(-13.8, -172.1))
        val position = fit(points, size, density)
        assertTrue(abs(position.center.lon) > 170, "centre should be over the Pacific, was ${position.center}")
        assertTrue(position.zoom > 3f, "a 10-degree span should not be fitted at world scale, zoom was ${position.zoom}")
        assertFramed(points, 0f)
    }

    @Test
    fun fitTurnsALongTrackAcrossAWideScreenWhenEastIsUp() {
        val northSouth = listOf(LatLon(40.0, 10.0), LatLon(50.0, 10.0))
        val wide = IntSize(1000, 200)
        val northUp = fit(northSouth, wide, density).zoom
        val eastUp = fit(northSouth, wide, density, bearing = 90f).zoom
        assertTrue(eastUp > northUp, "east-up should fit the track across the screen: $eastUp vs north-up $northUp")
    }

    @Test
    fun fitPutsASinglePointAtTheMaximumZoom() {
        val point = LatLon(51.5, -0.12)
        val position = fit(listOf(point), size, density)
        assertEquals(DEFAULT_FIT_MAX_ZOOM, position.zoom, "default maximum")
        assertEquals(12f, fit(listOf(point), size, density, maxZoom = 12f).zoom, "given maximum")
        assertTrue(
            abs(position.center.lat - point.lat) < 1e-9 && abs(position.center.lon - point.lon) < 1e-9,
            "a single point is the centre, was ${position.center}",
        )
    }

    @Test
    fun fitWithNoRoomFallsBackToTheFarthestZoom() {
        val position = fit(listOf(LatLon(0.0, 0.0), LatLon(1.0, 1.0)), IntSize(40, 40), density, padding = 20.dp)
        assertEquals(ZOOM_LIMITS.start, position.zoom, "no room inside the padding, zoom was ${position.zoom}")
    }

    @Test
    fun fitRefusesNoPoints() {
        assertFailsWith<IllegalArgumentException> { fit(emptyList(), size, density) }
    }

    @Test
    fun moveToTakesTheBearingToo() {
        val camera = MapCameraState()
        camera.moveTo(CameraPosition(LatLon(10.0, 20.0), zoom = 8f, bearing = 45f))
        assertEquals(8f, camera.zoom, "zoom")
        assertEquals(45f, camera.bearing, "bearing")
        assertTrue(abs(camera.center.lat - 10.0) < 1e-9 && abs(camera.center.lon - 20.0) < 1e-9, "centre ${camera.center}")
    }

    @Test
    fun screenPositionPutsTheCentreInTheMiddle() {
        val camera = MapCameraState(center = LatLon(43.65, -79.38), zoom = 12f)
        val at = camera.screenPosition(camera.center, size, density)
        assertTrue(abs(at.x - 400f) < 0.01f && abs(at.y - 300f) < 0.01f, "centre at $at")
    }

    @Test
    fun screenPositionPutsEastAtTheTopWhenTheBearingIs90() {
        val camera = MapCameraState(center = LatLon(0.0, 0.0), zoom = 10f, bearing = 90f)
        val east = camera.screenPosition(LatLon(0.0, 0.01), size, density)
        assertTrue(east.y < 300f && abs(east.x - 400f) < 0.01f, "east should be straight up, was $east")
    }

    @Test
    fun isOnScreenHonoursTheMargin() {
        val camera = MapCameraState(center = LatLon(0.0, 0.0), zoom = 10f)
        assertTrue(camera.isOnScreen(camera.center, size, density), "the centre is on screen")
        assertFalse(camera.isOnScreen(LatLon(0.0, 90.0), size, density), "a quarter of the world away is not")
        val nearEdge = LatLon(0.0, 0.0).let { centre ->
            // Find a point 5 px inside the right edge by walking east until it is there.
            var lon = 0.0
            while (camera.screenPosition(LatLon(centre.lat, lon), size, density).x < size.width - 5f) lon += 0.0001
            LatLon(centre.lat, lon)
        }
        assertTrue(camera.isOnScreen(nearEdge, size, density), "5 px inside the edge is on screen")
        assertFalse(camera.isOnScreen(nearEdge, size, density, margin = 8.dp), "but not inside a 16 px margin")
    }

    /** Puts a camera where [fit] says and checks every point lands inside the padded frame. */
    private fun assertFramed(points: List<LatLon>, bearing: Float) {
        val padding = 24.dp
        val position = fit(points, size, density, padding = padding, bearing = bearing)
        val camera = MapCameraState().apply { moveTo(position) }
        val inset = with(density) { padding.toPx() } - 0.5f
        for (point in points) {
            val at = camera.screenPosition(point, size, density)
            assertTrue(
                at.x >= inset && at.y >= inset && at.x <= size.width - inset && at.y <= size.height - inset,
                "$point landed at $at, outside the padded frame, at bearing $bearing (camera $position)",
            )
        }
    }

}
