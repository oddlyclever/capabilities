# The Map State and the Camera

DOC-0027 · 2026-11-04
Keywords: WayfinderMapState, rememberWayfinderMapState, CameraPositionState, move camera, animate camera, fly to, zoom in, camera snaps back, follow the user, map fights the user, isInteracting, course-up, heading, map does not rotate, visible bounds, search this area, viewport bounds, antimeridian, rotation loses camera, Saver, view model, MonotonicFrameClock is not available, fit camera to points, fit bounds, zoom to fit route, shows the whole world, Nothing to fit, CameraLimits, restrict map to a region, max bounds, min zoom, max zoom, ZOOM_LIMITS, screenPosition, isOnScreen, metersPerDp

`WayfinderMapState` is everything about a map that the app reads or drives: where the camera is, whether the user has hold of it, which way is up, and what is on screen. This page covers the state, the camera helpers and the camera limits.

## Making one

`rememberWayfinderMapState(center, zoom, bearing)` keeps one for the composition. The defaults are 0°, 0° at zoom 3, north-up.

- **Its arguments are where the camera starts, read the first time only**, as with any remembered state. A `center` that changes later, such as the first location fix, does not move the camera. Move it with `moveTo` or `animateTo` instead.
- **It is saved with the screen's instance state**, so a rotation keeps the camera where the user left it. `WayfinderMapState.Saver` saves the centre, zoom, bearing and orientation, the orientation by name. It does not save `heading`, which comes from a sensor the app reads again, nor `visibleBounds`, which the map recomputes. `Saver` is public, for a state kept elsewhere.

Construct `WayfinderMapState(center, zoom, bearing)` directly to hold one in a view model.

## Members

| Member | What it is |
|---|---|
| `center`, `zoom`, `bearing` | where the camera is; `bearing` is what is at the top of the map, in degrees clockwise from north |
| `moveTo(center, zoom)`, `moveTo(position)` | put the camera somewhere at once |
| `animateTo(center, zoom, animationSpec)`, `animateTo(position, animationSpec)` | fly there, the short way round the world; a `CameraPosition` is turned to its bearing after the flight |
| `zoomIn()`, `zoomOut()` | one level, animated, about the middle of the map |
| `isInteracting` | true while the user has hold of the map, and for two seconds after they let go |
| `orientation`, `heading` | `Orientation.NorthUp` or `CourseUp`; in course-up the map turns so `heading` is at the top |
| `visibleBounds` | what the map shows, as a `GeoBounds`, kept current as the map moves or is resized; null until laid out |
| `camera` | map-view's `MapCameraState`, for anything wayfinder does not wrap |

**The animations need a composition's frame clock.** `animateTo`, `zoomIn` and `zoomOut` are Compose animations, so call them from `LaunchedEffect` or `rememberCoroutineScope()`. Called from a view model's own scope they fail with "A MonotonicFrameClock is not available in this CoroutineContext". `moveTo` is immediate and needs no coroutine.

**The state is kept current only while a map shows it.** `visibleBounds`, `isInteracting` and the camera limits are all maintained by a composed `WayfinderMap` with that state. `isInteracting` and `visibleBounds` cannot be set by the app.

## Following the user without fighting them

Move the camera on the app's behalf only while `isInteracting` is false:

```kotlin
LaunchedEffect(position) {
    if (!state.isInteracting) state.animateTo(position)
}
```

The two-second settle, `WayfinderMapState.SETTLE_MILLIS`, is what stops the map snapping back the moment a pan ends: people lift a finger, look, and carry on panning.

## Course-up

Set `orientation = Orientation.CourseUp` and keep `heading` current, from a compass or the direction of travel. The map turns, animated and the short way round, so the heading is at the top. The user's two-finger turn is off, and tapping the `Tape` compass returns to north-up.

The turn is map-view's: it waits while the user has hold of the map, and leaves a heading within 2 degrees of the current bearing alone, since a GPS heading wobbles by about that much. With `heading` null the map does not turn, and stays at whatever bearing it had.

## Querying what is on screen

`visibleBounds` is the smallest box of latitude and longitude that holds everything visible, turned or not. With a bearing, the screen is a turned rectangle on the ground, so the box is the one around its four corners and holds some ground just off screen. Latitude is limited to what Web Mercator can show. A map zoomed out far enough to show the whole width of the world gets a box from -180 to 180.

