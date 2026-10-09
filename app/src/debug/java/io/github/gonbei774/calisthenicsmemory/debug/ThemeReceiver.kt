// V08b (debug builds only): sets the saved app theme for the UI screenshot runner.
//   adb shell am broadcast -n app.calisthenics.personal/io.github.gonbei774.calisthenicsmemory.debug.ThemeReceiver \
//       -a app.calisthenics.personal.debug.THEME --es theme LIGHT -f 0x20        (LIGHT | DARK | SYSTEM)
package io.github.gonbei774.calisthenicsmemory.debug

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import io.github.gonbei774.calisthenicsmemory.data.AppTheme
import io.github.gonbei774.calisthenicsmemory.data.ThemePreferences

class ThemeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val t = runCatching { AppTheme.valueOf(intent.getStringExtra("theme").orEmpty().uppercase()) }.getOrNull()
        if (t != null) ThemePreferences(context.applicationContext).setTheme(t)
        resultCode = if (t != null) 1 else 0
    }
}
