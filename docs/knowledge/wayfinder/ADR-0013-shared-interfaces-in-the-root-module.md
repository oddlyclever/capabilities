# Shared Interfaces in the Root Module

ADR-0013 · 2026-11-04 · Status: accepted
Keywords: why is wayfinder split into modules, why does wayfinder-frame not depend on wayfinder-vector, one big map library, plugin modules, MapLayer, PlacedLayer, MapFrameLayer, MapFrameTile, MapProjection, drawTrack on an image, track module, module dependency graph, pulling in the vector renderer

## Context

The wayfinder family draws maps in two ways, live in a composition and to an image without one, from two kinds of tile, vector and raster, with things drawn over them such as tracks and GeoJSON. An app wants some of these and not others: one that only draws raster images to a file has no use for the vector renderer. If the module that renders images depended on the vector and raster modules to get their tiles, or the module that draws tracks depended on the one that renders images, every app would carry all of them.

## Decision

The root module, `wayfinder`, holds the interfaces the other modules meet at, and each extension depends on the root and adds its own dependency and nothing else.

- **`MapLayer` and `PlacedLayer`** are how a module puts tiles on a live map. `WayfinderMap` takes any list of them.
- **`MapFrameLayer` and `MapFrameTile`** are how a module puts tiles on an image. The vector and raster modules each supply one, and `wayfinder-frame`'s renderer draws any of them without depending on either.
- **`MapProjection`** is where a coordinate falls on a map, live or an image. What draws over a map, a track or the user's position, draws through it, so it draws the same way on both without the live-map code and the image code depending on each other.

## Consequences

- An app takes the root and only the extensions it uses. An app that draws only raster images never pulls in the vector renderer.
- A new kind of tile, or a new thing drawn over a map, is a new module against the root's interfaces, with no change to the modules beside it.
- The root carries interfaces that it does not itself use for drawing, such as `MapFrameLayer`, and changing one of them changes every module that implements it.
- `wayfinder-headless` is the one module that depends on several siblings, since its single call has to draw both kinds of tile into an image.
