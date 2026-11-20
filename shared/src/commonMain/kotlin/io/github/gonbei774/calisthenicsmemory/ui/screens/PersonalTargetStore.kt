package io.github.gonbei774.calisthenicsmemory.ui.screens

import io.github.gonbei774.calisthenicsmemory.platform.PlatformContext
import io.github.gonbei774.calisthenicsmemory.platform.settingsProvider
import io.github.gonbei774.calisthenicsmemory.platform.todayEpochDay
import io.github.gonbei774.calisthenicsmemory.platform.nowEpochMs
import io.github.gonbei774.calisthenicsmemory.platform.readCatalogText
import app.calisthenics.data.settings.PersonalTargetStore as Shared
import app.calisthenics.domain.model.*
import app.calisthenics.domain.routine.SavedRoutine

object PersonalTargetStore {
 private fun provider(ctx: PlatformContext) = ctx.settingsProvider()
 fun load(ctx: PlatformContext) = Shared.load(provider(ctx))
 fun saveAll(ctx: PlatformContext, values: Map<String, PersonalLevelTargets>) = Shared.saveAll(provider(ctx), values)
 fun save(ctx: PlatformContext, exerciseId: String, values: List<Int>): PersonalLevelTargets {
  val catalog = app.calisthenics.domain.content.parseCatalog(ctx.readCatalogText())
  return Shared.save(provider(ctx), exerciseId, values, catalog, nowEpochMs())
 }
 fun reset(ctx: PlatformContext, catalog: Catalog, exerciseId: String) { val authored = catalog.copy(personalTargets = emptyMap()).policyForVariation(exerciseId) ?: return; save(ctx, exerciseId, authored.tiers.sortedBy { it.index }.map { it.target.value }) }
 fun effective(ctx: PlatformContext, catalog: Catalog) = Shared.effective(provider(ctx), catalog)
}
