package com.oddlyclever.kartog

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.spring
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.coderwise.libs.mapview.LatLon
import com.coderwise.libs.mapview.MapCameraState
import com.coderwise.libs.mapview.ZOOM_LIMITS
import com.coderwise.libs.mapview.flyTo
import kotlin.math.PI
import kotlin.math.atan
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.log2
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sinh
import kotlin.math.tan

/**
 * Where a camera should be: the point at the middle of the map, how far in, and which way is up.
 *
 * What [fit] returns, so that a map on screen ([moveTo]) and a map drawn to an image can both be pointed at the same
 * place. [bearing] follows map-view's meaning: what is at the top of the screen, in degrees clockwise from north.
 */
class CameraPosition(val center: LatLon, val zoom: Float, val bearing: Float = 0f) {

    override fun equals(other: Any?): Boolean =
        other is CameraPosition && center == other.center && zoom == other.zoom && bearing == other.bearing

    override fun hashCode(): Int = 31 * (31 * center.hashCode() + zoom.hashCode()) + bearing.hashCode()

    override fun toString(): String = "CameraPosition(center=$center, zoom=$zoom, bearing=$bearing)"
}

/**
 * The camera that shows every one of [points] on a map of [size] pixels, as close in as they allow.
 *
 * The fit respects [bearing]: a long north-south track on a map with east up is fitted across the screen, not down
 * it. Points either side of the antimeridian are fitted the short way round, so a route from Fiji to Samoa is framed
 * over the Pacific rather than stretched across the whole world.
 *
 * A single point, or points too close to need any zoom, sit at [maxZoom]; the default is street level, since a
 * single point fitted any closer shows nothing around it. The zoom is also kept within map-view's [ZOOM_LIMITS]. A
 * map with no room left inside its [padding] — one not yet measured, say — gets the farthest zoom rather than an
 * error.
 *
 * @throws IllegalArgumentException when [points] is empty.
 */
fun fit(
    points: Collection<LatLon>,
    size: IntSize,
    density: Density,
    padding: Dp = 0.dp,
    bearing: Float = 0f,
    maxZoom: Float = DEFAULT_FIT_MAX_ZOOM,
): CameraPosition {
    require(points.isNotEmpty()) { "Nothing to fit: the list of points is empty." }
    val ceiling = min(maxZoom, ZOOM_LIMITS.endInclusive)

    // Unwrap longitudes so the points sit in one unbroken span, starting just after the widest empty gap between them.
    val xs = points.map { WebMercator.x(it.lon) }.sorted()
    var gapEnd = xs.first()
    var widest = xs.first() + 1.0 - xs.last()
    for (i in 1 until xs.size) {
        val gap = xs[i] - xs[i - 1]
        if (gap > widest) {
            widest = gap
            gapEnd = xs[i]
        }
    }

    val radians = bearing * PI / 180.0
    val c = cos(radians)
    val s = sin(radians)
    var minAcross = Double.MAX_VALUE
    var maxAcross = -Double.MAX_VALUE
    var minDown = Double.MAX_VALUE
    var maxDown = -Double.MAX_VALUE
    for (point in points) {
        val x = WebMercator.x(point.lon).let { if (it < gapEnd) it + 1.0 else it }
        val y = WebMercator.y(point.lat)
        // World axes to screen axes, the way map-view turns a world offset onto the screen.
        val across = x * c + y * s
        val down = -x * s + y * c
        minAcross = min(minAcross, across); maxAcross = maxOf(maxAcross, across)
        minDown = min(minDown, down); maxDown = maxOf(maxDown, down)
    }

    // The middle of the framed points, turned back from screen axes into the world.
    val midAcross = (minAcross + maxAcross) / 2
    val midDown = (minDown + maxDown) / 2
    val centerX = midAcross * c - midDown * s
    val centerY = midAcross * s + midDown * c
    val center = LatLon(WebMercator.lat(centerY), WebMercator.lon(centerX))

    val inset = with(density) { padding.toPx() } * 2
    val roomAcross = size.width - inset
    val roomDown = size.height - inset
    if (roomAcross <= 0f || roomDown <= 0f) return CameraPosition(center, ZOOM_LIMITS.start, bearing)

    val tile = WebMercator.WORLD_TILE * density.density
    val zoomAcross = (maxAcross - minAcross).let { if (it > 0) log2(roomAcross / (it * tile)) else Double.MAX_VALUE }
    val zoomDown = (maxDown - minDown).let { if (it > 0) log2(roomDown / (it * tile)) else Double.MAX_VALUE }
    val zoom = min(zoomAcross, zoomDown).toFloat().coerceIn(ZOOM_LIMITS.start, ceiling)
    return CameraPosition(center, zoom, bearing)
}

