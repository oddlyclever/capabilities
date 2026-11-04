# Platform Notes

DOC-0006 · 2026-11-04
Keywords: h3 Android emulator x86, x86_64 emulator, UnsatisfiedLinkError libh3-java.so, R8 minification H3 crash, ProGuard keep rules com.uber.h3core, NativeMethods, NDK, CMake, Android minSdk 24, APK size, iOS CocoaPods, Swift Package Manager, Xcode setup, libh3.a, cinterop, Swift try, webpack fs polyfill, Module not found fs path os, h3-js browser build, Node, JVM native library platforms, Windows, linux-arm64

Every target binds Uber's own H3, of one pinned release, and none needs setup from the consuming app. This page is what arrives with the dependency on each target and what each one needs to know.

| Target | Backing | What arrives with the dependency |
|---|---|---|
| Android | `com.uber:h3-android` | Uber's own Android packaging of the same Java binding the JVM uses, with `libh3-java.so` for `arm64-v8a` and `armeabi-v7a`. Consumer keep rules. No NDK or CMake. |
| iOS | The H3 C library through cinterop | `libh3.a`, embedded in the published cinterop klib and linked automatically. |
| JVM | `com.uber:h3` | A transitive Maven dependency. |
| JS / wasmJs | `h3-js` | A transitive npm dependency, declared in the klib's `package.json`. |

Why every target binds Uber's H3 rather than a shared reimplementation is in [Uber's Own H3 on Every Target, at One Release](ADR-0002-ubers-own-h3-on-every-target-at-one-release.md).

## Android

- **ARM only.** `com.uber:h3-android` ships `arm64-v8a` and `armeabi-v7a`, which covers current Android devices and the emulators on Apple Silicon. There are no `x86` or `x86_64` builds, so H3 will not load on an Intel-based emulator: the first call throws `UnsatisfiedLinkError`.
- **No host unit tests.** The same applies to an Android unit test on the development machine: the native library exists only for a device, so code that calls the service has to be tested on a device or an ARM emulator.
- **Minification is handled.** `libh3-java.so` binds to `com.uber.h3core.NativeMethods` by name, and constructs `com.uber.h3core.util.LatLng` and `com.uber.h3core.exceptions.H3Exception` from native code. `com.uber:h3-android` ships no keep rules, so this library publishes them as consumer keep rules, in [`consumer-rules.pro`](../../../h3spatial/consumer-rules.pro). Without them a minified build fails to load H3 at all, not just on one call.
- **Loading.** The library is loaded with `H3Core.newSystemInstance()`, which calls `System.loadLibrary("h3-java")` and finds it in the AAR's `jni/` directory. `com.uber:h3`'s other loader extracts the native file from resources instead, which needs file APIs older Android versions lack.
- **Size and minimum SDK.** The native code is about 340 KB across the two ABIs before ABI splitting, about 170 KB for each, and about 135 KB of the AAR compressed. The declared minimum SDK is 24.

## iOS

- **Consumed as a Kotlin dependency, not a CocoaPod or Swift package.** It reaches iOS through your shared module's framework; nothing is added in Xcode.
- **No runtime setup.** The static library is linked into that framework, so there is no initialisation call and no bundle resource.
- **Targets.** `iosArm64` for devices and `iosSimulatorArm64` for the simulator on Apple Silicon. The static libraries are built for iOS 14.0 and later. There is no `iosX64` build, so the simulator on an Intel Mac is not supported.
- **Calling from Swift.** Every function that can fail is annotated `@Throws(H3SpatialException::class)`, so where it is visible from Swift it imports as a `throws` function: call it with `try` and catch the error. `isValidCell` never fails and imports without `throws`. Without the annotation, Kotlin/Native does not bridge the exception at all and an invalid cell would terminate the app. The same goes for a function in your own shared module that lets one of these exceptions escape to Swift: it needs the annotation too.
- **Units.** The C API works in radians and reports failure as an `H3Error` code; both are converted at the binding, so the service speaks degrees and `H3SpatialException` on iOS exactly as elsewhere.

## Web

`h3-js` ships dedicated browser builds, selected by its `package.json` `browser` field, that use none of Node's `fs`, `path` or `os`. No webpack polyfills or fallbacks are needed. Both web targets are browser targets; Node is not one of this module's targets.

## JVM

`com.uber:h3` extracts the native library that matches the machine from its own jar the first time the service is used. The 4.5.0 jar carries libraries for Linux on x64, x86, arm64, armv7, armv5, ppc64le and s390x, for macOS on x64 and arm64, and for Windows on x64 and x86. If it cannot load one, the service throws `LinkageError` with the message "Could not load the H3 native library", which is an `Error` and not an `H3SpatialException`; see [Errors and What Each Call Refuses](DOC-0005-errors-and-what-each-call-refuses.md).

## Wiring

`H3SpatialService` takes no constructor arguments and holds no state: no fields, cached handles or native resources. There is no Koin module and no Koin dependency; construct it where it is needed, or bind it in your own container as a factory. Why it ships no DI module, and why a factory rather than a singleton, is in [One Cell Vocabulary and One Failure Type on Every Target](ADR-0003-one-cell-vocabulary-and-one-failure-type-on-every-target.md).
