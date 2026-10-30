// First-run flag. Existing installs that already saved levels count as done so they never see the questionnaire again.
package app.calisthenics.data.settings



object OnboardingStore {
    private fun p(ctx: SettingsProvider) = ctx.legacy("onboarding")
    fun done(ctx: SettingsProvider): Boolean = p(ctx).getBoolean("done", false) || LevelStore.load(ctx).isNotEmpty()
    fun setDone(ctx: SettingsProvider, v: Boolean = true) { p(ctx).edit().putBoolean("done", v).apply() }
}
