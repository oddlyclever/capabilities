# kartog

Maps for Compose Multiplatform, built on [map-view](https://github.com/coderwise/libs-mobile). map-view draws the map and carries most of what a map screen needs: tiles, the camera, gestures, vector tiles and their labels, a tile cache, GeoJSON, and the controls around a map. kartog adds what it leaves out. It comes as a family: this base, and extensions an app takes only when it uses them.

**Targets:** JVM · Android · iOS (arm64, simulator) · JS · wasmJs

**In development.** Not yet released.

## Install

```kotlin
kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation("com.oddlyclever:kartog:0.0.3")
        }
    }
}
```

kartog exposes map-view and map-view-tiles as `api`, so their types (`MapView`, `MapCameraState`, `TileKey`, `MapState`) are available without declaring them. There is no dependency-injection container: everything is constructed directly.

On the JVM, kartog needs Java 21, because map-view's own classes do.

## The map

```kotlin
val state = rememberKartogMapState(center = LatLon(43.65, -79.38), zoom = 13f)

KartogMap(
    state = state,
    modifier = Modifier.fillMaxSize(),
    layers = listOf(vectorBasemap(streets, client)),          // from kartog-vector
    markers = places.map { MapMarker(it.id, it.position, Alignment.BottomCenter, it.name) { Pin() } },
    polylines = listOf(MapPolyline("route", route, Color(0xFF7C3AED))),
    ornaments = MapOrnaments(zoomButtons = true, orientationToggle = true),
    onMarkerClick = { open(it.id) },
) {
    Track(recorded, state)                                    // from kartog-track
}
```

- **Layers** are drawn in order and come from the modules that draw them: `vectorBasemap` in kartog-vector, `rasterBasemap` in kartog-raster. Each brings its source's attribution, which the map shows.
- **The overlay** draws anything placed by coordinate over the lines and under the markers: a track, the user's position, GeoJSON.
- **Markers** are any composable, anchored at a coordinate, the same size at every zoom; the default is a ringed dot. **Polylines** are drawn the same width at every zoom and can be tapped.
- **Ornaments** are map-view's controls, each switched on or off and placed in a corner: the compass tape (tap for north-up), a north-up/course-up toggle, the scale bar, zoom buttons and attribution.
- **Gestures** are map-view's: pan, pinch, fling, double-tap, double-tap-and-drag, the mouse wheel, and two-finger turning, which `MapGestures` can switch off.

`KartogMapState` is everything the app reads or drives:

- **The camera:** `center`, `zoom`, `bearing`; `moveTo`, `animateTo`, `zoomIn`, `zoomOut`; map-view's own `camera` for anything else.
- **`isInteracting`:** true while the user has hold of the map and for two seconds after, so an app that follows the user's position does not snap the map back the moment a pan ends.
- **`orientation` and `heading`:** in course-up the map turns so the heading is at the top, and the user cannot turn it.
- **`visibleBounds`:** what the map shows, kept current as it moves or is resized, for querying what is on screen.

## Tile clients

kartog fetches nothing. A `TileClient` is the app's HTTP client, keys and cache behind one call: `fetch(url, cacheKey)`, returning the bytes or null. Layers ask it for a tile's URL with a cache key naming the source and tile, `streets/14/4823/6160`. map-view-cache's `cached` and `FileTileStore` are one way to cache.

## Tile sources

A `TileSource` says where a layer's tiles come from: a URL template, the zoom levels the source has, whether it numbers rows TMS style, and the credit its licence asks for. It fetches nothing; loading is the app's.

```kotlin
val streets = TileSource(
    id = "streets",
    urlTemplate = "https://tiles.example.com/{z}/{x}/{y}.pbf",
    zoomRange = 0..14,
    attribution = "© Example contributors",
)

val ground = rememberTileState(key = streets, zoomRange = streets.zoomRange) { key ->
    http.get(streets.url(key))
}
```

- **All three placeholders are required.** A template without `{z}`, `{x}` or `{y}` is refused when the source is made, since it would fetch the same tile for many keys.
- **A TMS source has its row flipped.** map-view numbers rows from the top; `tms = true` turns the row over in `url(key)` for a source that numbers them from the bottom.
- **Use the source as the tile state's key.** Two sources are equal when they would fetch the same tiles, so switching to a different source starts fresh slots and leaves nothing of the old one on screen.
- **Pass the source's `zoomRange` on to the tile state.** Past its last level the map magnifies the deepest tile instead of asking for one the source does not have.

`url(key)` builds the same URL as map-view-cache's `tileUrl`, so a source and that cache can be used together. kartog does not depend on map-view-cache, which brings a file system library with it.

## Camera helpers

`fit` finds the camera that shows a set of points on a map of a given size, as close in as they allow:

```kotlin
val position = fit(track, size = mapSize, density = LocalDensity.current, padding = 24.dp, bearing = camera.bearing)
camera.moveTo(position)        // at once
camera.flyTo(position)         // animated, then turned to the position's bearing
```

- **It respects the bearing.** A long north-south track on a map with east up is fitted across the screen, not down it.
- **It crosses the antimeridian the short way.** Points either side of 180° are framed over the Pacific, not stretched across the world.
- **A single point sits at `maxZoom`**, street level by default, so it still shows the streets around it.
- **A map with no room inside its padding**, such as one not yet measured, gets the farthest zoom rather than an error.

`zoomIn()` and `zoomOut()` animate one level about the middle of the map. `screenPosition(point, size, density)` and `isOnScreen(point, size, density, margin)` answer where a point falls with the bearing taken into account, for code outside an overlay; inside one, map-view's own `project()` does the same. For a scale bar, map-view's `MapCameraState.metersPerDp` gives the ground a dp covers.

## The kartog family

| Module | What it adds |
|---|---|
| `kartog` | This root: the map, its state, markers, polylines, ornaments, tile sources and clients, camera helpers |
| [`kartog-vector`](../kartog-vector) | A vector basemap; light and dark styles; vector tiles drawn as images on GPUs that cannot draw their paths |
| [`kartog-raster`](../kartog-raster) | A raster basemap, decoded off the main thread and faded in |
| [`kartog-geojson`](../kartog-geojson) | GeoJSON on the map, from text or a URL |
| [`kartog-track`](../kartog-track) | Recorded tracks and the user's position, live and on images |
| [`kartog-static`](../kartog-static) | Maps drawn to images or to a painter that scales like a vector image, encoded as PNG or WebP |
| [`kartog-headless`](../kartog-headless) | Map images on a server, JVM only, in one call |

`StaticLayer`, `StaticTile` and `MapProjection`, in this root, are how the other modules work together without depending on each other.
