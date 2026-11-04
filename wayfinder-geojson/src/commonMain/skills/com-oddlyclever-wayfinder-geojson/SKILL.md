---
name: com-oddlyclever-wayfinder-geojson
description: >-
  GeoJSON on a wayfinder map in Compose Multiplatform: draw the points, lines
  and polygons of a GeoJSON document, from text or fetched from a URL, over the
  map, with one fill, outline and point style per layer, hidden below a chosen
  zoom, and tappable features. Use instead of writing a GeoJSON parser or
  drawing shapes on a map by hand, to show a coverage area, delivery zone,
  geofence, service or administrative boundary, trails or routes from a file,
  grid cell outlines such as H3 hexagons, or search results from a server, and
  to get the feature the user tapped with its properties and id. Handles
  polygons with holes, multi-part geometries and geometry collections, and
  shapes across the antimeridian. Also parses GeoJSON text into features on its
  own, for filtering or splitting a collection before drawing it.
license: Apache-2.0
metadata:
  version: "0.0.7"
  repository: https://github.com/oddlyclever/capabilities
---

# wayfinder-geojson

`com.oddlyclever:wayfinder-geojson`, package `com.oddlyclever.wayfinder.geojson`. Called from Kotlin only: there is no Swift or JavaScript surface. It brings `com.oddlyclever:wayfinder` with it, and map-view's GeoJSON support, whose `FeatureCollection`, `Feature`, `Geometry`, `GeoJsonStyle` and `GeoJson` are in `com.coderwise.libs.mapview.geojson`.

## What it solves

Shapes that arrive as data rather than as tiles: the outline of an area a service covers, a set of trails from a file, the hexagons of a spatial index, results a server returns as GeoJSON. They are drawn on a `WayfinderMap` by where they are, stay the same width on screen at every zoom, follow the map as it pans and pinches without recomposing, and report which feature the user tapped. GeoJSON from another library comes in as text, so that library needs no dependency on the map.

## How it is meant to be used

Both layers are drawn in a `WayfinderMap`'s trailing `overlay` lambda, which puts them over the map's polylines and under its markers. Layers are drawn in the order they are declared.

```kotlin
import com.coderwise.libs.mapview.geojson.GeoJsonStyle
import com.oddlyclever.wayfinder.geojson.GeoJsonLayer
import com.oddlyclever.wayfinder.geojson.RemoteGeoJsonLayer

WayfinderMap(state, layers = listOf(basemap)) {
    GeoJsonLayer(coverageText, style = GeoJsonStyle(fill = Color(0x330066FF), stroke = Color(0xFF0066FF)))
    RemoteGeoJsonLayer(
        url = "https://data.example.com/trails.geojson",
        load = { url -> httpGetBytes(url) }, // the app's HTTP client; null or a throw draws nothing
        minZoom = 12f,
        onFeatureClick = { feature -> select(feature.id) },
    )
}
```

A style applies to every feature in its layer, so two looks are two layers. Parse once off the main thread with `parseGeoJsonOrNull`, split the features, and draw each part with map-view's `GeoJson`, which takes a parsed collection:

```kotlin
import com.coderwise.libs.mapview.geojson.FeatureCollection
import com.coderwise.libs.mapview.geojson.GeoJson
import com.oddlyclever.wayfinder.geojson.parseGeoJsonOrNull

val parsed by produceState<FeatureCollection?>(null, text) {
    value = withContext(Dispatchers.Default) { parseGeoJsonOrNull(text) }
}
val (open, closed) = remember(parsed) {
    parsed?.features.orEmpty().partition { it.properties["status"] == "open" }
        .let { (a, b) -> FeatureCollection(a) to FeatureCollection(b) }
}
WayfinderMap(state, layers = listOf(basemap)) {
    GeoJson(open, style = GeoJsonStyle(stroke = Color(0xFF16A34A)))
    GeoJson(closed, style = GeoJsonStyle(stroke = Color(0xFFDC2626)))
}
```

`GeoJsonStyle(fill, stroke, strokeWidth, pointColor, pointRadius)` sets the polygons' inside, the outlines and lines, and the dots for points; a transparent colour or a width of 0 leaves that part out. `touchWidth`, 24 dp by default, is how close a tap must land to a line or point to count; a tap inside a polygon always counts.

## Invariants and traps

**A failed fetch is silent.** When `load` returns null or throws, `RemoteGeoJsonLayer` draws nothing and reports nothing, and it does not try again until `url` changes. Log inside `load` to see why a layer is missing.

**Text that is not GeoJSON draws nothing, silently.** `GeoJsonLayer` and `parseGeoJsonOrNull` turn any parse failure into no layer at all, and one bad position anywhere, such as a missing latitude, fails the whole document. To see why, call map-view's `parseGeoJson(text)` in `com.coderwise.libs.mapview.geojson`, which throws `IllegalArgumentException` with a "Not GeoJSON: …" message saying what it found.

**Positions are longitude first.** GeoJSON writes `[longitude, latitude]`, and the parser reads it so; coordinates written latitude first parse without complaint and draw in the wrong place. A `LatLon` from the parsed geometry is `LatLon(lat, lon)` as usual.

**`GeoJsonLayer` parses on the main thread.** The text is parsed during composition, once for each distinct text, so a large document costs a dropped frame or more when it first appears. For a large one, parse off the main thread as above and draw the collection with `GeoJson`. `RemoteGeoJsonLayer` already parses off the main thread.

**Use `minZoom`, not an `if`, to hide a layer.** A `RemoteGeoJsonLayer` is fetched again whenever its `url` changes or it leaves the composition and comes back, so one wrapped in `if (state.zoom > 12f)` refetches every time the user crosses that zoom. Below `minZoom` a layer is neither drawn nor tappable, and keeps what it fetched.

**The nearest feature takes the tap, not the top one.** Of the tappable features within `touchWidth`, in every layer, the nearest wins, and a tap that misses every feature goes on to the map underneath. A drag that starts on a feature still pans the map.

**Properties come back as JSON types.** Every number in `properties` is a `Double`, whole or not, so `properties["count"] as? Int` is null; read it as `(properties["count"] as? Double)?.toInt()`. A feature's `id` is a `String?`, a numeric id written without a fraction: `"7"`, not `7` or `"7.0"`.

**The map's attribution does not cover GeoJSON.** The credits ornament shows only its tile layers' sources. Data that must be credited needs a credit the app draws itself.

## What moved, and what it used to be called

`wayfinder-geojson` was first released in 0.0.3, so no earlier versions exist, and its API has not changed since.

- **Since 0.0.7, a `load` that throws draws nothing.** Before it, the exception escaped `RemoteGeoJsonLayer`'s effect and crashed the app, so code written for 0.0.6 caught inside `load`; that still works.

- **It is not a map-attached layer object.** There is no `GeoJsonLayer(map, resourceId)` with `addLayerToMap()`, and no JSON object or resource id input: `GeoJsonLayer` is a composable in the map's overlay, and it takes a `String`.
- **It was never published under `kartog`.** A project that built from source before 0.0.3 may have `com.oddlyclever.kartog.geojson` imports; the same functions are now in `com.oddlyclever.wayfinder.geojson`.

## What it is not for

It draws and parses GeoJSON; it does not write, edit or validate it, style features one by one from their properties, or cluster points. Points that should group as the map zooms out are markers with clustering in `com.oddlyclever:wayfinder`. It does not fetch anything itself: `RemoteGeoJsonLayer` uses the app's `load`.
