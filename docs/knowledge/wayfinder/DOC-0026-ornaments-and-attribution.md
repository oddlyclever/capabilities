# Ornaments and Attribution

DOC-0026 · 2026-11-04
Keywords: map controls, compass, compass tape, rosette, north arrow, north-up button, course-up toggle, scale bar, miles, metric, zoom buttons, plus minus buttons, attribution, map credits, OpenStreetMap copyright, attribution hidden, credits shrink to i, info button, ornaments under app bar, MapOrnaments, MapCompassTape, OrientationToggle, OrientationIcon, compass in top bar, LocalUriHandler, no attribution shown

The ornaments are the controls drawn over a map: a compass, an orientation toggle, a scale bar, zoom buttons and the credits. `MapOrnaments` switches each on or off and places it, and the compass tape and the orientation toggle are also public composables for a screen that places them in its own layout.

## MapOrnaments

| Field | Default | What it is |
|---|---|---|
| `compass`, `compassAlignment` | on, top centre | the compass; tapping it turns the map back to north-up |
| `compassStyle` | `Tape` | `Tape` is map-view's strip of headings across the map, for a map someone navigates by; `Rosette` is its small needle that always points north, for a map someone looks around on |
| `orientationToggle`, `orientationToggleAlignment` | off, top end | a button switching between north-up and course-up |
| `scaleBar`, `scaleBarAlignment`, `scaleUnits` | on, bottom start, metric | map-view's scale bar |
| `zoomButtons`, `zoomButtonsAlignment` | off, centre end | map-view's zoom buttons |
| `attribution`, `attributionAlignment` | on, bottom end | the credits the layers' sources ask for; leave it on wherever a licence requires attribution |
| `attributionStyle` | `Collapsible` | `Collapsible` shows the credits when the map appears, then shrinks them to an "i"; `Full` keeps them in full |
| `attributionTextSize` | 9 sp | the credits' text size |
| `padding` | 8 dp | how far ornaments sit from the edges, to clear an app bar or a sheet |

## The compass

Tapping the `Tape` compass turns the map back to north and leaves course-up. The `Rosette` needle also turns the map back to north when pressed, but it does not leave course-up, where the heading turns the map straight back. On a wayfinder map the rosette is always shown, even when the map already points north.

## The orientation toggle

The toggle shows a compass needle in north-up, its north half blue and its south half grey, and a white navigation arrowhead on blue in course-up. Tapping it switches between the two, and leaving course-up turns the map back to north. Its screen-reader description is in English: "North up: tap for course up" or "Course up: tap for north up".

With the `Tape` compass at the top, which spans the map's width, put the toggle somewhere other than a top corner, or it sits on the tape's end.

## Placing them in the screen's own layout

Three composables are public for a screen that wants them somewhere other than over the map, such as in a top bar. Leave the matching `MapOrnaments` flag off and draw them there:

- **`MapCompassTape(state, modifier, background)`** is the tape the ornament draws. It takes the width it is given. `background` is the tape's own plate, translucent white by default; pass `Color.Transparent` to draw it on a plate of the screen's.
- **`OrientationToggle(state, modifier, size = 40.dp)`** is the toggle. Give it the size of the buttons beside it.
- **`OrientationIcon(courseUp, modifier)`** is only what the toggle shows, for a screen that draws its own button around it. It is drawn rather than taken from an icon set, so the module needs no icon dependency.

## Attribution

The attribution ornament shows the `attribution` of each layer's `TileSource`, joined with " · ", and repeated credits once. A source made without an attribution shows nothing, and the ornament never shows credits for data drawn in the overlay, such as GeoJSON. Most tile licences, OpenStreetMap's among them, require the credit, so give every source the one its licence asks for.

**Collapsible, the default.** A map's credits clutter a small screen, so they are shown in full when the map appears and shrink to an "i" after five seconds or at the user's first pan, zoom or turn, whichever comes first. Tapping the "i" shows them again, by the same rule. The "i" is announced to a screen reader as "Map credits", a button.

That is the form OpenStreetMap's attribution guidelines allow for an interactive map: visible on first display, then collapsed, and there for anyone who looks for it. Hiding the credits from the start is not allowed, and their text must stay readable; 9 sp is about as small as reads. Why it is the default is in [Attribution That Collapses to an "i"](ADR-0016-attribution-that-collapses-to-an-i.md).

**Opening the page behind the credits.** Pass `onAttributionClick` to `WayfinderMap` to open it, such as `https://www.openstreetmap.org/copyright`, for instance with Compose's `LocalUriHandler`. What opens it is the app's: wayfinder does not know what a browser is.
