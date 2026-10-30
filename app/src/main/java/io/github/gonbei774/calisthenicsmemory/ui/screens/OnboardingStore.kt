package io.github.gonbei774.calisthenicsmemory.ui.screens

import android.content.Context
import app.calisthenics.data.settings.AndroidSettingsProvider
import app.calisthenics.data.settings.OnboardingStore as Shared
import app.calisthenics.domain.model.*
import app.calisthenics.domain.routine.SavedRoutine

object OnboardingStore {
 private fun provider(ctx: Context) = AndroidSettingsProvider(ctx)
 fun done(ctx: Context) = Shared.done(provider(ctx))
 fun setDone(ctx: Context, v: Boolean = true) = Shared.setDone(provider(ctx), v)
}
