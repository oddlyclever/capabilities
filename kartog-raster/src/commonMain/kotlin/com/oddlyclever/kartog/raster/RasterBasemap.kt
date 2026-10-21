package com.oddlyclever.kartog.raster

import com.coderwise.libs.mapview.tiles.rememberTileState
import com.coderwise.libs.mapview.tiles.shown
import com.oddlyclever.kartog.KartogLayer
import com.oddlyclever.kartog.PlacedLayer
import com.oddlyclever.kartog.TileClient
import com.oddlyclever.kartog.TileSource
import com.oddlyclever.kartog.fetch

/**
 * A raster basemap for a `KartogMap`: [source]'s image tiles fetched through [client], decoded off the main thread,
 * and faded in as they arrive. [alpha] draws it translucent, over another layer.
 */
fun rasterBasemap(
    source: TileSource,
    client: TileClient,
    alpha: Float = 1f,
    fadeMillis: Int = DEFAULT_FADE_MILLIS,
): KartogLayer = KartogLayer { _ ->
    val tiles = rememberTileState(key = source, zoomRange = source.zoomRange) { key ->
        decodeRasterTile(source.fetch(client, key))
    }
    PlacedLayer(
        tiles,
        listOf { key -> tiles.shown(key)?.let { FadingRasterSlot(it.content, it.src, alpha = alpha, fadeMillis = fadeMillis) } },
        source.attribution,
    )
}
