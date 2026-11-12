// The past sessions of ONE exercise, newest first: what the exercise detail page (Progress) shows below its levels. Pure; callers pass the zone.
package app.calisthenics.domain.history

import app.calisthenics.domain.feedback.Rating
import app.calisthenics.domain.feedback.effectiveRounds
import app.calisthenics.domain.model.BlockType
import app.calisthenics.domain.model.TargetType
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone

/**
 * One session of one exercise. [values] = the number of every done set after corrections (reps, or seconds for holds; a set left "as planned" shows its target);
 * [level] = the level it was prescribed at (null = free / mixed prescription). [good] = every set reached its target, none rated below or with pain: the
 * session the progression counts towards the next level (the same test as a qualifying session; the 14/30-day window is applied by the progression itself).
 */
data class ExerciseHistoryEntry(
    val sessionId: String, val day: LocalDate, val startedAtEpochMs: Long, val level: Int?, val isHold: Boolean,
    val values: List<Int>, val alternating: Boolean, val good: Boolean,
)

fun exerciseHistory(sessions: List<SessionWithPlan>, variationId: String, zone: TimeZone): List<ExerciseHistoryEntry> =
    sessions.filter { it.record.status == "COMPLETED" || it.record.status == "PARTIAL_FINISHED" }
        .sortedByDescending { it.record.startedAtEpochMs }
        .mapNotNull { s ->
            val done = s.record.blocks.filter { it.type == "WORK" && it.variationId == variationId && (it.outcome == "MET" || it.outcome == "PARTIAL") }
            if (done.isEmpty()) return@mapNotNull null
            val plan = s.plan
            val day = day(s.record.startedAtEpochMs, zone)
            if (plan == null) {
                return@mapNotNull ExerciseHistoryEntry(s.record.sessionId, day, s.record.startedAtEpochMs, null, false, done.mapNotNull { it.achievedValue }, false, false)
            }
            val work = plan.blocks.filter { it.type == BlockType.WORK && it.variationId == variationId }
            val byId = work.associateBy { it.id }
            val rounds = effectiveRounds(plan, variationId, done.associate { it.blockId to it.achievedValue }, s.rows)
            val values = rounds.mapNotNull { r -> r.value ?: byId[r.blockId]?.target?.shownValue() }
            val allDone = work.isNotEmpty() && done.size == work.size && done.all { it.outcome == "MET" }
            val reached = rounds.all { r ->
                val t = byId[r.blockId]?.judgedValue; val v = r.value ?: byId[r.blockId]?.target?.shownValue()
                t == null || (v != null && v >= t)
            }
            val good = allDone && reached && rounds.none { it.rating == Rating.BELOW || it.discomfort }
            ExerciseHistoryEntry(s.record.sessionId, day, s.record.startedAtEpochMs, work.mapNotNull { it.prescriptionTier }.distinct().singleOrNull(),
                work.firstOrNull()?.target?.type == TargetType.HOLD_SECONDS, values, work.firstOrNull()?.alternating == true, good)
        }
