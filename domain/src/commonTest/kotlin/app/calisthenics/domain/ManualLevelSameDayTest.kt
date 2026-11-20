package app.calisthenics.domain

import app.calisthenics.domain.content.parseCatalog
import app.calisthenics.domain.feedback.SessionEvidence
import app.calisthenics.domain.progression.*
import app.calisthenics.domain.session.summarizeSessionProgress
import kotlin.test.Test

/** Owner report (release 8.2): the archer push-up was set to level 3 by hand, the level-3 targets were met the same day, and the session did not count as a good session. */
class ManualLevelSameDayTest {
    private val c = parseCatalog(fixtureText("content/starter/catalog.json"))
    private val engine = ProgressionEngine(c)
    private val archer = "pushup-archer"
    private fun ev(day: Int, tier: Int = 3) =
        SessionEvidence("s$day", archer, c.variation(archer)!!.familyId, day, tier, 3, false, false, false, null, false, 3, 0, 2, progressionRouteId = "pushup")
    /** The hand edit "levels 1..2 are done, level 3 is current", the way the level editor stores it. */
    private fun edit(day: Int) = selfAssessmentFor(archer, storedForLevelsDone(2), day)

    @Test fun sessionOnTheDayOfTheEditIsTheFirstGoodSession() {
        val after = engine.replay(listOf(ev(100)), listOf(edit(100)))
        val p = after.variations.getValue(archer)
        assertEquals(3, p.tier); assertEquals(listOf(100), p.streakDays)
        val before = engine.replay(emptyList(), listOf(edit(100)))
        val s = summarizeSessionProgress(c, before, after, listOf(archer)).exercises.single()
        assertEquals(1, s.progress!!.have); assertEquals(3, s.progress!!.need); assertEquals(4, s.progress!!.toLevel)
    }

    @Test fun threeGoodSessionsFromTheEditDayEarnLevelFour() {
        val s = engine.replay(listOf(ev(100), ev(103), ev(106)), listOf(edit(100)))
        assertEquals(4, s.tierFor(archer)); assertTrue(3 in s.variations.getValue(archer).achievedTiers)
    }

    @Test fun sessionsBeforeTheEditDayStillDoNotCount() {
        val s = engine.replay(listOf(ev(99)), listOf(edit(100)))
        assertEquals(emptyList<Int>(), s.variations.getValue(archer).streakDays)
    }
}
