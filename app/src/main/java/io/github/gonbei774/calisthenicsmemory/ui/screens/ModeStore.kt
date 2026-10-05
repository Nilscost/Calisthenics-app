// Sticky workout-mode setting: timed rounds (60 s work / 60 s rest). App-private.
// The old "I have a high bar" flag moved into equipment profiles (U04); [takeLegacyHighBar] hands it over once.
package io.github.gonbei774.calisthenicsmemory.ui.screens

import android.content.Context

object ModeStore {
    private fun p(ctx: Context) = ctx.getSharedPreferences("mode", Context.MODE_PRIVATE)
    fun timed(ctx: Context) = p(ctx).getBoolean("timed", false)
    fun setTimed(ctx: Context, v: Boolean) { p(ctx).edit().putBoolean("timed", v).apply() }

    /** Returns the old high-bar toggle and clears it, so it is migrated into a profile exactly once. */
    fun takeLegacyHighBar(ctx: Context): Boolean {
        val v = p(ctx).getBoolean("highBar", false)
        if (p(ctx).contains("highBar")) p(ctx).edit().remove("highBar").apply()
        return v
    }
}
