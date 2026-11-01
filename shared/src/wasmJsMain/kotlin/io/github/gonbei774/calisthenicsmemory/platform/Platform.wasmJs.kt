package io.github.gonbei774.calisthenicsmemory.platform

import androidx.compose.runtime.Composable
import app.calisthenics.data.history.HistoryRepository
import app.calisthenics.data.history.IndexedDbHistoryRepository
import app.calisthenics.data.settings.SettingsProvider
import app.calisthenics.data.settings.WebSettingsProvider

actual abstract class PlatformContext private constructor() {
    /** The one web context. */
    companion object Instance : PlatformContext() {
        /** Set by the web entry point (Phase 3c/4) before the first screen reads the catalog. */
        var catalogJson: String? = null
        var privacyMarkdown: String? = null
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

actual fun elapsedRealtimeMs(): Long = performanceNow().toLong()

/** Compile-only for Phase 3b; Phase 4 may map this to browser history. */
@Composable
actual fun PlatformBackHandler(enabled: Boolean, onBack: () -> Unit) = Unit
