// :domain — pure Kotlin/JVM domain module (T03).
// Spec §1: no Android imports, no wall-clock globals, no network, no Room.
// Seed for the progression engine (T12/T13, ADR 0002 §C).
plugins {
    alias(libs.plugins.kotlin.jvm)
}

java {
    // Bytecode target 17 (spec §6) without pinning a toolchain: the host JDK
    // (21 here) compiles to 17-class files via these compatibility settings.
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

dependencies {
    testImplementation(libs.junit)
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}
