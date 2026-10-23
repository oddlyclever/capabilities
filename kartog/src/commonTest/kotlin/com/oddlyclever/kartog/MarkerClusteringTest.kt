package com.oddlyclever.kartog

import androidx.compose.ui.unit.dp
import com.coderwise.libs.mapview.LatLon
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Pins how markers that would cover each other are grouped: by distance on the map at a zoom, parting as the map zooms
 * in, in order and the same way every time, the short way round the antimeridian, regrouped only at a step of zoom,
 * and never at the closest zoom the map allows.
 */
class MarkerClusteringTest {

    private val town = LatLon(44.3894, -79.6903)
    private val village = LatLon(44.4500, -79.8300)
    private val city = LatLon(43.6532, -79.3832)
    private val farAway = LatLon(46.5136, -84.3358)

    private fun groupsAt(zoom: Float, vararg points: LatLon) = clusterPoints(points.toList(), zoom) { it }

    private fun marker(id: String, at: LatLon) = MapMarker(id, at)

    @Test
    fun pointsAFewKilometresApartAreOneGroupFromFarAwayAndPartWhenZoomedIn() {
        val far = groupsAt(6f, town, village)
        assertEquals(1, far.size, "one group from far away: $far")
        assertTrue(far.single().isGroup)

        val close = groupsAt(12f, town, village)
        assertEquals(2, close.size, "two groups close up: $close")
        assertFalse(close.any { it.isGroup })
    }

    @Test
    fun theRadiusDecidesWhatCountsAsCovering() {
        assertEquals(2, clusterPoints(listOf(town, village), 6f, radius = 1.dp) { it }.size)
        assertEquals(1, clusterPoints(listOf(town, village), 6f, radius = 40.dp) { it }.size)
    }

    @Test
    fun aGroupIsDrawnInTheMiddleOfItsPoints() {
        val group = groupsAt(6f, town, village).single()
        assertTrue(abs(group.position.lon - (town.lon + village.lon) / 2) < 1e-9, "longitude ${group.position.lon}")
        assertTrue(abs(group.position.lat - (town.lat + village.lat) / 2) < 1e-3, "latitude ${group.position.lat}")
    }

    @Test
    fun everyPointIsInExactlyOneGroupInTheOrderGiven() {
        val groups = groupsAt(6f, town, farAway, village, city)
        assertEquals(listOf(town, village), groups[0].items, "the first point's group takes the later points near it")
        assertEquals(listOf(farAway), groups[1].items, "a point with nothing near it is a group of one")
        assertEquals(4, groups.sumOf { it.size })
    }

    @Test
    fun theSamePointsAtTheSameZoomAlwaysGroupTheSameWay() {
        assertEquals(groupsAt(7f, town, village, city), groupsAt(7f, town, village, city))
    }

    @Test
    fun pointsEitherSideOfTheAntimeridianAreMeasuredTheShortWayRound() {
        val group = groupsAt(6f, LatLon(-17.0, 179.95), LatLon(-17.0, -179.95)).single()
        assertTrue(abs(abs(group.position.lon) - 180.0) < 0.01, "the middle is on the antimeridian, not at ${group.position.lon}")
    }

    @Test
    fun noPointsMakeNoGroups() {
        assertEquals(emptyList(), groupsAt(6f))
    }

    @Test
    fun groupsAreWorkedOutAtAStepOfZoom() {
        val clustering = MarkerClustering(step = 0.5f)
        assertEquals(6f, clustering.groupingZoom(6.2f))
        assertEquals(6.5f, clustering.groupingZoom(6.7f))
        assertEquals(7f, MarkerClustering(step = 1f).groupingZoom(7.9f))
    }

    @Test
    fun withoutClusteringOrAtTheClosestZoomEveryMarkerIsDrawnWhereItIs() {
        val markers = listOf(marker("a", town), marker("b", town))
        val limits = CameraLimits(zoom = 2f..18f)

        assertEquals(2, clusteredMarkers(markers, 6f, clustering = null, limits = limits).size)
        assertEquals(1, clusteredMarkers(markers, 6f, MarkerClustering(), limits).size, "grouped below the closest zoom")
        assertEquals(2, clusteredMarkers(markers, 18f, MarkerClustering(), limits).size, "never grouped at the closest zoom")
    }

    @Test
    fun aTappedGroupZoomsInByItsStepButNoCloserThanTheLimits() {
        val limits = CameraLimits(zoom = 2f..18f)
        assertEquals(8f, zoomIntoCluster(6f, MarkerClustering(zoomStep = 2f), limits))
        assertEquals(18f, zoomIntoCluster(17.5f, MarkerClustering(zoomStep = 2f), limits))
    }

    @Test
    fun aClusteringThatCouldNeverWorkIsRefused() {
        assertFailsWith<IllegalArgumentException> { MarkerClustering(radius = 0.dp) }
        assertFailsWith<IllegalArgumentException> { MarkerClustering(step = 0f) }
        assertFailsWith<IllegalArgumentException> { MarkerClustering(zoomStep = -1f) }
    }
}
