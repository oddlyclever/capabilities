---
name: com-oddlyclever-wayfinder-headless
description: >-
  Render a map image on a JVM server in one call: coordinates in, WebP or PNG
  bytes out, with no display, no UI framework and no headless browser. For a
  route or track map in an email, a link or share preview image, an Open Graph
  image, a thumbnail generated when a record is saved, or a static map endpoint
  in Ktor, Spring or any Kotlin backend, or in a container.
  Fits the camera to a route and waypoints, draws the route in pieces with
  start, end and waypoint markers, and fetches tiles through your own function
  from a vector (MVT, OpenMapTiles) or raster (PNG, JPEG, WebP) tile server,
  told apart automatically, in a light or dark style with place and road names.
  Bundles Skia's native libraries for Linux and macOS on x64 and arm64; not for
  Windows, and not for apps on a device. Use instead of screenshotting a map in
  a headless browser, calling a hosted static-maps API, or stitching tiles by
  hand. For maps drawn on a device or with an overlay of your own, see
  com.oddlyclever:wayfinder-frame.
license: Apache-2.0
metadata:
  version: "0.0.8"
  repository: https://github.com/oddlyclever/capabilities
---

# wayfinder-headless

`com.oddlyclever:wayfinder-headless`, package `com.oddlyclever.wayfinder.headless`. JVM only, and called from Kotlin only: Java cannot call `renderMapImage`, as the traps below explain.

## What it solves

A server that has to produce a picture of a map: the route of a trip in a confirmation email, a preview image for a shared link, a thumbnail stored beside a record when it is saved. `renderMapImage` takes the coordinates and a function that fetches tiles, frames the camera on them, draws the tiles, the route and its markers through Skia in memory, and returns the encoded image. Nothing is shown on a screen and no browser is involved, so it runs in an ordinary JVM process or a container.

## How it is meant to be used

One call, from a coroutine such as a Ktor route handler:

```kotlin
import com.oddlyclever.wayfinder.headless.ImageFormat
import com.oddlyclever.wayfinder.headless.renderMapImage

val png: ByteArray = renderMapImage(
    width = 600, height = 400, scale = 2f,                // a 1200 x 800 image, lines twice as thick
    lines = listOf(route),                                // List<List<LatLon>>, one list per piece
    points = waypoints,
    tiles = { key -> fetchTile("https://tiles.example.com/${key.z}/${key.x}/${key.y}.pbf") }, // ByteArray? from your client
    format = ImageFormat.Png,
)
```

`LatLon` and `TileKey` are map-view's, in `com.coderwise.libs.mapview`. The rest has defaults: WebP at quality 90, the light vector style, names on, `zoomRange = 0..14`, `maxZoom = 17f`, `padding = 48.dp`, and `line = HeadlessLineStyle(color, width, casing, casingWidth)`, a violet line over a dark casing.

The camera frames every coordinate inside the padding, no closer than `maxZoom`, street level (17) by default, so a short route fills the image and a single point shows the streets around it. The first point of the first line is marked as the start in green, the last point of the last line as the end in red, and `points` as waypoints in blue; on a loop the start and end share one marker, and with no lines the points are marked as places. Each piece of `lines` is drawn on its own, so a route in pieces never jumps straight across a gap.

## Invariants and traps

**`tiles` is not the last parameter.** `renderMapImage(600, 400, lines = l) { key -> … }` does not compile, because the trailing lambda lands on `line: HeadlessLineStyle`. Pass it as `tiles = { key -> … }`.

**`width` and `height` are multiplied by `scale`.** The image is `width × scale` by `height × scale` pixels, rounded, so `600, 400, scale = 2f` is 1200 by 800. Ask for the pixel size you want divided by the scale.

**A tile that fails is drawn from its ancestor, never thrown.** An exception from `tiles`, a `null`, or bytes that are neither an image nor a readable vector tile leave the cell to its nearest ancestor, up to five levels up, and then to the style's background colour. When the tile server is down, the call still succeeds and returns the route and markers on a plain background. Look at what `tiles` returns when the map comes out empty: an HTML error page, a gzip-compressed vector tile (the decoder does not decompress, so bytes from an MBTiles file or a client that does not undo `Content-Encoding: gzip` draw nothing), or tiles in a schema other than OpenMapTiles all look like this. An empty `ByteArray` is read as an empty vector tile, open ground, not as a missing one.

