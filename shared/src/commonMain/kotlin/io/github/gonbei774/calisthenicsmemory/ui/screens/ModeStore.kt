package io.github.gonbei774.calisthenicsmemory.ui.screens

import io.github.gonbei774.calisthenicsmemory.platform.PlatformContext
import io.github.gonbei774.calisthenicsmemory.platform.settingsProvider
import io.github.gonbei774.calisthenicsmemory.platform.todayEpochDay
import io.github.gonbei774.calisthenicsmemory.platform.nowEpochMs
import app.calisthenics.data.settings.ModeStore as Shared
import app.calisthenics.domain.model.*
import app.calisthenics.domain.routine.SavedRoutine

object ModeStore {
 private fun provider(ctx: PlatformContext) = ctx.settingsProvider()
 fun timed(ctx: PlatformContext) = Shared.timed(provider(ctx))
 fun setTimed(ctx: PlatformContext, v: Boolean) = Shared.setTimed(provider(ctx), v)
 fun takeLegacyHighBar(ctx: PlatformContext) = Shared.takeLegacyHighBar(provider(ctx))
}
