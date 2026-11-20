package app.calisthenics.domain

import app.calisthenics.domain.feedback.*
import app.calisthenics.domain.model.*
import app.calisthenics.domain.planner.*
import app.calisthenics.domain.progression.*
import app.calisthenics.domain.session.*
import kotlinx.serialization.json.Json
import kotlin.test.Test

/** Real generated prescription -> reducer logs -> serialized history -> evidence -> next plan. */
class PhoneFeedbackLoggingReplayTest {
    @Test fun loggedHigherPerformanceIsRetainedThroughReplayAndChangesTheNextPrescription() {
        val catalog = Fx.catalog()
        val engine = ProgressionEngine(catalog)
        val actions = listOf(UserAction.SelfAssessment(0, "pushup-knee", 1))
        val initial = engine.replay(emptyList(), actions)
        val routine = Routine("usual", 1, "Push", listOf(RoutineSlot("push", Pattern.PUSH_HORIZONTAL, Area.UPPER_BODY, "pushup-knee")))
        val settings = TrainSettings(rounds = 3, exercisesPerRound = 1, betweenSetMode = BetweenSetMode.FLOW)
        val profile = EquipmentProfile("home", "Home", emptyList(), emptySet())
        val json = Json { encodeDefaults = true }
        val source = (buildTrainPlan(catalog, routine, initial, profile, settings, nowEpochMs = 1,
            edits = PlanEdits(tierOverrides = mapOf("push" to 3))) as PlanResult.Ready).plan
        assertEquals(1, source.progressionEvidenceVersion)
        val actual = catalog.policyForVariation("pushup-knee")!!.tier(3).target.value + 1
        val historyPlan = json.decodeFromString(WorkoutPlan.serializer(), json.encodeToString(WorkoutPlan.serializer(), source))
        val work = historyPlan.blocks.filter { it.type == BlockType.WORK }
        assertTrue(work.isNotEmpty())
        var state = reduce(newSession("logged", historyPlan), SessionEvent.Start(0)).state
        var now = 0L
        while (!state.isTerminal) {
            val b = state.currentBlock!!
            if (b.type == BlockType.WORK) state = reduce(state, SessionEvent.LogBlock(b.id, BlockLog(reps = actual))).state
            now += b.durationSeconds * 1000L
            state = reduce(state, SessionEvent.Tick(now)).state
        }
        assertEquals(work.size, state.logged.size)
        assertTrue(state.logged.values.all { it.reps == actual })
        val rows = rowLogsFrom(historyPlan, state.logged)
        assertEquals(actual, rows.single().actualReps)
        val feedback = state.logged.mapValues { Feedback(actualReps = it.value.reps) }
        fun exposure(day: Int) = deriveEvidence("logged-$day", day, historyPlan,
            resolveFeedback(historyPlan, state.executions, feedback), { catalog.variation(it)!!.familyId },
            { id, tier -> catalog.policyForVariation(id)!!.tier(tier ?: 1).minQualifyingBlocks },
            loggedBlockIds = state.logged.keys, effectivePolicyOf = catalog::policyForVariation).single()
        assertEquals(work.size, exposure(1).confirmedBlocks)
        assertEquals(0, exposure(1).assumedBlocks)
        val adoption = engine.replay(listOf(exposure(1)), actions)
        assertEquals(3, adoption.tierFor("pushup-knee"))
        assertEquals(listOf(1), adoption.variations.getValue("pushup-knee").streakDays)
        val next = (buildTrainPlan(catalog, routine, adoption, profile, settings, nowDay = 2) as PlanResult.Ready).plan
        assertTrue(next.blocks.filter { it.type == BlockType.WORK }.all { it.prescriptionTier == 3 })
        val advanced = engine.replay(listOf(exposure(1), exposure(2), exposure(3)), actions)
        assertEquals(4, advanced.tierFor("pushup-knee"))
        assertEquals(advanced, engine.replay(listOf(exposure(1), exposure(2), exposure(3)), actions))
        // Late per-block correction below target removes the adoption on deterministic replay.
        val corrected = feedback + (work.last().id to Feedback(actualReps = 1))
        val revised = deriveEvidence("logged-1", 1, historyPlan, resolveFeedback(historyPlan, state.executions, corrected),
            { catalog.variation(it)!!.familyId }, { _, _ -> 2 }, loggedBlockIds = state.logged.keys,
            effectivePolicyOf = catalog::policyForVariation).single()
        assertEquals(1, engine.replay(listOf(revised), actions).tierFor("pushup-knee"))
    }
}
