---
name: com-oddlyclever-wayfinder-raster
description: >-
  Raster basemaps for a wayfinder map in Compose Multiplatform: image tiles in
  PNG, JPEG or WebP, such as satellite or aerial imagery, a topographic or
  scanned map, rendered street-map tiles, or a translucent hillshade or weather
  overlay stacked over another basemap. Use instead of writing your own tile
  image decoding and fade-in, to decode tile bytes to an ImageBitmap off the
  main thread, treat a broken image or an HTTP error page as a missing tile
  rather than a crash, fade tiles in as they arrive without flicker when the
  user pans back, set a layer's opacity, and draw the same image tiles on a map
  rendered to an image. Street maps drawn from vector tiles are
  com.oddlyclever:wayfinder-vector.
license: Apache-2.0
metadata:
  version: "0.0.7"
  repository: https://github.com/oddlyclever/capabilities
---

# wayfinder-raster

`com.oddlyclever:wayfinder-raster`, package `com.oddlyclever.wayfinder.raster`. Called from Kotlin only: there is no Swift or JavaScript surface. It brings `com.oddlyclever:wayfinder` with it, and map-view's raster slot.

## What it solves

A map whose ground is pictures: imagery from a satellite or an aircraft, a topographic sheet, a rendered street map, or a shaded-relief layer laid at half opacity over a street map. Each tile is fetched through the app's own `TileClient`, decoded to an image once, off the main thread, and faded in as it arrives, so a pan does not stutter on decoding and a tile that arrives late does not pop in. A server that answers with an error page or a truncated image costs one missing tile, drawn from its nearest ancestor, and never a crash.

## How it is meant to be used

Pass the layers to the map in order, bottom first. A translucent layer goes after the one it covers:

```kotlin
import com.oddlyclever.wayfinder.*
import com.oddlyclever.wayfinder.raster.*

val imagery = TileSource(id = "imagery", urlTemplate = "https://tiles.example.com/imagery/{z}/{x}/{y}.jpg", zoomRange = 0..18, attribution = "© Example imagery")
val shade = TileSource(id = "shade", urlTemplate = "https://tiles.example.com/shade/{z}/{x}/{y}.png", zoomRange = 0..15, attribution = "© Example elevation")

// client: the app's TileClient. Made once, outside composition.
val layers = listOf(rasterBasemap(imagery, client), rasterBasemap(shade, client, alpha = 0.5f))

@Composable
fun ImageryMap(state: WayfinderMapState) {
    WayfinderMap(state, layers = layers)
}
```

`rasterBasemap(source, client, alpha = 1f, fadeMillis = 300)`: `alpha` scales the whole layer, and `fadeMillis = 0` turns the fade off. A raster layer stacks over a vector basemap the same way, after it in the list.

For a map drawn without a composition, by `renderMap` or `MapFrame` in `com.oddlyclever:wayfinder-frame`, the same tiles are `rasterFrameLayer`:

```kotlin
val frameLayer = rasterFrameLayer(imagery.zoomRange) { key -> imagery.fetch(client, key) }
```

For code that builds its own map-view layer, `decodeRasterTile(bytes)` is the decoding step, meant for a tile state's `load`, and `FadingRasterSlot(tile, src, alpha = 1f, fadeMillis = 300)` is map-view's `RasterSlot` with the fade.

## Invariants and traps

**Make the client once.** Two `rasterBasemap(...)` calls with equal arguments give equal layers, and the map keeps a layer's tiles while an equal one is passed, so the call can sit inside a composable. The `TileClient` is compared by identity, though: one written inline as `TileClient { … }` is new at every recomposition, and every tile is fetched and decoded again each time. Changing `alpha` or `fadeMillis` is a new layer too, and starts its tiles again. Keep the client in a top-level or view-model `val`, or `remember` it.

**Nothing that goes wrong is reported.** `decodeRasterTile` returns null for no bytes, empty bytes, an error page, a truncated image or a format the platform cannot read, and an exception thrown by the `TileClient` is swallowed and the tile retried after ten seconds. Either way the cell shows its nearest ancestor magnified, or nothing, and no error reaches the app. A blank or blurry map means the client is failing: log inside it.

**A TMS source must say so.** A source that numbers rows from the bottom needs `TileSource(..., tms = true)`; without it every tile lands in the wrong row and the map is a scramble of the right tiles in the wrong places.

**Give the source's real zoom range.** Past the end of `zoomRange` the deepest tile is magnified, which blurs, as any picture does. A range that reaches past what the server has asks for tiles that fail, and asks again every ten seconds.

**Decoded tiles are large.** Each layer keeps up to 96 decoded tiles. At 256 pixels a tile is about a quarter of a megabyte, and a 512-pixel tile four times that, so a few stacked layers of high-resolution tiles hold a lot of memory. The tiles go when the map leaves the composition and are fetched again when it returns, so a cache belongs in the client.

**Only a late tile fades.** A tile that arrives after its cell has shown nothing fades in; a cell composed with its tile already loaded draws it at once, so panning back over loaded ground does not flicker. `alpha` applies with or without the fade.

**WebP is the platform's.** PNG and JPEG decode on every target; WebP decodes only where the platform's image decoder reads it, and where it does not, every tile is missing.

## What moved, and what it used to be called

`wayfinder-raster` was first released in 0.0.3, so no earlier versions exist, and its API has not changed since.

- **Since 0.0.7, `rasterBasemap` layers from equal arguments are equal.** Before it, each call was a new layer, and one made in a composable lost its tiles at every recomposition.

- **There is no tile provider class.** A raster layer is `rasterBasemap(source, client)` in a `WayfinderMap`'s `layers`, not a `TileOverlay` or `UrlTileProvider`: the URL is the `TileSource`'s template, and the fetching is the app's `TileClient`.
- **It was never published under `kartog`.** A project that built from source before 0.0.3 may have `com.oddlyclever.kartog.raster` imports; `staticRasterLayer` is now `rasterFrameLayer`.

## What it is not for

It draws image tiles and nothing else: vector tiles are `vectorBasemap` in `com.oddlyclever:wayfinder-vector`, and GeoJSON is in `com.oddlyclever:wayfinder-geojson`. It does not fetch, cache or store tiles, and it does not reproject, crop or resize them.
