package com.oddlyclever.h3spatial

import com.uber.h3core.H3Core
import com.uber.h3core.exceptions.H3Exception

/**
 * `com.uber:h3-android` — Uber's own Android packaging of the same Java binding the JVM target uses,
 * with `libh3-java.so` built by Uber for ARM and ARM64.
 *
 * Loaded with [H3Core.newSystemInstance], which calls `System.loadLibrary("h3-java")` and so finds the
 * library in the AAR's `jni/` directory. The jar's other loader extracts a native file from resources
 * instead, which needs file APIs older Android versions lack.
 *
 * This duplicates the JVM implementation apart from how the library is loaded. Sharing it would need
 * a custom JVM-plus-Android source set, which costs more than a short file.
 */
internal actual object H3Native {
    private val core: H3Core by lazy { H3Core.newSystemInstance() }

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
