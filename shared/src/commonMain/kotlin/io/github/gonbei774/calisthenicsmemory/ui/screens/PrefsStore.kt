package io.github.gonbei774.calisthenicsmemory.ui.screens

import io.github.gonbei774.calisthenicsmemory.platform.PlatformContext
import io.github.gonbei774.calisthenicsmemory.platform.settingsProvider
import io.github.gonbei774.calisthenicsmemory.platform.todayEpochDay
import io.github.gonbei774.calisthenicsmemory.platform.nowEpochMs
import app.calisthenics.data.settings.PrefsStore as Shared
import app.calisthenics.domain.model.*
import app.calisthenics.domain.routine.SavedRoutine

object PrefsStore {
 private fun provider(ctx: PlatformContext) = ctx.settingsProvider()
 fun load(ctx: PlatformContext) = Shared.load(provider(ctx))
 fun save(ctx: PlatformContext, p: Preferences) = Shared.save(provider(ctx), p)
}
