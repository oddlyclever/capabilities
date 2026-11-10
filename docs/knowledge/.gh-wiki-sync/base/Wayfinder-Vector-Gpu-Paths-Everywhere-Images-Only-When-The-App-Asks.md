# GPU Paths Everywhere, Images Only When the App Asks

ADR-0020 · 2026-11-04 · Status: accepted
Keywords: why no device list, device blocklist, GPU driver crash, segmentation fault drawing paths, Vulkan drawPath crash, Pixel 10, Tensor G5, PowerVR DXT-48-1536, automatic fallback to bitmaps, rasterize vector tiles, why rasterize is off by default, why names are not baked into tile images, CPU rendering

## Context

A vector basemap draws its tiles as paths on the GPU. On the Pixel 10 family (Tensor G5, PowerVR DXT-48-1536), an earlier map renderer drawing a street grid with Compose's `drawPath` on Vulkan had crashed the render thread with a segmentation fault. wayfinder used to switch to images by itself on those devices, drawing each tile on the CPU.

When map-view's own vector drawing was tried on a Pixel 10 Pro XL on Android 17, the fault did not reproduce. The test covered dense street grids at zooms 13 to 20, about 70 fast pans and 22 zooms, and 1,500 frames without a fatal signal.

## Decision

- **Tiles are drawn as paths on the GPU on every device.** The device rule was dropped, and the module keeps no list of devices.
- **Drawing to images stays, as an option the app turns on:** `vectorBasemap(…, rasterize = true)`, and `rasterizeVectorTile` as the step on its own. Which devices get it is the app's decision.
- **Names are never drawn into the image.** They are drawn from the decoded tile in a layer of their own, as without `rasterize`. On a map the user pans, zooms and turns, a name baked into a tile would be clipped at its edge, blur when the tile is magnified, and turn with the map.

## Consequences

- Every device gets sharp tiles at every zoom, and none pays the cost of drawing tiles on the CPU unless the app asks.
- If a GPU driver fault on complex paths appears on some device, an app can work around it with `rasterize` without waiting for a driver update.
- Knowing which devices need it is the app's job, from what the app has seen on them.
- A rasterized tile costs tens of milliseconds on the CPU and blurs when magnified past its level. [Drawing Tiles as Images](Wayfinder-Vector-Drawing-Tiles-As-Images) covers the costs.
