# kartog-geojson

GeoJSON on a [kartog](../kartog) map, from text or from a URL, drawn by map-view-geojson.

**Targets:** JVM · Android · iOS (arm64, simulator) · JS · wasmJs

## Install

```kotlin
implementation("com.oddlyclever:kartog-geojson:0.0.3")
```

It exposes `kartog` and map-view-geojson as `api`. There is no dependency-injection container.

## Use

In a `KartogMap`'s overlay:

```kotlin
KartogMap(state, layers = listOf(basemap)) {
    GeoJsonLayer(coverage, style = GeoJsonStyle(fill = Color(0x330066FF)))
    RemoteGeoJsonLayer("https://data.example.com/trails.geojson", client, minZoom = 12f) { feature -> select(feature) }
}
```

- **Points, lines and polygons with holes**, styled per layer, drawn from `minZoom` in, and tappable: the nearest feature takes the tap, and a drag still pans the map.
- **Text is parsed once** and kept while it is unchanged. Text that is not GeoJSON draws nothing rather than failing, and so does a URL that cannot be fetched.
- **A remote layer fetches through the app's `TileClient`**, so it shares the app's HTTP client and cache.
- **GeoJSON from another library comes in as text**, so that library needs no dependency on kartog. Cell outlines from an H3 library are one example.
