// T14 — pure session reducer (spec §6). No clock, no Android: time arrives in events as a
// monotonic millisecond value. The reducer returns new state + typed effects; adapters perform them.
//
// Timing model: while RUNNING, remaining time is derived from `deadlineMs - nowMs`; ticks only
// repaint/advance. Pause freezes `remainingMs`; resume sets a new deadline. A late tick that
// crosses several block boundaries advances through them one by one (no block is lost or doubled).
package app.calisthenics.domain.session

import app.calisthenics.domain.feedback.Execution
import app.calisthenics.domain.model.BlockType
import app.calisthenics.domain.model.WorkoutPlan

enum class Phase { READY, RUNNING, PAUSED, RECOVERY_REQUIRED, COMPLETED, PARTIAL_FINISHED }
enum class PauseReason { USER, AUDIO_INTERRUPTED, RECOVERY, EASIER_REPLACEMENT }
enum class FinishReason { ALL_BLOCKS_DONE, USER_FINISHED_EARLY, RECOVERED_PARTIAL }

data class SessionState(
    val sessionId: String,
    val plan: WorkoutPlan,
    val phase: Phase = Phase.READY,
    val blockIndex: Int = 0,
    /** Valid only while RUNNING. */
    val deadlineMs: Long = 0,
    /** Valid while PAUSED / READY / RECOVERY_REQUIRED. */
    val remainingMs: Long = 0,
    val pauseReason: PauseReason? = null,
    val executions: Map<String, Execution> = emptyMap(),
    /** Active milliseconds spent in each block (for PARTIAL). */
    val activeMs: Map<String, Long> = emptyMap(),
    val activeElapsedMs: Long = 0,
    val eventSequence: Long = 0,
    val finishReason: FinishReason? = null,
    /** Blocks replaced by the easier alternative: original id -> replacement id (provenance). */
    val replacements: Map<String, String> = emptyMap(),
    /** Set by recovery: up to this much work after the last checkpoint is unknown. */
    val unknownLossMs: Long = 0,
    val lastCheckpointSeq: Long = -1,
    val lastTickMs: Long = 0,
    val appliedCueIds: Set<String> = emptySet(),
    /** U07: what the user logged per work block while training (blockId -> log). Appended last with a default. */
    val logged: Map<String, BlockLog> = emptyMap(),
) {
    val currentBlock get() = plan.blocks.getOrNull(blockIndex)
    val isTerminal get() = phase == Phase.COMPLETED || phase == Phase.PARTIAL_FINISHED
    fun remainingAt(nowMs: Long): Long = if (phase == Phase.RUNNING) maxOf(0, deadlineMs - nowMs) else remainingMs
}

sealed interface SessionEvent {
    data class Start(val nowMs: Long) : SessionEvent
    data class Tick(val nowMs: Long) : SessionEvent
    data class Pause(val nowMs: Long, val reason: PauseReason = PauseReason.USER) : SessionEvent
    data class Resume(val nowMs: Long) : SessionEvent
    data class Skip(val nowMs: Long) : SessionEvent
    data class EarlyDone(val nowMs: Long) : SessionEvent
    /** [replacement] is the easier compatible block, supplied by the planner layer. */
    data class EasierAlternative(val nowMs: Long, val replacement: app.calisthenics.domain.model.TimelineBlock) : SessionEvent
    data class FinishEarly(val nowMs: Long) : SessionEvent
    /** Reps/seconds, "too hard" and discomfort for a work block that has started. Latest entry for a block wins. */
    data class LogBlock(val blockId: String, val log: BlockLog) : SessionEvent
    data class AudioInterrupted(val nowMs: Long) : SessionEvent
    data class PersistCheckpoint(val nowMs: Long) : SessionEvent
    /** Process restarted: old monotonic times are meaningless; [lastCheckpoint] is the stored state. */
    data object ProcessRecovered : SessionEvent
    data class RecoveryChoice(val resume: Boolean) : SessionEvent
}

