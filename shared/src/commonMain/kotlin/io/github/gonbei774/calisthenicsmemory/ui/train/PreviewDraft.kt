package io.github.gonbei774.calisthenicsmemory.ui.train

import io.github.gonbei774.calisthenicsmemory.platform.PlatformContext
import io.github.gonbei774.calisthenicsmemory.platform.settingsProvider
import io.github.gonbei774.calisthenicsmemory.platform.todayEpochDay
import io.github.gonbei774.calisthenicsmemory.platform.nowEpochMs
import app.calisthenics.domain.planner.TrainSettings

typealias PreviewDraft = app.calisthenics.domain.planner.PreviewDraft
fun draftKey(s: TrainSettings) = app.calisthenics.domain.planner.draftKey(s)
object PreviewDraftStore {
 fun get(ctx: PlatformContext, key: String) = app.calisthenics.data.settings.PreviewDraftStore.get(ctx.settingsProvider(), key, todayEpochDay())
 fun put(ctx: PlatformContext, draft: PreviewDraft) = app.calisthenics.data.settings.PreviewDraftStore.put(ctx.settingsProvider(), draft, todayEpochDay())
 fun clear(ctx: PlatformContext) = app.calisthenics.data.settings.PreviewDraftStore.clear(ctx.settingsProvider())
}
