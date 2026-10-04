package app.calisthenics.domain

import app.calisthenics.domain.feedback.Execution
import app.calisthenics.domain.model.*
import app.calisthenics.domain.model.Target
import app.calisthenics.domain.session.*
import org.junit.Assert.*
import org.junit.Test

class SessionReducerTest {
    private fun work(id: String, secs: Int = 40, early: String? = null) =
        TimelineBlock(id, BlockType.WORK, secs, 1, "s-$id", "pushup-knee", target = Target(TargetType.REPS, 5), prescriptionTier = 1, earlyCompletionStretchId = early)
    private fun rec(id: String, secs: Int = 20) = TimelineBlock(id, BlockType.STRETCH, secs, 1, variationId = "calf-stretch", side = Side.BOTH)

    private fun plan(vararg b: TimelineBlock) = WorkoutPlan("p", "usual", 1, 1, 0, "home", 600, b.sumOf { it.durationSeconds },
        setOf(StrengthFocus.FULL_BODY), true, null, 1, emptyList(), emptyList(), false, false, b.toList())

    private val p3 = plan(work("w1"), rec("r1"), work("w2"))
    private fun started(p: WorkoutPlan = p3, at: Long = 1_000) = reduce(newSession("S", p), SessionEvent.Start(at)).state

    @Test fun `start enters the first block with a deadline and a start cue`() {
        val r = reduce(newSession("S", p3), SessionEvent.Start(1_000))
        assertEquals(Phase.RUNNING, r.state.phase); assertEquals(41_000, r.state.deadlineMs)
        assertTrue(r.effects.any { it is SessionEffect.Cue && it.kind == CueKind.START })
        assertTrue(r.effects.any { it is SessionEffect.Persist })
    }

    @Test fun `remaining time derives from the deadline and ticks do not drift`() {
        var s = started()
        repeat(100) { s = reduce(s, SessionEvent.Tick(1_000 + (it + 1) * 100L)).state }
        assertEquals(41_000 - 11_000, s.remainingAt(11_000)); assertEquals(0, s.blockIndex)
    }

    @Test fun `a block completes exactly at its deadline`() {
        val s = reduce(started(), SessionEvent.Tick(41_000)).state
        assertEquals(1, s.blockIndex); assertEquals(Execution.COMPLETED, s.executions["w1"]); assertEquals(61_000, s.deadlineMs)
    }

    @Test fun `a very late tick advances through several blocks without duplicating or losing any`() {
        val (s, fx) = run(started(), listOf(SessionEvent.Tick(1_000 + 40_000 + 20_000 + 5_000)))
        assertEquals(2, s.blockIndex); assertEquals(Execution.COMPLETED, s.executions["w1"]); assertEquals(Execution.COMPLETED, s.executions["r1"])
        assertEquals(1_000 + 40_000 + 20_000 + 40_000, s.deadlineMs)
        assertEquals(fx.filterIsInstance<SessionEffect.Cue>().map { it.id }.distinct().size, fx.filterIsInstance<SessionEffect.Cue>().size)
    }

    @Test fun `finishing the last block completes the session once and later events change nothing`() {
        val s = reduce(started(), SessionEvent.Tick(1_000_000)).state
        assertEquals(Phase.COMPLETED, s.phase); assertEquals(FinishReason.ALL_BLOCKS_DONE, s.finishReason)
        assertSame(s, reduce(s, SessionEvent.Tick(2_000_000)).state)
        assertSame(s, reduce(s, SessionEvent.Skip(2_000_000)).state)
        assertTrue(reduce(started(), SessionEvent.Tick(1_000_000)).effects.contains(SessionEffect.ReleaseWakeLock))
    }

    @Test fun `pause freezes the remaining time and resume sets a new deadline without losing position`() {
        val p = reduce(started(), SessionEvent.Pause(11_000)).state
        assertEquals(Phase.PAUSED, p.phase); assertEquals(30_000, p.remainingMs)
        val ticked = reduce(p, SessionEvent.Tick(500_000)).state
        assertEquals(p, ticked) // ticks do nothing while paused
        val r = reduce(p, SessionEvent.Resume(100_000)).state
        assertEquals(130_000, r.deadlineMs); assertEquals(Phase.RUNNING, r.phase)
    }

