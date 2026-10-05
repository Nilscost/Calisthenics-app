package app.calisthenics.domain

import app.calisthenics.domain.feedback.Rating
import app.calisthenics.domain.history.*
import app.calisthenics.domain.model.*
import app.calisthenics.domain.model.Target
import org.junit.Assert.*
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneOffset

class SessionDetailTest {
    private fun work(r: Int, vid: String = "pushup-standard", t: Int = 8) = TimelineBlock("r$r-$vid", BlockType.WORK, 60, r, "s", vid, target = Target(TargetType.REPS, t), prescriptionTier = 3)
    private val blocks = listOf(work(1), work(2), work(3), work(1, "plank", 15).copy(target = Target(TargetType.HOLD_SECONDS, 15)))
    private val plan = WorkoutPlan("p", "r", 1, 1, 0, "home", 600, 240, setOf(StrengthFocus.FULL_BODY), true, null, 3, emptyList(), emptyList(), false, false, blocks)
    private fun rec(id: String, vid: String, outcome: String, v: Int?, secs: Int = 60) = BlockRecord(id, vid, "WORK", outcome, secs, v)
    private val day = LocalDate.of(2026, 10, 5) // a Monday
    private val ms = day.atTime(9, 0).toInstant(ZoneOffset.UTC).toEpochMilli()

    private fun session(vararg b: BlockRecord, end: Long? = ms + 45 * 60_000L, status: String = "COMPLETED") = SessionRecord("S", "p", ms, end, status, b.toList())

    @Test fun overviewCountsOnlyDoneWork() {
        val s = session(rec("r1-pushup-standard", "pushup-standard", "MET", 8), rec("r2-pushup-standard", "pushup-standard", "MET", 6),
            rec("r3-pushup-standard", "pushup-standard", "SKIPPED", null, 0), rec("r1-plank", "plank", "PARTIAL", 9, 10))
        val o = overviewOf(plan, s, ZoneOffset.UTC)
        assertEquals(45, o.minutes); assertEquals(2, o.rounds); assertEquals(2, o.exercises); assertEquals(day, o.day); assertFalse(o.finishedEarly)
    }

    @Test fun unfinishedMinutesFallBackToTheTimeSpent() {
        val o = overviewOf(plan, session(rec("r1-pushup-standard", "pushup-standard", "MET", 8, 600), end = null, status = "PARTIAL_FINISHED"), ZoneOffset.UTC)
        assertEquals(10, o.minutes); assertTrue(o.finishedEarly)
    }

    @Test fun overviewWorksWithoutThePlanSnapshot() {
        val o = overviewOf(null, session(rec("x", "plank", "MET", null)), ZoneOffset.UTC)
        assertEquals(1, o.exercises); assertEquals(0, o.rounds)
    }

    @Test fun detailsListEachRoundAndTheLowestTypedValue() {
        val d = exerciseDetails(plan, listOf(rec("r1-pushup-standard", "pushup-standard", "MET", 8), rec("r2-pushup-standard", "pushup-standard", "MET", 6),
            rec("r3-pushup-standard", "pushup-standard", "MET", null), rec("r1-plank", "plank", "SKIPPED", null)))
        assertEquals(listOf("pushup-standard"), d.map { it.variationId }) // the skipped plank is not shown
        val p = d.single()
        assertEquals(listOf(1, 2, 3), p.rounds.map { it.round }); assertEquals(listOf<Int?>(8, 6, null), p.rounds.map { it.reps })
        assertEquals(6, p.lowest); assertFalse(p.isHold)
    }

    @Test fun nothingTypedMeansNoLowestValue() {
        val d = exerciseDetails(plan, listOf(rec("r1-plank", "plank", "MET", null))).single()
        assertNull(d.lowest); assertTrue(d.isHold)
    }

    @Test fun correctedNumbersImplyTheRating() {
        val t = Target(TargetType.REPS, 8)
        assertEquals(Rating.BELOW, ratingFor(6, t, false)); assertEquals(Rating.MET, ratingFor(8, t, false))
        assertEquals(Rating.BELOW, ratingFor(10, t, true)); assertEquals(Rating.MET, ratingFor(null, t, false))
    }

    @Test fun weeksStartOnMondayAndHaveSevenDays() {
        assertEquals(day, weekStartOf(LocalDate.of(2026, 10, 11)))
        val w = weekDays(day)
        assertEquals(7, w.size); assertEquals(DayOfWeek.MONDAY, w.first().dayOfWeek); assertEquals(DayOfWeek.SUNDAY, w.last().dayOfWeek)
    }

    @Test fun unfinishedSessionsAreNotWorkouts() {
        val running = session(status = "RUNNING", end = null)
        assertTrue(finishedSessions(listOf(running, session())).size == 1)
    }
}
