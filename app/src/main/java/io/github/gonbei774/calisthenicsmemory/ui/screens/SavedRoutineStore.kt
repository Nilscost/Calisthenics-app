package io.github.gonbei774.calisthenicsmemory.ui.screens

import android.content.Context
import app.calisthenics.data.settings.AndroidSettingsProvider
import app.calisthenics.data.settings.SavedRoutineStore as Shared
import app.calisthenics.domain.model.*
import app.calisthenics.domain.routine.SavedRoutine

object SavedRoutineStore {
 private fun provider(ctx: Context) = AndroidSettingsProvider(ctx)
 fun load(ctx: Context) = Shared.load(provider(ctx))
 fun get(ctx: Context, id: String?) = Shared.get(provider(ctx), id)
 fun saveAll(ctx: Context, list: List<SavedRoutine>) = Shared.saveAll(provider(ctx), list)
 fun upsert(ctx: Context, r: SavedRoutine) = Shared.upsert(provider(ctx), r)
 fun delete(ctx: Context, id: String) = Shared.delete(provider(ctx), id)
}
