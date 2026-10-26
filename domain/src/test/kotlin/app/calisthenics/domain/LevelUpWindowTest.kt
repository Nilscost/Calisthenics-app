package app.calisthenics.domain

import app.calisthenics.domain.content.parseCatalog
import app.calisthenics.domain.feedback.SessionEvidence
import app.calisthenics.domain.progression.*
import org.junit.Assert.*
import org.junit.Test
import java.io.File

/** R8.1-T04 (F4): a level rises after 3 qualifying sessions inside any rolling 14-day window; same-rung alternates count together. */
class LevelUpWindowTest {
    private val c = parseCatalog(File(System.getProperty("repo.root"), "content/starter/catalog.json").readText())
    private val engine = ProgressionEngine(c)
    private fun ev(day: Int, vid: String, tier: Int, below: Boolean = false, tooHard: Boolean = false, pain: Boolean = false) =
        SessionEvidence("s$day$vid", vid, c.variation(vid)!!.familyId, day, tier, 3, false, below || tooHard, pain, if (pain) day else null, false, 3, 0, 2, progressionRouteId = "pushup")

    @Test fun threeSessionsInTenDaysRaiseTheLevelWithoutTrainingThreeTimesAWeek() {
        val s = engine.replay(listOf(ev(0, "pushup-standard", 3), ev(5, "pushup-standard", 3), ev(10, "pushup-standard", 3)), listOf(UserAction.SelfAssessment(0, "pushup-standard", 3)))
        assertEquals(4, s.tierFor("pushup-standard")); assertTrue(3 in s.variations.getValue("pushup-standard").achievedTiers)
    }

    @Test fun threeSessionsSpreadOverThreeWeeksDoNotButNothingIsLost() {
        val act = listOf(UserAction.SelfAssessment(0, "pushup-standard", 3))
        val s = engine.replay(listOf(ev(0, "pushup-standard", 3), ev(10, "pushup-standard", 3), ev(20, "pushup-standard", 3)), act)
        assertEquals(3, s.tierFor("pushup-standard"))
        // a month off, then the sessions count again; the stars earned before stay
        val earned = engine.replay(listOf(ev(0, "pushup-standard", 3), ev(5, "pushup-standard", 3), ev(10, "pushup-standard", 3), ev(60, "pushup-standard", 4)), act)
        assertEquals(4, earned.tierFor("pushup-standard")); assertTrue(3 in earned.variations.getValue("pushup-standard").achievedTiers)
        assertEquals(3, earned.variations.getValue("pushup-standard").earnedStars())
    }

    @Test fun tooHardOrPainSessionsDoNotCountAndPainStillPausesTheFamily() {
        val act = listOf(UserAction.SelfAssessment(0, "pushup-standard", 3))
        assertEquals(3, engine.replay(listOf(ev(0, "pushup-standard", 3), ev(3, "pushup-standard", 3, tooHard = true), ev(6, "pushup-standard", 3)), act).tierFor("pushup-standard"))
        val pain = engine.replay(listOf(ev(0, "pushup-standard", 3), ev(3, "pushup-standard", 3, pain = true), ev(6, "pushup-standard", 3), ev(9, "pushup-standard", 3)), act)
        assertNotNull(pain.holdFor("pushup")); assertEquals(2, pain.tierFor("pushup-standard"))   // W2: pain steps the level one easier by itself; the level-3 sessions that follow do not count and never clear the hold
    }

    @Test fun feetElevatedAndDiamondSessionsCountTogether() {
        val fe = "pushup-feet-elevated"; val dia = "pushup-diamond"
        val act = listOf(UserAction.SelfAssessment(0, "pushup-standard", 6), UserAction.SelfAssessment(0, fe, 3), UserAction.SelfAssessment(0, dia, 2))
        // one feet-elevated and two diamond sessions inside 14 days: both exercises rise, each at its own level
        val s = engine.replay(listOf(ev(1, fe, 3), ev(4, dia, 2), ev(8, dia, 2)), act)
        assertEquals(4, s.tierFor(fe)); assertEquals(3, s.tierFor(dia))
        // the days are used up for the level just earned: the next level needs three new sessions
        val again = engine.replay(listOf(ev(1, fe, 3), ev(4, dia, 2), ev(8, dia, 2), ev(9, fe, 4)), act)
        assertEquals(4, again.tierFor(fe))
        // sessions of a different rung do not count
        val other = engine.replay(listOf(ev(1, fe, 3), ev(4, "pushup-archer", 1), ev(8, "pushup-archer", 1)), act + UserAction.SelfAssessment(0, "pushup-archer", 1))
        assertEquals(3, other.tierFor(fe))
    }

    @Test fun tooEasyKeepsItsOneTapJump() {
        val act = listOf(UserAction.SelfAssessment(0, "pushup-standard", 1))
        val s = engine.replay(emptyList(), act + UserAction.AcceptHarder(1, "pushup-standard", 3))
        assertEquals(3, s.tierFor("pushup-standard")); assertTrue(setOf(1, 2).all { it in s.variations.getValue("pushup-standard").achievedTiers })
    }

    // Owner decision: once-a-week training levels up after about a month (4 sessions inside 30 days).
    @Test fun weeklyCadenceAdvancesAtTheFourthSession() {
        val act = listOf(UserAction.SelfAssessment(0, "pushup-standard", 3))
        val three = engine.replay(listOf(ev(0, "pushup-standard", 3), ev(7, "pushup-standard", 3), ev(14, "pushup-standard", 3)), act)
        assertEquals(3, three.tierFor("pushup-standard"))
        val four = engine.replay(listOf(ev(0, "pushup-standard", 3), ev(7, "pushup-standard", 3), ev(14, "pushup-standard", 3), ev(21, "pushup-standard", 3)), act)
        assertEquals(4, four.tierFor("pushup-standard")); assertTrue(3 in four.variations.getValue("pushup-standard").achievedTiers)
    }

    @Test fun fourSessionsSpreadOver35DaysDoNotAdvance() {
        val act = listOf(UserAction.SelfAssessment(0, "pushup-standard", 3))
        val s = engine.replay(listOf(0, 12, 24, 35).map { ev(it, "pushup-standard", 3) }, act)
        assertEquals(3, s.tierFor("pushup-standard"))
        // 0, 10, 20, 29 span exactly 30 calendar days: still inside
        assertEquals(4, engine.replay(listOf(0, 10, 20, 29).map { ev(it, "pushup-standard", 3) }, act).tierFor("pushup-standard"))
    }

    @Test fun fastPathStillWorksAndSlowPathDoesNotDoubleAdvance() {
        val act = listOf(UserAction.SelfAssessment(0, "pushup-standard", 3))
        // 3 in 10 days advance once; the 4th session (day 21) alone must not advance again
        val s = engine.replay(listOf(0, 5, 10, 21).map { ev(it, "pushup-standard", 3) }, act)
        assertEquals(4, s.tierFor("pushup-standard"))
        // the used days are cleared: the next level needs its own qualifying sessions (3 weekly ones are not enough, the 4th is)
        fun at4(vararg d: Int) = d.map { ev(it, "pushup-standard", 4) }
        val three = engine.replay(listOf(0, 5, 10).map { ev(it, "pushup-standard", 3) } + at4(17, 24, 31), act)
        assertEquals(4, three.tierFor("pushup-standard"))
        val four = engine.replay(listOf(0, 5, 10).map { ev(it, "pushup-standard", 3) } + at4(17, 24, 31, 38), act)
        assertEquals(5, four.tierFor("pushup-standard"))
    }
}
