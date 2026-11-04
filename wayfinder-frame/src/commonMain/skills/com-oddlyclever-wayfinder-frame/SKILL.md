---
name: com-oddlyclever-wayfinder-frame
description: >-
  Draw a map to an image without showing a live, interactive map: a static
  map, a map snapshot, a route thumbnail or preview for a card or list row, a
  share image, a notification picture or an upload, in Compose Multiplatform on
  Android, iOS, desktop JVM and the browser. Frames the camera on a route or a
  point, draws a recorded track, pins or shapes over the map, and encodes the
  result as PNG or lossy WebP with GPS location and capture time written into
  the file. Also a map composable that does not move and renders once at its
  laid-out size, and a painter that scales the whole map like a vector image.
  Use instead of capturing a map view off screen, calling a static-maps web
  API, or stitching tiles onto a canvas by hand. Tiles come from vector or
  raster frame layers; for map images in one call on a JVM server, see
  com.oddlyclever:wayfinder-headless.
license: Apache-2.0
metadata:
  version: "0.0.7"
  repository: https://github.com/oddlyclever/capabilities
---

# wayfinder-frame

`com.oddlyclever:wayfinder-frame`, package `com.oddlyclever.wayfinder.frame`. Called from Kotlin only: there is no Swift or JavaScript surface.

## What it solves

A picture of a map where a live map would be too much or impossible: the route of a finished run on a summary card, a thumbnail in a list, a map attached to a share sheet, a notification or an email, an image saved with a record. A frame is a map at one camera position and one size, drawn without a composition of the interactive map. It uses the same tile grid and the same renderers as the live map, so it looks like the map the user saw, and the overlay is drawn through a projection that places coordinates on the image. The result is an `ImageBitmap`, a `Painter`, or a `MapFrame` composable, and the bitmap encodes to PNG or WebP.

## How it is meant to be used

An image, from anywhere a suspend function can run. `size` is in pixels; `density` sets how big tiles, lines and names are drawn, so `IntSize(1200, 800)` at `Density(2f)` shows as much ground as a 600 by 400 dp map:

```kotlin
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.oddlyclever.wayfinder.fit
import com.oddlyclever.wayfinder.frame.encodeToWebp
import com.oddlyclever.wayfinder.frame.renderMap
import com.oddlyclever.wayfinder.track.TrackPoint
import com.oddlyclever.wayfinder.track.drawTrack
import com.oddlyclever.wayfinder.vector.MapStyles
import com.oddlyclever.wayfinder.vector.vectorFrameLayer

val size = IntSize(1200, 800)
val density = Density(2f)
val layer = vectorFrameLayer(0..14, MapStyles.Light) { key -> fetchTile(key) } // your client: ByteArray? per TileKey

val image = renderMap(size, density, fit(route, size, density, padding = 24.dp), listOf(layer), background = MapStyles.Light.background) { projection ->
    drawTrack(route.map { TrackPoint(it) }, projection)
}
val webp: ByteArray = image.encodeToWebp()
```

A map in a layout that does not move, for a card or a list row. It renders at the size it is laid out, again when that size changes, and shows `placeholder` until the tiles are in:

```kotlin
val names = rememberTextMeasurer()
val layers = remember(names) { listOf(vectorFrameLayer(0..14, names = names) { key -> fetchTile(key) }) }
MapFrame(
    layers = layers,
    modifier = Modifier.fillMaxWidth().aspectRatio(3f / 2f),
    fit = route,
    contentDescription = "Map of the route",
) { projection -> drawTrack(route.map { TrackPoint(it) }, projection) }
```

To fetch once and draw more than once, `prepareMap(size, density, position, layers, background)` returns a `PreparedMap`: `toImageBitmap(overlay)` draws it to a bitmap, and `MapFramePainter(map) { projection -> … }` puts it in an `Image`, where it scales as a whole, lines and overlay included. `renderMap` is `prepareMap` followed by `toImageBitmap`. To draw over an image made earlier, build `MapFrameProjection(position, size, density)` with the values it was rendered with.

The layers come from the module that decodes the tiles: `vectorFrameLayer` in `com.oddlyclever:wayfinder-vector` (package `com.oddlyclever.wayfinder.vector`) and `rasterFrameLayer` in `com.oddlyclever:wayfinder-raster` (package `com.oddlyclever.wayfinder.raster`). Any `MapFrameLayer` of your own works too.

## Invariants and traps

**A tile that fails is a missing tile, never an exception.** Whatever a layer throws, a `null`, or bytes the layer cannot decode leave the cell to its nearest ancestor, up to five levels up, and then to `background`. `background` is `Color.Transparent` by default, so when every fetch fails the result is a transparent image with only the overlay on it, which a viewer shows as black, white or a checkerboard. Pass the style's background, and when the map comes out empty, look at what the tile function returns rather than for an exception.

