package app.calisthenics.data.history

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class HistoryEngineTest {
    private val plan = PlanRecord("p", 100, "r", 2, 3, "home", "{\"plan\":1}")
    private val session = SessionRow("s", "p", 200, 300, "COMPLETED")
    private val block = BlockRow("s", "b", "MET", 30, 8)
    private val feedback = FeedbackRow("s", "v", 1, "MET", false, true, 300, 8)
    private val initial = HistoryEngine.save(HistoryState(), plan, session, listOf(block), listOf(feedback))

    @Test fun saveRejectsDuplicateAndMissingReferencesWithoutMutatingPriorState() {
        val before = initial.copy()
        assertFailsWith<IllegalArgumentException> { HistoryEngine.save(initial, plan, session, listOf(block), emptyList()) }
        assertFailsWith<IllegalArgumentException> {
            HistoryEngine.save(initial, plan, SessionRow("s2", "missing", 1, 2, "COMPLETED"), emptyList(), emptyList())
        }
        assertFailsWith<IllegalArgumentException> {
            HistoryEngine.save(initial, plan, SessionRow("s2", "p", 1, 2, "COMPLETED"), listOf(BlockRow("missing", "b", "MET", 1, 1)), emptyList())
        }
        assertEquals(before, initial)
    }

    @Test fun restoreKeepsExistingSessionsAndAddsOnlyNewChildrenAndEvents() {
        val newSession = session.copy(sessionId = "s2", startedAtEpochMs = 400)
        val event = EventRow("e", "v", "ADVANCE", 1, 2, "progress", 500)
        val (merged, added) = HistoryEngine.restore(initial, listOf(plan.copy(planJson = "changed")), listOf(session.copy(status = "PARTIAL"), newSession),
            listOf(block.copy(outcome = "SKIPPED"), block.copy(sessionId = "s2")),
            listOf(feedback.copy(rating = "BELOW"), feedback.copy(sessionId = "s2")), listOf(event, event))
        assertEquals(1, added)
        assertEquals(listOf(session, newSession), merged.sessions)
        assertEquals(listOf(plan), merged.plans)
        assertEquals(listOf(block, block.copy(sessionId = "s2")), merged.blocks)
        assertEquals(listOf(feedback, feedback.copy(sessionId = "s2")), merged.feedback)
        assertEquals(listOf(event), merged.events)
        assertEquals(merged to 0, HistoryEngine.restore(merged, listOf(plan), listOf(session, newSession), merged.blocks, merged.feedback, listOf(event)))
    }

    @Test fun restoreMissingPlanAbortsWholeTransaction() {
        val before = initial.copy()
        assertFailsWith<IllegalArgumentException> {
            HistoryEngine.restore(initial, listOf(plan.copy(planId = "new")), listOf(session.copy(sessionId = "s2", planId = "missing")), emptyList(), emptyList(), emptyList())
        }
        assertEquals(before, initial)
    }

    @Test fun roundCorrectionsAdvanceRevisionsAndRetainRepVersusHoldValues() {
        val (result, revision) = HistoryEngine.revise(initial, "s", "v", listOf(
            RoundRevision("b1", 6, "BELOW", false), RoundRevision("b2", 35, "MET", true, isHold = true)), 999)
        assertEquals(3, revision)
        assertEquals(listOf(feedback, FeedbackRow("s", "v", 2, "BELOW", false, false, 999, 6, "b1"),
            FeedbackRow("s", "v", 3, "MET", true, false, 999, null, "b2", 35)), result.feedback)
        assertFailsWith<IllegalArgumentException> { HistoryEngine.revise(initial, "missing", "v", listOf(RoundRevision("b", 1, "MET", false)), 1) }
        assertEquals(listOf(feedback), initial.feedback)
    }
}
