package app.calisthenics.domain

import app.calisthenics.domain.content.parseCatalog
import app.calisthenics.domain.equipment.SeedProfiles
import app.calisthenics.domain.model.*
import app.calisthenics.domain.planner.*
import app.calisthenics.domain.progression.ProgressSnapshot
import app.calisthenics.domain.routine.Presets
import app.calisthenics.domain.routine.StarterRoutine
import app.calisthenics.domain.routine.resolveSaved
import app.calisthenics.domain.routine.settingsFor
import org.junit.Assert.*
import org.junit.Test
import java.io.File

/** R8.1-T05 (F5): Straight sets / Pairs / Circuit decide the order of the work, for generated and routine workouts. */
class FormatOrderRegressionTest {
    private val c = parseCatalog(File(System.getProperty("repo.root"), "content/starter/catalog.json").readText())
    private val none = ProgressSnapshot(emptyMap(), emptyMap(), emptyMap(), emptyList())
    private fun order(plan: WorkoutPlan) = plan.blocks.filter { it.type == BlockType.WORK }.map { "${it.slotId}#${it.roundIndex}" }.distinct()

    private fun generated(f: WorkoutFormat) =
        (buildTrainPlan(c, StarterRoutine.routine, none, SeedProfiles.home, TrainSettings(rounds = 3, format = f, exercisesPerRound = 4)) as PlanResult.Ready).plan

    @Test fun generatedWorkoutsFollowTheChosenFormat() {
        val circuit = order(generated(WorkoutFormat.CIRCUIT)); val straight = order(generated(WorkoutFormat.STRAIGHT)); val pairs = order(generated(WorkoutFormat.PAIRS))
        assertEquals(listOf("push#1", "squat#1", "pull#1", "core#1", "push#2"), circuit.take(5))       // round by round
        assertEquals(listOf("push#1", "push#2", "push#3", "squat#1"), straight.take(4))                // all sets of one exercise first
        assertEquals(listOf("push#1", "squat#1", "push#2", "squat#2", "push#3", "squat#3", "pull#1"), pairs.take(7))   // two exercises alternate
        assertEquals(circuit.toSet(), straight.toSet()); assertEquals(circuit.toSet(), pairs.toSet())
        assertEquals(WorkoutFormat.STRAIGHT, generated(WorkoutFormat.STRAIGHT).format)
    }

    @Test fun aReadyMadeRoutineKeepsItsPairsAndTheFormatCanBeChangedForTheDay() {
        val rr = Presets.recommended(c, SeedProfiles.home)
        assertEquals(WorkoutFormat.PAIRS, rr.format)
        val today = settingsFor(rr, TrainSettings())
        assertEquals(WorkoutFormat.PAIRS, today.format)
        val resolved = resolveSaved(rr, today)
        val pairs = (buildTrainPlan(c, resolved.routine, none, SeedProfiles.home, resolved.settings, edits = resolved.edits) as PlanResult.Ready).plan
        val firstTwo = pairs.blocks.filter { it.type == BlockType.WORK }.map { it.slotId }.take(6)
        assertEquals(setOf("rr-pull", "rr-squat"), firstTwo.toSet())              // the first pair alternates for its sets
        val straight = (buildTrainPlan(c, resolved.routine, none, SeedProfiles.home, resolved.settings.copy(format = WorkoutFormat.STRAIGHT), edits = resolved.edits) as PlanResult.Ready).plan
        assertEquals(WorkoutFormat.STRAIGHT, straight.format)
        assertEquals(List(3) { "rr-pull" }, straight.blocks.filter { it.type == BlockType.WORK }.map { it.slotId }.take(3))
    }
}
