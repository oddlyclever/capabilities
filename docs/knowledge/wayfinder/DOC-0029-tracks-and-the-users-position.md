# Tracks and the User's Position

DOC-0029 · 2026-11-04
Keywords: GPS track, recorded run, breadcrumb trail, route coloured by speed, speed gradient, ghosting, direction of travel, blue dot, my location, user location marker, accuracy circle, heading cone, heading arrow, compass heading, course-up, track start and end markers, loop, waypoints, every point marked, band of rings, TrackPoint, TrackStyle, SpeedStyle, LineStyle, HeadingMarker, LocationStyle, LocationMarker, Track, drawTrack, MapProjection, track on an image, location provider

Recorded tracks and the user's position, drawn the same way on a live map and on a map frame. They are in the `com.oddlyclever.wayfinder.track` package. wayfinder draws them; finding the user's location and reading a compass are the app's.

## On a live map

In a `WayfinderMap`'s overlay:

```kotlin
val points = recorder.points.collectAsState()        // a growing List<TrackPoint>

WayfinderMap(state, layers = listOf(basemap)) {
    Track(points.value, state, TrackStyle(speed = SpeedStyle.Gradient))
    LocationMarker(here, state, heading = compassHeading, accuracy = fix.accuracyMetres)
}
```

- **`Track(points, state, style, waypoints, modifier)`** redraws as the track grows during recording, and follows the map as it moves.
- **`LocationMarker(position, state, heading, accuracy, style, modifier)`** draws the user's position: a ringed dot, an accuracy circle sized to the ground it covers at the current zoom, and a heading marker turned with the map. An unknown heading or accuracy is not drawn, and not guessed at.
- **Course-up:** set `state.orientation = Orientation.CourseUp` and `state.heading` from the same compass, and the map turns with the user while the marker keeps pointing the right way.

For a track still being recorded, pass `TrackStyle(markEnds = false)`: its end is the user's position, which has a marker of its own.

## On a map frame

`DrawScope.drawTrack(points, projection, style, waypoints)` draws the same thing through any `MapProjection`, such as the one [wayfinder-frame](../wayfinder-frame/README.md) hands its overlay:

```kotlin
val image = renderMap(size, density, fit(points.map { it.position }, size, density), layers) { projection ->
    drawTrack(points, projection)
}
```

`Track` is `drawTrack` through the live map's projection. A `MapProjection` is three things: where a coordinate falls in pixels, how many metres a pixel covers at the middle of the map, for drawing an accuracy as a radius, and the map's bearing, for turning a heading onto the screen. `MapOverlayScope.projection(camera, density)` makes one for a live map's overlay, for drawing of your own.

## TrackPoint

A `TrackPoint(position, speed, accuracy, heading)` is a recorded fix: a position, with an optional speed in metres per second, accuracy in metres, and heading in degrees clockwise from north.

## TrackStyle

| Field | Default | What it controls |
|---|---|---|
| `line` | `LineStyle.Solid` | `Solid`, `Dotted` or `None` |
| `lineWidth`, `lineColor` | 4 dp, indigo | the line when it is not coloured by speed |
| `speed` | `SpeedStyle.Both` | `Gradient` colours the line from `slowColor` (grey) to `fastColor` (indigo) with the speed; `Ghosting` adds dots that strengthen toward the end, so the direction reads at a glance; `Both`; `None` |
| `heading`, `markerColor` | `HeadingMarker.Cone`, indigo | the marker at the last fix: `Cone` (a translucent wedge), `Arrow`, `Dot` or `None` |
| `showAccuracy`, `accuracyColor` | on, translucent indigo | the accuracy circle at the last fix, when it carries an accuracy |
| `startColor`, `endColor`, `waypointColor`, `endMarkerRadius` | green, red, blue, 6 dp | the markers at the ends and at waypoints |
| `markEnds` | on | off for a track still being recorded, whose end is the user's position and has its own marker |

The speed gradient is relative to the track: each segment's colour is its speed as a fraction of the fastest point in the track. A point with no speed counts as zero, and a track with no speeds at all is drawn in `slowColor`.

## What is drawn

- **Only the ends and waypoints are marked, never the track's own points.** A dense track marked point by point becomes a band of rings that hides the line.
- **A loop shows one marker for both ends:** when the start and end would overlap, the end is drawn as a ring with the start inside it.
- **A single point with no line** has no ends, so it and any waypoints are each marked as a place.
- **The ends are drawn last**, so nothing covers them. Waypoints are marked whether `markEnds` is on or not.
- **The accuracy circle and the heading marker** are drawn only when the last fix carries an accuracy or a heading.

## LocationStyle

`LocationStyle(color, ring, radius, accuracyColor, heading)` styles `LocationMarker`: a blue dot 7 dp in radius inside a white ring, a translucent blue accuracy circle, and a `Cone` heading marker by default.
