# wayfinder-raster

Raster tiles for [wayfinder](../wayfinder): a raster basemap for `WayfinderMap`, tile bytes decoded to images off the main thread, and tiles that fade in as they arrive.

**Targets:** JVM · Android · iOS (arm64, simulator) · JS · wasmJs

## Install

```kotlin
implementation("com.oddlyclever:wayfinder-raster:0.0.4")
```

It exposes `wayfinder` and map-view-tiles-raster as `api`. There is no dependency-injection container.

## A raster basemap

```kotlin
val imagery = TileSource(id = "imagery", urlTemplate = "https://tiles.example.com/imagery/{z}/{x}/{y}.jpg", zoomRange = 0..18, attribution = "© Example imagery")

WayfinderMap(state, layers = listOf(rasterBasemap(imagery, client)))
```

`rasterBasemap(source, client, alpha = 1f, fadeMillis = 300)` fetches each tile through the client, decodes it off the main thread, and fades it in. Layers stack, so a translucent raster layer over another basemap is `rasterBasemap(hillshade, client, alpha = 0.5f)` after it in the list.

## Decoding

`decodeRasterTile(bytes)` decodes on `Dispatchers.Default` whatever thread calls it, so a tile is decoded once, as it arrives, upstream of the view. A missing, empty, truncated or non-image answer, such as a server's error page, is null: a missing tile, which the map draws from the nearest ancestor instead of failing. PNG and JPEG decode on every target, and WebP wherever the platform's decoder reads it.

## Fading in

`FadingRasterSlot(tile, src, alpha, fadeMillis)` is map-view's `RasterSlot` with a fade, for use in a map-view layer directly.

- **Only a tile arriving after its cell showed nothing fades in.** A cell composed with its tile already in hand draws it at once, so tiles you pan back over do not flicker in again.
- **`fadeMillis = 0`** turns the fade off; `alpha` scales the whole slot, faded or not.

## On an image

`rasterFrameLayer(zoomRange) { key -> bytes }` is the same basemap for wayfinder-frame's `renderMap`.
