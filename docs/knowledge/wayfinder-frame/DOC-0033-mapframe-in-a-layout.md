# MapFrame in a Layout

DOC-0033 · 2026-11-04
Keywords: MapFrame composable, static map in a card, map in a list row, LazyColumn map thumbnail, map that does not move, non-interactive map, map stuck on placeholder, map never shows, blank map in a scrolling column, fetches tiles on every recomposition, remember layers, aspectRatio, placeholder, shimmer, contentDescription, MapFramePainter, lite mode map

`MapFrame` is a map that does not move, for a card, a list row or a thumbnail, where a live `WayfinderMap` would be too much. It renders once at the size it is laid out, and again when that size changes. This page covers its parameters, when it renders, and the two ways it can wait for ever.

## The call

```kotlin
val names = rememberTextMeasurer()
val layers = remember(names) { listOf(vectorFrameLayer(0..14, names = names) { key -> fetchTile(key) }) }

MapFrame(
    layers = layers,
    modifier = Modifier.fillMaxWidth().aspectRatio(3f / 2f),
    fit = route,                                   // or position = CameraPosition(center, zoom)
    background = MapStyles.Light.background,
    contentDescription = "Map of the route",
    placeholder = { Shimmer() },
) { projection ->
    drawTrack(route.map { TrackPoint(it) }, projection)
}
```

| Parameter | Default | What it does |
|---|---|---|
| `layers` | required | The layers to draw, as for `renderMap`. See [Layers and Tiles](DOC-0032-layers-and-tiles.md). |
| `modifier` | `Modifier` | Must give the map a bounded, non-zero size. |
| `position` | `null` | The camera. Wins over `fit` when both are given. |
| `fit` | empty | Points to frame, used when `position` is null. |
| `padding` | `32.dp` | The space kept between the `fit` points and the edges. |
| `background` | `Color.Transparent` | What shows where no tile was drawn. |
| `contentDescription` | `null` | What a screen reader says for the map. |
| `placeholder` | nothing | Shown until the first map is ready. |
| `overlay` | nothing | Drawn over the map through its `MapFrameProjection`. |

The camera fitted to `fit` is north-up, at most street level. For another bearing, pass a `position` instead.

## When it renders

It renders when it has both a camera and a size. The size is the largest the layout allows, and the density is the composition's. It renders again whenever the size, `layers`, `position`, `fit`, `padding` or `background` changes. A changed `overlay` does not fetch anything again; the new one is drawn over the map it already has.

`placeholder` shows only until the first map is ready. When a later change starts a new render, the previous map stays on screen, stretched to the new size, until the new one replaces it.

The map is drawn as a `MapFramePainter`, filling the space, so if the layout scales it afterwards, its lines stay sharp. Raster tiles still blur when enlarged, as they are bitmaps.

## It can wait for ever

`MapFrame` shows `placeholder` until it has a camera and a bounded size, and it does not say why it is waiting:

- **No camera.** With `position` null and `fit` empty, nothing renders.
- **No bounded size.** Laid out with an unbounded or zero height, such as in a scrolling `Column` or a `LazyColumn` item with no height or aspect ratio, nothing renders. Give it a height, or `Modifier.aspectRatio`.

A map whose tiles all fail is not this: it renders, showing `background` and the overlay. See [Layers and Tiles](DOC-0032-layers-and-tiles.md).

## Remember the layers

Rendering restarts whenever `layers` changes, and the layers `vectorFrameLayer` and `rasterFrameLayer` return are compared by identity. So `layers = listOf(vectorFrameLayer(…))` written inline builds new layers on every recomposition, and every recomposition fetches every tile again. Keep the list in a `remember`, keyed on what it is built from, or outside the composable.

`fit` and `position` are compared by value, so a new list holding the same points does not restart it.

## Threads

The tiles are fetched in the composition's coroutine scope, and the vector and raster layers decode them on `Dispatchers.Default`. The map is drawn in the draw pass, like any painter, and a vector layer with names lays them out each time it is drawn.

## What it is not

It does not pan, zoom or take taps, and it shows no user position or ornaments. For a map that moves, use `WayfinderMap` in [wayfinder](../wayfinder/README.md). For a map as a file, use `renderMap`; see [Rendering a Map Image](DOC-0031-rendering-a-map-image.md).
