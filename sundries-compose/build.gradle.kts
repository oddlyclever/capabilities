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

// The Compose parts apps keep writing for themselves: modifier and test-tag helpers, colour and size conversions,
// window-size checks, text effects, and copying text to the clipboard with TextCopy. Its package is
// com.oddlyclever.sundries, without the `compose` the module name would give it, by the maintainer's choice.
kotlin {
    jvmToolchain(libs.versions.javaVersion.get().toInt())

    jvm()

    android {
        namespace = "${libs.versions.namespace.get()}.sundries"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()

        compilerOptions {
            jvmTarget.set(JvmTarget.fromTarget(libs.versions.javaVersion.get()))
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
                // api: the helpers extend and return Compose's own types.
                api(libs.compose.runtime)
                api(libs.compose.foundation)
                api(libs.compose.ui)
                api(libs.compose.material3)
                // The window-size checks; versioned apart from material3.
                api(libs.compose.material3.adaptive)
                // A copy runs in the composition's scope and resets after a delay.
                implementation(libs.kotlinx.coroutines.core)
            }
        }
        // The clipboard on the web, with the fallback for pages served over plain http. One actual for js and wasmJs.
        webMain {
            dependencies {
                implementation(libs.kotlinx.browser)
            }
        }
        commonTest {
            dependencies {
                implementation(libs.kotlin.test)
                implementation(libs.kotlinx.coroutines.test)
            }
        }
        // Compose UI tests of TextCopy, on the desktop with Skia's native library for this machine.
        jvmTest {
            dependencies {
                implementation(compose.desktop.currentOs)
                implementation(libs.compose.ui.test)
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

    coordinates(group.toString(), "sundries-compose", version.toString())

    pom {
        name = "sundries-compose"
        description = "The Compose Multiplatform parts every app ends up writing for itself: modifier and test-tag helpers, colour and size conversions, window-size checks, text effects, and copyable text."
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
