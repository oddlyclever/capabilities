# Parsing and Loading GeoJSON

DOC-0037 · 2026-11-04
Keywords: parseGeoJsonOrNull, parseGeoJson, GeoFeatureCollection, GeoFeature, aughtone-types, stringProperty, JsonObject properties, ring not closed, linear ring four positions, GeoJSON layer shows nothing, layer missing, invalid GeoJSON, longitude latitude order, coordinates swapped, shapes in the wrong place, properties as Int, feature id number, JsonPrimitive id, bbox, crs, altitude, fetch GeoJSON from a URL, RemoteGeoJsonLayer not loading, network error, retry, cache GeoJSON, UTF-8, large GeoJSON file dropped frames, parse off the main thread, kotlinx.serialization GeoJSON, H3 cells to GeoJSON

GeoJSON reaches the map as text, either given to `GeoJsonLayer` or fetched by `RemoteGeoJsonLayer` through the app's `load`, or already parsed into aughtone-types' model. This page covers what the parser accepts, what the parsed model looks like, how a remote layer fetches, and why a layer can be missing without an error.

## What it reads

`parseGeoJsonOrNull(text)` parses a document into aughtone-types' `GeoFeatureCollection`, or returns `null` for text that is not GeoJSON. `GeoJsonLayer` parses the same way. The document may be:

- a `FeatureCollection`,
- a single `Feature`, which comes back as a collection of one,
- a bare geometry, which comes back as a collection of one feature with no properties.

Every geometry type is read: `Point`, `MultiPoint`, `LineString`, `MultiLineString`, `Polygon`, `MultiPolygon` and `GeometryCollection`. A feature whose `geometry` is `null` or missing is allowed and draws nothing. `bbox` is kept on the model; `crs` and other members the specification does not define are ignored. A byte order mark before the document is skipped, and anything after it is an error.

The parser holds GeoJSON to RFC 7946, as aughtone-types' model does. **A line needs at least two positions, and a polygon ring at least four, ending where it starts.** A file with a ring left open, which some tools write, is not GeoJSON here, and draws nothing.

### Positions are longitude first

GeoJSON writes a position as `[longitude, latitude]`, with an optional altitude, and the model keeps it in that order: a `GeoPoint`'s `coordinates` are `[longitude, latitude, altitude?]`, and it has `longitude`, `latitude` and `altitude` to read them by name. Altitude is kept, and ignored when drawing. Build a `GeoPoint(longitude = …, latitude = …)` with named arguments: both are `Double`, so swapping them compiles.

Coordinates written latitude first parse without complaint and draw in the wrong place.

## The parsed model

The model is aughtone-types', in `io.github.aughtone.types.geo`, and comes with this module: `GeoFeatureCollection(features)`, `GeoFeature(geometry, properties, id)`, and the `GeoGeometry` types `GeoPoint`, `GeoMultiPoint`, `GeoLineString`, `GeoMultiLineString`, `GeoPolygon`, `GeoMultiPolygon` and `GeometryCollection`. They are `@Serializable`, so an app can encode them back to GeoJSON with kotlinx.serialization.

- **`properties`** is a `JsonObject?`, the document's values as they were written. Read one with `stringProperty`, `intProperty`, `doubleProperty`, `booleanProperty`, `objectProperty` or `arrayProperty`, which return `null` for a key that is absent or of another kind: `feature.intProperty("count")` reads `3` as `3`. A feature with no `properties` has `null`.
- **`id`** is a `JsonPrimitive?`, since GeoJSON allows a string or a number. `id?.content` is its text, as written: `7` is `"7"` and `7.0` is `"7.0"`.

## Why a layer shows nothing

A layer draws nothing, and reports nothing, when:

- **The text is not GeoJSON.** Any parse failure is no layer at all, and one bad geometry anywhere, such as a position with no latitude or a ring that is not closed, fails the whole document.
- **`load` returns `null` or throws,** for a remote layer.
- **The map is zoomed out past `minZoom`.** See [Drawing GeoJSON on a Map](DOC-0036-drawing-geojson-on-a-map.md).

To see why a document does not parse, call `parseGeoJson(text)`, which throws `IllegalArgumentException` with kotlinx.serialization's or aughtone-types' reason. To see why a remote layer is missing, log inside `load`.

## Parsing on the main thread

`GeoJsonLayer` parses its text during composition, once for each distinct text, and keeps the result while the text is unchanged. For a small document that costs nothing noticeable. A large one costs a dropped frame or more when it first appears.

For a large document, parse it off the main thread and draw the result with the `GeoJsonLayer` that takes a `GeoFeatureCollection`:

```kotlin
val parsed by produceState<GeoFeatureCollection?>(null, text) {
    value = withContext(Dispatchers.Default) { parseGeoJsonOrNull(text) }
}
WayfinderMap(state, layers = listOf(basemap)) {
    parsed?.let { GeoJsonLayer(it, style = GeoJsonStyle()) }
}
```

`RemoteGeoJsonLayer` already parses off the main thread.

## Loading from a URL

```kotlin
RemoteGeoJsonLayer(
    url = "https://data.example.com/trails.geojson",
    load = { url -> http.get(url).body<ByteArray>() },
)
```

`load` is the app's: it is called with `url` and returns the document's bytes, or `null`. The module makes no request of its own, so the app's HTTP client, headers, keys and cache all apply.

- **The bytes are read as UTF-8,** then parsed on `Dispatchers.Default`.
- **Nothing is drawn until the document arrives.**
- **A failure is no layer.** When `load` returns `null` or throws, the layer draws nothing, so `load` needs no catch of its own. Cancellation is passed on, so leaving the composition stops a fetch in progress.
- **It fetches again only when `url` changes,** or when the layer leaves the composition and comes back. Changing `load` alone does not fetch again. When `url` changes, the old document stops being drawn at once, and the new one appears when it arrives.
- **It never retries.** A failed fetch stays failed until `url` changes. A retry, a timeout or a cache goes in `load`.

Since 0.0.7, a `load` that throws draws nothing. Before it, the exception escaped the layer's effect and crashed the app, so code written for 0.0.6 caught inside `load`; that still works.

## GeoJSON from another library

GeoJSON comes in as a `String`, so a library that produces it needs no dependency on wayfinder, and wayfinder none on it. The outlines of H3 cells from [h3spatial](../h3spatial/README.md) are one example: its GeoJSON text goes straight into `GeoJsonLayer`. A library that already speaks aughtone-types' model hands over its `GeoFeatureCollection` with no text in between.

The module does not write or edit GeoJSON; aughtone-types' model and kotlinx.serialization do that.
