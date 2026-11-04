# wayfinder-frame

Map frames for [wayfinder](../wayfinder). A frame here is meant in the cartographic sense, as in a print layout's map frame: a map shown from one camera position, at one size, deliberately composed. It is not an animation frame, and not a container around the interactive map; that is `WayfinderMap`.

A frame is drawn without a composition: to an image for a share sheet, a notification or an upload; to a `Painter` that scales like a vector image; or as a `MapFrame` in a layout. Images are encoded as PNG or WebP. On a server, [wayfinder-headless](../wayfinder-headless) wraps it in one call.

**Targets:** JVM · Android · iOS (arm64, simulator) · JS · wasmJs

## Install

```kotlin
implementation("com.oddlyclever:wayfinder-frame:0.0.7")
```

It depends on `wayfinder`, and on [`media-webp`](../media-webp) for encoding. Layers come from the module that draws them, `vectorFrameLayer` in wayfinder-vector or `rasterFrameLayer` in wayfinder-raster, so an app takes only the one it uses. There is no dependency-injection container.

## A map in a layout

```kotlin
MapFrame(
    layers = listOf(vectorFrameLayer(streets.zoomRange, names = rememberTextMeasurer()) { key -> streets.fetch(client, key) }),
    modifier = Modifier.fillMaxWidth().aspectRatio(3f / 2f),
    fit = route,                                   // or position = CameraPosition(center, zoom)
    contentDescription = "Map of the route",
    placeholder = { Shimmer() },
) { projection ->
    drawTrack(track, projection)                   // from com.oddlyclever.wayfinder.track
}
```

A map that does not move, for a card, a list row or a thumbnail. It renders at the size it is laid out and again when that changes, framed on `position`, or fitted to `fit` inside `padding`. `placeholder` fills the space until the tiles arrive. It is drawn as a `MapFramePainter`, so it stays sharp if the layout scales it.

## A map as an image

```kotlin
val size = IntSize(600, 400)
val density = Density(2f)
val image: ImageBitmap = renderMap(
    size = size,
    density = density,
    position = fit(route, size, density, padding = 24.dp),
    layers = listOf(vectorFrameLayer(streets.zoomRange, MapStyles.Light) { key -> streets.fetch(client, key) }),
    background = MapStyles.Light.background,
) { projection ->
    drawTrack(track, projection)
}
val webp: ByteArray = image.encodeToWebp()
```

- **The image matches the live map.** Tiles are laid out on the grid map-view would use for the position, turned to its bearing, magnified past each layer's last level, and drawn by the same renderers. A tile a layer lacks is drawn from its nearest ancestor, up to five levels up; where there is none, `background` shows.
- **Every tile is fetched at once**, so rendering takes as long as the slowest tile, not the sum of them.
- **The overlay is drawn upright** through a `MapFrameProjection`: `project(point)` places a coordinate on the image, `metresPerPixel` turns a distance into a radius, and `bearing` turns a heading onto the image. To draw over an image rendered earlier, build a `MapFrameProjection(position, size, density)` with the same values.
- **Names are drawn when a layer is given a `TextMeasurer`** (`vectorFrameLayer(…, names = measurer)`), after every layer's ground so no tile covers a name that runs onto it, and level at any bearing. Outside a composition, build one from a font family resolver: `TextMeasurer(createFontFamilyResolver(), density, LayoutDirection.Ltr)` where the platform has one without a context, as the JVM does.

## A map that scales like a vector image

```kotlin
val map = prepareMap(size, density, position, layers)          // fetches the tiles once
Image(MapFramePainter(map) { drawTrack(track, it) }, contentDescription = null, Modifier.size(1200.dp, 800.dp))
val thumbnail = map.toImageBitmap()                             // the same map as a bitmap
```

`prepareMap` fetches and decodes the tiles and returns a `PreparedMap` that draws at any size. As a `MapFramePainter`, its intrinsic size is the size it was prepared for, and drawn larger it scales as a whole, roads, outlines and overlay included, the way an `ImageVector` does. Raster tiles in it are bitmaps and blur when enlarged. `renderMap` is `prepareMap` followed by `toImageBitmap`.

## Encoding

| Call | What it gives |
|---|---|
| `image.encodeToPng()` | lossless, larger; shown everywhere |
| `image.encodeToWebp(quality = MAP_WEBP_QUALITY, metadata = null)` | lossy, several times smaller than PNG; 90 keeps thin roads sharp |

- **WebP is encoded by the [media-webp](../media-webp) library:** libwebp built in on Android and iOS, so the output does not depend on the device's own encoder, and Skia on the JVM and the web. A failure throws a `WebpException`; it never returns an empty file.
- **Metadata goes into the file.** Pass a `WebpMetadata` to record where the map is, when it was made and what made it, as EXIF:

```kotlin
val webp = image.encodeToWebp(metadata = WebpMetadata(location = GpsLocation(center.lat, center.lon), software = "Acme"))
```

- **PNG** is encoded by Skia on the JVM, iOS and the web, and by `Bitmap.compress` on Android, and carries no metadata.

## On a server

`renderMap` runs on the JVM with no display, drawing through Skia into memory. wayfinder-headless packages it for servers with Skia's native libraries included. Run directly, a server needs:

- **Java 21,** since map-view's classes are built for it.
- **Skia's native library for its platform** on the classpath: `org.jetbrains.skiko:skiko-awt-runtime-linux-x64` or `-linux-arm64`, at the Skiko version Compose brings.
- **The system libraries that library links against,** even though nothing is shown on a screen: `libGL.so.1`, `libX11.so.6` and `libEGL.so.1`. On Debian or Ubuntu they are the packages `libgl1`, `libx11-6` and `libegl1`. Without them the first render fails with a `LibraryLoadException` naming the missing library.

## For coding agents

The module ships an [Agent Skill](https://agentskills.io/specification), `com-oddlyclever-wayfinder-frame`, for the agents of projects that depend on it: what the library is for, how it's meant to be called, and the mistakes that compile. It is written at [src/commonMain/skills/com-oddlyclever-wayfinder-frame/SKILL.md](src/commonMain/skills/com-oddlyclever-wayfinder-frame/SKILL.md) and published inside every sources jar, at `commonMain/skills/com-oddlyclever-wayfinder-frame/`.
