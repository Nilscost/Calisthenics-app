package io.github.gonbei774.calisthenicsmemory.platform

import androidx.compose.ui.Modifier
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

// Phase 4b: Intl.NumberFormat in the browser's language, matching Android's `%d:%02d` and `%.1f` in the default locale
// (localised digits and decimal separator; both round half up).
@JsFun("""(m, s) => {
  const loc = navigator.language || undefined;
  const c = (globalThis.__calNumFmt ||= {});
  const a = c.int ||= new Intl.NumberFormat(loc, { useGrouping: false });
  const b = c.two ||= new Intl.NumberFormat(loc, { useGrouping: false, minimumIntegerDigits: 2 });
  return a.format(m) + ':' + b.format(s);
}""")
private external fun intlClock(minutes: Int, seconds: Int): String

@JsFun("""(v) => {
  const loc = navigator.language || undefined;
  const c = (globalThis.__calNumFmt ||= {});
  return (c.one ||= new Intl.NumberFormat(loc, { useGrouping: false, minimumFractionDigits: 1, maximumFractionDigits: 1 })).format(v);
}""")
private external fun intlOneDecimal(value: Double): String

actual fun formatClockText(minutes: Int, seconds: Int): String = intlClock(minutes, seconds)

actual fun formatOneDecimal(value: Double): String = intlOneDecimal(value)

actual fun Modifier.exposeTestTagsAsResourceId(): Modifier = this

actual val IoDispatcher: CoroutineDispatcher = Dispatchers.Default
