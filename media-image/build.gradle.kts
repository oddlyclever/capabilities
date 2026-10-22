import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompileCommon

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.multiplatformLibrary)
    alias(libs.plugins.vanniktech.mavenPublish)
}

group = libs.versions.group.get()
version = libs.versions.versionName.get()

// Images downsized and encoded for an upload or a file: WebP through media-webp on every target, JPEG and PNG through
// each platform's own encoder. On Android the application context comes from :platform.
kotlin {
    jvmToolchain(17)

    jvm()

    android {
        namespace = "${libs.versions.namespace.get()}.media.image"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()

        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }


        // No host test: decoding and media-webp's encoder need a device. The common tests run there instead.
        withDeviceTestBuilder {
            sourceSetTreeName = "test"
        }.configure {
            instrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
                implementation(project(":media-webp"))
                implementation(libs.kotlinx.coroutines.core)
            }
        }
        androidMain {
            dependencies {
                // The application context, so ImageProcessor() needs none passed in.
                implementation(project(":platform"))
                // A photo's orientation, location and capture time, from JPEG, PNG, WebP and HEIC.
                implementation(libs.androidx.exifinterface)
            }
        }
        jvmMain {
            dependencies {
                // Decoding, resizing, and JPEG and PNG encoding.
                implementation(libs.skiko)
            }
        }
        webMain {
            dependencies {
                // The DOM: <img> to decode and <canvas> to resize and encode.
                implementation(libs.kotlinx.browser)
            }
        }
        commonTest {
            dependencies {
                implementation(libs.kotlin.test)
                implementation(libs.kotlinx.coroutines.test)
            }
        }
        getByName("androidDeviceTest") {
            dependencies {
                implementation(libs.kotlin.test)
                implementation(libs.kotlinx.coroutines.test)
                implementation(libs.androidx.test.ext.junit)
                implementation(libs.androidx.test.runner)
            }
        }
        // Skia, which the web tests wait for before encoding WebP.
        webTest {
            dependencies {
                implementation(libs.skiko)
            }
        }
        // Skia's native library for this machine, which decoding and encoding need on the JVM.
        jvmTest {
            dependencies {
                val os = System.getProperty("os.name").lowercase()
                val arm = System.getProperty("os.arch").let { it == "aarch64" || it == "arm64" }
                implementation(
                    when {
                        os.contains("mac") && arm -> libs.skiko.runtime.macos.arm64
                        os.contains("mac") -> libs.skiko.runtime.macos.x64
                        arm -> libs.skiko.runtime.linux.arm64
                        else -> libs.skiko.runtime.linux.x64
                    }
                )
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

// Skia's WebAssembly, which the web tests load. A Compose app's build unpacks it with the app; this module uses Skia
// without Compose, so its tests unpack it themselves, as test resources. Nothing published carries it.
val skikoWasmRuntime: Configuration by configurations.creating
dependencies {
    skikoWasmRuntime(libs.skiko.wasm.runtime)
}
val unpackSkikoWasm = tasks.register<Sync>("unpackSkikoWasm") {
    from({ skikoWasmRuntime.map { zipTree(it) } })
    include("*.mjs", "*.wasm")
    into(layout.buildDirectory.dir("skiko-wasm"))
}
kotlin.sourceSets {
    getByName("jsTest").resources.srcDir(unpackSkikoWasm)
    getByName("wasmJsTest").resources.srcDir(unpackSkikoWasm)
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

    coordinates(group.toString(), "media-image", version.toString())

    pom {
        name = "media-image"
        description = "Images downsized and encoded for an upload or a file, as WebP, JPEG or PNG, the same call on every target."
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
