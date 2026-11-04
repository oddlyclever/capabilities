# media-webp

WebP encoding for Kotlin Multiplatform: lossy or lossless, with GPS position and capture time written into the file, and the same call on every target. For photos an app uploads, map images it saves, and anything else that should be small and keep where and when it was made.

**Targets:** JVM · Android · iOS (arm64, simulator) · JS · wasmJs

## Install

```kotlin
implementation("com.oddlyclever:media-webp:0.0.7")
```

There is no dependency-injection container and nothing to initialise. Android and iOS encode with a libwebp built into this library. On the JVM and the web it uses Skia, which a Compose app already has; anything else adds Skia's native library itself, as [Platform Notes](../docs/knowledge/media-webp/DOC-0018-platform-notes.md) describes.

## Encode

```kotlin
val webp: ByteArray = bitmap.encodeToWebp(                  // Android Bitmap, iOS UIImage, or RgbaPixels anywhere
    quality = 80,
    metadata = WebpMetadata(
        location = GpsLocation(latitude = 51.5007, longitude = -0.1246),
        capturedAt = Clock.System.now(),
        software = "Acme Camera",
    ),
)
```

On the JVM, `bufferedImage.toRgbaPixels().encodeToWebp()`. Pass `compression = WebpCompression.Lossless` to keep every pixel exactly. A failure throws a `WebpException` or an `IllegalArgumentException`; it never returns an empty file. Encoding is CPU-bound, so call it off the main thread.

`webp.withWebpMetadata(metadata)` adds or replaces the metadata of a WebP from any encoder, and `metadata.toExif()` gives the same tags as a bare EXIF block for a JPEG or a PNG.

## Documentation

The full documentation is in the module's section of the [knowledge base](../docs/knowledge/media-webp/README.md), which is mirrored to the repository's wiki:

- [Encoding a WebP](../docs/knowledge/media-webp/DOC-0016-encoding-a-webp.md): every call and parameter, straight-alpha pixels, how each target encodes, and what throws.
- [Writing Metadata](../docs/knowledge/media-webp/DOC-0017-writing-metadata.md): the EXIF tags, the file layout, and metadata on a WebP from somewhere else.
- [Platform Notes](../docs/knowledge/media-webp/DOC-0018-platform-notes.md): Android ABIs and R8, iOS linking, and Skia on the JVM and the web.
- [The Vendored libwebp](../docs/knowledge/media-webp/DOC-0019-the-vendored-libwebp.md): maintainer notes on updating libwebp and rebuilding the native libraries.
- [libwebp Built In on Android and iOS](../docs/knowledge/media-webp/ADR-0010-libwebp-built-in-on-android-and-ios.md) and [Metadata Written in Kotlin](../docs/knowledge/media-webp/ADR-0011-metadata-written-in-kotlin.md): why the module is built the way it is.

## For coding agents

The module ships an [Agent Skill](https://agentskills.io/specification), `com-oddlyclever-media-webp`, for the agents of projects that depend on it: what the library is for, how it's meant to be called, and the mistakes that compile. It is written at [src/commonMain/skills/com-oddlyclever-media-webp/SKILL.md](src/commonMain/skills/com-oddlyclever-media-webp/SKILL.md) and published inside every sources jar, at `commonMain/skills/com-oddlyclever-media-webp/`.

## Licences

libwebp's encoder source is vendored under [`native/libwebp`](native/libwebp), unmodified, from the release recorded in [`native/PROVENANCE`](native/PROVENANCE), and the Android and iOS libraries built from it are committed.

libwebp is Copyright Google Inc., under a BSD 3-Clause licence with an additional patent grant; both are in [`native/libwebp`](native/libwebp) and travel in `META-INF` of every published jar as `LICENSE.libwebp` and `PATENTS.libwebp`. This library's own code is under the Apache License 2.0.
