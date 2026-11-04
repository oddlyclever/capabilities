package com.oddlyclever.wayfinder.geojson

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Pins that GeoJSON text is parsed for the layer, and that text that is not GeoJSON, or a fetch that returns nothing or
 * throws, draws nothing rather than failing.
 */
class GeoJsonLayerTest {

    @Test
    fun parsesAFeatureCollection() {
        val text = """{"type":"FeatureCollection","features":[{"type":"Feature","geometry":{"type":"Point","coordinates":[-79.38,43.65]},"properties":{"name":"here"}}]}"""
        val data = parseGeoJsonOrNull(text)
        assertEquals(1, data?.features?.size, "one feature, was ${data?.features?.size}")
    }

    @Test
    fun textThatIsNotGeoJsonIsNothing() {
        assertNull(parseGeoJsonOrNull("<html>502</html>"), "an error page is not GeoJSON")
        assertNull(parseGeoJsonOrNull(""), "nor is nothing")
    }

    @Test
    fun aFetchIsParsed() = runTest {
        val text = """{"type":"FeatureCollection","features":[{"type":"Feature","geometry":{"type":"Point","coordinates":[-79.38,43.65]},"properties":{}}]}"""
        val data = loadGeoJson("https://data.example.com/a.geojson") { text.encodeToByteArray() }
        assertEquals(1, data?.features?.size, "one feature, was ${data?.features?.size}")
    }

    @Test
    fun aFetchThatFailsIsNothing() = runTest {
        assertNull(loadGeoJson("https://data.example.com/a.geojson") { null }, "a null fetch")
        assertNull(loadGeoJson("https://data.example.com/a.geojson") { throw IllegalStateException("offline") }, "a fetch that throws")
    }
}
