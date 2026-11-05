# Raster Basemaps

DOC-0038 · 2026-11-04
Keywords: rasterBasemap, image tiles, satellite imagery, aerial imagery, topographic map, hillshade overlay, weather overlay, translucent layer, layer opacity, alpha, stack layers, fade in tiles, tiles pop in, tiles flicker, TMS, tiles in the wrong place, scrambled map, tiles reload on recomposition, TileClient lambda, attribution, memory use, tile cache, TileOverlay, UrlTileProvider

`rasterBasemap` is the module's live-map call: a `MapLayer` for a `WayfinderMap` whose tiles are images. This page covers the call, stacking layers, the fade, how a layer keeps its tiles across recompositions, and the memory it holds.

## The call

```kotlin
val imagery = TileSource(id = "imagery", urlTemplate = "https://tiles.example.com/imagery/{z}/{x}/{y}.jpg", zoomRange = 0..18, attribution = "© Example imagery")

WayfinderMap(state, layers = listOf(rasterBasemap(imagery, client)))
```

| Parameter | Default | What it does |
|---|---|---|
| `source` | required | The `TileSource`: the URL template, the levels it has, the row order and the attribution. |
| `client` | required | The app's `TileClient`, which fetches each tile's bytes. |
| `alpha` | `1f` | The layer's opacity, faded or not. |
| `fadeMillis` | `300` | How long a newly arrived tile takes to fade in. `0` turns the fade off. |

Each tile is fetched through the client, decoded to an image once, off the main thread, and faded in as it arrives. The module fetches nothing itself: the client builds the URL from the source, adds any key or header, and returns the body or null. `TileSource`, `TileClient` and `WayfinderMap` are `wayfinder`'s; see [wayfinder](../wayfinder/README.md). What happens to bytes that are not an image is in [Missing and Broken Tiles](DOC-0039-missing-and-broken-tiles.md).

There is no tile provider class, no `TileOverlay` or `UrlTileProvider`. The URL is the `TileSource`'s template, and the fetching is the app's `TileClient`.

The source's `attribution` is shown in the map's attribution ornament, joined with the other layers' credits.

## The source

- **Give the source's real zoom range.** Past the last level of `zoomRange` the deepest tile is magnified, which blurs, as any picture does. A range that reaches past what the server has asks for tiles that fail, and asks again every ten seconds. A `TileSource` with no range is given map-view's default, `0..19`.
- **A TMS source must say so.** A source that numbers rows from the bottom needs `TileSource(…, tms = true)`. Without it every tile lands in the wrong row, and the map is a scramble of the right tiles in the wrong places.

## Stacking layers

A `WayfinderMap` draws its layers in order, first at the bottom. A translucent layer goes after the one it covers:

```kotlin
val shade = TileSource(id = "shade", urlTemplate = "https://tiles.example.com/shade/{z}/{x}/{y}.png", zoomRange = 0..15, attribution = "© Example elevation")

val layers = listOf(rasterBasemap(imagery, client), rasterBasemap(shade, client, alpha = 0.5f))
```

A raster layer stacks over a vector basemap from [wayfinder-vector](../wayfinder-vector/README.md) the same way, after it in the list.

## Fading in

A tile that arrives into an empty cell fades in over `fadeMillis`, 300 by default: long enough to read as arriving, short enough not to lag a pan.

- **Only a tile arriving after its cell showed nothing fades.** A cell composed with its tile already in hand draws it at once, so tiles you pan back over do not flicker in again.
- **A tile that replaces a magnified ancestor does not fade.** The cell was already showing something, so its own tile is drawn at once.
- **`alpha` applies with or without the fade.** It scales the whole layer, and the fade runs from nothing to `alpha`.

## Keeping tiles across recompositions

The map keeps a layer's tiles for as long as it is passed an equal layer, and two `rasterBasemap` calls with equal arguments return equal layers. So the call can sit inside a composable, and every decoded tile survives recomposition.

- **The client is compared as an object.** A `TileClient { … }` lambda is equal only to itself. Written inline in a composable, it is a new client at every recomposition, so the layer is new, and every tile is fetched and decoded again each time. Make the client once: a top-level or view-model `val`, or `remember` it.
- **`TileSource` compares by its values**: id, template, zoom range, row order and attribution. A source made again with the same values is the same source.
- **Every argument counts.** Another source, client, `alpha` or `fadeMillis` is a new layer, which starts with no tiles. So `alpha` is not one to animate: each new value would fetch and decode every tile again.

Before 0.0.7 every call made a new layer, and a basemap made inside a composable lost its tiles at every recomposition.

## Memory and caching

Each layer keeps up to 96 decoded tiles, evicting the one least recently on screen. A decoded 256-pixel tile is about a quarter of a megabyte, and a 512-pixel tile four times that, so a few stacked layers of high-resolution tiles hold a lot of memory.

The tiles go when the map leaves the composition, and are fetched again when it comes back. The module keeps no other cache, so a cache belongs in the client. `wayfinder`'s `TileClient` KDoc shows one put in front of the HTTP client.

## What it does not do

It draws image tiles and nothing else. It does not fetch, cache or store tiles, and it does not reproject, crop or resize them. Vector tiles are `vectorBasemap` in [wayfinder-vector](../wayfinder-vector/README.md), and GeoJSON is in [wayfinder-geojson](../wayfinder-geojson/README.md).
