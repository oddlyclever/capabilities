import org.gradle.kotlin.dsl.the
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootExtension
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootPlugin
import org.jetbrains.kotlin.gradle.targets.wasm.nodejs.WasmNodeJsRootExtension
import org.jetbrains.kotlin.gradle.targets.wasm.nodejs.WasmNodeJsRootPlugin
import org.jetbrains.kotlin.gradle.targets.js.yarn.YarnPlugin
import org.jetbrains.kotlin.gradle.targets.js.yarn.YarnRootExtension

plugins {
    alias(libs.plugins.multiplatformLibrary) apply false
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.vanniktech.mavenPublish) apply false
    // Provisioned for modules that need Compose; Compose-free modules do not apply them.
    alias(libs.plugins.composeMultiplatform) apply false
    alias(libs.plugins.composeCompiler) apply false
    // kotlinSerialization for modules that serialize, kotlinJvm for JVM-only server modules. Declared here so
    // subprojects apply them without a version clash.
    alias(libs.plugins.kotlinSerialization) apply false
    alias(libs.plugins.kotlinJvm) apply false
}

rootProject.plugins.withType<YarnPlugin> {
    rootProject.the<YarnRootExtension>().apply {
        lockFileDirectory = project.rootDir.resolve("gradle/kotlin-js-store")
        yarnLockMismatchReport = org.jetbrains.kotlin.gradle.targets.js.yarn.YarnLockMismatchReport.WARNING
        reportNewYarnLock = false
        yarnLockAutoReplace = true
    }
}

// webpack for the browser tests and bundles, overriding the 5.108.1 that Kotlin 2.4.20 ships. That version rewrites
// the uses of an aliased `import.meta` (`const m = import.meta`) but leaves the `import.meta` in the declaration, so a
// classic-script bundle containing one fails to parse and the browser runs no tests at all. kotlinx-io's wasm Node.js
// support has exactly that line. Fixed in the catalog version. Remove once Kotlin's own tooling ships a webpack that is.
rootProject.plugins.withType<NodeJsRootPlugin> {
    rootProject.the<NodeJsRootExtension>().versions.webpack.version = libs.versions.webpack.get()
}
rootProject.plugins.withType<WasmNodeJsRootPlugin> {
    rootProject.the<WasmNodeJsRootExtension>().versions.webpack.version = libs.versions.webpack.get()
}
