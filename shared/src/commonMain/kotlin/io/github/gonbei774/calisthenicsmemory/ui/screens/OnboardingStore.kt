package io.github.gonbei774.calisthenicsmemory.ui.screens

import io.github.gonbei774.calisthenicsmemory.platform.PlatformContext
import io.github.gonbei774.calisthenicsmemory.platform.settingsProvider
import io.github.gonbei774.calisthenicsmemory.platform.todayEpochDay
import io.github.gonbei774.calisthenicsmemory.platform.nowEpochMs
import app.calisthenics.data.settings.OnboardingStore as Shared
import app.calisthenics.domain.model.*
import app.calisthenics.domain.routine.SavedRoutine

object OnboardingStore {
 private fun provider(ctx: PlatformContext) = ctx.settingsProvider()
 fun done(ctx: PlatformContext) = Shared.done(provider(ctx))
 fun setDone(ctx: PlatformContext, v: Boolean = true) = Shared.setDone(provider(ctx), v)
}
