// Process-local bridge: the SERVICE owns the timer and publishes state; the UI only observes and sends commands.
package io.github.gonbei774.calisthenicsmemory.session

import app.calisthenics.domain.session.SessionState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** Whether the finished session is in Room yet. */
enum class SaveStatus { NONE, SAVING, SAVED, FAILED }

object SessionBus {
    private val _state = MutableStateFlow<SessionState?>(null)
    val state: StateFlow<SessionState?> = _state
    private val _save = MutableStateFlow(SaveStatus.NONE)
    val saveStatus: StateFlow<SaveStatus> = _save
    /** True from the moment a start/recover was requested until the service publishes its first state (or gives up). Never saved: a restored SESSION route without it has no workout. */
    private val _starting = MutableStateFlow(false)
    val starting: StateFlow<Boolean> = _starting
    @Volatile var names: Map<String, String> = emptyMap()
    /** Wall-clock start and end of the session, for the same minutes History will show (0 = unknown). */
    @Volatile var startedAtEpochMs: Long = 0L
    @Volatile var endedAtEpochMs: Long = 0L
    var saved: Boolean
        get() = _save.value == SaveStatus.SAVED
        set(v) { _save.value = if (v) SaveStatus.SAVED else SaveStatus.NONE }
    fun publish(s: SessionState?) { _state.value = s; if (s != null) _starting.value = false }
    fun saving() { _save.value = SaveStatus.SAVING }
    fun saveFailed() { _save.value = SaveStatus.FAILED }
    fun clear() { _state.value = null; _save.value = SaveStatus.NONE; startedAtEpochMs = 0L; endedAtEpochMs = 0L }
    /** A start/recover was requested: drop a previous finished session (never a live one) and show "Starting…" until the first publish. */
    fun begin() {
        val cur = _state.value
        if (cur != null && !cur.isTerminal) return // the service ignores a second start while one runs
        clear(); _starting.value = true
    }
    /** The service gave up before publishing (nothing to recover). */
    fun abortStarting() { _starting.value = false }
}
