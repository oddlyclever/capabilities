# Rendering Options

DOC-0002 · 2026-11-04
Keywords: renderMapImage parameters, image size, retina image, scale, route map, waypoints, start and end markers, dark map, map style, line colour, padding, zoom level, WebP or PNG, email image, quality, concurrency

`renderMapImage` is the module's one call. This page covers each of its parameters, how it chooses the camera and the markers, and what it refuses.

## The call

```kotlin
val webp: ByteArray = renderMapImage(
    width = 600, height = 400, scale = 2f,
    lines = route.pieces,                     // List<List<LatLon>>, each piece drawn on its own
    points = route.waypoints,                 // List<LatLon>
    tiles = { key -> fetchTile(key) },        // ByteArray? from the app's own HTTP client
)
```

It is a `suspend` function, so it is called from a coroutine, such as a Ktor route handler. `LatLon` and `TileKey` are map-view's, in `com.coderwise.libs.mapview`.

## Parameters

| Parameter | Default | What it does |
|---|---|---|
| `width`, `height` | required | The image's size before `scale`. |
| `scale` | `1f` | The image is `width × scale` by `height × scale` pixels, rounded, with lines and markers `scale` times as thick. |
| `lines` | none | The route or track, in pieces. |
| `points` | none | Waypoints beside the lines; with no lines, the places the map is about. |
| `tiles` | required | Fetches a tile's bytes, or returns null for a tile it cannot supply. See [Tile Sources](DOC-0003-tile-sources.md). |
| `format` | `ImageFormat.Webp` | `ImageFormat.Webp` or `ImageFormat.Png`. |
| `quality` | 90 | WebP quality, 0 to 100. PNG ignores it. |
| `style` | `MapStyles.Light` | The style vector tiles are drawn in. |
| `names` | `true` | Whether vector tiles' names are drawn: places, roads, water, rivers, peaks and points of interest. |
| `zoomRange` | `0..14` | The levels the tile source has. |
| `padding` | `48.dp` | The space kept between the coordinates and the image's edges. |
| `line` | violet over a dark casing | `HeadlessLineStyle(color, width, casing, casingWidth)`. |

`tiles` comes before `scale` and the other options, so it is not the last parameter and cannot be passed as a trailing lambda. `renderMapImage(600, 400, lines = l) { key -> … }` does not compile, because the lambda lands on `line`. Name it: `tiles = { key -> … }`.

## Size and scale

`width` and `height` are in density-independent units and `scale` is the density, as on a phone screen. `600, 400, scale = 2f` gives a 1200 by 800 image whose lines, markers and names are drawn twice as thick, so it looks the same as a 600 by 400 image on a screen with twice the pixels. For an exact pixel size, divide it by the scale.

`padding` and the widths in `HeadlessLineStyle` are scaled the same way.

## The camera

The camera frames every coordinate in `lines` and `points` inside `padding`, and goes no closer than the last level of `zoomRange`. A single point, or a short route, therefore shows the streets around it rather than filling the image with one block. The map is always north-up.

`zoomRange` is the levels the tile source has. Tiles are only requested inside it, so a raster source that goes to level 19 is never shown closer than 14 unless `zoomRange` says `0..19`.

## Lines and markers

Each piece of `lines` is drawn on its own, so a route recorded in pieces, with gaps where the signal dropped, never jumps straight across a gap. Each piece is a line in `line.color` over a wider casing in `line.casing`, which keeps it readable on pale land and on water. A piece with fewer than two points draws no line.

The markers are:

- **The start**, in green: the first point of the first line.
- **The end**, in red: the last point of the last line. On a loop, where the two are the same, they share one marker.
- **Waypoints**, in blue: every one of `points`. With no lines, `points` are marked as places instead.

The lines' own points are never marked.

## Style and names

`style` is a map-view `VectorStyle`. `MapStyles.Light` and `MapStyles.Dark`, in `com.oddlyclever.wayfinder.vector`, come with the module; a style of your own is a `VectorStyle` too. The style's background colour shows wherever no tile was drawn.

`style` only applies to vector tiles. A raster tile is drawn as it is, so a dark image from a raster source needs a dark raster source.

Names are drawn in the machine's default font, so a server that draws them needs one installed. `names = false` turns them off. See [Running on a Server](DOC-0001-running-on-a-server.md).

## WebP or PNG

WebP is the default and is several times smaller. Some desktop mail clients do not show WebP images, so an image going into an email should be `ImageFormat.Png`. PNG is lossless and ignores `quality`.

There is no JPEG, and the result is always encoded bytes: there is no bitmap to encode afterwards.

## What it refuses

| Call | Result |
|---|---|
| No lines and no points | `IllegalArgumentException`: "Nothing to draw: no lines and no points." |
| A size that rounds to zero pixels | `IllegalArgumentException`, giving the size and scale |
| WebP with `quality` outside 0 to 100 | `IllegalArgumentException` |
| WebP more than 16,383 pixels on a side | `IllegalArgumentException`; that is WebP's limit |
| A WebP the encoder cannot produce | `WebpException`, from `com.oddlyclever.media.webp` |

A tile that fails is never an error. See [Tile Sources](DOC-0003-tile-sources.md).

## Concurrency

Each call builds its own layer, text measurer and bitmap and shares no mutable state, so calls can run at the same time. Vector tiles are decoded on `Dispatchers.Default`, but the drawing and encoding are CPU work done on the dispatcher `renderMapImage` is called from. A server rendering many images at once may want to call it on a dispatcher of its own, sized for the work.

## Calling it from Java

It cannot be called from Java. `padding` is a Compose `Dp`, a value class, so the function's JVM name is mangled into one that is not a legal Java identifier. It is also a `suspend` function, and its defaults have no `@JvmOverloads`. Java code calls it through a Kotlin function of its own.
