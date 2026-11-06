package app.calisthenics.domain

import app.calisthenics.domain.content.parseCatalog
import app.calisthenics.domain.equipment.SeedProfiles
import app.calisthenics.domain.model.BlockType
import app.calisthenics.domain.planner.*
import app.calisthenics.domain.progression.*
import app.calisthenics.domain.routine.StarterRoutine
import kotlin.test.Test

/** 9.2 (owner feedback 2026-11-06): the half one-arm push-up is a main step after the archer, never prescribed beside it. */
class HalfOneArmRouteTest {
    private val c = parseCatalog(fixtureText("content/starter/catalog.json"))
    private val half = "pushup-half-one-arm"

    @Test fun anArcherAtLevelThreeNeverPlansTheHalfOneArmPushUp() {
        val base = ProgressionEngine(c).replay(emptyList(), listOf(UserAction.SelfAssessment(0, "pushup-archer", 3)))
        assertEquals("pushup-archer", base.activeInFamily[c.variation("pushup-archer")!!.familyId])
        var sawArcher = false
        for (count in 0..7) {
            val p = base.copy(routeWorkoutCounts = mapOf("pushup" to count))
            for (rounds in listOf(3, 4, 6)) {
                val plan = (buildTrainPlan(c, StarterRoutine.routine, p, SeedProfiles.home, TrainSettings(rounds = rounds)) as PlanResult.Ready).plan
                val used = plan.blocks.filter { it.type == BlockType.WORK }.mapNotNull { it.variationId }.toSet()
                assertFalse("count $count rounds $rounds: $used", half in used)
                if ("pushup-archer" in used) sawArcher = true
            }
        }
        assertTrue("the archer itself is still planned", sawArcher)
    }
}
