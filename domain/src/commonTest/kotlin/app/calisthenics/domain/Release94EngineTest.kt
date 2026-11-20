package app.calisthenics.domain

import app.calisthenics.domain.content.parseCatalog
import app.calisthenics.domain.equipment.SeedProfiles
import app.calisthenics.domain.feedback.SessionEvidence
import app.calisthenics.domain.model.*
import app.calisthenics.domain.model.Target
import app.calisthenics.domain.planner.*
import app.calisthenics.domain.progression.*
import app.calisthenics.domain.routine.SessionDraft
import app.calisthenics.domain.tree.displayStars
import kotlin.test.Test

/** Release 9.4, lane L1 (docs/44): F1 stars follow your own level, F2 hold then step down, F4 re-entry applied by the engine, F8 stars for levels below. */
class Release94EngineTest {
    // ---- F1 / F4 / F8 on the real starter catalog -------------------------------------------------------------------------

    private val real = parseCatalog(fixtureText("content/starter/catalog.json"))
    private val engine = ProgressionEngine(real)

    private fun ev(day: Int, vid: String, tier: Int, completed: Int = 3, below: Boolean = false, partial: Boolean = false, version: Int = 0, shortfall: Int? = null, reentry: Int? = null) =
        SessionEvidence("s$day$vid", vid, real.variation(vid)!!.familyId, day, tier, completed, partial, below, false, null, false, completed, 0, 2,
            progressionRouteId = null, shortfallValue = shortfall, progressionEvidenceVersion = version, reentryFromTier = reentry)

    // (a) the archer push-up and the abandoned half one-arm push-up plan
    @Test fun anAbandonedPlanOfALaterExerciseDoesNotMakeTheArcherMastered() {
        val abandoned = ev(0, "pushup-half-one-arm", 1, completed = 0, partial = true)
        val s = engine.replay(listOf(abandoned) + listOf(0, 3, 8).map { ev(it, "pushup-archer", 4) }, listOf(UserAction.SelfAssessment(0, "pushup-archer", 4)))
        val archer = s.variations.getValue("pushup-archer")
        assertEquals(5, archer.tier)
        assertEquals(4, archer.earnedStars())
        assertEquals(4, displayStars(real, s, "pushup-archer"))
        assertFalse(s.variations.containsKey("pushup-half-one-arm"))      // no record from evidence without completed work
    }

    // (b) the full inverted row trained earlier, then three good sessions of the bent-knee row, all later
    @Test fun aLaterExerciseTrainedBeforeThisOneDoesNotCountAsMovedPast() {
        val full = ev(1, "inverted-row", 4)
        val bent = listOf(20, 22, 25).map { ev(it, "inverted-row-bent-knees", 3) }
        val s = engine.replay(listOf(full) + bent, listOf(UserAction.SelfAssessment(0, "inverted-row-bent-knees", 3), UserAction.SelfAssessment(0, "inverted-row", 4)))
        assertEquals(4, s.tierFor("inverted-row-bent-knees"))
        assertEquals(3, displayStars(real, s, "inverted-row-bent-knees"))
    }

    // (c) the knee push-up trained once long ago, then the standard push-up later
    @Test fun anExerciseYouLeftBehindKeepsFiveStars() {
        val s = engine.replay(listOf(ev(1, "pushup-knee", 2), ev(30, "pushup-standard", 1)), listOf(UserAction.SelfAssessment(0, "pushup-knee", 2)))
        assertEquals(5, displayStars(real, s, "pushup-knee"))
        assertEquals(0, displayStars(real, s, "pushup-standard"))
    }

    @Test fun aSuccessorTrainedTheSameDayOrEarlierOrNeverTrainedIsNotMovedPast() {
        val same = engine.replay(listOf(ev(5, "pushup-knee", 2), ev(5, "pushup-standard", 1)), listOf(UserAction.SelfAssessment(0, "pushup-knee", 2)))
        assertEquals(1, displayStars(real, same, "pushup-knee"))
        val handSet = engine.replay(listOf(ev(5, "pushup-knee", 2)), listOf(UserAction.SelfAssessment(0, "pushup-knee", 2), UserAction.SelfAssessment(0, "pushup-standard", 3)))
        assertEquals(1, displayStars(real, handSet, "pushup-knee"))
    }

