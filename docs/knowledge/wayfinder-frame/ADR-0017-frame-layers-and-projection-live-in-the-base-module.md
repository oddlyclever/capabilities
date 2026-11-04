# Frame Layers and Projection Live in the Base Module

ADR-0017 · 2026-11-04 · Status: accepted
Keywords: why is MapFrameLayer in wayfinder, why does wayfinder-frame not depend on wayfinder-vector, renderer depends on vector and raster, pull in the vector renderer for raster images, MapProjection, drawTrack on a live map and an image, where do frame layers come from, vectorFrameLayer, rasterFrameLayer, plug in a custom layer, names drawn in a second pass

## Context

A map frame draws tiles from vector or raster sources, and an overlay such as a recorded track over them. The code that decodes vector tiles lives in `wayfinder-vector` and the code that decodes images in `wayfinder-raster`. If the renderer in `wayfinder-frame` depended on both, an app that only draws raster images to a file would pull in the vector renderer too. In the same way, what draws over a map, a track or a location marker, has to draw the same on a live map and on an image, and neither the live map nor the frame should depend on the other to share it.

## Decision

The interfaces a frame is built from are in `wayfinder`, the base module, not in `wayfinder-frame`.

- **`MapFrameLayer` and `MapFrameTile` are in `com.oddlyclever.wayfinder`.** `wayfinder-vector` supplies `vectorFrameLayer` and `wayfinder-raster` supplies `rasterFrameLayer`, each implementing them. `wayfinder-frame` depends on `wayfinder` and `media-webp` only, and draws whatever layers it is given.
- **`MapProjection` is in `com.oddlyclever.wayfinder` too.** A live map's overlay gives one, and `MapFrameProjection` implements it, so `drawTrack` and other overlay code take a `MapProjection` and draw on both.
- **A tile draws in two passes,** its ground and then its names, and the renderer draws every tile's ground before any tile's names. A name can run past the edge of its own tile, and drawn with its ground it would be covered by the neighbour drawn after it.

## Consequences

- An app takes only the layer module it draws, and a frame of raster tiles carries no vector code.
- Any module, including the app, can supply a layer by implementing `MapFrameLayer`, with no change to `wayfinder-frame`.
- A track or marker is written once against `MapProjection` and drawn on a live map and on an image alike.
- A live map's layer and a frame's layer are different types: `vectorBasemap` and `rasterBasemap` return a `MapLayer`, which a frame does not take. A screen that shows the same tiles live and as an image builds both.
- `wayfinder-frame` has no layer of its own to offer, so every example of it needs one of the tile modules as well.
