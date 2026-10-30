package io.github.gonbei774.calisthenicsmemory.ui.history

import android.content.Context
import app.calisthenics.data.history.RepositoryHistorySource
import app.calisthenics.data.history.RoomHistoryRepository
import io.github.gonbei774.calisthenicsmemory.data.AppDatabase

typealias StoredFeedback = app.calisthenics.data.history.StoredFeedback
typealias StoredSession = app.calisthenics.data.history.StoredSession
typealias HistorySource = app.calisthenics.data.history.HistorySource
typealias RoundCorrection = app.calisthenics.data.history.RoundCorrection

class RoomHistorySource(ctx: Context) : HistorySource by RepositoryHistorySource(
    RoomHistoryRepository(AppDatabase.getDatabase(ctx).historyDao()), { System.currentTimeMillis() })
