// Web app (Phase 3c): wasmJs entry point hosting the shared Compose UI. Run `:web:wasmJsBrowserDistribution`
// for a static folder (build/dist/wasmJs/productionExecutable) that any HTTP server can serve.
plugins {
    kotlin("multiplatform")
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.compose.multiplatform)
}

@OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)
kotlin {
    wasmJs {
        outputModuleName.set("calisthenics-web")
        browser { commonWebpackConfig { outputFileName = "calisthenics-web.js" } }
        binaries.executable()
    }
    sourceSets {
        wasmJsMain {
            // Catalog and privacy text are copied from their single source in the Android assets (nothing committed twice).
            resources.srcDir(layout.buildDirectory.dir("generated/webAssets"))
            dependencies {
                implementation(project(":shared"))
                implementation(compose.runtime)
                implementation(compose.foundation)
                implementation(compose.material3)
            }
        }
    }
}

val copyWebAssets = tasks.register<Copy>("copyWebAssets") {
    from(rootProject.layout.projectDirectory.dir("app/src/main/assets")) {
        include("catalog.json", "catalog_meta.json", "privacy.md")
    }
    into(layout.buildDirectory.dir("generated/webAssets/data"))
}
tasks.matching { it.name == "wasmJsProcessResources" }.configureEach { dependsOn(copyWebAssets) }

// The Binaryen distribution repository is declared in settings.gradle.kts (project repositories are not allowed).
plugins.withType<org.jetbrains.kotlin.gradle.targets.wasm.binaryen.BinaryenPlugin> {
    extensions.getByType<org.jetbrains.kotlin.gradle.targets.wasm.binaryen.BinaryenEnvSpec>().downloadBaseUrl.set(null as String?)
}
