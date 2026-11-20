package io.github.gonbei774.calisthenicsmemory.platform

import androidx.compose.runtime.Composable
import kotlinx.datetime.LocalDate

// Phase 4b: Intl.DateTimeFormat in the browser's language (navigator.language), the same patterns as the Android actuals:
// FormatStyle.MEDIUM, the best pattern for "dMMM", and the short weekday name. The calendar day is formatted as a UTC
// date so the time zone never shifts it.
@JsFun("""(kind, y, m, d) => {
  const loc = navigator.language || undefined;
  const cache = (globalThis.__calDateFmt ||= {});
  const opts = kind === 0 ? { dateStyle: 'medium' } : kind === 1 ? { day: 'numeric', month: 'short' } : { weekday: 'short' };
  const f = cache[kind] ||= new Intl.DateTimeFormat(loc, { ...opts, timeZone: 'UTC' });
  return f.format(new Date(Date.UTC(y, m - 1, d, 12)));
}""")
private external fun intlDate(kind: Int, year: Int, month: Int, day: Int): String

/** The browser's language tag, e.g. "en-US" or "de-CH". */
@JsFun("() => navigator.language || 'en-US'")
internal external fun browserLocale(): String

actual fun formatMediumDate(day: LocalDate): String = intlDate(0, day.year, day.month.ordinal + 1, day.day)

actual fun chartDateLabel(day: LocalDate): String = intlDate(1, day.year, day.month.ordinal + 1, day.day)

@Composable
actual fun shortWeekdayName(day: LocalDate): String = intlDate(2, day.year, day.month.ordinal + 1, day.day)
