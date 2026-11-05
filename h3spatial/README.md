# H3 Spatial

Uber [H3](https://github.com/uber/h3) hexagonal grid indexing for Kotlin Multiplatform, behind one `H3SpatialService` with a `String` cell vocabulary that is identical on every target.

`com.oddlyclever:h3spatial`

## What it does

| Function | Use |
|---|---|
| `latLngToH3(lat, lng, resolution)` | The cell containing a point. |
| `cellToLatLng(cell)` / `h3ToPolygon(cell)` | A cell's centre, or its boundary ring. |
| `getResolution(cell)` · `isValidCell(cell)` · `isPentagon(cell)` | Inspection. `isValidCell` is the one function that never throws. |
| `gridDisk(cell, k)` · `gridDistance(a, b)` · `areNeighborCells(a, b)` | Grid traversal. |
| `cellToParent(cell, res)` · `cellToChildren(cell, res)` | Hierarchy. |
| `cellsToGeoJson(cells)` · `cellToGeoJson(cell)` | A GeoJSON `FeatureCollection`, as text, one polygon per cell. |

Cells are canonical H3 strings, so a cell computed here compares equal to one from any standard H3 tool and to one computed on another platform. Coordinates are degrees, rounded to eight decimal places so they compare equal across targets. Every function except `isValidCell` throws `H3SpatialException`, carrying H3's own error code, on invalid input, the same inputs on every target. The resolution is always the caller's choice.

## Install

Add it to the `commonMain` source set of the consuming module:

```kotlin
kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation("com.oddlyclever:h3spatial:0.0.7")
        }
    }
}
```

**Targets:** `jvm` · `android` · `js` (browser) · `wasmJs` (browser) · `iosArm64` · `iosSimulatorArm64`.

Every target binds Uber's own H3, all of one pinned release, and none needs setup: `com.uber:h3` on the JVM, `com.uber:h3-android` on Android, the H3 C library linked into the iOS artifacts, and `h3-js` on the web, all arriving with the dependency. On Android, H3's native code is ARM only, so it does not load on an x86 or x86_64 emulator. [Platform Notes](../docs/knowledge/h3spatial/DOC-0006-platform-notes.md) has what each target needs to know.

## Use

```kotlin
import com.oddlyclever.h3spatial.H3LatLng
import com.oddlyclever.h3spatial.H3SpatialService
import com.oddlyclever.h3spatial.cellsToGeoJson

val h3 = H3SpatialService()

val cell: String = h3.latLngToH3(lat = 45.4215, lng = -75.6972, resolution = 7) // "872b83b0effffff"
val nearby: List<String> = h3.gridDisk(cell, k = 1)       // the cell and the ring around it
val district: String = h3.cellToParent(cell, parentResolution = 5)
val centre: H3LatLng = h3.cellToLatLng(cell)               // degrees
val outline: List<H3LatLng> = h3.h3ToPolygon(cell)         // the boundary, not closed
val geoJson: String = h3.cellsToGeoJson(nearby)            // a FeatureCollection, as text
```

**Koin is not required, and none ships.** `H3SpatialService` takes no arguments and holds no state: call `H3SpatialService()` wherever it is needed, or bind it in your own container as a factory, `factory { H3SpatialService() }`.

## Documentation

The full documentation is in the module's section of the [knowledge base](../docs/knowledge/h3spatial/README.md), which is mirrored to the repository's wiki:

- [Cells, Coordinates and GeoJSON](../docs/knowledge/h3spatial/DOC-0004-cells-coordinates-and-geojson.md): cell strings, coordinates, traversal, hierarchy, boundaries and the GeoJSON output, at the edges.
- [Errors and What Each Call Refuses](../docs/knowledge/h3spatial/DOC-0005-errors-and-what-each-call-refuses.md): `H3SpatialException`, its codes, and the failures that are not one.
- [Platform Notes](../docs/knowledge/h3spatial/DOC-0006-platform-notes.md): what backs each target, Android ABIs and keep rules, iOS and Swift, the web and the JVM.
- [Keeping Every Target on One H3 Release](../docs/knowledge/h3spatial/DOC-0007-keeping-every-target-on-one-h3-release.md): for maintainers, the version pin and how to update the vendored C.
- [Adding a Function and Verifying It](../docs/knowledge/h3spatial/DOC-0008-adding-a-function-and-verifying-it.md): for maintainers, adding a call and the tests that run on every target.
- [Uber's Own H3 on Every Target, at One Release](../docs/knowledge/h3spatial/ADR-0002-ubers-own-h3-on-every-target-at-one-release.md) and [One Cell Vocabulary and One Failure Type on Every Target](../docs/knowledge/h3spatial/ADR-0003-one-cell-vocabulary-and-one-failure-type-on-every-target.md): why the module is built the way it is.

## For coding agents

The module ships an [Agent Skill](https://agentskills.io/specification), `com-oddlyclever-h3spatial`, for the agents of projects that depend on it: what the library is for, how it's meant to be called, and the mistakes that compile. It is written at [src/commonMain/skills/com-oddlyclever-h3spatial/SKILL.md](src/commonMain/skills/com-oddlyclever-h3spatial/SKILL.md) and published inside every sources jar, at `commonMain/skills/com-oddlyclever-h3spatial/`.

## Third-party code

The vendored C library is Uber's H3, unmodified, under the Apache License 2.0, and the agreement fixtures are derived from H3's own test data. The JVM, Android and web bindings are Uber's published packages, used as dependencies. See `NOTICE`, which is also included under `META-INF` in every published jar.

**An app that ships this library ships Uber's H3 too** — linked from the iOS static library, or from `com.uber:h3`, `com.uber:h3-android` or `h3-js`. The Apache License asks that app to include the license text and Uber's attribution (`Copyright 2017-2021 Uber Technologies, Inc.`) wherever it lists its third-party notices.

## License

Apache License 2.0. Maintained at [github.com/bpappin](https://github.com/bpappin).
