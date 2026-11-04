---
name: com-oddlyclever-wayfinder
description: >-
  Show an interactive map in a Compose Multiplatform app on Android, iOS,
  desktop and web: the WayfinderMap composable and its state, the base the other
  wayfinder modules plug basemap tiles into. Use instead of wrapping Google Maps
  or MapLibre, or writing a slippy map by hand, for pins and markers placed by
  latitude and longitude, marker clustering, polylines and routes, the user's
  current location with an accuracy circle and heading, a recorded GPS track
  coloured by speed, and the controls around a map: compass, north-up or
  course-up rotation, scale bar, zoom buttons and map attribution. Also covers pan, pinch-zoom and rotate gestures, moving or
  flying the camera, fitting the camera to a set of points, holding it to a
  zoom range or a bounding box, reading the visible bounds for a search-this-area
  query, taps on the map and on markers, and tile URL templates (XYZ or TMS)
  fetched through your own HTTP client and cache. Drawing a map to an image
  without a composition is com.oddlyclever:wayfinder-frame.
license: Apache-2.0
metadata:
  version: "0.0.7"
  repository: https://github.com/oddlyclever/capabilities
---

# wayfinder

`com.oddlyclever:wayfinder`, package `com.oddlyclever.wayfinder`, with tracks and the user's position in `com.oddlyclever.wayfinder.track`. Called from Kotlin only: there is no Swift or JavaScript surface. It is built on map-view (`com.coderwise.libs:map-view`), and map-view's types are part of its API and come with it: `LatLon(lat, lon)`, `TileKey`, `MapCameraState` and `MapOverlayScope` are in `com.coderwise.libs.mapview`.

## What it solves

A screen that needs a real map: places shown as pins the user can tap, a route or a recorded run drawn over streets, a dot for where the user is that turns with their compass, a map that follows them while they walk without snapping back while they pan, and a "search this area" query driven by what is on screen. `WayfinderMap` is that whole map, with gestures, the camera and the ornaments a map screen needs, on every Compose target from common code. It fetches nothing itself: tiles come through a `TileClient` the app writes with its own HTTP client, keys and cache, and the basemap layers that draw them come from the vector and raster modules beside this one.

## How it is meant to be used

Describe the source, give the app's fetching, make the layer once, and pass everything to the map as data. Markers, lines and layers are parameters, not child composables.

```kotlin
import com.coderwise.libs.mapview.LatLon
import com.oddlyclever.wayfinder.*
import com.oddlyclever.wayfinder.vector.vectorBasemap // from com.oddlyclever:wayfinder-vector

val streets = TileSource(
    id = "streets",
    urlTemplate = "https://tiles.example.com/{z}/{x}/{y}.pbf?key=$apiKey",
    zoomRange = 0..14,
    attribution = "© Example contributors",
)
// The app's HTTP client. Null for any failure, never an error page's body.
val client = TileClient { source, key -> httpGetBytesOrNull(source.url(key)) }
val basemap = vectorBasemap(streets, client) // once, outside composition

@Composable
fun PlacesMap(places: List<Place>, onOpen: (String) -> Unit) {
    val state = rememberWayfinderMapState(center = LatLon(43.65, -79.38), zoom = 13f)
    WayfinderMap(
        state = state,
        modifier = Modifier.fillMaxSize(),
        layers = listOf(basemap),
        markers = places.map { MapMarker(it.id, it.position, Alignment.BottomCenter, description = it.name) },
        clustering = MarkerClustering(description = { "${it.size} places" }),
        ornaments = MapOrnaments(zoomButtons = true),
        onMarkerClick = { onOpen(it.id) },
    )
}
```

Drive the camera through the state, from a composition's coroutine. Follow the user only while they are not holding the map, and fit a route once the map has a size:

```kotlin
LaunchedEffect(here) { if (!state.isInteracting) state.animateTo(here) }

var size by remember { mutableStateOf(IntSize.Zero) }
val density = LocalDensity.current
WayfinderMap(state, Modifier.fillMaxSize().onSizeChanged { size = it }, layers = listOf(basemap))
LaunchedEffect(route, size) {
    if (route.isNotEmpty() && size != IntSize.Zero) state.animateTo(fit(route, size, density, padding = 32.dp))
}

LaunchedEffect(state) {
    snapshotFlow { state.visibleBounds }.filterNotNull().collect { bounds -> search(bounds) }
}
```

Anything else placed by coordinate goes in the trailing `overlay` lambda, drawn over the polylines and under the markers. A track and the user's position are drawn there:

```kotlin
import com.oddlyclever.wayfinder.track.LocationMarker
import com.oddlyclever.wayfinder.track.Track
import com.oddlyclever.wayfinder.track.TrackStyle

WayfinderMap(state, layers = listOf(basemap)) {
    Track(points, state, TrackStyle(markEnds = false)) // points: List<TrackPoint>, growing while recording
    LocationMarker(here, state, heading = compassHeading, accuracy = fixAccuracyMetres)
}
```

For course-up, set `state.orientation = Orientation.CourseUp` and keep `state.heading` current from the same compass. `MapCompassTape`, `OrientationToggle` and `OrientationIcon` place the compass tape and the north-up/course-up button in the screen's own layout, such as a top bar, with the matching `MapOrnaments` flag left off. `clusterPoints(items, zoom) { it.position }` is the same grouping for things that are not markers, such as a list beside the map. `DrawScope.drawTrack(points, projection)` draws a track through any `MapProjection`, which is how a map frame draws one.

## Invariants and traps

**Make each layer once.** `vectorBasemap(...)` and `rasterBasemap(...)` return a new `MapLayer` on every call, and the map keys a layer's tiles on the layer object, so a layer built inside a composable that recomposes throws away every loaded tile: the map blanks and refetches whenever unrelated state changes. Build it in a top-level or view-model `val`, or `remember` it.

