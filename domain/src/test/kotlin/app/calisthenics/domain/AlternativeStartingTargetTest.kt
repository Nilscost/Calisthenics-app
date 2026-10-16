package app.calisthenics.domain

import app.calisthenics.domain.content.parseCatalog
import app.calisthenics.domain.equipment.SeedProfiles
import app.calisthenics.domain.feedback.*
import app.calisthenics.domain.model.*
import app.calisthenics.domain.planner.*
import app.calisthenics.domain.progression.*
import app.calisthenics.domain.routine.SessionDraft
import org.junit.Assert.*
import org.junit.Test
import java.io.File

class AlternativeStartingTargetTest {
    private val c = parseCatalog(File(System.getProperty("repo.root"), "content/starter/catalog.json").readText())
    private val main = c.variation("pushup-feet-elevated")!!
    private val alt = c.variation("pushup-diamond")!!
    private val actions = listOf(UserAction.SelfAssessment(0, "pushup-standard", 6), UserAction.SelfAssessment(0, main.id, 5))
    private val progress = ProgressionEngine(c).replay(emptyList(), actions).copy(routeWorkoutCounts = mapOf("pushup" to 1))
    private val routine = Routine("test", 1, "Test", listOf(RoutineSlot("push", Pattern.PUSH_HORIZONTAL, Area.UPPER_BODY, main.id, progressionRouteId = "pushup")))
    private val draft = SessionDraft(600, false, emptySet(), false, false, setOf(StrengthFocus.FULL_BODY), SeedProfiles.home.id, rounds = 2)
    private fun plan(d: SessionDraft = draft, p: ProgressSnapshot = progress) =
        (generate(PlanInput("test", 0, 1, c, routine, d, SeedProfiles.home, progress = p)) as PlanResult.Ready).plan

    @Test fun twelveMainRepsSuggestEightForTheUntriedSameRungAlternative() {
        val blocks = plan().blocks.filter { it.type == BlockType.WORK }
        assertTrue(blocks.all { it.variationId == alt.id && it.target!!.value == 8 && it.prescriptionTier == 3 && it.inferredStartTier == 3 })
        assertFalse(progress.variations.containsKey(alt.id)) // preview is read-only
    }

    @Test fun completedWorkoutKeepsItsStartingLevelWithoutTransferringAchievements() {
        val plan = plan()
        val outcomes = resolveFeedback(plan, plan.blocks.associate { it.id to Execution.COMPLETED })
        val evidence = deriveEvidence("first-alt", 1, plan, outcomes, { c.variation(it)!!.familyId }, { _, _ -> 2 })
        assertEquals(3, evidence.single().inferredStartTier)
        val replay = ProgressionEngine(c).replay(evidence, actions)
        val p = replay.variations.getValue(alt.id)
        assertEquals(3, p.tier)
        assertEquals(3, p.baselineTier)
        assertTrue(p.achievedTiers.isEmpty())
        val nextAltWorkout = plan(p = replay.copy(routeWorkoutCounts = mapOf("pushup" to 3)))
        assertTrue(nextAltWorkout.blocks.filter { it.type == BlockType.WORK }.all { it.target!!.value == 8 && it.inferredStartTier == null })
    }

    @Test fun ownHistoryManualTargetsAndRangesTakePrecedence() {
        val own = ProgressionEngine(c).replay(emptyList(), actions + UserAction.SelfAssessment(0, alt.id, 2))
            .copy(routeWorkoutCounts = mapOf("pushup" to 1))
        assertTrue(plan(p = own).blocks.filter { it.type == BlockType.WORK }.all { it.target!!.value == 6 && it.inferredStartTier == null })
        assertTrue(plan(draft.copy(tierOverrides = mapOf("push" to 1))).blocks.filter { it.type == BlockType.WORK }.all { it.target!!.value == 4 && it.inferredStartTier == null })
        assertTrue(plan(draft.copy(rule = ProgressionRule(RuleKind.REP_RANGE))).blocks.filter { it.type == BlockType.WORK }.all { it.inferredStartTier == null })
    }

    @Test fun aSkippedFirstWorkoutKeepsTheEstimateWithoutCompletingAnyTier() {
        val plan = plan()
        val outcomes = resolveFeedback(plan, plan.blocks.associate { it.id to Execution.SKIPPED })
        val evidence = deriveEvidence("skipped-alt", 1, plan, outcomes, { c.variation(it)!!.familyId }, { _, _ -> 2 })
        val p = ProgressionEngine(c).replay(evidence, actions).variations.getValue(alt.id)
        assertEquals(3, p.tier)
        assertEquals(3, p.baselineTier)
        assertTrue(p.achievedTiers.isEmpty())
        assertTrue(p.streakDays.isEmpty())
    }

    @Test fun transferRequiresMatchingRepMovementAndExplicitSameRungMembership() {
        assertNull(inferredAlternativeTier(c, main, alt.copy(kind = Kind.HOLD), progress, "pushup"))
        assertNull(inferredAlternativeTier(c, main, alt.copy(familyId = "other"), progress, "pushup"))
        assertNull(inferredAlternativeTier(c, main, alt.copy(unilateral = true), progress, "pushup"))
        assertNull(inferredAlternativeTier(c, main, alt, progress, "row"))
        val held = progress.copy(holds = mapOf(main.familyId to FamilyHold(main.familyId, 0, null, null)))
        assertNull(inferredAlternativeTier(c, main, alt, held, "pushup"))
    }
}
