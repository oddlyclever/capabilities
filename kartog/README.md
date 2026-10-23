# kartog

Maps for Compose Multiplatform. `KartogMap` is a map screen's worth of map: tiles, markers, lines, a compass, a scale bar, zoom buttons and attribution, with the camera, gestures and tile pipeline of [map-view](https://github.com/coderwise/libs-mobile) underneath. This root module is what an app uses; the other kartog modules each add one thing, and an app takes only the ones it needs.

**Targets:** JVM · Android · iOS (arm64, simulator) · JS · wasmJs

**In development.** Not yet released.

## Install

```kotlin
kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation("com.oddlyclever:kartog:0.0.3")
            implementation("com.oddlyclever:kartog-vector:0.0.3")   // for a vector basemap
        }
    }
}
```

kartog exposes map-view, map-view-tiles and map-view-controls as `api`, so their types (`LatLon`, `MapCameraState`, `TileKey`, `MapOverlayScope`) are available without declaring them. There is no dependency-injection container: everything is constructed directly, and Koin is not needed.

On the JVM, kartog needs Java 21, because map-view's classes are built for it.

## A complete map

```kotlin
// 1. Where the tiles come from.
val streets = TileSource(
    id = "streets",
    urlTemplate = "https://tiles.example.com/{z}/{x}/{y}.pbf?key=$apiKey",
    zoomRange = 0..14,
    attribution = "© Example contributors",
)

// 2. How tiles are fetched: the app's HTTP client. kartog never fetches anything itself.
val client = TileClient { source, key -> http.get(source.url(key)).body<ByteArray>() }

// 3. The map.
@Composable
fun PlacesMap(places: List<Place>, onOpen: (String) -> Unit) {
    val state = rememberKartogMapState(center = LatLon(43.65, -79.38), zoom = 13f)
    KartogMap(
        state = state,
        modifier = Modifier.fillMaxSize(),
        layers = listOf(vectorBasemap(streets, client)),
        markers = places.map { MapMarker(it.id, it.position, Alignment.BottomCenter, description = it.name) { Pin() } },
        ornaments = MapOrnaments(zoomButtons = true),
        onMarkerClick = { onOpen(it.id) },
    )
}
```

## KartogMap

