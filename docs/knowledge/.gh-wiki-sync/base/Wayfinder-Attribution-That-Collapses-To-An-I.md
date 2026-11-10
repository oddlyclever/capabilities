# Attribution That Collapses to an "i"

ADR-0016 · 2026-11-04 · Status: accepted
Keywords: map attribution too big, credits clutter small screen, hide attribution, OpenStreetMap attribution guidelines, OSM copyright notice, info button, collapsible attribution, AttributionStyle, Collapsible, Full, ATTRIBUTION_SHOWN_MILLIS, attribution text size, is it allowed to hide map credits

## Context

Most tile licences, OpenStreetMap's among them, require the source to be credited on the map. A line of credits in full clutters a small screen and covers part of the map. OpenStreetMap's attribution guidelines allow an interactive map to show the credit on first display and then collapse it, as long as it stays there for anyone who looks for it. Hiding it from the start is not allowed.

## Decision

The attribution ornament is on by default and uses `AttributionStyle.Collapsible` by default.

- **The credits are shown in full when the map appears**, and shrink to an "i" after five seconds (`ATTRIBUTION_SHOWN_MILLIS`) or at the user's first pan, zoom or turn, whichever comes first.
- **Tapping the "i" shows them again**, by the same rule.
- **`AttributionStyle.Full`** keeps them in full, for a source whose licence asks for that.
- **The text is 9 sp by default**, about as small as still reads, and the app can change it.
- **Opening the page behind the credits is the app's**, through `onAttributionClick`, since wayfinder does not know what a browser is.

## Consequences

- A map on a phone shows its credits as the licence asks without them covering the map for long.
- The credits are only as complete as the sources: the ornament shows each layer's `TileSource.attribution` and nothing else, so a source made without one shows no credit.
- Whether a particular source's licence accepts the collapsed form is the app's to check; `Full` is there for one that does not.
