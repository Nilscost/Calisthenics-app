package io.github.gonbei774.calisthenicsmemory.ui.screens

import android.content.Context
import app.calisthenics.data.settings.AndroidSettingsProvider
import app.calisthenics.data.settings.SuggestionStore as Shared
import app.calisthenics.domain.model.*
import app.calisthenics.domain.routine.SavedRoutine

object SuggestionStore {
 private fun provider(ctx: Context) = AndroidSettingsProvider(ctx)
 fun dismissed(ctx: Context) = Shared.dismissed(provider(ctx))
 fun dismiss(ctx: Context, id: String) = Shared.dismiss(provider(ctx), id)
 fun accept(ctx: Context, variationId: String, tier: Int, day: Int) = Shared.accept(provider(ctx), variationId, tier, day)
 fun acceptNext(ctx: Context, variationId: String, toVariationId: String, day: Int) = Shared.acceptNext(provider(ctx), variationId, toVariationId, day)
 fun clearAll(ctx: Context) = Shared.clearAll(provider(ctx))
 fun accepted(ctx: Context) = Shared.accepted(provider(ctx))
}
