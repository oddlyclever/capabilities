# wayfinder-frame

The full documentation of [`wayfinder-frame`](https://github.com/oddlyclever/capabilities/blob/HEAD/wayfinder-frame/README.md): map frames, a map drawn at one camera position and size without a composition, as an image, a painter or a `MapFrame` in a layout. Its README covers installing it and a first use, and its KDoc is the API reference; this section covers the rest. For map images on a server in one call, see [wayfinder-headless](Wayfinder-Headless), which is built on this module.

## Using it

- [Rendering a Map Image](Wayfinder-Frame-Rendering-A-Map-Image): `renderMap` and `prepareMap`, size and density, the camera, the overlay and its projection, drawing a prepared map at any size, what the calls refuse, and threads.
- [Layers and Tiles](Wayfinder-Frame-Layers-And-Tiles): the vector and raster frame layers, names, which tiles a frame asks for, what happens when one is missing, caching, and writing a layer of your own.
- [MapFrame in a Layout](Wayfinder-Frame-Mapframe-In-A-Layout): the composable's parameters, when it renders, why it can wait for ever, and why its layers must be remembered.
- [Encoding PNG and WebP](Wayfinder-Frame-Encoding-Png-And-Webp): the two formats, WebP quality and EXIF metadata, and what each encoder refuses.
- [Platform Notes](Wayfinder-Frame-Platform-Notes): what the JVM, Android, iOS and the browser each need, and notes for maintainers on the build and tests.

## Why it is built this way

- [Frame Layers and Projection Live in the Base Module](Wayfinder-Frame-Frame-Layers-And-Projection-Live-In-The-Base-Module): why the layer and projection interfaces are in `wayfinder`, so the renderer depends on neither tile module, and why names are drawn in a second pass.

A new page about this module alone goes here, numbered like every other document in the knowledge base. Knowledge that spans several modules goes in the shared sections instead.
