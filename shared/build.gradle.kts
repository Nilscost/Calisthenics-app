// Shared Compose Multiplatform UI foundation (Phase 3a): theme, fonts, strings, icons.
plugins {
    kotlin("multiplatform")
    id("com.android.kotlin.multiplatform.library")
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.compose.multiplatform)
    id("org.jetbrains.kotlin.plugin.serialization")
}

@OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)
kotlin {
    // PlatformContext and the clip handle are expect classes with Android typealias actuals.
    compilerOptions { freeCompilerArgs.add("-Xexpect-actual-classes") }
    android {
        namespace = "io.github.gonbei774.calisthenicsmemory.shared"
        compileSdk = 37
        minSdk = 26
        compilerOptions { jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17) }
        androidResources { enable = true }
    }
    wasmJs { browser() }
    sourceSets {
        commonMain.dependencies {
            api(project(":domain"))
            api(project(":data"))
            implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
            implementation("org.jetbrains.androidx.lifecycle:lifecycle-runtime-compose:2.9.6")
            api(compose.runtime)
            api(compose.foundation)
            api(compose.material3)
            api(compose.components.resources)
        }
        androidMain.dependencies {
            implementation("androidx.core:core-ktx:1.15.0")
            implementation("androidx.activity:activity-compose:1.9.3")
            // Licences list: Android-only behind expect/actual LicensesScreen (the generator plugin stays in :app).
            implementation("com.mikepenz:aboutlibraries-core:11.2.3")
            implementation("com.mikepenz:aboutlibraries-compose-m3:11.2.3")
        }
    }
}

compose.resources {
    packageOfResClass = "io.github.gonbei774.calisthenicsmemory.shared.resources"
    publicResClass = true
}
