package com.oddlyclever.h3spatial

/**
 * Renders H3 cells as a GeoJSON `FeatureCollection` string — one `Polygon` feature per cell, with
 * the cell's canonical index in `properties.h3`.
 *
 * Emitted as *text*, deliberately: this keeps the module free of any GeoJSON model dependency, while
 * any consumer that can parse GeoJSON — a map library's GeoJSON layer, typically — can draw it
 * directly. Coordinates follow
 * the GeoJSON convention (RFC 7946): `[lng, lat]` order and a closed ring (first position repeated
 * last). Vertex order is as the H3 library emits the boundary; pentagons yield 5-vertex rings.
 *
 * @param cells Cell indexes (canonical or zero-padded, as anywhere else in the service).
 * @return A GeoJSON `FeatureCollection`; empty `features` for an empty list.
 * @throws H3SpatialException if any index is invalid — same contract as every other function.
 */
@Throws(H3SpatialException::class)
fun H3SpatialService.cellsToGeoJson(cells: List<String>): String {
    val features = cells.joinToString(",") { cell ->
        val ring = h3ToPolygon(cell) // validates the index
        val closed = ring + ring.first()
        val coordinates = closed.joinToString(",") { "[${it.lng},${it.lat}]" }
        // h3ToPolygon accepted it, so trimming zero-padding yields the canonical form
        val canonical = cell.trimStart('0').lowercase()
        """{"type":"Feature","properties":{"h3":"$canonical"},"geometry":{"type":"Polygon","coordinates":[[$coordinates]]}}"""
    }
    return """{"type":"FeatureCollection","features":[$features]}"""
}

/** Single-cell convenience for [cellsToGeoJson]. */
@Throws(H3SpatialException::class)
fun H3SpatialService.cellToGeoJson(h3Index: String): String = cellsToGeoJson(listOf(h3Index))
