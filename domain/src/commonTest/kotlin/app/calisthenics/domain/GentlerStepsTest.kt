package app.calisthenics.domain

import app.calisthenics.domain.feedback.*
import app.calisthenics.domain.history.ratingFor
import app.calisthenics.domain.model.*
import app.calisthenics.domain.model.ProgressionPolicy
import app.calisthenics.domain.model.Target
import app.calisthenics.domain.model.Tier
import app.calisthenics.domain.planner.*
import app.calisthenics.domain.progression.*
import app.calisthenics.domain.session.*
import kotlinx.serialization.json.Json
import kotlin.test.Test

/** L4 (release 9.3): inside a level the prescribed target climbs towards the next level's; what counts as a good session never moves. */
class GentlerStepsTest {
    private fun pol(vid: String, vararg values: Int, type: TargetType = TargetType.REPS, cap: Int? = null) = ProgressionPolicy(
        id = "pol-$vid", variationId = vid, version = 1, maxRepsPerSet = cap,
        tiers = values.mapIndexed { i, v -> Tier(i + 1, Target(type, v), workWindowSeconds = if (type == TargetType.HOLD_SECONDS) 120 else 40, minRecoverySeconds = 20) })

    private val pullup = pol("pullup", 1, 3, 5, 8, 10)
    private val calf = pol("calf", 20, 25, 30, 35, 40)
    private val flat = pol("flat", 5, 6, 7, 8, 10)
    private val pushup = pol("pushup", 4, 6, 8, 10, 12)
    private val hold = pol("hold", 10, 20, 30, 40, 50, type = TargetType.HOLD_SECONDS)

    private fun value(p: ProgressionPolicy, tier: Int, days: List<Int>) = prescribedTarget(p, tier, days)!!.target.value

    // ---- the formula -----------------------------------------------------------------------------------------------------

    @Test fun pullUpLevelThreeClimbsFiveSixSevenThenEight() {
        assertEquals(listOf(5, 6, 7), listOf(emptyList(), listOf(1), listOf(1, 3)).map { value(pullup, 3, it) })
        assertEquals(8, value(pullup, 4, emptyList()))
    }

    @Test fun pullUpLevelOneClimbsOneOneTwoThenThree() {
        assertEquals(listOf(1, 1, 2), listOf(emptyList(), listOf(1), listOf(1, 3)).map { value(pullup, 1, it) })
        assertEquals(3, value(pullup, 2, emptyList()))
    }

    @Test fun calfRaiseLevelOneClimbsTwentyTwentyOneTwentyThreeThenTwentyFive() {
        assertEquals(listOf(20, 21, 23), listOf(emptyList(), listOf(1), listOf(1, 3)).map { value(calf, 1, it) })
        assertEquals(25, value(calf, 2, emptyList()))
    }

    @Test fun aGapOfOneOrNoNextLevelDoesNotRamp() {
        assertEquals(listOf(5, 5, 5), listOf(emptyList(), listOf(1), listOf(1, 3)).map { value(flat, 1, it) })
        assertEquals(listOf(10, 10, 10), listOf(emptyList(), listOf(1), listOf(1, 3)).map { value(pullup, 5, it) })   // level 5 has no next level
        assertFalse(prescribedTarget(flat, 1, listOf(1, 3))!!.ramped)
        assertNull(prescribedTarget(pullup, 6, emptyList()))
    }

    @Test fun neverReachesTheNextLevelsTargetEvenWithMoreGoodDaysThanTheGateNeeds() {
        // days that are spread too far apart to level up (no 3 in 14 days, no 4 in 30) still never push the number to the next level
        val spread = listOf(1, 20, 40, 60, 80)
        for (p in listOf(pullup, calf, pushup)) for (tier in 1..4) {
            val next = p.tiers[tier].target.value
            assertTrue(value(p, tier, spread) < next)
            assertTrue(value(p, tier, spread) >= p.tiers[tier - 1].target.value)
        }
    }

    @Test fun weeklyTrainersClimbOnTheFourInThirtyDaysPathAndTheLastStepStaysBelowTheNextLevel() {
        // one session a week: after 1 and 2 sessions the closer path is still the fast one; at the third the weekly path (3 of 4) is closer
        assertEquals(listOf(20, 21, 23, 23), listOf(emptyList(), listOf(0), listOf(0, 7), listOf(0, 7, 14)).map { value(calf, 1, it) })
        assertEquals(LevelProgress(3, 4, true, 2), levelProgress(listOf(0, 7, 14), 1))
        assertEquals(23, value(calf, 1, listOf(0, 7, 14)))      // floor(3 x 5 / 4) = 3, not 5
        assertEquals(7, value(pullup, 3, listOf(0, 7, 14)))     // floor(3 x 3 / 4) = 2
    }

