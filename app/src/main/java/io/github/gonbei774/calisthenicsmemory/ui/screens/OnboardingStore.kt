// First-run flag. Existing installs that already saved levels count as done so they never see the questionnaire again.
package io.github.gonbei774.calisthenicsmemory.ui.screens

import android.content.Context

object OnboardingStore {
    private fun p(ctx: Context) = ctx.getSharedPreferences("onboarding", Context.MODE_PRIVATE)
    fun done(ctx: Context): Boolean = p(ctx).getBoolean("done", false) || LevelStore.load(ctx).isNotEmpty()
    fun setDone(ctx: Context, v: Boolean = true) { p(ctx).edit().putBoolean("done", v).apply() }
}
