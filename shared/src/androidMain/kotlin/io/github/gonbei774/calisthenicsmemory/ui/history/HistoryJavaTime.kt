package io.github.gonbei774.calisthenicsmemory.ui.history

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import kotlinx.datetime.toKotlinLocalDate

/** Android callers (and the Robolectric tests) hold `java.time` dates; these overloads convert and delegate to the shared screen. */
@Composable
fun HistoryScreen(modifier: Modifier = Modifier, source: HistorySource? = null, today: java.time.LocalDate, onOpenExercise: (String) -> Unit = {}, onOpen: (String) -> Unit) =
    HistoryScreen(modifier, source, today.toKotlinLocalDate(), onOpenExercise, onOpen)

/** The date under a chart dot in the phone's locale order, from a `java.time` date. */
fun chartDateLabel(day: java.time.LocalDate, locale: java.util.Locale = java.util.Locale.getDefault()): String =
    io.github.gonbei774.calisthenicsmemory.platform.chartDateLabel(day, locale)
