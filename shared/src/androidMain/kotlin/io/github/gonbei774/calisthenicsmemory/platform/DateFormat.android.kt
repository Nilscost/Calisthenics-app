package io.github.gonbei774.calisthenicsmemory.platform

import android.text.format.DateFormat
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import kotlinx.datetime.LocalDate
import kotlinx.datetime.toJavaLocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.format.TextStyle
import java.util.Locale

private val mediumDate: DateTimeFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)

actual fun formatMediumDate(day: LocalDate): String = mediumDate.format(day.toJavaLocalDate())

actual fun chartDateLabel(day: LocalDate): String = chartDateLabel(day.toJavaLocalDate())

fun chartDateLabel(day: java.time.LocalDate, locale: Locale = Locale.getDefault()): String =
    DateTimeFormatter.ofPattern(DateFormat.getBestDateTimePattern(locale, "dMMM"), locale).format(day)

@Composable
actual fun shortWeekdayName(day: LocalDate): String =
    day.toJavaLocalDate().dayOfWeek.getDisplayName(TextStyle.SHORT, LocalConfiguration.current.locales[0])
