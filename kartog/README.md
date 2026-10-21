# kartog

Maps for Compose Multiplatform, built on [map-view](https://github.com/coderwise/libs-mobile). map-view draws the map and carries most of what a map screen needs: tiles, the camera, gestures, vector tiles and their labels, a tile cache, GeoJSON, and the controls around a map. kartog adds what it leaves out. It comes as a family: this base, and extensions an app takes only when it uses them.

**Targets:** JVM · Android · iOS (arm64, simulator) · JS · wasmJs

**In development.** This base holds tile sources, camera helpers, and the layer interface static maps are drawn through.

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
| `kartog` | This base: tile sources and camera helpers |
| [`kartog-vector`](../kartog-vector) | Light and dark vector styles, and vector tiles drawn to images on GPUs that cannot draw their paths |
| [`kartog-raster`](../kartog-raster) | Raster tiles decoded off the main thread, and a slot that fades each tile in |
| [`kartog-static`](../kartog-static) | Maps drawn to images without a composition, with a recorded track, encoded as PNG or WebP |

`StaticLayer` and `StaticTile`, in this base, are how the vector and raster modules supply layers to `kartog-static` without it depending on either. For a tile cache, GeoJSON, and controls such as course-up, a compass and a scale bar, use map-view's own `map-view-cache`, `map-view-geojson` and `map-view-controls`.
