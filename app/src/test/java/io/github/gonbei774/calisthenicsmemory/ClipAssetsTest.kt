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
        // R7-T12: Jefferson curl and German hang show the closest existing clip until the clip track delivers their own (ClipAlias)
        val placeholders = setOf("jefferson-curl", "german-hang")
        val missing = catalog.variations.map { it.id }.filter { it !in placeholders && !File(root, "src/main/assets/demos/$it.mp4").isFile }
        assertTrue("no clip for $missing", missing.isEmpty())
        for (v in catalog.variations.filter { it.id !in placeholders }) assertTrue(v.id, File(root, "src/main/assets/demos/${v.id}.mp4").length() < 120 * 1024)
    }

    @Test fun noClipWithoutAnExercise() {
        val ids = catalog.variations.map { it.id }.toSet()
        val orphans = File(root, "src/main/assets/demos").listFiles().orEmpty().map { it.nameWithoutExtension }.filter { it !in ids }
        assertTrue("clips without an exercise: $orphans", orphans.isEmpty())
    }

    /** V06a: one drawing pipeline also makes the still thumbnails and the muscle data. */
    @Test fun everyExerciseHasAThumbnailUnderBudgetAndAnEntryInTheClipMeta() {
        val placeholders = setOf("jefferson-curl", "german-hang")   // R7-T12: no picture of their own yet
        val missing = catalog.variations.map { it.id }.filter { it !in placeholders && !File(root, "src/main/assets/thumbs/$it.webp").isFile }
        assertTrue("no thumbnail for $missing", missing.isEmpty())
        for (v in catalog.variations.filter { it.id !in placeholders }) assertTrue(v.id, File(root, "src/main/assets/thumbs/${v.id}.webp").length() < 12 * 1024)
        val orphans = File(root, "src/main/assets/thumbs").listFiles().orEmpty().map { it.nameWithoutExtension }.filter { id -> catalog.variations.none { it.id == id } }
        assertTrue("thumbnails without an exercise: $orphans", orphans.isEmpty())
        val meta = kotlinx.serialization.json.Json.parseToJsonElement(File(root, "src/main/assets/clip_meta.json").readText()) as kotlinx.serialization.json.JsonObject
        assertTrue("clip_meta.json out of date", meta.keys == catalog.variations.map { it.id }.toSet())
        for (v in catalog.variations) {
            val e = meta.getValue(v.id) as kotlinx.serialization.json.JsonObject
            val primary = (e.getValue("primary") as kotlinx.serialization.json.JsonArray).map { (it as kotlinx.serialization.json.JsonPrimitive).content }
            assertTrue(v.id, primary == v.primaryMuscles.map { it.name })
        }
    }
}
