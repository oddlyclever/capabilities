# wayfinder-vector

Vector tiles for [wayfinder](../wayfinder): a vector basemap for `WayfinderMap`, light and dark styles for map-view's vector renderer, and vector tiles drawn as images on devices whose GPU driver cannot be trusted with their paths.

**Targets:** JVM · Android · iOS (arm64, simulator) · JS · wasmJs

## Install

```kotlin
implementation("com.oddlyclever:wayfinder-vector:0.0.8")
```

It exposes `wayfinder`, map-view-tiles-vector and map-view-tiles-raster as `api`. There is no dependency-injection container.

## Use

```kotlin
val streets = TileSource("streets", "https://tiles.example.com/{z}/{x}/{y}.pbf", zoomRange = 0..14, attribution = "© Example contributors")
val basemap = vectorBasemap(streets, client)   // client: the app's TileClient, made once

@Composable
fun MapScreen(state: WayfinderMapState) {
    VectorStyleProvider(systemVectorStyle()) {
        WayfinderMap(state, layers = listOf(basemap))
    }
}
```

`vectorBasemap(source, client, style = null, labels = true, rasterize = false)` fetches each tile through the client, decodes it off the main thread, and draws it with map-view's renderer, with the names of places, roads, water, rivers, peaks and points of interest over every tile. The tiles must use the OpenMapTiles schema. `systemVectorStyle()` is `MapStyles.Dark` in the system's dark mode and `MapStyles.Light` otherwise, and `VectorStyleProvider` gives it to every basemap inside.

Make the client, and any style of your own, once: a layer keeps its tiles across recompositions only while its arguments stay equal, and a client or a style built inline is new every time.

## Documentation

The full documentation is in the module's section of the [knowledge base](../docs/knowledge/wayfinder-vector/README.md), which is mirrored to the repository's wiki:

- [Vector Basemaps](../docs/knowledge/wayfinder-vector/DOC-0041-vector-basemaps.md): every parameter, the schema and bytes a tile must be, keeping tiles across recompositions, and why a map comes out blank.
- [Light, Dark and Custom Styles](../docs/knowledge/wayfinder-vector/DOC-0042-light-dark-and-custom-styles.md): the two styles, following dark mode, a palette of your own, and turning names off.
- [Drawing Tiles as Images](../docs/knowledge/wayfinder-vector/DOC-0043-drawing-tiles-as-images.md): `rasterize`, for a device whose GPU driver faults on complex paths.
- [Vector Tiles on a Map Frame](../docs/knowledge/wayfinder-vector/DOC-0044-vector-tiles-on-a-map-frame.md): `vectorFrameLayer` for a map drawn to an image, with names.
- [GPU Paths Everywhere, Images Only When the App Asks](../docs/knowledge/wayfinder-vector/ADR-0020-gpu-paths-everywhere-images-only-when-the-app-asks.md) and [A Dark Map as a Recolouring of map-view's Style](../docs/knowledge/wayfinder-vector/ADR-0021-a-dark-map-as-a-recolouring-of-map-views-style.md): why the module is built the way it is.

## For coding agents

The module ships an [Agent Skill](https://agentskills.io/specification), `com-oddlyclever-wayfinder-vector`, for the agents of projects that depend on it: what the library is for, how it's meant to be called, and the mistakes that compile. It is written at [src/commonMain/skills/com-oddlyclever-wayfinder-vector/SKILL.md](src/commonMain/skills/com-oddlyclever-wayfinder-vector/SKILL.md) and published inside every sources jar, at `commonMain/skills/com-oddlyclever-wayfinder-vector/`.
