package io.github.gonbei774.calisthenicsmemory.platform

import androidx.compose.ui.Modifier
import kotlinx.coroutines.CoroutineDispatcher

/** "m:ss" with the platform's number formatting (Android: `%d:%02d` in the default locale, as before). */
expect fun formatClockText(minutes: Int, seconds: Int): String

/** One decimal place, e.g. "1.5" (Android: `%.1f` in the default locale, as before). */
expect fun formatOneDecimal(value: Double): String

/** Lets UI-automation find controls by test tag (Android: `testTagsAsResourceId`, needed inside dialogs and sheets); no-op elsewhere. */
expect fun Modifier.exposeTestTagsAsResourceId(): Modifier

/** Dispatcher for blocking storage reads (Android: IO; the web has no thread pool). */
expect val IoDispatcher: CoroutineDispatcher
