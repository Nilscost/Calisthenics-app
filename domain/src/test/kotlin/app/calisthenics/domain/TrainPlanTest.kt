package app.calisthenics.domain

import app.calisthenics.domain.content.parseCatalog
import app.calisthenics.domain.equipment.SeedProfiles
import app.calisthenics.domain.model.*
import app.calisthenics.domain.planner.*
import app.calisthenics.domain.progression.ProgressSnapshot
import app.calisthenics.domain.routine.StarterRoutine
import org.junit.Assert.*
import org.junit.Test
import java.io.File

class TrainPlanTest {
    private val catalog = parseCatalog(File(System.getProperty("repo.root"), "content/starter/catalog.json").readText())
    private val none = ProgressSnapshot(emptyMap(), emptyMap(), emptyMap(), emptyList())
    private fun plan(s: TrainSettings = TrainSettings(), swaps: Map<String, String> = emptyMap()) =
        buildTrainPlan(catalog, StarterRoutine.routine, none, SeedProfiles.home, s, swaps)

    @Test fun defaultIsFourRoundsAndAboutAClassSessionLong() {
        val p = (plan() as PlanResult.Ready).plan
        assertEquals(4, p.rounds)
        assertTrue("4 rounds = ${p.plannedDurationSeconds / 60} min", p.plannedDurationSeconds in 40 * 60..50 * 60)
    }

    @Test fun moreRoundsMeansMoreMinutes() {
        val mins = (1..6).map { r -> (plan(TrainSettings(rounds = r)) as PlanResult.Ready).plan.plannedDurationSeconds }
        assertTrue(mins.zipWithNext().all { (a, b) -> b > a })
    }

    @Test fun roundsAreClampedToWhatThePlannerAllows() {
        assertEquals(1, (plan(TrainSettings(rounds = 0)) as PlanResult.Ready).plan.rounds)
        assertEquals(MAX_EXPLICIT_ROUNDS, (plan(TrainSettings(rounds = 99)) as PlanResult.Ready).plan.rounds)
    }

    @Test fun timedStyleIsReflectedInThePlan() {
        assertTrue((plan(TrainSettings(timed = true)) as PlanResult.Ready).plan.timed)
        assertFalse((plan(TrainSettings(timed = false)) as PlanResult.Ready).plan.timed)
    }

    @Test fun focusFollowsTheGoalUnlessTheUserPicksOne() {
        assertEquals(setOf(StrengthFocus.UPPER_BODY), TrainSettings(goalId = "body-upper").effectiveFocus())
        assertEquals(setOf(StrengthFocus.CORE), TrainSettings(goalId = "body-upper", focus = setOf(StrengthFocus.CORE)).effectiveFocus())
        val upper = (plan(TrainSettings(goalId = "body-upper")) as PlanResult.Ready).plan
        assertEquals(setOf(StrengthFocus.UPPER_BODY), upper.focus)
        val slots = upper.blocks.filter { it.type == BlockType.WORK }.mapNotNull { it.variationId }.map { catalog.variation(it)!! }
        assertTrue(slots.all { Area.UPPER_BODY in it.areas })
    }

    @Test fun aSkillGoalPutsTheStepTowardItFirst() {
        val p = (plan(TrainSettings(goalId = "hspu")) as PlanResult.Ready).plan
        assertEquals("hspu", p.goalId)
        assertEquals("goal", p.blocks.first { it.type == BlockType.WORK }.slotId)
        // with no assessed level the next step is the prerequisite that unlocks the chain, not a skill that is still locked
        assertNotEquals("hspu-wall", p.blocks.first { it.type == BlockType.WORK }.variationId)
    }

    @Test fun swapsApplyToTodayOnly() {
        val base = (plan() as PlanResult.Ready).plan.blocks.first { it.slotId == "push" && it.type == BlockType.WORK }.variationId
        val swapped = (plan(swaps = mapOf("push" to "pushup-knee")) as PlanResult.Ready).plan.blocks.first { it.slotId == "push" && it.type == BlockType.WORK }.variationId
        assertEquals("pushup-incline", base); assertEquals("pushup-knee", swapped)
    }

    @Test fun placeholderPlanIdMustBeReplacedByTheCaller() {
        assertEquals("draft", (plan() as PlanResult.Ready).plan.id)
    }

    @Test fun swapOptionsAreSamePatternUsableAndStartWithTheCurrentOne() {
        val routine = trainRoutine(catalog, StarterRoutine.routine, none, "general")
        val opts = swapOptions(catalog, routine, "push", SeedProfiles.home, "pushup-incline")
        assertEquals("pushup-incline", opts.first().id)
        assertTrue(opts.size > 1)
        val slot = routine.slots.first { it.id == "push" }
        assertTrue(opts.all { slot.area in it.areas && slot.intent in it.patterns })
        assertTrue(opts.all { app.calisthenics.domain.equipment.isAvailable(it, SeedProfiles.home) })
        assertTrue(opts.none { it.kind == Kind.STRETCH })
        // equipment decides: a bar-less profile never offers a pull-up for the pull slot
        val noBar = SeedProfiles.travel
        assertTrue(swapOptions(catalog, routine, "pull", noBar, "row-band").drop(1).none { "pullup" in it.id })
        assertTrue(swapOptions(catalog, routine, "nope", SeedProfiles.home, "x").isEmpty())
    }
}
