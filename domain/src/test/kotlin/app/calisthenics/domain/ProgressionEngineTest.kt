package app.calisthenics.domain

import app.calisthenics.domain.feedback.SessionEvidence
import app.calisthenics.domain.model.*
import app.calisthenics.domain.progression.*
import org.junit.Assert.*
import org.junit.Test

class ProgressionEngineTest {
    private val engine = ProgressionEngine(Fx.catalog())
    private val V = "pushup-knee"

    private fun ev(day: Int, tier: Int = 2, blocks: Int = 2, below: Boolean = false, discomfort: Boolean = false,
               skipped: Boolean = false, confirmed: Int = 0, vid: String = V, discDay: Int? = null) =
        SessionEvidence("s$day$vid", vid, "pushup", day, tier, blocks, skipped, below, discomfort,
            if (discomfort) (discDay ?: day) else null, false, blocks - confirmed, confirmed, 2)

    private val start = UserAction.SelfAssessment(0, V, 2)

    @Test fun `three qualifying days spanning seven advance one tier and award a star`() {
        val s = engine.replay(listOf(ev(0), ev(3), ev(7)), listOf(start))
        val p = s.variations.getValue(V)
        assertEquals(3, p.tier); assertEquals(setOf(2), p.achievedTiers)
        assertTrue(s.events.any { it.message.contains("Tier 2 achieved") })
        assertEquals(1, p.earnedStars().let { if (p.baselineTier == 2) 1 else it }.coerceAtLeast(1))
    }

    @Test fun `days 0 2 6 do not advance`() {
        assertEquals(2, engine.replay(listOf(ev(0), ev(2), ev(6)), listOf(start)).tierFor(V))
    }

    @Test fun `no advancement from calendar passage alone`() {
        val s = engine.replay(listOf(ev(0), ev(3)), listOf(start))
        assertEquals(2, s.tierFor(V))
        assertEquals(2, s.copy().tierFor(V)) // nothing time-based exists in replay input
    }

    @Test fun `replay is idempotent and order independent`() {
        val e = listOf(ev(0), ev(3), ev(7))
        val a = engine.replay(e, listOf(start)); val b = engine.replay(e.reversed(), listOf(start)); val c = engine.replay(e, listOf(start))
        assertEquals(a, b); assertEquals(a, c)
    }

    @Test fun `below target restarts the streak and two in a row suggest a lower target without changing it`() {
        val s = engine.replay(listOf(ev(0), ev(2, below = true), ev(4, below = true)), listOf(start))
        val p = s.variations.getValue(V)
        assertEquals(2, p.tier); assertTrue(p.lowerTargetSuggested); assertTrue(p.streakDays.isEmpty())
    }

    @Test fun `skipped or one block sessions do not count`() {
        val s = engine.replay(listOf(ev(0), ev(3, blocks = 1), ev(7, skipped = true)), listOf(start))
        assertEquals(2, s.tierFor(V))
    }

    @Test fun `discomfort pauses the whole family and silence never clears it`() {
        val s = engine.replay(listOf(ev(0), ev(3, discomfort = true), ev(7), ev(10), ev(14)), listOf(start))
        assertNotNull(s.holdFor("pushup")); assertEquals(2, s.tierFor(V))
    }

    @Test fun `discomfort on a sibling variation holds the family`() {
        val e = listOf(ev(0), ev(3, discomfort = true, vid = "pushup-std"), ev(7), ev(10), ev(14))
        val s = engine.replay(e, listOf(start))
        assertNotNull(s.holdFor("pushup")); assertEquals(2, s.tierFor(V))
    }

    @Test fun `comfort clearance resumes but never jumps immediately`() {
        val acts = listOf(start, UserAction.ComfortClearance(20, "pushup"))
        val s = engine.replay(listOf(ev(0), ev(3, discomfort = true), ev(7), ev(21)), acts)
        assertNull(s.holdFor("pushup")); assertEquals(2, s.tierFor(V))
        val later = engine.replay(listOf(ev(0), ev(3, discomfort = true), ev(21), ev(24), ev(28)), acts)
        assertEquals(3, later.tierFor(V))
    }

