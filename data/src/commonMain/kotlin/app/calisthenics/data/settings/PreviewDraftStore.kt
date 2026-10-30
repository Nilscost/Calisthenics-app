package app.calisthenics.data.settings

import app.calisthenics.domain.planner.PreviewDraft
import app.calisthenics.domain.planner.expireLevelOverrides
import kotlinx.serialization.json.Json

object PreviewDraftStore {
    private val json = Json { ignoreUnknownKeys = true }
    private fun p(ctx: SettingsProvider) = ctx.legacy("preview_draft")

    /** The draft saved for [key], or null when there is none or it belongs to another objective. */
    fun get(ctx: SettingsProvider, key: String, todayEpochDay: Long): PreviewDraft? {
        val d = runCatching { p(ctx).getString("draft", null)?.let { json.decodeFromString(PreviewDraft.serializer(), it) } }.getOrNull()
        // A manual level or typed number is for the next workout only: older ones are dropped
        return d?.takeIf { it.key == key }?.let { it.copy(edits = it.edits.expireLevelOverrides(todayEpochDay)) }
    }

    fun put(ctx: SettingsProvider, d0: PreviewDraft, todayEpochDay: Long) {
        // a draft written now is today's: levels it carries (typed by hand, or a saved routine's own) count from today
        val d = if (d0.edits.hasLevelOverrides() && d0.edits.overrideEpochDay == 0L) d0.copy(edits = d0.edits.copy(overrideEpochDay = todayEpochDay)) else d0
        runCatching { p(ctx).edit().putString("draft", json.encodeToString(PreviewDraft.serializer(), d)).apply() }
    }

    fun clear(ctx: SettingsProvider) {
        runCatching { p(ctx).edit().remove("draft").apply() }
    }
}
