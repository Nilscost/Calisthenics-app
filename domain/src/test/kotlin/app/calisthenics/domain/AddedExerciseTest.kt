package app.calisthenics.domain

import app.calisthenics.domain.content.parseCatalog
import app.calisthenics.domain.equipment.SeedProfiles
import app.calisthenics.domain.model.*
import app.calisthenics.domain.planner.*
import app.calisthenics.domain.progression.*
import org.junit.Assert.*
import org.junit.Test
import java.io.File

/** Bug E1: an exercise added in the Preview is exactly the exercise picked (no route replacement, no alternative rotation); the replacement note only claims completed tiers when true. */
class AddedExerciseTest {
    private val c = parseCatalog(File(System.getProperty("repo.root"), "content/starter/catalog.json").readText())
    private val engine = ProgressionEngine(c)
    private val everything = EquipmentProfile("everything", "Everything",
        items = c.variations.flatMap { it.equipmentAlternatives }.flatMap { it.needs }.groupBy { it.equipmentId }.map { (id, needs) ->
            EquipmentItem(id, quantity = 4, massGrams = 100_000, suitability = needs.flatMap { it.suitability }.toSet())
        },
        capabilities = c.variations.flatMap { it.equipmentAlternatives }.flatMap { it.capabilities }.toSet())

    private fun plan(base: Routine, progress: ProgressSnapshot, added: List<AddedSlot>, profile: EquipmentProfile = everything): WorkoutPlan =
        (buildTrainPlan(c, base, progress, profile, TrainSettings(), edits = PlanEdits(added = added)) as PlanResult.Ready).plan
    private fun workIds(p: WorkoutPlan) = p.blocks.filter { it.type == BlockType.WORK }.mapNotNull { it.variationId }

    private val base = Routine("saved", 1, "Saved", listOf(RoutineSlot("a", Pattern.SQUAT, Area.LOWER_BODY, "squat-air")))

    @Test fun addedExercisesAreNotReplacedByTheirRouteOrAnAlternative() {
        val histories = listOf(
            engine.replay(emptyList(), emptyList()),
            engine.replay(emptyList(), listOf(UserAction.SelfAssessment(0, "squat-air", 5, 4))),
            engine.replay(emptyList(), listOf(UserAction.SelfAssessment(0, "pullup-negative", 3))),
        )
        for (h in histories) for (count in 0..3) for (id in listOf("lunge-reverse", "dead-hang", "squat-air", "pullup-negative")) {
            val p = plan(base, h.copy(routeWorkoutCounts = mapOf("squat" to count, "pullup" to count), familyWorkoutCounts = mapOf("squat" to count, "pullup" to count)),
                listOf(AddedSlot("a", slotFor(c.variation(id)!!))))
            assertTrue("$id was replaced: ${workIds(p)}", id in workIds(p))
        }
    }

    @Test fun addedExerciseStillYieldsToMissingEquipment() {
        val v = c.variation("dead-hang")!!
        val bare = SeedProfiles.travel
        if (app.calisthenics.domain.equipment.isAvailable(v, bare)) return
        val p = plan(base, engine.replay(emptyList(), emptyList()), listOf(AddedSlot("a", slotFor(v))), bare)
        assertTrue("dead-hang" !in workIds(p))
    }

    @Test fun replacementNoteOnlyClaimsCompletionWhenTrue() {
        // feet-elevated push-up assessed at tier 5: the route position moves on from a push-up whose tiers were never completed
        val routine = Routine("saved", 1, "Saved", listOf(RoutineSlot("a", Pattern.PUSH_HORIZONTAL, Area.UPPER_BODY, "pushup-standard")))
        val h = engine.replay(emptyList(), listOf(UserAction.SelfAssessment(0, "pushup-feet-elevated", 5, 4)))
        val p = (buildTrainPlan(c, routine, h, SeedProfiles.home, TrainSettings()) as PlanResult.Ready).plan
        val std = h.variations["pushup-standard"]
        val claims = p.changesExplained.filter { "completed all tiers" in it }
        if (std == null || 5 !in std.achievedTiers) assertTrue(claims.toString(), claims.isEmpty())
    }
}
