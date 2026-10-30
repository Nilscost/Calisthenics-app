package io.github.gonbei774.calisthenicsmemory.ui.screens

import android.content.Context
import app.calisthenics.data.settings.AndroidSettingsProvider
import app.calisthenics.data.settings.ProfileStore as Shared
import app.calisthenics.domain.model.*
import app.calisthenics.domain.routine.SavedRoutine

object ProfileStore {
 private fun provider(ctx: Context) = AndroidSettingsProvider(ctx)
 fun load(ctx: Context) = Shared.load(provider(ctx))
 fun save(ctx: Context, list: List<EquipmentProfile>) = Shared.save(provider(ctx), list)
 fun upsert(ctx: Context, profile: EquipmentProfile) = Shared.upsert(provider(ctx), profile)
 fun delete(ctx: Context, id: String) = Shared.delete(provider(ctx), id)
 fun selected(ctx: Context) = Shared.selected(provider(ctx))
 fun select(ctx: Context, id: String) = Shared.select(provider(ctx), id)
}
