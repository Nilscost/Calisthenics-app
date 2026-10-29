package app.calisthenics.domain

import java.io.File
import kotlin.test.Test
import kotlin.test.assertContentEquals

class SeedFixtureDiskTest {
    @Test fun committedFilesMatchGeneratedFixture() {
        val root = File(requireNotNull(System.getProperty("repoRoot")))
        val dir = File(root, "app/src/debug/assets/seed")
        val (backup, levels) = SeedFixtureTest().build()
        val files = listOf(File(dir, "backup.json") to backup, File(dir, "levels.json") to levels)
        if (System.getProperty("write.seed") == "1") {
            dir.mkdirs()
            files.forEach { (file, text) -> file.writeText(text, Charsets.UTF_8) }
        }
        files.forEach { (file, text) -> assertContentEquals(text.toByteArray(Charsets.UTF_8), file.readBytes(), file.path) }
    }
}
