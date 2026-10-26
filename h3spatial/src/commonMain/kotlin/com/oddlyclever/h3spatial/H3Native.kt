package com.oddlyclever.h3spatial

/**
 * The seam between [H3SpatialService]'s shared logic and each platform's own H3.
 *
 * Every target binds Uber's H3 rather than reimplementing it: the C library through cinterop on iOS,
 * `com.uber:h3` on the JVM and Android, and `h3-js` in the browser. A reimplementation risks the worst
 * outcome for a spatial index — cells that agree *nearly* everywhere, so cross-platform comparison
 * fails only at cell edges, intermittently, in the field.
 *
 * Deliberately thin. Inputs arrive already validated by the service, indexes are the raw 64-bit value,
 * and coordinates are degrees. An implementation translates its platform's own failure into
 * [H3SpatialException], carrying H3's error code where the platform exposes one.
 */
internal expect object H3Native {
    fun latLngToCell(lat: Double, lng: Double, resolution: Int): ULong
    fun cellToLatLng(cell: ULong): H3LatLng
    fun cellToBoundary(cell: ULong): List<H3LatLng>
    fun isValidCell(cell: ULong): Boolean
    fun isPentagon(cell: ULong): Boolean
    fun gridDisk(origin: ULong, k: Int): List<ULong>
    fun gridDistance(origin: ULong, destination: ULong): Long
    fun areNeighborCells(origin: ULong, destination: ULong): Boolean
    fun cellToParent(cell: ULong, parentResolution: Int): ULong
    fun cellToChildren(cell: ULong, childResolution: Int): List<ULong>
}
