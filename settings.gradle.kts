// Not `capabilities`: a consumer that includes this build often has a `:capabilities` project of its own, and
// IntelliJ names modules after the root project.
rootProject.name = "oddlyclever-capabilities"
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

// One `include(":<module>")` per module, each a standalone Kotlin Multiplatform library published as
// `com.oddlyclever:<module>`. A module that drops a standard target says which, and why, beside its line.

// wayfinder: maps on map-view, with tracks and the user's position. The base every wayfinder-<part> extension builds on.
include(":wayfinder")

include(":wayfinder-raster")
include(":wayfinder-vector")
include(":wayfinder-frame")
include(":wayfinder-geojson")
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
