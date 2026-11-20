package app.calisthenics.domain

import app.calisthenics.domain.model.*
import app.calisthenics.domain.planner.*
import app.calisthenics.domain.progression.ProgressSnapshot
import kotlin.test.Test

class OptionalPoolsTest {
    private val profile = EquipmentProfile("test", "Test", emptyList(), emptySet())
    private val progress = ProgressSnapshot(emptyMap(), emptyMap(), emptyMap(), emptyList())
    private fun catalog(held: Boolean = false): Catalog {
        val base = Fx.catalog()
        val interval = Fx.strength("interval", pattern = Pattern.CONDITIONING).copy(demonstrationHold = held)
        val prep = Fx.strength("prep")
        return base.copy(variations = base.variations + interval + prep, policies = base.policies + Fx.policy("interval") + Fx.policy("prep"), progressionRoutes = listOf(
            ProgressionRoute("push", "Push", "pushup", mainPath = listOf("pushup-std"), members = listOf(ProgressionMember("pushup-std", ProgressionRole.MAIN)), defaultPool = true),
            ProgressionRoute("intervals", "Intervals", "conditioning", members = listOf(ProgressionMember("interval", ProgressionRole.LIBRARY)), focus = "CONDITIONING"),
            ProgressionRoute("prep", "Preparation", "prep", members = listOf(ProgressionMember("prep", ProgressionRole.SUPPORT, "pushup-std")), focus = "PREHAB"),
        ))
    }
    private fun plan(pools: Set<String>, held: Boolean = false): WorkoutPlan {
        val routine = Routine("test", 1, "Test", listOf(RoutineSlot("push", Pattern.PUSH_HORIZONTAL, Area.UPPER_BODY, "pushup-std")))
        return (buildTrainPlan(catalog(held), routine, progress, profile, TrainSettings(rounds = 1, stretchOn = false, additionalPools = pools)) as PlanResult.Ready).plan
    }
    @Test fun conditioningIsAnOptInTimedFinisherWithoutAchievementTargets() {
        assertTrue(plan(emptySet()).blocks.none { it.variationId == "interval" })
        val p = plan(setOf("CONDITIONING"))
        val work = p.blocks.filter { it.variationId == "interval" }
        assertEquals(3, work.size)
        assertTrue(work.all { it.durationSeconds == 30 && it.target == null && it.prescriptionTier == null })
        assertTrue(p.blocks.indexOf(work.first()) > p.blocks.indexOfLast { it.slotId == "push" && it.type == BlockType.WORK })
        assertEquals(p.blocks.sumOf { it.durationSeconds }, p.plannedDurationSeconds)
    }
    @Test fun aHeldDemonstrationIsNotPickedEvenForAnOptInFinisher() {
        assertTrue(plan(setOf("CONDITIONING"), held = true).blocks.none { it.variationId == "interval" })
    }
    @Test fun preparationIsAttachedToTheSessionsExerciseAndPrecedesWork() {
        val p = plan(setOf("PREHAB"))
        assertEquals("prep", p.blocks.first().variationId)
        assertEquals(BlockType.WARMUP, p.blocks.first().type)
        assertTrue(p.blocks.first().target == null && p.blocks.first().prescriptionTier == null)
    }

    @Test fun theDefaultExerciseCountKeepsDeliberatelySelectedPowerWork() {
        val base = catalog()
        val jump = Fx.strength("jump", "jump", Pattern.SQUAT, Area.LOWER_BODY).copy(progressionGroup = "jump")
        val c = base.copy(variations = base.variations + jump, policies = base.policies + Fx.policy("jump"),
            progressionRoutes = base.progressionRoutes + ProgressionRoute("power", "Power", "jump", mainPath = listOf("jump"),
                members = listOf(ProgressionMember("jump", ProgressionRole.MAIN)), focus = "POWER"))
        val routine = Routine("test", 1, "Test", listOf(RoutineSlot("push", Pattern.PUSH_HORIZONTAL, Area.UPPER_BODY, "pushup-std")))
        val settings = withDefaultLength(c, routine, progress, profile,
            TrainSettings(rounds = 1, stretchOn = false, additionalPools = setOf("POWER")))
        val p = (buildTrainPlan(c, routine, progress, profile, settings) as PlanResult.Ready).plan
        assertTrue(p.blocks.any { it.type == BlockType.WORK && it.variationId == "jump" })
        assertEquals(settings.exercisesPerRound, exercisesPerRoundOf(p))
    }
}
