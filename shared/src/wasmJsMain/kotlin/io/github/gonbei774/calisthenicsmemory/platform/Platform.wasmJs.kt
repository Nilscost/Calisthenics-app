package io.github.gonbei774.calisthenicsmemory.platform

import androidx.compose.runtime.Composable
import app.calisthenics.data.history.HistoryRepository
import app.calisthenics.data.history.IndexedDbHistoryRepository
import app.calisthenics.data.settings.SettingsProvider
import app.calisthenics.data.settings.WebSettingsProvider
import app.calisthenics.domain.session.SleepAwareClock

actual abstract class PlatformContext private constructor() {
    /** The one web context. */
    companion object Instance : PlatformContext() {
        /** Set by the web entry point (Phase 3c/4) before the first screen reads the catalog. */
        var catalogJson: String? = null
        var privacyMarkdown: String? = null
        /** THIRD_PARTY_NOTICES.md (copied into the build as data/licenses.md); shown by the licence screen. */
        var licensesMarkdown: String? = null
        internal val settings by lazy { WebSettingsProvider() }
        internal val history by lazy { IndexedDbHistoryRepository() }
    }
}

@Composable
actual fun currentPlatformContext(): PlatformContext = PlatformContext.Instance

actual fun PlatformContext.settingsProvider(): SettingsProvider = PlatformContext.settings

actual fun PlatformContext.historyRepository(): HistoryRepository = PlatformContext.history

actual fun PlatformContext.readCatalogText(): String =
    PlatformContext.catalogJson ?: error("catalog.json has not been loaded by the web entry point yet")

actual fun PlatformContext.readPrivacyText(): String = PlatformContext.privacyMarkdown ?: ""

actual fun PlatformContext.appVersionName(): String = "web"

@JsFun("() => performance.now()")
private external fun performanceNow(): Double

@JsFun("() => Date.now()")
private external fun dateNow(): Double

// performance.now() pauses while a phone sleeps; the wall clock does not. SleepAwareClock adds the slept time, so timers
// built on stored end times are right when the page comes back (Phase 4a).
private val monotonicClock = SleepAwareClock(::performanceNow, ::dateNow)

actual fun elapsedRealtimeMs(): Long = monotonicClock.nowMs()
