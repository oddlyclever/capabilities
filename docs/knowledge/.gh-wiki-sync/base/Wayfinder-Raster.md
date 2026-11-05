# wayfinder-raster

The full documentation of [`wayfinder-raster`](https://github.com/oddlyclever/capabilities/blob/HEAD/wayfinder-raster/README.md): raster basemaps for wayfinder, decoded off the main thread and faded in as they arrive. Its README covers installing it and a first use, and its KDoc is the API reference; this section covers the rest.

## Using it

- [Raster Basemaps](Wayfinder-Raster-Raster-Basemaps): `rasterBasemap`'s parameters, the source's zoom range and row order, stacking layers, the fade, keeping tiles across recompositions, and memory.
- [Missing and Broken Tiles](Wayfinder-Raster-Missing-And-Broken-Tiles): what counts as a missing tile, the formats each platform decodes, what the map does about a failure, and why a map comes out blank.
- [Raster Tiles Outside a WayfinderMap](Wayfinder-Raster-Raster-Tiles-Outside-A-Wayfindermap): `decodeRasterTile` and `FadingRasterSlot` for a map-view layer of your own, and `rasterFrameLayer` for `renderMap` and `MapFrame`.

## Why it is built this way

- [Decode Raster Tiles Upstream and Treat Failure as a Missing Tile](Wayfinder-Raster-Decode-Raster-Tiles-Upstream-And-Treat-Failure-As-A-Missing-Tile): why a tile is decoded once off the main thread, and why a bad tile is never an error.

A new page about this module alone goes here, numbered like every other document in the knowledge base. Knowledge that spans several modules goes in the shared sections instead.
