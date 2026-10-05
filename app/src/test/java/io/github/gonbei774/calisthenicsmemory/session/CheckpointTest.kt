package io.github.gonbei774.calisthenicsmemory.session

import app.calisthenics.domain.feedback.Execution
import app.calisthenics.domain.model.*
import app.calisthenics.domain.session.*
import org.junit.Assert.*
import org.junit.Test

class CheckpointTest {
    private fun plan() = WorkoutPlan(id = "p", routineId = "r", routineRevision = 1, catalogVersion = 1, createdAtEpochMs = 0, profileId = "x",
        requestedDurationSeconds = 60, plannedDurationSeconds = 60, focus = setOf(StrengthFocus.FULL_BODY), stretchOn = false, rounds = 1, changesExplained = emptyList(), warnings = emptyList(), needsAcceptance = false, usesDraftContent = false,
        blocks = listOf(TimelineBlock("b1", BlockType.WORK, 30, variationId = "v1"), TimelineBlock("b2", BlockType.PASSIVE_RECOVERY, 30)))

    @Test fun roundTripAndRecovery() {
        var s = newSession("s1", plan())
        s = reduce(s, SessionEvent.Start(1000)).state
        s = reduce(s, SessionEvent.Tick(11_000)).state
        val text = CheckpointCodec.encode(s, 5L, 11_000, 99L)
        val c = CheckpointCodec.decode(text)!!
        assertEquals("s1", c.sessionId); assertEquals(Phase.RUNNING, c.phase)
        val rec = reduce(CheckpointCodec.toState(c), SessionEvent.ProcessRecovered).state
        assertEquals(Phase.RECOVERY_REQUIRED, rec.phase)
        assertEquals(Execution.PARTIAL, rec.executions["b1"])
        assertTrue("remaining must not exceed the block", rec.remainingMs in 1..30_000)
        val paused = reduce(rec, SessionEvent.RecoveryChoice(true)).state
        assertEquals(Phase.PAUSED, paused.phase)
        val fin = reduce(rec, SessionEvent.RecoveryChoice(false)).state
        assertEquals(Phase.PARTIAL_FINISHED, fin.phase)
    }
    @Test fun whatTheUserLoggedSurvivesACrash() {
        var s = reduce(newSession("s1", plan()), SessionEvent.Start(1000)).state
        s = reduce(s, SessionEvent.Tick(31_000)).state // work done, recovery running
        s = reduce(s, SessionEvent.LogBlock("b1", BlockLog(reps = 7, discomfort = true))).state
        val c = CheckpointCodec.decode(CheckpointCodec.encode(s, 5L, 32_000, 99L))!!
        assertEquals(BlockLog(reps = 7, discomfort = true), c.logged["b1"])
        assertEquals(BlockLog(reps = 7, discomfort = true), reduce(CheckpointCodec.toState(c), SessionEvent.ProcessRecovered).state.logged["b1"])
        // a checkpoint written before U07 has no logs and still decodes
        val old = CheckpointCodec.encode(s, 5L, 32_000, 99L).replace(Regex(""","logged":\{.*?\}\}"""), "")
        assertTrue(CheckpointCodec.decode(old)?.logged?.isEmpty() == true)
    }
    @Test fun garbageIsRejected() { assertNull(CheckpointCodec.decode("{not json")) }
}
