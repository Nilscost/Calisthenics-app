package io.github.gonbei774.calisthenicsmemory.ui.screens

import android.content.Context
import app.calisthenics.data.history.RoomHistoryRepository
import app.calisthenics.domain.model.Catalog
import app.calisthenics.domain.progression.ProgressSnapshot
import io.github.gonbei774.calisthenicsmemory.data.AppDatabase
import kotlinx.datetime.TimeZone
import java.time.ZoneId

object ProgressLoader {
    suspend fun load(ctx: Context, catalog: Catalog, excludeSessionId: String? = null): ProgressSnapshot =
        app.calisthenics.data.history.ProgressLoader.load(
            RoomHistoryRepository(AppDatabase.getDatabase(ctx).historyDao()), catalog,
            TimeZone.of(ZoneId.systemDefault().id), LevelStore.resetAtMs(ctx),
            LevelStore.actions(ctx) + SuggestionStore.accepted(ctx), PrefsStore.load(ctx).autoProgression, excludeSessionId)
}
