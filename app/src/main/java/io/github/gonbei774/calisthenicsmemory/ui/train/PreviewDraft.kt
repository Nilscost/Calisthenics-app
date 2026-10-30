package io.github.gonbei774.calisthenicsmemory.ui.train

import android.content.Context
import app.calisthenics.data.settings.AndroidSettingsProvider
import app.calisthenics.domain.planner.TrainSettings

typealias PreviewDraft = app.calisthenics.domain.planner.PreviewDraft
fun draftKey(s: TrainSettings) = app.calisthenics.domain.planner.draftKey(s)
object PreviewDraftStore {
 fun get(ctx: Context, key: String) = app.calisthenics.data.settings.PreviewDraftStore.get(AndroidSettingsProvider(ctx), key, java.time.LocalDate.now().toEpochDay())
 fun put(ctx: Context, draft: PreviewDraft) = app.calisthenics.data.settings.PreviewDraftStore.put(AndroidSettingsProvider(ctx), draft, java.time.LocalDate.now().toEpochDay())
 fun clear(ctx: Context) = app.calisthenics.data.settings.PreviewDraftStore.clear(AndroidSettingsProvider(ctx))
}
