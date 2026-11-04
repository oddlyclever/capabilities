package com.oddlyclever.wayfinder

import androidx.compose.runtime.saveable.SaverScope
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import com.coderwise.libs.mapview.LatLon
import com.coderwise.libs.mapview.MapCameraState
import com.coderwise.libs.mapview.TileKey
import kotlinx.coroutines.test.runTest
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Pins the root's map helpers: the visible bounds hold what is on screen, turned or not and across the antimeridian,
 * a bounds box answers containment the way a map shows it, a tile client is asked for a source and a tile, and the
 * state saved for an activity's recreation comes back with its camera and orientation.
 */
class MapStateTest {

    private val density = Density(1f)
    private val size = IntSize(512, 256)

    @Test
    fun boundsHoldTheCentreAndAreBalancedAroundIt() {
        val camera = MapCameraState(center = LatLon(0.0, 0.0), zoom = 2f)
        val bounds = camera.visibleBounds(size, density)
        // 512 px at zoom 2 is half the world's 1024 px width: 90 degrees either side.
        assertTrue(abs(bounds.west + 90) < 1e-6 && abs(bounds.east - 90) < 1e-6, "west and east were $bounds")
        assertTrue(LatLon(0.0, 0.0) in bounds, "the centre is visible")
    }

    @Test
    fun boundsGrowWhenTheMapIsTurned() {
        val upright = MapCameraState(center = LatLon(45.0, 10.0), zoom = 8f).visibleBounds(size, density)
        val turned = MapCameraState(center = LatLon(45.0, 10.0), zoom = 8f, bearing = 45f).visibleBounds(size, density)
        assertTrue(turned.north - turned.south > upright.north - upright.south, "a turned screen covers more latitude: $turned vs $upright")
    }

    @Test
    fun boundsCrossTheAntimeridian() {
        val bounds = MapCameraState(center = LatLon(0.0, 179.0), zoom = 6f).visibleBounds(size, density)
        assertTrue(bounds.crossesAntimeridian, "a map centred at 179 E reaches past 180: $bounds")
        assertTrue(LatLon(0.0, -179.5) in bounds, "so a point just west of 180 W is visible")
        assertFalse(LatLon(0.0, 0.0) in bounds, "and the prime meridian is not")
    }

    @Test
    fun aTileClientIsAskedForTheSourceAndTile() = runTest {
        var asked: String? = null
        val client = TileClient { source, key -> asked = source.url(key); ByteArray(1) }
        val source = TileSource(id = "streets", urlTemplate = "https://tiles.example.com/{z}/{x}/{y}.pbf")
        source.fetch(client, TileKey(3, 2, 1))
        assertEquals("https://tiles.example.com/3/2/1.pbf", asked, "the client builds the URL from the source and tile")
    }

    @Test
    fun aSavedStateComesBackWithItsCameraAndOrientation() {
        val state = WayfinderMapState(center = LatLon(43.65, -79.38), zoom = 13.5f, bearing = 30f)
        state.orientation = Orientation.CourseUp
        state.heading = 90f
        val saved = with(WayfinderMapState.Saver) { SaverScope { true }.save(state) }
        val restored = WayfinderMapState.Saver.restore(saved!!)!!
        // The camera holds its centre through map-view's projection, which drifts in the last digits on some targets.
        assertTrue(abs(restored.center.lat - 43.65) < 1e-9 && abs(restored.center.lon + 79.38) < 1e-9, "centre was ${restored.center}")
        assertEquals(13.5f, restored.zoom, "zoom was ${restored.zoom}")
        assertEquals(30f, restored.bearing, "bearing was ${restored.bearing}")
        assertEquals(Orientation.CourseUp, restored.orientation, "orientation was ${restored.orientation}")
        assertEquals(null, restored.heading, "the heading comes from a sensor and is not saved, was ${restored.heading}")
    }
}
