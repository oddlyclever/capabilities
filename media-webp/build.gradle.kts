import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompileCommon

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.multiplatformLibrary)
    alias(libs.plugins.vanniktech.mavenPublish)
}

group = libs.versions.group.get()
version = libs.versions.versionName.get()

// WebP encoding with metadata on every target. Android and iOS encode with a libwebp built into this module and
// committed (native/build-natives.sh); the JVM and the web encode with Skia's libwebp. Metadata is written in
// common Kotlin, so it is the same everywhere.
kotlin {
    jvmToolchain(libs.versions.javaVersion.get().toInt())

    jvm()

    android {
        namespace = "${libs.versions.namespace.get()}.media.webp"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()

        compilerOptions {
            jvmTarget.set(JvmTarget.fromTarget(libs.versions.javaVersion.get()))
        }

        optimization {
            consumerKeepRules.apply {
                publish = true
                // libwebp_jni.so binds to NativeWebp by name.
                file("consumer-rules.pro")
            }
        }

        // No host test: the encoder is a native library that loads only on a device. The common tests run there
        // instead, through the device test below.
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

    fun KotlinNativeTarget.libwebpCInterop() {
        compilations.getByName("main").cinterops.create("libwebp") {
            defFile(project.file("src/iosMain/cinterop/libwebp/libwebp.def"))
            includeDirs(project.file("src/iosMain/cinterop/libwebp/headers"))
            // libwebp.a is prebuilt per target and committed, like the Android .so files.
            extraOpts("-libraryPath", project.file("src/iosMain/cinterop/libwebp/libs/$targetName").absolutePath)
        }
    }
    iosArm64 { libwebpCInterop() }
    iosSimulatorArm64 { libwebpCInterop() }

    sourceSets {
        jvmMain {
            dependencies {
                implementation(libs.skiko)
            }
        }
        webMain {
            dependencies {
                implementation(libs.skiko)
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
                implementation(libs.androidx.test.ext.junit)
                implementation(libs.androidx.test.runner)
            }
        }
        // Skia's native library for this machine, which the JVM encoder needs at run time.
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

// The published Android and iOS artifacts carry libwebp in object form, so its BSD licence and Google's patent grant
// travel with them. A klib cannot carry extra files, so both go into META-INF of every jar this module publishes,
// including the sources jar beside each klib. org.gradle.jvm.tasks.Jar, not the build-script default Jar: the Kotlin
// plugin registers its sources jars as the base type, which the default (a subclass) would not match.
tasks.withType<org.gradle.jvm.tasks.Jar>().configureEach {
    metaInf {
        from(layout.projectDirectory.file("native/libwebp/COPYING")) { rename { "LICENSE.libwebp" } }
        from(layout.projectDirectory.file("native/libwebp/PATENTS")) { rename { "PATENTS.libwebp" } }
    }
}

// Ships this library's agent skill in every sources jar, where a consumer's tooling reads it.
tasks.withType<Zip>()
    .matching { it.name == "sourcesJar" || it.name.endsWith("SourcesJar") }
    .configureEach {
        from("src/commonMain/skills") {
            include("*/SKILL.md", "*/references/**", "*/assets/**")
            into("commonMain/skills")
        }
    }

mavenPublishing {
    // No remote repository is configured: publishing goes to mavenLocal only. Signed only where a key is configured,
    // which nothing does yet, so the artifacts are unsigned.
    val hasSigningKey = project.hasProperty("signingInMemoryKey") ||
            project.hasProperty("signingInMemoryKeyId") ||
            project.hasProperty("signing.gnupg.keyName")
    if (hasSigningKey && !project.hasProperty("skip-signing")) {
        signAllPublications()
    }

    coordinates(group.toString(), "media-webp", version.toString())

    pom {
        name = "media-webp"
        description = "WebP encoding for Kotlin Multiplatform, lossy or lossless, with GPS and capture-time metadata, the same on every target."
        inceptionYear = "2026"
        url = "https://github.com/oddlyclever/capabilities"
        licenses {
            license {
                name = "The Apache License, Version 2.0"
                url = "https://www.apache.org/licenses/LICENSE-2.0"
                distribution = "https://www.apache.org/licenses/LICENSE-2.0.txt"
            }
            license {
                name = "libwebp: BSD 3-Clause License"
                url = "https://github.com/webmproject/libwebp/blob/main/COPYING"
                distribution = "repo"
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
