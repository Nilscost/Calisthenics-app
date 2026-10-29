package app.calisthenics.domain

import app.calisthenics.domain.equipment.SeedProfiles
import app.calisthenics.domain.feedback.*
import app.calisthenics.domain.model.*
import app.calisthenics.domain.model.Target
import app.calisthenics.domain.planner.*
import app.calisthenics.domain.progression.*
import app.calisthenics.domain.routine.SessionDraft
import kotlin.test.Test

/** W2: owner decisions on discomfort (easier, then normal), the lowest set as next target, and the top of a route. */
class OwnerProgressionDecisionsTest {
    private val c = Fx.catalog()
    private val engine = ProgressionEngine(c)

    private fun ev(day: Int, vid: String, tier: Int?, discomfort: Boolean = false, shortfall: Int? = null, reduced: Boolean = false, below: Boolean = shortfall != null) =
        SessionEvidence("s$day$vid", vid, c.variation(vid)!!.familyId, day, tier, 2, false, below, discomfort, if (discomfort) day else null, false, 0, 2, 2,
            shortfallValue = shortfall, reducedPrescription = reduced)

    private fun plan(progress: ProgressSnapshot, cat: Catalog = c, slotVid: String, day: Int = 12, nowRounds: Int = 1): WorkoutPlan {
        val r = Routine("t", 1, "T", listOf(RoutineSlot("push", Pattern.PUSH_HORIZONTAL, Area.UPPER_BODY, slotVid)))
        val d = SessionDraft(600, false, emptySet(), false, false, setOf(StrengthFocus.FULL_BODY), SeedProfiles.home.id, rounds = nowRounds)
        return (generate(PlanInput("plan", 0, day, cat, r, d, SeedProfiles.home, progress = progress)) as PlanResult.Ready).plan
    }
    private fun work(p: WorkoutPlan) = p.blocks.first { it.type == BlockType.WORK }

    // ---- 1. discomfort -> easier, then normal ----

    @Test fun discomfortStepsTheNextPlanOneLevelEasierWithoutAcceptingAnything() {
        val s = engine.replay(listOf(ev(0, "pushup-std", 3), ev(3, "pushup-std", 3, discomfort = true)), listOf(UserAction.SelfAssessment(0, "pushup-std", 3)))
        assertEquals(2, s.tierFor("pushup-std")); assertNotNull(s.holdFor("pushup"))
        val b = work(plan(s, slotVid = "pushup-std"))
        assertEquals("pushup-std", b.variationId); assertEquals(2, b.prescriptionTier); assertEquals(10, b.target!!.value)
    }

    @Test fun aCleanSessionOnTheEasierPrescriptionClearsTheHoldAndNormalProgressionContinuesFromThere() {
        val act = listOf(UserAction.SelfAssessment(0, "pushup-std", 3))
        val base = listOf(ev(0, "pushup-std", 3), ev(3, "pushup-std", 3, discomfort = true))
        val clean = engine.replay(base + ev(5, "pushup-std", 2), act)
        assertNull(clean.holdFor("pushup")); assertEquals(2, clean.tierFor("pushup-std"))
        assertEquals(listOf(5), clean.variations.getValue("pushup-std").streakDays)   // that clean session is success 1 at the easier level
        val later = engine.replay(base + listOf(ev(5, "pushup-std", 2), ev(7, "pushup-std", 2), ev(9, "pushup-std", 2)), act)
        assertEquals(3, later.tierFor("pushup-std")); assertTrue(2 in later.variations.getValue("pushup-std").achievedTiers)
        // another discomfort session while the easier one is not done keeps it held (a harder session does not clear it)
        assertNotNull(engine.replay(base + ev(5, "pushup-std", 3), act).holdFor("pushup"))
        // the session on the same day as the report does not count as "the next workout"
        assertNotNull(engine.replay(base + ev(3, "pushup-std", 2), act).holdFor("pushup"))
    }

