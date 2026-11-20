package app.calisthenics.domain

import app.calisthenics.domain.feedback.*
import app.calisthenics.domain.model.*
import app.calisthenics.domain.model.Target
import app.calisthenics.domain.session.*
import kotlin.test.Test

/** U07 (F10, F14): in-session logging of reps per round; untouched = assumed met; evidence uses the lowest round. */
class SessionLogTest {
    private fun work(round: Int, target: Int = 8, vid: String = "pushup-standard") =
        TimelineBlock("r$round-push-work", BlockType.WORK, 60, round, "push", vid, target = Target(TargetType.REPS, target), prescriptionTier = 3)
    private fun stretch(round: Int) = TimelineBlock("r$round-push-rec", BlockType.STRETCH, 30, round, "push", "stretch-chest-door", Side.BOTH)

    private val blocks = listOf(work(1), stretch(1), work(2), stretch(2), work(3))
    private val plan = WorkoutPlan("p", "usual", 1, 1, 0, "home", 600, blocks.sumOf { it.durationSeconds }, setOf(StrengthFocus.FULL_BODY), true,
        null, 3, emptyList(), emptyList(), false, false, blocks)

    /** Runs the session to the start of [block]; every earlier block completed. */
    private fun at(block: Int): SessionState {
        var s = reduce(newSession("S", plan), SessionEvent.Start(0)).state
        var now = 0L
        repeat(block) { now += plan.blocks[it].durationSeconds * 1000L; s = reduce(s, SessionEvent.Tick(now)).state }
        return s
    }

    @Test fun loggingStoresTheRepsForThatBlockAndPersistsAtOnce() {
        val s = at(1) // round 1 work is done, now in the stretch
        val r = reduce(s, SessionEvent.LogBlock("r1-push-work", BlockLog(reps = 6)))
        assertEquals(BlockLog(reps = 6), r.state.logged["r1-push-work"])
        assertTrue(r.effects.any { it is SessionEffect.Persist })
    }

    @Test fun eachRoundKeepsItsOwnValueAndTheLatestEntryForABlockWins() {
        var s = at(1)                                                       // round 1 done, in its stretch
        s = reduce(s, SessionEvent.LogBlock("r1-push-work", BlockLog(reps = 6))).state
        s = reduce(s, SessionEvent.LogBlock("r1-push-work", BlockLog(reps = 7))).state
        s = reduce(s, SessionEvent.Tick(90_000)).state                      // stretch over, round 2 running
        s = reduce(s, SessionEvent.LogBlock("r2-push-work", BlockLog(reps = 5))).state
        assertEquals(7, s.logged["r1-push-work"]?.reps)
        assertEquals(5, s.logged["r2-push-work"]?.reps)
        assertEquals(setOf("r1-push-work", "r2-push-work"), s.logged.keys)
    }

    @Test fun cannotLogABlockThatHasNotStartedOrIsNotWork() {
        val s = at(1)
        assertSame(s, reduce(s, SessionEvent.LogBlock("r3-push-work", BlockLog(reps = 9))).state) // future round
        assertSame(s, reduce(s, SessionEvent.LogBlock("r1-push-rec", BlockLog(reps = 9))).state)  // a stretch
        assertSame(s, reduce(s, SessionEvent.LogBlock("nope", BlockLog(reps = 9))).state)
    }

    @Test fun repsAreClampedAndAFinishedSessionIsImmutable() {
        val s = reduce(at(1), SessionEvent.LogBlock("r1-push-work", BlockLog(reps = 5000))).state
        assertEquals(999, s.logged["r1-push-work"]?.reps)
        val done = reduce(s, SessionEvent.FinishEarly(70_000)).state
        assertSame(done, reduce(done, SessionEvent.LogBlock("r1-push-work", BlockLog(reps = 1))).state)
    }

    @Test fun nothingLoggedMeansNoRowAndTheWorkIsAssumedMet() {
        assertTrue(rowLogsFrom(plan, emptyMap()).isEmpty())
        val outcomes = resolveFeedback(plan, plan.blocks.associate { it.id to Execution.COMPLETED })
        assertTrue(outcomes.all { it.rating == Rating.MET && it.origin == RatingOrigin.ASSUMED })
    }

    @Test fun theRowUsesTheLowestRoundAndBelowTargetIsBelow() {
        val rows = rowLogsFrom(plan, mapOf("r1-push-work" to BlockLog(reps = 8), "r2-push-work" to BlockLog(reps = 6), "r3-push-work" to BlockLog(reps = 8)))
        val row = rows.single()
        assertEquals(6, row.actualReps); assertEquals(Rating.BELOW, row.rating); assertFalse(row.discomfort)
        val ok = rowLogsFrom(plan, mapOf("r1-push-work" to BlockLog(reps = 8), "r2-push-work" to BlockLog(reps = 10))).single()
        assertEquals(8, ok.actualReps); assertEquals(Rating.MET, ok.rating)
    }

    @Test fun tooHardAndPainAreCarriedEvenWithoutTypedReps() {
        val row = rowLogsFrom(plan, mapOf("r2-push-work" to BlockLog(tooHard = true, discomfort = true))).single()
        assertEquals(Rating.BELOW, row.rating); assertTrue(row.discomfort); assertNull(row.actualReps)
    }

