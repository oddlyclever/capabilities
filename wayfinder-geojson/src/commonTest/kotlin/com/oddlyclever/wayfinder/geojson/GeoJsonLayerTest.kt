package com.oddlyclever.wayfinder.geojson

import io.github.aughtone.types.geo.GeoLineString
import io.github.aughtone.types.geo.GeoPoint
import io.github.aughtone.types.geo.GeoPolygon
import io.github.aughtone.types.geo.GeometryCollection
import io.github.aughtone.types.geo.stringProperty
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNull

/**
 * Pins how GeoJSON text becomes aughtone-types' model for the layers: every kind of document, ids and properties, the
 * members that are skipped, and that text that is not GeoJSON, or a fetch that returns nothing or throws, draws nothing
 * rather than failing.
 */
class GeoJsonLayerTest {

    private val here = """{"type":"Point","coordinates":[-79.38,43.65]}"""

    @Test
    fun parsesAFeatureCollection() {
        val text = """{"type":"FeatureCollection","features":[{"type":"Feature","geometry":$here,"properties":{"name":"here"}}]}"""
        val data = parseGeoJsonOrNull(text)
        assertEquals(1, data?.features?.size, "one feature, was ${data?.features?.size}")
        assertEquals("here", data?.features?.single()?.stringProperty("name"), "its name, was ${data?.features}")
    }

    @Test
    fun aSingleFeatureIsACollectionOfOne() {
        val data = parseGeoJson("""{"type":"Feature","id":7,"geometry":$here,"properties":null}""")
        val feature = data.features.single()
        assertEquals("7", feature.id?.content, "a numeric id, was ${feature.id}")
        assertNull(feature.properties, "null properties, was ${feature.properties}")
    }

    @Test
    fun aBareGeometryIsAFeatureWithoutProperties() {
        val point = assertIs<GeoPoint>(parseGeoJson(here).features.single().geometry)
        assertEquals(-79.38, point.longitude, "longitude first, as GeoJSON writes it, was $point")
        assertEquals(43.65, point.latitude, "then latitude, was $point")
    }

    @Test
    fun everyKindOfGeometryIsRead() {
        val text = """{"type":"GeometryCollection","geometries":[
            {"type":"LineString","coordinates":[[0,0],[1,1]]},
            {"type":"Polygon","coordinates":[[[0,0],[1,0],[1,1],[0,0]]]}
        ]}"""
        val geometry = parseGeoJson(text).features.single().geometry
        val parts = assertIs<GeometryCollection>(geometry).geometries
        assertIs<GeoLineString>(parts[0], "the line, was ${parts[0]}")
        assertIs<GeoPolygon>(parts[1], "the polygon, was ${parts[1]}")
    }

    @Test
    fun membersOutsideTheSpecificationAreSkipped() {
        val text = """{"type":"FeatureCollection","crs":{"type":"name"},"features":[{"type":"Feature","geometry":$here,"style":"red"}]}"""
        val feature = parseGeoJson(text).features.single()
        assertNull(feature.properties, "absent properties are null, was ${feature.properties}")
    }

    @Test
    fun aFeatureWithoutAGeometryIsKept() {
        val data = parseGeoJson("""{"type":"FeatureCollection","features":[{"type":"Feature","properties":{}}]}""")
        assertNull(data.features.single().geometry, "no geometry, was ${data.features}")
    }

    @Test
    fun aByteOrderMarkIsSkipped() {
        assertEquals(1, parseGeoJsonOrNull("\uFEFF$here")?.features?.size, "a file saved with a BOM")
    }

    @Test
    fun aRingThatIsNotClosedIsNotGeoJson() {
        val open = """{"type":"Polygon","coordinates":[[[0,0],[1,0],[1,1],[0,1]]]}"""
        assertFailsWith<IllegalArgumentException> { parseGeoJson(open) }
        assertNull(parseGeoJsonOrNull(open), "and the layer draws nothing")
    }

    @Test
    fun textThatIsNotGeoJsonIsNothing() {
        assertNull(parseGeoJsonOrNull("<html>502</html>"), "an error page is not GeoJSON")
        assertNull(parseGeoJsonOrNull(""), "nor is nothing")
        assertNull(parseGeoJsonOrNull("""{"type":"Circle","coordinates":[0,0]}"""), "nor is an unknown type")
        assertFailsWith<IllegalArgumentException> { parseGeoJson("""{"type":"Point","coordinates":[0]}""") }
    }

    @Test
    fun aFetchIsParsed() = runTest {
        val text = """{"type":"FeatureCollection","features":[{"type":"Feature","geometry":$here,"properties":{}}]}"""
        val data = loadGeoJson("https://data.example.com/a.geojson") { text.encodeToByteArray() }
        assertEquals(1, data?.features?.size, "one feature, was ${data?.features?.size}")
    }

    @Test
    fun aFetchThatFailsIsNothing() = runTest {
        assertNull(loadGeoJson("https://data.example.com/a.geojson") { null }, "a null fetch")
        assertNull(loadGeoJson("https://data.example.com/a.geojson") { throw IllegalStateException("offline") }, "a fetch that throws")
    }
}
