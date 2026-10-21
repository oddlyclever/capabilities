package com.oddlyclever.kartog.staticmap

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import com.coderwise.libs.mapview.LatLon
import com.coderwise.libs.mapview.MapCameraState
import com.coderwise.libs.mapview.TileKey
import com.coderwise.libs.mapview.onTileGrid
import com.oddlyclever.kartog.CameraPosition
import com.oddlyclever.kartog.StaticLayer
import com.oddlyclever.kartog.StaticTile
import com.oddlyclever.kartog.screenPosition
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.pow

/**
 * A map drawn to an image of [size] pixels, without a composition: on a device for a share sheet or a notification,
 * or on a server for an email.
 *
 * [layers] are drawn in order, each on the tile grid map-view would use for [position], turned to its bearing and
 * magnified past each layer's last level. A tile a layer does not have is drawn from its nearest ancestor, up to five
 * levels up, as on a live map; where there is none, [background] shows. [overlay] is drawn last, upright, through a
 * [StaticProjection] that places a coordinate where it falls on the image: a track ([drawTrack]), pins, shapes.
 *
 * Every layer's tiles are fetched at once, so the time it takes is the slowest tile's, not the sum.
 *
 * @throws IllegalArgumentException when [size] is empty.
 */
suspend fun renderMap(
    size: IntSize,
    density: Density,
    position: CameraPosition,
    layers: List<StaticLayer>,
    background: Color = Color.Transparent,
    overlay: DrawScope.(StaticProjection) -> Unit = {},
): ImageBitmap = coroutineScope {
    require(size.width > 0 && size.height > 0) { "A map image needs a size; got $size." }
    val placed = layers.map { layer -> async { place(layer, size, density, position) } }.awaitAll()
    val image = ImageBitmap(size.width, size.height)
    val canvas = Canvas(image)
    val zoom = floor(position.zoom).toInt()
    CanvasDrawScope().draw(density, LayoutDirection.Ltr, canvas, Size(size.width.toFloat(), size.height.toFloat())) {
        drawRect(background)
        translate(size.width / 2f, size.height / 2f) {
            // The map turns the other way to put the bearing at the top, as map-view's camera does.
            rotate(-position.bearing, pivot = Offset.Zero) {
                for (tiles in placed) for (tile in tiles) drawTile(tile, density, zoom)
            }
        }
        overlay(StaticProjection(position, size, density))
    }
    image
}

/**
 * Where a coordinate falls on a map image, for drawing over it. Built by [renderMap] for its overlay; build one
 * yourself to draw over an image rendered earlier with the same [position], [size] and [density].
 */
class StaticProjection(val position: CameraPosition, val size: IntSize, val density: Density) {
    private val camera = MapCameraState(position.center, position.zoom, position.bearing)

    /** Where [point] falls on the image, in pixels from its top left, with the bearing taken into account. */
    fun project(point: LatLon): Offset = camera.screenPosition(point, size, density)

    /** How many metres of ground a pixel covers at the middle of the image, for drawing a distance as a radius. */
    val metresPerPixel: Double get() = camera.metersPerDp / density.density
}

/** A tile ready to draw, where it goes in the turned frame centred on the camera, and how big. */
private class Placed(val tile: StaticTile, val src: Rect, val left: Float, val top: Float, val side: Float)

/** Each tile of [layer] covering the image, fetched together, with ancestors standing in for tiles it lacks. */
private suspend fun place(layer: StaticLayer, size: IntSize, density: Density, position: CameraPosition): List<Placed> =
    coroutineScope {
        val level = floor(position.zoom).toInt().coerceIn(layer.zoomRange)
        val side = (TILE_DP * density.density * 2.0.pow((position.zoom - level).toDouble())).toFloat()
        val centre = position.center.onTileGrid(level.toDouble())
        // Enough tiles to cover the image's diagonal, which is what a turned image can show.
        val reach = hypot(size.width.toDouble(), size.height.toDouble()) / 2 / side
        val count = 1 shl level
        val fetched = HashMap<TileKey, Deferred<StaticTile?>>()
        fun fetch(key: TileKey) = fetched.getOrPut(key) { async { runCatching { layer.tile(key) }.getOrNull() } }

        val cells = buildList {
            for (row in floor(centre.y - reach).toInt()..floor(centre.y + reach).toInt()) {
                if (row !in 0 until count) continue
                for (column in floor(centre.x - reach).toInt()..floor(centre.x + reach).toInt()) {
                    add(Triple(column, row, fetch(TileKey(level, column.mod(count), row))))
                }
            }
        }
        cells.mapNotNull { (column, row, own) ->
            val left = ((column - centre.x) * side).toFloat()
            val top = ((row - centre.y) * side).toFloat()
            own.await()?.let { return@mapNotNull Placed(it, WHOLE_TILE, left, top, side) }
            standIn(level, column.mod(count), row, layer, ::fetch)?.let { (tile, src) -> Placed(tile, src, left, top, side) }
        }
    }

/** The nearest ancestor the layer has, with the fraction of it this cell covers. */
private suspend fun standIn(
    level: Int,
    x: Int,
    y: Int,
    layer: StaticLayer,
    fetch: (TileKey) -> Deferred<StaticTile?>,
): Pair<StaticTile, Rect>? {
    for (up in 1..STAND_IN_LEVELS) {
        val z = level - up
        if (z < layer.zoomRange.first) return null
        val fraction = 1f / (1 shl up)
        val tile = fetch(TileKey(z, x shr up, y shr up)).await() ?: continue
        val left = (x - ((x shr up) shl up)) * fraction
        val top = (y - ((y shr up) shl up)) * fraction
        return tile to Rect(left, top, left + fraction, top + fraction)
    }
    return null
}

/** Draws one tile into its square; a pixel wider than its spacing, so neighbours meet without a hairline seam. */
private fun DrawScope.drawTile(placed: Placed, density: Density, zoom: Int) {
    val canvas = drawContext.canvas
    canvas.save()
    canvas.translate(placed.left, placed.top)
    val side = ceil(placed.side) + 1f
    CanvasDrawScope().draw(density, LayoutDirection.Ltr, canvas, Size(side, side)) {
        with(placed.tile) { draw(placed.src, zoom) }
    }
    canvas.restore()
}

/** A tile's side in dp, as map-view lays tiles out. */
private const val TILE_DP = 256

/** How far up the pyramid a missing tile looks for one to stand in for it, as map-view's own fallback does. */
private const val STAND_IN_LEVELS = 5

private val WHOLE_TILE = Rect(0f, 0f, 1f, 1f)
