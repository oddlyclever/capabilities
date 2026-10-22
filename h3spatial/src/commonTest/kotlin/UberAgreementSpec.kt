import com.oddlyclever.h3spatial.H3LatLng
import com.oddlyclever.h3spatial.H3SpatialService
import kotlin.math.abs
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.fail

/**
 * Pins every target to Uber's own expected answers: the service must agree with H3's reference test
 * data ([uberCellCenters], [uberCellBoundaries], [uberCellCentroids]) for the release it vendors.
 *
 * This is the drift guard. Each platform reaches H3 by a different route, so a wrong binding, a
 * mismatched native build or a transposed coordinate shows up as disagreement with Uber here — on the
 * platform where it happens — rather than as cells that differ between devices in the field.
 *
 * Run from a per-target suite for the same reason as [H3SpatialServiceSpec].
 */
internal object UberAgreementSpec {
    private val service = H3SpatialService()

    /**
     * About a centimetre. H3's reference data is written to 9–10 decimal places and the service
     * rounds to 8, so agreement is within a few billionths of a degree; this leaves room for that
     * rounding and nothing that could hide a real disagreement.
     */
    private const val TOLERANCE_DEGREES = 1e-7

    /** Each reference point lies in the cell Uber recorded for it, at that cell's resolution. */
    fun pointToCell() {
        uberCellCenters.forEach { row ->
            val (cell, lat, lng) = row.split(" ")
            val resolution = service.getResolution(cell)
            assertEquals(cell, service.latLngToH3(lat.toDouble(), normaliseLongitude(lng.toDouble()), resolution), "point for $row")
        }
    }

    /** Each cell's boundary has Uber's vertices, in Uber's order. */
    fun boundaries() {
        uberCellBoundaries.forEach { row ->
            val (cell, vertices) = row.split(" ")
            val expected = vertices.split(";").map { vertex ->
                val (lat, lng) = vertex.split(",")
                H3LatLng(lat.toDouble(), lng.toDouble())
            }
            val actual = service.h3ToPolygon(cell)
            assertEquals(expected.size, actual.size, "vertex count for $cell")
            expected.zip(actual).forEachIndexed { i, (e, a) -> assertClose(e, a, "vertex $i of $cell") }
        }
    }

    /** Each cell's centre matches Uber's. */
    fun centres() {
        uberCellCentroids.forEach { row ->
            val (cell, lat, lng) = row.split(" ")
            assertClose(H3LatLng(lat.toDouble(), lng.toDouble()), service.cellToLatLng(cell), "centre of $cell")
        }
    }

    /** Guards the guard: the data must actually be there, or every check above passes vacuously. */
    fun fixturesPresent() {
        assertTrue(uberCellCenters.size > 1000, "centres fixture has ${uberCellCenters.size} rows")
        assertTrue(uberCellBoundaries.size > 800, "boundaries fixture has ${uberCellBoundaries.size} rows")
        assertTrue(uberCellCentroids.size > 900, "centroids fixture has ${uberCellCentroids.size} rows")
    }

    private fun assertClose(expected: H3LatLng, actual: H3LatLng, what: String) {
        val latDiff = abs(expected.lat - actual.lat)
        val lngDiff = abs(longitudeDifference(expected.lng, actual.lng))
        if (latDiff > TOLERANCE_DEGREES || lngDiff > TOLERANCE_DEGREES) {
            fail("$what: expected $expected, was $actual (lat off by $latDiff, lng off by $lngDiff)")
        }
    }

    /** Uber's data writes some longitudes as 0..360; the service takes -180..180. */
    private fun normaliseLongitude(lng: Double): Double = if (lng > 180.0) lng - 360.0 else lng

    /** The shortest difference between two longitudes, so 180 and -180 compare equal. */
    private fun longitudeDifference(a: Double, b: Double): Double {
        val d = (a - b) % 360.0
        return when {
            d > 180.0 -> d - 360.0
            d < -180.0 -> d + 360.0
            else -> d
        }
    }
}
