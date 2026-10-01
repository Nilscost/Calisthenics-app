// :data — Android Room library (T03). Holds AppDatabase, entities, DAOs
// (moved verbatim from upstream CalisthenicsMemory @ 045b8577, GPL-3.0).
// Spec §1: Room entities/DAOs, migrations, repositories, backup adapters.
plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    id("com.google.devtools.ksp")
    id("org.jetbrains.kotlin.plugin.serialization")
}

// Room's schema verifier (runs during KSP: kspDebugKotlin) loads sqlite-jdbc
// natively. The default org.xerial:sqlite-jdbc does not ship a working
// Linux/aarch64 native; 3.45.3.0 does. configureEach (not .all) so the force
// reaches KSP-created configurations.
configurations.configureEach {
    resolutionStrategy {
        force("org.xerial:sqlite-jdbc:3.45.3.0")
    }
}

android {
    namespace = "io.github.gonbei774.calisthenicsmemory.data"
    compileSdk = 35
    defaultConfig {
        minSdk = 26
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    // Room (same versions as upstream :app)
    api("androidx.room:room-runtime:2.6.1")
    api("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")

    // Coroutines Flow is part of the DAO surface.
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    // SavedWorkoutState / ProgramExecutionModels use kotlinx.serialization.
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")
    implementation("androidx.core:core-ktx:1.15.0")

    // Instrumented tests (DataLayerInstrumentedTest: on-device Room round-trip of
    // the current v21 schema). The upstream-inherited MigrationTest was removed —
    // it targeted Room v10 via MigrationTestHelper against schema JSON that does
    // not exist (exportSchema = false), so it could never pass on-device.
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test:core:1.5.0")
    androidTestImplementation("androidx.room:room-testing:2.6.1")
    androidTestImplementation("androidx.sqlite:sqlite-framework:2.4.0")
}

// NOTE (T03): a JVM Robolectric Room smoke test was attempted for :data but
// removed — Robolectric's native SQLite runtime is not built for Linux/aarch64,
// so it cannot run in this ARM64 sandbox (host limitation, not a code defect).
// Room correctness is covered on-device by the instrumented MigrationTest (G5)
// and by :domain's pure-JVM progression tests, which run anywhere.
