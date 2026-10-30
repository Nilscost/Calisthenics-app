plugins { kotlin("multiplatform") version "2.4.21" }

val contractSource = file("../../../data/src/commonTest/kotlin/app/calisthenics/data/history/HistoryRepositoryContractTest.kt")
val generatedContracts = layout.buildDirectory.dir("generated/contracts")
val copyContract = tasks.register<Copy>("copyRepositoryContract") {
    from(contractSource)
    into(generatedContracts)
}

@OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)
kotlin {
    wasmJs { browser(); binaries.executable() }
    sourceSets {
        wasmJsMain {
            kotlin.srcDir(generatedContracts)
            dependencies {
                implementation("app.calisthenics:data")
                implementation(kotlin("test"))
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0")
            }
        }
    }
}
tasks.named("compileKotlinWasmJs") { dependsOn(copyContract) }

plugins.withType<org.jetbrains.kotlin.gradle.targets.wasm.nodejs.WasmNodeJsPlugin> {
    extensions.getByType<org.jetbrains.kotlin.gradle.targets.wasm.nodejs.WasmNodeJsEnvSpec>().download.set(false)
}
plugins.withType<org.jetbrains.kotlin.gradle.targets.wasm.binaryen.BinaryenPlugin> {
    extensions.getByType<org.jetbrains.kotlin.gradle.targets.wasm.binaryen.BinaryenEnvSpec>().download.set(false)
}
