// Derived from map-view-geojson 0.3.0's tests, part of map-view (https://github.com/coderwise/libs-mobile), licensed
// under the Apache License 2.0. Modified: moved into com.oddlyclever.wayfinder.geojson, and building aughtone-types'
// GeoJSON model in place of map-view-geojson's own. See the module README.

package com.oddlyclever.wayfinder.geojson

import com.coderwise.libs.mapview.LatLon
import com.coderwise.libs.mapview.toWorld
import io.github.aughtone.types.geo.GeoFeature
import io.github.aughtone.types.geo.GeoFeatureCollection
import io.github.aughtone.types.geo.GeoGeometry
import io.github.aughtone.types.geo.GeoLineString
import io.github.aughtone.types.geo.GeoMultiLineString
import io.github.aughtone.types.geo.GeoMultiPoint
import io.github.aughtone.types.geo.GeoMultiPolygon
import io.github.aughtone.types.geo.GeoPoint
import io.github.aughtone.types.geo.GeoPolygon
import kotlinx.serialization.json.JsonPrimitive

// The tests place things by LatLon, latitude first, as the map does; these turn that into GeoJSON's longitude-first
// positions, so a test reads the same as it did against map-view's model.

private fun LatLon.position() = listOf(lon, lat)

private fun List<LatLon>.positions() = map { it.position() }

/** A closed rectangle, as GeoJSON writes a ring: it ends where it starts. */
internal fun ring(west: Double, south: Double, east: Double, north: Double) = listOf(
    LatLon(south, west), LatLon(south, east), LatLon(north, east), LatLon(north, west), LatLon(south, west)
)

internal fun square(centreLon: Double, centreLat: Double, half: Double) =
    ring(centreLon - half, centreLat - half, centreLon + half, centreLat + half)

internal fun point(at: LatLon) = GeoPoint(at.position())

internal fun multiPoint(at: List<LatLon>) = GeoMultiPoint(at.positions())

internal fun lineString(at: List<LatLon>) = GeoLineString(at.positions())

internal fun multiLineString(lines: List<List<LatLon>>) = GeoMultiLineString(lines.map { it.positions() })

internal fun polygon(vararg rings: List<LatLon>) = GeoPolygon(rings.map { it.positions() })

internal fun multiPolygon(polygons: List<List<List<LatLon>>>) =
    GeoMultiPolygon(polygons.map { rings -> rings.map { it.positions() } })

internal fun feature(geometry: GeoGeometry?, id: String? = null) =
    GeoFeature(geometry, id = id?.let(::JsonPrimitive))

internal fun collection(vararg features: GeoFeature) = GeoFeatureCollection(features.toList())

/** A feature's id as the text the tests name it by. */
internal val GeoFeature.key: String? get() = id?.content

/** How far the shapes are from [at], in degrees of longitude: a world unit is 360 of them. */
internal fun Shapes.degreesTo(at: LatLon): Double? =
    world(at).let { nearest(it[0], it[1]) }?.let { it.distance * 360.0 }

internal fun Shapes.featureAt(at: LatLon): GeoFeature? =
    world(at).let { nearest(it[0], it[1]) }?.feature

private fun world(at: LatLon) = listOf(at).toWorld()
