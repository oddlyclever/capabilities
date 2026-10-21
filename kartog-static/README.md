# kartog-static

Maps drawn without a composition for [kartog](../kartog): to an image for a share sheet, a notification or an upload, to a `Painter` that scales like a vector image, or as a `StaticMapImage` in a layout. Encoded as PNG or WebP. On a server, [kartog-headless](../kartog-headless) wraps it in one call.

**Targets:** JVM · Android · iOS (arm64, simulator) · JS · wasmJs

## Install

```kotlin
implementation("com.oddlyclever:kartog-static:0.0.3")
```

It depends on `kartog` alone. Layers come from the module that draws them, `kartog-vector` or `kartog-raster`, so an app takes only the one it uses. There is no dependency-injection container.

The Kotlin package is `com.oddlyclever.kartog.staticmap`, not `.static`: `static` is a Java keyword, and Android generates Java classes in a library's package.

## Use

```kotlin
val size = IntSize(600, 400)
val density = Density(2f)
val image: ImageBitmap = renderMap(
    size = size,
    density = density,
    position = fit(track.map { it.position }, size, density, padding = 24.dp),
    layers = listOf(staticVectorLayer(streets.zoomRange, KartogStyles.Light) { key -> http.get(streets.url(key)) }),
) { projection ->
    drawTrack(track, projection)          // from kartog-track
}
val png: ByteArray = image.encodeToPng()
```

- **The image matches the live map.** Tiles are laid out on the grid map-view would use for the position, turned to its bearing, magnified past each layer's last level, and drawn by the same renderers. A tile a layer lacks is drawn from its nearest ancestor, up to five levels up; where there is none, `background` shows.
- **Every tile is fetched at once**, so rendering takes as long as the slowest tile, not the sum of them.
- **The overlay is drawn upright** through a `StaticProjection`, which places a coordinate where it falls on the image and gives the metres a pixel covers. Anything can be drawn there: map-view-geojson's `drawGeoJson` with an adapter, pins, text.
- **Tracks** are drawn with kartog-track's `drawTrack`, through the projection.
- **`StaticMapImage`** is a map in a layout that does not move: a card, a thumbnail. It renders at the size it is laid out, for a given position or fitted to points, and shows a placeholder until the tiles arrive.
- **`prepareMap`** fetches the tiles once and returns a `PreparedMap` that draws at any size. As a `StaticMapPainter` in an `Image`, it scales like a vector image: roads and outlines stay sharp. Raster tiles in it are bitmaps and blur when enlarged.
- **Labels are not drawn yet.** map-view draws place and road names only inside a composition; drawing them here waits on a change to map-view.
- **Encoding:** `encodeToPng()` everywhere, lossless, and `encodeToWebp(quality)` for a smaller image to send.

## On a server

`renderMap` runs on the JVM with no display, drawing through Skia into memory. It has been run on headless Linux, arm64 and x64, with three things in place:

- **Java 21,** since map-view's classes are built for it.
- **Skia's native library for the server's platform** on the classpath: `org.jetbrains.skiko:skiko-awt-runtime-linux-x64` or `-linux-arm64`, at the Skiko version Compose brings.
- **The system libraries that library links against,** even though nothing is shown on a screen: `libGL.so.1`, `libX11.so.6` and `libEGL.so.1`. On Debian or Ubuntu they are the packages `libgl1`, `libx11-6` and `libegl1`. Without them the first render fails with `LibraryLoadException: Failed to load library … libskiko-linux-….so`.
