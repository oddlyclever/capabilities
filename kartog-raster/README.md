# kartog-raster

Raster tiles for [kartog](../kartog): a raster basemap for `KartogMap`, tile bytes decoded to images off the main thread, and a raster slot that fades each tile in as it arrives.

```kotlin
KartogMap(state, layers = listOf(rasterBasemap(imagery, client)))
```

`staticRasterLayer` is the same for kartog-static.

**Targets:** JVM · Android · iOS (arm64, simulator) · JS · wasmJs

## Install

```kotlin
implementation("com.oddlyclever:kartog-raster:0.0.3")
```

It exposes `kartog` and map-view-tiles-raster as `api`. There is no dependency-injection container.

## Use

```kotlin
val satellite = rememberTileState(key = imagery, zoomRange = imagery.zoomRange) { key ->
    decodeRasterTile(http.get(imagery.url(key)))
}

MapView(camera, Modifier.fillMaxSize().mapGestures(camera)) {
    layer(satellite) { key -> satellite.shown(key)?.let { FadingRasterSlot(it.content, it.src) } }
}
```

- **`decodeRasterTile`** decodes on `Dispatchers.Default` whatever thread calls it, so call it from the tile state's `load` and each tile is decoded once, as it arrives. A missing, empty, truncated or non-image answer is null, a missing tile, so the map shows the nearest ancestor instead of failing.
- **`FadingRasterSlot`** is map-view's `RasterSlot` with a fade. Only a tile arriving after its cell showed nothing fades in; a cell composed with its tile already in hand draws it at once, so tiles you pan back over do not flicker in again. `fadeMillis = 0` turns the fade off.
- **Formats:** PNG and JPEG everywhere, WebP wherever the platform's image decoder reads it.
