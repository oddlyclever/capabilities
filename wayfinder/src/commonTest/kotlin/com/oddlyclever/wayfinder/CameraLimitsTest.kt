package com.oddlyclever.wayfinder

import com.coderwise.libs.mapview.LatLon
import com.coderwise.libs.mapview.ZOOM_LIMITS
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

/**
 * Pins how a map's camera is held to its limits: zoom kept in its range and in map-view's, the middle of the map kept
 * in its box and moved to the nearest edge from outside it, the short way round a box over the antimeridian.
 */
class CameraLimitsTest {

    private val town = GeoBounds(south = 43.5, west = -80.0, north = 44.0, east = -79.0)
    private val pacific = GeoBounds(south = -30.0, west = 170.0, north = 30.0, east = -170.0)

    @Test
    fun zoomIsKeptInItsRange() {
        val limits = CameraLimits(zoom = 10f..18f)
        assertEquals(10f, limits.hold(4f))
        assertEquals(18f, limits.hold(19.5f))
        assertEquals(12.5f, limits.hold(12.5f))
    }

    @Test
    fun zoomIsAlsoKeptInMapViewsRange() {
        val limits = CameraLimits(zoom = 0f..30f)
        assertEquals(ZOOM_LIMITS.start, limits.hold(0f))
        assertEquals(ZOOM_LIMITS.endInclusive, limits.hold(30f))
        assertEquals(ZOOM_LIMITS, CameraLimits(zoom = 25f..30f).effectiveZoom, "a range map-view cannot show falls back to map-view's")
    }

    @Test
    fun withNoBoxTheMiddleGoesAnywhere() {
        val point = LatLon(-60.0, 120.0)
        assertSame(point, CameraLimits().hold(point))
    }

    @Test
    fun insideTheBoxTheMiddleStaysPut() {
        val point = LatLon(43.7, -79.5)
        assertSame(point, CameraLimits(center = town).hold(point))
    }

    @Test
    fun outsideTheBoxTheMiddleGoesToTheNearestEdge() {
        val limits = CameraLimits(center = town)
        assertEquals(LatLon(44.0, -79.5), limits.hold(LatLon(45.0, -79.5)))
        assertEquals(LatLon(43.7, -80.0), limits.hold(LatLon(43.7, -81.0)))
        assertEquals(LatLon(43.5, -79.0), limits.hold(LatLon(40.0, -70.0)), "a corner, past two edges")
    }

    @Test
    fun aBoxOverTheAntimeridianHoldsBothSidesAndSnapsTheShortWay() {
        val limits = CameraLimits(center = pacific)
        assertEquals(LatLon(0.0, 175.0), limits.hold(LatLon(0.0, 175.0)))
        assertEquals(LatLon(0.0, -175.0), limits.hold(LatLon(0.0, -175.0)))
        assertEquals(LatLon(0.0, 170.0), limits.hold(LatLon(0.0, 160.0)), "west of the box goes to its west edge")
        assertEquals(LatLon(0.0, -170.0), limits.hold(LatLon(0.0, -160.0)), "east of the box goes to its east edge")
    }
}
