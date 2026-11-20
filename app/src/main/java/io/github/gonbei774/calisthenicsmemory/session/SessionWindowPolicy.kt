package io.github.gonbei774.calisthenicsmemory.session

import app.calisthenics.domain.session.Phase
import app.calisthenics.domain.session.SessionState
import android.view.Window
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.launch

/** The screen flag belongs to a visible Activity and an unfinished workout only. */
internal fun shouldKeepSessionScreenOn(state: SessionState?, windowVisible: Boolean): Boolean =
    windowVisible && state != null && !state.isTerminal && state.phase in setOf(Phase.RUNNING, Phase.PAUSED)

internal fun applySessionScreenPolicy(window: Window, state: SessionState?, windowVisible: Boolean) {
    val keepOn = shouldKeepSessionScreenOn(state, windowVisible)
    val currentlyOn = (window.attributes.flags and WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) != 0
    if (keepOn && !currentlyOn) window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    else if (!keepOn && currentlyOn) window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
}

/** Binds window ownership to the Activity lifecycle without coupling it to video or navigation. */
internal fun ComponentActivity.bindSessionScreenPolicy() {
    lifecycleScope.launch {
        repeatOnLifecycle(Lifecycle.State.STARTED) {
            SessionBus.state.collect { applySessionScreenPolicy(window, it, windowVisible = true) }
        }
    }
    lifecycle.addObserver(object : DefaultLifecycleObserver {
        override fun onStop(owner: LifecycleOwner) {
            applySessionScreenPolicy(window, null, windowVisible = false)
        }
        override fun onDestroy(owner: LifecycleOwner) {
            applySessionScreenPolicy(window, null, windowVisible = false)
        }
    })
}
