package app.calisthenics.domain

import app.calisthenics.domain.feedback.Execution
import app.calisthenics.domain.history.*
import app.calisthenics.domain.model.*
import app.calisthenics.domain.model.Target
import app.calisthenics.domain.session.*
import org.junit.Assert.*
import org.junit.Test
import java.time.ZoneOffset

/** The end screen shows overviewOf(recordOfState(...)): exactly the numbers History shows for the saved session. */
class EndScreenAgreesWithHistoryTest {
    private fun work(round: Int, vid: String) =
        TimelineBlock("r$round-$vid-work", BlockType.WORK, 60, round, "push", vid, target = Target(TargetType.REPS, 8), prescriptionTier = 3)
    private val blocks = listOf(work(1, "pushup-standard"), work(1, "squat-bodyweight"), work(2, "pushup-standard"))
    private val plan = WorkoutPlan("p", "usual", 1, 1, 0, "home", 600, 180, setOf(StrengthFocus.FULL_BODY), true,
        null, 3, emptyList(), emptyList(), false, false, blocks)

    @Test fun aPartialBlockCountsAndMinutesAreWallClock() {
        var s = reduce(newSession("S", plan), SessionEvent.Start(0)).state
        s = reduce(s, SessionEvent.Tick(60_000)).state       // first block completed
        s = reduce(s, SessionEvent.Tick(90_000)).state       // second block half done
        s = reduce(s, SessionEvent.FinishEarly(90_000)).state
        assertTrue(s.isTerminal)
        assertEquals(Execution.COMPLETED, s.executions["r1-pushup-standard-work"])
        val record = recordOfState(s, 1_000_000L, 1_000_000L + 3 * 60_000L)
        assertEquals(listOf("MET", "PARTIAL", "SKIPPED"), record.blocks.map { it.outcome })
        val o = overviewOf(plan, record, ZoneOffset.UTC)
        assertEquals(2, o.exercises)  // the old end screen counted only the COMPLETED block: 1
        assertEquals(1, o.rounds)
        assertEquals(3, o.minutes)    // wall clock, not the 1.5 active minutes
    }

    @Test fun unknownTimesFallBackToActiveSeconds() {
        var s = reduce(newSession("S", plan), SessionEvent.Start(0)).state
        s = reduce(s, SessionEvent.FinishEarly(120_000)).state
        val record = recordOfState(s, 0L, null)
        assertNull(record.endedAtEpochMs)
    }
}
