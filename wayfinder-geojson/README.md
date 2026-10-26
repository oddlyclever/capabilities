# wayfinder-geojson

GeoJSON on a [wayfinder](../wayfinder) map, from text or from a URL, drawn by map-view-geojson.

**Targets:** JVM · Android · iOS (arm64, simulator) · JS · wasmJs

## Install

```kotlin
implementation("com.oddlyclever:wayfinder-geojson:0.0.3")
```

It exposes `wayfinder` and map-view-geojson as `api`, so `FeatureCollection`, `Feature`, `Geometry` and `GeoJsonStyle` are available without declaring them. There is no dependency-injection container.

## Use

In a `WayfinderMap`'s overlay:

```kotlin
WayfinderMap(state, layers = listOf(basemap)) {
    GeoJsonLayer(coverageText, style = GeoJsonStyle(fill = Color(0x330066FF), stroke = Color(0xFF0066FF)))
    RemoteGeoJsonLayer(
        url = "https://data.example.com/trails.geojson",
        load = { url -> http.get(url).body<ByteArray>() },
        minZoom = 12f,
        onFeatureClick = { feature -> select(feature.id) },
    )
}
```

| Parameter | What it does |
|---|---|
| `style` | `GeoJsonStyle(fill, stroke, strokeWidth, pointColor, pointRadius)`: one style for the layer's polygons, lines and points |
| `minZoom` | below this zoom nothing is drawn, so a dense layer does not cover a zoomed-out map |
| `touchWidth` | how close a tap must land to a line or point to count, 24 dp by default |
| `onFeatureClick` | receives the feature tapped; the nearest feature takes the tap, and a drag still pans the map |

- **Every geometry type is drawn:** points, lines and polygons, multi-parts, polygons with holes, and geometry collections. Lines and polygon edges cross the antimeridian the short way.
- **Text is parsed once** and kept while it is unchanged. Text that is not GeoJSON draws nothing rather than failing; so does a URL whose `load` returns null.
- **A remote layer is fetched once per URL** with the app's own `load`, so it uses the app's HTTP client, keys and cache.
- **Different styles** are different layers: split a collection by property and draw each part with its own `GeoJsonLayer`.
- **`parseGeoJsonOrNull(text)`** parses on its own, for filtering or splitting before drawing.

GeoJSON from another library comes in as text, so that library needs no dependency on wayfinder. Cell outlines from an H3 library are one example.
