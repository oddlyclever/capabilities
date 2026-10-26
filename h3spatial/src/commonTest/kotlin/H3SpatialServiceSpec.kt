import com.oddlyclever.h3spatial.H3LatLng
import com.oddlyclever.h3spatial.H3SpatialException
import com.oddlyclever.h3spatial.H3SpatialService
import com.oddlyclever.h3spatial.cellsToGeoJson
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/**
 * Pins [H3SpatialService]'s contract: canonical indexes, geometry, traversal, hierarchy, GeoJSON and
 * the single failure type.
 *
 * Run from a per-target suite rather than as common `@Test`s, because every platform binds H3 by a
 * different mechanism and only a run on that platform proves its binding. A common `@Test` would also
 * run as an Android host unit test, where the native library cannot load.
 */
internal object H3SpatialServiceSpec {
    private val service = H3SpatialService()

    /** Point to cell: determinism, nearby agreement, and resolution. */
    fun pointToCell() {
        assertEquals(zeroLatLngRes4, service.latLngToH3(0.0, 0.0, 4))
        // asymmetric fixture — a lat/lng transposition in any binding fails here
        assertEquals(ottawaRes7, service.latLngToH3(ottawaLat, ottawaLng, 7))
        // coordinates a few metres apart at resolution 6 agree (offset from the cell's
        // own centre so the pair cannot straddle a boundary)
        val res6 = service.latLngToH3(ottawaLat, ottawaLng, 6)
        val centre = service.cellToLatLng(res6)
        assertEquals(res6, service.latLngToH3(centre.lat + 0.00005, centre.lng - 0.00005, 6))
        // distant coordinates at the same resolution differ
        assertNotEquals(zeroLatLngRes4, service.latLngToH3(ottawaLat, ottawaLng, 4))
        // finer resolutions subdivide — the indexes differ and report their resolution
        val at6 = service.latLngToH3(45.0, -75.0, 6)
        val at8 = service.latLngToH3(45.0, -75.0, 8)
        assertNotEquals(at6, at8)
        assertEquals(6, service.getResolution(at6))
        assertEquals(8, service.getResolution(at8))
    }

    /** Canonical, tool-interoperable index form on every target. */
    fun canonicalForm() {
        val idx = service.latLngToH3(0.0, 0.0, 4)
        assertEquals(15, idx.length, "cell indexes are canonical unpadded form")
        assertEquals(idx, idx.lowercase())
        assertFalse(idx.startsWith("0"))
        // zero-padded input is tolerated
        assertEquals(4, service.getResolution(paddedZeroLatLngRes4))
        assertEquals(zeroLatLngRes4, service.cellToParent(paddedZeroLatLngRes4, 4))
    }

    /** Boundary and centre, and the centre lies inside the boundary. */
    fun cellGeometry() {
        val polygon = service.h3ToPolygon(zeroLatLngRes4)
        assertEquals(zeroBoundary, polygon)
        val centre = service.cellToLatLng(zeroLatLngRes4)
        assertEquals(zeroCenter, centre)
        // the cell's own centre falls within its boundary (ray cast; cell is nowhere near
        // the antimeridian, so plain lng arithmetic is safe)
        assertTrue(contains(polygon, centre.lat, centre.lng), "centre inside own boundary")
    }

    private fun contains(polygon: List<H3LatLng>, lat: Double, lng: Double): Boolean {
        var inside = false
        var j = polygon.size - 1
        for (i in polygon.indices) {
            val a = polygon[i]
            val b = polygon[j]
            if ((a.lat > lat) != (b.lat > lat) &&
                lng < (b.lng - a.lng) * (lat - a.lat) / (b.lat - a.lat) + a.lng
            ) {
                inside = !inside
            }
            j = i
        }
        return inside
    }

    /** isValidCell is the designated no-throw validity check. */
    fun validity() {
        assertTrue(service.isValidCell(zeroLatLngRes4))
        assertTrue(service.isValidCell(paddedZeroLatLngRes4))
        assertFalse(service.isValidCell(invalidIndex))
        assertFalse(service.isValidCell(invalidCellHex))
    }

    /** gridDisk, neighbours, distance. */
    fun gridTraversal() {
        assertEquals(gridDisk1OfZero, service.gridDisk(zeroLatLngRes4, 1).sorted())
        assertEquals(listOf(zeroLatLngRes4), service.gridDisk(zeroLatLngRes4, 0))
        assertTrue(service.areNeighborCells(zeroLatLngRes4, neighborOfZeroLatLngRes4))
        assertFalse(service.areNeighborCells(zeroLatLngRes4, notNeighborOfZeroLatLngRes4))
        assertFalse(service.areNeighborCells(zeroLatLngRes4, zeroLatLngRes4))
        assertEquals(0L, service.gridDistance(zeroLatLngRes4, zeroLatLngRes4))
        assertEquals(1L, service.gridDistance(zeroLatLngRes4, neighborOfZeroLatLngRes4))
        assertEquals(2L, service.gridDistance(zeroLatLngRes4, notNeighborOfZeroLatLngRes4))
    }

    /** The twelve pentagons per resolution have five neighbours, not six. */
    fun pentagons() {
        assertTrue(service.isPentagon(pentagonRes4))
        assertFalse(service.isPentagon(zeroLatLngRes4))
        assertEquals(5, service.h3ToPolygon(pentagonRes4).size)
        val disk = service.gridDisk(pentagonRes4, 1)
        assertEquals(6, disk.size, "a pentagon's k=1 disk has 6 cells — never assume 7")
        assertEquals(pentagonDisk1, disk.sorted())
    }