When the map crosses the antimeridian, `west` is greater than `east` and `crossesAntimeridian` is true; `point in bounds` accounts for it, edges included.

```kotlin
LaunchedEffect(state) {
    snapshotFlow { state.visibleBounds }.filterNotNull().debounce(300).collect { bounds -> search(bounds) }
}
```

## Fitting the camera to points

`fit(points, size, density, padding = 0.dp, bearing = 0f, maxZoom = 17f)` returns the `CameraPosition` that frames every point inside the padding, as close in as they allow. A `CameraPosition` is a centre, a zoom and a bearing, and both a live map and a [map frame](../wayfinder-frame/README.md) take one.

```kotlin
state.animateTo(fit(route, mapSize, density, padding = 32.dp, bearing = state.bearing))
```

- **It respects the bearing.** A long north-south track on a map with east up is fitted across the screen, not down it.
- **It crosses the antimeridian the short way**, so a route from Fiji to Samoa is framed over the Pacific rather than stretched across the world.
- **A single point, or points too close to need any zoom, sit at `maxZoom`**, street level by default (`DEFAULT_FIT_MAX_ZOOM`, 17), since a single point fitted any closer shows nothing around it. The zoom is also kept inside map-view's `ZOOM_LIMITS`, 1 to 20.
- **It needs the map's size.** The size is the map's own, from `onSizeChanged` on its `modifier`. `IntSize.Zero`, or a map smaller than twice the padding, gets the farthest zoom rather than an error, so a fit made before the map is measured shows the whole world. Fit once the size is known:

```kotlin
var size by remember { mutableStateOf(IntSize.Zero) }
val density = LocalDensity.current
WayfinderMap(state, Modifier.fillMaxSize().onSizeChanged { size = it }, layers = listOf(basemap))
LaunchedEffect(route, size) {
    if (route.isNotEmpty() && size != IntSize.Zero) state.animateTo(fit(route, size, density, padding = 32.dp))
}
```

- **An empty list is refused** with `IllegalArgumentException`: "Nothing to fit: the list of points is empty."

## Helpers on map-view's camera

For code outside an overlay, such as a view model deciding whether to bring the camera back to the user:

- **`screenPosition(point, size, density)`** is where a point falls, in pixels from the map's top left, with the bearing taken into account. A point off the map gets a position off its edges. Inside an overlay, map-view's own `project()` answers the same question.
- **`isOnScreen(point, size, density, margin)`** says whether a point is on the map. `margin` shrinks the area that counts, so a point under a control at the edge can be treated as off screen.
- **`visibleBounds(size, density)`** is what `WayfinderMapState` keeps current.
- **`moveTo(position)` and `flyTo(position)`** take a `CameraPosition`. map-view's flight does not turn the map, so the bearing is set after it lands.
- **`zoomIn()` and `zoomOut()`**, one level, animated, stopping at `ZOOM_LIMITS`.

For a scale, map-view's own `MapCameraState.metersPerDp`.

## Camera limits

`CameraLimits(zoom = ZOOM_LIMITS, center = null)`, passed as `WayfinderMap`'s `limits`, keeps the camera in a range of zoom and, when `center` is a `GeoBounds`, keeps the middle of the map inside that box. The box can cross the antimeridian.

```kotlin
CameraLimits(zoom = 10f..18f, center = GeoBounds(south = 43.5, west = -80.0, north = 44.0, east = -79.0))   // one town
```

- **It limits where the middle goes, not what the screen shows**, as MapLibre's bounds do. Zoomed out, the edges can show ground outside the box.
- **The zoom can never leave map-view's own range**, 1 to 20, whatever `zoom` says. `effectiveZoom` is the range actually kept to; a range that misses map-view's entirely falls back to map-view's.
- **Outside the box, the camera goes to the nearest edge.** Outside a box that crosses the antimeridian, it goes to whichever side is nearer the short way round.
- **Limits correct the camera, they do not stop it.** wayfinder brings the camera back inside whenever a gesture or a move takes it out, or the limits change around it, so at an edge a gesture can show it past the limit for a frame. Why is in [Camera Limits Corrected After the Move](ADR-0015-camera-limits-corrected-after-the-move.md).

`hold(zoom)` and `hold(point)` are the corrections on their own, and are pure.
