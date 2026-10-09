package app.calisthenics.domain

import app.calisthenics.domain.feedback.*
import app.calisthenics.domain.model.*
import app.calisthenics.domain.progression.*
import org.junit.Assert.*
import org.junit.Test

/** V02 / D7: two sessions in a row "too easy" suggest one level up; nothing changes without the owner. */
class TooEasyTest {
    private val engine = ProgressionEngine(Fx.catalog())
    private val V = "pushup-knee"
    private val start = UserAction.SelfAssessment(0, V, 2)
    private fun ev(day: Int, above: Boolean = false, below: Boolean = false, discomfort: Boolean = false, tier: Int = 2, skipped: Boolean = false) =
        SessionEvidence("s$day", V, "pushup", day, tier, 2, skipped, below, discomfort, if (discomfort) day else null, false, 2, 0, 2, anyAbove = above)

    @Test fun `one too easy session suggests nothing`() {
        val p = engine.replay(listOf(ev(0, above = true)), listOf(start)).variations.getValue(V)
        assertEquals(1, p.consecutiveAbove); assertFalse(p.raiseSuggested)
    }

    @Test fun `two in a row suggest one level up and leave the tier alone`() {
        val s = engine.replay(listOf(ev(0, above = true), ev(2, above = true)), listOf(start))
        val p = s.variations.getValue(V)
        assertTrue(p.raiseSuggested); assertEquals(2, p.tier)
        assertEquals(1, s.events.count { it.message.contains("too easy") && it.message.contains("tier 3") })
    }

    @Test fun `a normal session between breaks the row`() {
        val p = engine.replay(listOf(ev(0, above = true), ev(2), ev(4, above = true)), listOf(start)).variations.getValue(V)
        assertFalse(p.raiseSuggested); assertEquals(1, p.consecutiveAbove)
    }

    @Test fun `below target or discomfort or skipped never counts as too easy`() {
        for (bad in listOf(ev(2, above = true, below = true), ev(2, above = true, discomfort = true), ev(2, above = true, skipped = true))) {
            val p = engine.replay(listOf(ev(0, above = true), bad), listOf(start)).variations.getValue(V)
            assertFalse(p.raiseSuggested)
        }
    }

    @Test fun `the suggestion is made once and only the owner's acceptance moves the tier`() {
        val e = listOf(ev(0, above = true), ev(2, above = true), ev(4, above = true))
        val s = engine.replay(e, listOf(start))
        assertEquals(1, s.events.count { it.message.contains("too easy") })
        assertEquals(2, s.tierFor(V))
        val acc = engine.replay(e, listOf(start, UserAction.AcceptHarder(5, V, 3))).variations.getValue(V)
        assertEquals(3, acc.tier); assertFalse(acc.raiseSuggested); assertEquals(0, acc.consecutiveAbove)
    }

    @Test fun `no suggestion at the top tier`() {
        val top = UserAction.SelfAssessment(0, V, 5)
        val p = engine.replay(listOf(ev(0, above = true, tier = 5), ev(2, above = true, tier = 5)), listOf(top)).variations.getValue(V)
        assertFalse(p.raiseSuggested)
    }

    @Test fun `too easy never speeds up the normal progression gate`() {
        // three qualifying days over 7 days advance exactly one tier, with or without "too easy"
        val plain = engine.replay(listOf(ev(0), ev(3), ev(7)), listOf(start)).tierFor(V)
        val easy = engine.replay(listOf(ev(0, above = true), ev(3, above = true), ev(7, above = true)), listOf(start)).tierFor(V)
        assertEquals(plain, easy); assertEquals(3, easy)
    }

    @Test fun `evidence derivation marks a session with an ABOVE rating`() {
        val blk = TimelineBlock("b", BlockType.WORK, 60, 1, "s", "pushup-knee", Side.NONE, Target(TargetType.REPS, 8), 2)
        val plan = WorkoutPlan("p", "r", 1, 1, 0L, "home", 0, 60, emptySet(), false, null, 1, emptyList(), emptyList(), false, false, listOf(blk))
        val out = resolveFeedback(plan, mapOf("b" to Execution.COMPLETED), emptyMap(), mapOf("pushup-knee" to Feedback(Rating.ABOVE)))
        val e = deriveEvidence("s", 0, plan, out, { "pushup" }, { _, _ -> 1 }).single()
        assertTrue(e.anyAbove); assertTrue(e.qualifying)
    }
}
