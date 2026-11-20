// Phase 4a: the platform-independent session owner. It was the body of Android's WorkoutSessionService and is now shared:
// the pure reducer decides, this class performs the effects (clock ticks, checkpointing, cues, saving the finished session)
// and publishes every state on SessionBus. Platform specifics (foreground notification, wake lock, SoundPool, Web Audio)
// stay behind [SessionHost] and [CuePlayer].
package io.github.gonbei774.calisthenicsmemory.session

import app.calisthenics.data.history.BlockRow
import app.calisthenics.data.history.FeedbackRow
import app.calisthenics.data.history.PlanRecord
import app.calisthenics.data.history.SessionRow
import app.calisthenics.domain.content.parseCatalog
import app.calisthenics.domain.feedback.Execution
import app.calisthenics.domain.history.outcomeOf
import app.calisthenics.domain.model.BlockType
import app.calisthenics.domain.model.WorkoutPlan
import app.calisthenics.domain.session.*
import io.github.gonbei774.calisthenicsmemory.platform.PlatformContext
import io.github.gonbei774.calisthenicsmemory.platform.elapsedRealtimeMs
import io.github.gonbei774.calisthenicsmemory.platform.historyRepository
import io.github.gonbei774.calisthenicsmemory.platform.nowEpochMs
import io.github.gonbei774.calisthenicsmemory.platform.readCatalogText
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import kotlin.concurrent.Volatile

/** Beeps and voice. Android: SoundPool + TextToSpeech. Web: Web Audio + speechSynthesis. Never blocks session time. */
interface CuePlayer {
    /** Starts loading samples/voices and registers for preference changes. */
    fun load()
    /** Blocks (Android: up to 5 s for SoundPool) until samples are ready; the session timer starts afterwards. */
    fun awaitLoaded() {}
    /** Re-reads the saved volume/voice preferences. */
    fun refreshPreferences()
    fun cue(kind: CueKind)
    fun say(text: String)
    fun release()
}

/** What the platform adds around a running session. */
interface SessionHost {
    val cues: CuePlayer
    /** A workout is starting: keep the device awake for at most [maxMs] (Android wake lock, web Screen Wake Lock). */
    fun keepAwakeStart(maxMs: Long)
    /** The reducer asked for the wake lock (AcquireWakeLock effect): re-acquire it if it was dropped. */
    fun keepAwakeEnsure()
    /** Phone call or ringtone is active (Android); the runner then reports AudioInterrupted. */
    fun audioInterrupted(): Boolean = false
    /** The visible block changed or the session was paused/resumed (Android: refresh the notification). */
    fun onBlockChanged(s: SessionState) {}
    /** The session ended and was saved (or saving failed): release everything the host acquired. */
    fun onFinished()
    fun logWarn(message: String) {}
    fun logError(message: String, t: Throwable? = null) {}
}

/**
 * One runner per workout. All events are applied one at a time (a mutex over a serial dispatcher), exactly as the old
 * single-threaded executor did. [now] is the monotonic clock of the timers, [wall] the wall clock of persisted times.
 */
