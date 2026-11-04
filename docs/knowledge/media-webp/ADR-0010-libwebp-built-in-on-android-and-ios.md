# libwebp Built In on Android and iOS

ADR-0010 · 2026-11-04 · Status: accepted
Keywords: why not Bitmap.compress WEBP, WEBP_LOSSY WEBP_LOSSLESS API 30, quality 100 lossless on some Android versions, Bitmap.compress returns no data, device maker libwebp, iOS has no WebP encoder, ImageIO WebP, Safari canvas toBlob webp, why bundle libwebp, prebuilt native libraries committed, no NDK for consumers, Skia WebP on JVM, lossless WebP in Kotlin, why raw RGBA pixels

## Context

An app that saves or uploads WebP wants the same file from the same pixels on every device. The platforms do not give it that.

- **Android.** `Bitmap.compress` with a WebP format has been part of Android since API 14, but what it produces depends on the release and the device. Quality 100 meant lossless only on some versions, the explicit lossy and lossless formats arrived in API 30, and the libwebp inside is whichever version the device maker shipped. On some API 30+ devices it has returned no data at all, without an error.
- **iOS** has no WebP encoder an app can rely on.
- **The JVM and the web** have Skia wherever Compose Multiplatform runs, and Skia's WebP encoder is libwebp. Skia offers no lossless WebP.

## Decision

- **Android and iOS encode with a libwebp built into this library,** v1.6.0, vendored from upstream and compiled into `libwebp_jni.so` for each Android ABI and `libwebp.a` for each iOS target, for both lossy and lossless.
- **The native libraries are built by a script and committed,** not built by Gradle, so a consumer needs no NDK, CMake or network access.
- **On Android, only the JNI entry point is exported,** so a second libwebp in the same app, inside an image loader for instance, cannot be bound in place of this one.
- **The JVM and the web encode lossy with Skia's libwebp,** which a Compose app already carries, so nothing native is bundled for them.
- **Lossless on the JVM and the web is a small encoder in Kotlin,** because Skia has none.
- **The encoder takes raw RGBA pixels,** `RgbaPixels`, rather than a platform image type, so it is the same call on every target and needs no UI toolkit. Each platform's image type converts to it.

## Consequences

- The same encoder and the same settings on every Android API level the library supports, instead of whatever the device ships, and a failure that throws instead of an empty file.
- Each Android ABI carries about 250 to 470 KB of native code, before ABI splitting.
- On iOS, another static libwebp in the same binary can clash with this one at link time.
- Updating libwebp is a maintainer step on macOS with Xcode and the Android NDK; [The Vendored libwebp](DOC-0019-the-vendored-libwebp.md) describes it.
- On the JVM and the web, lossy output depends on the libwebp in the Skia version the app brings, and needs Skia's native library or WebAssembly at run time.
- Lossless files from the JVM and the web are larger than libwebp's, because the Kotlin encoder has no spatial prediction or backward references. Every pixel still comes back exactly.
