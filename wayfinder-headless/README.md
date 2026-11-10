# wayfinder-headless

Map images on a server for [wayfinder](../wayfinder): coordinates in, WebP or PNG bytes out, in one call. For a map in an email, a share preview, or a thumbnail generated when a record is saved.

**Target:** JVM only. It exists to bundle Skia's native libraries for the platforms servers run on, which only the JVM loads; apps use [wayfinder-frame](../wayfinder-frame).

## Install

```kotlin
dependencies {
    implementation("com.oddlyclever:wayfinder-headless:0.0.8")
}
```

Skia's native library comes with it, for Linux and macOS on x64 and arm64. The server also needs Java 21, and on Linux the system libraries Skia links against and a font for the names; on Debian or Ubuntu, `libgl1`, `libx11-6`, `libegl1` and `fonts-dejavu-core`. [Running on a Server](../docs/knowledge/wayfinder-headless/DOC-0001-running-on-a-server.md) has the details, a container image, and what each failure looks like.

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

The camera fits the route and waypoints, the start, end and waypoints are marked, and the tiles may come from a vector or a raster tile server. For an image going into an email, pass `format = ImageFormat.Png`, since some desktop mail clients do not show WebP.

## Documentation

The full documentation is in the module's section of the [knowledge base](../docs/knowledge/wayfinder-headless/README.md), which is mirrored to the repository's wiki:

- [Running on a Server](../docs/knowledge/wayfinder-headless/DOC-0001-running-on-a-server.md): what the machine needs, a container image, and the failures.
- [Rendering Options](../docs/knowledge/wayfinder-headless/DOC-0002-rendering-options.md): every parameter, the camera, the markers, and what the call refuses.
- [Tile Sources](../docs/knowledge/wayfinder-headless/DOC-0003-tile-sources.md): what the tile function returns, and why a map comes out empty.
- [Skia on the JVM for Server-Side Map Images](../docs/knowledge/wayfinder-headless/ADR-0001-skia-on-the-jvm-for-server-side-map-images.md): why the module is built the way it is.

## For coding agents

The module ships an [Agent Skill](https://agentskills.io/specification), `com-oddlyclever-wayfinder-headless`, for the agents of projects that depend on it: what the library is for, how it's meant to be called, and the mistakes that compile. It is written at [src/main/skills/com-oddlyclever-wayfinder-headless/SKILL.md](src/main/skills/com-oddlyclever-wayfinder-headless/SKILL.md) and published inside the sources jar, at `skills/com-oddlyclever-wayfinder-headless/`.
