# wayfinder-vector

Vector tiles for [wayfinder](../wayfinder): a vector basemap for `WayfinderMap`, light and dark styles for map-view's vector renderer, and vector tiles drawn as images on devices whose GPU driver cannot be trusted with their paths.

**Targets:** JVM · Android · iOS (arm64, simulator) · JS · wasmJs

## Install

```kotlin
implementation("com.oddlyclever:wayfinder-vector:0.0.7")
```

It exposes `wayfinder`, map-view-tiles-vector and map-view-tiles-raster as `api`. There is no dependency-injection container.

## A vector basemap

```kotlin
WayfinderMap(state, layers = listOf(vectorBasemap(streets, client)))
```

`vectorBasemap(source, client, style = null, labels = true, rasterize = false)` fetches each tile through the client, decodes it off the main thread with map-view's decoder, and draws it with map-view's renderer: the ground first, then the names in a layer of their own, so every name sits over every tile: places, roads, water, rivers, peaks and points of interest. Tiles are decoded once, as they arrive, and a tile the source lacks is drawn from its nearest ancestor.

The source is expected to use the OpenMapTiles schema, as map-view's style is written for it: `water`, `landcover`, `landuse`, `park`, `building`, `transportation` and the rest.

## Light and dark

```kotlin
VectorStyleProvider(systemVectorStyle()) {
    WayfinderMap(state, layers = listOf(vectorBasemap(streets, client)))
}
```

- **`MapStyles.Light`** is map-view's own default style, a daytime map. **`MapStyles.Dark`** is the same style in `MapPalette.Dark`: roads as a brightness ramp of warm greys with no pure white, the one warm accent on motorways, cool water and near-black land.
- **`systemVectorStyle(light, dark)`** follows the system's dark mode as it changes. **`VectorStyleProvider(style)`** provides a style to every basemap inside it that is not given one, the way a theme provides colours; `LocalVectorStyle` reads it. A basemap given `style` uses that instead.
- **A palette changes colour and nothing else.** `VectorStyle.recolored(palette)` keeps map-view's road widths that grow with zoom, the zooms at which casings and building outlines appear, and the paint order. A class map-view leaves undrawn stays undrawn.
- **Your own palette:** copy `MapPalette.Dark` or `MapPalette.Light` and change what you need. `MapPalette` names every colour: land, water, greenery, bare ground, built-up land, buildings and their outlines, airports, boundaries, label ink, halo and water labels, a `RoadColors` (casing and core) for each road tier from motorway to service, and paths, rail and ferries.
- **Build a recoloured style once and keep it.** A style is compared by its lookups, and `recolored` makes new ones on every call, so a style rebuilt inside a composition looks new on every frame and throws away the decoded tiles. `MapStyles` holds the built ones; keep your own in a top-level `val`.
- **What is named is the style's.** Both styles name places, roads, water, rivers, peaks and points of interest, as map-view does by default. Turn a kind off with its `NameStyle`, and a kind that is off is not even decoded: `val Quiet = MapStyles.Light.copy(poiName = NameStyle(9.5f, enabled = false))`, kept in a top-level `val` like any style.
- **Not yet:** dashed footpaths, and tunnels and bridges drawn apart from other roads. map-view's road paint sees only a road's class; both are requests to map-view.

## Drawing tiles as images

Tiles are drawn as paths on the GPU, through whatever renderer the platform uses, on every device. An app that has a reason to keep tile paths off the GPU can ask for images instead:

```kotlin
// On Android, for instance: only on devices where this app has seen the GPU driver crash.
val rasterize = Build.MODEL.startsWith("Pixel 10")
WayfinderMap(state, layers = listOf(vectorBasemap(streets, client, rasterize = rasterize)))
```

- **`vectorBasemap(…, rasterize = true)`** draws each tile's ground to an image on the CPU and shows it with a raster slot. Names are still drawn from the decoded tile, since text does not go through the path renderer.
- **Which devices get it is the app's decision.** wayfinder keeps no list of devices.
- **`rasterizeVectorTile(bytes, zoom, density, style)`** is the step on its own: a tile drawn to an `ImageBitmap` at the size a tile is shown, 256 dp. It costs tens of milliseconds a tile, so call it from a tile state's load, off the main thread.
- **An image is a picture.** Zoomed past its level, it magnifies like any raster tile, where a vector tile stays sharp.

### Why the option exists

wayfinder used to switch to images by itself on the Pixel 10 family (Tensor G5, PowerVR DXT-48-1536). There, an earlier map renderer drawing a street grid with Compose's `drawPath` on Vulkan had crashed the render thread with a segmentation fault. When map-view's own vector drawing was tried on a Pixel 10 Pro XL on Android 17, the fault did not reproduce. That test covered dense street grids at zooms 13 to 20, about 70 fast pans and 22 zooms, and 1,500 frames without a fatal signal. So wayfinder dropped the device rule and draws on the GPU everywhere.

Keep `rasterize` in mind if a GPU driver fault on complex paths appears on some device. It is the way around one without waiting for a driver update.

## On an image

`vectorFrameLayer(zoomRange, style, names = null) { key -> bytes }` is the same basemap for wayfinder-frame's `renderMap` and `MapFrame`, drawn by the same renderer so a frame matches the live map. Give it a `TextMeasurer` as `names` and it draws the style's names too, laid out by map-view as on a live map, over every tile and level at any bearing. In a composable that is `rememberTextMeasurer()`; laying out text needs a context on Android, which is why it is the app's to give. Names cost a second decode of each tile.

## For coding agents

The module ships an [Agent Skill](https://agentskills.io/specification), `com-oddlyclever-wayfinder-vector`, for the agents of projects that depend on it: what the library is for, how it's meant to be called, and the mistakes that compile. It is written at [src/commonMain/skills/com-oddlyclever-wayfinder-vector/SKILL.md](src/commonMain/skills/com-oddlyclever-wayfinder-vector/SKILL.md) and published inside every sources jar, at `commonMain/skills/com-oddlyclever-wayfinder-vector/`.
