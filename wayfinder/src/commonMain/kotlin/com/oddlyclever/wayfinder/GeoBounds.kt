package com.oddlyclever.wayfinder

import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import com.coderwise.libs.mapview.LatLon
import com.coderwise.libs.mapview.MapCameraState
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.pow
import kotlin.math.sin

/**
 * A box of latitude and longitude: what a map shows, or what a query asks for.
 *
 * [west] is greater than [east] when the box crosses the antimeridian, so a box over the Pacific is not mistaken for
 * the rest of the world. [contains] accounts for that.
 */
class GeoBounds(val south: Double, val west: Double, val north: Double, val east: Double) {

    /** Whether the box crosses the antimeridian. */
    val crossesAntimeridian: Boolean get() = west > east

    /** Whether [point] is inside the box, edges included. */
    operator fun contains(point: LatLon): Boolean {
        if (point.lat < south || point.lat > north) return false
        return if (crossesAntimeridian) point.lon >= west || point.lon <= east else point.lon in west..east
    }

    override fun equals(other: Any?): Boolean =
        other is GeoBounds && south == other.south && west == other.west && north == other.north && east == other.east

    override fun hashCode(): Int =
        31 * (31 * (31 * south.hashCode() + west.hashCode()) + north.hashCode()) + east.hashCode()

    override fun toString(): String = "GeoBounds(south=$south, west=$west, north=$north, east=$east)"
}

/**
 * What a map of [size] pixels looking through this camera shows, as the smallest [GeoBounds] that holds all of it.
 *
 * With a bearing the screen is a turned rectangle on the ground, so the box is the one around its four corners and
 * holds some ground just off screen. Latitude is limited to what Web Mercator can show.
 */
fun MapCameraState.visibleBounds(size: IntSize, density: Density): GeoBounds {
    val world = WebMercator.WORLD_TILE * density.density * 2.0.pow(zoom.toDouble())
    val radians = bearing * PI / 180.0
    val c = cos(radians)
    val s = sin(radians)
    val centerX = WebMercator.x(center.lon)
    val centerY = WebMercator.y(center.lat)
    val halfWidth = size.width / 2.0
    val halfHeight = size.height / 2.0
    var minX = Double.MAX_VALUE
    var maxX = -Double.MAX_VALUE
    var minY = Double.MAX_VALUE
    var maxY = -Double.MAX_VALUE
    for ((across, down) in listOf(-halfWidth to -halfHeight, halfWidth to -halfHeight, -halfWidth to halfHeight, halfWidth to halfHeight)) {
        // Screen axes to world axes, as map-view turns a screen offset into the world.
        val x = centerX + (across * c - down * s) / world
        val y = centerY + (across * s + down * c) / world
        minX = minOf(minX, x); maxX = maxOf(maxX, x)
        minY = minOf(minY, y); maxY = maxOf(maxY, y)
    }
    if (maxX - minX >= 1.0) return GeoBounds(WebMercator.lat(maxY), -180.0, WebMercator.lat(minY), 180.0)
    val west = WebMercator.lon(minX - floor(minX))
    val east = WebMercator.lon(maxX - floor(maxX)).let { if (it == -180.0 && maxX > minX) 180.0 else it }
    return GeoBounds(south = WebMercator.lat(maxY), west = west, north = WebMercator.lat(minY), east = east)
}
