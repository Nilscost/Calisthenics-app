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

    @Test fun recoveryBlocksNeverUseCoolDownOnlyOrOptInStretches() {
        val p = (buildTrainPlan(catalog, StarterRoutine.routine, none, SeedProfiles.home, TrainSettings(rounds = 4, stretchOn = true)) as PlanResult.Ready).plan
        val used = p.blocks.filter { it.type == BlockType.STRETCH }.mapNotNull { it.variationId }.toSet()
        assertTrue(used.isNotEmpty())
        for (id in used) { val v = catalog.variation(id)!!; assertTrue(id, v.stretchUse != StretchUse.COOL_DOWN && !v.optIn) }
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
