// Back navigation on the web (Phase 4b): enabled PlatformBackHandlers are kept in a stack; while any is enabled exactly one
// sentinel entry sits on top of the browser history. The browser/iOS swipe-back gesture pops it (popstate) and runs the
// top handler; Escape runs it directly (Compose marks every Escape as default-prevented, so it cannot tell us whether a
// dialog already took it: Escape inside a dialog also goes back). When the last handler goes away the sentinel is removed again, so history never
// grows: at most one extra entry exists. With no handler enabled (Train) back leaves the app as usual.
package io.github.gonbei774.calisthenicsmemory.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.rememberUpdatedState

@JsFun("""(onPop, onEscape) => {
  if (globalThis.__calBackInstalled) return;
  globalThis.__calBackInstalled = true;
  // a reload on top of our sentinel entry would leave a dead entry behind: make it an ordinary one
  try { if (history.state && history.state.calBack) history.replaceState(null, ''); } catch (e) {}
  addEventListener('popstate', () => onPop());
  addEventListener('keydown', e => { if (e.key === 'Escape' && onEscape()) e.preventDefault(); });
}""")
private external fun installBackListeners(onPop: () -> Unit, onEscape: () -> Boolean)

@JsFun("() => { try { history.pushState({ calBack: 1 }, ''); } catch (e) {} }")
private external fun pushSentinel()

@JsFun("() => { try { history.back(); } catch (e) {} }")
private external fun historyBack()

@JsFun("(cb, ms) => { setTimeout(() => cb(), ms); }")
private external fun defer(cb: () -> Unit, ms: Int)

internal object WebBack {
    class Entry(val onBack: () -> Unit)

    private val entries = mutableListOf<Entry>()
    private var sentinel = false
    private var ignorePops = 0
    private var installed = false
    private var syncQueued = false

    fun add(e: Entry) { install(); entries += e; queueSync() }
    fun remove(e: Entry) { entries -= e; queueSync() }

    private fun install() {
        if (installed) return
        installed = true
        installBackListeners(::onPop, ::onEscape)
    }

    // Coalesced and slightly delayed: a screen change removes one handler and adds the next one within a few frames.
    private fun queueSync() {
        if (syncQueued) return
        syncQueued = true
        defer({ syncQueued = false; sync() }, 60)
    }

    private fun sync() {
        if (entries.isNotEmpty() && !sentinel) { pushSentinel(); sentinel = true }
        else if (entries.isEmpty() && sentinel) { sentinel = false; ignorePops++; historyBack() }
    }

    private fun onPop() {
        if (ignorePops > 0) { ignorePops--; queueSync(); return }
        if (!sentinel) return   // not a pop of ours (the person left the app's own history)
        sentinel = false        // the sentinel was just consumed
        entries.lastOrNull()?.onBack?.invoke()
        queueSync()
    }

    private fun onEscape(): Boolean {
        val top = entries.lastOrNull() ?: return false
        top.onBack()
        return true
    }
}

@Composable
actual fun PlatformBackHandler(enabled: Boolean, onBack: () -> Unit) {
    val current = rememberUpdatedState(onBack)
    DisposableEffect(enabled) {
        if (!enabled) return@DisposableEffect onDispose { }
        val entry = WebBack.Entry { current.value() }
        WebBack.add(entry)
        onDispose { WebBack.remove(entry) }
    }
}
