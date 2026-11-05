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
  shapes across the antimeridian. Also parses GeoJSON text into aughtone-types'
  GeoFeatureCollection on its own, for filtering or splitting a collection
  before drawing it, and draws a collection an app built in code.
license: Apache-2.0
metadata:
  version: "0.0.7"
  repository: https://github.com/oddlyclever/capabilities
---

# wayfinder-geojson

`com.oddlyclever:wayfinder-geojson`, package `com.oddlyclever.wayfinder.geojson`. Called from Kotlin only: there is no Swift or JavaScript surface. It brings `com.oddlyclever:wayfinder` with it, and aughtone-types, whose GeoJSON model it draws: `GeoFeatureCollection`, `GeoFeature`, the geometry types such as `GeoPoint` and `GeoPolygon`, and property readers such as `stringProperty`, all in `io.github.aughtone.types.geo`. `GeoJsonStyle` is this module's.

## What it solves

Shapes that arrive as data rather than as tiles: the outline of an area a service covers, a set of trails from a file, the hexagons of a spatial index, results a server returns as GeoJSON. They are drawn on a `WayfinderMap` by where they are, stay the same width on screen at every zoom, follow the map as it pans and pinches without recomposing, and report which feature the user tapped. GeoJSON from another library comes in as text, so that library needs no dependency on the map.

## How it is meant to be used

Both layers are drawn in a `WayfinderMap`'s trailing `overlay` lambda, which puts them over the map's polylines and under its markers. Layers are drawn in the order they are declared.

```kotlin
import com.oddlyclever.wayfinder.geojson.GeoJsonLayer
import com.oddlyclever.wayfinder.geojson.GeoJsonStyle
import com.oddlyclever.wayfinder.geojson.RemoteGeoJsonLayer
import io.github.aughtone.types.geo.stringProperty

WayfinderMap(state, layers = listOf(basemap)) {
    GeoJsonLayer(coverageText, style = GeoJsonStyle(fill = Color(0x330066FF), stroke = Color(0xFF0066FF)))
    RemoteGeoJsonLayer(
        url = "https://data.example.com/trails.geojson",
        load = { url -> httpGetBytes(url) }, // the app's HTTP client; null or a throw draws nothing
        minZoom = 12f,
        onFeatureClick = { feature -> select(feature.stringProperty("name")) },
    )
}
```

A style applies to every feature in its layer, so two looks are two layers. Parse once off the main thread with `parseGeoJsonOrNull`, split the features, and draw each part with the `GeoJsonLayer` that takes a `GeoFeatureCollection`:

```kotlin
import com.oddlyclever.wayfinder.geojson.parseGeoJsonOrNull
import io.github.aughtone.types.geo.GeoFeatureCollection
import io.github.aughtone.types.geo.stringProperty

val parsed by produceState<GeoFeatureCollection?>(null, text) {
    value = withContext(Dispatchers.Default) { parseGeoJsonOrNull(text) }
}
val (open, closed) = remember(parsed) {
    parsed?.features.orEmpty().partition { it.stringProperty("status") == "open" }
        .let { (a, b) -> GeoFeatureCollection(a) to GeoFeatureCollection(b) }
}
WayfinderMap(state, layers = listOf(basemap)) {
    GeoJsonLayer(open, style = GeoJsonStyle(stroke = Color(0xFF16A34A)))
    GeoJsonLayer(closed, style = GeoJsonStyle(stroke = Color(0xFFDC2626)))
}
```

`GeoJsonStyle(fill, stroke, strokeWidth, pointColor, pointRadius)` sets the polygons' inside, the outlines and lines, and the dots for points; a transparent colour or a width of 0 leaves that part out. `touchWidth`, 24 dp by default, is the width of the band around a line or point that a tap must land in, so a tap counts within half of it, 12 dp either side; a tap inside a polygon always counts.

## Invariants and traps

**A failed fetch is silent.** When `load` returns null or throws, `RemoteGeoJsonLayer` draws nothing and reports nothing, and it does not try again until `url` changes. Log inside `load` to see why a layer is missing.

