plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.vanniktech.mavenPublish)
}

group = libs.versions.group.get()
version = libs.versions.versionName.get()

// Map images on a server: one call from coordinates to WebP or PNG bytes. JVM only, deliberately: it exists to bundle
// Skia's native libraries for the platforms servers run on, which only the JVM loads. Devices use kartog-static.
kotlin {
    // 21, because map-view's classes are built for it.
    jvmToolchain(21)
}

dependencies {
    api(project(":kartog-static"))
    implementation(project(":kartog-vector"))
    implementation(project(":kartog-raster"))
    implementation(project(":kartog-track"))
    implementation(libs.kotlinx.coroutines.core)

    // Skia's native library is loaded at runtime from whichever of these matches the machine.
    runtimeOnly(libs.skiko.runtime.linux.x64)
    runtimeOnly(libs.skiko.runtime.linux.arm64)
    runtimeOnly(libs.skiko.runtime.macos.arm64)
    runtimeOnly(libs.skiko.runtime.macos.x64)

    testImplementation(kotlin("test-junit"))
    testImplementation(libs.kotlinx.coroutines.test)
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

    coordinates(group.toString(), "kartog-headless", version.toString())

    pom {
        name = "kartog-headless"
        description = "Map images on a server, JVM only: coordinates in, WebP or PNG out, with Skia's native libraries for Linux and macOS bundled."
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
