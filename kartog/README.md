# kartog

Maps for Compose Multiplatform, built on [map-view](https://github.com/coderwise/libs-mobile). map-view draws the map: tiles, the camera, gestures, vector tiles and their labels. kartog adds what an app needs around it. It comes as a family: this base, and extensions an app takes only when it uses them.

**Targets:** JVM · Android · iOS (arm64, simulator) · JS · wasmJs

**In development.** This base holds tile sources so far. Camera helpers, shapes on the map and the extensions are still to come.

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

## The kartog family

| Module | What it adds |
|---|---|
| `kartog` | This base: tile sources, and the parts every extension shares |

The extensions arrive here as they are built.
