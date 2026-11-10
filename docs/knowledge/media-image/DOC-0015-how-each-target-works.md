# How Each Target Works

DOC-0015 · 2026-11-04
Keywords: BitmapFactory inSampleSize, out of memory decoding a large photo, ImageIO thumbnail, CGImageSourceCreateThumbnailAtIndex, Skia decode on the JVM, canvas resize in the browser, toBlob, Safari WebP, AppContext, App Startup, InitializationProvider removed, IllegalStateException application context, ImageProcessor(context), skiko-awt-runtime missing, libGL.so.1, HEIC on the JVM, cross-origin image tainted canvas, SecurityError, Skia WebAssembly not loaded, WebP fails in the browser, compress from bytes in memory, CGImageSourceCreateWithData, ByteArrayInputStream, object URL, readSource memory

Each target decodes and resizes with what the platform has, and encodes WebP with media-webp. This page covers what each one uses, what it needs from the app, and where it differs.

## At a glance

| Target | Decodes and resizes with | WebP | JPEG and PNG |
|---|---|---|---|
| Android | `BitmapFactory`, subsampling while decoding so a large photo never sits in memory whole, turned upright from ExifInterface's orientation | media-webp, libwebp built in | `Bitmap.compress` |
| iOS | ImageIO, which decodes, shrinks and turns upright in one pass, so a large photo is never decoded whole | media-webp, libwebp built in | UIKit |
| JVM | Skia, which also reads WebP sources and turns them upright as it decodes | media-webp, Skia | Skia |
| JS, wasmJs | an `<img>`, which the browser turns upright, and an offscreen `<canvas>` | media-webp, Skia | the canvas |

WebP goes through media-webp everywhere rather than through each platform's encoder; [WebP Through media-webp on Every Target](ADR-0009-webp-through-media-webp-on-every-target.md) says why.

Given bytes rather than a location, every target feeds the same decoder from memory, so the result is the same either way. What differs is memory: the location form streams a large photo on Android and iOS, and the bytes form has the whole file in memory from the start. `readSource` reads the whole file on every target.

## Android

- **Decoding** reads the image's bounds first, then decodes it subsampled by a power of two, so a 50-megapixel photo never sits in memory whole, then scales it precisely and turns it upright in one pass. `BitmapFactory` ignores the EXIF orientation, so the orientation is read separately with ExifInterface.
- **The source is opened more than once,** for the bounds, the pixels and the EXIF, so it must be something that can be read again: a file or a `content://` URI, not a one-shot stream.
- **`ImageProcessor()` reads the application context from `AppContext`,** which [platform](../platform/README.md) captures at start-up through App Startup, so there is nothing to pass and no dependency-injection container is involved. An app that removes App Startup's `InitializationProvider` from its merged manifest gets an `IllegalStateException` from the constructor saying so. `ImageProcessor(context)`, which exists only in Android code, takes a context explicitly.
- **A relative path is not a file here.** Only a path starting with `/` is treated as a file; anything else is parsed as a URI, so a relative path fails as `SourceUnreadable`.
- **JPEG and PNG** come from `Bitmap.compress`, PNG at full quality.
- **From bytes,** each of the three reads is a fresh stream over the array, so the subsampling decoder still never holds the full-size pixels, though the encoded file is in memory throughout.

## iOS

- **Decoding** goes through ImageIO, which decodes, shrinks and stands the photo upright in one pass, from the file, so a 48-megapixel photo is never decoded whole. It is never asked for a size larger than the source, so a small image is not scaled up.
- **A missing file is checked for first,** because UIKit answers a missing file and one that is not an image alike; the first is `SourceUnreadable` and the second `DecodeFailed`.
- **Transparency** is checked in a Core Graphics bitmap context. An image that declares no alpha channel is not scanned.
- **JPEG and PNG** come from UIKit's `UIImageJPEGRepresentation`, at `quality / 100`, and `UIImagePNGRepresentation`.
- **From bytes,** ImageIO reads the image from memory with the same single pass, so a large photo is still never decoded whole.

## JVM

- **Skia's native library must be on the classpath at run time.** A Compose desktop app has it already; anything else adds `org.jetbrains.skiko:skiko-awt-runtime-<os>-<arch>` at the Skiko version its Compose brings. Without it, nothing decodes. On Linux, Skia also needs `libGL.so.1`, `libX11.so.6` and `libEGL.so.1`, from the `libgl1`, `libx11-6` and `libegl1` packages on Debian or Ubuntu.
- **Skia reads JPEG, PNG, WebP, GIF and BMP,** so a WebP can be resized as well as written. A HEIC file fails as `DecodeFailed` here, while iOS's ImageIO decodes it.
- **Resizing** draws with mipmapped sampling, so a large reduction averages the pixels it drops instead of aliasing them.
- **The whole source file is read into memory** before decoding, so a location and its bytes cost the same here.

## Web

- **The source must come from this origin.** A cross-origin image loads, but taints the canvas, and the browser then refuses to give its pixels back. WebP and PNG then fail as `EncodeFailed`, and JPEG as `AlphaNotRepresentable`, because pixels that cannot be checked are treated as possibly transparent rather than risk flattening them. Load it from this origin, or as a `blob:` URL.
- **Skia's WebAssembly must have loaded** before WebP is encoded, as it has once a Compose app is showing. Before that, WebP fails as `EncodeFailed`, and `compressImageWithFallback` moves on to JPEG.
- **JPEG and PNG** come from the canvas's `toBlob`. A browser that cannot write the type asked for silently writes PNG instead, so the type that came back is checked, and a mismatch is `Unsupported` rather than PNG bytes reported as something else.
- **A missing source and one that is not an image** are the same error from an `<img>`, so both arrive as `SourceUnreadable`.
- **It runs on the page's thread,** so a large image holds up the page while it is processed.
- **From bytes,** the image goes to the same `<img>` through an object URL the page makes and revokes itself, so it is same-origin and never taints the canvas. The URL is typed as what `imageKindOf` finds, which an `<img>` needs to draw an SVG, and the metadata is read from the bytes in hand rather than fetched again. Bytes the `<img>` cannot load arrive as `DecodeFailed`.
- **`readSource` fetches the source** as the page would, so it reads the same `blob:`, `data:` and same-origin URLs, and a cross-origin one only where the server allows it.
