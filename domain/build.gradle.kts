// Shared domain logic; Android consumes the JVM target and web consumes wasmJs.
plugins {
    kotlin("multiplatform")
    id("org.jetbrains.kotlin.plugin.serialization")
}

@OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)
kotlin {
    jvm {
        compilerOptions { jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17) }
    }
    wasmJs { nodejs() }

    sourceSets {
        commonMain.dependencies {
            implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
            api("org.jetbrains.kotlinx:kotlinx-datetime:0.8.0")
            implementation("org.kotlincrypto.hash:sha2:0.8.0")
        }
        commonTest.dependencies { implementation(kotlin("test")) }
        wasmJsMain.dependencies { implementation(npm("@js-joda/timezone", "2.23.0")) }
    }
}

tasks.named<Test>("jvmTest") {
    systemProperty("repo.root", rootProject.projectDir.absolutePath)
    systemProperty("repoRoot", rootProject.projectDir.absolutePath)
    systemProperty("write.seed", System.getenv("WRITE_SEED") ?: "")
    inputs.property("writeSeed", System.getenv("WRITE_SEED") ?: "")
    testLogging { events("failed"); exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL }
}

tasks.register("test") { dependsOn("jvmTest") }

// Common tests consume the actual repository fixtures on every target.
val fixturePaths = listOf(
    "content/starter/catalog.json",
    "docs/23-new-exercises.json",
    "app/src/debug/assets/seed/backup.json",
    "app/src/debug/assets/seed/levels.json",
)
val fixtureOutput = layout.buildDirectory.dir("generated/commonTestFixtures")
val generateTestFixtures = tasks.register("generateTestFixtures") {
    val inputsByPath = fixturePaths.associateWith { rootProject.file(it) }
    inputs.files(inputsByPath.values)
    outputs.dir(fixtureOutput)
    doLast {
        val destination = fixtureOutput.get().asFile
        destination.deleteRecursively()
        destination.mkdirs()
        val dispatch = StringBuilder("package app.calisthenics.domain\n\ninternal fun fixtureText(path: String): String = when (path) {\n")
        inputsByPath.entries.forEachIndexed { index, (path, file) ->
            val content = file.readText(Charsets.UTF_8)
            // Each literal and generated function stays below JVM class-file limits.
            val chunks = mutableListOf<String>()
            var start = 0
            while (start < content.length) {
                var end = minOf(start + 4000, content.length)
                if (end < content.length && Character.isHighSurrogate(content[end - 1]) && Character.isLowSurrogate(content[end])) end--
                chunks += content.substring(start, end)
                start = end
            }
            chunks.forEachIndexed { part, chunk ->
                val escaped = buildString {
                    chunk.forEach { ch ->
                        when (ch) {
                            '\\' -> append("\\\\")
                            '"' -> append("\\\"")
                            '$' -> append("\\$")
                            '\n' -> append("\\n")
                            '\r' -> append("\\r")
                            '\t' -> append("\\t")
                            else -> if (ch.code < 32) append("\\u%04x".format(ch.code)) else append(ch)
                        }
                    }
                }
                destination.resolve("Fixture${index}Part$part.kt").writeText(
                    "package app.calisthenics.domain\ninternal fun fixture${index}Part$part(): String = \"$escaped\"\n", Charsets.UTF_8
                )
            }
            dispatch.append("    \"").append(path).append("\" -> buildString {\n")
            chunks.indices.forEach { part -> dispatch.append("        append(fixture${index}Part$part())\n") }
            dispatch.append("    }\n")
        }
        dispatch.append("    else -> error(\"Unknown fixture: \$path\")\n}\n")
        destination.resolve("FixtureText.kt").writeText(dispatch.toString(), Charsets.UTF_8)
    }
}
kotlin.sourceSets.named("commonTest") { kotlin.srcDir(fixtureOutput) }
tasks.matching { it.name == "compileTestKotlinJvm" || it.name == "compileTestKotlinWasmJs" }.configureEach {
    dependsOn(generateTestFixtures)
}
