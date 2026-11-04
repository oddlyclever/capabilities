# Camera Limits Corrected After the Move

ADR-0015 · 2026-11-04 · Status: accepted
Keywords: camera limits overshoot, map goes past the edge for a frame, map bounces back, restrict panning, max bounds, bounding box for the map, min zoom, max zoom, CameraLimits, HoldCamera, ZOOM_LIMITS, map-view gestures, MapLibre bounds, limit the visible area

## Context

An app often needs to keep a map to one place, such as a town, or to a range of zoom. map-view, which wayfinder is built on, holds every camera to its own `ZOOM_LIMITS`, 1 to 20, and to nothing else, and its gestures move the camera directly. wayfinder has no way into those gestures to stop a move before it happens.

## Decision

`CameraLimits` is held by correction: whenever the camera moves outside the limits, by a gesture or by a move on the state, or the limits change around it, wayfinder moves it back to the nearest point inside them.

- **The zoom is kept in a range**, narrowed to map-view's own, since map-view cannot go outside its range anyway.
- **The box limits where the middle of the map goes, not what the screen shows**, as MapLibre's bounds do.
- **Holding the camera inside in the first place is asked of map-view**, in [coderwise/libs-mobile#5](https://github.com/coderwise/libs-mobile/issues/5), since it has to happen where map-view moves the camera. Until it lands, correction is how the limits are kept.

## Consequences

- An app can keep a map to a place and a zoom range now, without waiting on map-view.
- At an edge a gesture can show the camera past the limit for a frame before it is brought back.
- The limits are kept only while a `WayfinderMap` is composed with that state, since the correction runs in the map's composition.
- Zoomed out, the screen can show ground outside the box, because only the middle is held.
- When map-view can hold the camera itself, the correction can be removed without changing `CameraLimits`.