    @Test fun holdsClimbInFiveSecondSteps() {
        // gap 10: 0 / 3 / 6 seconds before rounding down to a multiple of 5
        assertEquals(listOf(10, 10, 15), listOf(emptyList(), listOf(1), listOf(1, 3)).map { value(hold, 1, it) })
        // a gap of 5 s (or less) has no room for a 5 s step below the next level
        val small = pol("small", 20, 25, 30, 35, 40, type = TargetType.HOLD_SECONDS)
        assertEquals(listOf(20, 20, 20), listOf(emptyList(), listOf(1), listOf(1, 3)).map { value(small, 1, it) })
        val wide = pol("wide", 15, 30, 40, 50, 60, type = TargetType.HOLD_SECONDS)   // gap 15: 0, 5, 10 (never 14)
        assertEquals(listOf(15, 20, 25), listOf(emptyList(), listOf(1), listOf(1, 3)).map { value(wide, 1, it) })
        assertEquals(TargetType.HOLD_SECONDS, prescribedTarget(wide, 1, listOf(1, 3))!!.target.type)
    }

    @Test fun theRepCeilingStillCapsTheRampedNumber() {
        val capped = pol("capped", 4, 6, 8, 10, 12, cap = 7)
        assertTrue(listOf(emptyList(), listOf(1), listOf(1, 3)).all { value(capped, 2, it) <= 7 })
        assertEquals(7, value(capped, 3, listOf(1, 3)))   // level 3 is itself capped to 7, so there is no gap left
    }

    // ---- through the planner ---------------------------------------------------------------------------------------------

    private val profile = EquipmentProfile("home", "Home", emptyList(), emptySet())
    private val settings = TrainSettings(rounds = 3, exercisesPerRound = 1, betweenSetMode = BetweenSetMode.FLOW)

    private inner class World(val vids: List<String>, val policies: List<ProgressionPolicy>, val start: Map<String, Int>) {
        val catalog = Fx.catalog().let { base ->
            val extra = mapOf("pullup" to (Pattern.PULL_VERTICAL to Area.UPPER_BODY), "calf" to (Pattern.CALF_RAISE to Area.LOWER_BODY), "pushup" to (Pattern.PUSH_HORIZONTAL to Area.UPPER_BODY),
                "flat" to (Pattern.SQUAT to Area.LOWER_BODY))
            val vs = vids.map { Fx.strength(it, it, extra.getValue(it).first, extra.getValue(it).second) }
            Catalog(1, vs + Fx.stretch, policies)
        }
        val engine = ProgressionEngine(catalog)
        val actions = start.map { (v, t) -> UserAction.SelfAssessment(0, v, t) }
        val evidence = mutableListOf<SessionEvidence>()
        private val routine = Routine("usual", 1, "Mixed", vids.map { RoutineSlot("slot-$it", catalog.variation(it)!!.patterns.first(), catalog.variation(it)!!.areas.first(), it) })
        fun progress() = engine.replay(evidence, actions)

        fun plan(day: Int, ramp: Boolean = true): WorkoutPlan {
            val p = (buildTrainPlan(catalog, routine, progress(), profile, settings.copy(exercisesPerRound = vids.size), nowEpochMs = day * 86_400_000L, nowDay = day) as PlanResult.Ready).plan
            // "without the ramp": the same plan as before this release (the level's own target, no judged value)
            return if (ramp) p else p.copy(blocks = p.blocks.map { if (it.levelTarget != null) it.copy(target = it.target!!.copy(value = it.levelTarget!!), levelTarget = null) else it })
        }
        fun targetOf(plan: WorkoutPlan, vid: String) = plan.blocks.first { it.type == BlockType.WORK && it.variationId == vid }.target!!.value

        /** Does the session of [plan]: each work block is logged with [reps]. Returns the evidence of every exercise of it. */
        fun train(day: Int, plan: WorkoutPlan, reps: (TimelineBlock) -> Int?): List<SessionEvidence> {
            val work = plan.blocks.filter { it.type == BlockType.WORK }
            val exec = work.associate { it.id to Execution.COMPLETED }
            val logged = work.mapNotNull { b -> reps(b)?.let { b.id to Feedback(actualReps = it) } }.toMap()
            val ev = deriveEvidence("s$day", day, plan, resolveFeedback(plan, exec, logged), { catalog.variation(it)!!.familyId },
                { id, tier -> catalog.policyForVariation(id)!!.tier(tier ?: 1).minQualifyingBlocks }, loggedBlockIds = logged.keys, effectivePolicyOf = catalog::policyForVariation)
            evidence += ev
            return ev
        }
    }

