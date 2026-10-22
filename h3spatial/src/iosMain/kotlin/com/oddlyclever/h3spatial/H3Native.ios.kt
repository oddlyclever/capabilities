package com.oddlyclever.h3spatial

import h3.CellBoundary
import h3.H3Error
import h3.H3IndexVar
import h3.LatLng
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.IntVar
import kotlinx.cinterop.LongVar
import kotlinx.cinterop.MemScope
import kotlinx.cinterop.alloc
import kotlinx.cinterop.allocArray
import kotlinx.cinterop.get
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.toKString
import kotlinx.cinterop.value

/**
 * The H3 C library, called directly through cinterop against the vendored release's `h3api.h`.
 *
 * The C API works in radians and reports failure as an `H3Error` code; both are converted at this
 * boundary. Output buffers are sized from the library's own size functions, and the service bounds
 * `k` and child resolution first so those sizes always fit an Int.
 */
@OptIn(ExperimentalForeignApi::class)
internal actual object H3Native {

    actual fun latLngToCell(lat: Double, lng: Double, resolution: Int): ULong = memScoped {
        val point = alloc<LatLng>()
        point.lat = h3.degsToRads(lat)
        point.lng = h3.degsToRads(lng)
        val out = alloc<H3IndexVar>()
        check(h3.latLngToCell(point.ptr, resolution, out.ptr))
        out.value
    }

    actual fun cellToLatLng(cell: ULong): H3LatLng = memScoped {
        val point = alloc<LatLng>()
        check(h3.cellToLatLng(cell, point.ptr))
        point.toDegrees()
    }

    actual fun cellToBoundary(cell: ULong): List<H3LatLng> = memScoped {
        val boundary = alloc<CellBoundary>()
        check(h3.cellToBoundary(cell, boundary.ptr))
        List(boundary.numVerts) { boundary.verts[it].toDegrees() }
    }

    actual fun isValidCell(cell: ULong): Boolean = h3.isValidCell(cell) != 0

    actual fun isPentagon(cell: ULong): Boolean = h3.isPentagon(cell) != 0

    actual fun gridDisk(origin: ULong, k: Int): List<ULong> = memScoped {
        val size = alloc<LongVar>()
        check(h3.maxGridDiskSize(k, size.ptr))
        val cells = cellBuffer(size.value)
        check(h3.gridDisk(origin, k, cells))
        // Slots the library could not fill (a disk touching a pentagon is smaller) are left as zero.
        (0 until size.value.toInt()).map { cells[it] }.filter { it != 0uL }
    }

    actual fun gridDistance(origin: ULong, destination: ULong): Long = memScoped {
        val out = alloc<LongVar>()
        check(h3.gridDistance(origin, destination, out.ptr))
        out.value
    }

    actual fun areNeighborCells(origin: ULong, destination: ULong): Boolean = memScoped {
        val out = alloc<IntVar>()
        check(h3.areNeighborCells(origin, destination, out.ptr))
        out.value != 0
    }

    actual fun cellToParent(cell: ULong, parentResolution: Int): ULong = memScoped {
        val out = alloc<H3IndexVar>()
        check(h3.cellToParent(cell, parentResolution, out.ptr))
        out.value
    }

    actual fun cellToChildren(cell: ULong, childResolution: Int): List<ULong> = memScoped {
        val size = alloc<LongVar>()
        check(h3.cellToChildrenSize(cell, childResolution, size.ptr))
        val cells = cellBuffer(size.value)
        check(h3.cellToChildren(cell, childResolution, cells))
        (0 until size.value.toInt()).map { cells[it] }.filter { it != 0uL }
    }

    private fun MemScope.cellBuffer(size: Long) =
        if (size in 0..Int.MAX_VALUE) {
            allocArray<H3IndexVar>(size.toInt())
        } else {
            throw H3SpatialException("H3 result too large to hold: $size cells", E_MEMORY_BOUNDS)
        }

    private fun LatLng.toDegrees() = H3LatLng(h3.radsToDegs(lat), h3.radsToDegs(lng))

    private fun check(error: H3Error) {
        if (error != 0u) {
            val code = error.toInt()
            throw H3SpatialException(h3.describeH3Error(error)?.toKString() ?: "H3 error $code", code)
        }
    }

    private const val E_MEMORY_BOUNDS = 14
}
