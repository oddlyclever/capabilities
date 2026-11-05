# Maintaining wayfinder

DOC-0030 · 2026-11-04
Keywords: upgrade map-view, map-view version, mapView in libs.versions.toml, breaking change from map-view, Web Mercator copy, projection mismatch, binaries.executable, CMP-4906, webpack browser tests, Skiko in browser tests, awaitSkia, missing function in JS test, KLIB uniqueName clash, KT-66568, out of memory linking JS test bundle, kotlin.daemon.jvmargs, Compose UI test for ornaments, agent skill in sources jar, Java 21 toolchain

Notes for working on the module itself: what it is pinned to, what it copies, and why its build is shaped the way it is. Using the module is covered by the other pages in this section.

## map-view

`mapView` in `gradle/libs.versions.toml` is the one version of map-view, currently 0.3.0. map-view's artifacts are released in lockstep, so the one version covers every artifact of it the wayfinder modules use. wayfinder follows every map-view release.

map-view is 0.x, and wayfinder exposes `map-view` and `map-view-tiles` as `api`, so their types are part of wayfinder's public API. A map-view release that changes them changes wayfinder's API, and the changelog entry for that release has to say so.

The ornaments in `com.oddlyclever.wayfinder.controls` are a copy of map-view-controls 0.3.0, Apache-2.0, taken when map-view planned to stop publishing it. Each file says so in its header and names what changed. They are wayfinder's to maintain now, and a fix to them in map-view does not reach wayfinder unless it is copied across.

Java 21 is the version of every module in the repository, set by `javaVersion` in the catalog, because map-view's JVM classes are Java 21: an app using the map modules runs on 21 anyway.

## The copy of Web Mercator

`WebMercator`, internal to the module, is a copy of the projection map-view lays its map out on: x and y in `[0, 1)`, origin at the top left, and a tile 256 dp across at zoom 0. Marker clustering and `visibleBounds` measure on it. It is a copy because map-view keeps its own internal.

It must agree with map-view's to the last digit. `CameraTest` pins that with a round trip through `MapCameraState`, which stores its centre in map-view's own projection. If map-view ever exposes its projection, the copy goes.

## The camera-limits workaround

`CameraLimits` holds the camera by moving it back after it leaves, because map-view holds a camera to its own `ZOOM_LIMITS` and nothing else, and its gestures move the camera directly. Holding it inside in the first place has to happen where map-view moves it, and is asked of map-view in [coderwise/libs-mobile#5](https://github.com/coderwise/libs-mobile/issues/5). When that lands, `HoldCamera` can go. See [Camera Limits Corrected After the Move](ADR-0015-camera-limits-corrected-after-the-move.md).

## The build

- **`binaries.executable()` on both web targets.** Since Compose 1.12, a browser test that has Skiko on its classpath must be bundled by webpack to load it, and the build refuses to run without one (JetBrains' CMP-4906). Nothing published changes: the klibs and Gradle metadata are the same with and without it.
- **Skia in the JS tests loads late.** On JS, Skia's WebAssembly module loads in the background after the page starts, so a test that decodes or draws before then fails on a missing function. The track tests call `awaitSkia()` first; on every other target it returns at once.
- **The KLIB unique-name workaround.** The `metadata { }` block gives the common metadata compilation a module name made of the group, the project name and the root project's name, to avoid KLIB `uniqueName` clashes between modules of the same repository (Kotlin's KT-66568).
- **Memory for the web test bundles.** The Kotlin compiler gets 6 GB in `gradle.properties`, since 3 GB ran out of memory linking the wayfinder modules' JS and Wasm test bundles.

## Tests

Common behaviour is pinned in `commonTest`, which runs on every target: camera fitting and the screen position, camera limits, the map state's bounds and saver, marker clustering, tile sources, and tracks. `jvmTest` adds Compose UI tests of the ornaments, such as the attribution's collapse, on the desktop with Skia's native library for the machine running them.

`./gradlew :wayfinder:check` compiles every target and runs them all.

## The agent skill

The module's agent skill is at `src/commonMain/skills/com-oddlyclever-wayfinder/SKILL.md`. The build copies it into every sources jar, at `commonMain/skills/com-oddlyclever-wayfinder/`, where a consumer's tooling reads it.
