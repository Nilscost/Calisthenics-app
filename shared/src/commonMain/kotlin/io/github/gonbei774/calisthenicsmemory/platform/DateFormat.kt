package io.github.gonbei774.calisthenicsmemory.platform

import androidx.compose.runtime.Composable
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import kotlin.time.Clock

fun todayLocalDate(): LocalDate = Clock.System.todayIn(TimeZone.currentSystemDefault())

/** Locale-formatted medium date ("13 Oct 2026" / "Oct 13, 2026"). */
expect fun formatMediumDate(day: LocalDate): String

/** The date under a chart dot, short and in the phone's locale order ("13 Oct" / "Oct 13"). */
expect fun chartDateLabel(day: LocalDate): String

/** Short weekday name in the locale of the composition ("Mon"). */
@Composable
expect fun shortWeekdayName(day: LocalDate): String
