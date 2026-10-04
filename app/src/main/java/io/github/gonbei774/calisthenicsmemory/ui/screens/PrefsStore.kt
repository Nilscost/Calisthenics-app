// Remembers ONLY the settings spec §3 says to remember (rememberOnStart), as JSON in app-private storage.
package io.github.gonbei774.calisthenicsmemory.ui.screens

import android.content.Context
import app.calisthenics.domain.model.Preferences
import kotlinx.serialization.json.Json

object PrefsStore {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    fun load(ctx: Context): Preferences = try {
        ctx.getSharedPreferences("planner_prefs", Context.MODE_PRIVATE).getString("prefs", null)
            ?.let { json.decodeFromString(Preferences.serializer(), it) } ?: Preferences()
    } catch (_: Exception) { Preferences() } // unreadable -> defaults, never crash
    fun save(ctx: Context, p: Preferences) {
        ctx.getSharedPreferences("planner_prefs", Context.MODE_PRIVATE).edit()
            .putString("prefs", json.encodeToString(Preferences.serializer(), p)).apply()
    }
}