**Text that is not GeoJSON draws nothing, silently.** `GeoJsonLayer` and `parseGeoJsonOrNull` turn any parse failure into no layer at all, and one bad geometry anywhere fails the whole document. The parser holds GeoJSON to RFC 7946: a line needs two positions, and a polygon ring four, ending where it starts, so a file with a ring left open draws nothing. To see why, call `parseGeoJson(text)`, which throws `IllegalArgumentException` with the reason.

**Positions are longitude first.** GeoJSON writes `[longitude, latitude]`, and aughtone-types' model keeps that order: a `GeoPoint`'s `coordinates` are `[longitude, latitude, altitude?]`, and `GeoPoint(longitude = …, latitude = …)` takes longitude first, so name the arguments. Coordinates written latitude first parse without complaint and draw in the wrong place.

**`GeoJsonLayer` parses on the main thread.** The text is parsed during composition, once for each distinct text, so a large document costs a dropped frame or more when it first appears. For a large one, parse off the main thread as above and draw the collection with the `GeoJsonLayer` that takes one. `RemoteGeoJsonLayer` already parses off the main thread.

**Use `minZoom`, not an `if`, to hide a layer.** A `RemoteGeoJsonLayer` is fetched again whenever its `url` changes or it leaves the composition and comes back, so one wrapped in `if (state.zoom > 12f)` refetches every time the user crosses that zoom. Below `minZoom` a layer is neither drawn nor tappable, and keeps what it fetched.

**The nearest feature takes the tap, not the top one.** Of the tappable features a tap reaches, in every layer, the nearest wins, and a tap that misses every feature goes on to the map underneath. A drag that starts on a feature still pans the map.

**Properties are a `JsonObject?`, not a map of Kotlin values.** Read them with `stringProperty`, `intProperty`, `doubleProperty`, `booleanProperty`, `objectProperty` or `arrayProperty`, which return null for a key that is absent or of another kind; `properties["name"]` is a `JsonElement`, and comparing it with a `String` is always false. A feature's `id` is a `JsonPrimitive?`, since GeoJSON allows a string or a number; `id?.content` is its text as written.

**The map's attribution does not cover GeoJSON.** The credits ornament shows only its tile layers' sources. Data that must be credited needs a credit the app draws itself.

## What moved, and what it used to be called

`wayfinder-geojson` was first released in 0.0.3, so no earlier versions exist.

- **After 0.0.7, the model is aughtone-types', not map-view's.** `FeatureCollection` is `GeoFeatureCollection`, `Feature` is `GeoFeature`, `Geometry.Polygon` is `GeoPolygon` and so on, in `io.github.aughtone.types.geo`; positions are `[longitude, latitude]` lists rather than `LatLon`. `GeoJsonStyle` moved from `com.coderwise.libs.mapview.geojson` to `com.oddlyclever.wayfinder.geojson`, and map-view's `GeoJson(data)` composable is `GeoJsonLayer(data)`. `onFeatureClick` gets a `GeoFeature`, whose properties are read with `stringProperty` and its siblings. `parseGeoJson` is this module's and throws the reason without the "Not GeoJSON:" prefix map-view's had. A ring left open, or a line of one position, no longer parses.

- **Since 0.0.7, a `load` that throws draws nothing.** Before it, the exception escaped `RemoteGeoJsonLayer`'s effect and crashed the app, so code written for 0.0.6 caught inside `load`; that still works.

- **It is not a map-attached layer object.** There is no `GeoJsonLayer(map, resourceId)` with `addLayerToMap()`, and no JSON object or resource id input: `GeoJsonLayer` is a composable in the map's overlay, and it takes a `String`.

## What it is not for

It draws and parses GeoJSON; it does not write, edit or validate it, style features one by one from their properties, or cluster points. Points that should group as the map zooms out are markers with clustering in `com.oddlyclever:wayfinder`. It does not fetch anything itself: `RemoteGeoJsonLayer` uses the app's `load`.
