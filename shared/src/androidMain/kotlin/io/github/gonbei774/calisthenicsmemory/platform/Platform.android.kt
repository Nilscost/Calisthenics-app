package io.github.gonbei774.calisthenicsmemory.platform

import android.content.Context
import android.os.SystemClock
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import app.calisthenics.data.history.HistoryRepository
import app.calisthenics.data.history.RoomHistoryRepository
import app.calisthenics.data.settings.AndroidSettingsProvider
import app.calisthenics.data.settings.SettingsProvider
import io.github.gonbei774.calisthenicsmemory.data.AppDatabase

actual typealias PlatformContext = Context

@Composable
actual fun currentPlatformContext(): PlatformContext = LocalContext.current

actual fun PlatformContext.settingsProvider(): SettingsProvider = AndroidSettingsProvider(this)

actual fun PlatformContext.historyRepository(): HistoryRepository = RoomHistoryRepository(AppDatabase.getDatabase(this).historyDao())

actual fun PlatformContext.readCatalogText(): String = assets.open("catalog.json").bufferedReader().use { it.readText() }

actual fun PlatformContext.readPrivacyText(): String = assets.open("privacy.md").bufferedReader().use { it.readText() }

actual fun PlatformContext.appVersionName(): String =
    packageManager.getPackageInfo(packageName, 0).versionName.orEmpty()

actual fun elapsedRealtimeMs(): Long = SystemClock.elapsedRealtime()

@Composable
actual fun PlatformBackHandler(enabled: Boolean, onBack: () -> Unit) = BackHandler(enabled, onBack)
