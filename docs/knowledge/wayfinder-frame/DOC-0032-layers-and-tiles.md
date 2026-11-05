# Layers and Tiles

DOC-0032 · 2026-11-04
Keywords: vectorFrameLayer, rasterFrameLayer, MapFrameLayer, MapFrameTile, vectorBasemap does not compile with renderMap, MapLayer is not a MapFrameLayer, TileSource, TileClient, no names on the map image, labels missing, TextMeasurer, createFontFamilyResolver, empty map, blank map, missing tiles, nearest ancestor, overzoom, blurry tiles, zoomRange, tile cache, custom layer, antimeridian

A map frame draws whatever its layers give it. This page covers the layers that come with wayfinder, how a frame chooses the tiles it asks them for, what happens when one is missing, and how to write a layer of your own.

## Where layers come from

A frame takes a `List<MapFrameLayer>`. The layers come from the module that decodes the tiles, so an app takes only the one it draws:

| Layer | Module | Package |
|---|---|---|
| `vectorFrameLayer(zoomRange, style, names, load)` | `wayfinder-vector` | `com.oddlyclever.wayfinder.vector` |
| `rasterFrameLayer(zoomRange, load)` | `wayfinder-raster` | `com.oddlyclever.wayfinder.raster` |

Both take a `load: suspend (TileKey) -> ByteArray?` that fetches a tile's bytes with the app's own HTTP client, and `zoomRange`, the levels the tile source has. With a wayfinder `TileSource` and `TileClient`, that is `vectorFrameLayer(streets.zoomRange) { key -> streets.fetch(client, key) }`.

The live map's layers do not fit. `vectorBasemap(…)` and `rasterBasemap(…)` return a `MapLayer`, for `WayfinderMap`; a frame takes a `MapFrameLayer`, which takes a load function rather than a `TileSource` and `TileClient`.

The vector layer draws with the same renderer as the live map, in `style`, `MapStyles.Light` by default. The raster layer decodes PNG and JPEG on every platform, and WebP wherever the platform's image decoder reads it. Both decode on `Dispatchers.Default`.

How the vector and raster layers fetch and decode tiles is documented with those modules: [wayfinder-vector](../wayfinder-vector/README.md) and [wayfinder-raster](../wayfinder-raster/README.md).

## Names

`vectorFrameLayer` draws names only when it is given a `TextMeasurer` as `names`. Without one, it draws roads and land with no labels, and says nothing. Which kinds of things are named, places, roads, water, peaks or points of interest, is the style's.

Laying text out needs a `TextMeasurer`, and on Android that takes a context, so the app supplies it:

- In a composable, `rememberTextMeasurer()`.
- Outside one, build it from a font family resolver: `TextMeasurer(createFontFamilyResolver(), density, LayoutDirection.Ltr)`, where the platform has a resolver that needs no context, as the JVM does.

Names cost a second decode of each tile, and they are laid out again every time the frame is drawn, so a `MapFramePainter` drawn many times lays them out each time.

## Which tiles are asked for

For each layer, the frame picks the tile level for the camera's zoom, rounded down and kept inside the layer's `zoomRange`. Past the layer's last level, its deepest tiles are magnified. Each layer picks its own level, so a raster layer that stops at 12 and a vector layer that goes to 14 can be drawn together.

It asks for enough tiles to cover the image's diagonal, so a map turned to any bearing has no bare corners. A north-up image therefore asks for a few more tiles than it shows. Columns wrap round the antimeridian, so a map centred on the Pacific is covered on both sides. Rows past the top or bottom of the world are not asked for.

Every tile of every layer is requested at once. Within a layer, a tile needed twice, such as an ancestor standing in for several cells, is fetched once.

## When a tile is missing

A tile that fails never fails the call. Each of these is a missing tile:

- `load` returns `null`.
- `load` throws.
- The bytes cannot be decoded: an error page, a truncated image, a vector tile the decoder cannot read.

A missing tile is drawn from its nearest ancestor, magnified, up to five levels up, as a live map does, but never from a level below the start of the layer's `zoomRange`. Where there is no ancestor either, `background` shows.

So a broken load function gives an empty map, not an exception. When a frame comes out empty, look at what `load` returns. A vector tile in a schema the style has no rules for decodes without error and draws nothing.

## No cache

A frame keeps no tiles between calls. `renderMap` and `prepareMap` each request every tile they need. Caching belongs in the load function, in front of the HTTP client, along with any rate limit, timeout or retry. To draw the same map twice without fetching twice, prepare it once with `prepareMap`; see [Rendering a Map Image](DOC-0031-rendering-a-map-image.md).

## A layer of your own

`MapFrameLayer` and `MapFrameTile` are in `com.oddlyclever.wayfinder`, so any module can supply a layer:

```kotlin
val shaded = object : MapFrameLayer {
    override val zoomRange = 0..19
    override suspend fun tile(key: TileKey): MapFrameTile? =
        if (key.z <= 12) MapFrameTile { _, _ -> drawRect(Color(0x22000000)) } else null
}
```

- `zoomRange` is the levels the layer has. The frame never asks for a tile outside it.
- `tile(key)` returns the tile decoded and ready to draw, or `null` when the layer has none, which leaves the cell to an ancestor.
- `MapFrameTile.draw(src, zoom)` draws the `src` fraction of the tile, a square inside `0..1`, to fill the whole scope it is given. For an ancestor standing in for a missing tile, `src` is the part of it that covers the cell. `zoom` is the map's zoom, rounded down, which is what line widths and zoom-gated paints are resolved against; it is not the level the tile was cut at.
- `MapFrameTile.drawLabels(src, zoom, bearing)` draws the tile's names, after every layer's ground. Names may run past the tile's square. `bearing` is the map's, so a name can turn back and stay level. It does nothing by default.
