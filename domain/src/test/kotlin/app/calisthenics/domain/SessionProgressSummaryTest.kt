package app.calisthenics.domain

import app.calisthenics.domain.content.parseCatalog
import app.calisthenics.domain.feedback.SessionEvidence
import app.calisthenics.domain.progression.*
import app.calisthenics.domain.session.*
import org.junit.Assert.*
import org.junit.Test
import java.io.File

/** The end screen compares the progress before this session with the progress after it (replay without / with the session's evidence). */
class SessionProgressSummaryTest {
    private val c = parseCatalog(File(System.getProperty("repo.root"), "content/starter/catalog.json").readText())
    private val engine = ProgressionEngine(c)
    private val std = "pushup-standard"

    private fun ev(day: Int, vid: String, tier: Int, below: Boolean = false, pain: Boolean = false, shortfall: Int? = null) =
        SessionEvidence("s$day$vid", vid, c.variation(vid)!!.familyId, day, tier, 3, false, below, pain, if (pain) day else null, false, 3, 0, 2,
            progressionRouteId = "pushup", shortfallValue = shortfall)

    /** [days] are the earlier sessions; the last of [days] is "this session": before = without it, after = with it. */
    private fun summary(vid: String, tier: Int, days: List<Int>, mk: (Int) -> SessionEvidence = { ev(it, vid, tier) }): SessionProgressSummary {
        val act = listOf(UserAction.SelfAssessment(0, vid, tier))
        val all = days.map(mk)
        val before = engine.replay(all.dropLast(1), act)
        val after = engine.replay(all, act)
        return summarizeSessionProgress(c, before, after, listOf(vid))
    }

    @Test fun aLevelUpNamesTheNewLevelAndLightsTheNewStar() {
        val s = summary(std, 3, listOf(0, 5, 10))
        val e = s.exercises.single()
        assertEquals(4, e.leveledUpTo); assertEquals(4, e.tier)
        assertEquals(2, e.starsBefore); assertEquals(3, e.stars)
        assertFalse(e.mastered); assertEquals(listOf(e), s.levelUps); assertTrue(s.hasUnlocks)
        assertEquals(0, e.progress!!.have); assertEquals(5, e.progress!!.toLevel)
    }

    @Test fun masteringTheTopLevelOfARouteAlsoUnlocksTheNextExercise() {
        val s = summary(std, 5, listOf(0, 5, 10))
        val e = s.exercises.single()
        assertTrue(e.mastered); assertTrue(e.masteredAlready); assertNull(e.progress)
        assertEquals(listOf(e), s.mastered); assertTrue(s.levelUps.isEmpty())
        val next = s.newExercises.first()
        assertEquals("pushup-feet-elevated", next.variationId); assertEquals(std, next.unlockedBy)
        assertTrue(s.newExercises.size <= 3)
    }

    @Test fun anExerciseAlreadyMasteredIsNotMasteredAgain() {
        val act = listOf(UserAction.SelfAssessment(0, std, 5))
        val first = engine.replay(listOf(ev(0, std, 5), ev(5, std, 5), ev(10, std, 5)), act)
        val later = engine.replay(listOf(ev(0, std, 5), ev(5, std, 5), ev(10, std, 5), ev(12, std, 5)), act)
        val s = summarizeSessionProgress(c, first, later, listOf(std))
        assertFalse(s.exercises.single().mastered); assertTrue(s.exercises.single().masteredAlready); assertTrue(s.newExercises.isEmpty())
    }

    @Test fun progressCountsGoodSessionsOnTheFourteenDayPath() {
        val one = summary(std, 3, listOf(0)).exercises.single().progress!!
        assertEquals(LevelProgress(1, 3, false, 4), one)
        val two = summary(std, 3, listOf(0, 5)).exercises.single().progress!!
        assertEquals(LevelProgress(2, 3, false, 4), two)
        assertNull(summary(std, 3, listOf(0, 5)).exercises.single().leveledUpTo)
        assertFalse(summary(std, 3, listOf(0, 5)).hasUnlocks)
    }

    @Test fun aWeeklyRhythmShowsTheFourInThirtyDaysPath() {
        val p = summary(std, 3, listOf(0, 7, 14)).exercises.single().progress!!
        assertEquals(LevelProgress(3, 4, true, 4), p)
    }

    @Test fun theCloserPathWinsAndATieStaysOnTheFastOne() {
        assertEquals(LevelProgress(2, 3, false, 4), levelProgress(listOf(0, 3), 3))
        assertEquals(LevelProgress(0, 3, false, 4), levelProgress(emptyList(), 3))
        assertEquals(LevelProgress(3, 4, true, 6), levelProgress(listOf(0, 10, 20), 5))
    }

    @Test fun discomfortSaysEasierNextTimeAndShowsNoDots() {
        val s = summary(std, 3, listOf(0, 4)) { if (it == 4) ev(it, std, 3, pain = true) else ev(it, std, 3) }
        val e = s.exercises.single()
        assertTrue(e.easierNextTime); assertNull(e.progress); assertFalse(e.levelKept)
    }

    @Test fun aShortfallGivesTheLowestSetAsNextTargetAndKeepsTheLevel() {
        val s = summary(std, 3, listOf(0, 4)) { if (it == 4) ev(it, std, 3, below = true, shortfall = 6) else ev(it, std, 3) }
        val e = s.exercises.single()
        assertEquals(6, e.nextTarget); assertFalse(e.nextTargetIsHold)
        assertTrue(e.levelKept); assertNull(e.leveledUpTo); assertEquals(3, e.tier)
    }

    @Test fun belowTargetWithoutALoweredTargetStillSaysLevelKept() {
        val s = summary(std, 3, listOf(0, 4)) { if (it == 4) ev(it, std, 3, below = true) else ev(it, std, 3) }
        assertTrue(s.exercises.single().levelKept); assertNull(s.exercises.single().nextTarget)
    }

    @Test fun exercisesWithoutLevelsAreListedWithoutProgress() {
        val stretch = c.variations.first { c.policyForVariation(it.id) == null }.id
        val before = engine.replay(emptyList(), emptyList())
        val s = summarizeSessionProgress(c, before, before, listOf(stretch))
        assertFalse(s.exercises.single().hasProgression); assertNull(s.exercises.single().progress)
        assertFalse(s.hasUnlocks)
    }

    @Test fun anExerciseNeverTrainedBeforeIsNotALevelUp() {
        val before = engine.replay(emptyList(), emptyList())
        val after = engine.replay(listOf(ev(0, std, 1)), emptyList())
        val e = summarizeSessionProgress(c, before, after, listOf(std)).exercises.single()
        assertNull(e.leveledUpTo); assertFalse(e.mastered); assertTrue(e.hasProgression)
    }

}
