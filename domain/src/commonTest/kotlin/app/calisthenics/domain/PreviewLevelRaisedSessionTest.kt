package app.calisthenics.domain

import app.calisthenics.domain.content.parseCatalog
import app.calisthenics.domain.feedback.*
import app.calisthenics.domain.model.*
import app.calisthenics.domain.planner.*
import app.calisthenics.domain.progression.*
import app.calisthenics.domain.session.*
import kotlinx.serialization.json.Json
import kotlin.test.Test

/**
 * Owner report (release 8.2): the archer push-up was at level 1, the Preview level + was tapped twice (level 3, "12 reps, 6 per side"), the reps were done
 * and the session was not counted: level 3 did not become current and the end screen showed no good session.
 */
class PreviewLevelRaisedSessionTest {
    private val archer = "pushup-archer"

    private fun run(startTier: Int, perSideReps: Int?, actions0: List<UserAction> = emptyList()): Triple<ProgressSnapshot, ProgressSnapshot, WorkoutPlan> {
        val catalog = parseCatalog(fixtureText("content/starter/catalog.json"))
        val engine = ProgressionEngine(catalog)
        val actions = (if (startTier > 0) listOf(UserAction.SelfAssessment(0, archer, startTier)) else emptyList()) + actions0
        val before = engine.replay(emptyList(), actions)
        val routine = Routine("usual", 1, "Push", listOf(RoutineSlot("push", Pattern.PUSH_HORIZONTAL, Area.UPPER_BODY, archer)))
        val settings = TrainSettings(rounds = 3, exercisesPerRound = 1, betweenSetMode = BetweenSetMode.FLOW)
        val profile = EquipmentProfile("home", "Home", emptyList(), emptySet())
        val json = Json { encodeDefaults = true }
        val source = (buildTrainPlan(catalog, routine, before, profile, settings, nowEpochMs = 1, nowDay = 100,
            edits = PlanEdits(tierOverrides = mapOf("push" to 3)).stampOverride("push", archer, 100L)) as PlanResult.Ready).plan
        val plan = json.decodeFromString(WorkoutPlan.serializer(), json.encodeToString(WorkoutPlan.serializer(), source))
        var state = reduce(newSession("logged", plan), SessionEvent.Start(0)).state
        var now = 0L
        while (!state.isTerminal) {
            val b = state.currentBlock!!
            if (b.type == BlockType.WORK) state = reduce(state, SessionEvent.LogBlock(b.id, BlockLog(reps = perSideReps))).state
            now += b.durationSeconds * 1000L
            state = reduce(state, SessionEvent.Tick(now)).state
        }
        val feedback = state.logged.mapValues { Feedback(actualReps = it.value.reps) }
        val ev = deriveEvidence("logged-100", 100, plan, resolveFeedback(plan, state.executions, feedback), { catalog.variation(it)!!.familyId },
            { id, tier -> catalog.policyForVariation(id)!!.tier(tier ?: 1).minQualifyingBlocks },
            loggedBlockIds = state.logged.keys, effectivePolicyOf = catalog::policyForVariation)
        return Triple(before, engine.replay(ev, actions), plan)
    }

    private fun counted(before: ProgressSnapshot, after: ProgressSnapshot) {
        val catalog = parseCatalog(fixtureText("content/starter/catalog.json"))
        val p = after.variations.getValue(archer)
        assertEquals(3, p.tier)
        assertEquals(listOf(100), p.streakDays)
        val s = summarizeSessionProgress(catalog, before, after, listOf(archer)).exercises.single()
        assertEquals(1, s.progress!!.have); assertEquals(3, s.progress!!.need); assertEquals(4, s.progress!!.toLevel)
    }

    @Test fun levelThreeChosenInThePreviewCountsWhenItsTargetIsTyped() {
        val catalog = parseCatalog(fixtureText("content/starter/catalog.json"))
        val target = catalog.policyForVariation(archer)!!.tier(3).target.value   // 6 per side = "12 reps (6 per side)"
        val (before, after, plan) = run(startTier = 1, perSideReps = target)
        assertTrue(plan.blocks.filter { it.type == BlockType.WORK }.all { it.prescriptionTier == 3 && it.alternating && it.target?.value == target })
        counted(before, after)
    }

    /** The owner's case: the logger starts at the level's own number (6 per side) and DONE without editing sends no number (BlockLog.reps = null). */
    @Test fun levelThreeChosenInThePreviewCountsWhenDoneIsPressedWithoutTyping() {
        val (before, after, _) = run(startTier = 1, perSideReps = null)
        counted(before, after)
    }

    @Test fun anExerciseNeverTrainedBeforeCountsToo() {
        val (before, after, _) = run(startTier = 0, perSideReps = null)
        counted(before, after)
    }

    @Test fun threeSuchSessionsOnDistinctDaysEarnLevelFour() {
        val catalog = parseCatalog(fixtureText("content/starter/catalog.json"))
        val engine = ProgressionEngine(catalog)
        val ev = (100..106 step 3).map { d ->
            SessionEvidence("s$d", archer, "pushup", d, 3, 3, false, false, false, null, false, 3, 0, 2, progressionRouteId = "pushup", progressionEvidenceVersion = 1)
        }
        val s = engine.replay(ev, listOf(UserAction.SelfAssessment(0, archer, 1)))
        assertEquals(4, s.tierFor(archer)); assertTrue(3 in s.variations.getValue(archer).achievedTiers)
    }

    @Test fun belowTheLevelThreeTargetDoesNotCount() {
        val (_, after, _) = run(startTier = 1, perSideReps = 5)
        assertEquals(1, after.tierFor(archer))
        assertEquals(emptyList<Int>(), after.variations.getValue(archer).streakDays)
    }
}