    @Test fun anExerciseTrainedBeforeAndAfterTheLaterOneTakesTheMoreRecentOrder() {
        // later exercise trained on day 10, this one again on day 20: this one is the current one again
        val s = engine.replay(listOf(ev(1, "pushup-knee", 2), ev(10, "pushup-standard", 1), ev(20, "pushup-knee", 2)), listOf(UserAction.SelfAssessment(0, "pushup-knee", 2)))
        assertEquals(1, displayStars(real, s, "pushup-knee"))
    }

    @Test fun fullySkippedEvidenceCreatesNoRecordAndNoLastTrainedDay() {
        val skipped = ev(40, "pushup-knee", 2, completed = 0, partial = true)
        val none = engine.replay(listOf(skipped), emptyList())
        assertTrue(none.variations.isEmpty()); assertTrue(none.activeInFamily.isEmpty())
        val before = engine.replay(listOf(ev(1, "pushup-knee", 2)), emptyList())
        val after = engine.replay(listOf(ev(1, "pushup-knee", 2), skipped), emptyList())
        assertEquals(1, after.variations.getValue("pushup-knee").lastTrainedDay)
        assertEquals(before.variations.getValue("pushup-knee"), after.variations.getValue("pushup-knee"))
        // re-entry is not postponed by an abandoned plan
        assertNotNull(after.reentryNeeded("pushup-knee", 40))
    }

    @Test fun discomfortOnAnExerciseWithNoCompletedWorkStillStepsDownAndWorkoutCountsAreUnchanged() {
        val e = ev(3, "pushup-standard", 3, completed = 0, partial = true).copy(discomfort = true, discomfortReportedDay = 3)
        val s = engine.replay(listOf(ev(0, "pushup-standard", 3), e), listOf(UserAction.SelfAssessment(0, "pushup-standard", 3)))
        assertNotNull(s.holdFor("pushup"))
        assertEquals(1, s.familyWorkoutCounts["pushup"])
    }

    // ---- F4: the engine applies a session at the re-entry level like a ReentryChoice --------------------------------------

    private val sa4 = listOf(UserAction.SelfAssessment(0, "pushup-standard", 4))

    @Test fun aSessionOneLevelDownAfterABreakBecomesTheLevelAndKeepsStars() {
        val s = engine.replay(listOf(ev(2, "pushup-standard", 4), ev(20, "pushup-standard", 3, reentry = 4)), sa4)
        val p = s.variations.getValue("pushup-standard")
        assertEquals(3, p.tier); assertEquals(20, p.reentryResolvedDay); assertEquals(20, p.backFromBreakDay); assertEquals(4, p.highestTierReached)
        assertEquals(listOf(20), p.streakDays)                    // the session was good: a normal streak starts with it
        assertEquals(20, p.enrolledAtDay)
        assertEquals(3, p.earnedStars())                          // starting level 4 means 3 stars below it, kept
        assertEquals(null, s.reentryNeeded("pushup-standard", 25))
    }

    @Test fun reEarningTheOldLevelTakesTheNormalThreeGoodSessions() {
        val s = engine.replay(listOf(ev(2, "pushup-standard", 4), ev(20, "pushup-standard", 3, reentry = 4), ev(22, "pushup-standard", 3), ev(24, "pushup-standard", 3)), sa4)
        val p = s.variations.getValue("pushup-standard")
        assertEquals(4, p.tier); assertEquals(setOf(3), p.achievedTiers); assertEquals(4, p.highestTierReached)
    }

    @Test fun aBelowTargetReEntrySessionStillSetsTheLevelButStartsNoStreak() {
        val s = engine.replay(listOf(ev(2, "pushup-standard", 4), ev(20, "pushup-standard", 3, below = true, reentry = 4)), sa4)
        val p = s.variations.getValue("pushup-standard")
        assertEquals(3, p.tier); assertEquals(20, p.backFromBreakDay); assertTrue(p.streakDays.isEmpty())
    }

