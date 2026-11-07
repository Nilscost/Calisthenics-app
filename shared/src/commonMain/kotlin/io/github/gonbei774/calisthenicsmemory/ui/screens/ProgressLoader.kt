package io.github.gonbei774.calisthenicsmemory.ui.screens

import app.calisthenics.domain.model.Catalog
import app.calisthenics.domain.progression.ProgressSnapshot
import io.github.gonbei774.calisthenicsmemory.platform.PlatformContext
import io.github.gonbei774.calisthenicsmemory.platform.historyRepository
import io.github.gonbei774.calisthenicsmemory.platform.systemTimeZone

object ProgressLoader {
    suspend fun load(ctx: PlatformContext, catalog: Catalog, excludeSessionId: String? = null): ProgressSnapshot =
        app.calisthenics.data.history.ProgressLoader.load(
            ctx.historyRepository(), catalog,
            systemTimeZone(), LevelStore.resetAtMs(ctx),
            LevelStore.actions(ctx) + SuggestionStore.accepted(ctx), PrefsStore.load(ctx).autoProgression, excludeSessionId)
}
