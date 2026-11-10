# Rendering a Map Image

DOC-0031 · 2026-11-04
Keywords: static map image, map snapshot, map screenshot, capture WayfinderMap to a bitmap, captureToImage, renderMap, prepareMap, PreparedMap, MapFramePainter, ImageBitmap, share image, notification picture, route thumbnail, density, retina image, bearing, rotated map, overlay in the wrong place, drawTrack on an image, MapFrameProjection, transparent image, black or checkerboard map, "A map image needs a size", "Nothing to fit", scale bar, compass, attribution on an image

`renderMap` draws a map at one camera position and one size into an `ImageBitmap`, with no composition of the live map. `prepareMap` does the same work in two steps, so one fetch of the tiles can be drawn more than once. This page covers both, how the camera and the overlay work, and what goes wrong.

## The call

```kotlin
val size = IntSize(1200, 800)
val density = Density(2f)
val image: ImageBitmap = renderMap(
    size = size,
    density = density,
    position = fit(route, size, density, padding = 24.dp),
    layers = listOf(vectorFrameLayer(0..14, MapStyles.Light) { key -> fetchTile(key) }),
    background = MapStyles.Light.background,
) { projection ->
    drawTrack(route.map { TrackPoint(it) }, projection)
}
```

It is a `suspend` function, so it runs from a coroutine: a view model, a worker, or a server's request handler. `fit` and `CameraPosition` are in `com.oddlyclever.wayfinder`, `drawTrack` and `TrackPoint` in `com.oddlyclever.wayfinder.track`, and `vectorFrameLayer` and `MapStyles` in `com.oddlyclever.wayfinder.vector`. Where the layers come from is in [Layers and Tiles](Wayfinder-Frame-Layers-And-Tiles).

## Size and density

`size` is the image in pixels. `density` sets how big the tiles, lines and names are drawn on it: a tile is 256 dp across, as on the live map, so `IntSize(1200, 800)` at `Density(2f)` shows the same ground as a 600 by 400 dp map, drawn twice as sharp. For an image that looks like the map on the device, pass the screen's density; on a server, pick the density the image will be viewed at.

## The camera

`position` is a `CameraPosition(center, zoom, bearing)`. `bearing` is what is at the top of the image, in degrees clockwise from north, as on the live map.

`fit(points, size, density, padding, bearing, maxZoom)` returns the camera that shows every point inside `padding`, as close in as they allow. A single point, or points close together, sit at `maxZoom`, street level (17) by default. Points either side of the antimeridian are fitted the short way round. `fit` throws `IllegalArgumentException` ("Nothing to fit: the list of points is empty.") for an empty list. When the padding leaves no room, it returns the farthest zoom rather than failing.

`fit` and `renderMap` must get the same `size` and `density`. A camera fitted for one size and drawn at another frames the wrong ground.

There is no snapshot of a `WayfinderMap`: a frame is always rendered on its own from a `CameraPosition`. To show the same place as a live map, give the frame the same position, which a live map can also move to.

## How the image is put together

1. The image is filled with `background`.
2. Each layer's tiles are drawn, in the order of `layers`. Each layer picks the tile level for the zoom, magnified past its last level, and the whole map is turned to the bearing. See [Layers and Tiles](Wayfinder-Frame-Layers-And-Tiles) for which tiles are asked for and what happens when one is missing.
3. Every layer's names are drawn, after all the ground, so no tile covers a name that runs onto it. Names stay level at any bearing.
4. `overlay` is drawn last, upright, through a `MapFrameProjection`, and clipped to the image.

The map draws no ornaments: no scale bar, compass or attribution. An image whose tiles' licence asks for a credit draws it in the overlay.

### The background

`background` is `Color.Transparent` by default. Where no tile was drawn, it shows. So when every tile fails, the result is a transparent image with only the overlay on it, which a viewer shows as black, white or a checkerboard depending on the app. Pass the style's background, such as `MapStyles.Light.background`, and the empty parts look like open ground.

## The overlay and the projection

The overlay is a `DrawScope.(MapFrameProjection) -> Unit`. The projection answers three questions:

- `project(point)`: where a `LatLon` falls on the image, in pixels from its top left, with the bearing taken into account.
- `metresPerPixel`: how much ground a pixel covers at the middle of the image, for drawing a distance as a radius.
- `bearing`: the bearing the image was drawn at, for turning a heading onto it.

`MapFrameProjection` implements wayfinder's `MapProjection`, the same interface a live map's overlay gives. So `drawTrack`, and anything else written against `MapProjection`, draws the same way on a live map and on an image.

To draw over an image rendered earlier, build `MapFrameProjection(position, size, density)` with the values it was rendered with.

## Prepare once, draw more than once

```kotlin
val map = prepareMap(size, density, position, layers, background)   // fetches and decodes the tiles once
val full = map.toImageBitmap { drawTrack(track, it) }
Image(MapFramePainter(map) { drawTrack(track, it) }, contentDescription = null, Modifier.size(1200.dp, 800.dp))
```

`prepareMap` fetches and decodes every tile and returns a `PreparedMap`, which holds its `size`, `density`, `position` and `projection`. It draws in three ways:

- **`toImageBitmap(overlay)`** draws it to a bitmap at its own size. `renderMap` is `prepareMap` followed by this.
- **`MapFramePainter(map, overlay)`** is a Compose `Painter` whose intrinsic size is the size the map was prepared for. Drawn larger, it scales as a whole, roads, outlines, names and overlay included, the way an `ImageVector` does, instead of blurring like an enlarged bitmap.
- **`draw(overlay)`** draws it into any `DrawScope`, called as `with(map) { draw(overlay) }`.

All three draw the overlay in the pixels of the prepared size and scale it with the map. `draw` stretches the map to fill the scope it is given, width and height separately, so a scope of another aspect ratio distorts it. Raster tiles in a prepared map are bitmaps, and they blur when it is drawn larger.

## What it refuses

| Call | Result |
|---|---|
| A `size` with a zero width or height | `IllegalArgumentException`: "A map image needs a size; got …" |
| `fit` with no points | `IllegalArgumentException`: "Nothing to fit: the list of points is empty." |
| A tile that fails to load or decode | Never an error. The cell is drawn from an ancestor or left to `background`. |

## Threads

Every tile of every layer is requested at once, so rendering takes as long as the slowest tile, not the sum of them. The vector and raster layers decode their tiles on `Dispatchers.Default`. The drawing itself, `toImageBitmap` and `renderMap`'s last step, is blocking CPU work on the dispatcher it is called from. On Android, call it from `Dispatchers.Default` rather than the main thread for anything larger than a thumbnail.

Each call builds its own `PreparedMap` and bitmap. A layer passed to several calls running at once is shared by them, and so are its load function and the `TextMeasurer` it was given. [wayfinder-headless](Wayfinder-Headless), which renders on a server, builds a fresh layer and text measurer for every call.

Platform requirements, such as Skia's native library on the JVM, are in [Platform Notes](Wayfinder-Frame-Platform-Notes).
