package app.calisthenics.domain

import app.calisthenics.domain.content.parseCatalog
import app.calisthenics.domain.equipment.SeedProfiles
import app.calisthenics.domain.goals.Goals
import app.calisthenics.domain.model.*
import app.calisthenics.domain.planner.*
import app.calisthenics.domain.progression.ProgressSnapshot
import app.calisthenics.domain.routine.StarterRoutine
import kotlin.test.Test

/** The number of exercises per round Train shows is the number of exercises the workout (and so the Preview) has. */
class TrainCountTest {
    private val catalog = parseCatalog(fixtureText("content/starter/catalog.json"))
    private val none = ProgressSnapshot(emptyMap(), emptyMap(), emptyMap(), emptyList())
    private fun plan(s: TrainSettings, profile: EquipmentProfile = SeedProfiles.home) =
        (buildTrainPlan(catalog, StarterRoutine.routine, none, profile, s) as PlanResult.Ready).plan

    @Test fun theDefaultCountIsWhatEveryObjectiveAndOptionBuilds() {
        val bad = mutableListOf<String>()
        for (goal in Goals.all.map { it.id }) for (profile in listOf(SeedProfiles.home, SeedProfiles.travel, SeedProfiles.starterHome)) for (fmt in WorkoutFormat.values())
            for (cond in listOf(false, true)) for (rounds in listOf(2, 4)) {
                val s = withDefaultLength(catalog, StarterRoutine.routine, none, profile, TrainSettings(goalId = goal, profileId = profile.id, format = fmt, conditioningOn = cond, rounds = rounds))
                val r = buildTrainPlan(catalog, StarterRoutine.routine, none, profile, s) as? PlanResult.Ready ?: continue
                if (exercisesPerRoundOf(r.plan) != s.exercisesPerRound) bad += "$goal ${profile.id} $fmt cond=$cond r=$rounds: Train ${s.exercisesPerRound}, workout ${exercisesPerRoundOf(r.plan)}"
            }
        assertTrue(bad.take(5).toString(), bad.isEmpty())
    }

    @Test fun aRequestedSlotTheRoundLeavesOutIsReplacedByTheNextFiller() {
        // back lever on the home profile: the second skill step is left out of the plan ("already works the same movement"), so 7 requested slots built 6 exercises
        val top = reachableExercisesPerRound(catalog, StarterRoutine.routine, none, SeedProfiles.home, TrainSettings(goalId = "back-lever"))
        for (n in 1..top) assertEquals("asked $n", n, exercisesPerRoundOf(plan(TrainSettings(goalId = "back-lever", rounds = 2, exercisesPerRound = n))))
        assertEquals(top, exercisesPerRoundOf(plan(TrainSettings(goalId = "back-lever", rounds = 2, exercisesPerRound = MAX_EXERCISES_PER_ROUND))))
    }

    @Test fun theStepperTopIsWhatThePlanCanHold() {
        val s = TrainSettings(goalId = "back-lever", rounds = 2)
        val r = routineForPlan(catalog, StarterRoutine.routine, none, SeedProfiles.home, s)
        val pool = maxExercisesPerRound(catalog, r, fillFocusOf(r, s, catalog), SeedProfiles.home, none)
        assertTrue(reachableExercisesPerRound(catalog, StarterRoutine.routine, none, SeedProfiles.home, s) <= pool)
    }

    @Test fun exercisesTakenOutByHandAreNotPutBack() {
        val s = TrainSettings(goalId = "back-lever", rounds = 2, exercisesPerRound = 5)
        val base = plan(s)
        val slot = base.blocks.first { it.type == BlockType.WORK && it.slotId == "ext-row" }.slotId!!
        val p = (buildTrainPlan(catalog, StarterRoutine.routine, none, SeedProfiles.home, s, edits = PlanEdits(removed = setOf(slot))) as PlanResult.Ready).plan
        assertTrue(p.blocks.none { it.slotId == slot })
        assertTrue(exercisesPerRoundOf(p) < exercisesPerRoundOf(base))
    }
}
