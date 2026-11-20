package io.github.gonbei774.calisthenicsmemory.ui.history

import app.calisthenics.data.history.RepositoryHistorySource
import io.github.gonbei774.calisthenicsmemory.platform.PlatformContext
import io.github.gonbei774.calisthenicsmemory.platform.historyRepository
import io.github.gonbei774.calisthenicsmemory.platform.nowEpochMs

typealias StoredFeedback = app.calisthenics.data.history.StoredFeedback
typealias StoredSession = app.calisthenics.data.history.StoredSession
typealias HistorySource = app.calisthenics.data.history.HistorySource
typealias RoundCorrection = app.calisthenics.data.history.RoundCorrection

/** The platform's history store (Room on Android, IndexedDB on the web) as a [HistorySource]. */
class RoomHistorySource(ctx: PlatformContext) : HistorySource by RepositoryHistorySource(ctx.historyRepository(), { nowEpochMs() })
