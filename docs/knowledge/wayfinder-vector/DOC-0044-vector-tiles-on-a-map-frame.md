# Vector Tiles on a Map Frame

DOC-0044 · 2026-11-04
Keywords: vectorFrameLayer, renderMap, MapFrame, map as an image, static map, map thumbnail, map frame has no names, labels missing on rendered map, TextMeasurer, rememberTextMeasurer, dark frame beside dark map, frame style does not match live map, VectorStyleProvider ignored

`vectorFrameLayer` is the vector basemap for a map drawn without a composition: `renderMap` and `MapFrame` in [wayfinder-frame](../wayfinder-frame/README.md), and the server-side images of [wayfinder-headless](../wayfinder-headless/README.md). It is drawn by the same map-view renderer as `vectorBasemap`, so a frame matches the live map.

## The call

```kotlin
val names = rememberTextMeasurer()
val layer = vectorFrameLayer(streets.zoomRange, style = MapStyles.Dark, names = names) { key -> streets.fetch(client, key) }
```

| Parameter | Default | What it does |
|---|---|---|
| `zoomRange` | required | The levels the source has. Past its last level the renderer magnifies the deepest tile. |
| `style` | `MapStyles.Light` | The style to draw in. |
| `names` | `null` | A `TextMeasurer`, to draw names. Without one there are none. |
| `load` | required | Fetches a tile's bytes, or null. It is the last parameter, so it can be a trailing lambda. |

It takes a function rather than a `TileSource` and a client, so the bytes can come from anywhere. `streets.fetch(client, key)` is the same fetch a live basemap makes.

Unlike `vectorBasemap`, each call returns a new layer that is equal only to itself. A `MapFrame` given `listOf(vectorFrameLayer(…))` written inline in a composable sees new layers at every recomposition and draws its map again, fetching every tile. Make the layers once, in a `remember` or outside the composable.

## Tiles

Each tile's bytes are decoded on `Dispatchers.Default`. Null, or bytes that do not decode, leave the cell to its nearest ancestor, magnified, as on a live map. An empty byte array is a tile of open ground, drawn in the style's background colour.

The ground is drawn at the zoom the frame is drawn at, so roads have the widths the live map gives them at that zoom.

## Names

Give the layer a `TextMeasurer` as `names` and it draws the style's names: places, roads, water, rivers, peaks and points of interest, as the style turns them on. They are laid out by map-view exactly as on a live map, over every layer's ground and every level, at any bearing.

- **Without a `TextMeasurer` the frame has no names, and says nothing about it.** That is the default.
- **The app gives the measurer** because laying out text needs a context on Android. In a composable that is `rememberTextMeasurer()`; elsewhere, a `TextMeasurer` built from a font family resolver.
- **Names cost a second decode of each tile**, since the ground's drawing cannot carry them. They are laid out again whenever the frame is drawn.

## The style does not follow the provider

`vectorFrameLayer` is not composable, so it never reads `VectorStyleProvider`. Its `style` defaults to `MapStyles.Light` even when the live map beside it is dark. Pass the same style to both, such as the one `systemVectorStyle()` returns. Styles themselves are covered in [Light, Dark and Custom Styles](DOC-0042-light-dark-and-custom-styles.md).
