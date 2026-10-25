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

// Every target binds Uber's H3 of one pinned release: com.uber:h3 on the JVM, com.uber:h3-android on
// Android, the vendored C through cinterop on iOS, and h3-js on JS and wasmJs. Their versions move
// together — see README "Version pin".
kotlin {
    jvmToolchain(libs.versions.javaVersion.get().toInt())

    jvm()

    android {
        namespace = "${libs.versions.namespace.get()}.h3spatial"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()

        compilerOptions {
            jvmTarget.set(JvmTarget.fromTarget(libs.versions.javaVersion.get()))
        }

        optimization {
            consumerKeepRules.apply {
                publish = true
                // com.uber:h3-android's native library binds to com.uber.h3core classes by name, and the
                // AAR ships no keep rules of its own.
                file("consumer-rules.pro")
            }
        }

        // No withHostTest, although commonTest exists and AGP warns about it: the H3 binding loads a native library
        // that only exists on a device. The common tests run on Android through the device test below instead.

        // The JNI binding only loads on Android, so an instrumented run is the only way to exercise
        // that target.
        withDeviceTestBuilder {
            sourceSetTreeName = "test"
        }.configure {
            instrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        }
    }

    js {
        browser()
    }
    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser()
    }

    fun KotlinNativeTarget.h3CInterop() {
        compilations.getByName("main").cinterops.create("h3") {
            defFile(project.file("src/iosMain/cinterop/h3/h3.def"))
            includeDirs(project.file("src/iosMain/cinterop/h3"))
            // libh3.a is prebuilt per target and committed, like the Android .so files.
            extraOpts(
                "-libraryPath",
                project.file("src/iosMain/cinterop/h3/libs/$targetName").absolutePath
            )
        }
    }
    iosArm64 { h3CInterop() }
    iosSimulatorArm64 { h3CInterop() }

    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }

    sourceSets {
        commonTest {
            dependencies {
                implementation(libs.kotlin.test)
            }
        }
        jvmMain {
            dependencies {
                implementation(libs.uber.h3)
            }
        }
        androidMain {
            dependencies {
                implementation(libs.uber.h3.android)
            }
        }
        jsMain {
            dependencies {
                implementation(npm("h3-js", libs.versions.h3Js.get()))
            }
        }
        wasmJsMain {
            dependencies {
                implementation(npm("h3-js", libs.versions.h3Js.get()))
            }
        }
        getByName("androidDeviceTest") {
            dependencies {
                implementation(libs.kotlin.test)
                implementation(libs.androidx.test.ext.junit)
                implementation(libs.androidx.test.runner)
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

// The published iOS artifacts embed libh3.a, which is Uber's H3 in object form, so the Apache License
// requires the license and Uber's NOTICE to travel with them. A klib cannot carry extra files, so both go
// into META-INF of every jar this module publishes, including the sources jar beside each klib.
// org.gradle.jvm.tasks.Jar, not the build-script default Jar: the Kotlin plugin registers its sources jars
// as the base type, which the default (a subclass) would not match.
tasks.withType<org.gradle.jvm.tasks.Jar>().configureEach {
    metaInf {
        from(layout.projectDirectory.file("LICENSE"))
        from(layout.projectDirectory.file("NOTICE"))
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

    coordinates(group.toString(), "h3spatial", version.toString())

    pom {
        name = "h3spatial"
        description = "Uber H3 hexagonal grid indexing for Kotlin Multiplatform behind one String-cell API: cell indexing, grid traversal and GeoJSON cell rendering on Android, iOS, JVM, JS and wasmJs."
        inceptionYear = "2026"
        url = "https://github.com/oddlyclever/capabilities"
        licenses {
            license {
                name = "The Apache License, Version 2.0"
                url = "https://www.apache.org/licenses/LICENSE-2.0"
                distribution = "https://www.apache.org/licenses/LICENSE-2.0.txt"
            }
            license {
                name = "Uber H3: The Apache License, Version 2.0"
                url = "https://github.com/uber/h3/blob/master/LICENSE"
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