    private fun single(p: ProgressionPolicy, tier: Int) = World(listOf(p.variationId), listOf(p), mapOf(p.variationId to tier))

    @Test fun thePlannerPrescribesTheClimbingTargetSessionAfterSessionUntilTheLevelUp() {
        val w = single(pullup, 3)
        val seen = mutableListOf<Int>()
        for (day in listOf(1, 3, 5, 7)) {
            val plan = w.plan(day)
            seen += w.targetOf(plan, "pullup")
            w.train(day, plan) { it.target!!.value }       // does exactly what is shown
        }
        assertEquals(listOf(5, 6, 7, 8), seen)
        assertEquals(4, w.progress().tierFor("pullup"))
    }

    @Test fun calfRaiseAndAGapOfOneThroughThePlanner() {
        val c = single(calf, 1)
        val seen = mutableListOf<Int>()
        for (day in listOf(1, 3, 5, 7)) { val plan = c.plan(day); seen += c.targetOf(plan, "calf"); c.train(day, plan) { it.target!!.value } }
        assertEquals(listOf(20, 21, 23, 25), seen)
        val f = single(flat, 1)
        val flatSeen = mutableListOf<Int>()
        for (day in listOf(1, 3, 5, 7)) { val plan = f.plan(day); flatSeen += f.targetOf(plan, "flat"); f.train(day, plan) { it.target!!.value } }
        assertEquals(listOf(5, 5, 5, 6), flatSeen)
        assertTrue(f.plan(9).blocks.none { it.levelTarget != null })
    }

    @Test fun doingTheLevelTargetButNotTheClimbingOneIsAGoodSessionWithNoLevelKept() {
        val w = single(pullup, 3)
        val before = mutableListOf<ProgressSnapshot>()
        for (day in listOf(1, 3, 5)) {
            val plan = w.plan(day)
            val shown = w.targetOf(plan, "pullup")
            val level = plan.blocks.first { it.type == BlockType.WORK }.judgedValue!!
            assertEquals(5, level)
            if (day > 1) assertTrue(shown > level)                      // 6, then 7: more than the person does
            before += w.progress()
            val ev = w.train(day, plan) { level }.single()
            assertFalse(ev.anyBelow); assertTrue(ev.qualifying)         // below the shown number, but not "below target"
            assertNull(ev.shortfallValue); assertFalse(ev.reducedPrescription)
            val after = w.progress()
            assertEquals(0, after.variations.getValue("pullup").consecutiveBelow)
            if (day < 5) {
                val s = summarizeSessionProgress(w.catalog, before.last(), after, listOf("pullup")).exercises.single()
                assertFalse("no Level kept chip on day $day", s.levelKept)
                assertEquals(day / 2 + 1, s.progress!!.have)           // 1, then 2 of 3 good sessions
            }
        }
        assertEquals(4, w.progress().tierFor("pullup"))                 // the gate is unchanged: three good sessions
    }

    @Test fun missingTheLevelTargetIsStillBelowAndItsLowestSetBecomesTheNextTarget() {
        val w = single(pullup, 3)
        for (day in listOf(1, 3)) { val plan = w.plan(day); w.train(day, plan) { it.target!!.value } }     // 5, 6 done: 2 good sessions
        val plan = w.plan(5)
        assertEquals(7, w.targetOf(plan, "pullup"))
        val ev = w.train(5, plan) { 4 }.single()                       // under the level's own 5
        assertTrue(ev.anyBelow); assertEquals(4, ev.shortfallValue)
        val p = w.progress().variations.getValue("pullup")
        assertEquals(3, p.tier); assertEquals(2, p.streakDays.size)     // the shortfall is not a good session and takes nothing away
        assertEquals(4, p.reducedTarget)
        // the lowered target keeps priority over the climbing one
        val next = w.plan(7)
        assertEquals(4, w.targetOf(next, "pullup"))
        assertNull(next.blocks.first { it.type == BlockType.WORK }.levelTarget)
        val after = w.train(7, next) { it.target!!.value }.single()
        assertFalse(after.qualifying)                                   // a lowered session is done and kept, not a good one
        assertEquals(7, w.targetOf(w.plan(9), "pullup"))   // the lowered target built back up to the level's own; the climb resumes from the 2 good sessions
    }

