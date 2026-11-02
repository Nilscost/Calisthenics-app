import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.window.ComposeViewport
import app.calisthenics.data.settings.AppTheme
import app.calisthenics.data.settings.ThemeStore
import io.github.gonbei774.calisthenicsmemory.platform.PlatformContext
import io.github.gonbei774.calisthenicsmemory.platform.settingsProvider
import io.github.gonbei774.calisthenicsmemory.session.webInstallAudioUnlock
import io.github.gonbei774.calisthenicsmemory.ui.nav.AppNav
import io.github.gonbei774.calisthenicsmemory.ui.theme.CalisthenicsMemoryTheme
import kotlinx.coroutines.await
import kotlinx.coroutines.launch
import kotlin.js.Promise

@JsFun("(url) => fetch(url).then(r => { if (!r.ok) throw new Error(url + ' ' + r.status); return r.text(); })")
private external fun fetchText(url: String): Promise<JsString>

@JsFun("() => window.matchMedia('(prefers-color-scheme: dark)').matches")
private external fun prefersDark(): Boolean

@JsFun("() => { try { if (navigator.vibrate) navigator.vibrate(10); } catch (e) {} }")
private external fun vibrateTick()

/** Haptics (Phase 4b): a short `navigator.vibrate` tick where the browser supports it (Android browsers; iOS Safari has none), otherwise nothing. */
private object WebHaptics : HapticFeedback {
    override fun performHapticFeedback(hapticFeedbackType: HapticFeedbackType) = vibrateTick()
}

// The shared UI reads the catalog synchronously, so it is fetched before the first frame.
@OptIn(ExperimentalComposeUiApi::class)
suspend fun start() {
    PlatformContext.catalogJson = fetchText("data/catalog.json").await<JsString>().toString()
    PlatformContext.privacyMarkdown = fetchText("data/privacy.md").await<JsString>().toString()
    PlatformContext.licensesMarkdown = runCatching { fetchText("data/licenses.md").await<JsString>().toString() }.getOrNull()
    webInstallAudioUnlock() // first tap anywhere unlocks Web Audio and speech (iOS Safari rule)
    val store = ThemeStore(PlatformContext.settingsProvider())
    ComposeViewport {
        var theme by remember { mutableStateOf(store.getTheme()) }
        val dark = when (theme) {
            AppTheme.SYSTEM -> prefersDark()
            AppTheme.LIGHT -> false
            AppTheme.DARK -> true
        }
        CompositionLocalProvider(LocalHapticFeedback provides WebHaptics) {
            CalisthenicsMemoryTheme(darkTheme = dark) {
                Box(Modifier.fillMaxSize()) {
                    AppNav(theme) { theme = it; store.setTheme(it) }
                }
            }
        }
    }
}

@OptIn(kotlin.js.ExperimentalWasmJsInterop::class, kotlinx.coroutines.DelicateCoroutinesApi::class)
fun main() {
    kotlinx.coroutines.GlobalScope.launch { start() }
}
