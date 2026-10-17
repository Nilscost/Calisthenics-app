package app.calisthenics.domain

import app.calisthenics.domain.content.parseCatalog
import app.calisthenics.domain.equipment.SeedProfiles
import app.calisthenics.domain.model.*
import app.calisthenics.domain.planner.*
import app.calisthenics.domain.goals.Goals
import app.calisthenics.domain.progression.*
import app.calisthenics.domain.routine.StarterRoutine
import org.junit.Assert.*
import org.junit.Test
import java.io.File

/** R8.1-T02 (F2): one exercise per logical route per session, alternates and support included (only a hand-added one may repeat). */
class OneExercisePerRouteTest {
    private val c = parseCatalog(File(System.getProperty("repo.root"), "content/starter/catalog.json").readText())
    private val engine = ProgressionEngine(c)

    /** Every item the catalog asks for, with every suitability flag seen. */
    private val everything = EquipmentProfile("everything", "Everything",
        items = c.variations.flatMap { it.equipmentAlternatives }.flatMap { it.needs }.groupBy { it.equipmentId }.map { (id, needs) ->
            EquipmentItem(id, quantity = 4, massGrams = 100_000, suitability = needs.flatMap { it.suitability }.toSet())
        },
        capabilities = c.variations.flatMap { it.equipmentAlternatives }.flatMap { it.capabilities }.toSet())

    private fun conflicts(plan: WorkoutPlan): List<String> {
        val ids = plan.blocks.filter { it.type == BlockType.WORK }.mapNotNull { it.variationId }.distinct()
        val out = mutableListOf<String>()
        for (i in ids.indices) for (j in i + 1 until ids.size) {
            if (c.sharesLogicalRoute(ids[i], ids[j])) out += "${ids[i]} + ${ids[j]} in ${c.logicalRoutesOf(ids[i]).intersect(c.logicalRoutesOf(ids[j]))}"
        }
        return out
    }

    private val histories: Map<String, ProgressSnapshot> = mapOf(
        "none" to engine.replay(emptyList(), emptyList()),
        "feet-elevated" to engine.replay(emptyList(), listOf(UserAction.SelfAssessment(0, "pushup-feet-elevated", 5, 4))),
        "standard" to engine.replay(emptyList(), listOf(UserAction.SelfAssessment(0, "pushup-standard", 3))),
        "split" to engine.replay(emptyList(), listOf(UserAction.SelfAssessment(0, "split-squat", 3), UserAction.SelfAssessment(0, "pushup-standard", 6))),
    )

    @Test fun noRouteIsTrainedTwiceInOneGeneratedWorkout() {
        val bad = mutableListOf<String>()
        for ((hn, h) in histories) for (profile in listOf(SeedProfiles.home, SeedProfiles.travel, everything)) for (goal in Goals.all) for (n in listOf(null) + (1..10)) for (count in 0..3) {
            val p = h.copy(routeWorkoutCounts = mapOf("pushup" to count, "squat" to count, "row" to count), familyWorkoutCounts = mapOf("pushup" to count, "squat" to count, "row" to count))
            val r = buildTrainPlan(c, StarterRoutine.routine, p, profile, TrainSettings(goalId = goal.id, exercisesPerRound = n))
            if (r is PlanResult.Ready) conflicts(r.plan).forEach { bad += "[$hn/${profile.id}/${goal.id}/n=$n/c=$count] $it" }
        }
        assertTrue("${bad.size} conflicts, e.g.:\n" + bad.distinct().take(25).joinToString("\n"), bad.isEmpty())
    }

    @Test fun noRouteTwiceInTheOtherStartingRoutines() {
        val bad = mutableListOf<String>()
        val bases = listOf(app.calisthenics.domain.routine.SeedRoutine.fullBody) + app.calisthenics.domain.routine.Presets.all(c, everything).map { it.routine }
        for ((hn, h) in histories) for (profile in listOf(SeedProfiles.home, everything)) for (base in bases) for (n in listOf(null) + (1..10)) for (count in 0..3) {
            val p = h.copy(routeWorkoutCounts = mapOf("pushup" to count, "squat" to count, "row" to count), familyWorkoutCounts = mapOf("pushup" to count, "squat" to count, "row" to count))
            val r = buildTrainPlan(c, base, p, profile, TrainSettings(exercisesPerRound = n))
            if (r is PlanResult.Ready) conflicts(r.plan).forEach { bad += "[$hn/${profile.id}/${base.id}/n=$n/c=$count] $it" }
        }
        assertTrue("${bad.size} conflicts, e.g.:\n" + bad.distinct().take(25).joinToString("\n"), bad.isEmpty())
    }

    private fun workIds(plan: WorkoutPlan) = plan.blocks.filter { it.type == BlockType.WORK }.mapNotNull { it.variationId }.distinct()

    /** The owner's case: a routine whose slots name knee push-up and standard push-up (a saved or edited routine) used to plan both. */
    @Test fun kneeAndStandardPushUpNeverShareAWorkout() {
        val routine = Routine("saved", 1, "Saved", listOf(
            RoutineSlot("a", Pattern.PUSH_HORIZONTAL, Area.UPPER_BODY, "pushup-knee"),
            RoutineSlot("b", Pattern.PUSH_HORIZONTAL, Area.UPPER_BODY, "pushup-standard"),
            RoutineSlot("c", Pattern.SQUAT, Area.LOWER_BODY, "squat-air")))
        val plan = (buildTrainPlan(c, routine, histories.getValue("none"), SeedProfiles.home, TrainSettings()) as PlanResult.Ready).plan
        val ids = workIds(plan)
        assertEquals(1, ids.count { it.startsWith("pushup-") })
        assertTrue(plan.changesExplained.any { "already trains its progression" in it })
    }

    @Test fun anAlternateAndItsMainStepNeverShareAWorkout() {
        val routine = Routine("saved", 1, "Saved", listOf(
            RoutineSlot("a", Pattern.PUSH_HORIZONTAL, Area.UPPER_BODY, "pushup-feet-elevated"),
            RoutineSlot("b", Pattern.PUSH_HORIZONTAL, Area.UPPER_BODY, "pushup-diamond")))
        val plan = (buildTrainPlan(c, routine, histories.getValue("none"), everything, TrainSettings()) as PlanResult.Ready).plan
        assertEquals(1, workIds(plan).size)
    }

    @Test fun anExerciseAddedByHandInThePreviewMayRepeatAProgression() {
        val base = Routine("saved", 1, "Saved", listOf(RoutineSlot("a", Pattern.PUSH_HORIZONTAL, Area.UPPER_BODY, "pushup-knee")))
        val added = slotFor(c.variation("pushup-standard")!!)
        val plan = (buildTrainPlan(c, base, histories.getValue("none"), SeedProfiles.home, TrainSettings(), edits = PlanEdits(added = listOf(AddedSlot("a", added)))) as PlanResult.Ready).plan
        assertEquals(setOf("pushup-knee", "pushup-standard"), workIds(plan).toSet())
    }
}
