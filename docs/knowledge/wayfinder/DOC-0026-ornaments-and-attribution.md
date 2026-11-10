# Ornaments and Attribution

DOC-0026 · 2026-11-04
Keywords: map controls, compass, compass tape, rosette, north arrow, north-up button, course-up toggle, scale bar, miles, metric, zoom buttons, plus minus buttons, attribution, map credits, OpenStreetMap copyright, attribution hidden, credits shrink to i, info button, ornaments under app bar, MapOrnaments, MapCompassTape, OrientationToggle, OrientationIcon, compass in top bar, LocalUriHandler, no attribution shown

The ornaments are the controls drawn over a map: a compass, an orientation toggle, a scale bar, zoom buttons and the credits. `MapOrnaments` switches each on or off and places it, and the compass tape and the orientation toggle are also public composables for a screen that places them in its own layout.

## MapOrnaments

| Field | Default | What it is |
|---|---|---|
| `compass`, `compassAlignment` | on, top centre | the compass; tapping it turns the map back to north-up |
| `compassStyle` | `Tape` | `Tape` is a strip of headings across the map, for a map someone navigates by; `Rosette` is its small needle that always points north, for a map someone looks around on |
| `orientationToggle`, `orientationToggleAlignment` | off, top end | a button switching between north-up and course-up |
| `scaleBar`, `scaleBarAlignment`, `scaleUnits` | on, bottom start, metric | a bar of a round distance, in `ScaleUnits.Metric` or `Imperial`, true at the middle of the map |
| `zoomButtons`, `zoomButtonsAlignment` | off, centre end | zoom in and out a level a press, off at the ends of the zoom range |
| `attribution`, `attributionAlignment` | on, bottom end | the credits the layers' sources ask for; leave it on wherever a licence requires attribution |
| `attributionStyle` | `Collapsible` | `Collapsible` shows the credits when the map appears, then shrinks them to an "i"; `Full` keeps them in full |
| `attributionTextSize` | 9 sp | the credits' text size |
| `padding` | 8 dp | how far ornaments sit from the edges, to clear an app bar or a sheet |
| `labels` | nothing set | the app's own words for the controls, over wayfinder's; see [Labels and languages](#labels-and-languages) |

## The compass

Tapping the `Tape` compass turns the map back to north and leaves course-up. The `Rosette` needle also turns the map back to north when pressed, but it does not leave course-up, where the heading turns the map straight back. On a wayfinder map the rosette is always shown, even when the map already points north.

## The orientation toggle

The toggle shows a compass needle in north-up, its north half blue and its south half grey, and a white navigation arrowhead on blue in course-up. Tapping it switches between the two, and leaving course-up turns the map back to north. To a screen reader it says what a tap does, in the device's language.

With the `Tape` compass at the top, which spans the map's width, put the toggle somewhere other than a top corner, or it sits on the tape's end.

## Placing them in the screen's own layout

Three composables are public for a screen that wants them somewhere other than over the map, such as in a top bar. Leave the matching `MapOrnaments` flag off and draw them there:

- **`MapCompassTape(state, modifier, background, labels)`** is the tape the ornament draws. It takes the width it is given. `background` is the tape's own plate, translucent white by default; pass `Color.Transparent` to draw it on a plate of the screen's.
- **`OrientationToggle(state, modifier, size = 40.dp, labels)`** is the toggle. Give it the size of the buttons beside it.

Both say what they are in the device's language on their own. A screen that sets `labels` on the map's `MapOrnaments` passes the same ones here, so a control says the same thing wherever it is drawn.
- **`OrientationIcon(courseUp, modifier)`** is only what the toggle shows, for a screen that draws its own button around it. It is drawn rather than taken from an icon set, so the module needs no icon dependency.

## Labels and languages

The ornaments name themselves in the device's language with no setup: what a screen reader says for each control, and the compass tape's points. The language is aughtone-types' `Locale.current`. wayfinder has its words in 53 languages, chosen by the language's code whatever the region, so French in Canada gets French; Chinese is traditional in Taiwan, Hong Kong and Macau and simplified elsewhere. Inuktitut and the other Canadian Indigenous languages aughtone-types knows, such as the Cree and Ojibwe it added in 4.2.0, get English for now, until translations checked by speakers are added; an app that needs them sooner sets `MapOrnamentLabels`. Any other language gets English too. Where a language has no settled short form for the points of the compass, the tape uses the international N, NE, E and so on.

`MapOrnamentLabels` replaces any of them with the app's own, field by field. Every field is null unless set, and a null field keeps wayfinder's word:

| Field | English | Used by |
|---|---|---|
| `zoomIn`, `zoomOut` | "Zoom in", "Zoom out" | the zoom buttons |
| `north` | "North" | the `Rosette` compass |
| `compass` | "Compass" | the `Tape` compass |
| `credits` | "Map credits" | the "i" the credits shrink to |
| `northUp`, `courseUp` | "North up: tap for course up", "Course up: tap for north up" | the orientation toggle, in each orientation |
| `compassPoints` | N, NE, E, SE, S, SW, W, NW | the tape's points, from north clockwise; it must have eight |

Set one for a language wayfinder does not cover, or where the app's own term for a control should win, and pass it as `MapOrnaments(labels = …)`. wayfinder's translations are in `OrnamentTranslations.kt`; a language is added there with all eight fields, and `MapOrnamentLabels`' KDoc says which languages there are.

The scale bar needs none of this. Its distance is written in the device's locale by aughtone's `format-readable`, with that locale's digit grouping and decimal separator, so 1,000 ft in English is 1 000 ft in French. The unit symbols, m, km, ft and mi, are international and stay as they are. Metric picks its own prefix, so a bar shorter than a metre is in millimetres.

## Attribution

The attribution ornament shows the `attribution` of each layer's `TileSource`, joined with " · ", and repeated credits once. A source made without an attribution shows nothing, and the ornament never shows credits for data drawn in the overlay, such as GeoJSON. Most tile licences, OpenStreetMap's among them, require the credit, so give every source the one its licence asks for.

**Collapsible, the default.** A map's credits clutter a small screen, so they are shown in full when the map appears and shrink to an "i" after five seconds or at the user's first pan, zoom or turn, whichever comes first. Tapping the "i" shows them again, by the same rule. The "i" is announced to a screen reader as "Map credits", a button.

That is the form OpenStreetMap's attribution guidelines allow for an interactive map: visible on first display, then collapsed, and there for anyone who looks for it. Hiding the credits from the start is not allowed, and their text must stay readable; 9 sp is about as small as reads. Why it is the default is in [Attribution That Collapses to an "i"](ADR-0016-attribution-that-collapses-to-an-i.md).

**Opening the page behind the credits.** Pass `onAttributionClick` to `WayfinderMap` to open it, such as `https://www.openstreetmap.org/copyright`, for instance with Compose's `LocalUriHandler`. What opens it is the app's: wayfinder does not know what a browser is.
