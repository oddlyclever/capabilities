package com.oddlyclever.wayfinder

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.coderwise.libs.mapview.TileKey

/**
 * One layer of a map drawn without a composition: a map rendered to an image, on a device or on a server.
 *
 * Here in the base rather than in the module that renders, so that the vector and raster modules can each supply a
 * layer without the renderer depending on either: an app that draws only raster images to a file never pulls in the
 * vector renderer.
 */
interface MapFrameLayer {
    /** The levels the layer has. Past its last level the renderer magnifies the deepest tile. */
    val zoomRange: IntRange

    /**
     * The tile at [key], decoded and ready to draw, or null when the layer has none. A null tile is drawn from the
     * nearest ancestor the layer does have, magnified, as on a live map.
     */
    suspend fun tile(key: TileKey): MapFrameTile?
}

/** A tile, decoded and ready to draw without a composition. */
fun interface MapFrameTile {
    /**
     * Draws the [src] fraction of this tile (a square in `0..1`) to fill the whole of this scope. [zoom] is the
     * zoom the map is drawn at, floored, which is what line widths and zoom-gated paints are resolved against; it is
     * not the level the tile was cut at.
     */
    fun DrawScope.draw(src: Rect, zoom: Int)
}
