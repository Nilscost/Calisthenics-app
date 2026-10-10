// V27b (doc 17 §2.5): what the History tab adds on top of the week strip: three summary tiles, and the lowest set per session for each exercise.
package app.calisthenics.domain.history

import app.calisthenics.domain.model.WorkoutPlan
import app.calisthenics.domain.progression.ProgressEvent
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters

/** Sessions this week, level-ups this month and work minutes this week (finished sessions only). */
data class HistoryTiles(val sessionsThisWeek: Int, val levelUpsThisMonth: Int, val workMinutesThisWeek: Int)

/** A progress event that is a step forward: a level earned, all five levels done, or a routine's rule met. */
fun isLevelUp(e: ProgressEvent): Boolean =
    (e.message.startsWith("Tier ") && " achieved after " in e.message) || e.message.startsWith("All five tiers earned") || e.message.startsWith("Every set of ")

private fun day(ms: Long, zone: ZoneId) = Instant.ofEpochMilli(ms).atZone(zone).toLocalDate()

fun historyTiles(sessions: List<SessionRecord>, levelUpDays: List<LocalDate>, today: LocalDate, zone: ZoneId): HistoryTiles {
    val done = sessions.filter { it.status == "COMPLETED" || it.status == "PARTIAL_FINISHED" }
    val monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    val week = done.filter { day(it.startedAtEpochMs, zone) in monday..monday.plusDays(6) }
    val workSeconds = week.flatMap { it.blocks }.filter { it.type == "WORK" && it.outcome != "SKIPPED" }.sumOf { it.actualSeconds }
    return HistoryTiles(week.size, levelUpDays.count { it.year == today.year && it.month == today.month }, workSeconds / 60)
}

data class SetPoint(val day: LocalDate, val lowest: Int)

/**
 * For every exercise, the lowest number (reps or seconds) of any set of each finished session, oldest first. The lowest set is the one
 * that gates a level, so it is what the chart shows. Uses what was logged per block; later corrections are not applied here.
 */
fun lowestSetSeries(sessions: List<SessionRecord>, zone: ZoneId): Map<String, List<SetPoint>> {
    val out = linkedMapOf<String, MutableList<SetPoint>>()
    for (s in sessions.filter { it.status == "COMPLETED" || it.status == "PARTIAL_FINISHED" }.sortedBy { it.startedAtEpochMs }) {
        val d = day(s.startedAtEpochMs, zone)
        s.blocks.filter { it.type == "WORK" && it.variationId != null && (it.outcome == "MET" || it.outcome == "PARTIAL") && it.achievedValue != null }
            .groupBy { it.variationId!! }.forEach { (v, bs) -> out.getOrPut(v) { mutableListOf() } += SetPoint(d, bs.minOf { it.achievedValue!! }) }
    }
    return out
}
