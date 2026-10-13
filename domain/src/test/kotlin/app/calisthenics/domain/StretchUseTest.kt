package app.calisthenics.domain

import app.calisthenics.domain.equipment.SeedProfiles
import app.calisthenics.domain.model.*
import app.calisthenics.domain.planner.*
import app.calisthenics.domain.routine.StarterRoutine
import org.junit.Assert.*
import org.junit.Test
import java.io.File

/** L11: the planner puts only gentle (between-sets) stretches into the recovery blocks. */
class StretchUseTest {
    private val catalog = app.calisthenics.domain.content.parseCatalog(File(System.getProperty("repo.root"), "content/starter/catalog.json").readText())
    private val none = app.calisthenics.domain.progression.ProgressSnapshot(emptyMap(), emptyMap(), emptyMap(), emptyList())

    // R7-T12 (D8): the old assertion (no cool-down-only stretch between sets) is replaced: only opt-in stretches stay out of the automatic rotation
    @Test fun recoveryBlocksNeverUseOptInStretchesButDeepOnesAreAllowed() {
        val p = (buildTrainPlan(catalog, StarterRoutine.routine, none, SeedProfiles.home, TrainSettings(rounds = 6, stretchOn = true)) as PlanResult.Ready).plan
        val used = p.blocks.filter { it.type == BlockType.STRETCH }.mapNotNull { it.variationId }.toSet()
        assertTrue(used.isNotEmpty())
        for (id in used) assertFalse(id, catalog.variation(id)!!.optIn)
        assertTrue("a deep stretch is used somewhere in the rotation: $used", used.any { catalog.variation(it)!!.stretchUse == StretchUse.COOL_DOWN })
    }

    @Test fun aDeepStretchBetweenSetsGetsItsOwnTimePerSideAndNeverLessThanTheRest() {
        for ((id, perSide) in listOf("stretch-pigeon" to true, "stretch-pancake" to false)) {
            val own = catalog.variation(id)!!.defaultSeconds!!
            assertTrue("$id has a realistic hold", own in 45..60)
            val p = (buildTrainPlan(catalog, StarterRoutine.routine, none, SeedProfiles.home, TrainSettings(rounds = 2, stretchOn = true), edits = PlanEdits(stretchPicks = mapOf("push" to id))) as PlanResult.Ready).plan
            val blocks = p.blocks.filter { it.slotId == "push" && it.roundIndex == 1 && it.variationId == id }
            assertEquals(if (perSide) 2 else 1, blocks.size)
            assertTrue(blocks.all { it.durationSeconds >= own })
        }
    }

    @Test fun aHandPickedStretchStillWorksEvenIfItIsCoolDownOnly() {
        val picks = mapOf("push" to "stretch-pigeon")
        val p = (buildTrainPlan(catalog, StarterRoutine.routine, none, SeedProfiles.home, TrainSettings(rounds = 2, stretchOn = true), edits = PlanEdits(stretchPicks = picks)) as PlanResult.Ready).plan
        assertTrue(p.blocks.any { it.variationId == "stretch-pigeon" })
    }

    @Test fun oldVariationsWithoutTheNewFieldsDecodeAsBoth() {
        val j = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }
        val v = j.decodeFromString(ExerciseVariation.serializer(), """{"id":"s","familyId":"s","name":"S","patterns":["STRETCH"],"kind":"STRETCH"}""")
        assertEquals(StretchUse.BOTH, v.stretchUse); assertFalse(v.optIn)
    }
}
