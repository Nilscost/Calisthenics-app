package app.calisthenics.domain

import app.calisthenics.domain.content.parseCatalog
import app.calisthenics.domain.feedback.*
import app.calisthenics.domain.history.*
import app.calisthenics.domain.model.*
import app.calisthenics.domain.model.Target
import app.calisthenics.domain.progression.*
import app.calisthenics.domain.session.*
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.time.ZoneOffset

/**
 * R8.1-T03 (F3): what the person logged is what progression, charts and History use. The owner's case: inverted rows prescribed with 10, logged 6.
 * The test runs the same chain the app does: the session reducer (logger events, DONE), the saved block results and row feedback, the evidence and the replay.
 */
class ActualRepsTest {
    private val c = parseCatalog(File(System.getProperty("repo.root"), "content/starter/catalog.json").readText())
    private val vid = "inverted-row"
    private val tier = c.policyForVariation(vid)!!.tiers.first { it.target.value == 10 }   // prescribed 10
    private fun work(round: Int) = TimelineBlock("r$round-pull-work", BlockType.WORK, 45, round, "pull", vid, target = Target(TargetType.REPS, 10), prescriptionTier = tier.index)
    private fun rest(round: Int) = TimelineBlock("r$round-pull-rec", BlockType.PASSIVE_RECOVERY, 30, round, "pull")
    private val blocks = listOf(work(1), rest(1), work(2), rest(2), work(3))
    private val plan = WorkoutPlan("p", "usual", 1, 1, 0, "home", 600, blocks.sumOf { it.durationSeconds }, setOf(StrengthFocus.FULL_BODY), true,
        null, 3, emptyList(), emptyList(), false, false, blocks)

    /** Runs the session: [logs] are typed during the block (before DONE); DONE ends each work block early, rests run out. */
    private fun run(logs: Map<String, Int?>): SessionState {
        var s = reduce(newSession("S", plan), SessionEvent.Start(0)).state
        var now = 0L
        for (b in plan.blocks) {
            now += 1000
            if (b.type == BlockType.WORK) {
                logs[b.id]?.let { s = reduce(s, SessionEvent.LogBlock(b.id, BlockLog(reps = it))).state }
                s = reduce(s, SessionEvent.EarlyDone(now)).state
            } else { now += b.durationSeconds * 1000L; s = reduce(s, SessionEvent.Tick(now)).state }
        }
        return s
    }

    /** What WorkoutSessionService.finish stores, then what ProgressLoader and HistorySource read back. */
    private fun record(s: SessionState): SessionRecord = SessionRecord("S", plan.id, 86_400_000L * 20, null, "COMPLETED",
        plan.blocks.filter { it.type == BlockType.WORK || it.type == BlockType.STRETCH }.map { b ->
            BlockRecord(b.id, b.variationId, b.type.name, when (s.executions[b.id]) { Execution.COMPLETED -> "MET"; Execution.PARTIAL -> "PARTIAL"; else -> "SKIPPED" }, 0, s.logged[b.id]?.reps)
        })
    private fun rows(s: SessionState) = rowLogsFrom(plan, s.logged).map { FeedbackRow(it.variationId, null, 1, it.rating, it.discomfort, it.actualReps) }

    @Test fun loggedSixIsWhatChartDetailsAndEvidenceSee() {
        val s = run(mapOf("r1-pull-work" to 6, "r2-pull-work" to 6, "r3-pull-work" to 6))
        val rec = record(s)
        // chart / History progressions
        val series = effectiveSetSeries(listOf(SessionWithPlan(rec, plan, rows(s))), ZoneOffset.UTC)
        assertEquals(6, series.getValue(vid).single().lowest)
        // History detail rounds
        val details = exerciseDetails(plan, rec.blocks).single()
        assertEquals(listOf(6, 6, 6), details.rounds.map { it.reps })
        // progression evidence: below the prescribed 10, so no streak and the level is held
        val achieved = rec.blocks.filter { it.outcome == "MET" }.associate { it.blockId to it.achievedValue }
        val fb = effectiveFeedback(plan, achieved, rows(s))
        val ev = deriveEvidence("S", 20, plan, resolveFeedback(plan, plan.blocks.associate { it.id to (s.executions[it.id] ?: Execution.NOT_STARTED) }, emptyMap(), fb), { c.variation(it)!!.familyId }, { _, _ -> 2 })
        assertTrue(ev.single().anyBelow); assertFalse(ev.single().qualifying)
        val p = ProgressionEngine(c).replay(ev, listOf(UserAction.SelfAssessment(0, vid, tier.index))).variations.getValue(vid)
        assertEquals(tier.index, p.tier); assertTrue(tier.index !in p.achievedTiers); assertTrue(p.streakDays.isEmpty())
    }

    @Test fun acceptingTheTargetUnchangedCountsAsTheTarget() {
        val s = run(emptyMap())
        val series = effectiveSetSeries(listOf(SessionWithPlan(record(s), plan, rows(s))), ZoneOffset.UTC)
        assertEquals(10, series.getValue(vid).single().lowest)
        assertTrue(s.logged.isEmpty())
    }

    @Test fun onlyTheEditedSetIsBelowTheOthersAreAccepted() {
        val s = run(mapOf("r2-pull-work" to 6))
        val series = effectiveSetSeries(listOf(SessionWithPlan(record(s), plan, rows(s))), ZoneOffset.UTC)
        assertEquals(6, series.getValue(vid).single().lowest)       // the lowest set is what counts
    }

    @Test fun theNextSetStartsAtTheNumberJustLoggedSoDoneWithoutEditingKeepsIt() {
        val s = run(mapOf("r1-pull-work" to 6))
        assertEquals(6, carriedReps(plan, s.logged, plan.blocks[2]))                 // round 2 shows 6, not 10
        assertNull(carriedReps(plan, s.logged, plan.blocks[0]))                      // the first set starts at the target
        assertEquals(6, carriedReps(plan, s.logged, plan.blocks[4]))                 // and round 3 after it
    }

    @Test fun aCorrectionOfOneRoundDoesNotTurnTheOthersBackIntoThePrescription() {
        val s = run(mapOf("r1-pull-work" to 6, "r2-pull-work" to 6, "r3-pull-work" to 6))
        val rec = record(s)
        val fix = listOf(FeedbackRow(vid, "r2-pull-work", 2, Rating.MET, false, 8), FeedbackRow(vid, "r1-pull-work", 3, Rating.BELOW, false, 6), FeedbackRow(vid, "r3-pull-work", 4, Rating.BELOW, false, 6))
        val series = effectiveSetSeries(listOf(SessionWithPlan(rec, plan, rows(s) + fix)), ZoneOffset.UTC)
        assertEquals(6, series.getValue(vid).single().lowest)
    }

    @Test fun skippingASetAfterTypingARealNumberKeepsTheNumberAsAPartialSet() {
        var s = reduce(newSession("S", plan), SessionEvent.Start(0)).state
        s = reduce(s, SessionEvent.LogBlock("r1-pull-work", BlockLog(reps = 6))).state
        s = reduce(s, SessionEvent.Skip(1000)).state
        assertEquals(Execution.PARTIAL, s.executions["r1-pull-work"])
        assertEquals(6, record(s).blocks.first { it.blockId == "r1-pull-work" }.achievedValue)
        // nothing typed: a skip is still a skip
        var t = reduce(newSession("S", plan), SessionEvent.Start(0)).state
        t = reduce(t, SessionEvent.Skip(1000)).state
        assertEquals(Execution.SKIPPED, t.executions["r1-pull-work"])
    }
}
