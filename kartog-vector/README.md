# kartog-vector

Vector tiles for [kartog](../kartog): a vector basemap for `KartogMap`, light and dark styles for map-view's vector renderer, and vector tiles drawn as images on devices whose GPU driver cannot be trusted with their paths.

**Targets:** JVM · Android · iOS (arm64, simulator) · JS · wasmJs

## Install

```kotlin
implementation("com.oddlyclever:kartog-vector:0.0.3")
```

It exposes `kartog`, map-view-tiles-vector and map-view-tiles-raster as `api`. There is no dependency-injection container.

## A vector basemap

```kotlin
KartogMap(state, layers = listOf(vectorBasemap(streets, client)))
```

`vectorBasemap(source, client, style = null, labels = true, rasterize = vectorTilesNeedRasterizing())` fetches each tile through the client, decodes it off the main thread with map-view's decoder, and draws it with map-view's renderer: the ground first, then place and road names in a layer of their own, so every name sits over every tile. Tiles are decoded once, as they arrive, and a tile the source lacks is drawn from its nearest ancestor.

The source is expected to use the OpenMapTiles schema, as map-view's style is written for it: `water`, `landcover`, `landuse`, `park`, `building`, `transportation` and the rest.

## Light and dark

```kotlin
VectorStyleProvider(systemVectorStyle()) {
    KartogMap(state, layers = listOf(vectorBasemap(streets, client)))
}
```

- **`KartogStyles.Light`** is map-view's own default style, a daytime map. **`KartogStyles.Dark`** is the same style in `MapPalette.Dark`: roads as a brightness ramp of warm greys with no pure white, the one warm accent on motorways, cool water and near-black land.
- **`systemVectorStyle(light, dark)`** follows the system's dark mode as it changes. **`VectorStyleProvider(style)`** provides a style to every basemap inside it that is not given one, the way a theme provides colours; `LocalVectorStyle` reads it. A basemap given `style` uses that instead.
- **A palette changes colour and nothing else.** `VectorStyle.recolored(palette)` keeps map-view's road widths that grow with zoom, the zooms at which casings and building outlines appear, and the paint order. A class map-view leaves undrawn stays undrawn.
- **Your own palette:** copy `MapPalette.Dark` or `MapPalette.Light` and change what you need. `MapPalette` names every colour: land, water, greenery, bare ground, built-up land, buildings and their outlines, airports, boundaries, label ink, halo and water labels, a `RoadColors` (casing and core) for each road tier from motorway to service, and paths, rail and ferries.
- **Build a recoloured style once and keep it.** A style is compared by its lookups, and `recolored` makes new ones on every call, so a style rebuilt inside a composition looks new on every frame and throws away the decoded tiles. `KartogStyles` holds the built ones; keep your own in a top-level `val`.
- **Not yet:** dashed footpaths, and tunnels and bridges drawn apart from other roads. map-view's road paint sees only a road's class; both are requests to map-view.

## When the GPU cannot draw the paths

Some GPU drivers crash the app when Compose draws complex paths. On the Pixel 10 (Tensor G5, PowerVR DXT-48-1536), drawing a street grid with `drawPath` on Vulkan kills the render thread with a segmentation fault.

- **`vectorTilesNeedRasterizing()`** is true only on Android devices known to have the fault, and false on every other target. A device forced onto the OpenGL renderer (`debug.hwui.renderer=skiagl`) does not have it, so is false too.
- **`vectorBasemap` handles it by itself.** On such a device it draws each tile's ground to an image on the CPU and shows it with a raster slot. Names are still drawn from the decoded tile, since text does not go through the path renderer.
- **`rasterizeVectorTile(bytes, zoom, density, style)`** is the step on its own: a tile drawn to an `ImageBitmap` at the size a tile is shown, 256 dp. It costs tens of milliseconds a tile, so call it from a tile state's load, off the main thread.
- **An image is a picture.** Zoomed past its level, it magnifies like any raster tile, where a vector tile stays sharp. That is the price of not crashing.

## On an image

`staticVectorLayer(zoomRange, style) { key -> bytes }` is the same basemap for kartog-static's `renderMap`, drawn by the same renderer so an image matches the live map. Names are not drawn on images yet: map-view draws them only inside a composition.
