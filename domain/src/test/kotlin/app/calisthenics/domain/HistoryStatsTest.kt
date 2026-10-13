package app.calisthenics.domain

import app.calisthenics.domain.history.*
import app.calisthenics.domain.progression.ProgressEvent
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset

/** V27b (doc 17 §2.5). */
class HistoryStatsTest {
    private val zone: ZoneId = ZoneOffset.UTC
    private fun ms(d: LocalDate) = d.atTime(10, 0).toInstant(ZoneOffset.UTC).toEpochMilli()
    private fun work(id: String, v: String, secs: Int, value: Int?, outcome: String = "MET") = BlockRecord(id, v, "WORK", outcome, secs, value)
    private fun session(id: String, d: LocalDate, status: String = "COMPLETED", vararg b: BlockRecord) = SessionRecord(id, "p$id", ms(d), ms(d) + 1, status, b.toList())
    private val today = LocalDate.of(2026, 10, 10)   // a Saturday; the week starts Monday 5

    @Test fun tilesCountThisWeekThisMonthAndWorkMinutes() {
        val s = listOf(
            session("a", LocalDate.of(2026, 10, 6), "COMPLETED", work("1", "pushup-standard", 120, 8), work("2", "pushup-standard", 180, 8)),
            session("b", LocalDate.of(2026, 10, 9), "PARTIAL_FINISHED", work("3", "squat-air", 60, 10)),
            session("c", LocalDate.of(2026, 10, 2), "COMPLETED", work("4", "squat-air", 600, 10)),   // last week
            session("d", LocalDate.of(2026, 10, 10), "RUNNING", work("5", "squat-air", 600, 10)),   // not finished: not counted
        )
        val t = historyTiles(s, listOf(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 8), LocalDate.of(2026, 9, 30)), today, zone)
        assertEquals(2, t.sessionsThisWeek); assertEquals(6, t.workMinutesThisWeek); assertEquals(2, t.levelUpsThisMonth)
        assertEquals(HistoryTiles(0, 0, 0), historyTiles(emptyList(), emptyList(), today, zone))
    }

    @Test fun levelUpsAreTheStepsForward() {
        fun e(m: String) = ProgressEvent(1, "v", m)
        assertTrue(isLevelUp(e("Tier 2 achieved after 3 qualifying sessions over 8 days. Next target: 10 reps (tier 3).")))
        assertTrue(isLevelUp(e("All five tiers earned. Moving on to X (tier 1).")))
        assertTrue(isLevelUp(e("Every set of Push-up reached the top of the range. Moving on to Y")))
        assertFalse(isLevelUp(e("Below target — difficulty held at tier 2; the qualifying streak restarts.")))
        assertFalse(isLevelUp(e("You accepted a harder target (tier 4).")))   // not counted: the person's choice, not earned
    }

    @Test fun theLowestSetOfEachSessionIsTheChartPoint() {
        val s = listOf(
            session("b", LocalDate.of(2026, 10, 9), "COMPLETED", work("1", "pushup-standard", 30, 9), work("2", "pushup-standard", 30, 7), work("3", "pushup-standard", 30, 8), work("4", "pushup-standard", 30, null, "SKIPPED")),
            session("a", LocalDate.of(2026, 10, 2), "COMPLETED", work("5", "pushup-standard", 30, 6), work("6", "pushup-standard", 30, 8)),
            session("x", LocalDate.of(2026, 10, 3), "RUNNING", work("7", "pushup-standard", 30, 1)),
        )
        val series = lowestSetSeries(s, zone)
        assertEquals(listOf(6, 7), series.getValue("pushup-standard").map { it.lowest })    // oldest first; skipped and running ignored
        assertEquals(listOf(LocalDate.of(2026, 10, 2), LocalDate.of(2026, 10, 9)), series.getValue("pushup-standard").map { it.day })
    }

    // ---- R7-T06 (B7, B8): the chart uses what the person effectively did
    private fun blk(id: String, v: String, round: Int, t: app.calisthenics.domain.model.Target) =
        app.calisthenics.domain.model.TimelineBlock(id, app.calisthenics.domain.model.BlockType.WORK, 60, round, "s", v, app.calisthenics.domain.model.Side.NONE, t, 3)
    private fun planOf(vararg b: app.calisthenics.domain.model.TimelineBlock) =
        app.calisthenics.domain.model.WorkoutPlan("p", "r", 1, 1, 0L, "home", 0, 60, emptySet(), false, null, 1, emptyList(), emptyList(), false, false, b.toList())
    private val reps = { n: Int -> app.calisthenics.domain.model.Target(app.calisthenics.domain.model.TargetType.REPS, n) }

    @Test fun anAsPlannedSetCountsAsItsTarget() {
        val plan = planOf(blk("1", "pushup-standard", 1, reps(10)), blk("2", "pushup-standard", 2, reps(10)))
        val s = session("a", LocalDate.of(2026, 10, 2), "COMPLETED", work("1", "pushup-standard", 30, null), work("2", "pushup-standard", 30, 12))
        val series = effectiveSetSeries(listOf(SessionWithPlan(s, plan)), zone)
        assertEquals(listOf(10), series.getValue("pushup-standard").map { it.lowest })   // set 1 untouched = 10, set 2 typed 12 -> lowest 10
        assertTrue(lowestSetSeries(listOf(s), zone).getValue("pushup-standard").single().lowest == 12)   // the old series ignored the untouched set
    }

    @Test fun aCorrectionMovesThePoint() {
        val plan = planOf(blk("1", "pushup-standard", 1, reps(10)), blk("2", "pushup-standard", 2, reps(10)))
        val s = session("a", LocalDate.of(2026, 10, 2), "COMPLETED", work("1", "pushup-standard", 30, 8), work("2", "pushup-standard", 30, 9))
        val rows = listOf(app.calisthenics.domain.feedback.FeedbackRow("pushup-standard", "1", 1, app.calisthenics.domain.feedback.Rating.MET, false, 14),
            app.calisthenics.domain.feedback.FeedbackRow("pushup-standard", "2", 1, app.calisthenics.domain.feedback.Rating.MET, false, 14))
        assertEquals(8, effectiveSetSeries(listOf(SessionWithPlan(s, plan)), zone).getValue("pushup-standard").single().lowest)
        assertEquals(14, effectiveSetSeries(listOf(SessionWithPlan(s, plan, rows)), zone).getValue("pushup-standard").single().lowest)
    }

    @Test fun holdsAreInSecondsAndARangeCountsAtItsTop() {
        val hold = app.calisthenics.domain.model.Target(app.calisthenics.domain.model.TargetType.HOLD_SECONDS, 10, 30)
        val plan = planOf(blk("1", "dip-support-hold", 1, hold), blk("2", "dip-support-hold", 2, hold))
        val s = session("a", LocalDate.of(2026, 10, 2), "COMPLETED", work("1", "dip-support-hold", 33, null), work("2", "dip-support-hold", 33, 25))
        assertEquals(25, effectiveSetSeries(listOf(SessionWithPlan(s, plan)), zone).getValue("dip-support-hold").single().lowest)
        val allPlanned = session("b", LocalDate.of(2026, 10, 3), "COMPLETED", work("1", "dip-support-hold", 33, null), work("2", "dip-support-hold", 33, null))
        assertEquals(30, effectiveSetSeries(listOf(SessionWithPlan(allPlanned, plan)), zone).getValue("dip-support-hold").single().lowest)
    }

    @Test fun withoutAPlanTheLoggedNumbersAreUsed() {
        val s = session("a", LocalDate.of(2026, 10, 2), "COMPLETED", work("1", "pushup-standard", 30, 7), work("2", "pushup-standard", 30, null))
        assertEquals(7, effectiveSetSeries(listOf(SessionWithPlan(s, null)), zone).getValue("pushup-standard").single().lowest)
    }

    @Test fun theChartDatesAreOnEveryDotUpToSixThenEverySecondOneCountedBackFromTheLast() {   // R7.2-T08 (F8)
        assertEquals(setOf(0), chartDateIndexes(1)); assertEquals(setOf(0, 1), chartDateIndexes(2)); assertEquals((0..5).toSet(), chartDateIndexes(6))
        assertEquals(setOf(0, 2, 4, 6), chartDateIndexes(7))
        assertEquals(setOf(1, 3, 5, 7), chartDateIndexes(8))
        for (n in 7..12) { val idx = chartDateIndexes(n); assertTrue("last dot is dated for $n", n - 1 in idx); assertTrue("no neighbours for $n", idx.none { it + 1 in idx }) }
    }
}
