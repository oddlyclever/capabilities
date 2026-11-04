---
name: com-oddlyclever-h3spatial
description: >-
  Put latitude/longitude points into Uber H3 hexagonal grid cells in Kotlin
  Multiplatform, with the same cell IDs on the JVM, Android, iOS, JS and Wasm.
  Use to bucket or bin locations into hexagons, as a spatial index or
  geohash-style area key, for heatmaps, density maps and counts per area, to
  find nearby cells and neighbours (k-ring, grid disk), measure grid distance
  between cells, roll cells up to a coarser parent or down to their children
  for zoom levels, get a cell's centre point or boundary polygon, check whether
  an H3 index string is valid, and draw cells on a map as GeoJSON polygons.
  Cells are plain canonical H3 hex strings that match any other H3 tool, and
  every invalid input throws one exception type, with H3's error code, on
  every platform.
license: Apache-2.0
metadata:
  version: "0.0.7"
  repository: https://github.com/oddlyclever/capabilities
---

# h3spatial

`com.oddlyclever:h3spatial`, package `com.oddlyclever.h3spatial`. Called from Kotlin only: there is no Swift or JavaScript surface of its own.

## What it solves

Grouping locations into areas: counting events per neighbourhood for a heatmap, keying records by the area they fall in, finding what is near a point without computing distances, or coarsening a set of areas as a map zooms out. It uses Uber's H3 grid, so a cell computed here is the same cell any other H3 tool computes for that point. Each target calls Uber's own H3 rather than a reimplementation (`com.uber:h3` on the JVM, `com.uber:h3-android` on Android, the H3 C library compiled into the iOS artifacts, `h3-js` on JS and Wasm), all of one pinned H3 release, and every target is tested against H3's own reference data, so a phone and a server put a point in the same cell and report the same centre.

## How it is meant to be used

```kotlin
import com.oddlyclever.h3spatial.H3LatLng
import com.oddlyclever.h3spatial.H3SpatialService
import com.oddlyclever.h3spatial.cellsToGeoJson

val h3 = H3SpatialService()

val cell: String = h3.latLngToH3(lat = 45.4215, lng = -75.6972, resolution = 7) // "872b83b0effffff"
val nearby: List<String> = h3.gridDisk(cell, k = 1)          // the cell and the ring around it
val district: String = h3.cellToParent(cell, parentResolution = 5)
val centre: H3LatLng = h3.cellToLatLng(cell)                  // degrees
val outline: List<H3LatLng> = h3.h3ToPolygon(cell)            // the boundary, not closed
val geoJson: String = h3.cellsToGeoJson(nearby)               // a FeatureCollection, as text
```

`H3SpatialService` holds no state and takes no configuration: construct it where it is needed, or bind it in your own container as a factory. No Koin module ships, and none is needed. Every function takes and returns cells as `String`, and the resolution, 0 (coarsest) to 15 (finest), is always an argument, never a default. The other calls are `getResolution`, `isPentagon`, `gridDistance`, `areNeighborCells`, `cellToChildren` and `cellToGeoJson` for one cell.

Coordinates come back as `H3LatLng(lat, lng)` in degrees, rounded to eight decimal places, so a centre or boundary computed on one target compares equal with `==` to the same one computed on another.

`isValidCell` is the one call that never throws for bad input; use it on an index from outside before passing it on. Every other call throws `H3SpatialException` for invalid input, the same inputs on every target, and `code` carries H3's own error code: `2` for a bad `k`, `3` for coordinates out of range, `4` for a resolution out of range or the wrong way round, `5` for an invalid cell, `12` for cells at different resolutions, `14` for a result too large to list.

`cellsToGeoJson` returns a `FeatureCollection` with one `Polygon` per cell, its ring closed, positions in GeoJSON's `[lng, lat]` order, and the cell's canonical index in `properties.h3`. It is text so that any GeoJSON reader can take it; to draw it on a wayfinder map, `com.oddlyclever:wayfinder-geojson`, package `com.oddlyclever.wayfinder.geojson`, takes GeoJSON text.

## Invariants and traps

**Latitude comes first.** `latLngToH3(lat, lng, resolution)`, while GeoJSON and many map APIs order a position `[lng, lat]`. A swapped pair whose longitude happens to lie within ±90 is accepted and gives a valid cell somewhere else entirely, with no error. Pass named arguments when the values come from a `[lng, lat]` source.

