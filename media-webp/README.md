# media-webp

WebP encoding for Kotlin Multiplatform: lossy or lossless, with GPS position and capture time written into the file, and the same call on every target. For photos an app uploads, map images it saves, and anything else that should be small and keep where and when it was made.

**Targets:** JVM · Android · iOS (arm64, simulator) · JS · wasmJs

## Install

```kotlin
implementation("com.oddlyclever:media-webp:0.0.3")
```

There is no dependency-injection container and nothing to initialise. On the JVM and the web it uses Skia, which a Compose app already has; see [JVM and web](#jvm-and-web).

## Encode

```kotlin
val webp: ByteArray = bitmap.encodeToWebp(                  // Android Bitmap, iOS UIImage, or RgbaPixels anywhere
    quality = 80,
    metadata = WebpMetadata(
        location = GpsLocation(latitude = 45.4215, longitude = -75.6972),
        capturedAt = Clock.System.now(),
        software = "Acme Camera",
    ),
)
```

| Call | What it encodes |
|---|---|
| `RgbaPixels(width, height, bytes, rowBytes).encodeToWebp(…)` | raw pixels, four bytes each in R, G, B, A order, alpha not premultiplied; every target |
| `Bitmap.encodeToWebp(…)` / `Bitmap.toRgbaPixels()` | an Android bitmap of any config, hardware bitmaps included |
| `UIImage.encodeToWebp(…)` / `UIImage.toRgbaPixels()` | an iOS image, at its full pixel size and turned upright as it is shown |
| `BufferedImage.toRgbaPixels()` | a JVM image |
| `webp.withWebpMetadata(metadata)` | adds or replaces the metadata of a WebP from any encoder, leaving the image as it is |

| Parameter | Default | |
|---|---|---|
| `quality` | `DEFAULT_WEBP_QUALITY`, 80 | 0 to 100 for lossy; higher is larger and closer to the original |
| `compression` | `WebpCompression.Lossy` | `Lossless` keeps every pixel exactly, for screenshots, line art and anything re-encoded later |
| `metadata` | none | written as EXIF; nothing is written when it is null or empty |

- **A failure throws.** The encoder refusing an image is a `WebpException`, and a quality outside 0..100 or pixels that do not fit their size an `IllegalArgumentException`. It never returns an empty or cut-short file, which is how Android's own WebP encoder has been seen to fail.
- **It is CPU-bound,** tens of milliseconds for a photo, so call it off the main thread.
- **A WebP is at most 16383 pixels a side.** Larger images are refused when the `RgbaPixels` is made.

## Metadata

`WebpMetadata(location, capturedAt, software)` is written as an EXIF chunk:

| Field | EXIF tags |
|---|---|
| `location: GpsLocation(latitude, longitude, altitude?)` | GPS latitude and longitude, to about 3 mm, and altitude above or below sea level when given |
| `capturedAt: Instant` | original date and time in UTC, with a `+00:00` offset and milliseconds; with a location, the GPS date and time stamp too |
| `software: String` | the software tag |

- **Written in Kotlin, the same on every target.** The EXIF and the WebP container around it are built in common code rather than by libwebp's mux library, so the JVM and the web, whose encoder has no mux, write the same metadata as Android and iOS.
- **Only the fields that are set are written,** so a reader finds nothing it would have to second-guess.
- **A file with metadata is an extended WebP:** a `VP8X` header, the image data, then the `EXIF` chunk, before any `XMP `. The EXIF payload starts with its TIFF header, with no `Exif\0\0` prefix, as the WebP container specification has it. The tests check every tag by reading it back, and it has been checked against Pillow's reader.
- **`withWebpMetadata`** rewrites only the container, so it adds metadata to a WebP from a server, from Skia, or from a camera, and `WebpMetadata()` with nothing set removes it.

## How each target encodes

| Target | Lossy | Lossless |
|---|---|---|
| Android | libwebp 1.6.0, built into this library | libwebp |
| iOS | libwebp 1.6.0, built into this library | libwebp |
| JVM | Skia's libwebp | the Kotlin encoder below |
| JS, wasmJs | Skia's libwebp | the Kotlin encoder below |

**Why Android carries its own libwebp.** `Bitmap.compress` with a WebP format has been part of Android since API 14, but what it produces depends on the release and the device. Quality 100 meant lossless only on some versions, the explicit lossy and lossless formats arrived in API 30, and the libwebp inside is whichever version the device maker shipped. On some API 30+ devices it has returned no data at all without an error. A bundled libwebp gives the same encoder and the same settings on every API level the library supports.

**Lossless on the JVM and the web** is written by a small encoder in Kotlin, because Skia offers no lossless WebP. Its output is a standard VP8L file that any decoder reads, and every pixel comes back exactly. It uses the subtract-green transform and Huffman coding but no spatial prediction or backward references, so its files are larger than libwebp's lossless files.

### Android

- **Native code for every ABI:** `arm64-v8a`, `armeabi-v7a`, `x86_64` and `x86`, from 250 to 470 KB each before ABI splitting. Built for 16 KB pages, which Android 15 devices may use.
- **No clash with another libwebp.** An app may carry another libwebp inside an image loader. This library's copy exports only its JNI entry point, so neither can bind to the other's functions.
- **Minification is handled.** The native library registers its methods against `NativeWebp` by name, and the library publishes the keep rule that stops R8 renaming it.
- **Minimum SDK 24,** and tested on an API 24 device.

### iOS

- **Consumed as a Kotlin dependency,** not a CocoaPod or Swift package. `libwebp.a` is embedded in the published cinterop klib and linked into your shared framework, so there is nothing to add in Xcode and no setup call.
- **Another static libwebp in the same binary** can clash with this one at link time; SDWebImageWebPCoder, for instance, carries one. With this library in place, such a coder is no longer needed to encode.

### JVM and web

- **Skia's native library must be present at run time.** A Compose Multiplatform app has it already. A server adds `org.jetbrains.skiko:skiko-awt-runtime-<os>-<arch>` at the Skiko version its Compose brings. On Linux, Skia also needs `libGL.so.1`, `libX11.so.6` and `libEGL.so.1`: the `libgl1`, `libx11-6` and `libegl1` packages on Debian or Ubuntu.
- **On the web, Skia's WebAssembly must have loaded** before the first lossy encode, as it has once a Compose app is showing. Lossless needs no Skia at all.

## The vendored libwebp

libwebp's encoder source is vendored under [`native/libwebp`](native/libwebp), unmodified, from the release recorded in [`native/PROVENANCE`](native/PROVENANCE). The Android and iOS libraries are built from it and committed, so a consumer needs no NDK, CMake or network access.

| Script | What it does |
|---|---|
| [`native/update-source.sh`](native/update-source.sh) `v1.6.0` | replaces the vendored source with that upstream tag and rewrites `PROVENANCE` |
| [`native/build-natives.sh`](native/build-natives.sh) | rebuilds `libwebp_jni.so` for each Android ABI and `libwebp.a` for each iOS target; needs macOS with Xcode and the Android NDK |

Run both, then commit the source, `PROVENANCE`, the libraries and the copied headers together. `NativeWebpTest` checks that the Android library is the version `PROVENANCE` names.

libwebp is Copyright Google Inc., under a BSD 3-Clause licence with an additional patent grant; both are in [`native/libwebp`](native/libwebp) and travel in `META-INF` of every published jar as `LICENSE.libwebp` and `PATENTS.libwebp`. This library's own code is under the Apache License 2.0.
