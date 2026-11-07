package io.github.gonbei774.calisthenicsmemory.ui.screens

import io.github.gonbei774.calisthenicsmemory.platform.PlatformContext
import io.github.gonbei774.calisthenicsmemory.platform.settingsProvider
import io.github.gonbei774.calisthenicsmemory.platform.todayEpochDay
import io.github.gonbei774.calisthenicsmemory.platform.nowEpochMs
import app.calisthenics.data.settings.ProfileStore as Shared
import app.calisthenics.domain.model.*
import app.calisthenics.domain.routine.SavedRoutine

object ProfileStore {
 private fun provider(ctx: PlatformContext) = ctx.settingsProvider()
 fun load(ctx: PlatformContext) = Shared.load(provider(ctx))
 fun save(ctx: PlatformContext, list: List<EquipmentProfile>) = Shared.save(provider(ctx), list)
 fun upsert(ctx: PlatformContext, profile: EquipmentProfile) = Shared.upsert(provider(ctx), profile)
 fun delete(ctx: PlatformContext, id: String) = Shared.delete(provider(ctx), id)
 fun selected(ctx: PlatformContext) = Shared.selected(provider(ctx))
 fun select(ctx: PlatformContext, id: String) = Shared.select(provider(ctx), id)
}
