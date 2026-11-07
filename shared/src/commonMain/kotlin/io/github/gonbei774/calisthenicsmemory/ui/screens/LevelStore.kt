package io.github.gonbei774.calisthenicsmemory.ui.screens

import io.github.gonbei774.calisthenicsmemory.platform.PlatformContext
import io.github.gonbei774.calisthenicsmemory.platform.settingsProvider
import io.github.gonbei774.calisthenicsmemory.platform.todayEpochDay
import io.github.gonbei774.calisthenicsmemory.platform.nowEpochMs
import app.calisthenics.data.settings.LevelStore as Shared
import app.calisthenics.domain.model.*
import app.calisthenics.domain.routine.SavedRoutine

object LevelStore {
 private fun provider(ctx: PlatformContext) = ctx.settingsProvider()
 fun load(ctx: PlatformContext) = Shared.load(provider(ctx))
 fun days(ctx: PlatformContext) = Shared.days(provider(ctx))
 fun save(ctx: PlatformContext, m: Map<String, Int>, today: Int = todayEpochDay().toInt(), restart: Boolean = false, undated: Boolean = false) = Shared.save(provider(ctx), m, today, restart, undated)
 fun actions(ctx: PlatformContext) = Shared.actions(provider(ctx))
 fun resetAtMs(ctx: PlatformContext) = Shared.resetAtMs(provider(ctx))
 fun markReset(ctx: PlatformContext, atMs: Long) = Shared.markReset(provider(ctx), atMs)
 fun snapshot(catalog: Catalog, levels: Map<String, Int>) = Shared.snapshot(catalog, levels)
}
