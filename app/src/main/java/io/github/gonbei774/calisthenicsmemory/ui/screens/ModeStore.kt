// Sticky workout-mode settings: timed rounds (60 s work / 60 s rest) and "I have a high bar". App-private.
package io.github.gonbei774.calisthenicsmemory.ui.screens

import android.content.Context

object ModeStore {
    private fun p(ctx: Context) = ctx.getSharedPreferences("mode", Context.MODE_PRIVATE)
    fun timed(ctx: Context) = p(ctx).getBoolean("timed", false)
    fun setTimed(ctx: Context, v: Boolean) { p(ctx).edit().putBoolean("timed", v).apply() }
    fun highBar(ctx: Context) = p(ctx).getBoolean("highBar", false)
    fun setHighBar(ctx: Context, v: Boolean) { p(ctx).edit().putBoolean("highBar", v).apply() }
}
