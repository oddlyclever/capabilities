# wayfinder-headless

Map images on a server for [wayfinder](../wayfinder): coordinates in, WebP or PNG bytes out, in one call. For a map in an email, a share preview, or a thumbnail generated when a record is saved.

**Target:** JVM only. It exists to bundle Skia's native libraries for the platforms servers run on, which only the JVM loads; apps use [wayfinder-frame](../wayfinder-frame).

## Install

```kotlin
dependencies {
    implementation("com.oddlyclever:wayfinder-headless:0.0.7")
}
```

Skia's native library comes with it, for Linux and macOS on x64 and arm64, and is unpacked and loaded on first use. The server needs two more things:

- **Java 21,** since map-view's classes are built for it.
- **The system libraries Skia links against,** even with no display: `libGL.so.1`, `libX11.so.6` and `libEGL.so.1`. Without them the first render fails with a `LibraryLoadException` that names the missing library.
- **A font,** for the place and road names, which are drawn in the machine's default font. `fonts-dejavu-core` is one; with `names = false` none is needed.

In a Debian or Ubuntu based image:

```dockerfile
FROM eclipse-temurin:21-jre
RUN apt-get update && apt-get install -y --no-install-recommends libgl1 libx11-6 libegl1 fonts-dejavu-core && rm -rf /var/lib/apt/lists/*
```

## Use

```kotlin
val webp: ByteArray = renderMapImage(
    width = 600, height = 400, scale = 2f,
    lines = route.pieces,                                   // List<List<LatLon>>, each piece drawn on its own
    points = route.waypoints,
    tiles = { key -> http.get(tileUrl(template, key)).body<ByteArray>() },
)

// In a Ktor route:
call.respondBytes(webp, ContentType("image", "webp"))
```

| Parameter | Default | What it does |
|---|---|---|
| `width`, `height`, `scale` | `scale` 1 | the image is `width × scale` by `height × scale` pixels, with lines and markers `scale` times as thick |
| `lines` | none | the route or track, in pieces; each piece is drawn on its own, so the image never joins pieces with a straight line |
| `points` | none | waypoints beside the lines; with no lines, the places the map is about |
| `tiles` | required | fetches a tile's bytes, or returns null for a tile it cannot supply |
| `format`, `quality` | `ImageFormat.Webp`, 90 | WebP is several times smaller than PNG; `ImageFormat.Png` is lossless |
| `style` | `MapStyles.Light` | the vector style; `MapStyles.Dark` for a dark image |
| `names` | on | draws the style's names on vector tiles: places, roads, water, rivers, peaks and points of interest |
| `zoomRange` | `0..14` | the levels the tile source has; the camera goes no closer than its last |
| `padding` | 48 dp | the space kept between the coordinates and the edges |
| `line` | violet over a dark casing | `HeadlessLineStyle(color, width, casing, casingWidth)` |

- **The camera frames every coordinate** inside the padding, no closer than the last level of `zoomRange`, so a single point shows the streets around it.
- **Tiles may be images or vector tiles.** PNG, JPEG and WebP are told apart from vector tiles by their bytes, so one tile server of either kind works with no setting. Vector tiles are drawn in `style`. A tile `tiles` cannot supply is drawn from its nearest ancestor.
- **Markers:** the first point of the first line is the start, in green, and the last point of the last line the end, in red; on a loop they show as one marker. `points` are waypoints, in blue. The lines' own points are never marked.
- **WebP or PNG.** Some desktop mail clients do not show WebP, so ask for `ImageFormat.Png` where the image goes into an email.
- **It refuses** an image with no coordinates or an empty size, with an `IllegalArgumentException`. A WebP the encoder cannot produce is a `WebpException`, from the [media-webp](../media-webp) library that encodes it.
- **Rendering is all in memory** and safe to run concurrently; tiles for one image are fetched at once.

## For coding agents

The module ships an [Agent Skill](https://agentskills.io/specification), `com-oddlyclever-wayfinder-headless`, for the agents of projects that depend on it: what the library is for, how it's meant to be called, and the mistakes that compile. It is written at [src/main/skills/com-oddlyclever-wayfinder-headless/SKILL.md](src/main/skills/com-oddlyclever-wayfinder-headless/SKILL.md) and published inside the sources jar, at `skills/com-oddlyclever-wayfinder-headless/`.
