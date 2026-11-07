plugins { id("com.android.application") version "9.1.1" }
configurations.configureEach { exclude(group = "androidx.lifecycle") }
android {
    namespace = "app.calisthenics.personal.oracleprobe"
    compileSdk = 37
    defaultConfig { applicationId = "app.calisthenics.personal.oracleprobe"; minSdk = 26; targetSdk = 35 }
    signingConfigs {
        getByName("debug") {
            storeFile = file(providers.gradleProperty("debugKeyPath").get())
            storePassword = "android"
            keyAlias = "calisthenicsdebug"
            keyPassword = "android"
        }
    }
    buildTypes { getByName("debug") { signingConfig = signingConfigs.getByName("debug") } }
}
dependencies {
    implementation("androidx.test:runner:1.6.1")
    implementation("androidx.test.ext:junit:1.2.1")
}
