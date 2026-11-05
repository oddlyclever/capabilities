# Cells, Coordinates and GeoJSON

DOC-0004 · 2026-11-04
Keywords: H3 index string, canonical H3 form, leading zeros, zero-padded index, uppercase hex index, Long index, H3Core, latLngToCell, cellToBoundary, geoToH3, h3ToGeo, kRing, hex ring size, gridDisk not 7 cells, pentagon neighbours, boundary vertex count, seven vertices, ten vertices, polygon not closed, counter-clockwise, rounding coordinates, coordinates differ between platforms, lat lng swapped, GeoJSON lng lat order, antimeridian band across the map, FeatureCollection text, merge cells into one outline, polyfill, compact, cell area

What the values going in and out of `H3SpatialService` look like, and how each call behaves at the edges. The KDoc on [`H3SpatialService`](../../../h3spatial/src/commonMain/kotlin/com/oddlyclever/h3spatial/H3SpatialService.kt) and [`H3GeoJson.kt`](../../../h3spatial/src/commonMain/kotlin/com/oddlyclever/h3spatial/H3GeoJson.kt) is the API reference. What a call refuses, and with which code, is in [Errors and What Each Call Refuses](DOC-0005-errors-and-what-each-call-refuses.md).

## Cells are canonical strings

Every function takes and returns cells as `String`. A returned cell is always in canonical H3 form: lowercase hex with no leading zeros, the form the H3 C library, `h3-js` and `com.uber:h3` all produce. A cell computed here compares equal to one from any standard H3 tool, and to one computed by this library on another target.

Input is more forgiving than output. Upper-case hex digits are accepted, and so is the zero-padded 16-digit form. Only hex digits are accepted: a sign or a `0x` prefix is rejected, because each platform's own hex parser tolerates a different set of variants and the same text has to mean the same cell everywhere.

Input is accepted but not rewritten. A result is canonical even when the input was not, so `h3.gridDisk(input, 1).contains(input)` is `false` when `input` is `"084754A9FFFFFFFF"`. Canonicalise an index from outside once before using it as a map key or comparing it, for example with `h3.cellToParent(index, h3.getResolution(index))`. An index held as a `Long`, as `com.uber:h3` returns it, becomes the canonical string with `index.toString(16)`.

There is no `H3Core` and no `Long` index in this API.

## Coordinates are degrees, rounded

Points go in as `lat`, `lng` in degrees and come back as `H3LatLng(lat, lng)` in degrees. Returned coordinates are rounded to eight decimal places, about a millimetre. Each platform's H3 computes them in its own floating-point environment, and the rounding is what makes a centre or a boundary computed on one target compare equal with `==` to the same one computed on another, instead of differing in the last few bits.

Latitude comes first: `latLngToH3(lat, lng, resolution)`. GeoJSON and many map APIs order a position `[lng, lat]`. A swapped pair whose longitude happens to lie within ±90 is accepted and gives a valid cell somewhere else entirely, with no error. Pass named arguments when the values come from a `[lng, lat]` source.

Latitude must be within -90..90 and longitude within -180..180. Nothing is wrapped or normalised: a longitude of `190.0` from a map scrolled past the antimeridian throws, and has to be wrapped into range by the caller first.

## Resolution is always an argument

Resolutions run from 0, the coarsest, to 15, the finest. No call has a default resolution: the right cell size depends entirely on what is being bucketed, so the caller always chooses. `getResolution(cell)` reads a cell's resolution back.

## Traversal

`gridDisk(cell, k)` returns every cell within grid distance `k`, including the cell itself. A full disk has `3k(k+1)+1` cells, but each resolution has twelve pentagons, and a disk that touches one is smaller: a pentagon has five neighbours, not six, so a pentagon's `k = 1` disk is six cells. Never assume seven cells at `k = 1`. The order of the result is whatever the platform's H3 returns and is not part of the contract; sort before comparing two disks.

`gridDistance(a, b)` is the distance between two same-resolution cells in cells, which gives proximity without projecting to metres. `areNeighborCells(a, b)` says whether two same-resolution cells border each other; a cell is not its own neighbour. Both need the two cells at one resolution.

## Hierarchy

`cellToParent(cell, res)` is the coarser cell containing this one; at the cell's own resolution it returns the cell itself, in canonical form. `cellToChildren(cell, res)` lists the finer cells, `7^n` of them for a hexagon `n` resolutions finer. It lists at most `H3SpatialService.MAX_CHILD_RESOLUTION_STEPS`, 11, resolutions finer: `7^11` fits in an `Int`, `7^12` does not, and no target could return a list that size.

## Boundaries

`h3ToPolygon(cell)` returns the cell's boundary as H3 orders it: counter-clockwise, and not closed, so the first vertex is not repeated at the end. A hexagon has six vertices and a pentagon five at even resolutions. At odd resolutions H3 adds a vertex wherever an edge crosses an icosahedron face, so a hexagon can have seven or eight and a pentagon ten. Size buffers and drawing code from the list, not from `isPentagon`.

`cellToLatLng(cell)` is the cell's centre, the inverse of `latLngToH3`.

Nothing is split at the antimeridian. A cell that straddles longitude 180 has boundary longitudes near both `+180` and `-180` in one ring, which many renderers draw as a band across the whole map. Split or shift such rings before drawing them.

## GeoJSON

`cellsToGeoJson(cells)` returns a GeoJSON `FeatureCollection` as a `String`, with one `Polygon` feature per cell and the cell's canonical index in `properties.h3`, even when the input was zero-padded or upper case. Positions follow RFC 7946: `[lng, lat]` order, and a closed ring, the first position repeated last. Vertex order is as H3 emits the boundary, so a ring has as many positions as `h3ToPolygon` gives, plus the closing one. An empty list gives a collection with empty `features`. `cellToGeoJson(cell)` is the same for one cell.

It is text rather than a model so the library carries no GeoJSON dependency. Anything that parses GeoJSON can draw it; on a wayfinder map, [`wayfinder-geojson`](../wayfinder-geojson/README.md) takes GeoJSON text.

Numbers are written with each platform's own `Double.toString`, so the same cells can produce different text on different targets. Compare parsed coordinates, not strings.

`cellsToGeoJson` draws one ring per cell. It does not merge a set of cells into one outline.

## What is not here

The service indexes points and cells. It does not fill a polygon with cells, compact or uncompact a cell set, compute a cell's area or edge length in metres, list a ring or a path between cells, or expose H3's edges, vertices or local IJ coordinates. It stores, queries and draws nothing itself. Every target's H3 already has these calls, so adding one is a change to the service alone; [Adding a Function and Verifying It](DOC-0008-adding-a-function-and-verifying-it.md) says how.

Two calls are named differently from H3 4: the cell for a point is `latLngToH3`, not `latLngToCell`, and a cell's boundary is `h3ToPolygon`, not `cellToBoundary`. The rest keep H3 4's names. H3 3's names, such as `geoToH3`, `h3ToGeo`, `kRing` and `h3Distance`, do not exist, and the point type is `H3LatLng`, not `LatLng` or `GeoCoord`.
