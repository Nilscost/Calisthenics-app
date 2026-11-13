package app.calisthenics.domain

import app.calisthenics.domain.content.parseCatalog
import app.calisthenics.domain.feedback.Execution
import app.calisthenics.domain.feedback.Feedback
import app.calisthenics.domain.feedback.SessionEvidence
import app.calisthenics.domain.feedback.deriveEvidence
import app.calisthenics.domain.feedback.resolveFeedback
import app.calisthenics.domain.model.BlockType
import app.calisthenics.domain.model.Side
import app.calisthenics.domain.model.StrengthFocus
import app.calisthenics.domain.model.TimelineBlock
import app.calisthenics.domain.model.WorkoutPlan
import app.calisthenics.domain.progression.*
import app.calisthenics.domain.session.*
import kotlin.test.Test

/** The end screen compares the progress before this session with the progress after it (replay without / with the session's evidence). */
class SessionProgressSummaryTest {
    private val c = parseCatalog(fixtureText("content/starter/catalog.json"))
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
        // 9.3: the card says why the level was reached (three good sessions at level 3, all filled), not progress towards level 5
        assertEquals(LevelProgress(3, 3, false, 4, finished = true), e.progress); assertEquals(LevelUpBy.STREAK, e.levelUpBy)
    }

    @Test fun aLevelReachedOnTheWeeklyPathShowsFourOfFour() {
        val e = summary(std, 3, listOf(0, 10, 20, 28)).exercises.single()
        assertEquals(4, e.leveledUpTo); assertEquals(LevelUpBy.STREAK, e.levelUpBy)
        assertEquals(LevelProgress(4, 4, true, 4, finished = true), e.progress)
    }

    @Test fun twoLevelUpsByStreakLookTheSameWhateverTheEngineDoesWithTheStreak() {
        // the engine clears the streak after a level-up (0 of 3); the card must not depend on that
        val e = summary(std, 1, listOf(0, 4, 8)).exercises.single()
        assertEquals(2, e.leveledUpTo)
        assertEquals(LevelProgress(3, 3, false, 2, finished = true), e.progress)
    }

    /** Typed reps that already met level 3's targets: the level is adopted at once, with one good session towards level 4. */
    @Test fun aLevelReachedByDemonstratedRepsSaysSoAndKeepsTheNormalDots() {
        val policy = c.policyForVariation(std)!!
        val plan = WorkoutPlan("p3", "usual", 1, 1, 0, "home", 180, 180, setOf(StrengthFocus.FULL_BODY), false, null, 3, emptyList(), emptyList(), false,
            false, emptyList(), progressionEvidenceVersion = 1, progressionPolicies = mapOf(std to policy))
        val work = List(3) { i -> TimelineBlock("w${i + 1}", BlockType.WORK, 90, i + 1, "push", std, side = Side.NONE, target = policy.tier(3).target, prescriptionTier = 3, freeTarget = false) }
        val full = plan.copy(blocks = work)
        val logs = work.associate { it.id to Feedback(actualReps = policy.tier(3).target.value) }
        val outcomes = resolveFeedback(full, work.associate { it.id to Execution.COMPLETED }, logs)
        val ev = deriveEvidence("s5", 5, full, outcomes, { c.variation(it)?.familyId ?: it }, { v, t -> c.policyForVariation(v)?.tier(t ?: 1)?.minQualifyingBlocks ?: 2 },
            loggedBlockIds = work.map { it.id }.toSet(), effectivePolicyOf = { c.policyForVariation(it) })
        val act = listOf(UserAction.SelfAssessment(0, std, 1))
        val e = summarizeSessionProgress(c, engine.replay(emptyList(), act), engine.replay(ev, act), listOf(std)).exercises.single()
        assertEquals(3, e.leveledUpTo); assertEquals(LevelUpBy.DEMONSTRATED, e.levelUpBy)
        assertEquals(LevelProgress(1, 3, false, 4), e.progress)
    }

    @Test fun anExerciseThatDidNotLevelUpHasNoLevelUpBy() {
        assertNull(summary(std, 3, listOf(0, 5)).exercises.single().levelUpBy)
    }

    @Test fun aMasteredExerciseNamesTheNextStepOfItsRouteWhenTheRouteMovedOn() {
        val archer = "pushup-archer"
        val now = summary(archer, 5, listOf(0, 5, 10)).exercises.single()
        assertTrue(now.mastered); assertEquals("pushup-half-one-arm", now.nextVariationId)
        // mastered before this session: still named
        val act = listOf(UserAction.SelfAssessment(0, archer, 5))
        val days = listOf(0, 5, 10, 14)
        val all = days.map { ev(it, archer, 5) }
        val s = summarizeSessionProgress(c, engine.replay(all.dropLast(1), act), engine.replay(all, act), listOf(archer)).exercises.single()
        assertFalse(s.mastered); assertTrue(s.masteredAlready); assertEquals("pushup-half-one-arm", s.nextVariationId)
    }

    @Test fun noNextStepWhenTheRouteEndsOrTheExerciseIsNotMastered() {
        val last = c.progressionRoutes.first { it.id == "pushup" }.mainPath.last()
        assertNull(summary(last, 5, listOf(0, 5, 10)).exercises.single().nextVariationId)
        assertNull(summary(std, 3, listOf(0, 5, 10)).exercises.single().nextVariationId)
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

    // 9.2: the next main step of the exercise the person still trains is not announced as "new"; the route moving on to it is
    private val split = "split-squat"
    private val bulgarian = "split-squat-bulgarian"
    private fun squatEv(day: Int, tier: Int) =
        SessionEvidence("s$day$split", split, c.variation(split)!!.familyId, day, tier, 3, false, false, false, null, false, 3, 0, 2, progressionRouteId = "squat")

    @Test fun reachingSplitSquatTierThreeDoesNotAnnounceTheBulgarianSplitSquat() {
        val s = summary(split, 3, listOf(0, 5, 10)) { squatEv(it, 3) }
        assertTrue(s.newExercises.none { it.variationId == bulgarian })
    }

    @Test fun movingOnAfterSplitSquatMasteryAnnouncesTheBulgarianSplitSquat() {
        val s = summary(split, 5, listOf(0, 5, 10)) { squatEv(it, 5) }
        assertTrue(s.exercises.single().mastered)
        val next = s.newExercises.firstOrNull { it.variationId == bulgarian }
        assertEquals(split, next?.unlockedBy)
    }

    @Test fun unrelatedPrerequisitesMetThisSessionAreStillAnnounced() {
        val s = summary(split, 5, listOf(0, 5, 10)) { squatEv(it, 5) }
        assertTrue(s.newExercises.isNotEmpty())
    }

}
