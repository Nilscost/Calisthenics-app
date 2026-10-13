package app.calisthenics.domain

import app.calisthenics.domain.feedback.*
import app.calisthenics.domain.model.*
import app.calisthenics.domain.progression.*
import org.junit.Assert.*
import org.junit.Test

/** V02 / D7, changed by R7.2-T06 (owner decision O1, 2026-10-13): ONE session rated "too easy" suggests a move on; nothing changes without the owner. */
class TooEasyTest {
    private val engine = ProgressionEngine(Fx.catalog())
    private val V = "pushup-knee"
    private val start = UserAction.SelfAssessment(0, V, 2)
    private fun ev(day: Int, above: Boolean = false, below: Boolean = false, discomfort: Boolean = false, tier: Int = 2, skipped: Boolean = false, ruleMet: Boolean? = null) =
        SessionEvidence("s$day", V, "pushup", day, tier, 2, skipped, below, discomfort, if (discomfort) day else null, false, 2, 0, 2, anyAbove = above, ruleMet = ruleMet)

    @Test fun `one too easy session suggests a move on and leaves the tier alone`() {   // was: one suggests nothing, two suggest (D7 / B9)
        val s = engine.replay(listOf(ev(0, above = true)), listOf(start))
        val p = s.variations.getValue(V)
        assertEquals(1, p.consecutiveAbove); assertTrue(p.raiseSuggested); assertFalse(p.ruleAbove); assertEquals(2, p.tier)
        assertEquals(1, s.events.count { it.message.contains("too easy") })
    }

    @Test fun `a normal session between restarts the count and a new too easy counts again`() {   // was: asserted raiseSuggested false (it needed two in a row)
        val p = engine.replay(listOf(ev(0, above = true), ev(2), ev(4, above = true)), listOf(start)).variations.getValue(V)
        assertEquals(1, p.consecutiveAbove); assertTrue(p.raiseSuggested)
        assertEquals(0, engine.replay(listOf(ev(0, above = true), ev(2)), listOf(start)).variations.getValue(V).consecutiveAbove)
    }

    @Test fun `below target or discomfort or skipped in the same session never counts as too easy`() {   // was: a first too easy, then the bad one
        for (bad in listOf(ev(2, above = true, below = true), ev(2, above = true, discomfort = true), ev(2, above = true, skipped = true))) {
            val p = engine.replay(listOf(bad), listOf(start)).variations.getValue(V)
            assertFalse(p.raiseSuggested); assertEquals(0, p.consecutiveAbove)
        }
    }

    @Test fun `the suggestion is made once and only the owner's acceptance moves the tier and counts the skipped levels as done`() {
        val e = listOf(ev(0, above = true), ev(2, above = true), ev(4, above = true))
        val s = engine.replay(e, listOf(start))
        assertEquals(1, s.events.count { it.message.contains("too easy") })
        assertEquals(2, s.tierFor(V)); assertEquals(3, s.variations.getValue(V).consecutiveAbove)
        val acc = engine.replay(e, listOf(start, UserAction.AcceptHarder(5, V, 4))).variations.getValue(V)
        assertEquals(4, acc.tier); assertFalse(acc.raiseSuggested); assertEquals(0, acc.consecutiveAbove)
        assertTrue("the jumped levels 2 and 3 are done", acc.achievedTiers.containsAll(setOf(2, 3)))
        assertEquals(3, acc.earnedStars())   // baseline 2: levels 2 and 3 done, 1 counts below the start
    }

    @Test fun `at the top tier the move on is flagged too and the count is kept`() {   // was: no raise at the top tier
        val top = UserAction.SelfAssessment(0, V, 5)
        val p = engine.replay(listOf(ev(0, above = true, tier = 5), ev(2, above = true, tier = 5)), listOf(top)).variations.getValue(V)
        assertTrue(p.raiseSuggested); assertEquals(2, p.consecutiveAbove)
    }

    @Test fun `under a rep range rule the session is flagged as rule based so the move on is the next exercise`() {
        val p = engine.replay(listOf(ev(0, above = true, ruleMet = false)), listOf(start)).variations.getValue(V)
        assertTrue(p.raiseSuggested); assertTrue(p.ruleAbove)
        val levels = engine.replay(listOf(ev(0, above = true)), listOf(start)).variations.getValue(V)
        assertFalse(levels.ruleAbove)
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
