// Sticky workout-mode setting: timed rounds (60 s work / 60 s rest). App-private.
// The old "I have a high bar" flag moved into equipment profiles (U04); [takeLegacyHighBar] hands it over once.
package app.calisthenics.data.settings



object ModeStore {
    private fun p(ctx: SettingsProvider) = ctx.legacy("mode")
    fun timed(ctx: SettingsProvider) = p(ctx).getBoolean("timed", false)
    fun setTimed(ctx: SettingsProvider, v: Boolean) { p(ctx).edit().putBoolean("timed", v).apply() }

    /** Returns the old high-bar toggle and clears it, so it is migrated into a profile exactly once. */
    fun takeLegacyHighBar(ctx: SettingsProvider): Boolean {
        val v = p(ctx).getBoolean("highBar", false)
        if (p(ctx).contains("highBar")) p(ctx).edit().remove("highBar").apply()
        return v
    }
}