sealed interface SessionEffect {
    data class Cue(val id: String, val kind: CueKind, val blockId: String) : SessionEffect
    /** Idempotent: (sessionId, sequence) is the unique key; adapters must ignore a duplicate. */
    data class Persist(val sessionId: String, val sequence: Long, val state: SessionState) : SessionEffect
    data class ShowBlock(val blockId: String) : SessionEffect
    data object ReleaseWakeLock : SessionEffect
    data object AcquireWakeLock : SessionEffect
}

enum class CueKind { START, ROUND, PREVIEW_NEXT, COUNTDOWN_3, COUNTDOWN_2, COUNTDOWN_1, FINISHED }

data class Reduced(val state: SessionState, val effects: List<SessionEffect> = emptyList())

const val PREVIEW_AT_SECONDS = 5

fun newSession(sessionId: String, plan: WorkoutPlan): SessionState {
    require(plan.blocks.isNotEmpty()) { "plan has no blocks" }
    return SessionState(sessionId, plan, remainingMs = plan.blocks.first().durationSeconds * 1000L)
}

fun reduce(s: SessionState, e: SessionEvent): Reduced {
    if (s.isTerminal) return Reduced(s) // terminal states are immutable (feedback edits live elsewhere)
    return when (e) {
        is SessionEvent.Start -> if (s.phase != Phase.READY) Reduced(s) else begin(s, e.nowMs)
        is SessionEvent.Tick -> if (s.phase != Phase.RUNNING) Reduced(s) else advance(s, e.nowMs)
        is SessionEvent.Pause -> pause(s, e.nowMs, e.reason)
        is SessionEvent.AudioInterrupted -> pause(s, e.nowMs, PauseReason.AUDIO_INTERRUPTED)
        is SessionEvent.Resume -> resume(s, e.nowMs)
        is SessionEvent.Skip -> if (s.phase == Phase.RUNNING || s.phase == Phase.PAUSED) skip(s, e.nowMs) else Reduced(s)
        is SessionEvent.EarlyDone -> earlyDone(s, e.nowMs)
        is SessionEvent.EasierAlternative -> easier(s, e)
        is SessionEvent.FinishEarly -> finishEarly(s, e.nowMs)
        is SessionEvent.LogBlock -> logBlock(s, e)
        is SessionEvent.PersistCheckpoint -> if (s.phase == Phase.RUNNING && e.nowMs - s.lastTickMs < 0) Reduced(s)
            else persistOnly(s)
        SessionEvent.ProcessRecovered -> recover(s)
        is SessionEvent.RecoveryChoice -> recoveryChoice(s, e.resume)
    }
}

// ---------------------------------------------------------------------------------------------

private fun bump(s: SessionState): SessionState = s.copy(eventSequence = s.eventSequence + 1)

private fun persist(s: SessionState): SessionEffect = SessionEffect.Persist(s.sessionId, s.eventSequence, s)

private fun persistOnly(s: SessionState): Reduced {
    if (s.lastCheckpointSeq == s.eventSequence) return Reduced(s) // idempotent
    val n = s.copy(lastCheckpointSeq = s.eventSequence)
    return Reduced(n, listOf(persist(n)))
}

private fun begin(s: SessionState, now: Long): Reduced {
    val b = s.plan.blocks.first()
    val n = bump(s.copy(
        phase = Phase.RUNNING, deadlineMs = now + b.durationSeconds * 1000L, lastTickMs = now,
        executions = s.executions + (b.id to Execution.RUNNING), appliedCueIds = s.appliedCueIds + "start-${b.id}",
    ))
    return Reduced(n, listOf(SessionEffect.AcquireWakeLock, SessionEffect.Cue("start-${b.id}", CueKind.START, b.id),
        SessionEffect.ShowBlock(b.id), persist(n)))
}

