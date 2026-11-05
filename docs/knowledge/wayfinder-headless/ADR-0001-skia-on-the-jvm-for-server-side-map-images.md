# Skia on the JVM for Server-Side Map Images

ADR-0001 · 2026-11-04 · Status: accepted
Keywords: static map image on a server, render a map without a browser, headless Chrome screenshot, Puppeteer, hosted static maps API, stitch tiles by hand, Java2D, why JVM only, why no Windows, why bundle native libraries, Skiko runtime, map image in an email

## Context

A server often needs a picture of a map: a trip's route in a confirmation email, a preview image for a shared link, a thumbnail saved with a record. The usual ways to get one each cost something. A headless browser, taking a screenshot of a web map, is a large dependency with slow starts and a lot of memory per image. A hosted static-maps API is a per-image cost, sends every route to a third party, and draws in its own style rather than the app's. Stitching tiles together by hand means writing a second renderer that draws routes and vector tiles differently from the app.

wayfinder already draws maps without a composition: `wayfinder-frame`'s `renderMap` draws layers and an overlay into an image through Compose's graphics and Skia, on every target. On the JVM, Skia is a native library that Skiko loads at runtime, and a JVM app normally gets it from Compose for desktop. A server has no Compose desktop setup and doesn't want one.

## Decision

Server-side map images are a separate module, `wayfinder-headless`, which renders through the same `renderMap` the devices use and bundles Skia's native libraries itself.

- **It draws with Skia, through `wayfinder-frame`.** A server image uses the same styles, line drawing and markers as the app, with no second renderer to keep in step.
- **It is JVM only.** Bundling a native library only makes sense where it is loaded at runtime from the classpath, which is the JVM. Android, iOS and the browser get Skia from the platform or from Compose, and use `wayfinder-frame` directly.
- **It bundles Skiko's runtime for Linux and macOS, on x64 and arm64,** as `runtimeOnly` dependencies, so a server needs nothing from Compose for desktop. Linux is for servers and containers, and macOS for running the same code on a developer's machine. Windows is not bundled.
- **It is one call,** `renderMapImage`, with the camera fitted and the markers chosen for the common case. A caller who wants more control uses `wayfinder-frame`, which this module exposes.
- **The tiles come through a function the caller passes,** so the module makes no network requests and holds no URLs, keys or cache.

## Consequences

- A Kotlin server gets a map image from one dependency and one call, with no browser, no external service and no per-image cost.
- Images from the server look like the app's maps, because they are drawn by the same code.
- Every app that depends on the module carries Skia's library for all four platforms, about 57 MB of jars in Skiko 0.150.2, of which a machine loads one. The alternative, asking each server to add the one artifact for its platform, makes a wrong guess about the deployment machine fail only in production.
- The server still has to provide what Skia links against on Linux, and a font for names. [Running on a Server](DOC-0001-running-on-a-server.md) lists it all.
- It does not run on Windows. A Windows server would need Skiko's Windows runtime added by the app.
- The bundled Skiko version has to match the one Compose Multiplatform brings, and moves with every Compose update.
- `renderMapImage` takes a Compose `Dp` and is a `suspend` function, so it can only be called from Kotlin.