    @Test fun repeatedDiscomfortStepsDownAgainAndNeverBelowTheLowestLevel() {
        val act = listOf(UserAction.SelfAssessment(0, "pushup-std", 3))
        val s = engine.replay(listOf(ev(0, "pushup-std", 3, discomfort = true), ev(3, "pushup-std", 2, discomfort = true)), act)
        assertEquals(1, s.tierFor("pushup-std")); assertEquals(2, s.holdFor("pushup")!!.steps)
        // at level 1 there is still a lower rung: the previous exercise at its top level
        val third = engine.replay(listOf(ev(0, "pushup-std", 3, discomfort = true), ev(3, "pushup-std", 2, discomfort = true), ev(6, "pushup-std", 1, discomfort = true)), act)
        assertEquals("pushup-knee", third.holdFor("pushup")!!.easierVariationId); assertEquals(5, third.tierFor("pushup-knee"))
        val b = work(plan(third, slotVid = "pushup-std"))
        assertEquals("pushup-knee", b.variationId); assertEquals(25, b.target!!.value)
    }

    @Test fun easierRungThenCleanSessionMovesTheRoutePositionDownAndProgressionResumes() {
        val act = listOf(UserAction.SelfAssessment(0, "pushup-std", 1))
        val held = engine.replay(listOf(ev(0, "pushup-std", 1, discomfort = true)), act)
        assertEquals("pushup-knee", held.holdFor("pushup")!!.easierVariationId)
        val resumed = engine.replay(listOf(ev(0, "pushup-std", 1, discomfort = true), ev(2, "pushup-knee", 5)), act)
        assertNull(resumed.holdFor("pushup")); assertEquals("pushup-knee", resumed.activeInFamily["pushup"])
        // three clean sessions at the top of the easier rung move on to the harder one again (normal level-up rules)
        val back = engine.replay(listOf(ev(0, "pushup-std", 1, discomfort = true), ev(2, "pushup-knee", 5), ev(4, "pushup-knee", 5), ev(6, "pushup-knee", 5)), act)
        assertEquals("pushup-std", back.activeInFamily["pushup"])
    }

    @Test fun theSlotIsNeverDroppedBecauseOfAHold() {
        val s = engine.replay(listOf(ev(0, "pushup-knee", 1, discomfort = true)), listOf(UserAction.SelfAssessment(0, "pushup-knee", 1)))
        val b = work(plan(s, slotVid = "pushup-knee"))
        assertEquals("pushup-knee", b.variationId); assertEquals(5, b.target!!.value)
    }

    // ---- 2. lowest set becomes the next target ----

    @Test fun deriveEvidenceFindsTheLowestSetOnlyWhenASetFellShort() {
        fun derive(values: List<Int>): SessionEvidence {
            val pol = c.policyForVariation("pushup-std")!!
            val p = WorkoutPlan("p", "usual", 1, 1, 0, "home", 180, 180, setOf(StrengthFocus.FULL_BODY), false, null, 3, emptyList(), emptyList(), false, false, emptyList(),
                progressionEvidenceVersion = 1, progressionPolicies = mapOf("pushup-std" to pol))
            val blocks = values.indices.map { TimelineBlock("w$it", BlockType.WORK, 60, it + 1, "push", "pushup-std", target = pol.tier(2).target, prescriptionTier = 2) }
            val plan = p.copy(blocks = blocks)
            val fb = blocks.mapIndexed { i, b -> b.id to Feedback(actualReps = values[i]) }.toMap()
            val out = resolveFeedback(plan, blocks.associate { it.id to Execution.COMPLETED }, fb)
            return deriveEvidence("s", 1, plan, out, { "pushup" }, { _, _ -> 2 }, loggedBlockIds = blocks.map { it.id }.toSet(), effectivePolicyOf = { pol }).single()
        }
        assertEquals(7, derive(listOf(10, 10, 7)).shortfallValue)
        assertEquals(8, derive(listOf(9, 10, 8)).shortfallValue)
        assertNull(derive(listOf(10, 12, 11)).shortfallValue)
    }

