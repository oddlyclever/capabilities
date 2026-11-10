# wayfinder-raster

Raster tiles for [wayfinder](../wayfinder): a raster basemap for `WayfinderMap`, tile bytes decoded to images off the main thread, and tiles that fade in as they arrive.

**Targets:** JVM · Android · iOS (arm64, simulator) · JS · wasmJs

## Install

```kotlin
implementation("com.oddlyclever:wayfinder-raster:0.0.8")
```

It exposes `wayfinder` and map-view-tiles-raster as `api`. There is no dependency-injection container.

## Use

```kotlin
val imagery = TileSource(id = "imagery", urlTemplate = "https://tiles.example.com/imagery/{z}/{x}/{y}.jpg", zoomRange = 0..18, attribution = "© Example imagery")

WayfinderMap(state, layers = listOf(rasterBasemap(imagery, client)))   // client: the app's TileClient, made once
```

`rasterBasemap(source, client, alpha = 1f, fadeMillis = 300)` fetches each tile through the client, decodes it off the main thread, and fades it in. Layers stack, so a translucent raster layer over another basemap is `rasterBasemap(hillshade, client, alpha = 0.5f)` after it in the list. PNG and JPEG decode on every target, and WebP wherever the platform's decoder reads it. A tile that is missing or does not decode is drawn from its nearest ancestor, never an error.

Make the client once: a layer keeps its tiles across recompositions only while its arguments stay equal, and a client built inline is new every time.

## Documentation

The full documentation is in the module's section of the [knowledge base](../docs/knowledge/wayfinder-raster/README.md), which is mirrored to the repository's wiki:

- [Raster Basemaps](../docs/knowledge/wayfinder-raster/DOC-0038-raster-basemaps.md): every parameter, stacking, the fade, keeping tiles across recompositions, and memory.
- [Missing and Broken Tiles](../docs/knowledge/wayfinder-raster/DOC-0039-missing-and-broken-tiles.md): what counts as a missing tile, and why a map comes out blank or blurry.
- [Raster Tiles Outside a WayfinderMap](../docs/knowledge/wayfinder-raster/DOC-0040-raster-tiles-outside-a-wayfindermap.md): `decodeRasterTile`, `FadingRasterSlot`, and `rasterFrameLayer` for a map drawn to an image.
- [Decode Raster Tiles Upstream and Treat Failure as a Missing Tile](../docs/knowledge/wayfinder-raster/ADR-0019-decode-raster-tiles-upstream-and-treat-failure-as-a-missing-tile.md): why the module is built the way it is.

## For coding agents

The module ships an [Agent Skill](https://agentskills.io/specification), `com-oddlyclever-wayfinder-raster`, for the agents of projects that depend on it: what the library is for, how it's meant to be called, and the mistakes that compile. It is written at [src/commonMain/skills/com-oddlyclever-wayfinder-raster/SKILL.md](src/commonMain/skills/com-oddlyclever-wayfinder-raster/SKILL.md) and published inside every sources jar, at `commonMain/skills/com-oddlyclever-wayfinder-raster/`.
