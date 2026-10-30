package io.github.gonbei774.calisthenicsmemory.ui.screens

import android.content.Context
import app.calisthenics.data.settings.AndroidSettingsProvider
import app.calisthenics.data.settings.GoalStore as Shared
import app.calisthenics.domain.model.*
import app.calisthenics.domain.routine.SavedRoutine

object GoalStore {
 private fun provider(ctx: Context) = AndroidSettingsProvider(ctx)
 fun load(ctx: Context) = Shared.load(provider(ctx))
 fun save(ctx: Context, id: String) = Shared.save(provider(ctx), id)
}
