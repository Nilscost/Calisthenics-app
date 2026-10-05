// Process-local bridge: the SERVICE owns the timer and publishes state; the UI only observes and sends commands.
package io.github.gonbei774.calisthenicsmemory.session

import app.calisthenics.domain.session.SessionState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

object SessionBus {
    private val _state = MutableStateFlow<SessionState?>(null)
    val state: StateFlow<SessionState?> = _state
    @Volatile var names: Map<String, String> = emptyMap()
    @Volatile var saved: Boolean = false
    fun publish(s: SessionState?) { _state.value = s }
    fun clear() { _state.value = null; saved = false }
}