    @Test fun loggedLowRoundMakesTheSessionNonQualifyingForProgression() {
        val exec = plan.blocks.associate { it.id to Execution.COMPLETED }
        fun evidence(logged: Map<String, BlockLog>): SessionEvidence {
            val row = rowLogsFrom(plan, logged).associate { it.variationId to Feedback(rating = it.rating, actualReps = it.actualReps, discomfort = it.discomfort) }
            return deriveEvidence("S", 1, plan, resolveFeedback(plan, exec, emptyMap(), row), { it }, { _, _ -> 2 }).single()
        }
        assertTrue(evidence(emptyMap()).qualifying)                                            // untouched = assumed met
        assertTrue(evidence(mapOf("r1-push-work" to BlockLog(reps = 9))).qualifying)           // above target is fine
        assertFalse(evidence(mapOf("r1-push-work" to BlockLog(reps = 9), "r3-push-work" to BlockLog(reps = 5))).qualifying) // lowest round counts
    }

    @Test fun tooEasyGivesAboveUnlessSomethingIsBelow() {
        assertEquals(Rating.ABOVE, rowLogsFrom(plan, mapOf("r1-push-work" to BlockLog(reps = 8, tooEasy = true))).single().rating)
        assertEquals(Rating.ABOVE, rowLogsFrom(plan, mapOf("r1-push-work" to BlockLog(tooEasy = true))).single().rating)
        // a round below target (or "too hard") wins over "too easy" in another round
        assertEquals(Rating.BELOW, rowLogsFrom(plan, mapOf("r1-push-work" to BlockLog(tooEasy = true), "r2-push-work" to BlockLog(reps = 3))).single().rating)
        assertEquals(Rating.BELOW, rowLogsFrom(plan, mapOf("r1-push-work" to BlockLog(tooEasy = true, tooHard = true))).single().rating)
    }

    @Test fun ratingForHistoryCorrections() {
        val t = Target(TargetType.REPS, 8)
        assertEquals(Rating.ABOVE, app.calisthenics.domain.history.ratingFor(8, t, tooHard = false, tooEasy = true))
        assertEquals(Rating.BELOW, app.calisthenics.domain.history.ratingFor(6, t, tooHard = false, tooEasy = true))
        assertEquals(Rating.MET, app.calisthenics.domain.history.ratingFor(8, t, false))
    }

    @Test fun oldCheckpointLogsWithoutTooEasyStillDecode() {
        val j = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }
        val old = j.decodeFromString(BlockLog.serializer(), """{"reps":5,"tooHard":true,"discomfort":false}""")
        assertEquals(BlockLog(5, true, false, tooEasy = false), old)
    }
}

class HeaderTest {
    private val catalog = app.calisthenics.domain.content.parseCatalog(fixtureText("content/starter/catalog.json"))
    private val none = app.calisthenics.domain.progression.ProgressSnapshot(emptyMap(), emptyMap(), emptyMap(), emptyList())
    private fun plan(f: WorkoutFormat) = (app.calisthenics.domain.planner.buildTrainPlan(catalog, app.calisthenics.domain.routine.StarterRoutine.routine, none, app.calisthenics.domain.equipment.SeedProfiles.home,
        app.calisthenics.domain.planner.TrainSettings(rounds = 3, format = f)) as app.calisthenics.domain.planner.PlanResult.Ready).plan

    @Test fun theHeaderNamesTheRoundThePairOrTheExerciseAndTheSet() {
        for ((f, kind) in listOf(WorkoutFormat.CIRCUIT to HeaderKind.ROUND, WorkoutFormat.PAIRS to HeaderKind.PAIR, WorkoutFormat.STRAIGHT to HeaderKind.EXERCISE)) {
            val p = plan(f)
            val i = p.blocks.indexOfLast { it.type == BlockType.WORK && it.roundIndex == 2 }
            val h = headerFor(p, p.blocks[i], i)
            assertEquals(kind, h.kind); assertEquals(2, h.set); assertEquals(3, h.sets)
            if (f == WorkoutFormat.CIRCUIT) assertNull(h.group) else assertNotNull(h.group)
        }
        val pairs = plan(WorkoutFormat.PAIRS)
        val second = pairs.blocks.indexOfFirst { it.slotId == "pull" && it.type == BlockType.WORK }
        assertEquals(2, headerFor(pairs, pairs.blocks[second], second).group)
    }

    @Test fun breaksAndGetReadyBlocksKeepTheirPairAndSet() {
        val p = plan(WorkoutFormat.PAIRS)
        for ((i, b) in p.blocks.withIndex()) if (b.roundIndex != null) assertNotNull("block ${b.id} (${b.type}) has no pair number", headerFor(p, b, i).group)
    }

    // R7-T02 (B6): the old assertion (a warm-up block is "step n of N") is replaced: warm-up and cool-down count their own blocks
    @Test fun warmupAndCooldownCountOnlyTheirOwnBlocks() {
        val w = (1..3).map { TimelineBlock("w$it", BlockType.WARMUP, 30) }
        val work = TimelineBlock("x", BlockType.WORK, 40, 1, "s", "v")
        val tr = TimelineBlock("tr", BlockType.TRANSITION, 5)
        val c = (1..2).map { TimelineBlock("c$it", BlockType.COOLDOWN, 30) }
        val p = plan(WorkoutFormat.CIRCUIT).copy(blocks = w.take(2) + tr + w.drop(2) + work + c)
        val h2 = headerFor(p, p.blocks[1], 1)
        assertEquals(HeaderKind.WARMUP, h2.kind); assertEquals(2, h2.set); assertEquals(3, h2.sets)
        val ht = headerFor(p, p.blocks[2], 2) // the get-ready inside the warm-up reads like the block it leads to
        assertEquals(HeaderKind.WARMUP, ht.kind); assertEquals(3, ht.set)
        val hc = headerFor(p, p.blocks[6], 6)
        assertEquals(HeaderKind.COOLDOWN, hc.kind); assertEquals(2, hc.set); assertEquals(2, hc.sets)
        assertEquals(HeaderKind.ROUND, headerFor(p, p.blocks[4], 4).kind)
    }
}
