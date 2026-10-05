package io.github.gonbei774.calisthenicsmemory

import app.calisthenics.domain.content.parseCatalog
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** U11: every exercise in the catalog ships a demo clip, and each stays small. */
class ClipAssetsTest {
    private val root = File(System.getProperty("user.dir"))
    private val catalog = parseCatalog(File(root, "src/main/assets/catalog.json").readText())

    @Test fun everyCatalogExerciseHasAClipUnderTheSizeBudget() {
        val missing = catalog.variations.map { it.id }.filter { !File(root, "src/main/assets/demos/$it.mp4").isFile }
        assertTrue("no clip for $missing", missing.isEmpty())
        for (v in catalog.variations) assertTrue(v.id, File(root, "src/main/assets/demos/${v.id}.mp4").length() < 120 * 1024)
    }

    @Test fun noClipWithoutAnExercise() {
        val ids = catalog.variations.map { it.id }.toSet()
        val orphans = File(root, "src/main/assets/demos").listFiles().orEmpty().map { it.nameWithoutExtension }.filter { it !in ids }
        assertTrue("clips without an exercise: $orphans", orphans.isEmpty())
    }
}
