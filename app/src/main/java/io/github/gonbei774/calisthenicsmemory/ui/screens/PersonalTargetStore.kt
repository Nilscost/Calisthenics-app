package io.github.gonbei774.calisthenicsmemory.ui.screens

import android.content.Context
import app.calisthenics.data.settings.AndroidSettingsProvider
import app.calisthenics.data.settings.PersonalTargetStore as Shared
import app.calisthenics.domain.model.*
import app.calisthenics.domain.routine.SavedRoutine

object PersonalTargetStore {
 private fun provider(ctx: Context) = AndroidSettingsProvider(ctx)
 fun load(ctx: Context) = Shared.load(provider(ctx))
 fun saveAll(ctx: Context, values: Map<String, PersonalLevelTargets>) = Shared.saveAll(provider(ctx), values)
 fun save(ctx: Context, exerciseId: String, values: List<Int>): PersonalLevelTargets {
  val catalog = ctx.assets.open("catalog.json").bufferedReader().use { app.calisthenics.domain.content.parseCatalog(it.readText()) }
  return Shared.save(provider(ctx), exerciseId, values, catalog, System.currentTimeMillis())
 }
 fun reset(ctx: Context, catalog: Catalog, exerciseId: String) { val authored = catalog.copy(personalTargets = emptyMap()).policyForVariation(exerciseId) ?: return; save(ctx, exerciseId, authored.tiers.sortedBy { it.index }.map { it.target.value }) }
 fun effective(ctx: Context, catalog: Catalog) = Shared.effective(provider(ctx), catalog)
}
