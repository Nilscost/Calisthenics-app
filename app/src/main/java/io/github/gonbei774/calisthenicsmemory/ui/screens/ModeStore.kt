package io.github.gonbei774.calisthenicsmemory.ui.screens

import android.content.Context
import app.calisthenics.data.settings.AndroidSettingsProvider
import app.calisthenics.data.settings.ModeStore as Shared
import app.calisthenics.domain.model.*
import app.calisthenics.domain.routine.SavedRoutine

object ModeStore {
 private fun provider(ctx: Context) = AndroidSettingsProvider(ctx)
 fun timed(ctx: Context) = Shared.timed(provider(ctx))
 fun setTimed(ctx: Context, v: Boolean) = Shared.setTimed(provider(ctx), v)
 fun takeLegacyHighBar(ctx: Context) = Shared.takeLegacyHighBar(provider(ctx))
}