    @Test fun noReEntryWithinFourteenDaysAtTheSameLevelWithoutWorkOrAfterAHold() {
        assertEquals(4, engine.replay(listOf(ev(2, "pushup-standard", 4), ev(15, "pushup-standard", 3, reentry = 4)), sa4).tierFor("pushup-standard"))        // 13 days
        assertEquals(4, engine.replay(listOf(ev(2, "pushup-standard", 4), ev(30, "pushup-standard", 4)), sa4).tierFor("pushup-standard"))        // same level
        val p = engine.replay(listOf(ev(2, "pushup-standard", 4), ev(30, "pushup-standard", 3, completed = 0, partial = true, reentry = 4)), sa4).variations.getValue("pushup-standard")
        assertEquals(4, p.tier); assertNull(p.backFromBreakDay)
    }

    // ---- F8: levels below the one the engine brought you to count as stars ---------------------------------------------------

    @Test fun levelsBelowTheCurrentOneCountWhenTheEngineRaisedIt() {
        // older plans prescribed level 3 as a starting estimate: no baseline, levels 3 and 4 earned, now level 5
        val evs = listOf(1, 2, 3, 5, 6, 7).map { ev(it, "pushup-standard", if (it < 4) 3 else 4) }
        val p = engine.replay(evs, emptyList()).variations.getValue("pushup-standard")
        assertEquals(5, p.tier); assertEquals(setOf(3, 4), p.achievedTiers)
        assertEquals(4, p.earnedStars())
    }

    @Test fun theStarsAreKeptWhenTheLevelGoesDownAgain() {
        val evs = listOf(1, 2, 3).map { ev(it, "pushup-standard", 3) } + listOf(20).map { ev(it, "pushup-standard", 3) }
        val up = engine.replay(listOf(1, 2, 3).map { ev(it, "pushup-standard", 3) }, emptyList()).variations.getValue("pushup-standard")
        assertEquals(4, up.tier); assertEquals(3, up.earnedStars())
        val back = engine.replay(evs, listOf(UserAction.ReentryChoice(19, "pushup-standard", 3))).variations.getValue("pushup-standard")
        assertEquals(3, back.tier); assertEquals(3, back.earnedStars())
    }

    @Test fun aLevelSetByHandIsNotCreditedAsEngineStars() {
        val s = engine.replay(emptyList(), listOf(UserAction.SelfAssessment(0, "pushup-standard", 2), UserAction.ManualOverride(0, "pushup-standard", 5)))
        assertEquals(1, s.variations.getValue("pushup-standard").earnedStars().coerceAtMost(1)); assertEquals(0, s.variations.getValue("pushup-standard").starsFloor)
    }

    @Test fun anOrdinaryClimbFromLevelOneKeepsItsStarCount() {
        val p = engine.replay(listOf(1, 2, 3).map { ev(it, "pushup-standard", 1) }, emptyList()).variations.getValue("pushup-standard")
        assertEquals(2, p.tier); assertEquals(1, p.earnedStars())
    }

    // ---- F2 on a small catalog ----------------------------------------------------------------------------------------------

    private fun pol(vid: String, vararg values: Int, type: TargetType = TargetType.REPS) = ProgressionPolicy(
        id = "pol-$vid", variationId = vid, version = 1,
        tiers = values.mapIndexed { i, v -> Tier(i + 1, Target(type, v), workWindowSeconds = if (type == TargetType.HOLD_SECONDS) 120 else 40, minRecoverySeconds = 20) })

    private val chin = pol("chin", 1, 3, 5, 8, 10)
    private val row = pol("row", 3, 5, 8, 10, 12)
    private val plank = pol("plank", 10, 20, 30, 40, 50, type = TargetType.HOLD_SECONDS)
    private val small = Catalog(1, listOf(Fx.strength("chin", "chin"), Fx.strength("row", "row"), Fx.strength("plank", "plank").copy(kind = Kind.HOLD), Fx.stretch), listOf(chin, row, plank))
    private val e2 = ProgressionEngine(small)

    private fun miss(day: Int, vid: String, tier: Int, shortfall: Int) =
        SessionEvidence("m$day$vid", vid, vid, day, tier, 3, false, true, false, null, false, 0, 3, 2, shortfallValue = shortfall)
    private fun good(day: Int, vid: String, tier: Int, reduced: Boolean = false) =
        SessionEvidence("g$day$vid", vid, vid, day, tier, 3, false, false, false, null, false, 0, 3, 2, reducedPrescription = reduced)

