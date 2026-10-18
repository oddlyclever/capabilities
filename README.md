# Capabilities

Independent, composable Kotlin Multiplatform libraries, one capability each: sign-in, calendars and schedules, crash reporting, file viewing, permissions, spatial indexing and the like. Each is a small library you depend on, not code you copy, and none knows anything about the app that uses it.

Every module is published as `com.oddlyclever:<module>` and builds for the same targets:

**JVM · Android · iOS (arm64, simulator) · JS (browser) · wasmJs (browser)**

A module that cannot support one of these says which and why, in its own README and in the module list below.

## Modules

None yet. Modules arrive here one at a time as each is ready for public use, and are listed here as they do.

## How the modules are shaped

- **A capability is mechanism, not policy.** It provides the ability, such as asking for a permission, holding a token or reporting a crash. What to ask for, when, and what to show the user stay with the app.
- **Vendors sit behind a neutral core.** Where a capability needs a third-party backend, the core module owns the interface, and a `-<backend>` module supplies one implementation. The app chooses a backend in its own wiring, and nothing upstream of that names the vendor.
- **Classes construct directly.** Some modules ship a Koin module where it saves real wiring across platforms, but a dependency-injection container is never the only way to get an instance. Each module's README says whether Koin is required or optional.
- **The public API is a released surface.** It widens before it narrows and deprecates before it deletes. A source-incompatible change is called out in the release notes.

## Using a module

Released modules are on Maven Central:

```kotlin
kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation("com.oddlyclever:example:0.0.3")
        }
    }
}
```

Every module in a release carries the same version.

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

The toolchain is Kotlin 2.4.21, Android Gradle Plugin 9.4.1 and Gradle 9.8.0. Gradle provisions the JDK 21 it runs on, and the modules compile for JDK 17. Building the iOS targets needs a Mac with Xcode.

## License

Apache License 2.0; see [LICENSE](LICENSE) and [NOTICE](NOTICE). A module derived from third-party work names its origin in its own README.

Maintained by [@bpappin](https://github.com/bpappin).