    @Test fun `pause time is not counted as active time`() {
        var s = started(); s = reduce(s, SessionEvent.Pause(11_000)).state; s = reduce(s, SessionEvent.Resume(500_000)).state
        s = reduce(s, SessionEvent.Tick(510_000)).state
        assertEquals(20_000, s.activeElapsedMs)
    }

    @Test fun `double pause and double resume are harmless`() {
        val p = reduce(started(), SessionEvent.Pause(11_000)).state
        assertSame(p, reduce(p, SessionEvent.Pause(12_000)).state)
        val r = reduce(p, SessionEvent.Resume(20_000)).state
        assertSame(r, reduce(r, SessionEvent.Resume(21_000)).state)
    }

    @Test fun `audio interruption pauses with its reason`() {
        val s = reduce(started(), SessionEvent.AudioInterrupted(5_000)).state
        assertEquals(Phase.PAUSED, s.phase); assertEquals(PauseReason.AUDIO_INTERRUPTED, s.pauseReason)
    }

    @Test fun `skip marks the block skipped not met and moves on`() {
        val s = reduce(started(), SessionEvent.Skip(5_000)).state
        assertEquals(Execution.SKIPPED, s.executions["w1"]); assertEquals(1, s.blockIndex)
        assertEquals(25_000, s.deadlineMs)
    }

    @Test fun `skipping the last block finishes the session`() {
        var s = started(plan(work("only")))
        s = reduce(s, SessionEvent.Skip(3_000)).state
        assertEquals(Phase.COMPLETED, s.phase); assertEquals(Execution.SKIPPED, s.executions["only"])
    }

    @Test fun `early done without a reviewed stretch does nothing and never makes passive rest`() {
        val s = started()
        assertSame(s, reduce(s, SessionEvent.EarlyDone(5_000)).state)
    }

    @Test fun `early done with a reviewed stretch completes the work and jumps to the stretch`() {
        val s = reduce(started(plan(work("w1", early = "calf-stretch"), rec("r1"))), SessionEvent.EarlyDone(9_000)).state
        assertEquals(Execution.COMPLETED, s.executions["w1"]); assertEquals(1, s.blockIndex)
    }

    @Test fun `easier alternative pauses, keeps the original as partial and inserts the replacement`() {
        val easierBlock = TimelineBlock("w1-easier", BlockType.WORK, 40, 1, "s-w1", "pushup-knee", target = Target(TargetType.REPS, 3), prescriptionTier = 1)
        val s = reduce(started(), SessionEvent.EasierAlternative(11_000, easierBlock)).state
        assertEquals(Phase.PAUSED, s.phase); assertEquals(Execution.PARTIAL, s.executions["w1"])
        assertEquals("w1-easier", s.replacements["w1"]); assertEquals("w1-easier", s.currentBlock?.id)
        assertEquals(10_000L, s.activeMs["w1"]) // original keeps the actual work
        assertTrue(s.plan.blocks.map { it.id }.containsAll(listOf("w1", "w1-easier", "r1", "w2")))
        assertEquals(s.plan.blocks.sumOf { it.durationSeconds }, s.plan.plannedDurationSeconds)
        // replacement runs only after an explicit resume
        assertEquals(Phase.RUNNING, reduce(s, SessionEvent.Resume(20_000)).state.phase)
    }

    @Test fun `finish early marks the running block partial and later blocks not started`() {
        val s = reduce(started(), SessionEvent.FinishEarly(15_000)).state
        assertEquals(Phase.PARTIAL_FINISHED, s.phase); assertEquals(Execution.PARTIAL, s.executions["w1"])
        assertNull(s.executions["r1"]); assertNull(s.executions["w2"])
    }

    @Test fun `finish early before any active time is not started`() {
        val s = reduce(started(), SessionEvent.FinishEarly(1_000)).state
        assertEquals(Execution.NOT_STARTED, s.executions["w1"])
    }

    @Test fun `process recovery freezes everything and never auto completes blocks`() {
        val running = reduce(started(), SessionEvent.Tick(11_000)).state
        val rec = reduce(running, SessionEvent.ProcessRecovered).state
        assertEquals(Phase.RECOVERY_REQUIRED, rec.phase); assertEquals(0, rec.blockIndex)
        assertEquals(1000, rec.unknownLossMs); assertEquals(Execution.PARTIAL, rec.executions["w1"])
        // wall time passing changes nothing
        assertSame(rec, reduce(rec, SessionEvent.Tick(99_999_999)).state)
        assertEquals(30_000, rec.remainingMs)
    }