    @Test fun theTopLevelTargetAndTheSelfRaisedLevelFiveTargetAreNotRamped() {
        val w = single(pullup, 5)
        for (day in listOf(1, 3)) { val plan = w.plan(day); assertEquals(10, w.targetOf(plan, "pullup")); assertNull(plan.blocks.first { it.type == BlockType.WORK }.levelTarget); w.train(day, plan) { 10 } }
    }

    @Test fun typedRepsThatMeetTheNextLevelStillAdoptItAndNothingAboutThatChanges() {
        val w = single(pullup, 3)
        val plan = w.plan(1)
        w.train(1, plan) { 8 }                                          // typed: the level 4 target
        val p = w.progress().variations.getValue("pullup")
        assertEquals(4, p.tier)
        assertEquals(listOf(1), p.streakDays)                           // unchanged adoption rule
        assertEquals(8, w.targetOf(w.plan(3), "pullup"))                // 1 of 3 at gap 2: floor(2/3) = 0
    }

    @Test fun aFreelyTypedOrOverriddenTargetIsNeverRamped() {
        val w = single(pullup, 3)
        for (day in listOf(1, 3)) { val plan = w.plan(day); w.train(day, plan) { it.target!!.value } }
        val routine = Routine("usual", 1, "Pull", listOf(RoutineSlot("slot-pullup", Pattern.PULL_VERTICAL, Area.UPPER_BODY, "pullup")))
        val free = (buildTrainPlan(w.catalog, routine, w.progress(), profile, settings, nowDay = 5, edits = PlanEdits(freeTargets = mapOf("slot-pullup" to 4))) as PlanResult.Ready).plan
        assertTrue(free.blocks.filter { it.type == BlockType.WORK }.all { it.target!!.value == 4 && it.levelTarget == null })
        val other = (buildTrainPlan(w.catalog, routine, w.progress(), profile, settings, nowDay = 5, edits = PlanEdits(tierOverrides = mapOf("slot-pullup" to 4))) as PlanResult.Ready).plan
        assertTrue(other.blocks.filter { it.type == BlockType.WORK }.all { it.target!!.value == 8 && it.levelTarget == null })
    }

    // ---- every place that judges a set uses the level's own target ---------------------------------------------------------

    private val rampedBlock = TimelineBlock("b", BlockType.WORK, 40, 1, "s", "pullup", target = Target(TargetType.REPS, 7), prescriptionTier = 3, levelTarget = 5)

    @Test fun theLoggerTheEditorsAndTheHistoryJudgeAgainstTheLevelTarget() {
        val plan = WorkoutPlan("p", "usual", 1, 1, 0, "home", 600, 180, setOf(StrengthFocus.FULL_BODY), true, null, 3, emptyList(), emptyList(), false, false, listOf(rampedBlock))
        assertEquals(5, rampedBlock.judgedValue)
        assertEquals(Rating.MET, rowLogsFrom(plan, mapOf("b" to BlockLog(reps = 5))).single().rating)
        assertEquals(Rating.BELOW, rowLogsFrom(plan, mapOf("b" to BlockLog(reps = 4))).single().rating)
        assertEquals(Rating.MET, ratingFor(5, rampedBlock.target, false, judged = rampedBlock.judgedValue))
        assertEquals(Rating.BELOW, ratingFor(4, rampedBlock.target, false, judged = rampedBlock.judgedValue))
        assertEquals(Rating.BELOW, ratingFor(5, rampedBlock.target, false))        // without the judged value the old rule: shown target
        val met = resolveFeedback(plan, mapOf("b" to Execution.COMPLETED), mapOf("b" to Feedback(actualReps = 5))).single()
        assertEquals(Rating.MET, met.rating); assertNull(met.conflict)
        val below = resolveFeedback(plan, mapOf("b" to Execution.COMPLETED), mapOf("b" to Feedback(actualReps = 4))).single()
        assertEquals(Rating.BELOW, below.rating)
        val round = app.calisthenics.domain.feedback.effectiveRounds(plan, "pullup", mapOf("b" to 5), emptyList()).single()
        assertEquals(Rating.MET, round.rating)
        assertEquals(Rating.BELOW, app.calisthenics.domain.feedback.effectiveRounds(plan, "pullup", mapOf("b" to 4), emptyList()).single().rating)
    }

    @Test fun anUnramptedBlockIsJudgedAgainstItsOwnTargetAsBefore() {
        val plain = rampedBlock.copy(levelTarget = null)
        assertEquals(7, plain.judgedValue)
        assertEquals(Rating.BELOW, ratingFor(5, plain.target, false, judged = plain.judgedValue))
    }