```kotlin
@Composable
fun KartogMap(
    state: KartogMapState,
    modifier: Modifier = Modifier,
    layers: List<KartogLayer> = emptyList(),
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

Drawn bottom to top: the `layers` in order, the `polylines`, the `overlay`, the `markers`, then the ornaments.

- **Layers** come from the modules that draw tiles: `vectorBasemap` (kartog-vector) and `rasterBasemap` (kartog-raster). Each brings its source's attribution, which the attribution ornament shows. Layers stack: a raster hillshade at half alpha over a vector basemap.
- **Polylines** (`MapPolyline`) are drawn the same width on screen at every zoom, cross the antimeridian the short way, and are tappable with `onClick`. Draw a route in pieces as several polylines, so the map never joins the pieces with a straight line.
- **The overlay** is for anything else placed by coordinate: a track and the user's position (kartog-track), GeoJSON (kartog-geojson), or your own drawing through `MapOverlayScope.project`.
- **Markers** (`MapMarker`) are any composable, anchored at a coordinate and the same size at every zoom. `anchor` is the part of the content that lands on the point, `Alignment.BottomCenter` for the tip of a pin. `description` is what a screen reader says. With no content the marker is `DefaultMarker()`, a ringed dot. `onMarkerClick` receives the marker that was tapped.
- **`onTap`** receives the coordinate of a tap that no marker or line took, once the double-tap window has passed.

### Gestures

`MapGestures(enabled = true, rotate = true)`. Enabled, the map pans, pinches, flings, zooms on double-tap and on double-tap-and-drag, and zooms on the mouse wheel. `rotate` allows two-finger turning, with a slop so a pinch is not mistaken for a twist; it is off in course-up regardless. `enabled = false` makes a map the user can look at but not move.

### Limits

`CameraLimits(zoom = ZOOM_LIMITS, center = null)` keeps the camera in a range of zoom and, when `center` is a `GeoBounds`, keeps the middle of the map inside that box, which can cross the antimeridian. It limits where the middle goes, not what the screen shows, so zoomed out the edges can show ground outside the box. `CameraLimits(zoom = 10f..18f, center = GeoBounds(south = 43.5, west = -80.0, north = 44.0, east = -79.0))` holds a map to one town. map-view moves the camera directly from its gestures, so kartog brings it back inside the limits whenever it moves out; at an edge a gesture can show it past the limit for a frame. Holding it there in the first place is asked of map-view.

### Clustering

Markers close together cover each other, so only the top one can be read or tapped. Pass `clustering = MarkerClustering()` and markers whose positions fall within `radius` (40 dp) of each other at the current zoom are drawn as one marker for the group, centred on the middle of its markers. As the map zooms in, the groups part.

```kotlin
KartogMap(
    state = state,
    markers = markers,
    clustering = MarkerClustering(
        description = { "${it.size} places" },
        content = { group -> DefaultClusterMarker(group.size) },
    ),
)
```

- **Tapping a group** zooms the map `zoomStep` (2) levels closer, centred on it, no closer than the limits allow. Give `onClusterClick` to do something else, such as listing the group's markers; `onMarkerClick` is only for single markers.
- **The groups are worked out again at every `step` (0.5) of zoom**, not every frame of a pinch, so a map of hundreds of markers stays smooth and groups do not flicker at a boundary.
- **At the closest zoom the limits allow, nothing is grouped.** Zooming no further cannot part markers at the same spot, so there they are drawn on top of each other, as they would be without clustering.
- **A group's look and words are the app's.** `content` draws it, `DefaultClusterMarker(count)` (a ringed dot with the count) unless the app draws its own, and `description` is what a screen reader says, null unless the app gives it.
- **`clusterPoints(items, zoom, radius) { position }`** is the grouping on its own, for anything that is not a `MapMarker`: a list beside the map, a count in a header. It measures on the same plane map-view draws, the short way round the antimeridian, and gives the same groups for the same points at the same zoom. It is quadratic, meant for the hundreds of markers a screen shows.

### Ornaments

| `MapOrnaments` field | Default | What it is |
|---|---|---|
| `compass`, `compassAlignment` | on, top centre | the compass; tapping it turns the map back to north-up |
| `compassStyle` | `Tape` | `Tape` is map-view's strip of headings across the map, for a map someone navigates by; `Rosette` is its small needle that always points north, for a map someone looks around on |
| `orientationToggle`, `orientationToggleAlignment` | off, top end | a button switching between north-up and course-up |
| `scaleBar`, `scaleBarAlignment`, `scaleUnits` | on, bottom start, metric | map-view's scale bar |
| `zoomButtons`, `zoomButtonsAlignment` | off, centre end | map-view's zoom buttons |
| `attribution`, `attributionAlignment` | on, bottom end | the credits the layers' sources ask for; leave it on wherever a licence requires attribution |
| `attributionStyle` | `Collapsible` | `Collapsible` shows the credits when the map appears, then shrinks them to an "i"; `Full` keeps them in full |
| `attributionTextSize` | 9 sp | the credits' text size |
| `padding` | 8 dp | how far ornaments sit from the edges, to clear an app bar or a sheet |

**Attribution, small and out of the way.** A map's credits clutter a small screen, so by default they are shown in full when the map appears and shrink to an "i" after five seconds or at the user's first pan, zoom or turn, whichever comes first. Tapping the "i" shows them again, by the same rule. That is the form OpenStreetMap's attribution guidelines allow for an interactive map: visible on first display, then collapsed, and there for anyone who looks for it. Hiding the credits from the start is not allowed, and their text must stay readable; 9 sp is about as small as reads. Pass `onAttributionClick` to `KartogMap` to open the page behind them, such as `https://www.openstreetmap.org/copyright`, for instance with Compose's `LocalUriHandler`.

## KartogMapState

Everything the app reads or drives. `rememberKartogMapState(center, zoom, bearing)` keeps one for the composition; construct `KartogMapState(...)` directly to hold it in a view model.

| Member | What it is |
|---|---|
| `center`, `zoom`, `bearing` | where the camera is |
| `moveTo(center, zoom)`, `moveTo(position)` | put the camera somewhere at once |
| `animateTo(center, zoom)`, `animateTo(position)` | fly there, the short way round the world |
| `zoomIn()`, `zoomOut()` | one level, animated |
| `isInteracting` | true while the user has hold of the map, and for two seconds after they let go |
| `orientation`, `heading` | `Orientation.NorthUp` or `CourseUp`; in course-up the map turns so `heading` is at the top |
| `visibleBounds` | what the map shows, as a `GeoBounds`, kept current as the map moves or is resized |
| `camera` | map-view's `MapCameraState`, for anything kartog does not wrap |

**Following the user without fighting them.** Move the camera on the app's behalf only while `isInteracting` is false. The two-second settle is what stops the map snapping back the moment a pan ends: people lift a finger, look, and carry on.

