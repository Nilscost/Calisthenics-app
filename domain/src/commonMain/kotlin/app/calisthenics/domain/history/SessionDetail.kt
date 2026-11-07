// U12 (§3.8): what the History tab shows for one workout and for a week. Pure; callers pass the zone.
package app.calisthenics.domain.history

import app.calisthenics.domain.feedback.Execution
import app.calisthenics.domain.feedback.Rating
import app.calisthenics.domain.model.BlockType
import app.calisthenics.domain.model.Side
import app.calisthenics.domain.model.Target
import app.calisthenics.domain.model.TargetType
import app.calisthenics.domain.model.WorkoutPlan
import app.calisthenics.domain.session.Phase
import app.calisthenics.domain.session.SessionState
import kotlinx.datetime.*
import kotlin.time.Instant

data class SessionOverview(val sessionId: String, val day: LocalDate, val startedAtEpochMs: Long, val minutes: Int, val rounds: Int, val exercises: Int, val finishedEarly: Boolean, val format: app.calisthenics.domain.model.WorkoutFormat = app.calisthenics.domain.model.WorkoutFormat.CIRCUIT)

/** The outcome stored for a block of a finished session (the one rule for the saved row and the end screen). */
fun outcomeOf(ex: Execution): String = when (ex) { Execution.COMPLETED -> "MET"; Execution.PARTIAL -> "PARTIAL"; else -> "SKIPPED" }

/**
 * The record a finished [s] will be saved as, so the end screen can show exactly what History will show ([overviewOf]).
 * [endedAtEpochMs] null (or [startedAtEpochMs] unknown) falls back to the active seconds, as for an unfinished record.
 */
fun recordOfState(s: SessionState, startedAtEpochMs: Long, endedAtEpochMs: Long?): SessionRecord {
    val blocks = s.plan.blocks.filter { it.type == BlockType.WORK || it.type == BlockType.STRETCH }.map { b ->
        BlockRecord(b.id, b.variationId, b.type.name, outcomeOf(s.executions[b.id] ?: Execution.NOT_STARTED),
            ((s.activeMs[b.id] ?: 0L) / 1000L).toInt(), s.logged[b.id]?.reps)
    }
    val known = startedAtEpochMs > 0L && endedAtEpochMs != null && endedAtEpochMs >= startedAtEpochMs
    return SessionRecord(s.sessionId, s.plan.id, startedAtEpochMs, if (known) endedAtEpochMs else null,
        if (s.phase == Phase.COMPLETED) "COMPLETED" else "PARTIAL_FINISHED", blocks)
}

/** Rounds and exercises count only work that was actually done (outcome MET or PARTIAL), never skipped blocks. */
fun overviewOf(plan: WorkoutPlan?, s: SessionRecord, zone: TimeZone): SessionOverview {
    val done = s.blocks.filter { it.type == "WORK" && (it.outcome == "MET" || it.outcome == "PARTIAL") }
    val planned = plan?.blocks?.filter { it.type == BlockType.WORK }?.associateBy { it.id }.orEmpty()
    val rounds = done.mapNotNull { planned[it.blockId]?.roundIndex }.distinct().size
    val exercises = done.mapNotNull { it.variationId ?: planned[it.blockId]?.variationId }.distinct().size
    val seconds = s.endedAtEpochMs?.let { ((it - s.startedAtEpochMs) / 1000L).toInt() } ?: s.blocks.sumOf { it.actualSeconds }
    return SessionOverview(s.sessionId, Instant.fromEpochMilliseconds(s.startedAtEpochMs).toLocalDateTime(zone).date, s.startedAtEpochMs,
        (seconds + 30) / 60, rounds, exercises, s.status == "PARTIAL_FINISHED", plan?.format ?: app.calisthenics.domain.model.WorkoutFormat.CIRCUIT)
}

data class RoundEntry(val round: Int, val side: Side, val blockId: String, val outcome: String, /** null = not typed (assumed as planned) */ val reps: Int?)

data class ExerciseDetail(val variationId: String, val target: Target?, val rounds: List<RoundEntry>, /** One block worked both sides: the numbers are per side. */ val alternating: Boolean = false) {
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
            ExerciseDetail(vid, bs.first().target, bs.map { b -> RoundEntry(b.roundIndex ?: 1, b.side, b.id, results.getValue(b.id).outcome, results.getValue(b.id).achievedValue) }, bs.first().alternating)
        }
}

/** The rating a corrected number implies (spec §5): below the target is BELOW, otherwise MET unless the user said "too hard". */
fun ratingFor(reps: Int?, target: Target?, tooHard: Boolean, tooEasy: Boolean = false): Rating =
    if (tooHard || (reps != null && target != null && reps < target.value)) Rating.BELOW else if (tooEasy) Rating.ABOVE else Rating.MET

internal fun LocalDate.mondayOfWeek(): LocalDate = minus(dayOfWeek.ordinal, DateTimeUnit.DAY)

fun weekStartOf(day: LocalDate): LocalDate = day.mondayOfWeek()
fun weekDays(weekStart: LocalDate): List<LocalDate> = (0L..6L).map { weekStart.plus(it.toInt(), DateTimeUnit.DAY) }
/** Unfinished sessions (RUNNING, RECOVERY_REQUIRED) are not shown as workouts. */
fun finishedSessions(sessions: List<SessionRecord>) = sessions.filter { it.status == "COMPLETED" || it.status == "PARTIAL_FINISHED" }
