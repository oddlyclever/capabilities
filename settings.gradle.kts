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

// kartog: maps on map-view. The base every kartog-<part> extension builds on.
include(":kartog")

include(":kartog-raster")
include(":kartog-vector")
include(":kartog-static")
include(":kartog-track")
include(":kartog-geojson")
// JVM only: kartog-headless bundles Skia native libraries for servers. Devices use kartog-static.
include(":kartog-headless")

// WebP encoding with metadata, on every target.
include(":webp")