```kotlin
LaunchedEffect(position) {
    if (!state.isInteracting) state.animateTo(position)
}
```

**Course-up.** Set `orientation = Orientation.CourseUp` and keep `heading` current, from a compass or the direction of travel. The map turns, the user's two-finger turn is off, and tapping the compass returns to north-up.

**Querying what is on screen.** `visibleBounds` is the smallest latitude and longitude box holding everything visible, turned or not. When the map crosses the antimeridian, `west` is greater than `east` and `crossesAntimeridian` is true; `point in bounds` accounts for it.

```kotlin
LaunchedEffect(state) {
    snapshotFlow { state.visibleBounds }.filterNotNull().debounce(300).collect { bounds -> search(bounds) }
}
```

## Tile sources and clients

A `TileSource` says where a layer's tiles come from: an `id`, a `{z}/{x}/{y}` `urlTemplate`, the `zoomRange` the source has, whether it numbers rows TMS style (`tms = true` flips the row in `url(key)`), and the `attribution` its licence asks for. A template missing a placeholder is refused when the source is made. Two sources are equal when they would fetch the same tiles.

A `TileClient` is the app's fetching behind one call, `fetch(source, key): ByteArray?`. The app builds the URL with `source.url(key)` and adds its keys and headers. Return null for a tile that is not there or could not be fetched: the map draws the nearest ancestor instead.

**Caching** is the app's choice, and map-view-cache (`com.coderwise.libs:map-view-cache`) fits in front of a client without translation, since its stores are keyed by source and tile too:

```kotlin
val store = FileTileStore(FileSystem.SYSTEM, cacheDir / "tiles", maxBytes = 200L * 1024 * 1024)
val client = TileClient { source, key ->
    cached(store, source.id, CacheMode.CacheFirst) { k -> http.get(source.url(k)).body<ByteArray>() }(key)
}
```

It also has a `MemoryTileStore`, and the modes `NetworkFirst`, `OfflineOnly` and `NoCache`.

## Camera helpers

`fit(points, size, density, padding, bearing, maxZoom)` returns the `CameraPosition` that frames every point inside the padding, as close in as they allow. It respects the bearing, crosses the antimeridian the short way, puts a single point at `maxZoom` (street level, 17, by default), and gives the farthest zoom rather than an error for a map not yet measured.

```kotlin
state.animateTo(fit(route, mapSize, density, padding = 32.dp, bearing = state.bearing))
```

On map-view's camera directly: `screenPosition(point, size, density)` and `isOnScreen(point, size, density, margin)` say where a point falls with the bearing taken into account, for code outside an overlay, and `visibleBounds(size, density)` is what `KartogMapState` keeps current. For a scale, map-view's own `MapCameraState.metersPerDp`.

## Writing a layer

A `KartogLayer` is how a module puts tiles on the map. In `place(state)`, which `KartogMap` calls during composition, it remembers its tile state and returns a `PlacedLayer`: the map-view `MapState` holding the tiles, one or more composables that draw a tile, each drawn as a layer of its own so a basemap's names go over every tile, and its attribution.

```kotlin
val hillshade = KartogLayer { _ ->
    val tiles = rememberTileState(key = shade, zoomRange = shade.zoomRange) { key -> decodeRasterTile(shade.fetch(client, key)) }
    PlacedLayer(tiles, listOf { key -> tiles.shown(key)?.let { RasterSlot(it.content, it.src, Modifier.alpha(0.5f)) } }, shade.attribution)
}
```

## How the modules fit together

| Module | What it adds |
|---|---|
| `kartog` | This root: the map, its state, markers, polylines, ornaments, tile sources and clients, camera helpers |
| [`kartog-vector`](../kartog-vector) | A vector basemap; light and dark styles; vector tiles drawn as images on GPUs that cannot draw their paths |
| [`kartog-raster`](../kartog-raster) | A raster basemap, decoded off the main thread and faded in |
| [`kartog-geojson`](../kartog-geojson) | GeoJSON on the map, from text or a URL |
| [`kartog-track`](../kartog-track) | Recorded tracks and the user's position, live and on images |
| [`kartog-static`](../kartog-static) | Maps drawn to images, or to a painter that scales like a vector image, encoded as PNG or WebP |
| [`kartog-headless`](../kartog-headless) | Map images on a server, JVM only, in one call |

Three interfaces here are how the other modules work together without depending on one another: `KartogLayer` and `PlacedLayer` put tiles on a live map, `StaticLayer` and `StaticTile` put them on an image, and `MapProjection` places a coordinate on either, so a track draws the same way on both.
