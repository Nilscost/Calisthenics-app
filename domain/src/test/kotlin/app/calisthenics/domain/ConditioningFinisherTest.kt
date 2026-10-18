package app.calisthenics.domain

import app.calisthenics.domain.content.parseCatalog
import app.calisthenics.domain.equipment.SeedProfiles
import app.calisthenics.domain.model.*
import app.calisthenics.domain.planner.*
import app.calisthenics.domain.progression.*
import app.calisthenics.domain.routine.StarterRoutine
import org.junit.Assert.*
import org.junit.Test
import java.io.File

/** R8.2-T03 (G3): the Include row's conditioning finisher: 5 to 10 minutes after the strength work, timed intervals, flow, equipment filtered. */
class ConditioningFinisherTest {
    private val c = parseCatalog(File(System.getProperty("repo.root"), "content/starter/catalog.json").readText())
    private val none = ProgressSnapshot(emptyMap(), emptyMap(), emptyMap(), emptyList())
    private fun plan(s: TrainSettings, p: ProgressSnapshot = none, profile: EquipmentProfile = SeedProfiles.home, edits: PlanEdits = PlanEdits()) =
        (buildTrainPlan(c, StarterRoutine.routine, p, profile, s, edits = edits) as PlanResult.Ready).plan
    private fun cond(p: WorkoutPlan) = p.blocks.filter { it.slotId?.startsWith("cond") == true }

    @Test fun offByDefaultAndOnAddsFiveToTenMinutesAfterTheStrengthWork() {
        assertTrue(cond(plan(TrainSettings(rounds = 2))).isEmpty())
        for (m in listOf(5, 6, 8, 10)) {
            val base = plan(TrainSettings(rounds = 2)); val p = plan(TrainSettings(rounds = 2, conditioningOn = true, conditioningMinutes = m))
            val secs = cond(p).sumOf { it.durationSeconds }
            assertTrue("$m min -> $secs s", secs in (m * 60 - 90)..(m * 60))
            assertEquals(base.plannedDurationSeconds + secs, p.plannedDurationSeconds)
            val firstCond = p.blocks.indexOfFirst { it.slotId?.startsWith("cond") == true }
            assertTrue(p.blocks.take(firstCond).any { it.type == BlockType.WORK })
            assertTrue(p.blocks.drop(firstCond).filter { it.slotId?.startsWith("cond") != true }.none { it.type == BlockType.WORK })
        }
    }

    @Test fun itIsATimedFlowCircuitWithNoRestBetweenExercisesAndNeverCountsTowardsLevels() {
        val p = plan(TrainSettings(rounds = 2, conditioningOn = true, conditioningMinutes = 6))
        val blocks = cond(p)
        val work = blocks.filter { it.type == BlockType.WORK }
        assertTrue(work.size >= 8 && work.all { it.durationSeconds == CONDITIONING_INTERVAL_SECONDS && it.target == null && it.freeTarget })
        assertTrue(blocks.filter { it.type == BlockType.PASSIVE_RECOVERY }.all { it.durationSeconds == CONDITIONING_ROUND_REST_SECONDS })
        val firstRound = work.filter { it.roundIndex == 1 }
        blocks.forEachIndexed { i, b -> if (b.type == BlockType.PASSIVE_RECOVERY) assertEquals(b.roundIndex!! + 1, blocks[i + 1].roundIndex) }   // rest only between rounds, never between exercises of a round
        assertTrue(firstRound.map { it.variationId }.distinct().size == firstRound.size)
        assertTrue(work.all { c.variation(it.variationId!!)!!.familyId in listOf("conditioning", "agility", "crawl") })
    }

    @Test fun equipmentAndLevelDecideTheExercises() {
        val withRope = SeedProfiles.home.copy(items = SeedProfiles.home.items + EquipmentItem("jump-rope"))
        val a = cond(plan(TrainSettings(rounds = 1, conditioningOn = true), profile = SeedProfiles.home)).mapNotNull { it.variationId }.toSet()
        val b = cond(plan(TrainSettings(rounds = 1, conditioningOn = true), profile = withRope)).mapNotNull { it.variationId }.toSet()
        assertFalse("jump-rope" in a); assertTrue("jump-rope" in b)
        assertTrue("squat-thrust-step-back" in a)                                // a beginner starts with the easy burpee
        val strong = ProgressionEngine(c).replay(emptyList(), listOf(UserAction.SelfAssessment(0, "pushup-standard", 6), UserAction.SelfAssessment(0, "squat-air", 6), UserAction.SelfAssessment(0, "squat-thrust", 6)))
        val s = cond(plan(TrainSettings(rounds = 1, conditioningOn = true), p = strong)).mapNotNull { it.variationId }.toSet()
        assertTrue("burpee" in s && "squat-thrust-step-back" !in s)
    }

    @Test fun minutesAndRemovedExercisesFromThePreviewAreHonouredAndTheSettingsAreSaved() {
        val removed = plan(TrainSettings(rounds = 1, conditioningOn = true), edits = PlanEdits(conditioningRemoved = setOf("squat-thrust-step-back")))
        assertTrue(cond(removed).none { it.variationId == "squat-thrust-step-back" })
        val longer = plan(TrainSettings(rounds = 1, conditioningOn = true, conditioningMinutes = 6), edits = PlanEdits(conditioningMinutes = 10))
        assertTrue(cond(longer).sumOf { it.durationSeconds } > 8 * 60)
        val saved = app.calisthenics.domain.routine.savedFrom("c", "C", 1, StarterRoutine.routine, PlanEdits(), TrainSettings(conditioningOn = true, conditioningMinutes = 8), ProgressionRule(), 1L)
        assertTrue(saved.conditioningOn); assertEquals(8, saved.conditioningMinutes)
        assertTrue(app.calisthenics.domain.routine.settingsFor(saved, TrainSettings()).conditioningOn)
    }
}
