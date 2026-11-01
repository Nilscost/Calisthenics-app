// Shared Compose Multiplatform UI foundation (Phase 3a): theme, fonts, strings, icons.
plugins {
    kotlin("multiplatform")
    id("com.android.kotlin.multiplatform.library")
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.compose.multiplatform)
}

@OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)
kotlin {
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
            api(compose.runtime)
            api(compose.foundation)
            api(compose.material3)
            api(compose.components.resources)
        }
        androidMain.dependencies {
            implementation("androidx.core:core-ktx:1.15.0")
        }
    }
}

compose.resources {
    packageOfResClass = "io.github.gonbei774.calisthenicsmemory.shared.resources"
    publicResClass = true
}