class SessionRunner(
    private val ctx: PlatformContext,
    private val host: SessionHost,
    dispatcher: CoroutineDispatcher,
    private val now: () -> Long = ::elapsedRealtimeMs,
    private val wall: () -> Long = ::nowEpochMs,
    private val tickMs: Long = TICK_MS,
) {
    companion object {
        const val TICK_MS = 200L
        private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    }

    private val job = SupervisorJob()
    private val scope = CoroutineScope(dispatcher + job + CoroutineExceptionHandler { _, t -> host.logError("session task failed", t) })
    private val mutex = Mutex()
    private var state: SessionState? = null
    private var startedAt = 0L
    private var lastCkptMs = 0L
    @Volatile private var booted = false

    /** True from the first start/recover until the session ended (saved or failed). */
    val isRunning: Boolean get() = booted && !job.isCancelled

    fun start(planJson: String, sessionId: String) {
        if (booted) return // one session at a time
        boot(json.decodeFromString(WorkoutPlan.serializer(), planJson), sessionId, null)
    }

    /** [c] is the checkpoint the caller read from [CheckpointStore]; [resume] false saves the part done so far. */
    fun recover(c: Checkpoint, resume: Boolean) {
        if (booted) return
        startedAt = c.startedAtEpochMs
        val rec = reduce(CheckpointCodec.toState(c), SessionEvent.ProcessRecovered).state
        val chosen = reduce(rec, SessionEvent.RecoveryChoice(resume)).state
        boot(c.plan, c.sessionId, chosen)
    }

    fun pause() = send(SessionEvent.Pause(now()))
    fun resume() { host.cues.refreshPreferences(); send(SessionEvent.Resume(now())) }
    fun skip() = send(SessionEvent.Skip(now()))
    fun finishEarly() = send(SessionEvent.FinishEarly(now()))
    fun earlyDone() = send(SessionEvent.EarlyDone(now()))
    fun log(blockId: String, log: BlockLog) = send(SessionEvent.LogBlock(blockId, log))
    /** Recomputes timers from the clock now (a hidden tab or locked phone returns here). */
    fun tick() = send(SessionEvent.Tick(now()))
    /**
     * Writes the checkpoint immediately and synchronously (the page is about to go away and a queued task might never run).
     * Only for single-threaded hosts such as the web, where nothing can interleave with it.
     */
    fun checkpointNow() { state?.let { checkpoint(it, force = true) } }
    fun close() { job.cancel() }

    private fun boot(plan: WorkoutPlan, sid: String, restored: SessionState?) {
        booted = true
        SessionBus.clear()
        SessionBus.startedAtEpochMs = startedAt
        SessionBus.names = try {
            parseCatalog(ctx.readCatalogText()).variations.associate { it.id to it.name }
        } catch (_: Exception) { emptyMap() }
        host.cues.load()
        host.cues.refreshPreferences()
        host.keepAwakeStart(plan.blocks.sumOf { it.durationSeconds } * 1000L + 30 * 60_000L)
        scope.launch {
            mutex.withLock {
                host.cues.awaitLoaded()
                if (restored == null) {
                    startedAt = wall()
                    SessionBus.startedAtEpochMs = startedAt
                    state = newSession(sid, plan)
                    send(SessionEvent.Start(now()))
                } else {
                    state = restored
                    if (restored.isTerminal) SessionBus.endedAtEpochMs = wall()
                    SessionBus.publish(restored)
                    host.onBlockChanged(restored)
                    if (restored.isTerminal) finish(restored)
                }
                if (isActive) launchTicker()
            }
        }
    }

    private fun CoroutineScope.launchTicker(): Job = launch {
        while (isActive) {
            delay(tickMs)
            if (host.audioInterrupted() && state?.phase == Phase.RUNNING) send(SessionEvent.AudioInterrupted(now()))
            send(SessionEvent.Tick(now()))
        }
    }

    private fun send(e: SessionEvent) {
        scope.launch { mutex.withLock { apply(e) } }
    }

    private suspend fun apply(e: SessionEvent) {
        val cur = state ?: return
        val r = reduce(cur, e)
        state = r.state
        val s = r.state
        if (s.isTerminal && SessionBus.endedAtEpochMs == 0L) SessionBus.endedAtEpochMs = wall() // before publish: the end screen counts minutes from it
        SessionBus.publish(s)
        r.effects.forEach { perform(it, s) }
        checkpoint(s, force = r.effects.any { it is SessionEffect.Persist })
        if (r.effects.any { it is SessionEffect.ShowBlock } || e is SessionEvent.Pause || e is SessionEvent.Resume) host.onBlockChanged(s)
        if (s.isTerminal) finish(s)
    }

    private fun checkpoint(s: SessionState, force: Boolean) {
        if (s.isTerminal) return
        val n = now()
        if (!force && n - lastCkptMs < 5_000) return
        lastCkptMs = n
        try { CheckpointStore.write(ctx, CheckpointCodec.encode(s, startedAt, n, wall())) } catch (e: Exception) { host.logError("checkpoint failed", e) }
    }

    private fun perform(f: SessionEffect, s: SessionState) {
        val cues = host.cues
        when (f) {
            is SessionEffect.Cue -> when (f.kind) {
                CueKind.START, CueKind.ROUND -> { cues.cue(f.kind); cues.say(CueText.spoken(s.plan, f.blockId, SessionBus.names)) }
                CueKind.PREVIEW_NEXT -> CueText.nextLabel(s.plan, f.blockId, SessionBus.names)?.let { cues.say("Next: $it") }
                CueKind.COUNTDOWN_3, CueKind.COUNTDOWN_2, CueKind.COUNTDOWN_1 -> cues.cue(f.kind)
                CueKind.FINISHED -> { cues.cue(f.kind); cues.say("Workout complete") }
            }
            SessionEffect.AcquireWakeLock -> host.keepAwakeEnsure()
            SessionEffect.ReleaseWakeLock -> {}
            is SessionEffect.ShowBlock, is SessionEffect.Persist -> {} // the checkpoint is written by checkpoint() after every event
        }
    }

    private suspend fun finish(s: SessionState) {
        if (SessionBus.saveStatus.value != SaveStatus.NONE) return
        SessionBus.saving()
        try {
            val repository = ctx.historyRepository()
            val planJson = json.encodeToString(WorkoutPlan.serializer(), s.plan)
            val blocks = s.plan.blocks.filter { it.type == BlockType.WORK || it.type == BlockType.STRETCH }.map { b ->
                val ex = s.executions[b.id] ?: Execution.NOT_STARTED
                BlockRow(sessionId = s.sessionId, blockId = b.id, outcome = outcomeOf(ex),
                    actualSeconds = ((s.activeMs[b.id] ?: 0L) / 1000L).toInt(), achievedValue = s.logged[b.id]?.reps)
            }
            // What the user logged during the workout becomes the first feedback revision of each exercise, in the same transaction.
            val at = wall()
            val feedback = rowLogsFrom(s.plan, s.logged).map { r ->
                FeedbackRow(sessionId = s.sessionId, variationId = r.variationId, revision = 1, rating = r.rating.name,
                    discomfort = r.discomfort, assumedMet = false, createdAtEpochMs = at, actualReps = r.actualReps)
            }
            val status = if (s.phase == Phase.COMPLETED) "COMPLETED" else "PARTIAL_FINISHED"
            repository.saveFinishedSession(
                PlanRecord(s.plan.id, s.plan.createdAtEpochMs, s.plan.routineId, s.plan.routineRevision, s.plan.catalogVersion, s.plan.profileId, planJson),
                SessionRow(s.sessionId, s.plan.id, startedAt, SessionBus.endedAtEpochMs.takeIf { it > 0L } ?: wall(), status), blocks, feedback)
            CheckpointStore.clear(ctx) // only after the session row is safely in the history store
            SessionBus.saved = true
        } catch (e: Exception) { SessionBus.saveFailed(); host.logError("save failed", e) }
        job.cancel()
        host.cues.release()
        host.onFinished()
    }
}