    @Test fun theLowestLoggedValueIsTheNextTargetThenItBuildsBackUp() {
        val act = listOf(UserAction.SelfAssessment(0, "pushup-std", 2))
        val short = engine.replay(listOf(ev(0, "pushup-std", 2, shortfall = 8)), act)
        val p = short.variations.getValue("pushup-std")
        assertEquals(8, p.reducedTarget); assertEquals(2, p.reducedTargetTier); assertEquals(2, short.tierFor("pushup-std"))
        assertEquals(8, work(plan(short, slotVid = "pushup-std")).target!!.value)
        // doing the lowered target builds it back up by one, without counting towards the level
        val up = engine.replay(listOf(ev(0, "pushup-std", 2, shortfall = 8), ev(2, "pushup-std", 2, reduced = true)), act)
        assertEquals(9, up.variations.getValue("pushup-std").reducedTarget); assertTrue(up.variations.getValue("pushup-std").streakDays.isEmpty())
        assertEquals(9, work(plan(up, slotVid = "pushup-std")).target!!.value)
        // back at the level's own target: normal again, and the next full-target sessions count
        val full = engine.replay(listOf(ev(0, "pushup-std", 2, shortfall = 8), ev(2, "pushup-std", 2, reduced = true), ev(4, "pushup-std", 2, reduced = true)), act)
        assertNull(full.variations.getValue("pushup-std").reducedTarget)
        assertEquals(10, work(plan(full, slotVid = "pushup-std")).target!!.value)
        assertEquals(3, engine.replay(listOf(ev(0, "pushup-std", 2, shortfall = 8), ev(2, "pushup-std", 2, reduced = true), ev(4, "pushup-std", 2, reduced = true), ev(6, "pushup-std", 2), ev(8, "pushup-std", 2), ev(10, "pushup-std", 2)), act).tierFor("pushup-std"))
    }

    @Test fun holdsMirrorTheSameRuleInSeconds() {
        val hc = Catalog(1, listOf(Fx.strength("plank", "plank", Pattern.CORE_ANTI_EXTENSION, Area.CORE).copy(kind = Kind.HOLD), Fx.stretch), listOf(Fx.policy("plank", type = TargetType.HOLD_SECONDS)))
        val e = SessionEvidence("s", "plank", "plank", 0, 2, 2, false, true, false, null, false, 0, 2, 2, shortfallValue = 7)
        val s = ProgressionEngine(hc).replay(listOf(e), listOf(UserAction.SelfAssessment(0, "plank", 2)))
        assertEquals(7, s.variations.getValue("plank").reducedTarget)
        val up = ProgressionEngine(hc).replay(listOf(e, e.copy(sessionId = "s2", day = 2, shortfallValue = null, anyBelow = false, reducedPrescription = true)), listOf(UserAction.SelfAssessment(0, "plank", 2)))
        // 7 + 5 reaches the level's own 10 s: back to normal
        assertNull(up.variations.getValue("plank").reducedTarget)
    }

    @Test fun levelJumpsUpStayAllowedAndClearTheLoweredTarget() {
        val act = listOf(UserAction.SelfAssessment(0, "pushup-std", 2))
        val s = engine.replay(listOf(ev(0, "pushup-std", 2, shortfall = 8), ev(2, "pushup-std", 2, reduced = true).copy(demonstratedTier = 3)), act)
        assertNull(s.variations.getValue("pushup-std").reducedTarget)
    }

    // ---- 3. the top of a route ----

    private val top = Fx.strength("main", "pushup")
    private val alt = Fx.strength("alternate", "pushup")
    private val tc = Catalog(1, listOf(top, alt, Fx.stretch), listOf(Fx.policy("main"), Fx.policy("alternate")), exerciseGroups = listOf(ExerciseGroup("g", "main", listOf("alternate"))))

    private fun topEv(day: Int) = SessionEvidence("t$day", "main", "pushup", day, 5, 2, false, false, false, null, false, 0, 2, 2)

    @Test fun atTheTopOfARouteTheLevelFiveTargetKeepsRisingAfterEachStreak() {
        val e = ProgressionEngine(tc); val act = listOf(UserAction.SelfAssessment(0, "main", 6))
        assertNull(e.replay(listOf(topEv(1), topEv(2)), act).variations.getValue("main").topTarget)
        val first = e.replay(listOf(topEv(1), topEv(2), topEv(3)), act)
        assertEquals(26, first.variations.getValue("main").topTarget)
        val second = e.replay((1..6).map { topEv(it) }, act)
        assertEquals(27, second.variations.getValue("main").topTarget)
        val b = work(plan(second, tc, "main"))
        assertEquals(27, b.target!!.value); assertTrue(b.levelFiveEligible)
    }

