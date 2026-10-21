# kartog-track

Recorded tracks and the user's position for [kartog](../kartog), drawn the same way on a live map and on a map image.

**Targets:** JVM · Android · iOS (arm64, simulator) · JS · wasmJs

## Install

```kotlin
implementation("com.oddlyclever:kartog-track:0.0.3")
```

It depends on `kartog` alone. There is no dependency-injection container.

## On a live map

In a `KartogMap`'s overlay:

```kotlin
KartogMap(state, layers = listOf(basemap)) {
    Track(recorded, state)
    LocationMarker(here, state, heading = compass, accuracy = fix.accuracy)
}
```

- **`Track`** redraws as the track grows during recording, and follows the map as it moves.
- **`LocationMarker`** draws the user's position: a ringed dot, an accuracy circle sized to the ground it covers, and a heading marker turned with the map. An unknown accuracy or heading is not drawn.

## On an image

`DrawScope.drawTrack(points, projection, style, waypoints)` draws the same thing through any `MapProjection`, such as kartog-static's: `renderMap(...) { projection -> drawTrack(track, projection) }`.

## What is drawn

A `TrackPoint` is a position with an optional speed (m/s), accuracy (m) and heading (degrees from north).

- **The line**, solid or dotted, coloured by speed with `SpeedStyle.Gradient` from `slowColor` to `fastColor`, with dots that strengthen toward the end with `Ghosting`, or both.
- **At the last fix**, an accuracy circle and a heading marker (`Cone`, `Arrow` or `Dot`), each only when the fix carries the value.
- **Markers at the ends and at waypoints only.** The track's own points are never marked: a dense track marked point by point becomes a band of rings that hides the line. The start is green and the end red; when they would overlap, as on a loop, one marker shows both. A single point, with no line, is marked as a place.