**Nothing is cached.** Every call fetches every tile it needs through `tiles`, all at once, so a busy endpoint sends a burst of requests to the tile server for each image. Caching belongs in the `tiles` function.

**The style comes from `MapStyles`.** `style = MapStyles.Dark` draws a dark map; `MapStyles` is in `com.oddlyclever.wayfinder.vector`, which comes with this module, along with map-view's `VectorStyle` for a style of your own.

**`zoomRange` is the levels your source has; `maxZoom` is how close the camera goes.** Tiles are requested only inside `zoomRange`, and past its last level the deepest tiles are magnified: vector tiles stay sharp, raster tiles soften. The default stops at 14, so a raster source that goes to 19 is magnified from 14 unless `zoomRange` says `0..19`.

**Skia's native library is bundled for Linux and macOS, on x64 and arm64, and nothing else.** On Windows the first render fails with Skiko's `LibraryLoadException`, saying the native dependency is missing. On any other operating system or architecture Skiko fails with an `Unknown OS` or `Unknown arch` error, which is a `java.lang.Error`, so a `catch (e: Exception)` does not catch it.

**The server needs more than the jar.** Java 21, or loading fails with `UnsupportedClassVersionError`. On Linux, the system libraries Skia links against even with no display, `libGL.so.1`, `libX11.so.6` and `libEGL.so.1` (the Debian and Ubuntu packages `libgl1`, `libx11-6` and `libegl1`), or the first render fails with a `LibraryLoadException` naming the missing one. A font, since names are drawn in the machine's default font; with `names = false` none is needed. And a writable directory to unpack Skia's library into on first use: `~/.skiko/`, or the directory the `skiko.data.path` system property names.

**What it refuses.** `IllegalArgumentException` for no lines and no points ("Nothing to draw: no lines and no points."), for a size that rounds to zero, and, with WebP, for `quality` outside 0 to 100 or an image more than 16,383 pixels a side. A WebP the encoder cannot produce is a `WebpException` from `com.oddlyclever.media.webp`. PNG ignores `quality`.

**Some desktop mail clients do not show WebP.** WebP is the default and several times smaller; pass `format = ImageFormat.Png` for an image going into an email.

**The drawing and encoding run on the calling dispatcher.** Vector tiles are decoded on `Dispatchers.Default`, but rendering and encoding are CPU work done where `renderMapImage` is called. Each call builds its own layer, text measurer and bitmap and shares no mutable state, so calls can run concurrently.

**Java cannot call `renderMapImage`.** It takes `padding` as a Compose `Dp`, a value class, so Kotlin compiles it under a mangled JVM name that is not a legal Java identifier; it is also a `suspend` function, and its defaults have no `@JvmOverloads`. `HeadlessLineStyle` is the same: from Java only its no-argument constructor is usable. Java code reaches it only through a Kotlin function that calls it.

## What moved, and what it used to be called

`wayfinder-headless` was first released in 0.0.3 and its API has only grown since, so code written for an earlier version still compiles.

- **Since 0.0.8, `maxZoom` is how close the camera fits, 17 by default.** Before it the camera stopped at the last level of `zoomRange`, 14 by default, so a route under about a kilometre sat small in the middle of the image. Code that widened `zoomRange` past the source's levels to get closer can narrow it back and leave the closeness to `maxZoom`.
- **Since 0.0.7, `style` can be passed.** In 0.0.3 to 0.0.6 the vector module was not exposed, so `MapStyles` was an unresolved reference and passing `style` failed with "Cannot access class 'com.coderwise.libs.mapview.tiles.vector.VectorStyle'" unless `com.oddlyclever:wayfinder-vector` was also a dependency.
- **There is no tile URL setting.** The library makes no HTTP requests: `tiles` is a function from a `TileKey` to bytes, written with your own client, keys and cache.
- **The result is encoded bytes, not a bitmap.** `renderMapImage` returns a `ByteArray` in `format`; there is no `ImageBitmap` or `BufferedImage` to encode afterwards. The formats are `ImageFormat.Webp` and `ImageFormat.Png`, and there is no JPEG.

## What it is not for

It is the one call for a server, with the line and markers drawn its way. A map image with an overlay of your own, several layers, a fixed camera rather than a fitted one, or drawing on a device is `renderMap` in `com.oddlyclever.wayfinder.frame`, which this module calls and exposes. It does not fetch, cache or serve anything: the tile requests and the HTTP response are the server's. It does not run on Windows, Android, iOS or in the browser.
