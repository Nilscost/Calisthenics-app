package io.github.gonbei774.calisthenicsmemory.ui.screens

import android.content.Context
import app.calisthenics.data.settings.AndroidSettingsProvider
import app.calisthenics.data.settings.LevelStore as Shared
import app.calisthenics.domain.model.*
import app.calisthenics.domain.routine.SavedRoutine

object LevelStore {
 private fun provider(ctx: Context) = AndroidSettingsProvider(ctx)
 fun load(ctx: Context) = Shared.load(provider(ctx))
 fun days(ctx: Context) = Shared.days(provider(ctx))
 fun save(ctx: Context, m: Map<String, Int>, today: Int = java.time.LocalDate.now().toEpochDay().toInt(), restart: Boolean = false, undated: Boolean = false) = Shared.save(provider(ctx), m, today, restart, undated)
 fun actions(ctx: Context) = Shared.actions(provider(ctx))
 fun resetAtMs(ctx: Context) = Shared.resetAtMs(provider(ctx))
 fun markReset(ctx: Context, atMs: Long) = Shared.markReset(provider(ctx), atMs)
 fun snapshot(catalog: Catalog, levels: Map<String, Int>) = Shared.snapshot(catalog, levels)
}
