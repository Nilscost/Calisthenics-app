package io.github.gonbei774.calisthenicsmemory.ui.screens

import android.content.Context
import app.calisthenics.data.settings.AndroidSettingsProvider
import app.calisthenics.data.settings.PrefsStore as Shared
import app.calisthenics.domain.model.*
import app.calisthenics.domain.routine.SavedRoutine

object PrefsStore {
 private fun provider(ctx: Context) = AndroidSettingsProvider(ctx)
 fun load(ctx: Context) = Shared.load(provider(ctx))
 fun save(ctx: Context, p: Preferences) = Shared.save(provider(ctx), p)
}
