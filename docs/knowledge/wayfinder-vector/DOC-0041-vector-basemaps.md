# Vector Basemaps

DOC-0041 · 2026-11-04
Keywords: vectorBasemap, vector tiles, Mapbox Vector Tile, MVT, .pbf, OpenMapTiles schema, street map, blank map, map shows only land, no roads or water, gzip tiles, MBTiles, Content-Encoding, tiles reload on recomposition, map refetches every tile, TileClient lambda, labels off, names over every tile, attribution, tile cache, memory, MapLibre style URL, Mapbox style JSON

`vectorBasemap` is the module's live-map call: a `MapLayer` for a `WayfinderMap` that draws a source's vector tiles as a street map with names over it. This page covers what it does with each tile, what the tiles have to be, how a layer keeps its tiles across recompositions, and what goes wrong.

## The call

```kotlin
val streets = TileSource("streets", "https://tiles.example.com/{z}/{x}/{y}.pbf", zoomRange = 0..14, attribution = "© Example contributors")
val basemap = vectorBasemap(streets, client)   // client: the app's TileClient, made once

WayfinderMap(state, layers = listOf(basemap))
```

| Parameter | Default | What it does |
|---|---|---|
| `source` | required | The `TileSource`: the URL template, the levels it has, and its attribution. |
| `client` | required | The app's `TileClient`, which fetches each tile's bytes. |
| `style` | `null` | The style to draw in. Null takes the one from `VectorStyleProvider`, else `MapStyles.Light`. See [Light, Dark and Custom Styles](DOC-0042-light-dark-and-custom-styles.md). |
| `labels` | `true` | Whether names are drawn. |
| `rasterize` | `false` | Draw each tile's ground to an image on the CPU instead of as paths. See [Drawing Tiles as Images](DOC-0043-drawing-tiles-as-images.md). |

The module fetches nothing itself. Each tile is asked of `client`, which builds the URL, adds any key or header, and returns the body or null. `TileSource`, `TileClient` and `WayfinderMap` are `wayfinder`'s; see [wayfinder](../wayfinder/README.md).

## What happens to a tile

1. The client returns the tile's bytes.
2. map-view's decoder turns them into the paths that draw the tile and the names on it. It runs on `Dispatchers.Default`, whatever thread asks, so decoding never costs a frame on the main thread.
3. The decoded tile is kept, so each tile is decoded once, as it arrives, not each time it is drawn.
4. map-view's renderer draws it in two layers: the ground first, then the names in a layer of their own. Every name is drawn over every tile, so a name that runs past its tile's edge is never covered by the neighbouring tile.

The names are places, roads, water, rivers, peaks and points of interest, as the style asks. `labels = false` hides them all. It does not save the work of decoding them; turning a kind off in the style does, as [Light, Dark and Custom Styles](DOC-0042-light-dark-and-custom-styles.md) explains.

The source's `attribution` is shown in the map's attribution ornament, joined with the other layers' credits.

## Missing tiles and zooming past the source

A cell whose tile has not arrived, or does not exist, is drawn from its nearest ancestor, magnified, up to five levels up. With no ancestor either, the cell is empty and the map's background shows.

Past the last level of `zoomRange` the deepest tile is magnified. A vector tile stays sharp when it is magnified, and its roads are drawn at the width the camera's zoom asks for, not the width of the level the tile was cut at.

A tile that fails is asked for again after ten seconds, for as long as it is wanted on screen. That retry is map-view's tile queue, not something the module adds.

## The tiles must be OpenMapTiles

The styles read OpenMapTiles' layer names, `water`, `waterway`, `landcover`, `landuse`, `park`, `building`, `aeroway`, `boundary` and `transportation`, and their `class` tags (and `admin_level` for boundaries). Names come from `place`, `transportation_name`, `waterway`, `water_name`, `poi` and `mountain_peak`. A layer the style does not read is not decoded at all, so tiles in another schema come out as bare land: no roads, no water, no names, and no error.

There is no MapLibre or Mapbox style document and no style URL. A style is a Kotlin `VectorStyle`, and the basemap is `vectorBasemap(source, client)`.

## The bytes must be a plain vector tile

The decoder reads an uncompressed Mapbox Vector Tile and nothing else.

- **Gzipped bytes are not a tile.** Vector tiles from an MBTiles file are usually stored gzipped, and a server that sends them with `Content-Encoding: gzip` relies on the HTTP client to undo it. Bytes still compressed fail to decode, which counts as a failed tile: it is drawn from its ancestor, if any, and asked for again in ten seconds. Decompress in the client.
- **An empty byte array is a tile**, of open ground with nothing on it. It is drawn as plain land, not replaced by the magnified tile above it. Return null for a tile that is not there.
- **An error page is not a tile.** HTML or JSON from the server fails to decode, and is a failed tile like gzipped bytes.

Nothing reports a failure. An exception thrown by the client is caught by map-view's tile queue and treated like a null. When the map is blank or blurry, log inside the client.

## Keeping tiles across recompositions

The map keeps a layer's tiles for as long as it is passed an equal layer, and two `vectorBasemap` calls with equal arguments return equal layers. So the call can sit inside a composable, and every decoded tile survives recomposition.

"Equal arguments" has two catches:

- **The client is compared as an object.** A `TileClient { … }` lambda is equal only to itself. Written inline in a composable, it is a new client at every recomposition, so the layer is new, and every tile is fetched and decoded again each time. Make the client once: a top-level or view-model `val`, or `remember` it.
- **The style is compared by its lookups.** A style built in the composition, such as `MapStyles.Light.recolored(palette)`, is a new style every time, with the same result. Keep it in a top-level `val`.

`TileSource` compares by its id, template, zoom range, row order and attribution, so a source made again with the same values is the same source.

A real change of argument starts the layer again with no tiles: another source, client, `labels` or `rasterize`. So does a change of the style it resolves to, including the one `VectorStyleProvider` gives it, so switching from light to dark fetches every tile again through the client.

Before 0.0.7 every call made a new layer, and a basemap made inside a composable lost its tiles at every recomposition.

## Memory and caching

Each layer keeps up to 96 decoded tiles, evicting the one least recently on screen. The tiles go when the map leaves the composition, and are fetched again when it comes back. The module holds no other cache, so a cache that survives that, or a switch of style, belongs in the client. `wayfinder`'s `TileClient` KDoc shows one put in front of the HTTP client.

## What it does not draw

- **Dashed footpaths, and tunnels and bridges drawn apart from other roads**, are not in the module's two styles. [Light, Dark and Custom Styles](DOC-0042-light-dark-and-custom-styles.md) says what a style of your own can do.
- **Image tiles**, such as imagery or a hillshade, are `rasterBasemap` in [wayfinder-raster](../wayfinder-raster/README.md).
- **GeoJSON** is in [wayfinder-geojson](../wayfinder-geojson/README.md).
