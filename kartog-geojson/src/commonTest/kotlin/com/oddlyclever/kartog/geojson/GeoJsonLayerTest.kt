package com.oddlyclever.kartog.geojson

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** Pins that GeoJSON text is parsed for the layer, and that text that is not GeoJSON draws nothing rather than failing. */
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
}