    private fun at(vid: String, tier: Int, vararg evs: SessionEvidence) = e2.replay(evs.toList(), listOf(UserAction.SelfAssessment(0, vid, tier))).variations.getValue(vid)

    @Test fun theFirstMissHoldsTheTargetAndLosesNothing() {
        val p = at("chin", 3, miss(1, "chin", 3, 4))                // 5 5 4 at target 5
        assertNull(p.reducedTarget); assertEquals(5, p.missedTarget); assertEquals(3, p.missedTargetTier)
        assertEquals(3, p.tier); assertEquals(1, p.consecutiveBelow)
    }

    @Test fun theSecondMissInARowAtTheSameTargetStepsDownOneRep() {
        val p = at("chin", 3, miss(1, "chin", 3, 4), miss(3, "chin", 3, 4))
        assertEquals(4, p.reducedTarget); assertEquals(3, p.reducedTargetTier); assertNull(p.missedTarget)
        assertEquals(2, p.consecutiveBelow)
        assertTrue(p.lowerTargetSuggested)                           // "Below target twice in a row: an easier level is suggested" stays
    }

    @Test fun aGoodSessionBetweenTwoMissesStartsTheCountAgain() {
        val p = at("chin", 3, miss(1, "chin", 3, 4), good(3, "chin", 3), miss(5, "chin", 3, 4))
        assertNull(p.reducedTarget); assertEquals(5, p.missedTarget)
    }

    @Test fun aFarOffMissGoesToTheLowestSetAtOnce() {
        val p = at("row", 4, miss(1, "row", 4, 3))                  // 3 3 3 at target 10
        assertEquals(3, p.reducedTarget); assertNull(p.missedTarget)
        assertEquals(5, at("row", 4, miss(1, "row", 4, 5)).reducedTarget)   // exactly half counts as far off
        assertNull(at("row", 4, miss(1, "row", 4, 6)).reducedTarget)         // one more is an ordinary miss
    }

    @Test fun theStepDownNeverGoesBelowThePreviousLevelsTarget() {
        // level 3 of row (target 8), previous level 5. Every session misses by one rep below the target in force: a pair of misses is one step down.
        val evs = mutableListOf<SessionEvidence>()
        val seen = mutableListOf<Int?>()
        for (day in 1..9) {
            val p = e2.replay(evs, listOf(UserAction.SelfAssessment(0, "row", 3))).variations.getValue("row")
            val inForce = p.reducedTarget ?: 8
            evs += miss(day, "row", 3, inForce - 1)
            seen += e2.replay(evs, listOf(UserAction.SelfAssessment(0, "row", 3))).variations.getValue("row").reducedTarget
        }
        assertEquals(listOf(null, 7, 7, 6, 6, 5, 5, 5, 5), seen)
    }

    @Test fun levelOneNeverGoesBelowOneRepOrFiveSeconds() {
        val c3 = pol("c3", 3, 5, 8, 10, 12)
        val e = ProgressionEngine(Catalog(1, listOf(Fx.strength("c3", "c3"), Fx.stretch), listOf(c3)))
        fun lvl(vararg evs: SessionEvidence) = e.replay(evs.toList(), listOf(UserAction.SelfAssessment(0, "c3", 1))).variations.getValue("c3").reducedTarget
        assertEquals(2, lvl(miss(1, "c3", 1, 2), miss(2, "c3", 1, 2)))
        assertEquals(1, lvl(miss(1, "c3", 1, 2), miss(2, "c3", 1, 2), miss(3, "c3", 1, 1), miss(4, "c3", 1, 1)))      // 3 -> 2 -> 1
        assertEquals(1, lvl(miss(1, "c3", 1, 2), miss(2, "c3", 1, 2), miss(3, "c3", 1, 1), miss(4, "c3", 1, 1), miss(5, "c3", 1, 1), miss(6, "c3", 1, 1)))   // stays at 1
        val h = pol("h", 10, 20, 30, 40, 50, type = TargetType.HOLD_SECONDS)
        val eh = ProgressionEngine(Catalog(1, listOf(Fx.strength("h", "h").copy(kind = Kind.HOLD), Fx.stretch), listOf(h)))
        fun hold(vararg evs: SessionEvidence) = eh.replay(evs.toList(), listOf(UserAction.SelfAssessment(0, "h", 1))).variations.getValue("h").reducedTarget
        assertEquals(5, hold(miss(1, "h", 1, 8), miss(2, "h", 1, 8)))                                              // 10 s -> 5 s, the floor
        assertEquals(5, hold(miss(1, "h", 1, 8), miss(2, "h", 1, 8), miss(3, "h", 1, 5), miss(4, "h", 1, 5)))    // never below 5 s
    }

