// V27b (doc 17 §2.5): what the History tab adds on top of the week strip: three summary tiles, and the lowest set per session for each exercise.
package app.calisthenics.domain.history

import app.calisthenics.domain.feedback.FeedbackRow
import app.calisthenics.domain.feedback.effectiveRounds
import app.calisthenics.domain.model.BlockType
import app.calisthenics.domain.model.TargetType
import app.calisthenics.domain.model.WorkoutPlan
import app.calisthenics.domain.progression.ProgressEvent
import kotlinx.datetime.*
import kotlin.time.Instant

/** Sessions this week, level-ups this month and work minutes this week (finished sessions only). */
data class HistoryTiles(val sessionsThisWeek: Int, val levelUpsThisMonth: Int, val workMinutesThisWeek: Int)

/** A progress event that is a step forward: a level earned, all five levels done, or a routine's rule met. */
fun isLevelUp(e: ProgressEvent): Boolean =
    (e.message.startsWith("Level ") && " done after " in e.message) || e.message.startsWith("All five levels done") || e.message.startsWith("Every set of ")

private fun day(ms: Long, zone: TimeZone) = Instant.fromEpochMilliseconds(ms).toLocalDateTime(zone).date

fun historyTiles(sessions: List<SessionRecord>, levelUpDays: List<LocalDate>, today: LocalDate, zone: TimeZone): HistoryTiles {
    val done = sessions.filter { it.status == "COMPLETED" || it.status == "PARTIAL_FINISHED" }
    val monday = today.mondayOfWeek()
    val week = done.filter { day(it.startedAtEpochMs, zone) in monday..monday.plus(6, DateTimeUnit.DAY) }
    val workSeconds = week.flatMap { it.blocks }.filter { it.type == "WORK" && it.outcome != "SKIPPED" }.sumOf { it.actualSeconds }
    return HistoryTiles(week.size, levelUpDays.count { it.year == today.year && it.month == today.month }, workSeconds / 60)
}

data class SetPoint(val day: LocalDate, val lowest: Int)

/** R7.2-T08 (F8): which dots of the progression chart get a date under them: all up to 6, else every second one counted back from the last, so the newest always has its date and no two labels sit side by side. */
fun chartDateIndexes(count: Int): Set<Int> =
    if (count <= 6) (0 until count).toSet() else (0 until count).filter { (count - 1 - it) % 2 == 0 }.toSet()

/**
 * For every exercise, the lowest number (reps or seconds) of any set of each finished session, oldest first. The lowest set is the one
 * that gates a level, so it is what the chart shows. Uses what was logged per block; later corrections are not applied here.
 */
fun lowestSetSeries(sessions: List<SessionRecord>, zone: TimeZone): Map<String, List<SetPoint>> {
    val out = linkedMapOf<String, MutableList<SetPoint>>()
    for (s in sessions.filter { it.status == "COMPLETED" || it.status == "PARTIAL_FINISHED" }.sortedBy { it.startedAtEpochMs }) {
        val d = day(s.startedAtEpochMs, zone)
        s.blocks.filter { it.type == "WORK" && it.variationId != null && (it.outcome == "MET" || it.outcome == "PARTIAL") && it.achievedValue != null }
            .groupBy { it.variationId!! }.forEach { (v, bs) -> out.getOrPut(v) { mutableListOf() } += SetPoint(d, bs.minOf { it.achievedValue!! }) }
    }
    return out
}

/** A finished session with the plan it came from and its stored feedback revisions (corrections). */
data class SessionWithPlan(val record: SessionRecord, val plan: WorkoutPlan?, val rows: List<FeedbackRow> = emptyList())

/**
 * R7-T06 (B7, B8): the lowest set of each finished session per exercise, with the numbers the person effectively did:
 * a set left "as planned" counts as its target (the top of a range, as the logger pre-fills it), corrections are applied,
 * and holds are in seconds. Sessions without a stored plan fall back to the numbers logged per block.
 */
fun effectiveSetSeries(sessions: List<SessionWithPlan>, zone: TimeZone): Map<String, List<SetPoint>> {
    val out = linkedMapOf<String, MutableList<SetPoint>>()
    for (s in sessions.filter { it.record.status == "COMPLETED" || it.record.status == "PARTIAL_FINISHED" }.sortedBy { it.record.startedAtEpochMs }) {
        val d = day(s.record.startedAtEpochMs, zone)
        val done = s.record.blocks.filter { it.type == "WORK" && it.variationId != null && (it.outcome == "MET" || it.outcome == "PARTIAL") }
        val plan = s.plan
        if (plan == null) {
            done.filter { it.achievedValue != null }.groupBy { it.variationId!! }.forEach { (v, bs) -> out.getOrPut(v) { mutableListOf() } += SetPoint(d, bs.minOf { it.achievedValue!! }) }
            continue
        }
        val achieved = done.associate { it.blockId to it.achievedValue }
        for (vid in done.mapNotNull { it.variationId }.distinct()) {
            val byId = plan.blocks.associateBy { it.id }
            val values = effectiveRounds(plan, vid, achieved, s.rows).mapNotNull { r ->
                r.value ?: byId[r.blockId]?.target?.shownValue()
            }
            if (values.isNotEmpty()) out.getOrPut(vid) { mutableListOf() } += SetPoint(d, values.min())
        }
    }
    return out
}
