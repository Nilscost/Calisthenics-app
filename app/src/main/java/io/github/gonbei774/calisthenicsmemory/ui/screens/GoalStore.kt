// Sticky goal: stays until the user changes it. App-private.
package io.github.gonbei774.calisthenicsmemory.ui.screens

import android.content.Context

object GoalStore {
    fun load(ctx: Context): String = ctx.getSharedPreferences("goal", Context.MODE_PRIVATE).getString("id", "general") ?: "general"
    fun save(ctx: Context, id: String) { ctx.getSharedPreferences("goal", Context.MODE_PRIVATE).edit().putString("id", id).apply() }
}
