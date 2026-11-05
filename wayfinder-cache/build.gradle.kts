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

// A tile cache that goes in front of the app's TileClient: stores in memory or on disk, and four ways of sharing the work
// with the network. Compose-free, so a server drawing map images with wayfinder-headless can use it too.
kotlin {
    jvmToolchain(libs.versions.javaVersion.get().toInt())

    jvm()

    android {
        namespace = "${libs.versions.namespace.get()}.wayfinder.cache"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()

        compilerOptions {
            jvmTarget.set(JvmTarget.fromTarget(libs.versions.javaVersion.get()))
        }

        withHostTest {}
    }

    js {
        browser()
    }

    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser()
    }

    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        commonMain {
            dependencies {
                // api: TileKey is in every signature, and FileTileStore is made from an okio FileSystem and Path. Only the
                // key, not map-view itself, because a cache has no use for Compose.
                api(libs.mapview.core)
                api(libs.okio)
                // The stores' locks, and moving disk work off the caller's thread.
                implementation(libs.kotlinx.coroutines.core)
            }
        }
        commonTest {
            dependencies {
                implementation(libs.kotlin.test)
                implementation(libs.kotlinx.coroutines.test)
                implementation(libs.okio.fakefilesystem)
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

    coordinates(group.toString(), "wayfinder-cache", version.toString())

    pom {
        name = "wayfinder-cache"
        description = "A tile cache for wayfinder maps, in front of the app's own tile client: a size-bounded disk store, a memory store, and cache-first, network-first and offline modes."
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