    @Test fun holdsStepDownByFiveSeconds() {
        assertNull(at("plank", 3, miss(1, "plank", 3, 25)).reducedTarget)
        assertEquals(25, at("plank", 3, miss(1, "plank", 3, 25), miss(2, "plank", 3, 25)).reducedTarget)
        assertEquals(10, at("plank", 3, miss(1, "plank", 3, 10)).reducedTarget)      // 10 <= 15, far off
        // never below the previous level's 20 s
        val low = at("plank", 3, miss(1, "plank", 3, 25), miss(2, "plank", 3, 25), miss(3, "plank", 3, 21), miss(4, "plank", 3, 21), miss(5, "plank", 3, 21), miss(6, "plank", 3, 21))
        assertEquals(20, low.reducedTarget)
    }

    @Test fun missingTheLoweredTargetCountsAgainstTheLoweredTarget() {
        val base = miss(1, "row", 4, 4)                                              // 4 <= 5: far off, target 4
        val p1 = at("row", 4, base, miss(2, "row", 4, 3))                           // misses the lowered 4: first miss at 4 (3 is not <= 2)
        assertEquals(4, p1.reducedTarget); assertEquals(4, p1.missedTarget)
        // the far-off drop may go below the previous level's target (8); the step-down itself never goes lower than that floor, so it stays at 4
        val p2 = at("row", 4, base, miss(2, "row", 4, 3), miss(3, "row", 4, 3))
        assertEquals(4, p2.reducedTarget)
    }

    @Test fun buildingBackUpFromALoweredTargetIsUnchanged() {
        val p = at("chin", 3, miss(1, "chin", 3, 4), miss(2, "chin", 3, 4), good(3, "chin", 3, reduced = true))
        assertNull(p.reducedTarget)                                                  // 4 + 1 reaches the level's own 5
        assertNull(p.missedTarget)
    }

    @Test fun missingARampedTargetWhileMeetingTheLevelTargetIsStillAGoodSession() {
        // an ordinary good session is not a miss: no hold, no step down, and an earlier first miss is forgotten
        val p = at("chin", 3, miss(1, "chin", 3, 4), good(2, "chin", 3))
        assertNull(p.missedTarget); assertNull(p.reducedTarget); assertEquals(0, p.consecutiveBelow)
    }

    @Test fun afterAFirstMissThePlannerPrescribesTheLevelTargetAgainNotTheRamp() {
        val c = Fx.catalog()
        val e = ProgressionEngine(c)
        fun planTarget(p: ProgressSnapshot): Int {
            val r = Routine("t", 1, "T", listOf(RoutineSlot("push", Pattern.PUSH_HORIZONTAL, Area.UPPER_BODY, "pushup-std")))
            val d = SessionDraft(600, false, emptySet(), false, false, setOf(StrengthFocus.FULL_BODY), SeedProfiles.home.id, rounds = 1)
            val plan = (generate(PlanInput("plan", 0, 4, c, r, d, SeedProfiles.home, progress = p)) as PlanResult.Ready).plan
            return plan.blocks.first { it.type == BlockType.WORK }.target!!.value
        }
        fun e1(day: Int, below: Boolean = false, short: Int? = null) = SessionEvidence("s$day", "pushup-std", "pushup", day, 2, 3, false, below, false, null, false, 0, 3, 2, shortfallValue = short)
        val act = listOf(UserAction.SelfAssessment(0, "pushup-std", 2))
        val goodOnce = e.replay(listOf(e1(1)), act)
        assertEquals(11, planTarget(goodOnce))                                      // level 10, ramp +1 after one good session
        val missed = e.replay(listOf(e1(1), e1(3, below = true, short = 9)), act)
        assertEquals(10, planTarget(missed))                                         // first miss: the level target again
        val twice = e.replay(listOf(e1(1), e1(3, below = true, short = 9), e1(5, below = true, short = 9)), act)
        assertEquals(9, planTarget(twice))                                           // second miss: one rep less
    }

