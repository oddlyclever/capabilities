# Changelog

Every module is released together, at one version, so one entry covers them all. A change that breaks source compatibility says so in its entry, with what to do about it.

The format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/). Versions are 0.x: until 1.0, a minor release may change a public API, and its entry says how.

## [Unreleased]

### Added

- **`sundries-compose`**, a new module: the Compose parts every app ends up writing for itself. `TextCopy` is text that copies itself when tapped, with a tick when copied and a warning when refused, one labelled button to a screen reader, and on the web a fallback for pages served over plain http, where browsers have no clipboard API; `rememberCopier` gives the same copying to an app's own controls. With it come conditional modifiers, optional test tags, colour and size conversions, window-size checks, glowing text and `rawStringResource`, which reads a raw text file with the app's own resource reader. Its package is `com.oddlyclever.sundries`.
- **`wayfinder-cache`**, a new module: a tile cache in front of the app's `TileClient`, with a size-bounded store on disk or in memory and the modes `CacheFirst`, `NetworkFirst`, `OfflineOnly` and `NoCache`. It is map-view's map-view-cache 0.3.0, which map-view is dropping, moved into `com.oddlyclever.wayfinder.cache`; code written against map-view-cache changes its imports, and builds tile URLs with `TileSource.url` in place of the dropped `tileUrl`.
- **`wayfinder`**: the map's ornaments speak the device's language: what each control is called to a screen reader, and the compass tape's points, in every language aughtone-types defines but Inuktitut, chosen by aughtone's `Locale.current`, and English for any other language. `MapOrnamentLabels`, on `MapOrnaments.labels` and as a parameter of `MapCompassTape` and `OrientationToggle`, replaces any of those words with the app's own; every field is null unless set.
- **Every module ships an agent skill:** an [Agent Skill](https://agentskills.io/specification) named `com-oddlyclever-<module>`, for the coding agents of projects that depend on it, saying what the module is for, how it is meant to be called, and the mistakes that compile. It is published inside every sources jar, at `commonMain/skills/com-oddlyclever-<module>/`; `wayfinder-headless`, a JVM module, has it at `skills/com-oddlyclever-wayfinder-headless/`.

### Fixed

- **`wayfinder-headless`**: `renderMapImage`'s `style` can be passed. The module now exposes `wayfinder-vector`, so `MapStyles` and `VectorStyle` resolve; before, passing `style` failed to compile unless the app also depended on `wayfinder-vector`.
- **`wayfinder-vector`, `wayfinder-raster`**: `vectorBasemap` and `rasterBasemap` layers made from equal arguments are equal, so a layer made inside a composable keeps its tiles across recompositions instead of fetching them all again. The `TileClient` is compared by identity, so it still has to be made once.
- **`wayfinder-geojson`**: a `RemoteGeoJsonLayer` whose `load` throws draws nothing, as one that returns null does, instead of crashing the app.
- **`wayfinder`**: `rememberWayfinderMapState` is saved with the screen's instance state, so a rotation keeps the camera and the orientation. `WayfinderMapState.Saver` is public, for a state kept elsewhere.
- **`h3spatial`**: the KDoc of `h3ToPolygon` and `cellsToGeoJson` no longer says a pentagon has five vertices, which holds at even resolutions only.

### Changed

- **Documentation:** every module's full documentation is now in its section of the knowledge base, `docs/knowledge/<module>/`, mirrored to the repository's wiki: each feature in detail, platform notes, deployment, troubleshooting and the decisions behind it. A module's README keeps install, a first use and links to those pages.
- **`jsonlogic`**: its agent skill is now an [Agent Skill](https://agentskills.io/specification) named `com-oddlyclever-jsonlogic`, shipped in every sources jar at `commonMain/skills/com-oddlyclever-jsonlogic/`. The old `META-INF/ai-skills/` file in the JVM jar is gone.
- **`wayfinder`**: the map's ornaments are wayfinder's own and it no longer depends on map-view-controls, which map-view plans to stop publishing. They are a copy of map-view-controls 0.3.0 and look and behave the same. **Source-incompatible:** `ScaleUnits` moves to `com.oddlyclever.wayfinder`, so `import com.coderwise.libs.mapview.controls.ScaleUnits` becomes `import com.oddlyclever.wayfinder.ScaleUnits`. An app that called map-view-controls itself, such as `ZoomButtons`, `CompassTape` or `turnTo`, was getting it through wayfinder and now has to depend on `com.coderwise.libs:map-view-controls:0.3.0` directly, or use `MapOrnaments`, `MapCompassTape` and `OrientationToggle`.
- **`wayfinder`**: the scale bar writes its distance in the device's locale, with that locale's digit grouping and decimal separator, through aughtone's `format-readable`, which wayfinder now depends on, with `aughtone-types`. In English a bar of a thousand feet now reads `1,000 ft`, and a bar shorter than a metre is in millimetres.
- **Building from source:** the Kotlin compiler gets 6 GB in `gradle.properties`, up from 3 GB, which ran out of memory linking the wayfinder modules' JS and Wasm test bundles.

## [0.0.6] - 2026-11-01

### Added

- **`jsonlogic`**: a clean-room [JSONLogic](https://jsonlogic.com) rule evaluator in pure common Kotlin, with no dependencies beyond the standard library. Rules and data are plain maps, lists and values, so the caller chooses how JSON becomes them, and a rule gives the same result on every target.

## [0.0.5] - 2026-10-29

No changes to the libraries: the tests moved to a self-hosted Mac.

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

[Unreleased]: https://github.com/oddlyclever/capabilities/compare/v0.0.6...develop
[0.0.6]: https://github.com/oddlyclever/capabilities/releases/tag/v0.0.6
[0.0.5]: https://github.com/oddlyclever/capabilities/releases/tag/v0.0.5
[0.0.4]: https://github.com/oddlyclever/capabilities/releases/tag/v0.0.4
[0.0.3]: https://github.com/oddlyclever/capabilities/releases/tag/v0.0.3
