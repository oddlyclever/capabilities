# Raster Tiles Outside a WayfinderMap

DOC-0040 · 2026-11-04
Keywords: decodeRasterTile, FadingRasterSlot, RasterSlot with fade, custom map-view layer, tile state load, decode tile bytes to ImageBitmap, rasterFrameLayer, renderMap, MapFrame, map as an image, static map, thumbnail, frame refetches tiles

`rasterBasemap` is built from parts that are public on their own: the decoding step, the fading slot, and a frame layer for a map drawn without a composition. This page covers each, for code that builds its own map-view layer or draws a map to an image.

## Decoding: decodeRasterTile

`decodeRasterTile(bytes)` turns a tile's bytes into an `ImageBitmap`, or null when there is nothing to draw.

- **It runs on `Dispatchers.Default`, whatever thread calls it.** Decoding a 256-pixel tile takes long enough to cost a frame on the main thread.
- **Call it from a tile state's `load`,** upstream of the view, so each tile is decoded once as it arrives rather than whenever a slot wants it.
- **Null covers every failure**: no bytes, empty bytes, an error page, a truncated image, or a format the platform cannot read. The list, and the formats, are in [Missing and Broken Tiles](Wayfinder-Raster-Missing-And-Broken-Tiles). Cancellation is not swallowed: a cancelled caller is still cancelled.

```kotlin
val tiles = rememberTileState(key = imagery, zoomRange = imagery.zoomRange) { key ->
    decodeRasterTile(imagery.fetch(client, key))
}
```

## Fading: FadingRasterSlot

`FadingRasterSlot(tile, src, modifier = Modifier, alpha = 1f, fadeMillis = 300)` is map-view's `RasterSlot` with a fade, for a map-view layer of your own.

- **Only a tile that arrives after its cell showed nothing fades.** The slot decides when it is first composed: with a tile already in hand, or with `fadeMillis` of 0, it draws at full strength at once. So panning back over loaded tiles does not make them flicker in again.
- **Null draws nothing,** leaving whatever is beneath the layer.
- **`alpha` scales the whole slot**, faded or not, for a layer drawn translucent over another.
- **The fade redraws, it does not recompose.** The slot's opacity is read in the draw phase, so each frame of the fade costs a redraw of the layer only.

`DEFAULT_FADE_MILLIS`, 300, is the default for both `FadingRasterSlot` and `rasterBasemap`. The rules for a basemap's fade are in [Raster Basemaps](Wayfinder-Raster-Raster-Basemaps).

## On an image: rasterFrameLayer

`rasterFrameLayer(zoomRange) { key -> bytes }` is the same tiles for a map drawn without a composition: `renderMap` and `MapFrame` in [wayfinder-frame](Wayfinder-Frame), and the server-side images of [wayfinder-headless](Wayfinder-Headless).

```kotlin
val frameLayer = rasterFrameLayer(imagery.zoomRange) { key -> imagery.fetch(client, key) }
```

- **`zoomRange`** is the levels the source has. Past its last level the renderer magnifies the deepest tile.
- **Bytes are decoded with `decodeRasterTile`,** so a missing or unreadable tile leaves the cell to its nearest ancestor, magnified, as on a live map.
- **Each call returns a new layer, equal only to itself.** A `MapFrame` given `listOf(rasterFrameLayer(…))` written inline in a composable sees new layers at every recomposition and draws its map again, fetching every tile. Make the layers once, in a `remember` or outside the composable.