    // ---- L1b ----

    @Test fun aSessionOneLevelDownWithoutThePlannersReentryMarkerChangesNothing() {
        val s = engine.replay(listOf(ev(2, "pushup-standard", 4), ev(20, "pushup-standard", 3), ev(22, "pushup-standard", 4)), sa4)
        assertEquals(4, s.tierFor("pushup-standard")); assertNull(s.variations.getValue("pushup-standard").backFromBreakDay)
        val marked = engine.replay(listOf(ev(2, "pushup-standard", 4), ev(20, "pushup-standard", 3, reentry = 4)), sa4)
        assertEquals(3, marked.tierFor("pushup-standard"))
        // a marker for a different level (the level changed since the plan) does nothing
        assertEquals(4, engine.replay(listOf(ev(2, "pushup-standard", 4), ev(20, "pushup-standard", 3, reentry = 5)), sa4).tierFor("pushup-standard"))
    }

    @Test fun thePlannerMarksAReentryPlanOnlyWhenTheLoweredLevelIsTheFinalOne() {
        val c = Fx.catalog(); val e = ProgressionEngine(c)
        val act = listOf(UserAction.SelfAssessment(0, "pushup-std", 3))
        val p = e.replay(listOf(SessionEvidence("s1", "pushup-std", "pushup", 1, 3, 3, false, false, false, null, false, 3, 0, 2)), act)
        fun work(tierOverride: Int?): TimelineBlock {
            val r = Routine("t", 1, "T", listOf(RoutineSlot("push", Pattern.PUSH_HORIZONTAL, Area.UPPER_BODY, "pushup-std")))
            val d = SessionDraft(600, false, emptySet(), false, false, setOf(StrengthFocus.FULL_BODY), SeedProfiles.home.id, rounds = 1).let { if (tierOverride != null) it.copy(tierOverrides = mapOf("push" to tierOverride)) else it }
            return (generate(PlanInput("plan", 0, 30, c, r, d, SeedProfiles.home, progress = p)) as PlanResult.Ready).plan.blocks.first { it.type == BlockType.WORK }
        }
        assertEquals(2, work(null).prescriptionTier); assertEquals(3, work(null).reentryFromTier)
        assertNull(work(4).reentryFromTier); assertNull(work(1).reentryFromTier)
    }

    @Test fun aCleanSessionBetweenTwoMissesWeeksApartResetsTheBelowCount() {
        val p = at("chin", 3, miss(1, "chin", 3, 4), good(10, "chin", 3, reduced = true), good(20, "chin", 3, reduced = true), miss(40, "chin", 3, 4))
        assertEquals(1, p.consecutiveBelow); assertFalse(p.lowerTargetSuggested)
        val q = at("chin", 3, miss(1, "chin", 3, 4), good(3, "chin", 3, reduced = true))
        assertEquals(0, q.consecutiveBelow)
        assertTrue(at("chin", 3, miss(1, "chin", 3, 4), miss(3, "chin", 3, 4)).lowerTargetSuggested)
    }

    @Test fun theFirstRealSessionOfAnOldPlanCountsTheLevelsBelowItsStartLevelAsStars() {
        val p = engine.replay(listOf(ev(1, "pushup-standard", 3, completed = 0, partial = true), ev(3, "pushup-standard", 3)), emptyList()).variations.getValue("pushup-standard")
        assertEquals(3, p.tier); assertEquals(2, p.earnedStars())
        assertEquals(3, engine.replay(listOf(ev(3, "pushup-standard", 4)), emptyList()).variations.getValue("pushup-standard").earnedStars())
        assertEquals(0, engine.replay(listOf(ev(3, "pushup-standard", 1)), emptyList()).variations.getValue("pushup-standard").earnedStars())
    }
}
