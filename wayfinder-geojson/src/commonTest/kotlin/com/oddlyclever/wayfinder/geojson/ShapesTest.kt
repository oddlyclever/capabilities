// Derived from map-view-geojson 0.3.0's tests, part of map-view (https://github.com/coderwise/libs-mobile), licensed
// under the Apache License 2.0. Modified: moved into com.oddlyclever.wayfinder.geojson, and building aughtone-types'
// GeoJSON model in place of map-view-geojson's own. See the module README.

package com.oddlyclever.wayfinder.geojson

import com.coderwise.libs.mapview.LatLon
import com.coderwise.libs.mapview.toWorld
import io.github.aughtone.types.geo.GeometryCollection
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

/** Flattening geometries into the shapes that are drawn and touched. */
class ShapesTest {

    @Test
    fun `a point is a pair of world coordinates`() {
        val at = LatLon(51.5, -0.12)
        val shaped = collection(feature(point(at))).toShapes().features.single()
        assertContentEquals(listOf(at).toWorld(), shaped.points)
    }

    @Test
    fun `a multi-geometry is all its parts`() {
        val shaped = collection(
            feature(multiPoint(listOf(LatLon(1.0, 1.0), LatLon(2.0, 2.0), LatLon(3.0, 3.0))))
        ).toShapes().features.single()
        assertEquals(6, shaped.points.size)

        val lines = collection(
            feature(multiLineString(listOf(listOf(LatLon(0.0, 0.0), LatLon(1.0, 1.0)), listOf(LatLon(2.0, 2.0), LatLon(3.0, 3.0)))))
        ).toShapes().features.single()
        assertEquals(2, lines.lines.size)

        val polygons = collection(
            feature(multiPolygon(listOf(listOf(square(0.0, 0.0, 1.0), square(0.0, 0.0, 0.5)), listOf(square(5.0, 5.0, 1.0)))))
        ).toShapes().features.single()
        assertEquals(listOf(2, 1), polygons.polygons.map { it.size }, "rings of each polygon")
    }

    @Test
    fun `a collection is flattened`() {
        val shaped = collection(
            feature(
                GeometryCollection(
                    listOf(
                        point(LatLon(1.0, 1.0)),
                        GeometryCollection(listOf(lineString(listOf(LatLon(0.0, 0.0), LatLon(1.0, 1.0))))),
                        polygon(square(0.0, 0.0, 1.0))
                    )
                )
            )
        ).toShapes().features.single()
        assertEquals(2, shaped.points.size)
        assertEquals(1, shaped.lines.size)
        assertEquals(1, shaped.polygons.size)
    }

    @Test
    fun `a feature without a geometry has no shapes but is kept`() {
        val shapes = collection(feature(null, id = "ghost")).toShapes()
        val shaped = shapes.features.single()
        assertEquals("ghost", shaped.feature.key)
        assertEquals(0, shaped.points.size + shaped.lines.size + shaped.polygons.size)
    }

    @Test
    fun `a polygon without rings has none`() {
        // aughtone-types refuses a line of one position and a ring of fewer than four, so those never reach the shapes.
        val empty = collection(feature(polygon())).toShapes().features.single()
        assertEquals(listOf(0), empty.polygons.map { it.size }, "rings of the one polygon")
    }

    @Test
    fun `a layer is shown from its minimum zoom up`() {
        assertEquals(false, shownAt(11.99f, 12f))
        assertEquals(true, shownAt(12f, 12f))
        assertEquals(true, shownAt(15f, 12f))
        assertEquals(true, shownAt(1f, 0f))
    }
}
