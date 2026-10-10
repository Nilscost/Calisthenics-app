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
}
