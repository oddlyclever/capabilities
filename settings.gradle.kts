// Not `capabilities`: a consumer that includes this build often has a `:capabilities` project of its own, and
// IntelliJ names modules after the root project.
rootProject.name = "Capabilities"
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

// No mavenLocal(): everything must resolve from public repositories, so a build that passes here passes for anyone.
pluginManagement {
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
    }
}

// map-view, which the wayfinder modules build on, built from a sibling checkout of github.com/coderwise/mapview-mobile
// until it publishes from that repository. A checkout of it beside this one is required to build. The versions in
// gradle/libs.versions.toml still name the artifacts, but this build replaces them whatever their version.
includeBuild("../mapview-mobile") {
    name = "mapview-mobile"
    dependencySubstitution {
        substitute(module("com.coderwise.libs:map-view")).using(project(":map-view"))
        substitute(module("com.coderwise.libs:map-view-core")).using(project(":map-view-core"))
        substitute(module("com.coderwise.libs:map-view-tiles")).using(project(":map-view-tiles"))
        substitute(module("com.coderwise.libs:map-view-tiles-raster")).using(project(":map-view-tiles-raster"))
        substitute(module("com.coderwise.libs:map-view-tiles-vector")).using(project(":map-view-tiles-vector"))
    }
}

// One `include(":<module>")` per module, each a standalone Kotlin Multiplatform library published as
// `com.oddlyclever:<module>`. A module that drops a standard target says which, and why, beside its line.

// wayfinder: maps on map-view, with tracks and the user's position. The base every wayfinder-<part> extension builds on.
include(":wayfinder")

include(":wayfinder-raster")
include(":wayfinder-vector")
include(":wayfinder-frame")
include(":wayfinder-geojson")
// A tile cache for the app's TileClient, in memory or on disk. Compose-free, so a server can use it too.
include(":wayfinder-cache")
// JVM only: wayfinder-headless bundles Skia native libraries for servers. Devices use wayfinder-frame.
include(":wayfinder-headless")

// WebP encoding with metadata, on every target.
include(":media-webp")

// The application context and the activity in front, captured once at process start; and OsFamily.
include(":platform")

// Images downsized and encoded for an upload or a file.
include(":media-image")

// Uber H3 hexagonal grid indexing behind one String-cell API, on every standard target.
include(":h3spatial")
include(":jsonlogic")

// The Compose parts apps keep writing for themselves, TextCopy among them. Its package is com.oddlyclever.sundries,
// not the com.oddlyclever.sundries.compose its name would give, by the maintainer's choice: see its README.
include(":sundries-compose")
