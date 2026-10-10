import java.util.Properties
import java.io.FileInputStream

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("com.google.devtools.ksp")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("com.mikepenz.aboutlibraries.plugin")
}

// AboutLibraries: exclude timestamp for reproducible builds
// See: https://codeberg.org/Gonbei774/CalisthenicsMemory/issues/2
aboutLibraries {
    excludeFields = arrayOf("generated")
    configPath = "config/aboutlibraries"
}

android {
    namespace = "io.github.gonbei774.calisthenicsmemory"
    compileSdk = 35

    defaultConfig {
        // Spec §1: namespace/application ID proposal app.calisthenics.personal;
        // working display name "Calisthenics" (values/strings.xml).
        applicationId = "app.calisthenics.personal"
        minSdk = 26
        targetSdk = 35
        versionCode = 9
        versionName = "0.9.0-r6"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    // Keystore設定を読み込み
    val keystorePropertiesFile = rootProject.file("keystore.properties")
    val keystoreProperties = Properties()
    if (keystorePropertiesFile.exists()) {
        keystoreProperties.load(FileInputStream(keystorePropertiesFile))
    }

    // 署名設定
    signingConfigs {
        // One shared DEBUG key (not a secret: debug builds only) so APKs built in the sandbox
        // and on the Mac can update each other without wiping data. Path comes from the Gradle
        // property calisthenics.debugKeystore; absent = Android's default debug key.
        val sharedDebug = (findProperty("calisthenics.debugKeystore") as String?)?.let { file(it) }
        if (sharedDebug != null && sharedDebug.exists()) {
            getByName("debug") {
                storeFile = sharedDebug
                storePassword = "android"
                keyAlias = "calisthenicsdebug"
                keyPassword = "android"
            }
        }
        create("release") {
            if (keystorePropertiesFile.exists()) {
                storeFile = file(keystoreProperties["storeFile"].toString())
                storePassword = keystoreProperties["storePassword"].toString()
                keyAlias = keystoreProperties["keyAlias"].toString()
                keyPassword = keystoreProperties["keyPassword"].toString()
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // リリースビルド時に署名を適用
            signingConfig = signingConfigs.getByName("release")
            // 依存関係メタデータを無効化（F-Droid要件）
            dependenciesInfo {
                includeInApk = false
                includeInBundle = false
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    testOptions {
        unitTests.isIncludeAndroidResources = true
    }

    // Lint: see app/lint.xml (auto-detected by AGP). U01b removed the fork's partial translations (English only, Q3).
}

dependencies {
    // :domain (pure JVM) and :data (Room) modules — T03 (ADR 0001 path mapping).
    implementation(project(":domain"))
    implementation(project(":data"))

    // Compose BOM
    implementation(platform("androidx.compose:compose-bom:2024.10.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")

    // Activity Compose
    implementation("androidx.activity:activity-compose:1.9.3")

    // ViewModel
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")

    // Room Database
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")

    // Core KTX
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")

    // AppCompat for language settings
    implementation("androidx.appcompat:appcompat:1.7.0")

    // AboutLibraries
    implementation("com.mikepenz:aboutlibraries-core:11.2.3")
    implementation("com.mikepenz:aboutlibraries-compose-m3:11.2.3")

    // Testing
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
    testImplementation("androidx.room:room-testing:2.6.1")
    testImplementation("androidx.arch.core:core-testing:2.2.0")
    // U01: Compose UI tests on the JVM (Robolectric; android-all 14 / SDK 34 is the cached one, see @Config in the tests)
    testImplementation("org.robolectric:robolectric:4.13")
    testImplementation("androidx.compose.ui:ui-test-junit4")
    testImplementation("org.conscrypt:conscrypt-openjdk-uber:2.7.0") // 2.5.2 (Robolectric default) has no linux-aarch64 native
    testImplementation("androidx.test:core:1.6.1")
    testImplementation("androidx.test.espresso:espresso-core:3.6.1") // pins the cached version ui-test would otherwise ask 3.5.0 for
    testImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
    androidTestImplementation("androidx.arch.core:core-testing:2.2.0")
    androidTestImplementation("androidx.room:room-testing:2.6.1")
    androidTestImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
    androidTestImplementation(platform("androidx.compose:compose-bom:2024.10.01"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")   
}
