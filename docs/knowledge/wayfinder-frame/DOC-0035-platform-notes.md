# Platform Notes

DOC-0035 · 2026-11-04
Keywords: renderMap on a server, LibraryLoadException, libGL.so.1, libX11.so.6, libEGL.so.1, skiko-awt-runtime, Skia native library missing, Compose desktop, Java 21, UnsupportedClassVersionError, Android main thread, TextMeasurer needs a context, iOS, Kotlin/JS, wasmJs, Skia WebAssembly not loaded, awaitSkiko, Swift, JavaScript API, binaries.executable, webpack

`wayfinder-frame` runs on the JVM, Android, iOS (arm64 and the arm64 simulator), and JS and wasmJs in the browser. It is called from Kotlin only: there is no Swift or JavaScript surface. This page covers what each platform needs and where they differ.

## The JVM

Drawing goes through Skia, which on the JVM is a native library that Skiko loads at run time. A Compose for desktop app already has it. Any other JVM process, such as a server, has to provide it:

- **Skia's native library for the machine** on the runtime classpath, such as `org.jetbrains.skiko:skiko-awt-runtime-linux-x64` or `-linux-arm64`, at the Skiko version Compose Multiplatform brings: 0.150.2 with Compose 1.12.2. Without it, the first render fails with Skiko's `LibraryLoadException`.
- **The system libraries Skia links against**, on Linux: `libGL.so.1`, `libX11.so.6` and `libEGL.so.1`. They are needed even though nothing is shown and there is no display. On Debian or Ubuntu they are the packages `libgl1`, `libx11-6` and `libegl1`. Without them, the first render fails with a `LibraryLoadException` naming the missing library.
- **Java 21 or later,** since map-view's classes are built for it.

For map images on a server, [wayfinder-headless](../wayfinder-headless/README.md) wraps `renderMap` in one call and bundles Skia for Linux and macOS, so the process needs only Java 21, the system libraries and a font. Its section covers a container image and every failure in detail.

On the JVM, a `TextMeasurer` for names can be built outside a composition, from `createFontFamilyResolver()`. Names are drawn in the machine's fonts, so a machine with none installed draws none.

## Android

- `TextMeasurer` takes a context, so names need one from the app, such as `rememberTextMeasurer()` in a composable.
- Drawing an image and encoding it are blocking CPU work. Call `renderMap`, `toImageBitmap`, `encodeToPng` and `encodeToWebp` from `Dispatchers.Default` rather than the main thread for anything larger than a thumbnail.
- PNG is encoded by `Bitmap.compress`. WebP is encoded by the libwebp built into media-webp, not by the device's own encoder.

## iOS

PNG is encoded by Skia, and WebP by the libwebp built into media-webp.

## JS and wasmJs

Skia runs as WebAssembly in the browser, and PNG and WebP are both encoded by it. On JS, Skia's WebAssembly module loads in the background after the page starts, and decoding or drawing an image before it is ready fails on a missing function. The module's own JS tests wait for Skiko's `awaitSkiko` promise before drawing for this reason.

## For maintainers

- **Both web targets declare `binaries.executable()`.** Since Compose 1.12, a browser test with Skiko on its classpath must be bundled by webpack to load, and the build refuses to run the tests without it. The published klibs and Gradle metadata are the same with and without it.
- **The JVM tests depend on `compose.desktop.currentOs`**, which brings Skia's native library for the machine running them.
- **The tests call `awaitSkia()` before drawing.** It is an `expect` function that waits for Skiko's WebAssembly on JS and returns at once on every other target.
- **`encodeToPng` has an `actual` for each Skia target,** the JVM, iOS and the web, with the same lines in each, because Skia's classes are not visible from common code. Android's uses `Bitmap.compress`.
