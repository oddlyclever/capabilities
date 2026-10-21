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
val points = recorder.points.collectAsState()        // a growing List<TrackPoint>

KartogMap(state, layers = listOf(basemap)) {
    Track(points.value, state, TrackStyle(speed = SpeedStyle.Gradient))
    LocationMarker(here, state, heading = compassHeading, accuracy = fix.accuracyMetres)
}
```

- **`Track(points, state, style, waypoints)`** redraws as the track grows during recording, and follows the map as it moves.
- **`LocationMarker(position, state, heading, accuracy, style)`** draws the user's position: a ringed dot, an accuracy circle sized to the ground it covers at the current zoom, and a heading marker turned with the map. An unknown heading or accuracy is not drawn, and not guessed at.
- **Course-up:** set `state.orientation = Orientation.CourseUp` and `state.heading` from the same compass, and the map turns with the user while the marker keeps pointing the right way.

## On an image

`DrawScope.drawTrack(points, projection, style, waypoints)` draws the same thing through any `MapProjection`, such as the one kartog-static hands its overlay:

```kotlin
val image = renderMap(size, density, fit(points.map { it.position }, size, density), layers) { projection ->
    drawTrack(points, projection)
}
```

## What is drawn

A `TrackPoint(position, speed, accuracy, heading)` is a recorded fix: a position, with an optional speed in metres per second, accuracy in metres, and heading in degrees clockwise from north.

| `TrackStyle` field | Default | What it controls |
|---|---|---|
| `line` | `LineStyle.Solid` | `Solid`, `Dotted` or `None` |
| `lineWidth`, `lineColor` | 4 dp, indigo | the line when it is not coloured by speed |
| `speed` | `SpeedStyle.Both` | `Gradient` colours the line from `slowColor` to `fastColor` with the speed; `Ghosting` adds dots that strengthen toward the end, so the direction reads at a glance; `Both`; `None` |
| `heading`, `markerColor` | `HeadingMarker.Cone` | the marker at the last fix: `Cone` (a translucent wedge), `Arrow`, `Dot` or `None` |
| `showAccuracy`, `accuracyColor` | on | the accuracy circle at the last fix, when it carries an accuracy |
| `startColor`, `endColor`, `waypointColor`, `endMarkerRadius` | green, red, blue, 6 dp | the markers at the ends and at waypoints |

- **Only the ends and waypoints are marked, never the track's own points.** A dense track marked point by point becomes a band of rings that hides the line.
- **A loop shows one marker for both ends:** when start and end would overlap, the end is drawn as a ring with the start inside it.
- **A single point with no line** has no ends, so it and any waypoints are marked as places.

`LocationStyle(color, ring, radius, accuracyColor, heading)` styles `LocationMarker`.
