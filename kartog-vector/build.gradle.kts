import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompileCommon

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.multiplatformLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.vanniktech.mavenPublish)
}

group = libs.versions.group.get()
version = libs.versions.versionName.get()

// Vector tiles for kartog: light and dark styles for map-view's vector renderer, and drawing a vector tile to an
// image on devices whose GPU driver cannot be trusted with its paths. Depends on map-view-tiles-vector, and on
// map-view-tiles-raster to show those images.
kotlin {
    // 21, not the repository's usual 17: map-view's JVM classes are Java 21, so a JVM app using kartog runs on 21
    // whatever this module targets, and tests on 17 cannot load them.
    jvmToolchain(21)

    jvm()

    android {
        namespace = "${libs.versions.namespace.get()}.kartog.vector"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()

        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }

    // binaries.executable() on both web targets: since Compose 1.12 a browser test that has Skiko on its classpath
    // must be bundled by webpack to load it, and the build refuses to run without one (CMP-4906). Nothing published
    // changes — the klibs and Gradle metadata are identical with and without it.
    js {
        browser()
        binaries.executable()
    }

    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser()
        binaries.executable()
    }

    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        commonMain {
            dependencies {
                api(project(":kartog"))
                // api: VectorStyle and VectorTile are what this module's styles and decoding hand back.
                api(libs.mapview.tiles.vector)
                // api: a vector tile drawn to an image is shown with RasterSlot.
                api(libs.mapview.tiles.raster)
                implementation(libs.kotlinx.coroutines.core)
            }
        }
        commonTest {
            dependencies {
                implementation(libs.kotlin.test)
                implementation(libs.kotlinx.coroutines.test)
            }
        }
        // Skia's native library for this machine, which decoding and drawing an image need on the JVM.
        jvmTest {
            dependencies {
                implementation(compose.desktop.currentOs)
            }
        }
    }

    // Workaround for KLIB uniqueName clashes across modules in the same repo.
    // https://youtrack.jetbrains.com/issue/KT-66568
    metadata {
        compilations.all {
            val compilationName = rootProject.name
            compileTaskProvider.configure {
                if (this is KotlinCompileCommon) {
                    moduleName = "${project.group}:${project.name}_$compilationName"
                }
            }
        }
    }
}

mavenPublishing {
    publishToMavenCentral(automaticRelease = true)

    // Signed only where a key is configured, which is CI; a local build publishes to mavenLocal unsigned.
    val hasSigningKey = project.hasProperty("signingInMemoryKey") ||
            project.hasProperty("signingInMemoryKeyId") ||
            project.hasProperty("signing.gnupg.keyName")
    if (hasSigningKey && !project.hasProperty("skip-signing")) {
        signAllPublications()
    }

    coordinates(group.toString(), "kartog-vector", version.toString())

    pom {
        name = "kartog-vector"
        description = "Vector tiles for kartog: light and dark styles for map-view's vector renderer, and vector tiles drawn to images where a GPU driver cannot draw their paths."
        inceptionYear = "2026"
        url = "https://github.com/oddlyclever/capabilities"
        licenses {
            license {
                name = "The Apache License, Version 2.0"
                url = "https://www.apache.org/licenses/LICENSE-2.0"
                distribution = "https://www.apache.org/licenses/LICENSE-2.0.txt"
            }
        }
        developers {
            developer {
                id = "bpappin"
                name = "bpappin"
                url = "https://github.com/bpappin"
            }
        }
        scm {
            url = "https://github.com/oddlyclever/capabilities"
            connection = "scm:git:https://github.com/oddlyclever/capabilities.git"
            developerConnection = "scm:git:ssh://git@github.com/oddlyclever/capabilities.git"
        }
    }
}