    @Test fun `late discomfort edit is applied from the report day and is deterministic`() {
        // advanced on day 7, discomfort for the day-3 session reported on day 9: tier already earned stays,
        // but no further advance is allowed afterwards.
        val e = listOf(ev(0), ev(3, discomfort = true, discDay = 9), ev(7))
        val s = engine.replay(e, listOf(start))
        assertNotNull(s.holdFor("pushup")); assertEquals(e.size, 3)
        assertEquals(s, engine.replay(e.reversed(), listOf(start)))
    }

    @Test fun `accepting an easier target is explicit and keeps the hold`() {
        val s = engine.replay(listOf(ev(0), ev(3, discomfort = true)), listOf(start, UserAction.AcceptEasier(4, V, 1)))
        assertEquals(1, s.tierFor(V)); assertNotNull(s.holdFor("pushup"))
    }

    @Test fun `manual easier pauses automatic increases until resume`() {
        val acts = listOf(start, UserAction.ManualOverride(1, V, 1))
        val s = engine.replay(listOf(ev(2, tier = 1), ev(5, tier = 1), ev(9, tier = 1)), acts)
        assertEquals(1, s.tierFor(V)); assertTrue(s.variations.getValue(V).autoPaused)
        val resumed = engine.replay(listOf(ev(2, tier = 1), ev(5, tier = 1), ev(9, tier = 1), ev(12, tier = 1), ev(16, tier = 1), ev(20, tier = 1)),
            acts + UserAction.ResumeNormal(10, V))
        assertEquals(2, resumed.tierFor(V))
    }

    @Test fun `only one tier per seven days even with many sessions`() {
        val days = listOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
        val s = engine.replay(days.map { ev(it, tier = if (it <= 7) 2 else 3) }, listOf(start))
        assertEquals(3, s.tierFor(V))
    }

    @Test fun `five stars kept and successor chosen after top tier`() {
        // walk up tiers 2..5 every 7 days, then the top-tier gate
        val evs = mutableListOf<SessionEvidence>(); var d = 0; var tier = 2
        val acts = listOf(start)
        repeat(4) { // tiers 2,3,4,5 achieved => after tier 5 successor
            evs += ev(d, tier); evs += ev(d + 3, tier); evs += ev(d + 7, tier); d += 7; tier++
        }
        val s = engine.replay(evs, acts)
        val p = s.variations.getValue(V)
        assertTrue(p.achievedTiers.containsAll(setOf(2, 3, 4, 5)))
        assertEquals("pushup-std", s.activeInFamily["pushup"])
    }

    @Test fun `reentry after fourteen days offers one tier lower and keeps stars`() {
        val s = engine.replay(listOf(ev(0), ev(3), ev(7)), listOf(start))
        assertEquals(2, s.reentryNeeded(V, 7 + 14))
        assertNull(s.reentryNeeded(V, 7 + 13))
        val after = engine.replay(listOf(ev(0), ev(3), ev(7)), listOf(start, UserAction.ReentryChoice(30, V, 2)))
        assertNull(after.reentryNeeded(V, 31)); assertEquals(setOf(2), after.variations.getValue(V).achievedTiers)
    }

    @Test fun `assumed results are disclosed in the explanation`() {
        val s = engine.replay(listOf(ev(0), ev(3), ev(7)), listOf(start))
        assertTrue(s.events.any { it.message.contains("assumed") })
        val confirmed = engine.replay(listOf(ev(0, confirmed = 2), ev(3, confirmed = 2), ev(7, confirmed = 2)), listOf(start))
        assertTrue(confirmed.events.none { it.message.contains("assumed") })
    }

    @Test fun `self reported baseline is kept separately from earned stars`() {
        val s = engine.replay(emptyList(), listOf(start))
        assertEquals(2, s.variations.getValue(V).baselineTier); assertEquals(1, s.variations.getValue(V).earnedStars()) // D8: the level below the starting level counts as a star
    }
}
