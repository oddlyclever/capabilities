# H3 Spatial

Uber [H3](https://github.com/uber/h3) hexagonal grid indexing for Kotlin Multiplatform, behind one `H3SpatialService` with a `String` cell vocabulary that is identical on every target.

`com.oddlyclever:h3spatial`

## What it does

| Function | Use |
|---|---|
| `latLngToH3(lat, lng, resolution)` | The cell containing a point. |
| `cellToLatLng(cell)` / `h3ToPolygon(cell)` | A cell's centre, or its boundary ring. |
| `getResolution(cell)` · `isValidCell(cell)` · `isPentagon(cell)` | Inspection. `isValidCell` is the one function that never throws. |
| `gridDisk(cell, k)` · `gridDistance(a, b)` · `areNeighborCells(a, b)` | Grid traversal. |
| `cellToParent(cell, res)` · `cellToChildren(cell, res)` | Hierarchy. |
| `cellsToGeoJson(cells)` · `cellToGeoJson(cell)` | A GeoJSON `FeatureCollection`, as text, one polygon per cell. |

**Cells are canonical H3 strings** — unpadded lowercase hex, the form the H3 C library, `h3-js` and `com.uber:h3` all produce — so a cell computed here compares equal to one from any standard H3 tool, and to one computed by this library on another platform. Zero-padded input is accepted too.

**Coordinates are degrees, rounded to eight decimal places** (about a millimetre). Each platform computes them in its own floating-point environment; rounding is what makes a centre or boundary compare equal across targets rather than differ in the last few bits.

**One failure type on every target.** Each platform binds H3 differently and fails differently — a Java exception, a C error code, a JS throw, and in `h3-js`'s case sentinel values that do not fail at all. Every function except `isValidCell` throws `H3SpatialException` on invalid input, carrying H3's own error code, so one `catch` covers all five targets. Validation happens in shared code before any platform call, so every target rejects the same inputs with the same code. Out-of-range coordinates throw rather than being silently normalised, because a valid-looking cell in the wrong place is worse than an error.

**Resolution is always the caller's choice.** There is no default, because the right cell size depends entirely on what is being bucketed.

`cellsToGeoJson` returns text rather than a model so the library carries no GeoJSON dependency; anything that parses GeoJSON — a map library's GeoJSON layer, typically — can draw the result directly.

## Install

Add it to the `commonMain` source set of the consuming module:

```kotlin
kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation("com.oddlyclever:h3spatial:0.0.3")
        }
    }
}
```

**Targets:** `jvm` · `android` · `js` (browser) · `wasmJs` (browser) · `iosArm64` · `iosSimulatorArm64`.

Every target binds Uber's own H3 of one pinned release rather than reimplementing it, and none needs setup from you:

| Target | Backing | What arrives with the dependency |
|---|---|---|
| Android | `com.uber:h3-android` | Uber's own Android packaging of the same Java binding the JVM uses, with `libh3-java.so` for `arm64-v8a` and `armeabi-v7a`. Consumer keep rules. No NDK or CMake. |
| iOS | The H3 C library through cinterop | `libh3.a`, embedded in the published cinterop klib and linked automatically. |
| JVM | `com.uber:h3` | A transitive Maven dependency. |
| JS / wasmJs | `h3-js` | A transitive npm dependency, declared in the klib's `package.json`. |

### Android notes

- **ARM only.** `com.uber:h3-android` ships `arm64-v8a` and `armeabi-v7a`, which covers current Android devices and the emulators on Apple Silicon. There are no `x86` or `x86_64` builds, so H3 will not load on an Intel-based emulator.
- **Minification is handled.** `libh3-java.so` binds to `com.uber.h3core.NativeMethods` by name and constructs `com.uber.h3core.util.LatLng` and `com.uber.h3core.exceptions.H3Exception` from native code. `com.uber:h3-android` ships no keep rules, so this library publishes them; without them a minified build fails to load H3 at all, not just on one call.
- **Size and minimum SDK.** About 290 KB of native code across the two ABIs, before ABI splitting. The declared minimum SDK is 24.

### iOS notes

- **Consumed as a Kotlin dependency, not a CocoaPod or SPM package.** It reaches iOS through your shared module's framework; nothing is added in Xcode.
- **No runtime setup.** The static library is linked into that framework, so there is no initialisation call and no bundle resource.
- **Calling from Swift:** every function that can fail is annotated `@Throws(H3SpatialException::class)`, so it imports as a Swift `throws` function — call it with `try` and catch the error. `isValidCell` never fails and imports without `throws`. Without the annotation Kotlin/Native does not bridge the exception at all and an invalid cell would terminate the app, so keep it on anything new that can throw.

### Web notes

`h3-js` ships dedicated browser builds, selected by its `package.json` `browser` field, that use none of Node's `fs`, `path` or `os`. No webpack polyfills or fallbacks are needed.

## Wiring it up

**This module ships no dependency-injection module and has no Koin dependency.** `H3SpatialService` takes no constructor arguments and has no platform variance to resolve, so a DI module inside the library would be a pass-through that forced a container onto every consumer. Construct it, or bind it in your own container.

**Bind it as a factory, not a singleton.**

```kotlin
val appModule = module {
    factory { H3SpatialService() }
}
```

The service is entirely stateless — the instance holds no fields, cached handles or native resources. A factory keeps it that way: nothing is retained, nothing is shared across threads, and no caller can come to depend on instance identity. Binding it as a singleton would cache an object with nothing in it, while presenting a shared dependency where none exists.

Without a container, simply call `H3SpatialService()` wherever it is needed.

## Version pin — currently H3 4.5.0

Every target runs the same H3 release, so the version is asserted in several places that must agree:

- `native/PROVENANCE` — the tag and commit the H3 C source was vendored from, and the h3-java release the JVM and Android artifacts must be. Authoritative; regenerated by script, never hand-edited.
- `native/h3lib/VERSION` — copied verbatim from the upstream tree.
- `uberH3` in `gradle/libs.versions.toml` — `com.uber:h3` for the JVM and `com.uber:h3-android` for Android, which h3-java releases together. It must be the h3-java release named in `native/PROVENANCE`.
- `h3Js` in `gradle/libs.versions.toml` — the npm package for JS and wasmJs.

**These move together or not at all.** A version that advances on some targets and not others gives different answers per platform for the same input, which is the failure this whole arrangement exists to prevent. The agreement tests below catch it if they drift.

## Waiting on 4.5.1

The library is deliberately held at 4.5.0 even though upstream `master` has moved ahead of the tag.

There is no 4.5.1 tag of H3, and the matching `h3-js` changes are unreleased on npm. `com.uber:h3` has a 4.5.1, but it is the same binding on H3 4.5.0, re-released to sign with a new key, so it changes nothing this library runs. Only the vendored C could follow `master`, and doing so would put iOS ahead of every other target and replace a reproducible tag pin with a moving branch reference. Neither is worth it: nothing in the current `master` delta is reachable from this library's surface.

When H3 4.5.1 ships, move once `com.uber:h3` and `h3-js` have released against it too. That release is expected to carry the fix for an out-of-bounds read in `_baseCellToCCWrot60` when passed a face index equal to `NUM_ICOSA_FACES` (uber/h3#978, fixed by uber/h3#1224). The affected path is reached only from H3's vertex API, which this library does not expose.

**Vertex-mode bindings are a candidate follow-on at that point**, not before. One clarification worth recording, because it is easy to assume otherwise: drawing *merged* cells — a set of cells rendered as one outline rather than one ring per cell — does not need vertex mode. It needs `cellsToLinkedMultiPolygon`, which is already public in 4.5.0 and does not route through the vertex code at all. Vertex mode is a separate capability, useful for treating shared corners as first-class identities: integer equality on a corner shared by three cells, and consistent coordinates regardless of which cell you approach it from.

## Maintaining the native code

The H3 C library is vendored, unmodified, under `native/h3lib`, and the iOS static libraries built from it are committed, so consumers need no CMake and no network access to an upstream checkout. Only iOS compiles it: the JVM and Android use Uber's published artifacts, and the web uses `h3-js`. Updating takes two scripts, run in order and committed together:

```bash
./h3spatial/native/update-source.sh v4.5.1 v4.5.1
```

```bash
./h3spatial/native/build-natives.sh
```

`update-source.sh <h3-tag> <h3-java-tag>` re-vendors the C source, regenerates the agreement test fixtures from H3's test data at the same tag, and rewrites `native/PROVENANCE`. It also checks that the given h3-java release was built against that H3 tag, and refuses to continue otherwise — that release is what `com.uber:h3` and `com.uber:h3-android` must be, so the check keeps iOS on the same H3 as the JVM and Android. It needs `git` and `python3`. Never hand-edit anything it writes — it replaces each file wholesale, so a local edit is silently lost on the next update.

`build-natives.sh` rebuilds both committed `libh3.a` files plus the generated cinterop header. It locates CMake itself. The header and the library must come from the same version: a stale header against a newer library compiles cleanly and fails at runtime.

After updating, set `uberH3` and `h3Js` in the version catalog to the same release, and run the checks under *Verification*.

When checking a rebuild against what is committed, **compare exported symbols, not bytes**. Different toolchain generations produce byte-different binaries from identical source; a symbol difference is the signal that matters.

This repository's `gradle.properties` enables cinterop commonization. The shared iOS source set imports the cinterop `h3` package, and without it the metadata compilation that publishing needs cannot see that package.

## Adding a function

Every target already has Uber's full H3 API available — `com.uber:h3` on the JVM and Android, the whole of `h3api.h` through cinterop on iOS, and `h3-js` on the web — so adding a function needs no native rebuild:

1. Add it to `H3SpatialService`, with its validation, and to the internal `H3Native` seam.
2. Implement `H3Native` for each target: a `com.uber:h3` call on the JVM and Android, a cinterop call on iOS, and an external declaration plus call for JS and wasmJs from `h3-js`'s own typings.
3. Annotate it `@Throws(H3SpatialException::class)` if it can fail.
4. Add it to `H3SpatialServiceSpec`, and to `UberAgreementSpec` if H3's test data covers it.

Functions returning H3's linked structures take more care on iOS, where the C API hands back memory the caller must free. `cellsToLinkedMultiPolygon`, for instance, returns a `LinkedGeoPolygon` that must be walked and then released with `destroyLinkedMultiPolygon`; `com.uber:h3` and `h3-js` return ordinary collections for the same call.

## Verification

Two suites run on every target, each from a per-target test class, because every platform binds H3 by a different route and only a run on that platform proves its binding:

- **`H3SpatialServiceSpec`** pins the service's own contract — canonical indexes, geometry, traversal, pentagons, hierarchy, GeoJSON and the failure contract, including error codes.
- **`UberAgreementSpec`** is the drift guard. It checks the service against Uber's own reference data for the vendored release: 1,468 points that must fall in their recorded cells, 892 cell boundaries including every resolution-0 pentagon, and 964 cell centres. A wrong binding, a mismatched native build or a transposed coordinate fails here, on the platform where it happens.

Both pass on `jvmTest`, `jsBrowserTest`, `wasmJsBrowserTest` and `iosSimulatorArm64Test`, which `./gradlew :h3spatial:check` runs. Android cannot load its native library in a host test, so its suite lives in `androidDeviceTest` and runs with `./gradlew :h3spatial:connectedAndroidDeviceTest` against an ARM device or emulator; it has been run on an Android 16-era arm64 emulator with 16 KB memory pages and on an API 24 arm64 emulator.

## Third-party code

The vendored C library is Uber's H3, unmodified, under the Apache License 2.0, and the agreement fixtures are derived from H3's own test data. The JVM, Android and web bindings are Uber's published packages, used as dependencies. See `NOTICE`, which is also included under `META-INF` in every published jar.

**An app that ships this library ships Uber's H3 too** — linked from the iOS static library, or from `com.uber:h3`, `com.uber:h3-android` or `h3-js`. The Apache License asks that app to include the license text and Uber's attribution (`Copyright 2017-2021 Uber Technologies, Inc.`) wherever it lists its third-party notices.

## License

Apache License 2.0. Maintained at [github.com/bpappin](https://github.com/bpappin).
