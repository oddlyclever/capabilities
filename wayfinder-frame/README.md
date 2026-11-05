# wayfinder-frame

Map frames for [wayfinder](../wayfinder). A frame here is meant in the cartographic sense, as in a print layout's map frame: a map shown from one camera position, at one size, deliberately composed. It is not an animation frame, and not a container around the interactive map; that is `WayfinderMap`.

A frame is drawn without a composition: to an image for a share sheet, a notification or an upload; to a `Painter` that scales like a vector image; or as a `MapFrame` in a layout. Images are encoded as PNG or WebP. On a server, [wayfinder-headless](../wayfinder-headless) wraps it in one call.

**Targets:** JVM · Android · iOS (arm64, simulator) · JS · wasmJs

## Install

```kotlin
implementation("com.oddlyclever:wayfinder-frame:0.0.7")
```

It depends on `wayfinder`, and on [`media-webp`](../media-webp) for encoding. Layers come from the module that draws them, `vectorFrameLayer` in wayfinder-vector or `rasterFrameLayer` in wayfinder-raster, so an app takes only the one it uses. There is no dependency-injection container.

## A map as an image

```kotlin
val size = IntSize(1200, 800)
val density = Density(2f)
val image: ImageBitmap = renderMap(
    size = size,
    density = density,
    position = fit(route, size, density, padding = 24.dp),
    layers = listOf(vectorFrameLayer(streets.zoomRange, MapStyles.Light) { key -> streets.fetch(client, key) }),
    background = MapStyles.Light.background,
) { projection ->
    drawTrack(track, projection)                   // from com.oddlyclever.wayfinder.track
}
val webp: ByteArray = image.encodeToWebp()
```

The tiles are laid out and drawn as the live map would draw them, every tile is fetched at once, and the overlay is drawn upright through a projection that places coordinates on the image. Pass the style's `background`: a tile that fails is never an error, and with the default transparent background a map whose tiles all failed comes out transparent.

## A map in a layout

```kotlin
val layers = remember { listOf(vectorFrameLayer(streets.zoomRange) { key -> streets.fetch(client, key) }) }
MapFrame(
    layers = layers,
    modifier = Modifier.fillMaxWidth().aspectRatio(3f / 2f),
    fit = route,                                   // or position = CameraPosition(center, zoom)
    contentDescription = "Map of the route",
    placeholder = { Shimmer() },
) { projection ->
    drawTrack(track, projection)
}
```

A map that does not move, for a card, a list row or a thumbnail. It renders at the size it is laid out and again when that changes. Keep the layers in a `remember`: a new list of layers starts the render again and fetches every tile again.

## Documentation

The full documentation is in the module's section of the [knowledge base](../docs/knowledge/wayfinder-frame/README.md), which is mirrored to the repository's wiki:

- [Rendering a Map Image](../docs/knowledge/wayfinder-frame/DOC-0031-rendering-a-map-image.md): `renderMap` and `prepareMap`, the camera, the overlay, and drawing a map at any size.
- [Layers and Tiles](../docs/knowledge/wayfinder-frame/DOC-0032-layers-and-tiles.md): the frame layers, names, missing tiles, and a layer of your own.
- [MapFrame in a Layout](../docs/knowledge/wayfinder-frame/DOC-0033-mapframe-in-a-layout.md): the composable, when it renders, and why it can wait for ever.
- [Encoding PNG and WebP](../docs/knowledge/wayfinder-frame/DOC-0034-encoding-png-and-webp.md): the formats, WebP quality and metadata, and failures.
- [Platform Notes](../docs/knowledge/wayfinder-frame/DOC-0035-platform-notes.md): what each platform needs, including Skia on a server.
- [Frame Layers and Projection Live in the Base Module](../docs/knowledge/wayfinder-frame/ADR-0017-frame-layers-and-projection-live-in-the-base-module.md): why the module is shaped the way it is.

## For coding agents

The module ships an [Agent Skill](https://agentskills.io/specification), `com-oddlyclever-wayfinder-frame`, for the agents of projects that depend on it: what the library is for, how it's meant to be called, and the mistakes that compile. It is written at [src/commonMain/skills/com-oddlyclever-wayfinder-frame/SKILL.md](src/commonMain/skills/com-oddlyclever-wayfinder-frame/SKILL.md) and published inside every sources jar, at `commonMain/skills/com-oddlyclever-wayfinder-frame/`.
