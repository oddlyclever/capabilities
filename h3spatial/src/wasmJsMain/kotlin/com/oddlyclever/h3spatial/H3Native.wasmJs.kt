package com.oddlyclever.h3spatial

import com.oddlyclever.h3spatial.h3js.areNeighborCells as jsAreNeighborCells
import com.oddlyclever.h3spatial.h3js.cellToBoundary as jsCellToBoundary
import com.oddlyclever.h3spatial.h3js.cellToChildren as jsCellToChildren
import com.oddlyclever.h3spatial.h3js.cellToLatLng as jsCellToLatLng
import com.oddlyclever.h3spatial.h3js.cellToParent as jsCellToParent
import com.oddlyclever.h3spatial.h3js.gridDisk as jsGridDisk
import com.oddlyclever.h3spatial.h3js.gridDistance as jsGridDistance
import com.oddlyclever.h3spatial.h3js.isPentagon as jsIsPentagon
import com.oddlyclever.h3spatial.h3js.isValidCell as jsIsValidCell
import com.oddlyclever.h3spatial.h3js.latLngToCell as jsLatLngToCell

/**
 * Uber's `h3-js`, the H3 C library compiled to JavaScript, called through its string-index API.
 *
 * h3-js reports failure by throwing an `Error` whose message ends in `(code: N)`. Kotlin/Wasm surfaces
 * a JavaScript throw as an exception carrying that message, so the code is read back from it and
 * carried into [H3SpatialException]. Some h3-js functions return sentinel values instead of throwing
 * on invalid input, which is why the service validates everything before calling here.
 */
internal actual object H3Native {
    actual fun latLngToCell(lat: Double, lng: Double, resolution: Int): ULong =
        call { jsLatLngToCell(lat, lng, resolution).toCell() }

    actual fun cellToLatLng(cell: ULong): H3LatLng =
        call { jsCellToLatLng(cell.toCanonical()).let { H3LatLng(it[0]!!.toDouble(), it[1]!!.toDouble()) } }

    actual fun cellToBoundary(cell: ULong): List<H3LatLng> = call {
        val ring = jsCellToBoundary(cell.toCanonical())
        List(ring.length) { i -> ring[i]!!.let { H3LatLng(it[0]!!.toDouble(), it[1]!!.toDouble()) } }
    }

    actual fun isValidCell(cell: ULong): Boolean = jsIsValidCell(cell.toCanonical())

    actual fun isPentagon(cell: ULong): Boolean = call { jsIsPentagon(cell.toCanonical()) }

    actual fun gridDisk(origin: ULong, k: Int): List<ULong> =
        call { jsGridDisk(origin.toCanonical(), k).toCells() }

    actual fun gridDistance(origin: ULong, destination: ULong): Long =
        call { jsGridDistance(origin.toCanonical(), destination.toCanonical()).toLong() }

    actual fun areNeighborCells(origin: ULong, destination: ULong): Boolean =
        call { jsAreNeighborCells(origin.toCanonical(), destination.toCanonical()) }

    actual fun cellToParent(cell: ULong, parentResolution: Int): ULong =
        call { jsCellToParent(cell.toCanonical(), parentResolution).toCell() }

    actual fun cellToChildren(cell: ULong, childResolution: Int): List<ULong> =
        call { jsCellToChildren(cell.toCanonical(), childResolution).toCells() }

    private fun String.toCell(): ULong = toULong(radix = 16)

    private fun JsArray<JsString>.toCells(): List<ULong> = List(length) { get(it)!!.toString().toCell() }

    private val ERROR_CODE = Regex("""\(code: (\d+)""")

    private inline fun <T> call(block: () -> T): T = try {
        block()
    } catch (e: Throwable) {
        val code = e.message?.let { ERROR_CODE.find(it) }?.groupValues?.get(1)?.toIntOrNull()
        throw H3SpatialException(e.message ?: "h3-js error", code, e)
    }
}
