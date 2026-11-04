# wayfinder-raster

The full documentation of [`wayfinder-raster`](../../../wayfinder-raster/README.md): raster basemaps for wayfinder, decoded off the main thread and faded in as they arrive. Its README covers installing it and a first use, and its KDoc is the API reference; this section covers the rest.

## Using it

- [Raster Basemaps](DOC-0038-raster-basemaps.md): `rasterBasemap`'s parameters, the source's zoom range and row order, stacking layers, the fade, keeping tiles across recompositions, and memory.
- [Missing and Broken Tiles](DOC-0039-missing-and-broken-tiles.md): what counts as a missing tile, the formats each platform decodes, what the map does about a failure, and why a map comes out blank.
- [Raster Tiles Outside a WayfinderMap](DOC-0040-raster-tiles-outside-a-wayfindermap.md): `decodeRasterTile` and `FadingRasterSlot` for a map-view layer of your own, and `rasterFrameLayer` for `renderMap` and `MapFrame`.

## Why it is built this way

- [Decode Raster Tiles Upstream and Treat Failure as a Missing Tile](ADR-0019-decode-raster-tiles-upstream-and-treat-failure-as-a-missing-tile.md): why a tile is decoded once off the main thread, and why a bad tile is never an error.

A new page about this module alone goes here, numbered like every other document in the knowledge base. Knowledge that spans several modules goes in the shared sections instead.
