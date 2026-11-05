# wayfinder-vector

The full documentation of [`wayfinder-vector`](../../../wayfinder-vector/README.md): vector basemaps for wayfinder, light and dark styles for map-view's vector renderer, and vector tiles drawn to images on devices whose GPU driver cannot be trusted with their paths. Its README covers installing it and a first use, and its KDoc is the API reference; this section covers the rest.

## Using it

- [Vector Basemaps](DOC-0041-vector-basemaps.md): `vectorBasemap`'s parameters, what happens to each tile, the OpenMapTiles schema, the bytes a tile must be, keeping tiles across recompositions, and memory.
- [Light, Dark and Custom Styles](DOC-0042-light-dark-and-custom-styles.md): `MapStyles`, following the system's dark mode, a palette of your own, what recolouring keeps, turning names off, and footpaths, tunnels and bridges.
- [Drawing Tiles as Images](DOC-0043-drawing-tiles-as-images.md): `rasterize` and `rasterizeVectorTile`, when to use them, and what they cost.
- [Vector Tiles on a Map Frame](DOC-0044-vector-tiles-on-a-map-frame.md): `vectorFrameLayer` for `renderMap` and `MapFrame`, with names from a `TextMeasurer`.

## Why it is built this way

- [GPU Paths Everywhere, Images Only When the App Asks](ADR-0020-gpu-paths-everywhere-images-only-when-the-app-asks.md): why there is no device list, and why names are never drawn into a tile's image.
- [A Dark Map as a Recolouring of map-view's Style](ADR-0021-a-dark-map-as-a-recolouring-of-map-views-style.md): why a palette changes colour and nothing else.

A new page about this module alone goes here, numbered like every other document in the knowledge base. Knowledge that spans several modules goes in the shared sections instead.
