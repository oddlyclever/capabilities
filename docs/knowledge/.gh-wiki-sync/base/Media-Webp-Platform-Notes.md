# Platform Notes

DOC-0018 · 2026-11-04
Keywords: libwebp_jni.so, UnsatisfiedLinkError, NativeWebp, R8 keep rule, ProGuard, minification, 16 KB page size, Android 15, ABI split, APK size, arm64-v8a armeabi-v7a x86_64 x86, minSdk 24, duplicate libwebp, image loader with its own libwebp, iOS duplicate symbol link error, CocoaPod, Swift package, cinterop klib, skiko-awt-runtime, Skia native library missing, libGL.so.1, Skia WebAssembly not loaded, Compose desktop, server encoding WebP

What each target brings, what an app has to provide, and the clashes to know about.

## Android

- **Native code for every ABI:** `arm64-v8a`, `armeabi-v7a`, `x86_64` and `x86`, from about 250 to 470 KB each before ABI splitting. The libraries are built for 16 KB pages, which Android 15 devices may use, and every symbol is resolved when they are built rather than on a device.
- **No clash with another libwebp.** An app may carry another libwebp, inside an image loader for instance. This library's copy exports only its JNI entry point and keeps libwebp's own functions hidden, so neither library can bind to the other's.
- **Minification is handled.** The native library registers its methods against `com.oddlyclever.media.webp.NativeWebp` by name when it loads, and the module publishes the consumer keep rule that stops R8 renaming that class or its native methods. The app adds nothing.
- **Minimum SDK 24,** the repository's minimum.
- **There is no host test.** The encoder is a native library that loads only on a device, so the module's common tests run on a device or emulator, as an Android device test.

## iOS

- **Consumed as a Kotlin dependency,** not a CocoaPod or a Swift package. `libwebp.a` is embedded in the published cinterop klib and linked into the app's shared framework, so there is nothing to add in Xcode and no setup call.
- **Another static libwebp in the same binary** can clash with this one at link time, for instance one carried by an image library's WebP coder. With this library in place, such a coder is no longer needed to encode.

## JVM

- **Skia's native library must be present at run time.** The module depends on Skiko's classes only. A Compose Multiplatform desktop app has the native library already. A server or any other JVM app adds `org.jetbrains.skiko:skiko-awt-runtime-<os>-<arch>` at the Skiko version its Compose brings; this module is built against Skiko 0.150.2, the version Compose Multiplatform 1.12.2 brings.
- **On Linux, Skia links against `libGL.so.1`, `libX11.so.6` and `libEGL.so.1`,** even with no display: the `libgl1`, `libx11-6` and `libegl1` packages on Debian or Ubuntu.
- **Only lossy needs Skia.** Lossless is the Kotlin encoder, which runs without it.

## Web

- **Skia's WebAssembly must have loaded** before the first lossy encode, as it has once a Compose app is showing. It loads asynchronously, so an encode at start-up, before Compose is up, can run ahead of it.
- **Lossless needs no Skia at all.**
- **There is no worker thread** in the call: a large image holds up the page while it encodes.

## Licences in the published artifacts

The Android and iOS artifacts carry libwebp in object form. libwebp's BSD licence and Google's patent grant go into `META-INF` of every jar the module publishes, the sources jar beside each klib included, as `LICENSE.libwebp` and `PATENTS.libwebp`. The pom lists libwebp's BSD 3-Clause licence beside the module's own Apache License 2.0.
