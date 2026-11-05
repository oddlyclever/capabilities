# How the Wayfinder Modules Fit Together

DOC-0023 · 2026-11-04
Keywords: which wayfinder module do I need, wayfinder family, basemap module, vector or raster, GeoJSON on a map, map to image, map image on a server, module dependencies, map-view types, LatLon not found, MapCameraState, TileKey, MapOverlayScope, Koin, dependency injection, Java 21, UnsupportedClassVersionError, MapLayer, PlacedLayer, MapFrameLayer, MapFrameTile, MapProjection

wayfinder is a family of modules. `wayfinder`, the root, is the map an app shows; every other module adds one thing to it, and an app takes only the ones it needs. This page says what each module adds, how they connect without depending on one another, and what comes with the root.

## The modules

| Module | What it adds |
|---|---|
| `wayfinder` | The root: the map, its state, markers, polylines, ornaments, recorded tracks and the user's position, tile sources and clients, camera helpers |
| [`wayfinder-vector`](Wayfinder-Vector) | A vector basemap; light and dark styles; vector tiles drawn as images on GPUs that cannot draw their paths |
| [`wayfinder-raster`](Wayfinder-Raster) | A raster basemap, decoded off the main thread and faded in |
| [`wayfinder-geojson`](Wayfinder-Geojson) | GeoJSON on the map, from text or a URL |
| [`wayfinder-cache`](Wayfinder-Cache) | A tile cache in front of the app's `TileClient`, on disk or in memory; Compose-free |
| [`wayfinder-frame`](Wayfinder-Frame) | Map frames: a map at one camera position and size, drawn to an image or to a painter that scales like a vector image, encoded as PNG or WebP |
| [`wayfinder-headless`](Wayfinder-Headless) | Map images on a server, JVM only, in one call |

The root draws no basemap by itself: a `WayfinderMap` with no `layers` shows no tiles. A typical app takes `wayfinder` and one basemap module, `wayfinder-vector` or `wayfinder-raster`, and adds the others as it needs them.

## How they connect

`wayfinder-vector`, `wayfinder-raster`, `wayfinder-geojson` and `wayfinder-frame` each depend on the root and on nothing else in the family. `wayfinder-headless` is the exception: it is built from `wayfinder-frame`, `wayfinder-vector` and `wayfinder-raster`. `wayfinder-cache` depends on none of them, not even the root: it wraps the app's tile fetching, so it needs only map-view-core's `TileKey`.

Three interfaces in the root are how the modules work together without depending on one another:

- **`MapLayer` and `PlacedLayer`** put tiles on a live map. `vectorBasemap` and `rasterBasemap` return a `MapLayer`, and `WayfinderMap` takes a list of them. See [The Map and What Goes on It](Wayfinder-The-Map-And-What-Goes-On-It).
- **`MapFrameLayer` and `MapFrameTile`** put tiles on an image. The vector and raster modules each supply one, and `wayfinder-frame`'s renderer draws any of them, so an app that only draws raster images never pulls in the vector renderer.
- **`MapProjection`** places a coordinate on either a live map or an image, so a track and the user's position draw the same way on both. See [Tracks and the User's Position](Wayfinder-Tracks-And-The-Users-Position).

Why these live in the root is in [Shared Interfaces in the Root Module](Wayfinder-Shared-Interfaces-In-The-Root-Module).

## What comes with the root

wayfinder is built on [map-view](https://github.com/coderwise/libs-mobile), a tiled map for Compose Multiplatform, and exposes two of its artifacts as `api`: `map-view` and `map-view-tiles`. Their types are part of wayfinder's own API, so `LatLon`, `MapCameraState`, `MapState`, `TileKey` and `MapOverlayScope`, in `com.coderwise.libs.mapview`, are available without declaring them. map-view exposes Compose in turn, which is where wayfinder's Compose comes from.

The map's ornaments, the compass, scale bar, zoom buttons and credits, are wayfinder's own. They began as map-view's `map-view-controls`, which wayfinder no longer depends on, and are internal to wayfinder apart from `ScaleUnits`, in `com.oddlyclever.wayfinder`.

A coordinate is map-view's `LatLon(lat, lon)`. There is no `LatLng`.

There is no dependency-injection container. Everything is constructed directly, and Koin is not needed.

## Platforms

The root runs on the JVM, Android, iOS (arm64 and the arm64 simulator), JS and wasmJs in the browser. It is called from Kotlin only: there is no Swift or JavaScript surface.

On the JVM it needs Java 21, because map-view's classes are built for it. An older JVM fails at class loading with `UnsupportedClassVersionError`.

## Versions

The API is 0.x and built on map-view's, which is 0.x too, so a minor release may change it. wayfinder stays in step with map-view's releases, and a map-view release that changes its types changes wayfinder's API with them. The [changelog](https://github.com/oddlyclever/capabilities/blob/HEAD/CHANGELOG.md) says when a release breaks source compatibility.
