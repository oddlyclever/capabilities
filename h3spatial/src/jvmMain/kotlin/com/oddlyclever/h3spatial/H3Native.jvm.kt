package com.oddlyclever.h3spatial

import com.uber.h3core.H3Core
import com.uber.h3core.exceptions.H3Exception
import java.io.IOException

/**
 * `com.uber:h3`, which extracts the native library matching this JVM's platform from its own jar.
 *
 * A failure to load it is infrastructure, not invalid input, so it surfaces as an [Error] that the
 * service passes through rather than as an [H3SpatialException].
 */
internal actual object H3Native {
    private val core: H3Core by lazy {
        try {
            H3Core.newInstance()
        } catch (e: IOException) {
            throw LinkageError("Could not load the H3 native library", e)
        }
    }

    actual fun latLngToCell(lat: Double, lng: Double, resolution: Int): ULong =
        call { core.latLngToCell(lat, lng, resolution).toULong() }

    actual fun cellToLatLng(cell: ULong): H3LatLng =
        call { core.cellToLatLng(cell.toLong()).let { H3LatLng(it.lat, it.lng) } }

    actual fun cellToBoundary(cell: ULong): List<H3LatLng> =
        call { core.cellToBoundary(cell.toLong()).map { H3LatLng(it.lat, it.lng) } }

    actual fun isValidCell(cell: ULong): Boolean = core.isValidCell(cell.toLong())

    actual fun isPentagon(cell: ULong): Boolean = call { core.isPentagon(cell.toLong()) }

    actual fun gridDisk(origin: ULong, k: Int): List<ULong> =
        call { core.gridDisk(origin.toLong(), k).map { it.toULong() } }

    actual fun gridDistance(origin: ULong, destination: ULong): Long =
        call { core.gridDistance(origin.toLong(), destination.toLong()) }

    actual fun areNeighborCells(origin: ULong, destination: ULong): Boolean =
        call { core.areNeighborCells(origin.toLong(), destination.toLong()) }

    actual fun cellToParent(cell: ULong, parentResolution: Int): ULong =
        call { core.cellToParent(cell.toLong(), parentResolution).toULong() }

    actual fun cellToChildren(cell: ULong, childResolution: Int): List<ULong> =
        call { core.cellToChildren(cell.toLong(), childResolution).map { it.toULong() } }

    private inline fun <T> call(block: () -> T): T = try {
        block()
    } catch (e: H3Exception) {
        throw H3SpatialException(e.message ?: "H3 error ${e.code}", e.code, e)
    }
}