private fun addActive(s: SessionState, now: Long): SessionState {
    val b = s.currentBlock ?: return s
    val spent = (now - s.lastTickMs).coerceAtLeast(0)
    val cap = b.durationSeconds * 1000L - (s.activeMs[b.id] ?: 0L)
    val add = minOf(spent, maxOf(0, cap))
    return s.copy(activeMs = s.activeMs + (b.id to (s.activeMs[b.id] ?: 0L) + add), activeElapsedMs = s.activeElapsedMs + add, lastTickMs = now)
}

/** Advance through every block whose deadline has passed; emits countdown/preview cues once per id. */
private fun advance(s0: SessionState, now: Long): Reduced {
    var s = s0
    val fx = mutableListOf<SessionEffect>()
    var guard = 0
    while (s.phase == Phase.RUNNING && guard++ < 10_000) {
        val b = s.currentBlock!!
        if (now >= s.deadlineMs) {
            // block ends at its deadline (not at `now`): late callbacks never lengthen it
            val endAt = s.deadlineMs
            s = addActive(s, endAt)
            s = s.copy(executions = s.executions + (b.id to Execution.COMPLETED))
            val next = s.blockIndex + 1
            if (next >= s.plan.blocks.size) {
                s = bump(s.copy(phase = Phase.COMPLETED, finishReason = FinishReason.ALL_BLOCKS_DONE, remainingMs = 0))
                fx += SessionEffect.Cue("finished", CueKind.FINISHED, b.id); fx += SessionEffect.ReleaseWakeLock; fx += persist(s)
                return Reduced(s, fx)
            }
            val nb = s.plan.blocks[next]
            s = bump(s.copy(blockIndex = next, deadlineMs = endAt + nb.durationSeconds * 1000L, lastTickMs = endAt,
                executions = s.executions + (nb.id to Execution.RUNNING)))
            fx += blockStartEffects(s, nb); fx += persist(s)
        } else break
    }
    if (s.phase == Phase.RUNNING) {
        s = addActive(s, now)
        fx += cuesFor(s, now).also { s = s.copy(appliedCueIds = s.appliedCueIds + it.map { c -> c.id }) }
    }
    return Reduced(s, fx)
}

private fun blockStartEffects(s: SessionState, nb: app.calisthenics.domain.model.TimelineBlock): List<SessionEffect> {
    val out = mutableListOf<SessionEffect>(SessionEffect.ShowBlock(nb.id))
    val id = "start-${nb.id}"
    if (id !in s.appliedCueIds) out += SessionEffect.Cue(id, CueKind.START, nb.id)
    return out
}

private fun cuesFor(s: SessionState, now: Long): List<SessionEffect.Cue> {
    val b = s.currentBlock ?: return emptyList()
    val rem = (s.deadlineMs - now).coerceAtLeast(0)
    val out = mutableListOf<SessionEffect.Cue>()
    fun add(id: String, k: CueKind) { if (id !in s.appliedCueIds) out += SessionEffect.Cue(id, k, b.id) }
    val dur = b.durationSeconds
    val hasNext = s.blockIndex + 1 < s.plan.blocks.size
    // preview suppressed on short blocks so it never overlaps the countdown (ADR D1)
    if (hasNext && dur > PREVIEW_AT_SECONDS + 3 && rem <= PREVIEW_AT_SECONDS * 1000L && rem > 3000L) add("preview-${b.id}", CueKind.PREVIEW_NEXT)
    if (hasNext) {
        if (dur > 3 && rem <= 3000L && rem > 2000L) add("c3-${b.id}", CueKind.COUNTDOWN_3)
        if (dur > 3 && rem <= 2000L && rem > 1000L) add("c2-${b.id}", CueKind.COUNTDOWN_2)
        if (dur > 3 && rem <= 1000L && rem > 0L) add("c1-${b.id}", CueKind.COUNTDOWN_1)
    }
    return out.take(1) // one spoken cue per tick: never overlap utterances
}

