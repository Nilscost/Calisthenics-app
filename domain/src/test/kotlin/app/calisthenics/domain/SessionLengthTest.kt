package app.calisthenics.domain

import app.calisthenics.domain.content.parseCatalog
import app.calisthenics.domain.equipment.SeedProfiles
import app.calisthenics.domain.goals.Goals
import app.calisthenics.domain.model.*
import app.calisthenics.domain.planner.*
import app.calisthenics.domain.progression.ProgressSnapshot
import app.calisthenics.domain.routine.StarterRoutine
import org.junit.Assert.*
import org.junit.Test
import java.io.File

/** R7-T08 (D1): exercises per round. A body part is filled until the session lasts 30 minutes; the stepper overrides it. */
class SessionLengthTest {
    private val catalog = parseCatalog(File(System.getProperty("repo.root"), "content/starter/catalog.json").readText())
    private val none = ProgressSnapshot(emptyMap(), emptyMap(), emptyMap(), emptyList())
    private val profile = SeedProfiles.home

    private fun plan(s: TrainSettings, effective: Boolean = true): WorkoutPlan {
        val eff = if (effective) withDefaultLength(catalog, StarterRoutine.routine, none, profile, s) else s
        return (buildTrainPlan(catalog, StarterRoutine.routine, none, profile, eff) as PlanResult.Ready).plan
    }

    @Test fun anUpperBodySessionUsesTheAvailableDefaultPoolBeforeStoppingShortOfThirtyMinutes() {
        val s = TrainSettings(goalId = "body-upper", rounds = 3)
        val bare = plan(s, effective = false)
        assertTrue("without the default it is short: ${bare.plannedDurationSeconds / 60} min", bare.plannedDurationSeconds < MIN_SESSION_SECONDS)
        val p = plan(s)
        val focused = Goals.routineFor(catalog, StarterRoutine.routine, "body-upper", none, profile)
        assertEquals(maxExercisesPerRound(catalog, focused, setOf(StrengthFocus.UPPER_BODY), profile, none), exercisesPerRoundOf(p))
        assertTrue("a short pool does not pull in specialist extras", p.blocks.filter { it.type == BlockType.WORK }.all { block ->
            catalog.routeById(block.progressionRouteId)?.defaultPool == true
        })
        assertTrue(exercisesPerRoundOf(p) > exercisesPerRoundOf(bare))
        assertTrue("only upper-body exercises", p.blocks.filter { it.type == BlockType.WORK }.mapNotNull { catalog.variation(it.variationId!!) }.all { Area.UPPER_BODY in it.areas })
    }

    @Test fun lowerAndCoreSessionsAreFilledWithTheirOwnArea() {
        for ((goal, area) in listOf("body-lower" to Area.LOWER_BODY, "body-core" to Area.CORE)) {
            val s = TrainSettings(goalId = goal, rounds = 4)
            val bare = plan(s, effective = false)
            val p = plan(s)
            assertTrue("$goal got more exercises", exercisesPerRoundOf(p) > exercisesPerRoundOf(bare))
            assertTrue("$goal is longer", p.plannedDurationSeconds > bare.plannedDurationSeconds)
            assertTrue("$goal: only $area exercises", p.blocks.filter { it.type == BlockType.WORK }.mapNotNull { catalog.variation(it.variationId!!) }.all { area in it.areas })
        }
    }

    @Test fun theStepperCountIsRespectedAndNothingRepeats() {
        for (n in 1..7) {
            val p = plan(TrainSettings(goalId = "body-upper", rounds = 2, exercisesPerRound = n))
            assertEquals("count $n", minOf(n, maxExercisesPerRound(catalog, StarterRoutine.routine, setOf(StrengthFocus.UPPER_BODY), profile, none)), exercisesPerRoundOf(p))
            val first = p.blocks.filter { it.type == BlockType.WORK && it.roundIndex == 1 }.mapNotNull { it.variationId }
            assertEquals("no repeated exercise at $n", first.size, first.distinct().size)
            val families = first.distinct().map { catalog.variation(it)!!.familyId }
            assertEquals("no repeated progression at $n", families.size, families.distinct().size)
        }
    }

    @Test fun aFewerCountDropsTheOptionalExercisesFirst() {
        val s = TrainSettings(goalId = "general", rounds = 2, exercisesPerRound = 5)
        val p = plan(s)
        assertEquals(5, exercisesPerRoundOf(p))
        assertTrue("the optional lunge and core2 go first", p.blocks.none { it.slotId == "slot-lunge" || it.slotId == "slot-core2" })
    }

    @Test fun aSavedOrReadyMadeRoutineKeepsItsOwnList() {
        val s = TrainSettings(goalId = "general", rounds = 2, routineId = "preset-rr")
        assertNull(defaultExercisesPerRound(catalog, StarterRoutine.routine, none, profile, s))
        assertEquals(s, withDefaultLength(catalog, StarterRoutine.routine, none, profile, s))
    }

    @Test fun anExplicitNumberOverridesTheDefault() {
        val s = TrainSettings(goalId = "body-upper", rounds = 3, exercisesPerRound = 2)
        assertEquals(s, withDefaultLength(catalog, StarterRoutine.routine, none, profile, s))
        assertEquals(2, exercisesPerRoundOf(plan(s)))
    }

    @Test fun theDefaultKeepsTheSessionBetweenThirtyAndFortyFiveMinutesWhenACountCan() {
        for (rounds in 2..5) {
            val s = TrainSettings(goalId = "general", rounds = rounds)
            val p = plan(s)
            val n = exercisesPerRoundOf(p)
            val mandatory = StarterRoutine.routine.slots.count { !it.optional }
            assertTrue("rounds=$rounds: ${p.plannedDurationSeconds / 60} min with $n exercises", p.plannedDurationSeconds in MIN_SESSION_SECONDS..MAX_SESSION_SECONDS || n == mandatory)
        }
    }

    @Test fun theDefaultNeverGoesBelowTheMandatoryExercisesEvenWhenTheyAreLongerThanFortyFiveMinutes() {
        val s = TrainSettings(goalId = "general", rounds = MAX_EXPLICIT_ROUNDS)
        val mandatory = StarterRoutine.routine.slots.count { !it.optional }
        val n = defaultExercisesPerRound(catalog, StarterRoutine.routine, none, profile, s)!!
        assertEquals(mandatory, n)
        assertTrue(plan(s).plannedDurationSeconds > MAX_SESSION_SECONDS)
    }

    @Test fun theLargestCountInsideTheWindowWinsAndTheStepperCanGoHigher() {
        val s = TrainSettings(goalId = "general", rounds = 3)
        val n = defaultExercisesPerRound(catalog, StarterRoutine.routine, none, profile, s)!!
        fun minutes(count: Int) = (buildTrainPlan(catalog, StarterRoutine.routine, none, profile, s.copy(exercisesPerRound = count)) as PlanResult.Ready).plan.plannedDurationSeconds
        assertTrue(minutes(n) in MIN_SESSION_SECONDS..MAX_SESSION_SECONDS)
        if (n < maxExercisesPerRound(catalog, StarterRoutine.routine, setOf(StrengthFocus.FULL_BODY), profile, none)) assertTrue("one more is over 45 min", minutes(n + 1) > MAX_SESSION_SECONDS)
        assertEquals(n + 1, exercisesPerRoundOf(plan(s.copy(exercisesPerRound = n + 1))).coerceAtMost(n + 1))   // an explicit count is respected
    }
}