    @Test fun `recovery choice resumable or partial`() {
        val rec = reduce(reduce(started(), SessionEvent.Tick(11_000)).state, SessionEvent.ProcessRecovered).state
        val paused = reduce(rec, SessionEvent.RecoveryChoice(true)).state
        assertEquals(Phase.PAUSED, paused.phase); assertEquals(30_000, paused.remainingMs)
        assertEquals(Phase.PARTIAL_FINISHED, reduce(rec, SessionEvent.RecoveryChoice(false)).state.phase)
    }

    @Test fun `recovering a paused session keeps its exact position`() {
        val paused = reduce(started(), SessionEvent.Pause(11_000)).state
        val rec = reduce(paused, SessionEvent.ProcessRecovered).state
        assertEquals(30_000, rec.remainingMs); assertEquals(0, rec.blockIndex)
    }

    @Test fun `every state changing event bumps the sequence and persists it`() {
        var s = newSession("S", p3); val seen = mutableListOf<Long>()
        for (e in listOf(SessionEvent.Start(0), SessionEvent.Pause(1_000), SessionEvent.Resume(2_000), SessionEvent.Skip(3_000), SessionEvent.FinishEarly(4_000))) {
            val r = reduce(s, e); s = r.state
            r.effects.filterIsInstance<SessionEffect.Persist>().forEach { seen += it.sequence }
        }
        assertEquals(seen.distinct(), seen); assertEquals(seen.sorted(), seen); assertTrue(seen.size >= 5)
    }

    @Test fun `repeated checkpoint requests are idempotent`() {
        val s = started()
        val a = reduce(s.copy(lastCheckpointSeq = s.eventSequence - 1), SessionEvent.PersistCheckpoint(2_000))
        assertEquals(1, a.effects.size)
        assertTrue(reduce(a.state, SessionEvent.PersistCheckpoint(2_100)).effects.isEmpty())
    }

    @Test fun `each cue id is emitted once and cues never overlap in one tick`() {
        val p = plan(work("w1", 20), work("w2", 20))
        var s = started(p, 0); val all = mutableListOf<SessionEffect.Cue>()
        for (t in 0..40_000 step 100) {
            val r = reduce(s, SessionEvent.Tick(t.toLong())); s = r.state
            val cues = r.effects.filterIsInstance<SessionEffect.Cue>()
            // a block boundary may produce a finish/start pair but never two countdown/preview cues at once
            assertTrue(cues.count { it.kind != CueKind.START && it.kind != CueKind.FINISHED } <= 1)
            all += cues
        }
        assertEquals(all.size, all.map { it.id }.distinct().size)
        assertTrue(all.any { it.kind == CueKind.PREVIEW_NEXT }); assertEquals(listOf(CueKind.COUNTDOWN_3, CueKind.COUNTDOWN_2, CueKind.COUNTDOWN_1), all.map { it.kind }.filter { it.name.startsWith("COUNT") })
    }

    @Test fun `preview is suppressed on short blocks so it cannot overlap the countdown`() {
        val p = plan(work("w1", 6), work("w2", 6))
        var s = started(p, 0); val kinds = mutableListOf<CueKind>()
        for (t in 0..6_000 step 100) { val r = reduce(s, SessionEvent.Tick(t.toLong())); s = r.state; kinds += r.effects.filterIsInstance<SessionEffect.Cue>().map { it.kind } }
        assertFalse(CueKind.PREVIEW_NEXT in kinds)
    }

    @Test fun `repeated identical late callbacks are idempotent`() {
        val a = reduce(started(), SessionEvent.Tick(70_000)).state
        val b = reduce(a, SessionEvent.Tick(70_000)).state
        assertEquals(a, b)
    }

    @Test fun `full run with a pause skip and finish gives a consistent trace`() {
        val (s, fx) = run(started(), listOf(SessionEvent.Tick(10_000), SessionEvent.Pause(12_000), SessionEvent.Resume(30_000),
            SessionEvent.Tick(60_000), SessionEvent.Skip(61_000), SessionEvent.Tick(200_000)))
        assertEquals(Phase.COMPLETED, s.phase)
        assertTrue(fx.contains(SessionEffect.ReleaseWakeLock))
        assertEquals(setOf("w1", "r1", "w2"), s.executions.keys)
    }
}