**Out-of-range coordinates throw; nothing wraps.** Latitude must be within -90..90 and longitude within -180..180, or the call throws `H3SpatialException` with code `3`, where H3 itself would quietly normalise. A longitude from a map that has scrolled past the antimeridian, such as `190.0`, has to be wrapped into range first. `NaN` throws the same way.

**Compare canonical strings.** Results are always lowercase hex without leading zeros. Input is also accepted in upper case or zero-padded to 16 digits, but is not rewritten, so `h3.gridDisk(input, 1).contains(input)` is `false` when `input` is `"084754A9FFFFFFFF"`. Canonicalise an index from outside once, for example with `h3.cellToParent(index, h3.getResolution(index))`, before using it as a map key or comparing it. A `0x` prefix or a sign is rejected. An index held as a `Long`, as `com.uber:h3` returns it, becomes the canonical string with `index.toString(16)`.

**A disk is not always seven cells.** `gridDisk(cell, k)` holds `3k(k+1)+1` cells only when no pentagon is nearby; each resolution has twelve pentagons, and a pentagon's `k = 1` disk is six cells. Its order is not part of the contract: sort before comparing two disks. A negative `k`, or one whose disk could not fit in a list, throws with code `2`.

**A boundary is not always six vertices.** H3 adds vertices where a cell crosses an icosahedron edge, so at odd resolutions a hexagon's boundary can have seven or eight vertices and a pentagon's ten; only at even resolutions is a pentagon's boundary five. Size buffers and draw code from the list, not from `isPentagon`. `h3ToPolygon` returns the ring unclosed; `cellsToGeoJson` closes it.

**Nothing is split at the antimeridian.** A cell that straddles longitude 180 has boundary longitudes near both `+180` and `-180` in one ring, which many renderers draw as a band across the whole map. Split or shift such rings before drawing them. The GeoJSON text writes numbers with each platform's `Double.toString`, so the same cells can produce different text on different targets; compare parsed coordinates, not strings.

**Two-cell calls need one resolution.** `gridDistance` and `areNeighborCells` throw with code `12` for cells at different resolutions, and a cell is not its own neighbour. `cellToParent` with a finer resolution, or `cellToChildren` with a coarser one, throws with code `4`. `cellToChildren` lists at most `H3SpatialService.MAX_CHILD_RESOLUTION_STEPS` (11) resolutions finer, since a hexagon has `7^n` children; deeper throws with code `14`. `gridDistance` can still throw for two valid cells at one resolution that H3 cannot measure between, such as across a pentagon's distortion.

**A native library that fails to load is an `Error`, not an `H3SpatialException`.** Android ships H3's native code for `arm64-v8a` and `armeabi-v7a` only, so on an x86 or x86_64 emulator, and in an Android unit test on the host, the first call throws `UnsatisfiedLinkError`. On the JVM a native library that cannot be extracted throws `LinkageError` ("Could not load the H3 native library"). `isValidCell` lets these through too. `catch (e: H3SpatialException)` does not hide them, by design; do not widen the catch to `Throwable` to make a broken runtime look like a bad index.

**Your own functions exported to Swift need `@Throws`.** Every throwing call here is annotated `@Throws(H3SpatialException::class)`. If a function in your shared module calls one and lets the exception escape to Swift, it needs the same annotation, or invalid input terminates the app instead of arriving as a Swift error.

## What moved, and what it used to be called

`h3spatial` was first released in 0.0.3, and its API has not changed since.

The likely wrong guess is the API of `com.uber:h3`, which it wraps. There is no `H3Core` here and no `Long` index: cells are `String`s on every target. Two calls are named differently from H3 4: the cell for a point is `latLngToH3`, not `latLngToCell` or `latLngToCellAddress`, and a cell's boundary is `h3ToPolygon`, not `cellToBoundary`. The rest keep H3 4's names: `cellToLatLng`, `gridDisk`, `gridDistance`, `areNeighborCells`, `cellToParent`, `cellToChildren`, `isValidCell`, `isPentagon` and `getResolution`. H3 3's names, such as `geoToH3`, `h3ToGeo`, `kRing` and `h3Distance`, do not exist. The point type is `H3LatLng`, not `LatLng` or `GeoCoord`, and the service is constructed with `H3SpatialService()`, not reached through an `instance` or `newInstance()`.

## What it is not for

It indexes points and cells; it does not fill a polygon with cells, compact or uncompact a cell set, compute a cell's area or edge length in metres, list a ring or a path between cells, or expose H3's edges, vertices or local IJ coordinates. It does not merge cells into one outline: `cellsToGeoJson` gives one polygon per cell. It stores, queries and draws nothing itself.