    @Test fun holdsRiseByFiveSecondsAtTheTop() {
        val hc = Catalog(1, listOf(Fx.strength("plank", "plank", Pattern.CORE_ANTI_EXTENSION, Area.CORE).copy(kind = Kind.HOLD), Fx.stretch), listOf(Fx.policy("plank", type = TargetType.HOLD_SECONDS)))
        val evs = (1..3).map { SessionEvidence("t$it", "plank", "plank", it, 5, 2, false, false, false, null, false, 0, 2, 2) }
        val s = ProgressionEngine(hc).replay(evs, listOf(UserAction.SelfAssessment(0, "plank", 6)))
        assertEquals(30, s.variations.getValue("plank").topTarget)
    }

    @Test fun whereASuccessorExistsTheExerciseStillMovesOn() {
        val s = engine.replay((1..3).map { SessionEvidence("t$it", "pushup-knee", "pushup", it, 5, 2, false, false, false, null, false, 0, 2, 2) }, listOf(UserAction.SelfAssessment(0, "pushup-knee", 5)))
        assertEquals("pushup-std", s.activeInFamily["pushup"]); assertNull(s.variations.getValue("pushup-knee").topTarget)
    }

    @Test fun theTopExerciseAlternatesWithItsSameRungVariationAcrossWorkouts() {
        val s = ProgressionEngine(tc).replay((1..3).map { topEv(it) }, listOf(UserAction.SelfAssessment(0, "main", 6)))
        val picks = (0..3).map { n -> work(plan(s.copy(familyWorkoutCounts = mapOf("pushup" to n)), tc, "main")).variationId }
        assertEquals(listOf("main", "alternate", "main", "alternate"), picks)
        // the main exercise keeps its raised target on its turns
        assertEquals(26, work(plan(s.copy(familyWorkoutCounts = mapOf("pushup" to 2)), tc, "main")).target!!.value)
    }
}

/** W2: hand edits and questionnaire answers are dated self-assessments: they win from their day on. */
class DatedLevelEditTest {
    private val c = Fx.catalog()
    private val engine = ProgressionEngine(c)
    private fun ev(day: Int, tier: Int, below: Boolean = false) =
        SessionEvidence("s$day", "pushup-std", "pushup", day, tier, 2, false, below, false, null, false, 0, 2, 2)

    @Test fun loweringByHandAfterHistoryStaysLoweredAndProgressesNormallyFromThere() {
        val history = listOf(ev(1, 3), ev(3, 3), ev(5, 3))   // earned level 3, now at 4
        val start = UserAction.SelfAssessment(0, "pushup-std", 3)
        assertEquals(4, engine.replay(history, listOf(start)).tierFor("pushup-std"))
        val lowered = engine.replay(history, listOf(start, UserAction.SelfAssessment(10, "pushup-std", 1)))
        val p = lowered.variations.getValue("pushup-std")
        assertEquals(1, p.tier); assertTrue(p.achievedTiers.isEmpty()); assertEquals(0, p.earnedStars() - 0)
        val later = engine.replay(history + listOf(ev(11, 1), ev(13, 1), ev(15, 1)), listOf(start, UserAction.SelfAssessment(10, "pushup-std", 1)))
        assertEquals(2, later.tierFor("pushup-std"))
    }

    @Test fun raisingByHandLeavesNoStaleLowerTargetSuggestion() {
        val history = listOf(ev(1, 3, below = true), ev(3, 3, below = true))
        val start = UserAction.SelfAssessment(0, "pushup-std", 3)
        assertTrue(engine.replay(history, listOf(start)).variations.getValue("pushup-std").lowerTargetSuggested)
        val raised = engine.replay(history, listOf(start, UserAction.SelfAssessment(10, "pushup-std", 4)))
        assertEquals(4, raised.tierFor("pushup-std")); assertFalse(raised.variations.getValue("pushup-std").lowerTargetSuggested)
    }
}
