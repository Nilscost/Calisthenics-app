// U12 (§3.8): what the History tab shows for one workout and for a week. Pure; callers pass the zone.
package app.calisthenics.domain.history

import app.calisthenics.domain.feedback.Rating
import app.calisthenics.domain.model.BlockType
import app.calisthenics.domain.model.Side
import app.calisthenics.domain.model.Target
import app.calisthenics.domain.model.TargetType
import app.calisthenics.domain.model.WorkoutPlan
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters

data class SessionOverview(val sessionId: String, val day: LocalDate, val startedAtEpochMs: Long, val minutes: Int, val rounds: Int, val exercises: Int, val finishedEarly: Boolean)

/** Rounds and exercises count only work that was actually done (outcome MET or PARTIAL), never skipped blocks. */
fun overviewOf(plan: WorkoutPlan?, s: SessionRecord, zone: ZoneId): SessionOverview {
    val done = s.blocks.filter { it.type == "WORK" && (it.outcome == "MET" || it.outcome == "PARTIAL") }
    val planned = plan?.blocks?.filter { it.type == BlockType.WORK }?.associateBy { it.id }.orEmpty()
    val rounds = done.mapNotNull { planned[it.blockId]?.roundIndex }.distinct().size
    val exercises = done.mapNotNull { it.variationId ?: planned[it.blockId]?.variationId }.distinct().size
    val seconds = s.endedAtEpochMs?.let { ((it - s.startedAtEpochMs) / 1000L).toInt() } ?: s.blocks.sumOf { it.actualSeconds }
    return SessionOverview(s.sessionId, Instant.ofEpochMilli(s.startedAtEpochMs).atZone(zone).toLocalDate(), s.startedAtEpochMs,
        (seconds + 30) / 60, rounds, exercises, s.status == "PARTIAL_FINISHED")
}

data class RoundEntry(val round: Int, val side: Side, val blockId: String, val outcome: String, /** null = not typed (assumed as planned) */ val reps: Int?)

data class ExerciseDetail(val variationId: String, val target: Target?, val rounds: List<RoundEntry>) {
    /** The lowest typed round: what the progression evidence uses. */
    val lowest: Int? get() = rounds.mapNotNull { it.reps }.minOrNull()
    val isHold: Boolean get() = target?.type == TargetType.HOLD_SECONDS
}

/** One entry per exercise of the session, with what was logged in each round (read from the block results). */
fun exerciseDetails(plan: WorkoutPlan, blocks: List<BlockRecord>): List<ExerciseDetail> {
    val results = blocks.associateBy { it.blockId }
    return plan.blocks.filter { it.type == BlockType.WORK && it.variationId != null && results[it.id]?.outcome.let { o -> o == "MET" || o == "PARTIAL" } }
        .groupBy { it.variationId!! }
        .map { (vid, bs) ->
            ExerciseDetail(vid, bs.first().target, bs.map { b -> RoundEntry(b.roundIndex ?: 1, b.side, b.id, results.getValue(b.id).outcome, results.getValue(b.id).achievedValue) })
        }
}

/** The rating a corrected number implies (spec §5): below the target is BELOW, otherwise MET unless the user said "too hard". */
fun ratingFor(reps: Int?, target: Target?, tooHard: Boolean): Rating =
    if (tooHard || (reps != null && target != null && reps < target.value)) Rating.BELOW else Rating.MET

fun weekStartOf(day: LocalDate): LocalDate = day.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
fun weekDays(weekStart: LocalDate): List<LocalDate> = (0L..6L).map { weekStart.plusDays(it) }
/** Unfinished sessions (RUNNING, RECOVERY_REQUIRED) are not shown as workouts. */
fun finishedSessions(sessions: List<SessionRecord>) = sessions.filter { it.status == "COMPLETED" || it.status == "PARTIAL_FINISHED" }
