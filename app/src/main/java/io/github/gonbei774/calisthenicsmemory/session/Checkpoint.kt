// T15: crash-safe checkpoint. The service writes this every few seconds and at block changes; after a crash or
// process death the user is offered recovery. Pure codec (testable on the JVM) + a tiny SharedPreferences store.
package io.github.gonbei774.calisthenicsmemory.session

import android.content.Context
import app.calisthenics.domain.feedback.Execution
import app.calisthenics.domain.model.WorkoutPlan
import app.calisthenics.domain.session.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class Checkpoint(
    val sessionId: String, val plan: WorkoutPlan, val phase: Phase, val blockIndex: Int, val remainingMs: Long,
    val executions: Map<String, Execution>, val activeMs: Map<String, Long>, val activeElapsedMs: Long,
    val eventSequence: Long, val replacements: Map<String, String>, val appliedCueIds: Set<String>,
    val pauseReason: PauseReason?, val startedAtEpochMs: Long, val savedAtEpochMs: Long,
    /** U07: per-block logs, so a crash never loses what the user typed. Appended last; older checkpoints decode with none. */
    val logged: Map<String, BlockLog> = emptyMap(),
)

object CheckpointCodec {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    /** [nowMs] is the monotonic clock, used to freeze the remaining time of a RUNNING block. */
    fun encode(s: SessionState, startedAt: Long, nowMs: Long, wallMs: Long): String = json.encodeToString(Checkpoint.serializer(),
        Checkpoint(s.sessionId, s.plan, s.phase, s.blockIndex, s.remainingAt(nowMs), s.executions, s.activeMs, s.activeElapsedMs,
            s.eventSequence, s.replacements, s.appliedCueIds, s.pauseReason, startedAt, wallMs, s.logged))

    fun decode(text: String): Checkpoint? = try { json.decodeFromString(Checkpoint.serializer(), text) } catch (_: Exception) { null }

    /** Rebuilds a state in its pre-crash phase; the caller then feeds ProcessRecovered (never trusts old deadlines). */
    fun toState(c: Checkpoint) = SessionState(c.sessionId, c.plan, c.phase, c.blockIndex, 0, c.remainingMs, c.pauseReason,
        c.executions, c.activeMs, c.activeElapsedMs, c.eventSequence, null, c.replacements, 0, -1, 0, c.appliedCueIds, c.logged)
}

object CheckpointStore {
    private const val PREFS = "workout_checkpoint"; private const val KEY = "json"
    fun write(ctx: Context, text: String) { ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY, text).commit() }
    fun read(ctx: Context): Checkpoint? = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, null)?.let { CheckpointCodec.decode(it) }
    fun clear(ctx: Context) { ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().remove(KEY).commit() }
}
