# Drawing Tiles as Images

DOC-0043 · 2026-11-04
Keywords: rasterize, rasterizeVectorTile, GPU driver crash, segmentation fault drawing paths, SIGSEGV render thread, Vulkan drawPath crash, Pixel 10, Tensor G5, PowerVR, CPU rendering, vector tile to bitmap, ImageBitmap from MVT, device blocklist, device list, map blurry when zoomed in, slow tile drawing

A vector basemap draws its tiles as paths on the GPU, through whatever renderer the platform uses, on every device. `rasterize` is the way around a GPU driver that cannot be trusted with those paths: each tile's ground is drawn to an image on the CPU, and the image is shown instead. This page covers when to use it, what it costs, and the step on its own.

## Turning it on

```kotlin
// On Android, for instance: only on devices where this app has seen the GPU driver crash.
val rasterize = Build.MODEL.startsWith("Pixel 10")
WayfinderMap(state, layers = listOf(vectorBasemap(streets, client, rasterize = rasterize)))
```

Nothing turns it on by itself, and the module keeps no list of devices. Which devices get it is the app's decision, made from what the app has seen. Why it is built that way is in [GPU Paths Everywhere, Images Only When the App Asks](Wayfinder-Vector-Gpu-Paths-Everywhere-Images-Only-When-The-App-Asks).

## What changes

With `rasterize = true`, each tile is decoded as usual, then decoded a second time and drawn to an image on the CPU when it arrives, and the map shows the image with map-view's raster slot.

- **Names are not in the image.** They are still drawn from the decoded tile, in their own layer over every tile, as without `rasterize`. Text does not go through the path renderer, and a name baked into a tile would be clipped at its edge, blur when the tile is magnified, and turn with the map.
- **A tile that will not draw to an image falls back to paths.** If the image cannot be made but the tile decoded, that tile is drawn as paths on the GPU, as without `rasterize`.
- **Switching `rasterize` is a new layer**, and fetches every tile again.

## What it costs

- **Time.** Drawing a tile to an image costs tens of milliseconds a tile on the CPU, on top of decoding it. It runs off the main thread, as the tile arrives.
- **Sharpness.** An image is a picture. Zoomed past its level, it magnifies like any raster tile, and its lines grow and blur with it, where a vector tile stays sharp and keeps its roads at the width the camera's zoom asks for.
- **Memory.** Each image is 256 dp square at the screen's density, 768 pixels on a side at density 3, and the layer keeps up to 96 tiles, each now holding its image as well as its decoded paths.

It is a workaround, not a speed-up.

## The step on its own

`rasterizeVectorTile(bytes, zoom, density, style = MapStyles.Light)` is the drawing step, for code that builds its own map-view layer:

- It returns an `ImageBitmap` of the tile drawn by map-view's renderer in `style`, at the size a tile is shown: 256 dp at `density`, rounded up to whole pixels.
- `zoom` is the level the tile was cut at, which decides the road widths it is drawn with.
- It draws the ground only. Show the names with map-view's `VectorLabels` over the image, as over a vector slot, which is what `vectorBasemap` does.
- It runs on `Dispatchers.Default` whatever thread calls it. Call it from a tile state's `load`, upstream of the view, so each tile is drawn once as it arrives.
- It returns null for bytes that are not a tile. An empty byte array is a tile, of open ground, and comes back as an image of the style's background colour.
