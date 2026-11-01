package io.github.gonbei774.calisthenicsmemory.ui.screens

import io.github.gonbei774.calisthenicsmemory.platform.PlatformContext
import io.github.gonbei774.calisthenicsmemory.platform.settingsProvider
import io.github.gonbei774.calisthenicsmemory.platform.todayEpochDay
import io.github.gonbei774.calisthenicsmemory.platform.nowEpochMs
import app.calisthenics.data.settings.SuggestionStore as Shared
import app.calisthenics.domain.model.*
import app.calisthenics.domain.routine.SavedRoutine

object SuggestionStore {
 private fun provider(ctx: PlatformContext) = ctx.settingsProvider()
 fun dismissed(ctx: PlatformContext) = Shared.dismissed(provider(ctx))
 fun dismiss(ctx: PlatformContext, id: String) = Shared.dismiss(provider(ctx), id)
 fun accept(ctx: PlatformContext, variationId: String, tier: Int, day: Int) = Shared.accept(provider(ctx), variationId, tier, day)
 fun acceptNext(ctx: PlatformContext, variationId: String, toVariationId: String, day: Int) = Shared.acceptNext(provider(ctx), variationId, toVariationId, day)
 fun clearAll(ctx: PlatformContext) = Shared.clearAll(provider(ctx))
 fun accepted(ctx: PlatformContext) = Shared.accepted(provider(ctx))
}
