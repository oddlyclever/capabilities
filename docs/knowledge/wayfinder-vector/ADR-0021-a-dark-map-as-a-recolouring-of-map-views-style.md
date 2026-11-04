# A Dark Map as a Recolouring of map-view's Style

ADR-0021 · 2026-11-04 · Status: accepted
Keywords: why a palette, MapPalette, recolored, dark style, night map, road widths in dark mode, custom colours only, change road width, MapStyles, map-view default style

## Context

map-view's default `VectorStyle` is a daytime map. Beyond its colours it carries road widths that grow with zoom, the zooms at which casings and building outlines appear, and the paint order, all tuned by map-view against real tiles. A dark map wants the same hierarchy in different ink.

## Decision

- **A `MapPalette` holds every colour a vector map is drawn in, and nothing else.** Land, water, greenery, bare ground, built-up land, buildings and their outlines, airports, boundaries, label ink, halo and water labels, a casing and a core for each road tier, and paths, rail and ferries.
- **`VectorStyle.recolored(palette)` recolours a style and keeps everything else.** Each lookup of the new style asks the base style for its paint and recolours what comes back, so a class the base leaves undrawn stays undrawn and one it draws keeps its weight.
- **`MapStyles.Dark` is map-view's default style in `MapPalette.Dark`**, and `MapStyles.Light` is map-view's default style as it is. Both are built once and held in `MapStyles`.

## Consequences

- The dark map has the same road hierarchy and zoom behaviour as the light one, and both follow map-view's default when it changes.
- An app's own colours are a palette copied from `MapPalette.Light` or `MapPalette.Dark`, with no style to write. A different width, zoom gate or paint is a change to the `VectorStyle`, not to the palette.
- `recolored` makes new lookups on every call, and a `VectorStyle` is compared by its lookups, so a recoloured style must be built once and kept. One rebuilt in a composition looks new at every recomposition, and the basemap throws its tiles away.
- A style's `roadDetail` lookup is not recoloured. [Light, Dark and Custom Styles](DOC-0042-light-dark-and-custom-styles.md) says how to work with it.
