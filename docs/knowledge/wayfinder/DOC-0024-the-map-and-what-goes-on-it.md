# The Map and What Goes on It

DOC-0024 · 2026-11-04
Keywords: WayfinderMap parameters, map composable, layers, basemap, markers, pins, MapMarker, anchor, BottomCenter, DefaultMarker, polylines, MapPolyline, route with gaps, straight line across a gap, overlay, MapOverlayScope, project, onTap, onMarkerClick, tap on map, gestures, pan, pinch, rotate, mouse wheel, disable map gestures, static map, custom layer, MapLayer, PlacedLayer, tiles refetched on recomposition, map blanks, Google Maps Compose, GoogleMap, Marker composable, MapLibre, style URL

`WayfinderMap` is the map: tiles, markers, lines and anything else placed by coordinate, with gestures and the ornaments around them. Everything on it is passed as data. Markers, lines and layers are parameters, not child composables, so there is no `GoogleMap`, no `Marker(state = ...)` and no style URL.

## The signature

```kotlin
@Composable
fun WayfinderMap(
    state: WayfinderMapState,
    modifier: Modifier = Modifier,
    layers: List<MapLayer> = emptyList(),
    markers: List<MapMarker> = emptyList(),
    polylines: List<MapPolyline> = emptyList(),
    gestures: MapGestures = MapGestures(),
    ornaments: MapOrnaments = MapOrnaments(),
    limits: CameraLimits = CameraLimits(),
    onTap: ((LatLon) -> Unit)? = null,
    onMarkerClick: ((MapMarker) -> Unit)? = null,
    onAttributionClick: (() -> Unit)? = null,
    clustering: MarkerClustering? = null,
    onClusterClick: ((PointCluster<MapMarker>) -> Unit)? = null,
    overlay: @Composable MapOverlayScope.() -> Unit = {},
)
```

`state` is covered in [The Map State and the Camera](DOC-0027-the-map-state-and-the-camera.md), `limits` there too, `ornaments` and `onAttributionClick` in [Ornaments and Attribution](DOC-0026-ornaments-and-attribution.md), and `clustering` and `onClusterClick` in [Marker Clustering](DOC-0025-marker-clustering.md).

## Drawing order

Bottom to top: the `layers` in order, the `polylines`, the `overlay`, the `markers`, then the ornaments.

## Layers

Layers come from the modules that draw tiles: `vectorBasemap` in [wayfinder-vector](../wayfinder-vector/README.md) and `rasterBasemap` in [wayfinder-raster](../wayfinder-raster/README.md). Each brings its source's attribution, which the attribution ornament shows. Layers stack, so a raster hillshade at half alpha can go over a vector basemap.

A map with no layers shows no tiles.

## Polylines

A `MapPolyline(id, points, color, width = 4.dp, onClick = null)` is a line through its points in order. It is drawn the same width on screen at every zoom, crosses the antimeridian the short way, and is tappable when it has an `onClick`.

A polyline joins every point to the next. A route recorded in pieces, with gaps where the signal dropped, is several polylines, or the map draws a straight line across each gap.

## The overlay

The trailing `overlay` lambda is for anything else placed by coordinate: a track and the user's position (see [Tracks and the User's Position](DOC-0029-tracks-and-the-users-position.md)), GeoJSON from [wayfinder-geojson](../wayfinder-geojson/README.md), or your own drawing through `MapOverlayScope.project`, which turns a `LatLon` into a position on screen. It is drawn over the polylines and under the markers.

## Markers

A `MapMarker(id, position, anchor = Alignment.Center, description = null, content = { DefaultMarker() })` is any composable, anchored at a coordinate and the same size at every zoom.

- **`anchor`** is the part of the content that lands on the point: `Alignment.BottomCenter` for the tip of a pin.
- **`description`** is what a screen reader says for it.
- **`content`** defaults to `DefaultMarker()`, a white-ringed dot, 14 dp across, blue unless given a colour.
- **`id`** keeps the marker's state across recompositions, and is how the app tells which one was tapped.

`onMarkerClick` receives the `MapMarker` that was tapped. With clustering on, it runs only for single markers.

## Taps

`onTap` receives the coordinate of a tap that no marker or line took, once the double-tap window has passed.

## Gestures

`MapGestures(enabled = true, rotate = true)`.

- **Enabled**, the map pans, pinches, flings, zooms on double-tap and on double-tap-and-drag, and zooms on the mouse wheel.
- **`rotate`** allows two-finger turning, with a slop so a pinch is not mistaken for a twist. It is off in course-up whatever it says, since the heading turns the map then.
- **`enabled = false`** makes a map the user can look at but not move.

The gestures are on a box around the map, so the map and every layer on it move together.

## Writing a layer

A `MapLayer` is how a module puts tiles on the map. In `place(state)`, which `WayfinderMap` calls during composition once per layer, it remembers its tile state and returns a `PlacedLayer`:

- **`state`**, the map-view `MapState` holding the tiles;
- **`contents`**, one or more composables that draw a tile, each drawn as a layer of its own, in order. A vector basemap is two, ground then names, so every name goes over every tile;
- **`attribution`**, the credit its source asks for, or null.

```kotlin
val hillshade = MapLayer { _ ->
    val tiles = rememberTileState(key = shade, zoomRange = shade.zoomRange) { key -> decodeRasterTile(shade.fetch(client, key)) }
    PlacedLayer(tiles, listOf { key -> tiles.shown(key)?.let { RasterSlot(it.content, it.src, Modifier.alpha(0.5f)) } }, shade.attribution)
}
```

**A layer has to be equal across recompositions, or it loses its tiles.** The map keys a layer's tile state on the layer itself, by `equals`. A layer that compares equal to the one passed last time keeps its tiles; one that does not starts again with none, and the map blanks and refetches everything. The layers the wayfinder modules return are equal whenever they were made from equal arguments, so they can be made inside a composable. A layer written as a lambda, like the one above, is equal only to itself, so make it once, outside the composable that draws the map.

The same holds for what a basemap is made from: a `TileClient` is compared by identity, so one written inline in a composable is a new client on every recomposition. See [Tile Sources and Tile Clients](DOC-0028-tile-sources-and-tile-clients.md).
