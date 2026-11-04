package com.oddlyclever.wayfinder.raster

import androidx.compose.runtime.Composable
import com.coderwise.libs.mapview.tiles.rememberTileState
import com.coderwise.libs.mapview.tiles.shown
import com.oddlyclever.wayfinder.MapLayer
import com.oddlyclever.wayfinder.PlacedLayer
import com.oddlyclever.wayfinder.TileClient
import com.oddlyclever.wayfinder.TileSource
import com.oddlyclever.wayfinder.WayfinderMapState
import com.oddlyclever.wayfinder.fetch

/**
 * A raster basemap for a `WayfinderMap`: [source]'s image tiles fetched through [client], decoded off the main thread,
 * and faded in as they arrive. [alpha] draws it translucent, over another layer.
 *
 * Two calls with equal arguments give equal layers, and the map keeps a layer's tiles for as long as an equal layer is
 * passed, so calling this inside a composable keeps every loaded tile across recompositions. [client] is compared by
 * identity, as a lambda is: a client made once keeps the tiles, one written inline as a new lambda on each
 * recomposition does not.
 */
fun rasterBasemap(
    source: TileSource,
    client: TileClient,
    alpha: Float = 1f,
    fadeMillis: Int = DEFAULT_FADE_MILLIS,
): MapLayer = RasterBasemap(source, client, alpha, fadeMillis)

/** A data class so that the map, which keys each layer's tiles on the layer, sees equal arguments as the same layer. */
private data class RasterBasemap(
    val source: TileSource,
    val client: TileClient,
    val alpha: Float,
    val fadeMillis: Int,
) : MapLayer {
    @Composable
    override fun place(state: WayfinderMapState): PlacedLayer {
        val tiles = rememberTileState(key = source, zoomRange = source.zoomRange) { key ->
            decodeRasterTile(source.fetch(client, key))
        }
        return PlacedLayer(
            tiles,
            listOf { key -> tiles.shown(key)?.let { FadingRasterSlot(it.content, it.src, alpha = alpha, fadeMillis = fadeMillis) } },
            source.attribution,
        )
    }
}
