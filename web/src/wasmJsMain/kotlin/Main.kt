import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.ComposeViewport
import app.calisthenics.data.settings.AppTheme
import app.calisthenics.data.settings.ThemeStore
import io.github.gonbei774.calisthenicsmemory.platform.PlatformContext
import io.github.gonbei774.calisthenicsmemory.platform.settingsProvider
import io.github.gonbei774.calisthenicsmemory.ui.nav.AppNav
import io.github.gonbei774.calisthenicsmemory.ui.theme.CalisthenicsMemoryTheme
import kotlinx.coroutines.await
import kotlinx.coroutines.launch
import kotlin.js.Promise

@JsFun("(url) => fetch(url).then(r => { if (!r.ok) throw new Error(url + ' ' + r.status); return r.text(); })")
private external fun fetchText(url: String): Promise<JsString>

@JsFun("() => window.matchMedia('(prefers-color-scheme: dark)').matches")
private external fun prefersDark(): Boolean

// The shared UI reads the catalog synchronously, so it is fetched before the first frame.
@OptIn(ExperimentalComposeUiApi::class)
suspend fun start() {
    PlatformContext.catalogJson = fetchText("data/catalog.json").await<JsString>().toString()
    PlatformContext.privacyMarkdown = fetchText("data/privacy.md").await<JsString>().toString()
    val store = ThemeStore(PlatformContext.settingsProvider())
    ComposeViewport {
        var theme by remember { mutableStateOf(store.getTheme()) }
        val dark = when (theme) {
            AppTheme.SYSTEM -> prefersDark()
            AppTheme.LIGHT -> false
            AppTheme.DARK -> true
        }
        CalisthenicsMemoryTheme(darkTheme = dark) {
            Box(Modifier.fillMaxSize()) {
                AppNav(theme) { theme = it; store.setTheme(it) }
            }
        }
    }
}

@OptIn(kotlin.js.ExperimentalWasmJsInterop::class, kotlinx.coroutines.DelicateCoroutinesApi::class)
fun main() {
    kotlinx.coroutines.GlobalScope.launch { start() }
}
