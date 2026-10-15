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

/** R7-T08 (D1): exercises per round. A body part is filled until the session lasts 30 minutes; the stepper overrides it. */
class SessionLengthTest {
    private val catalog = parseCatalog(File(System.getProperty("repo.root"), "content/starter/catalog.json").readText())
    private val none = ProgressSnapshot(emptyMap(), emptyMap(), emptyMap(), emptyList())
    private val profile = SeedProfiles.home

    private fun plan(s: TrainSettings, effective: Boolean = true): WorkoutPlan {
        val eff = if (effective) withDefaultLength(catalog, StarterRoutine.routine, none, profile, s) else s
        return (buildTrainPlan(catalog, StarterRoutine.routine, none, profile, eff) as PlanResult.Ready).plan
    }

    @Test fun anUpperBodySessionOfThreeRoundsIsFilledToThirtyMinutes() {
        val s = TrainSettings(goalId = "body-upper", rounds = 3)
        val bare = plan(s, effective = false)
        assertTrue("without the default it is short: ${bare.plannedDurationSeconds / 60} min", bare.plannedDurationSeconds < MIN_SESSION_SECONDS)
        val p = plan(s)
        assertTrue("upper, 3 rounds: ${p.plannedDurationSeconds / 60} min", p.plannedDurationSeconds >= MIN_SESSION_SECONDS)
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

    @Test fun theDefaultNeverGoesBelowTheRoutinesOwnCount() {
        val s = TrainSettings(goalId = "general", rounds = 6)
        val base = exercisesPerRoundOf(plan(s, effective = false))
        assertTrue((defaultExercisesPerRound(catalog, StarterRoutine.routine, none, profile, s) ?: 0) >= base)
    }
}
