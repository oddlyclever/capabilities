package com.oddlyclever.wayfinder.raster

import com.coderwise.libs.mapview.tiles.rememberTileState
import com.coderwise.libs.mapview.tiles.shown
import com.oddlyclever.wayfinder.MapLayer
import com.oddlyclever.wayfinder.PlacedLayer
import com.oddlyclever.wayfinder.TileClient
import com.oddlyclever.wayfinder.TileSource
import com.oddlyclever.wayfinder.fetch

/**
 * A raster basemap for a `WayfinderMap`: [source]'s image tiles fetched through [client], decoded off the main thread,
 * and faded in as they arrive. [alpha] draws it translucent, over another layer.
 */
fun rasterBasemap(
    source: TileSource,
    client: TileClient,
    alpha: Float = 1f,
    fadeMillis: Int = DEFAULT_FADE_MILLIS,
): MapLayer = MapLayer { _ ->
    val tiles = rememberTileState(key = source, zoomRange = source.zoomRange) { key ->
        decodeRasterTile(source.fetch(client, key))
    }
    PlacedLayer(
        tiles,
        listOf { key -> tiles.shown(key)?.let { FadingRasterSlot(it.content, it.src, alpha = alpha, fadeMillis = fadeMillis) } },
        source.attribution,
    )
}