    @Test fun theLevelTargetIsOnlyWrittenWhenThereIsARamp() {
        val json = Json { encodeDefaults = true }
        val plain = json.encodeToString(TimelineBlock.serializer(), rampedBlock.copy(levelTarget = null))
        assertFalse(plain.contains("levelTarget"))                                    // plans without a ramp serialize exactly as before
        val ramped = json.encodeToString(TimelineBlock.serializer(), rampedBlock)
        assertTrue(ramped.contains("\"levelTarget\":5"))
        assertEquals(rampedBlock, json.decodeFromString(TimelineBlock.serializer(), ramped))
        assertNull(Json { ignoreUnknownKeys = false }.decodeFromString(TimelineBlock.serializer(), plain).levelTarget)
    }

    // ---- replay -------------------------------------------------------------------------------------------------------------

    /**
     * Fourteen weeks of a realistic routine (pull-up from level 1 to 4, calf raise, a push-up), irregular days, a shortfall, a set at the ramped number,
     * one weekly stretch. The same person does the same level-relative things with and without the ramp: the evidence, and so every level, star and
     * good-session count, must be identical; only the prescription differs.
     */
    @Test fun replayingTheSameHistoryWithAndWithoutTheRampGivesTheSameLevelsStarsAndGoodSessions() {
        val vids = listOf("pullup", "calf", "pushup")
        val pols = listOf(pullup, calf, pushup)
        val start = mapOf("pullup" to 1, "calf" to 1, "pushup" to 2)
        val days = listOf(1, 3, 6, 8, 11, 13, 16, 18, 20, 27, 34, 41, 42, 44, 47, 49, 52, 55, 56, 59, 62, 63, 66, 70, 73, 77, 80, 84, 88, 91, 95, 98)
        // what the person does relative to the level's own target (judged), by session index and exercise; null = just what is shown
        fun behaviour(i: Int, vid: String, judged: Int, shown: Int): Int = when {
            vid == "pullup" && i == 9 -> judged - 1          // a shortfall
            vid == "pushup" && i == 14 -> judged - 2         // another one
            i % 5 == 0 -> shown                              // sometimes does exactly the shown (ramped) number
            else -> judged                                   // otherwise just the level's own target
        }
        val with = World(vids, pols, start); val without = World(vids, pols, start)
        var rampedSeen = false
        for ((i, day) in days.withIndex()) {
            val a = with.plan(day, ramp = true); val b = without.plan(day, ramp = false)
            for (vid in vids) {
                val ta = with.targetOf(a, vid); val tb = without.targetOf(b, vid)
                assertTrue("$vid day $day: ramped target below the plain one ($ta < $tb)", ta >= tb)
                if (ta > tb) rampedSeen = true
                val level = pols.first { it.variationId == vid }
                val next = level.tiers.firstOrNull { it.index == with.progress().tierFor(vid)!! + 1 }?.target?.value
                if (next != null) assertTrue("$vid day $day: $ta reaches the next level ($next)", ta < next)
            }
            with.train(day, a) { blk -> behaviour(i, blk.variationId!!, blk.judgedValue!!, blk.target!!.value) }
            without.train(day, b) { blk -> behaviour(i, blk.variationId!!, blk.judgedValue!!, blk.target!!.value) }
            assertEquals("evidence differs on day $day", without.evidence.toList(), with.evidence.toList())
        }
        assertTrue(rampedSeen)
        val pa = with.progress(); val pb = without.progress()
        for (vid in vids) {
            val x = pa.variations.getValue(vid); val y = pb.variations.getValue(vid)
            assertEquals(y.tier, x.tier); assertEquals(y.earnedStars(), x.earnedStars()); assertEquals(y.achievedTiers, x.achievedTiers)
            assertEquals(with.engine.qualifyingDays(pb, vid), with.engine.qualifyingDays(pa, vid))
            assertEquals(y.streakDays, x.streakDays); assertEquals(y.reducedTarget, x.reducedTarget)
            assertEquals(y.consecutiveBelow, x.consecutiveBelow)
        }
        assertEquals(pb, pa)
        // the realistic ladder really was climbed: the pull-up went through levels 1 to 4 at least
        assertTrue(pa.variations.getValue("pullup").tier >= 4)
    }

    @Test fun historyStoredBeforeTheRampReplaysTheSameBecauseItsBlocksCarryNoLevelTarget() {
        // a plan as saved by 9.2 (no levelTarget key) judges against its own target exactly as before
        val w = single(pullup, 3)
        val old = w.plan(1, ramp = false)
        assertTrue(old.blocks.all { it.levelTarget == null })
        val ev = w.train(1, old) { it.target!!.value - 1 }.single()
        assertTrue(ev.anyBelow); assertEquals(4, ev.shortfallValue)
    }
}
