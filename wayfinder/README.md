# wayfinder

Maps for Compose Multiplatform. `WayfinderMap` is a map screen's worth of map: tiles, markers, lines, recorded tracks and the user's position, a compass, a scale bar, zoom buttons and attribution, with the camera, gestures and tile pipeline of [map-view](https://github.com/coderwise/libs-mobile) underneath. This root module is what an app uses; the other wayfinder modules each add one thing, and an app takes only the ones it needs.

**Targets:** JVM · Android · iOS (arm64, simulator) · JS · wasmJs

First released in 0.0.3, and not yet published to Maven Central. Its API is 0.x and built on map-view's, which is 0.x too, so a minor release may change it; the [changelog](../CHANGELOG.md) says when one does.

## Install

```kotlin
kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation("com.oddlyclever:wayfinder:0.0.7")
            implementation("com.oddlyclever:wayfinder-vector:0.0.7")   // for a vector basemap
        }
    }
}
```

wayfinder exposes map-view and map-view-tiles as `api`, so their types (`LatLon`, `MapCameraState`, `TileKey`, `MapOverlayScope`) are available without declaring them. There is no dependency-injection container: everything is constructed directly, and Koin is not needed.

On the JVM, wayfinder needs Java 21, because map-view's classes are built for it.

## A complete map

```kotlin
// 1. Where the tiles come from.
val streets = TileSource(
    id = "streets",
    urlTemplate = "https://tiles.example.com/{z}/{x}/{y}.pbf?key=$apiKey",
    zoomRange = 0..14,
    attribution = "© Example contributors",
)

// 2. How tiles are fetched: the app's HTTP client. wayfinder never fetches anything itself.
val client = TileClient { source, key -> http.get(source.url(key)).body<ByteArray>() }

// 3. The map.
@Composable
fun PlacesMap(places: List<Place>, onOpen: (String) -> Unit) {
    val state = rememberWayfinderMapState(center = LatLon(43.65, -79.38), zoom = 13f)
    WayfinderMap(
        state = state,
        modifier = Modifier.fillMaxSize(),
        layers = listOf(vectorBasemap(streets, client)),
        markers = places.map { MapMarker(it.id, it.position, Alignment.BottomCenter, description = it.name) { Pin() } },
        ornaments = MapOrnaments(zoomButtons = true),
        onMarkerClick = { onOpen(it.id) },
    )
}
```

Make the `TileClient` once, outside the composable, as here: it is compared by identity, and a new one on every recomposition makes the map refetch every tile. Drive the camera through the state, `state.moveTo(...)` or `state.animateTo(...)`, and read what is on screen from `state.visibleBounds`.

## How the modules fit together

The root holds the map, its state, markers, polylines, ornaments, recorded tracks and the user's position, tile sources and clients, and camera helpers, but draws no basemap by itself. [wayfinder-vector](../docs/knowledge/wayfinder-vector/README.md) and [wayfinder-raster](../docs/knowledge/wayfinder-raster/README.md) add vector and raster basemaps, [wayfinder-geojson](../docs/knowledge/wayfinder-geojson/README.md) adds GeoJSON, [wayfinder-frame](../docs/knowledge/wayfinder-frame/README.md) draws a map to an image, [wayfinder-headless](../docs/knowledge/wayfinder-headless/README.md) makes map images on a server, and [wayfinder-cache](../docs/knowledge/wayfinder-cache/README.md) caches tiles in front of the app's client. They meet at interfaces in the root, so none of them depends on another; [How the Wayfinder Modules Fit Together](../docs/knowledge/wayfinder/DOC-0023-how-the-wayfinder-modules-fit-together.md) has the details.

## Documentation

The full documentation is in the module's section of the [knowledge base](../docs/knowledge/wayfinder/README.md), which is mirrored to the repository's wiki:

- [How the Wayfinder Modules Fit Together](../docs/knowledge/wayfinder/DOC-0023-how-the-wayfinder-modules-fit-together.md): what each module adds, and what comes with the root.
- [The Map and What Goes on It](../docs/knowledge/wayfinder/DOC-0024-the-map-and-what-goes-on-it.md): every parameter of `WayfinderMap`, layers, polylines, the overlay, markers, gestures, and writing a layer.
- [Marker Clustering](../docs/knowledge/wayfinder/DOC-0025-marker-clustering.md): markers that cover each other drawn as one.
- [Ornaments and Attribution](../docs/knowledge/wayfinder/DOC-0026-ornaments-and-attribution.md): the compass, orientation toggle, scale bar, zoom buttons and credits.
- [The Map State and the Camera](../docs/knowledge/wayfinder/DOC-0027-the-map-state-and-the-camera.md): following the user, course-up, the visible bounds, fitting the camera, and limits.
- [Tile Sources and Tile Clients](../docs/knowledge/wayfinder/DOC-0028-tile-sources-and-tile-clients.md): URL templates, what the client returns, why a map comes out blank, and caching.
- [Tracks and the User's Position](../docs/knowledge/wayfinder/DOC-0029-tracks-and-the-users-position.md): recorded tracks and the location marker, live and on a map frame.
- [Maintaining wayfinder](../docs/knowledge/wayfinder/DOC-0030-maintaining-wayfinder.md): notes for working on the module itself.
- The decisions behind its shape: [Shared Interfaces in the Root Module](../docs/knowledge/wayfinder/ADR-0013-shared-interfaces-in-the-root-module.md), [The App Fetches Every Tile](../docs/knowledge/wayfinder/ADR-0014-the-app-fetches-every-tile.md), [Camera Limits Corrected After the Move](../docs/knowledge/wayfinder/ADR-0015-camera-limits-corrected-after-the-move.md) and [Attribution That Collapses to an "i"](../docs/knowledge/wayfinder/ADR-0016-attribution-that-collapses-to-an-i.md).

## Third-party code

The map's ornaments, in `com.oddlyclever.wayfinder.controls`, are derived from map-view-controls 0.3.0, part of [map-view](https://github.com/coderwise/libs-mobile), under the Apache License 2.0. They were moved into wayfinder's package and made internal, with `ScaleUnits` moved to `com.oddlyclever.wayfinder`, their words translated, and the scale bar's distance written by aughtone's `format-readable`; each file's header says so.

## For coding agents

The module ships an [Agent Skill](https://agentskills.io/specification), `com-oddlyclever-wayfinder`, for the agents of projects that depend on it: what the library is for, how it's meant to be called, and the mistakes that compile. It is written at [src/commonMain/skills/com-oddlyclever-wayfinder/SKILL.md](src/commonMain/skills/com-oddlyclever-wayfinder/SKILL.md) and published inside every sources jar, at `commonMain/skills/com-oddlyclever-wayfinder/`.
