---
name: com-oddlyclever-wayfinder-vector
description: >-
  Vector basemaps for a wayfinder map in Compose Multiplatform: streets, roads,
  water, parks, buildings and place names drawn from vector tiles (Mapbox
  Vector Tile, MVT, .pbf) in the OpenMapTiles schema, rather than from image
  tiles. Use instead of writing an MVT decoder or a map renderer, for a light
  and a dark map style (night mode) that can follow the system theme, a custom
  colour palette or brand colours for the whole map, turning off
  point-of-interest, lake, river or peak labels, roads and names that stay
  sharp at every zoom and rotation, drawing vector tiles to bitmaps on the CPU
  to work around a GPU driver crash, and the same vector basemap on a map
  rendered to an image. Image tiles, such as satellite imagery or a hillshade,
  are com.oddlyclever:wayfinder-raster.
license: Apache-2.0
metadata:
  version: "0.0.7"
  repository: https://github.com/oddlyclever/capabilities
---

# wayfinder-vector

`com.oddlyclever:wayfinder-vector`, package `com.oddlyclever.wayfinder.vector`. Called from Kotlin only: there is no Swift or JavaScript surface. It brings `com.oddlyclever:wayfinder` with it, and map-view's vector renderer, whose `VectorStyle` and `NameStyle` are in `com.coderwise.libs.mapview.tiles.vector`.

## What it solves

A street map under a `WayfinderMap`: roads with casings that widen as the map zooms in, water, parks and buildings, and the names of places, roads, lakes, rivers, peaks and points of interest drawn over every tile so they are never cut off at a tile's edge. The same tiles give a daytime map and a night map, or a map in an app's own colours, without a second tile source. Tiles are decoded off the main thread once, as they arrive, and drawn as paths, so they stay sharp when the map is magnified or turned.

## How it is meant to be used

Make the basemap once and hand it to the map. `VectorStyleProvider` gives every basemap inside it a style, the way a theme gives colours, and `systemVectorStyle()` follows the system's dark mode as it changes:

```kotlin
import com.oddlyclever.wayfinder.*
import com.oddlyclever.wayfinder.vector.*

val streets = TileSource("streets", "https://tiles.example.com/{z}/{x}/{y}.pbf", zoomRange = 0..14, attribution = "© Example contributors")
val basemap = vectorBasemap(streets, client) // client: the app's TileClient

@Composable
fun MapScreen(state: WayfinderMapState) {
    VectorStyleProvider(systemVectorStyle()) {
        WayfinderMap(state, layers = listOf(basemap))
    }
}
```

A style of your own is `MapStyles.Light` or `MapStyles.Dark` recoloured or adjusted, kept in a top-level `val`. A palette changes colour and nothing else: road widths, the zooms at which casings and outlines appear, and paint order stay map-view's. A label kind that is turned off is not even decoded.

```kotlin
val Brand: VectorStyle = MapStyles.Light.recolored(MapPalette.Light.copy(water = Color(0xFF8FB8D8), land = Color(0xFFF2EEE6)))
val Quiet: VectorStyle = MapStyles.Light.copy(poiName = NameStyle(9.5f, enabled = false))

val quietBasemap = vectorBasemap(streets, client, style = Quiet)
```

For a map drawn without a composition, by `renderMap` or `MapFrame` in `com.oddlyclever:wayfinder-frame`, the same basemap is `vectorFrameLayer`. Give it a `TextMeasurer` to draw names:

```kotlin
val names = rememberTextMeasurer()
val layer = vectorFrameLayer(streets.zoomRange, style = MapStyles.Dark, names = names) { key -> streets.fetch(client, key) }
```

`vectorBasemap(..., rasterize = true)` draws each tile's ground to an image on the CPU instead of as paths on the GPU, for an app that has seen a GPU driver fault on complex paths on some device; names are still drawn from the decoded tile. `rasterizeVectorTile(bytes, zoom, density, style)` is that step on its own.

## Invariants and traps

**Make each basemap once.** `vectorBasemap(...)` returns a new layer on every call, and the map keys the layer's tiles on that object, so a call inside a composable that recomposes throws away every decoded tile: the map blanks and refetches whenever unrelated state changes. Keep it in a top-level or view-model `val`, or `remember` it.

**Build a recoloured style once.** `recolored` makes new lookups on every call, and a `VectorStyle` is compared by its lookups, so `MapStyles.Light.recolored(palette)` written in a composition is a new style at every recomposition, and every tile is fetched and decoded again each time. Use `MapStyles`, or a style in a top-level `val`. Even a real change of style, such as light to dark, starts fresh tiles and fetches each again through the client, which is one reason to cache in the client.

**`MapStyles.Light` is not `MapPalette.Light`.** `MapStyles.Light` is map-view's own default style; `MapPalette.Light` is a different, warmer daytime palette. `MapStyles.Light.recolored(MapPalette.Light)` is a different-looking map from `MapStyles.Light`.

**The tiles must be OpenMapTiles.** The style reads OpenMapTiles' layer names, `water`, `landcover`, `landuse`, `park`, `building`, `transportation` and the rest, and their `class` tags. Tiles in another schema decode, but their layers are not drawn, so the map comes out as bare land with no roads or water.

**The bytes must be a plain vector tile.** The decoder does not unzip: gzipped tile bytes, as an MBTiles file stores them, read as no tile, and the map stays blank. A server's gzip content encoding is the HTTP client's to undo. An empty byte array is not a missing tile but an empty one, drawn as plain land instead of the magnified tile above it: return null for a tile that is not there.

**A frame layer does not follow the provider.** `vectorFrameLayer` is not composable, so it never reads `VectorStyleProvider`: its `style` defaults to `MapStyles.Light` even beside a dark live map. Pass the same style to both.

**A frame layer without a `TextMeasurer` has no names**, and says nothing about it. Laying text out on Android needs a context, which is why the app gives it: `rememberTextMeasurer()` in a composable. Names cost a second decode of each tile.

**Rasterizing is a workaround, not a speed-up.** It costs tens of milliseconds a tile on the CPU, and an image magnified past its level blurs where a vector tile stays sharp. Nothing turns it on by itself: which devices get it is the app's decision.

**`labels = false` drops every name from the live map**, not only some kinds. To drop one kind, turn off its `NameStyle` in the style: `poiName`, `waterName`, `riverName` or `peakName`.

## What moved, and what it used to be called

`wayfinder-vector` was first released in 0.0.3, so no earlier versions exist, and its API has not changed since.

- **There is no MapLibre or Mapbox style JSON.** A style is a Kotlin `VectorStyle`, and a vector basemap is `vectorBasemap(source, client)`, not a style URL.
- **There is no device list.** The ground is drawn as paths on the GPU on every device unless the app passes `rasterize = true`.
- **It was never published under `kartog`.** A project that built from source before 0.0.3 may have `com.oddlyclever.kartog.vector` imports: `KartogStyles` is now `MapStyles` and `staticVectorLayer` is now `vectorFrameLayer`.

## What it is not for

It draws OpenMapTiles vector tiles and nothing else: image tiles are `rasterBasemap` in `com.oddlyclever:wayfinder-raster`, and GeoJSON is in `com.oddlyclever:wayfinder-geojson`. It does not host, fetch or cache tiles, and it does not read MapLibre style documents. Dashed footpaths, and tunnels and bridges drawn apart from other roads, are not drawn yet, since map-view's road paint sees only a road's class.