**`MapFrame` can wait for ever without an error.** It shows `placeholder` until it has a camera and a bounded size: with neither `position` nor a non-empty `fit`, or laid out with an unbounded or zero height, such as in a scrolling column with no height or aspect ratio, nothing renders. `position` wins over `fit` when both are given.

**Remember the layers you pass to `MapFrame`.** Rendering restarts whenever `layers` changes, and the layers `vectorFrameLayer` and `rasterFrameLayer` return are compared by identity, so `layers = listOf(vectorFrameLayer(…))` written inline builds new layers on every recomposition and fetches every tile again each time. Keep them in a `remember` or outside the composable.

**The projection belongs to one size and density.** `fit`, `renderMap` and `MapFrameProjection` must get the same `size` and `density`, or the camera and the overlay land in the wrong place. The overlay draws in the pixels of the size the map was prepared for, even when a `MapFramePainter` is drawn larger. `PreparedMap.draw` stretches to the scope it is drawn into, so a scope of another aspect ratio distorts the map; inside a `DrawScope` it is called as `with(map) { draw(overlay) }`.

**Drawing and encoding run on the calling thread.** `renderMap` and `prepareMap` fetch every tile at once, and the vector and raster layers decode on `Dispatchers.Default`, but the drawing runs on the caller's dispatcher. `toImageBitmap`, `encodeToPng` and `encodeToWebp` are blocking CPU work. On Android, call them from `Dispatchers.Default` rather than the main thread for anything bigger than a thumbnail.

**No names unless a layer gets a `TextMeasurer`.** `vectorFrameLayer` without `names` draws roads and land with no labels, silently. In a composable use `rememberTextMeasurer()`; on the JVM outside one, `TextMeasurer(createFontFamilyResolver(), density, LayoutDirection.Ltr)`.

**WebP here is lossy only.** `encodeToWebp(quality = MAP_WEBP_QUALITY, metadata = null)` takes `quality` 0 to 100, 90 by default, and throws `IllegalArgumentException` outside it or for an image more than 16,383 pixels a side; an encoder failure is `WebpException`, never an empty array. WebP is encoded by libwebp built into `com.oddlyclever:media-webp` on Android and iOS and by Skia on the JVM and the web. `encodeToPng()` uses `Bitmap.compress` on Android and Skia elsewhere, carries no metadata, and throws `IllegalStateException` when the platform refuses the image. There is no JPEG.

**An empty size or nothing to fit throws.** `renderMap` and `prepareMap` throw `IllegalArgumentException` ("A map image needs a size") for a zero width or height, and `fit` throws for an empty list of points.

**On the JVM, drawing needs Skia's native library for the machine at run time.** A Compose Desktop app already has it; a server process without it fails on the first render with Skiko's `LibraryLoadException`. On Linux, Skia also links against `libGL.so.1`, `libX11.so.6` and `libEGL.so.1`, even with no display. For map images on a server, `com.oddlyclever:wayfinder-headless` bundles Skia for Linux and macOS. Java 21 is required, as for the rest of wayfinder.

## What moved, and what it used to be called

`wayfinder-frame` was first released in 0.0.3 and its API has not changed since, so no published version had another shape.

- **Before 0.0.3 it was developed as `kartog-static` and never published.** A project that built it from source may have `com.oddlyclever.kartog.staticmap` imports, `StaticMapImage`, `StaticMapPainter` and `StaticProjection`. They are now `com.oddlyclever.wayfinder.frame`, `MapFrame`, `MapFramePainter` and `MapFrameProjection`.
- **A frame is not a capture of the live map.** There is no snapshot or `captureToImage` of a `WayfinderMap` and no frame built from a `WayfinderMapState`: a frame is rendered on its own from a `CameraPosition`, which `fit` returns and a live map can also move to.
- **Live-map layers do not fit.** `vectorBasemap(…)` and `rasterBasemap(…)` return `MapLayer`, for `WayfinderMap`; a frame takes `MapFrameLayer`, from `vectorFrameLayer` or `rasterFrameLayer`, which take a tile-loading function rather than a `TileSource` and `TileClient`.
- **"Frame" is the cartographic sense:** a composed map at one position and size. It has nothing to do with animation frames or with a container around `WayfinderMap`.

## What it is not for

It draws a map that does not move; panning, zooming, markers that respond to touch and the user's live position are the interactive map's, in `com.oddlyclever:wayfinder`. It does not fetch tiles or cache them between calls: the layer's load function is the app's, with its own HTTP client and cache. It encodes PNG and lossy WebP only, and draws no map ornaments such as a scale bar, compass or attribution; an image that needs attribution draws it in the overlay.