**A failing `TileClient` fails silently.** An exception thrown from `fetch` is swallowed and the tile retried after ten seconds, exactly as if it had returned null, so a wrong key or an expired token gives a blank map and no error. Log inside the client. `fetch` runs on background workers, several tiles at once, never on the main thread, so the client must be safe to call concurrently. Return null for a non-success response rather than its body: the map then draws the nearest ancestor tile.

**No credit is shown unless a source carries one.** The attribution ornament shows only the `attribution` of each layer's `TileSource`; a source made without one shows nothing, and the ornament never shows credits for data drawn in the overlay, such as GeoJSON. Most tile licences, OpenStreetMap's among them, require the credit.

**`rememberWayfinderMapState` takes its arguments once.** It is a plain `remember` with no keys: a `center` that arrives later, such as the first location fix, is ignored and the map stays at the default, 0°, 0° at zoom 3. Move it with `moveTo` or `animateTo` instead. It is not saved either, so on Android an activity recreation, such as a rotation, puts the camera back where it started. Construct `WayfinderMapState(...)` in a view model to keep it.

**The animations need a composition's frame clock.** `animateTo`, `zoomIn` and `zoomOut` are Compose animations: call them from `LaunchedEffect` or `rememberCoroutineScope()`. Called from a view model's own scope they fail with "A MonotonicFrameClock is not available in this CoroutineContext". `moveTo` is immediate and needs no coroutine.

**What the state tracks, it tracks only while the map is on screen.** `visibleBounds` is null until the map is laid out and is updated only while a `WayfinderMap` with that state is composed; the same holds for `isInteracting` and for the `limits`. `isInteracting` cannot be set by the app, and is never true on a map with `MapGestures(enabled = false)`.

**Course-up needs a heading.** With `heading` null the map does not turn, and stays at whatever bearing it had. In course-up the user's two-finger turn is off whatever `MapGestures.rotate` says. Tapping the `Tape` compass leaves course-up for north-up; tapping the `Rosette` compass does not leave course-up, so the heading turns the map straight back.

**`fit` refuses an empty list and needs the map's size.** No points throw `IllegalArgumentException` ("Nothing to fit: the list of points is empty."). `IntSize.Zero`, or a map smaller than twice the padding, returns the farthest zoom, so a fit made before the map is measured shows the whole world. The size is the map's own, from `onSizeChanged` on its `modifier`.

**A bad `TileSource` fails when it is made, not when it draws.** A template missing `{z}`, `{x}` or `{y}` throws `IllegalArgumentException` ("Tile URL template … is missing {y}."), and so do a blank `id` and an empty or negative `zoomRange`. Give `zoomRange` the levels the source really has: past its end the map magnifies the deepest tile, but a range wider than the source asks for tiles that fail, and retries them. A TMS source, which numbers rows from the bottom, needs `tms = true`, or its tiles land in the wrong rows.

**Limits correct the camera, they do not stop it.** `CameraLimits` brings the camera back whenever a gesture or a move takes it out, so an edge can be overshot for a frame. The `center` box limits the middle of the map, not what the screen shows, and the zoom can never leave map-view's own range, 1 to 20, whatever `CameraLimits` says.

**Clusters are not markers.** With `clustering`, `onMarkerClick` runs only for single markers; a tapped group zooms the map in by `zoomStep`, two levels by default, or runs `onClusterClick` when one is given. At the closest zoom the limits allow nothing is grouped, so markers at the same spot are drawn on top of each other there. `MarkerClustering` refuses a radius, `step` or `zoomStep` that is not positive.

**A polyline joins its points in order.** A route with gaps is several `MapPolyline`s, or the map draws a straight line across each gap. Polylines, markers and tracks keep their width and size on screen at every zoom.

## What moved, and what it used to be called

`wayfinder` was first released in 0.0.3, so no earlier versions exist. 0.0.4 made `MapCompassTape`, `OrientationToggle` and `OrientationIcon` public; nothing else in its API has changed since.

- **It is not Google Maps Compose or MapLibre.** There is no `GoogleMap`, `CameraPositionState`, `Marker(state = ...)` child composable or style URL. The map is `WayfinderMap`, the camera is `WayfinderMapState`, markers are `MapMarker` values in the `markers` list, a coordinate is map-view's `LatLon(lat, lon)`, not `LatLng`, and tiles come from a `TileSource` and the app's `TileClient`.
- **It was never published as `kartog`.** A project that built these modules from source before 0.0.3 may have `com.oddlyclever.kartog` imports: `KartogMap`, `KartogMapState` and `rememberKartogMapState` are now `WayfinderMap`, `WayfinderMapState` and `rememberWayfinderMapState`; `KartogLayer` is `MapLayer`; `PointCluster.isGroup` is `isCluster`; and the separate track module is now the `com.oddlyclever.wayfinder.track` package of this one.

## What it is not for

It draws no basemap by itself: a map with no `layers` shows no tiles. Vector basemaps are in `com.oddlyclever:wayfinder-vector` (package `com.oddlyclever.wayfinder.vector`), raster basemaps in `com.oddlyclever:wayfinder-raster` (`com.oddlyclever.wayfinder.raster`), GeoJSON in `com.oddlyclever:wayfinder-geojson` (`com.oddlyclever.wayfinder.geojson`), maps drawn to images in `com.oddlyclever:wayfinder-frame` and map images on a server in `com.oddlyclever:wayfinder-headless`. It does not fetch, cache or store tiles, find the user's location or read a compass, search for places or route between them; the app supplies those.
