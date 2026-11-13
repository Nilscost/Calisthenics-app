// The narrow platform seam of the shared UI (Phase 3b). Everything Android- or browser-specific that the screens need
// is declared here (or in the other `platform` files) and implemented per target; screens never import platform APIs.
package io.github.gonbei774.calisthenicsmemory.platform

import androidx.compose.runtime.Composable
import app.calisthenics.data.history.HistoryRepository
import app.calisthenics.data.settings.SettingsProvider
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import kotlin.time.Clock

/**
 * What shared code passes around as "the context" (stores, catalog, clips, session control).
 * Android: a typealias of `android.content.Context`, so existing call sites and tests keep working unchanged.
 * Web: a singleton with no state of its own.
 */
expect abstract class PlatformContext

/** The platform context of the composition (Android: `LocalContext.current`). */
@Composable
expect fun currentPlatformContext(): PlatformContext

/** Named key-value files (same preference file names and keys as before on Android; localStorage on the web). */
expect fun PlatformContext.settingsProvider(): SettingsProvider

/** Workout history: Room on Android, IndexedDB on the web. */
expect fun PlatformContext.historyRepository(): HistoryRepository

/** The bundled `catalog.json` text (Android reads the app's assets as before). */
expect fun PlatformContext.readCatalogText(): String

/** The privacy policy text shown in Settings (Android: assets/privacy.md). */
expect fun PlatformContext.readPrivacyText(): String

/** The version string shown in backup files (Android: `versionName`). */
expect fun PlatformContext.appVersionName(): String

/** Monotonic milliseconds for on-screen timers (Android: `SystemClock.elapsedRealtime`). */
expect fun elapsedRealtimeMs(): Long

/** System back (Android: activity BackHandler; the web has no equivalent yet). */
@Composable
expect fun PlatformBackHandler(enabled: Boolean = true, onBack: () -> Unit)

fun nowEpochMs(): Long = Clock.System.now().toEpochMilliseconds()

/** Today as days since 1970-01-01 in the device time zone (same value as `LocalDate.now().toEpochDay()`). */
fun todayEpochDay(): Long = Clock.System.todayIn(TimeZone.currentSystemDefault()).toEpochDays()

fun systemTimeZone(): TimeZone = TimeZone.currentSystemDefault()

/** A random lowercase UUID string (same shape as `java.util.UUID.randomUUID().toString()`). */
@OptIn(kotlin.uuid.ExperimentalUuidApi::class)
fun newUuid(): String = kotlin.uuid.Uuid.random().toString()