    /** Parent/children round-trip. */
    fun hierarchy() {
        assertEquals(parentRes3OfZero, service.cellToParent(zeroLatLngRes4, 3))
        assertEquals(zeroLatLngRes4, service.cellToParent(zeroLatLngRes4, 4)) // own res → itself
        val children = service.cellToChildren(zeroLatLngRes4, 5)
        assertEquals(7, children.size)
        assertEquals(childrenRes5OfZero, children.sorted())
        children.forEach { assertEquals(zeroLatLngRes4, service.cellToParent(it, 4)) }
    }

    /** GeoJSON emission: [lng,lat] order, closed ring, canonical h3 property, contract on errors. */
    fun geoJson() {
        val json = service.cellsToGeoJson(listOf(paddedZeroLatLngRes4))
        assertTrue(json.startsWith("""{"type":"FeatureCollection","features":["""))
        assertTrue(""""h3":"$zeroLatLngRes4"""" in json, "canonical index in properties")
        val first = zeroBoundary.first()
        assertTrue("[${first.lng},${first.lat}]" in json, "GeoJSON is [lng,lat] order")
        // closed ring: first vertex appears twice
        val vertex = "[${first.lng},${first.lat}]"
        assertEquals(2, json.split(vertex).size - 1, "ring is closed")
        // pentagon: 5 distinct vertices + closing repeat = 6 positions
        val pentagon = service.cellsToGeoJson(listOf(pentagonRes4))
        assertEquals(6, pentagon.split("],[").size, "pentagon ring has 6 positions")
        assertEquals(
            """{"type":"FeatureCollection","features":[]}""",
            service.cellsToGeoJson(emptyList()),
        )
        // one feature per cell ("FeatureCollection" also contains "Feature" — count the full needle)
        val two = service.cellsToGeoJson(listOf(zeroLatLngRes4, neighborOfZeroLatLngRes4))
        assertEquals(2, two.split(""""type":"Feature","properties"""").size - 1)
        assertFailsWith<H3SpatialException> { service.cellsToGeoJson(listOf(invalidIndex)) }
    }

    /** Invalid input throws H3SpatialException — the same type, and the same code, on every target. */
    fun failureContract() {
        assertFailsWith<H3SpatialException> { service.h3ToPolygon(invalidIndex) }
        assertFailsWith<H3SpatialException> { service.h3ToPolygon(invalidCellHex) }
        assertFailsWith<H3SpatialException> { service.getResolution(invalidIndex) }
        assertFailsWith<H3SpatialException> { service.cellToLatLng(invalidIndex) }
        assertFailsWith<H3SpatialException> { service.isPentagon(invalidIndex) }
        assertFailsWith<H3SpatialException> { service.latLngToH3(0.0, 0.0, 16) }
        assertFailsWith<H3SpatialException> { service.latLngToH3(0.0, 0.0, -1) }
        assertFailsWith<H3SpatialException> { service.latLngToH3(91.0, 0.0, 4) }
        assertFailsWith<H3SpatialException> { service.latLngToH3(0.0, 181.0, 4) }
        assertFailsWith<H3SpatialException> { service.gridDisk(zeroLatLngRes4, -1) }
        assertFailsWith<H3SpatialException> { service.gridDisk(invalidIndex, 1) }
        // huge k must fail the contract way — on native targets an unchecked value would truncate
        // the output buffer and corrupt the heap (found in review; regression guard)
        assertFailsWith<H3SpatialException> { service.gridDisk(zeroLatLngRes4, 37837) }
        assertFailsWith<H3SpatialException> { service.gridDisk(zeroLatLngRes4, Int.MAX_VALUE) }
        // '+' is accepted by some platform hex parsers and not others — rejected uniformly
        assertFailsWith<H3SpatialException> { service.h3ToPolygon("+$zeroLatLngRes4") }
        assertFalse(service.isValidCell("+$zeroLatLngRes4"))
        assertFailsWith<H3SpatialException> { service.cellToParent(zeroLatLngRes4, 5) }   // finer than cell
        assertFailsWith<H3SpatialException> { service.cellToChildren(zeroLatLngRes4, 3) } // coarser than cell
        assertFailsWith<H3SpatialException> { service.gridDistance(zeroLatLngRes4, parentRes3OfZero) }
        assertFailsWith<H3SpatialException> { service.areNeighborCells(zeroLatLngRes4, parentRes3OfZero) }
        // too many resolutions finer to list: the same code on every target rather than an
        // out-of-memory on one and a truncated buffer on another
        val tooDeep = H3SpatialService.MAX_CHILD_RESOLUTION_STEPS + 1
        val cellAtRes0 = service.cellToParent(zeroLatLngRes4, 0)
        assertEquals(14, assertFailsWith<H3SpatialException> { service.cellToChildren(cellAtRes0, tooDeep) }.code)
        // the error code is H3's own, whichever platform raised it
        assertEquals(4, assertFailsWith<H3SpatialException> { service.latLngToH3(0.0, 0.0, 16) }.code)
        assertEquals(5, assertFailsWith<H3SpatialException> { service.h3ToPolygon(invalidCellHex) }.code)
    }
}
