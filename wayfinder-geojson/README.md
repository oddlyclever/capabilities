# wayfinder-geojson

GeoJSON on a [wayfinder](../wayfinder) map, from text, from a URL, or already in [aughtone-types](https://github.com/aughtone/aughtone-types)' GeoJSON model.

**Targets:** JVM · Android · iOS (arm64, simulator) · JS · wasmJs

## Install

```kotlin
implementation("com.oddlyclever:wayfinder-geojson:0.0.8")
```

It exposes `wayfinder` and aughtone-types as `api`, so `GeoFeatureCollection`, `GeoFeature` and the geometry types, in `io.github.aughtone.types.geo`, are available without declaring them. There is no dependency-injection container.

## Use

In a `WayfinderMap`'s overlay:

```kotlin
WayfinderMap(state, layers = listOf(basemap)) {
    GeoJsonLayer(coverageText, style = GeoJsonStyle(fill = Color(0x330066FF), stroke = Color(0xFF0066FF)))
    RemoteGeoJsonLayer(
        url = "https://data.example.com/trails.geojson",
        load = { url -> http.get(url).body<ByteArray>() },
        minZoom = 12f,
        onFeatureClick = { feature -> select(feature.stringProperty("name")) },
    )
}
```

Points, lines and polygons are drawn, holes and multi-parts included, with one style per layer; below `minZoom` a layer is hidden, and `onFeatureClick` gets the `GeoFeature` tapped. A `GeoFeatureCollection` the app built or parsed itself goes to the `GeoJsonLayer` that takes one, with no text in between. Text that is not GeoJSON draws nothing rather than failing, and so does a `load` that returns null or throws, so a network failure costs the layer, never the app. GeoJSON from another library, such as [h3spatial](../h3spatial)'s cell outlines, comes in as text, so that library needs no dependency on wayfinder.

## Documentation

The full documentation is in the module's section of the [knowledge base](../docs/knowledge/wayfinder-geojson/README.md), which is mirrored to the repository's wiki:

- [Drawing GeoJSON on a Map](../docs/knowledge/wayfinder-geojson/DOC-0036-drawing-geojson-on-a-map.md): drawing order, styles, `minZoom`, taps and attribution.
- [Parsing and Loading GeoJSON](../docs/knowledge/wayfinder-geojson/DOC-0037-parsing-and-loading-geojson.md): what is parsed, properties and ids, why a layer shows nothing, and remote layers.
- [GeoJSON Comes In as Text and Is Fetched by the App](../docs/knowledge/wayfinder-geojson/ADR-0018-geojson-comes-in-as-text-and-is-fetched-by-the-app.md): why the module is shaped the way it is.
- [The GeoJSON Model Is aughtone-types'](../docs/knowledge/wayfinder-geojson/ADR-0022-the-geojson-model-is-aughtone-types.md): why the model is aughtone-types' and not map-view's.

## Third-party code

The drawing and hit-testing, in `GeoJsonOverlay.kt`, `Draw.kt`, `Shapes.kt` and `GeoJsonStyle.kt`, and their tests, are derived from map-view-geojson 0.3.0, part of [map-view](https://github.com/coderwise/libs-mobile), under the Apache License 2.0. They were moved into this module's package and draw aughtone-types' GeoJSON model in place of map-view-geojson's own, and map-view's `GeoJson` composable became the `GeoJsonLayer` that takes a `GeoFeatureCollection`; each file's header says so.

## For coding agents

The module ships an [Agent Skill](https://agentskills.io/specification), `com-oddlyclever-wayfinder-geojson`, for the agents of projects that depend on it: what the library is for, how it's meant to be called, and the mistakes that compile. It is written at [src/commonMain/skills/com-oddlyclever-wayfinder-geojson/SKILL.md](src/commonMain/skills/com-oddlyclever-wayfinder-geojson/SKILL.md) and published inside every sources jar, at `commonMain/skills/com-oddlyclever-wayfinder-geojson/`.