/** The fit's default closest zoom: street level, where a single point still shows the streets around it. */
const val DEFAULT_FIT_MAX_ZOOM = 17f

/** Puts the camera at [position] at once, bearing included. To get there by animation, see [flyTo]. */
fun MapCameraState.moveTo(position: CameraPosition) {
    moveTo(position.center, position.zoom)
    rotateTo(position.bearing)
}

/**
 * Animates the camera to [position] with map-view's [flyTo], then sets its bearing. The flight itself keeps the
 * current bearing, since map-view's flight does not turn the map.
 */
suspend fun MapCameraState.flyTo(position: CameraPosition, animationSpec: AnimationSpec<Float> = spring()) {
    flyTo(position.center, position.zoom, animationSpec)
    rotateTo(position.bearing)
}

/** One level closer, animated, about the middle of the map. Stops at map-view's [ZOOM_LIMITS]. */
suspend fun MapCameraState.zoomIn(animationSpec: AnimationSpec<Float> = spring()) =
    flyTo(center, (zoom + 1f).coerceIn(ZOOM_LIMITS), animationSpec)

/** One level farther, animated, about the middle of the map. Stops at map-view's [ZOOM_LIMITS]. */
suspend fun MapCameraState.zoomOut(animationSpec: AnimationSpec<Float> = spring()) =
    flyTo(center, (zoom - 1f).coerceIn(ZOOM_LIMITS), animationSpec)

/**
 * Where [point] falls on a map of [size] pixels looking through this camera, in pixels from its top left, with the
 * bearing taken into account. A point off the map gets a position off its edges.
 *
 * Inside an overlay, map-view's own `project()` answers the same question; this is for code outside one, such as a
 * view model deciding whether to bring the camera back to the user.
 */
fun MapCameraState.screenPosition(point: LatLon, size: IntSize, density: Density): Offset {
    val world = WebMercator.WORLD_TILE * density.density * 2.0.pow(zoom.toDouble())
    val dx = (WebMercator.x(point.lon) - WebMercator.x(center.lon)).let { it - floor(it + 0.5) } * world
    val dy = (WebMercator.y(point.lat) - WebMercator.y(center.lat)) * world
    val radians = bearing * PI / 180.0
    val c = cos(radians)
    val s = sin(radians)
    return Offset((dx * c + dy * s + size.width / 2.0).toFloat(), (-dx * s + dy * c + size.height / 2.0).toFloat())
}

/**
 * Whether [point] is on a map of [size] pixels, with the bearing taken into account. [margin] shrinks the area that
 * counts, so a point under a control at the edge can be treated as off screen.
 */
fun MapCameraState.isOnScreen(point: LatLon, size: IntSize, density: Density, margin: Dp = 0.dp): Boolean {
    val at = screenPosition(point, size, density)
    val inset = with(density) { margin.toPx() }
    return at.x >= inset && at.y >= inset && at.x <= size.width - inset && at.y <= size.height - inset
}

/**
 * Web Mercator on the unit square, as map-view lays its map out: x and y in `[0, 1)`, origin top left, and a tile
 * 256 dp across at zoom 0.
 *
 * A copy, because map-view keeps its own internal; [fit], [screenPosition] and [isOnScreen] need it, and map-view has
 * no fit or on-screen test of its own. It must agree with map-view's to the last digit, which the tests pin, and it
 * goes if map-view ever exposes its projection.
 */
internal object WebMercator {
    const val WORLD_TILE = 256
    private const val MAX_LAT = 85.05112877980659

    fun x(lon: Double) = ((lon + 180.0).mod(360.0)) / 360.0
    fun y(lat: Double) = 0.5 - ln(tan(PI / 4 + lat.coerceIn(-MAX_LAT, MAX_LAT) * PI / 180.0 / 2)) / (2 * PI)
    fun lon(x: Double) = x.mod(1.0) * 360.0 - 180.0
    fun lat(y: Double) = atan(sinh(PI * (1 - 2 * y.coerceIn(0.0, 1.0)))) * 180.0 / PI
}
