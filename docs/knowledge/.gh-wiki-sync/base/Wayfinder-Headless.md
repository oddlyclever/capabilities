# wayfinder-headless

The full documentation of [`wayfinder-headless`](https://github.com/oddlyclever/capabilities/blob/HEAD/wayfinder-headless/README.md): map images on a server in one call, JVM only. Its README covers installing it and a first use, and its KDoc is the API reference; this section covers the rest.

## Using it

- [Running on a Server](Wayfinder-Headless-Running-On-A-Server): the platforms Skia is bundled for, what the machine has to provide, a container image, and what each failure looks like.
- [Rendering Options](Wayfinder-Headless-Rendering-Options): every parameter of `renderMapImage`, how the camera and markers are chosen, WebP or PNG, what the call refuses, and concurrency.
- [Tile Sources](Wayfinder-Headless-Tile-Sources): what the tile function returns, images or vector tiles, what happens when a tile fails, and caching.

## Why it is built this way

- [Skia on the JVM for Server-Side Map Images](Wayfinder-Headless-Skia-On-The-Jvm-For-Server-Side-Map-Images): why it draws through Skia, why it is JVM only, and why it bundles the native libraries.

A new page about this module alone goes here, numbered like every other document in the knowledge base. Knowledge that spans several modules goes in the shared sections instead.
