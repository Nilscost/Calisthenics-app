package io.github.gonbei774.calisthenicsmemory.ui.screens

import io.github.gonbei774.calisthenicsmemory.platform.PlatformContext
import io.github.gonbei774.calisthenicsmemory.platform.settingsProvider
import io.github.gonbei774.calisthenicsmemory.platform.todayEpochDay
import io.github.gonbei774.calisthenicsmemory.platform.nowEpochMs
import app.calisthenics.data.settings.SavedRoutineStore as Shared
import app.calisthenics.domain.model.*
import app.calisthenics.domain.routine.SavedRoutine

object SavedRoutineStore {
 private fun provider(ctx: PlatformContext) = ctx.settingsProvider()
 fun load(ctx: PlatformContext) = Shared.load(provider(ctx))
 fun get(ctx: PlatformContext, id: String?) = Shared.get(provider(ctx), id)
 fun saveAll(ctx: PlatformContext, list: List<SavedRoutine>) = Shared.saveAll(provider(ctx), list)
 fun upsert(ctx: PlatformContext, r: SavedRoutine) = Shared.upsert(provider(ctx), r)
 fun delete(ctx: PlatformContext, id: String) = Shared.delete(provider(ctx), id)
}
