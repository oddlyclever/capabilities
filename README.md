# Capabilities

Independent, composable Kotlin Multiplatform libraries, one capability each: maps, spatial indexing, image and WebP encoding, and the platform plumbing they share. Each is a small library you depend on, not code you copy, and none knows anything about the app that uses it.

Every module's coordinates are `com.oddlyclever:<module>`, and every module builds for the same targets:

**JVM · Android · iOS (arm64, simulator) · JS (browser) · wasmJs (browser)**

A module that cannot support one of these says which and why, in its own README and in the module list below.

## Modules

| Module | What it does |
|---|---|
| [`h3spatial`](h3spatial) | Uber's H3 hexagonal grid behind one `String`-cell API: indexing, traversal, hierarchy and GeoJSON, with the same answers on every target. |
| [`wayfinder`](wayfinder) | Maps for Compose Multiplatform on [map-view](https://github.com/coderwise/libs-mobile): the map, its state, markers, polylines, controls, recorded tracks and the user's position. The root of the wayfinder family. |
| [`wayfinder-vector`](wayfinder-vector) | Light and dark styles for map-view's vector tiles, and vector tiles drawn to images on GPUs that cannot draw their paths. |
| [`wayfinder-raster`](wayfinder-raster) | Raster basemaps for wayfinder: decoded off the main thread, and faded in as they arrive. |
| [`wayfinder-geojson`](wayfinder-geojson) | GeoJSON on a wayfinder map, from text or a URL. |
| [`wayfinder-frame`](wayfinder-frame) | Map frames: a map at one camera position and size, drawn without a composition to an image or to a painter that scales like a vector image; PNG or WebP. |
| [`wayfinder-headless`](wayfinder-headless) | Map images on a server in one call, WebP or PNG. **JVM only**: it bundles Skia's native libraries for servers. |
| [`jsonlogic`](jsonlogic) | A clean-room [JSONLogic](https://jsonlogic.com) rule evaluator in pure common Kotlin: one rule, the same result on every target. |
| [`platform`](platform) | What must exist once in a process: on Android, the application context and the activity in front, captured at start-up so no library has to ask for them; and `OsFamily`. |
| [`media-image`](media-image) | Images downsized and encoded for an upload or a file, as WebP, JPEG or PNG, with the same call on every target. |
| [`media-webp`](media-webp) | WebP encoding, lossy or lossless, with GPS position and capture time in the file; the same call on every target, with libwebp built in on Android and iOS. |

## How the modules are shaped

- **A capability is mechanism, not policy.** It provides the ability, such as asking for a permission, holding a token or reporting a crash. What to ask for, when, and what to show the user stay with the app.
- **Vendors sit behind a neutral core.** Where a capability needs a third-party backend, the core module owns the interface, and a `-<backend>` module supplies one implementation. The app chooses a backend in its own wiring, and nothing upstream of that names the vendor.
- **Classes construct directly.** Some modules ship a Koin module where it saves real wiring across platforms, but a dependency-injection container is never the only way to get an instance. Each module's README says whether Koin is required or optional.
- **The public API is a released surface.** It widens before it narrows and deprecates before it deletes. A source-incompatible change is called out in the [changelog](CHANGELOG.md).

## Using a module

The modules are not on Maven Central yet. Until they are, build them from source: include this repository as a composite build (below), or `publishToMavenLocal`. Once published, a module is one dependency:

```kotlin
kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation("com.oddlyclever:example:0.0.7")
        }
    }
}
```

Every module in a release carries the same version, and the [changelog](CHANGELOG.md) says what each release changed.

What a consuming app needs:

- **On the JVM, Java 21.** Every module's JVM classes are Java 21.
- **On Android, API level 24 or later**, and an app that calls one of the modules' inline functions compiles with `jvmTarget` 21.
- **On iOS**, arm64 devices or the arm64 simulator.
- **On the web**, a browser: the JS and wasmJs targets are built for it, not for Node.js.

To work on a module alongside an app, include this repository as a **composite build**, which picks up local edits with no publish step. In the consuming project's `settings.gradle.kts`:

```kotlin
includeBuild("../capabilities") {
    name = "oddlyclever-capabilities"
    dependencySubstitution {
        substitute(module("com.oddlyclever:example")).using(project(":example"))
        // one line per module the app uses
    }
}
```

The app keeps depending on the same coordinates, and Gradle builds them from your checkout. To try a local change without a composite build, publish it to your local Maven repository with `./gradlew :example:publishToMavenLocal`.

## Building

```bash
./gradlew check
```

`check` compiles every target and runs every module's tests. A single module is `./gradlew :<module>:check`.

The toolchain is Kotlin 2.4.21, Android Gradle Plugin 9.4.1 and Gradle 9.8.0. Gradle provisions the JDK 21 it runs on. Every module builds and targets Java 21, set once by `javaVersion` in `gradle/libs.versions.toml`. Building the iOS targets needs a Mac with Xcode.

## License

Apache License 2.0; see [LICENSE](LICENSE) and [NOTICE](NOTICE). A module derived from third-party work names its origin in its own README; `media-webp` carries libwebp, under its own BSD licence.

Maintained by [@bpappin](https://github.com/bpappin).
