package app.calisthenics.domain

import app.calisthenics.domain.content.parseCatalog
import app.calisthenics.domain.equipment.SeedProfiles
import app.calisthenics.domain.feedback.SessionEvidence
import app.calisthenics.domain.model.BlockType
import app.calisthenics.domain.planner.*
import app.calisthenics.domain.progression.*
import app.calisthenics.domain.routine.StarterRoutine
import kotlin.test.Test

/**
 * 9.3 (owner screenshots 2026-11-12): the archer push-up showed "Mastered" and was still in the workout. Every history that earns the fifth star
 * (three good sessions at level 5, the level editor's "all five done", a hand-set level followed by sessions) moves the route on to the half
 * one-arm push-up, and the planner then prescribes only that one. The archer stays planned only when the stars are not earned yet, or when the
 * person picked it explicitly (swap). Evidence: docs/evidence/9.3-l3-mastered-still-planned.md.
 */
class MasteredStillPlannedTest {
    private val c = parseCatalog(fixtureText("content/starter/catalog.json"))
    private val engine = ProgressionEngine(c)
    private val archer = "pushup-archer"
    private val half = "pushup-half-one-arm"
    private val family = c.variation(archer)!!.familyId

    private fun ev(day: Int, tier: Int) =
        SessionEvidence("s$day", archer, family, day, tier, 3, false, false, false, null, false, 3, 0, 2, progressionRouteId = "pushup")

    private fun planned(p: ProgressSnapshot, swaps: Map<String, String> = emptyMap()): Set<String> {
        val used = mutableSetOf<String>()
        for (count in 0..7) for (rounds in listOf(3, 4, 6)) {
            val plan = (buildTrainPlan(c, StarterRoutine.routine, p.copy(routeWorkoutCounts = mapOf("pushup" to count)), SeedProfiles.home, TrainSettings(rounds = rounds), swaps) as PlanResult.Ready).plan
            used += plan.blocks.filter { it.type == BlockType.WORK }.mapNotNull { it.variationId }
        }
        return used.filter { it.startsWith("pushup") }.toSet()
    }

    private fun assertMovedOn(label: String, p: ProgressSnapshot) {
        assertTrue("$label: archer mastered", 5 in p.variations.getValue(archer).achievedTiers)
        assertEquals("$label: route moved on", half, p.activeInFamily[family])
        assertEquals("$label: only the next step is planned", setOf(half), planned(p))
    }

    @Test fun masteryByThreeGoodSessionsAtLevelFiveMovesTheRouteOn() =
        assertMovedOn("streak", engine.replay(listOf(ev(0, 5), ev(5, 5), ev(10, 5)), listOf(UserAction.SelfAssessment(0, archer, 5))))

    @Test fun theLevelEditorsAllFiveDoneMovesTheRouteOn() =
        assertMovedOn("editor", engine.replay(emptyList(), listOf(selfAssessmentFor(archer, storedForLevelsDone(5), 100))))

    @Test fun levelsOneToFourByHandThenSessionsAtFiveMovesTheRouteOn() =
        assertMovedOn("editor 1..4", engine.replay(listOf(ev(100, 5), ev(103, 5), ev(106, 5)), listOf(selfAssessmentFor(archer, storedForLevelsDone(4), 100))))

    @Test fun aHandSetLevelThreeClimbedByStreaksMovesTheRouteOn() =
        assertMovedOn("climbed", engine.replay((0..2).map { ev(it * 4, 3) } + (3..5).map { ev(it * 4, 4) } + (6..8).map { ev(it * 4, 5) }, listOf(UserAction.SelfAssessment(0, archer, 3))))

    @Test fun aPlainHandSetLevelFiveIsNotMasteryYet() {
        val p = engine.replay(emptyList(), listOf(UserAction.SelfAssessment(0, archer, 5)))
        assertFalse(5 in p.variations.getValue(archer).achievedTiers)
        assertEquals(archer, p.activeInFamily[family])
        assertTrue(archer in planned(p))
    }

    @Test fun theSessionThatMastersItIsPlannedBeforeItIsMastered() {
        val before = engine.replay(listOf(ev(0, 5), ev(5, 5)), listOf(UserAction.SelfAssessment(0, archer, 5)))
        assertTrue("the workout that earns the star was planned with the archer", archer in planned(before))
        val after = engine.replay(listOf(ev(0, 5), ev(5, 5), ev(10, 5)), listOf(UserAction.SelfAssessment(0, archer, 5)))
        assertEquals(setOf(half), planned(after))
    }

    @Test fun anExplicitlySwappedInMasteredExerciseStaysBecauseThePersonPickedIt() {
        val p = engine.replay(listOf(ev(0, 5), ev(5, 5), ev(10, 5)), listOf(UserAction.SelfAssessment(0, archer, 5)))
        assertTrue(archer in planned(p, swaps = mapOf("push" to archer)))
    }
}
