package app.calisthenics.domain

import app.calisthenics.domain.content.parseCatalog
import app.calisthenics.domain.equipment.SeedProfiles
import app.calisthenics.domain.feedback.SessionEvidence
import app.calisthenics.domain.model.*
import app.calisthenics.domain.planner.*
import app.calisthenics.domain.progression.*
import app.calisthenics.domain.routine.StarterRoutine
import org.junit.Assert.*
import org.junit.Test
import java.io.File

/** R8.1-T01 (F1): the planner never prescribes an exercise below what the person has shown on the same route. */
class LevelNeverBelowTest {
    private val c = parseCatalog(File(System.getProperty("repo.root"), "content/starter/catalog.json").readText())
    private val engine = ProgressionEngine(c)

    private fun rung(routeId: String, id: String): Int {
        val r = c.routeById(routeId)!!
        return r.mainPath.indexOf(c.routeAnchorFor(id, r) ?: id)
    }

    /** Plans the full-body session for workout counts 0..9 (alternates rotate with the count) and returns the exercise of [slot] in each. */
    private fun slotExercises(progress: ProgressSnapshot, slot: String, extra: TrainSettings = TrainSettings()): Set<String> =
        (0..9).flatMap { n ->
            val p = progress.copy(routeWorkoutCounts = progress.routeWorkoutCounts.mapValues { n }.ifEmpty { mapOf("pushup" to n, "squat" to n) },
                familyWorkoutCounts = mapOf("pushup" to n, "squat" to n))
            val plan = (buildTrainPlan(c, StarterRoutine.routine, p, SeedProfiles.home, extra) as PlanResult.Ready).plan
            plan.blocks.filter { it.type == BlockType.WORK && it.slotId == slot }.mapNotNull { it.variationId }
        }.toSet()

    @Test fun fourStarsOnFeetElevatedNeverGivesKneeOrStandardPushUp() {
        val progress = engine.replay(emptyList(), listOf(UserAction.SelfAssessment(0, "pushup-feet-elevated", 5, 4)))
        val seen = slotExercises(progress, "push")
        assertTrue("saw $seen", seen.isNotEmpty())
        seen.forEach { assertTrue("$it is below feet-elevated", rung("pushup", it) >= rung("pushup", "pushup-feet-elevated")) }
    }

    @Test fun workDoneOnTheAlternateAloneAlsoCountsForTheRoutePosition() {
        // only diamond (alternate at feet-elevated) was trained, at 4 stars
        val progress = engine.replay(emptyList(), listOf(UserAction.SelfAssessment(0, "pushup-diamond", 5, 4)))
        val seen = slotExercises(progress, "push")
        seen.forEach { assertTrue("$it is below the feet-elevated rung", rung("pushup", it) >= rung("pushup", "pushup-feet-elevated")) }
    }

    @Test fun splitSquatWorkNeverSendsTheSquatSlotBackToAssistedOrAir() {
        val progress = engine.replay(emptyList(), listOf(UserAction.SelfAssessment(0, "split-squat", 3)))
        val seen = slotExercises(progress, "squat")
        assertTrue("saw $seen", seen.isNotEmpty())
        seen.forEach { assertTrue("$it is below split squat", rung("squat", it) >= rung("squat", "split-squat")) }
    }

    @Test fun splitSquatSessionsAfterAirSquatsKeepTheSquatSlotAtSplitSquat() {
        val ev = listOf(
            evidence("s1", 1, "squat-air", 5), evidence("s2", 3, "squat-air", 5),
            evidence("s3", 5, "split-squat", 2), evidence("s4", 7, "split-squat", 2),
        )
        val progress = engine.replay(ev, emptyList())
        val seen = slotExercises(progress, "squat")
        seen.forEach { assertTrue("$it is below split squat", rung("squat", it) >= rung("squat", "split-squat")) }
    }

    @Test fun importedHistoryOnlyOnRelatedExercisesKeepsThePosition() {
        // a backup brings completed workouts of feet-elevated and split squat but no self-assessment
        val ev = listOf(evidence("b1", 1, "pushup-feet-elevated", 4), evidence("b2", 3, "pushup-feet-elevated", 4),
            evidence("b3", 5, "split-squat", 3), evidence("b4", 8, "split-squat", 3))
        val progress = engine.replay(ev, emptyList())
        slotExercises(progress, "push").forEach { assertTrue("$it < feet-elevated", rung("pushup", it) >= rung("pushup", "pushup-feet-elevated")) }
        slotExercises(progress, "squat").forEach { assertTrue("$it < split squat", rung("squat", it) >= rung("squat", "split-squat")) }
    }

    private fun evidence(session: String, day: Int, vid: String, tier: Int) = SessionEvidence(session, vid, c.variation(vid)!!.familyId, day, tier, 3, false, false, false, null, false, 3, 0, 2)
}
