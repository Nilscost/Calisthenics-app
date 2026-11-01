package io.github.gonbei774.calisthenicsmemory.platform

import androidx.compose.runtime.Composable
import kotlinx.datetime.LocalDate

private val months = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
private val weekdays = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")

// Compile-only for Phase 3b: fixed English formats; Phase 4 switches to Intl.DateTimeFormat.
actual fun formatMediumDate(day: LocalDate): String = "${day.day} ${months[day.month.ordinal]} ${day.year}"

actual fun chartDateLabel(day: LocalDate): String = "${day.day} ${months[day.month.ordinal]}"

@Composable
actual fun shortWeekdayName(day: LocalDate): String = weekdays[day.dayOfWeek.ordinal]