private fun pause(s: SessionState, now: Long, reason: PauseReason): Reduced {
    if (s.phase != Phase.RUNNING) return Reduced(s)
    val t = addActive(s, now)
    val rem = (t.deadlineMs - now).coerceAtLeast(0)
    val n = bump(t.copy(phase = Phase.PAUSED, remainingMs = rem, pauseReason = reason))
    return Reduced(n, listOf(SessionEffect.ReleaseWakeLock, persist(n)))
}

private fun resume(s: SessionState, now: Long): Reduced {
    if (s.phase != Phase.PAUSED) return Reduced(s)
    val n = bump(s.copy(phase = Phase.RUNNING, deadlineMs = now + s.remainingMs, lastTickMs = now, pauseReason = null))
    return Reduced(n, listOf(SessionEffect.AcquireWakeLock, persist(n)))
}

private fun skip(s: SessionState, now: Long): Reduced {
    val b = s.currentBlock ?: return Reduced(s)
    val t = if (s.phase == Phase.RUNNING) addActive(s, now) else s
    val exec = if (b.type == BlockType.WORK) Execution.SKIPPED else Execution.SKIPPED
    return moveOn(t.copy(executions = t.executions + (b.id to exec)), now)
}

/**
 * "Done": the user finished the work before the window ended, so the session moves straight to what follows
 * (the recovery, which is the stretch when stretch is on — the unused part of the window is simply not used).
 * Reps: completed. Holds: completed only if the target time was actually held, otherwise partial.
 */
private fun earlyDone(s: SessionState, now: Long): Reduced {
    val b = s.currentBlock ?: return Reduced(s)
    if (s.phase != Phase.RUNNING || b.type != BlockType.WORK) return Reduced(s)
    val t = addActive(s, now)
    val heldEnough = b.target?.type != app.calisthenics.domain.model.TargetType.HOLD_SECONDS ||
        (t.activeMs[b.id] ?: 0L) >= (b.target?.value ?: 0) * 1000L
    return moveOn(t.copy(executions = t.executions + (b.id to if (heldEnough) Execution.COMPLETED else Execution.PARTIAL)), now)
}

private fun logBlock(s: SessionState, e: SessionEvent.LogBlock): Reduced {
    val b = s.plan.blocks.firstOrNull { it.id == e.blockId } ?: return Reduced(s)
    val started = (s.executions[b.id] ?: Execution.NOT_STARTED) != Execution.NOT_STARTED
    if (b.type != BlockType.WORK || !started) return Reduced(s)
    val n = bump(s.copy(logged = s.logged + (b.id to e.log.copy(reps = e.log.reps?.coerceIn(0, 999)))))
    return Reduced(n, listOf(persist(n))) // saved to the crash-safe checkpoint right away
}

private fun moveOn(t: SessionState, now: Long): Reduced {
    val next = t.blockIndex + 1
    if (next >= t.plan.blocks.size) {
        val n = bump(t.copy(phase = Phase.COMPLETED, finishReason = FinishReason.ALL_BLOCKS_DONE, remainingMs = 0))
        return Reduced(n, listOf(SessionEffect.Cue("finished", CueKind.FINISHED, t.currentBlock!!.id), SessionEffect.ReleaseWakeLock, persist(n)))
    }
    val nb = t.plan.blocks[next]
    val running = t.phase == Phase.RUNNING
    val n = bump(t.copy(blockIndex = next, deadlineMs = if (running) now + nb.durationSeconds * 1000L else 0,
        remainingMs = nb.durationSeconds * 1000L, lastTickMs = now,
        executions = if (running) t.executions + (nb.id to Execution.RUNNING) else t.executions))
    return Reduced(n, blockStartEffects(n, nb) + persist(n))
}

