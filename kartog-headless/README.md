# kartog-headless

Map images on a server for [kartog](../kartog): coordinates in, WebP or PNG bytes out, in one call.

**Target:** JVM only. It exists to bundle Skia's native libraries for the platforms servers run on, which only the JVM loads; apps use [kartog-static](../kartog-static).

## Install

```kotlin
implementation("com.oddlyclever:kartog-headless:0.0.3")
```

Skia's native library comes with it, for Linux and macOS on x64 and arm64. The server needs two more things:

- **Java 21,** since map-view's classes are built for it.
- **The system libraries Skia links against,** even with no display: `libGL.so.1`, `libX11.so.6` and `libEGL.so.1`. On Debian or Ubuntu, install `libgl1`, `libx11-6` and `libegl1`. Without them the first render fails with `LibraryLoadException`.

## Use

```kotlin
val webp: ByteArray = renderMapImage(
    width = 600, height = 400, scale = 2f,
    lines = route.pieces,                 // each piece drawn on its own
    points = route.waypoints,
    tiles = { key -> http.get(tileUrl(template, key)) },
)
```

- **The camera** frames every coordinate inside the padding, no closer than the last level of `zoomRange` (14 by default).
- **Tiles** may be images (PNG, JPEG, WebP) or vector tiles, told apart by their bytes. Vector tiles are drawn in `style`, the light style by default.
- **Lines** are drawn over a darker casing, so they read on land and water. The first point of the first line is marked as the start and the last point of the last as the end, and `points` as waypoints; the lines' own points are never marked.
- **Encoding** is WebP by default, several times smaller than PNG. Some desktop mail clients do not show WebP, so ask for `ImageFormat.Png` where the image goes into an email.
- **`scale`** multiplies the size and the line widths together: 600 by 400 at scale 2 is a 1200 by 800 image.
