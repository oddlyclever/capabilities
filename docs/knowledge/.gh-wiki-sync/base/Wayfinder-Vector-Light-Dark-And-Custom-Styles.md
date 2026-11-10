# Light, Dark and Custom Styles

DOC-0042 · 2026-11-04
Keywords: dark map, night mode, map dark theme, follow system dark mode, MapStyles, MapPalette, VectorStyleProvider, LocalVectorStyle, systemVectorStyle, recolored, brand colours, custom map colours, map colour palette, hide points of interest, turn off POI labels, hide lake names, hide road names, NameStyle, map flickers when style changes, tiles reload every frame, dashed footpaths, tunnels and bridges, roadDetail, MapLibre style JSON

A vector basemap is drawn in a map-view `VectorStyle`. This module adds a dark one, a way to recolour any style, and a way to give every basemap on a screen the same style the way a theme gives colours. This page covers each, and the one rule they all share: build a style once.

## The two styles

`MapStyles` holds the styles the module builds:

- **`MapStyles.Light`** is map-view's own default style, `VectorStyle()`, a daytime map.
- **`MapStyles.Dark`** is that style recoloured in `MapPalette.Dark`: roads as a brightness ramp of muted warm greys with no pure white, so side streets do not overpower the map; the one warm accent spent on motorways; cool water, near-black land and a hint of green.

Both name places, roads, water, rivers, peaks and points of interest, as map-view does by default.

`MapStyles.Light` is not `MapPalette.Light`. `MapPalette.Light` is a different, warmer daytime palette, with a warm accent on motorways and grey casings that darken with importance, so side streets recede. `MapStyles.Light.recolored(MapPalette.Light)` is a third map, unlike `MapStyles.Light`.

## Following the system, or the app

```kotlin
VectorStyleProvider(systemVectorStyle()) {
    WayfinderMap(state, layers = listOf(vectorBasemap(streets, client)))
}
```

- **`systemVectorStyle(light = MapStyles.Light, dark = MapStyles.Dark)`** is `dark` while the system is in dark mode and `light` otherwise, and follows the setting as it changes. It is a convenience: a map that should stay light at night, or follow the app's own theme instead, passes the style it wants.
- **`VectorStyleProvider(style) { … }`** gives `style` to every basemap inside it that is not given one, the way `MaterialTheme` gives colours. `LocalVectorStyle` reads it, and is `MapStyles.Light` outside any provider.
- **A basemap given `style`** uses that and ignores the provider.

A change of style starts the basemap's tiles again, so switching light to dark fetches every tile again through the client. A cache in the client makes that cheap. `vectorFrameLayer` is not composable and never reads the provider; see [Vector Tiles on a Map Frame](Wayfinder-Vector-Vector-Tiles-On-A-Map-Frame).

## A palette of your own

`MapPalette` names every colour a map is drawn in, and nothing else. Copy `MapPalette.Light` or `MapPalette.Dark` and change what you need:

```kotlin
val Brand: VectorStyle = MapStyles.Light.recolored(MapPalette.Light.copy(water = Color(0xFF8FB8D8), land = Color(0xFFF2EEE6)))
```

| Field | What it colours |
|---|---|
| `land` | The background. OpenMapTiles has no land layer: land is what shows where nothing else is drawn. |
| `water` | Water areas and waterways. |
| `greenery` | Woods, grass, parks and wetland, and green land use. |
| `bareGround` | Sand, beach, ice, glacier, snow, rock, bare rock, scree, farmland, farm, orchard and vineyard. |
| `builtUp` | Every other land use: residential, commercial, industrial and the rest. |
| `building`, `buildingOutline` | Buildings, and their outlines where the style draws them. |
| `airport` | Airport ground and runways. |
| `boundary` | Borders. |
| `ink`, `halo` | Place and road names, and the halo drawn under them so a name survives a road beneath it. |
| `waterInk` | Lake and river names. |
| `motorway`, `major`, `tertiary`, `minor`, `service` | A `RoadColors(casing, core)` for each road tier. Motorway and trunk are `motorway`; primary and secondary are `major`; every other road is `minor`. |
| `path`, `rail`, `ferry` | Tracks, paths, footways, cycleways, pedestrian ways and steps; rail and transit; ferry routes. |

The green land uses are cemetery, recreation ground, dog park, park, grass and meadow. Which kind goes to which colour is fixed by `recolored`, whatever the palette: farmland is bare ground, not greenery.

## What recolouring keeps

`VectorStyle.recolored(palette)` changes colour and nothing else. Each lookup of the new style asks the base style for its paint and recolours what comes back, so:

- map-view's road widths that grow with zoom, the zooms at which casings and building outlines appear, and the paint order are all kept;
- a class the base style leaves undrawn stays undrawn, and one it draws keeps its weight;
- name sizes and which kinds of name are on are kept.

A width, a zoom gate or a paint of your own is a change to the `VectorStyle` itself, through map-view's `copy`, not to the palette. A style's `roadDetail` lookup, if it has one, is kept as it is and not recoloured.

## Build a style once

A `VectorStyle` is a data class whose lookups are functions, and two styles are equal only when their lookups are equal, which a new lambda never is. `recolored` makes new lookups on every call, so `MapStyles.Light.recolored(palette)` written in a composition is a new style at every recomposition. The basemap sees a new style, throws its decoded tiles away and fetches them all again, every time.

`MapStyles` holds the built ones, made once. Keep your own in a top-level `val`, as `Brand` is above. The same goes for a style written with a lambda literal, such as a `copy` that passes a new `road = { … }`.

## Turning names off

Which kinds of name are drawn is the style's. Each of the four optional kinds has a map-view `NameStyle(size, enabled)`: `poiName`, `waterName`, `riverName` and `peakName`. A kind that is off is not even decoded, so it costs nothing:

```kotlin
val Quiet: VectorStyle = MapStyles.Light.copy(poiName = NameStyle(9.5f, enabled = false))
```

Road names are `roadName`, a size in sp; null leaves roads unnamed. Keep the style in a top-level `val` like any other.

`vectorBasemap(…, labels = false)` hides every name from the live map at once. It still decodes them, so to save the work, turn the kinds off in the style instead.

## Footpaths, tunnels and bridges

The module's two styles draw footpaths as solid lines, and draw tunnels and bridges like any other road of their class. map-view 0.3.0 has the means for both: `LayerStyle.dash` for a dashed line, and `VectorStyle.roadDetail`, a lookup that sees a road's `subclass` and `brunnel` tags as well as its class. A style of your own can use them through map-view's `copy`. `recolored` does not recolour what `roadDetail` returns, so recolour first and set `roadDetail` after, in the colours you want.