private fun easier(s: SessionState, e: SessionEvent.EasierAlternative): Reduced {
    val b = s.currentBlock ?: return Reduced(s)
    if (b.type != BlockType.WORK) return Reduced(s)
    val t = if (s.phase == Phase.RUNNING) addActive(s, e.nowMs) else s
    require(e.replacement.id != b.id && t.plan.blocks.none { it.id == e.replacement.id }) { "replacement id must be new" }
    val blocks = t.plan.blocks.toMutableList().also { it.add(t.blockIndex + 1, e.replacement) }
    // Original keeps its recorded partial work and grants no success; replacement is a recorded amendment.
    val n = bump(t.copy(
        plan = t.plan.copy(blocks = blocks, plannedDurationSeconds = blocks.sumOf { it.durationSeconds }),
        phase = Phase.PAUSED, pauseReason = PauseReason.EASIER_REPLACEMENT,
        remainingMs = e.replacement.durationSeconds * 1000L,
        executions = t.executions + (b.id to Execution.PARTIAL),
        replacements = t.replacements + (b.id to e.replacement.id),
        blockIndex = t.blockIndex + 1,
    ))
    return Reduced(n, listOf(SessionEffect.ReleaseWakeLock, SessionEffect.ShowBlock(e.replacement.id), persist(n)))
}

private fun finishEarly(s: SessionState, now: Long): Reduced {
    val t = if (s.phase == Phase.RUNNING) addActive(s, now) else s
    val ex = t.executions.toMutableMap()
    t.currentBlock?.let { b -> if (ex[b.id] == Execution.RUNNING) ex[b.id] = if ((t.activeMs[b.id] ?: 0L) > 0) Execution.PARTIAL else Execution.NOT_STARTED }
    val n = bump(t.copy(phase = Phase.PARTIAL_FINISHED, finishReason = FinishReason.USER_FINISHED_EARLY, executions = ex))
    return Reduced(n, listOf(SessionEffect.ReleaseWakeLock, persist(n)))
}

private fun recover(s: SessionState): Reduced {
    if (s.phase != Phase.RUNNING && s.phase != Phase.PAUSED) return Reduced(s)
    // Never apply old monotonic deadlines or wall-clock gaps. Whatever block was running is frozen
    // with its last checkpointed remaining time; up to 1 s of work after the checkpoint is unknown.
    val b = s.currentBlock
    val rem = if (s.phase == Phase.PAUSED) s.remainingMs else (b?.durationSeconds?.times(1000L) ?: 0L) - (b?.let { s.activeMs[it.id] } ?: 0L)
    val ex = s.executions.toMutableMap()
    b?.let { if (ex[it.id] == Execution.RUNNING) ex[it.id] = Execution.PARTIAL }
    val n = bump(s.copy(phase = Phase.RECOVERY_REQUIRED, remainingMs = rem.coerceAtLeast(0), deadlineMs = 0,
        unknownLossMs = 1000, executions = ex, pauseReason = PauseReason.RECOVERY))
    return Reduced(n, listOf(SessionEffect.ReleaseWakeLock, persist(n)))
}

private fun recoveryChoice(s: SessionState, resume: Boolean): Reduced {
    if (s.phase != Phase.RECOVERY_REQUIRED) return Reduced(s)
    val n = if (resume) bump(s.copy(phase = Phase.PAUSED, pauseReason = PauseReason.RECOVERY,
        executions = s.currentBlock?.let { s.executions + (it.id to Execution.PARTIAL) } ?: s.executions))
    else bump(s.copy(phase = Phase.PARTIAL_FINISHED, finishReason = FinishReason.RECOVERED_PARTIAL))
    return Reduced(n, listOf(persist(n)))
}

/** Replay helper for tests/traces: feed events, collect the full effect list. */
fun run(initial: SessionState, events: List<SessionEvent>): Pair<SessionState, List<SessionEffect>> {
    var s = initial; val fx = mutableListOf<SessionEffect>()
    for (e in events) { val r = reduce(s, e); s = r.state; fx += r.effects }
    return s to fx
}
