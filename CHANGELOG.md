# Changelog

Every module is released together, at one version, so one entry covers them all. A change that breaks source compatibility says so in its entry, with what to do about it.

The format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/). Versions are 0.x: until 1.0, a minor release may change a public API, and its entry says how.

## [0.0.4] - 2026-10-26

Tagged here and, like 0.0.3, not yet published to Maven Central.

### Added

- **`wayfinder`**: `MapCompassTape`, `OrientationToggle` and `OrientationIcon` are public, so a screen can place the compass tape and the orientation toggle in its own layout, or draw its own button around the icon. `MapCompassTape` takes the tape's background, for a screen that draws it on a plate of its own. The map's own ornaments draw the same composables, so nothing changes for a map that keeps them.

## [0.0.3] - 2026-10-26

The first release, as `com.oddlyclever:<module>:0.0.3`. It is tagged here but not yet published to Maven Central, so until it is, these coordinates resolve from a composite build or `mavenLocal` only.

### Added

- **`wayfinder`**: maps for Compose Multiplatform on map-view. `WayfinderMap` with its state, markers that cluster when they cover each other, polylines, a compass, scale bar, zoom buttons and an orientation toggle, camera limits, recorded tracks and the user's position.
- **`wayfinder-vector`**: vector basemaps in light and dark styles that name places, roads, water, rivers, peaks and points of interest, and vector tiles drawn to images on devices whose GPU cannot draw their paths.
- **`wayfinder-raster`**: raster basemaps, decoded off the main thread and faded in as they arrive.
- **`wayfinder-geojson`**: GeoJSON on a wayfinder map, from text or a URL.
- **`wayfinder-frame`**: map frames, a map drawn at one camera position and size without a composition, as an image, a painter that scales like a vector image, or a `MapFrame` in a layout, with names and a track over it; PNG or WebP.
- **`wayfinder-headless`**: map images on a server in one call, WebP or PNG. JVM only: it bundles Skia's native libraries for Linux and macOS.
- **`h3spatial`**: Uber's H3 hexagonal grid behind one `String`-cell API, with the same answers on every target.
- **`media-image`**: images downsized and encoded for an upload or a file, as WebP, JPEG or PNG, stood upright, keeping their location and capture time when asked.
- **`media-webp`**: WebP encoding, lossy or lossless, with GPS position and capture time in the file, and libwebp built in on Android and iOS.
- **`platform`**: on Android, the application context and the activity in front, captured once at start-up; and `OsFamily` everywhere.

### Requirements

- **Targets:** JVM, Android, iOS (arm64 and the arm64 simulator), JS and wasmJs in the browser. `wayfinder-headless` is JVM only.
- **JVM:** Java 21.
- **Android:** API level 24 or later. An app that calls one of these modules' inline functions compiles with `jvmTarget` 21.
- **Built with:** Kotlin 2.4.21, Compose Multiplatform 1.12.2 (Skiko 0.150.2) and map-view 0.3.0.

### For projects that built from source before this release

The map modules were developed as `kartog`, built only from source and never published under that name. A project that included this repository as a composite build switches to the `wayfinder` names with [docs/outbox/wayfinder-migration.md](docs/outbox/wayfinder-migration.md), a prompt an agent in that project can follow.

[0.0.4]: https://github.com/oddlyclever/capabilities/releases/tag/v0.0.4
[0.0.3]: https://github.com/oddlyclever/capabilities/releases/tag/v0.0.3
