# The Vendored libwebp

DOC-0019 · 2026-11-04
Keywords: update libwebp, new libwebp release, re-vendor, PROVENANCE, update-source.sh, build-natives.sh, rebuild native libraries, Android NDK, CMake, Xcode, libwebp.a, libwebp_jni.so, cinterop headers, webp_jni.c, JNI_OnLoad RegisterNatives, rename NativeWebp, NativeWebpTest version check, mux demux not vendored, Skiko version bump, skiko-js-wasm-runtime, maintainer notes

Maintainer notes for `media-webp`: where libwebp comes from, how the native libraries are rebuilt, and what has to move together.

## What is vendored

libwebp's encoder source is vendored under [`native/libwebp`](https://github.com/oddlyclever/capabilities/blob/HEAD/media-webp/native/libwebp), unmodified, from the release recorded in [`native/PROVENANCE`](https://github.com/oddlyclever/capabilities/blob/HEAD/media-webp/native/PROVENANCE): today v1.6.0, from the upstream repository at `https://github.com/webmproject/libwebp`. `PROVENANCE` records the tag and the commit it resolved to, and is rewritten by the update script; do not edit it by hand.

Only what the build needs is kept: the library sources under `src/` and `sharpyuv/`, and the licence, patent grant and authors list that must travel with anything built from them. Upstream's own build files are removed, since the module builds with its own [`CMakeLists.txt`](https://github.com/oddlyclever/capabilities/blob/HEAD/media-webp/CMakeLists.txt). libwebp's container libraries, mux and demux, are removed too, because metadata is written in Kotlin; see [Metadata Written in Kotlin](Media-Webp-Metadata-Written-In-Kotlin). The decoder is compiled, because parts of the encoder call into it.

The Android and iOS libraries built from it are committed, so a consumer needs no NDK, CMake or network access. Gradle does not build them.

## The scripts

| Script | What it does |
|---|---|
| [`native/update-source.sh`](https://github.com/oddlyclever/capabilities/blob/HEAD/media-webp/native/update-source.sh) `v1.6.0` | replaces the vendored source with that upstream tag, wholesale, and rewrites `PROVENANCE` |
| [`native/build-natives.sh`](https://github.com/oddlyclever/capabilities/blob/HEAD/media-webp/native/build-natives.sh) | rebuilds `libwebp_jni.so` for each Android ABI and `libwebp.a` for each iOS target, and copies the headers cinterop parses |

`update-source.sh` replaces each file it writes wholesale, so a local edit to the vendored source is lost on the next update.

`build-natives.sh` needs macOS with Xcode, CMake, and the Android NDK. It finds the NDK through `ANDROID_NDK_HOME`, or the newest one in the Android SDK named by `ANDROID_HOME` or `ANDROID_SDK_ROOT`, and CMake through the `CMAKE` variable, the Android SDK or the `PATH`. It always builds clean, since a CMake cache remembers the path it was made at. It produces:

- `src/androidMain/jniLibs/<abi>/libwebp_jni.so` for `arm64-v8a`, `armeabi-v7a`, `x86_64` and `x86`, at Android platform 24, the module's minimum SDK, stripped.
- `src/iosMain/cinterop/libwebp/libs/<target>/libwebp.a` for `iosArm64` and `iosSimulatorArm64`, with a deployment target of iOS 14.0.
- `src/iosMain/cinterop/libwebp/headers/webp/encode.h` and `types.h`, copied from the vendored source rather than kept by hand, because cinterop parses these headers and links the library, and both must come from the same release.

## Updating libwebp

1. Run `native/update-source.sh <tag>`.
2. Run `native/build-natives.sh`.
3. Update the expected version in `NativeWebpTest`, which checks that the Android library loaded is the release `PROVENANCE` names.
4. Commit the source, `PROVENANCE`, the libraries and the copied headers together.

## The Android binding

The JNI binding is [`native/jni/webp_jni.c`](https://github.com/oddlyclever/capabilities/blob/HEAD/media-webp/native/jni/webp_jni.c), over libwebp's simple encoding API. Its methods are registered by name in `JNI_OnLoad`, against the class named in one place, `WEBP_JNI_CLASS`, rather than found by `Java_<package>_<class>_<method>` symbol names. A mismatch then fails loudly when the library loads instead of at the first call. Renaming or moving `NativeWebp` means changing that name, rebuilding the libraries, and updating the keep rule in `consumer-rules.pro`.

The Android build exports only `JNI_OnLoad`: libwebp's functions are compiled with hidden visibility and `WEBP_EXTERN` redefined, so another libwebp in the same app cannot be bound in their place. It links with a 16 KB maximum page size, `--no-undefined` and section garbage collection.

## Skia

The JVM and the web encode lossy through Skia, which carries its own libwebp, so nothing native is built for them. The Skiko version comes from the repository's version catalog and must be the one Compose Multiplatform brings, moving with every Compose update.

The module's tests need Skia's runtime themselves. The JVM tests add the `skiko-awt-runtime` artifact for the machine they run on. The web tests unpack Skia's WebAssembly from `skiko-js-wasm-runtime` as test resources, since a Compose app's build would otherwise do it and this module uses Skia without Compose; nothing published carries it. Skiko stops publishing that artifact from 0.151.0, so the Compose 1.13 update has to move the web tests to Skiko's other runtime variant. Both web targets also declare `binaries.executable()`, because since Compose 1.12 a browser test with Skiko on its classpath must be bundled by webpack; the published klibs and Gradle metadata are the same with or without it.
