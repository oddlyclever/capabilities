# kartog-vector

Vector tiles for [kartog](../kartog): a vector basemap for `KartogMap`, light and dark styles for map-view's vector renderer, and vector tiles drawn to images on devices whose GPU driver cannot be trusted with their paths.

```kotlin
KartogMap(state, layers = listOf(vectorBasemap(streets, client)))
```

`vectorBasemap` draws the ground and then the names over every tile, in the style given or the one `VectorStyleProvider` provides (light by default), and switches to images by itself on a device that needs it. `staticVectorLayer` is the same for kartog-static.

**Targets:** JVM · Android · iOS (arm64, simulator) · JS · wasmJs

## Install

```kotlin
implementation("com.oddlyclever:kartog-vector:0.0.3")
```

It exposes `kartog`, map-view-tiles-vector and map-view-tiles-raster as `api`. There is no dependency-injection container.

## Light and dark

```kotlin
val style = systemVectorStyle()          // follows the system's dark mode
val ground = rememberTileState(key = streets to style) { key ->
    load(key)?.let { decodeVectorTile(key.z, it, style) }
}
```

- **`KartogStyles.Light`** is map-view's own default style, a daytime map. **`KartogStyles.Dark`** is the same style in `MapPalette.Dark`: a night map whose roads are a brightness ramp of warm greys, with the one warm accent on motorways.
- **A palette changes colour and nothing else.** `VectorStyle.recolored(palette)` keeps map-view's road widths, the zooms at which casings and outlines appear, and the paint order. A class map-view leaves undrawn stays undrawn.
- **Make your own** from `MapPalette`: copy `MapPalette.Dark` or `MapPalette.Light` and change what you need, then `VectorStyle().recolored(it)`.
- **Build a recoloured style once and keep it.** A style is compared by its lookups, and `recolored` makes new ones on every call, so one rebuilt inside a composition looks new on every frame. `KartogStyles` holds the built ones.
- **Not yet:** dashed footpaths, and tunnels and bridges drawn differently. map-view's road paint sees only a road's class; both are requests to map-view.

## When the GPU cannot draw the paths

Some GPU drivers crash the app when Compose draws complex paths. On the Pixel 10 (Tensor G5), drawing a street grid with `drawPath` on Vulkan kills the render thread. On such a device, draw each tile to an image instead, and show it with a raster slot:

```kotlin
val asImages = vectorTilesNeedRasterizing()
val density = LocalDensity.current.density
val ground = rememberTileState(key = streets) { key ->
    val bytes = load(key) ?: return@rememberTileState null
    if (asImages) rasterizeVectorTile(bytes, key.z, density, style) else decodeVectorTile(key.z, bytes, style)
}
```

- **`vectorTilesNeedRasterizing()`** is true only on Android devices known to have the fault, and false on every other target. A device forced onto the OpenGL renderer does not have it, so is false too.
- **`rasterizeVectorTile`** draws the tile with map-view's own renderer, on the CPU, off the main thread, at the size a tile is shown at. It costs tens of milliseconds a tile, so call it from the tile state's `load`.
- **The image has no labels.** map-view draws labels only inside a composition, so draw `VectorLabels` over the image as you would over a vector slot.
- **The image is a picture.** Zoomed past its level it magnifies like any raster tile, where a vector tile stays sharp: that is the price of not crashing.
