# Decode Raster Tiles Upstream and Treat Failure as a Missing Tile

ADR-0019 · 2026-11-04 · Status: accepted
Keywords: why decode off the main thread, decode in the slot, jank when panning, frame drops decoding tiles, why errors are not reported, tile error callback, onTileError, exception for a bad tile, error page crashes the map, missing tile instead of crash, decodeRasterTile returns null

## Context

A raster tile arrives as bytes and has to become an image before it can be drawn. Decoding a 256-pixel tile takes long enough to cost a frame on the main thread, and a slot that decoded its own tile would decode it again whenever it wanted it. Tile servers also answer with things that are not images: an HTML error page, a truncated download, an empty body.

## Decision

- **A tile is decoded once, as it arrives, upstream of the view.** `rasterBasemap` decodes in its tile state's `load`, and `decodeRasterTile` is that step on its own, for code that builds its own layer.
- **Decoding runs on `Dispatchers.Default`, whatever thread calls it,** so it never costs a frame on the main thread.
- **Anything that does not decode is a missing tile, not a crash.** `decodeRasterTile` returns null for no bytes, empty bytes, and bytes no decoder on the platform can read, and the map shows the nearest ancestor instead.

## Consequences

- Panning does not stutter on decoding, and a tile already loaded is not decoded again when it is drawn again.
- A server that answers with an error page or a truncated image costs one missing tile, never the app.
- Nothing that goes wrong is reported. A blank or blurry map means the client is failing, and the place to find out why is inside the client. [Missing and Broken Tiles](DOC-0039-missing-and-broken-tiles.md) lists the causes.
- Decoded images are held in memory, up to 96 a layer, rather than their bytes. [Raster Basemaps](DOC-0038-raster-basemaps.md) says what that costs.
