# Parsing and Loading GeoJSON

DOC-0037 · 2026-11-04
Keywords: parseGeoJsonOrNull, parseGeoJson, "Not GeoJSON", GeoJSON layer shows nothing, layer missing, invalid GeoJSON, longitude latitude order, coordinates swapped, shapes in the wrong place, properties as Double, ClassCastException Int, feature id number, bbox, crs, altitude, fetch GeoJSON from a URL, RemoteGeoJsonLayer not loading, network error, retry, cache GeoJSON, UTF-8, large GeoJSON file dropped frames, parse off the main thread, kotlinx.serialization GeoJSON, H3 cells to GeoJSON

GeoJSON reaches the map as text, either given to `GeoJsonLayer` or fetched by `RemoteGeoJsonLayer` through the app's `load`. This page covers what the parser accepts, what the parsed model looks like, how a remote layer fetches, and why a layer can be missing without an error.

## What it reads

`parseGeoJsonOrNull(text)` parses a document and returns a `FeatureCollection`, or `null` for text that is not GeoJSON. `GeoJsonLayer` parses the same way. The document may be:

- a `FeatureCollection`,
- a single `Feature`, which comes back as a collection of one,
- a bare geometry, which comes back as a collection of one feature with no properties.

Every geometry type is read: `Point`, `MultiPoint`, `LineString`, `MultiLineString`, `Polygon`, `MultiPolygon` and `GeometryCollection`. A feature whose `geometry` is `null` is allowed, as the specification allows it, and draws nothing. `bbox`, `crs` and other members the model has no place for are ignored.

A byte order mark before the document is skipped. Anything after it is an error, and so is nesting deeper than 256 levels, so a hostile file of brackets is an error rather than a stack overflow.

### Positions are longitude first

GeoJSON writes a position as `[longitude, latitude]`, with an optional altitude, and the parser reads it that way. The altitude is ignored. In the parsed model a position is a map-view `LatLon(lat, lon)`, latitude first, as everywhere else on the map.

Coordinates written latitude first parse without complaint and draw in the wrong place.

## The parsed model

The model is map-view's, in `com.coderwise.libs.mapview.geojson`, and comes with this module: `FeatureCollection(features)`, `Feature(geometry, properties, id)`, and the `Geometry` types.

- **`properties`** is a `Map<String, Any?>` of plain values: `String`, `Double`, `Boolean`, `null`, `List` and `Map`. Every number is a `Double`, whole or not, so `properties["count"] as? Int` is `null`. Read it as `(properties["count"] as? Double)?.toInt()`. A feature with no `properties`, or one that is not an object, has an empty map.
- **`id`** is a `String?`. GeoJSON allows a string or a number. A whole number is written without a fraction, so `7` becomes `"7"`, not `"7.0"`; a number with a fraction keeps it. Any other kind of `id` is `null`.

## Why a layer shows nothing

A layer draws nothing, and reports nothing, when:

- **The text is not GeoJSON.** Any parse failure is no layer at all. One bad position anywhere, such as one with no latitude, fails the whole document.
- **`load` returns `null` or throws,** for a remote layer.
- **The map is zoomed out past `minZoom`.** See [Drawing GeoJSON on a Map](DOC-0036-drawing-geojson-on-a-map.md).

To see why a document does not parse, call map-view's `parseGeoJson(text)`, in `com.coderwise.libs.mapview.geojson`. It throws `IllegalArgumentException` with a message that starts "Not GeoJSON:", says what it found, and gives the offset in the text where it can. To see why a remote layer is missing, log inside `load`.

## Parsing on the main thread

`GeoJsonLayer` parses its text during composition, once for each distinct text, and keeps the result while the text is unchanged. For a small document that costs nothing noticeable. A large one costs a dropped frame or more when it first appears.

For a large document, parse it off the main thread and draw the result with map-view's `GeoJson`, which takes a parsed `FeatureCollection`:

```kotlin
val parsed by produceState<FeatureCollection?>(null, text) {
    value = withContext(Dispatchers.Default) { parseGeoJsonOrNull(text) }
}
WayfinderMap(state, layers = listOf(basemap)) {
    parsed?.let { GeoJson(it, style = GeoJsonStyle()) }
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

GeoJSON comes in as a `String`, so a library that produces it needs no dependency on wayfinder, and wayfinder none on it. The outlines of H3 cells from [h3spatial](../h3spatial/README.md) are one example: its GeoJSON text goes straight into `GeoJsonLayer`. A document built with a JSON library is encoded to text first.

The module does not write, edit or validate GeoJSON, and has no serializer for any JSON library's types.
