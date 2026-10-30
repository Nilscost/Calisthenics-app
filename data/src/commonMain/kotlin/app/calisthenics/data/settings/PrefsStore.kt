// Remembers ONLY the settings spec §3 says to remember (rememberOnStart), as JSON in app-private storage.
package app.calisthenics.data.settings


import app.calisthenics.domain.model.Preferences
import kotlinx.serialization.json.Json

object PrefsStore {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    fun load(ctx: SettingsProvider): Preferences = try {
        ctx.legacy("planner_prefs").getString("prefs", null)
            ?.let { json.decodeFromString(Preferences.serializer(), it).withClampedVolume() } ?: Preferences()
    } catch (_: Exception) { Preferences() } // unreadable -> defaults, never crash
    fun save(ctx: SettingsProvider, p: Preferences) {
        ctx.legacy("planner_prefs").edit()
            .putString("prefs", json.encodeToString(Preferences.serializer(), p.withClampedVolume())).apply()
    }

    private fun Preferences.withClampedVolume() = copy(appVolumePercent = appVolumePercent.coerceIn(0, 100))
}
