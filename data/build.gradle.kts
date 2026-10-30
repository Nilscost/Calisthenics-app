// Common repositories/settings; Android retains the existing Room database and schema.
plugins {
    kotlin("multiplatform")
    id("com.android.kotlin.multiplatform.library")
    id("com.google.devtools.ksp")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("androidx.room")
}

// Keep the schema verifier's previously validated native SQLite runtime.
configurations.configureEach {
    resolutionStrategy { force("org.xerial:sqlite-jdbc:3.45.3.0") }
}

@OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)
kotlin {
    android {
        namespace = "io.github.gonbei774.calisthenicsmemory.data"
        compileSdk = 35
        minSdk = 26
        compilerOptions { jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17) }
        withHostTestBuilder {}.configure {}
        withDeviceTestBuilder { sourceSetTreeName = "test" }.configure {
            instrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        }
        androidResources { enable = true }
    }
    wasmJs { nodejs() }
    sourceSets {
        commonMain.dependencies {
            api(project(":domain"))
            implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
            api("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")
            implementation("com.russhwolf:multiplatform-settings:1.3.0")
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0")
        }
        androidMain.dependencies {
            api("androidx.room:room-runtime:2.8.5")
            api("androidx.room:room-ktx:2.8.5")
            implementation("androidx.core:core-ktx:1.15.0")
        }
        named("androidDeviceTest") { dependencies {
            implementation("androidx.test.ext:junit:1.2.1")
            implementation("androidx.test:core:1.5.0")
            implementation("androidx.test:runner:1.6.1")
            implementation("androidx.room:room-testing:2.8.5")
            implementation("androidx.sqlite:sqlite-framework:2.4.0")
        } }
    }
}

dependencies { add("kspAndroid", "androidx.room:room-compiler:2.8.5") }
room { schemaDirectory("$projectDir/schemas") }

extensions.configure<com.android.build.api.variant.KotlinMultiplatformAndroidComponentsExtension> {
    onVariants { variant ->
        variant.deviceTests.values.forEach { test ->
            test.sources.assets?.addStaticSourceDirectory("schemas")
        }
    }
}

// Preserve existing CI/local command entry points; these execute the new test tasks.
tasks.register("testDebugUnitTest") { dependsOn("testAndroidHostTest") }
tasks.register("connectedDebugAndroidTest") { dependsOn("connectedAndroidDeviceTest") }
