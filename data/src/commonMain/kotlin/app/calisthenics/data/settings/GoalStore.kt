// Sticky goal: stays until the user changes it. App-private.
package app.calisthenics.data.settings



object GoalStore {
    fun load(ctx: SettingsProvider): String = ctx.legacy("goal").getString("id", "general") ?: "general"
    fun save(ctx: SettingsProvider, id: String) { ctx.legacy("goal").edit().putString("id", id).apply() }
}
