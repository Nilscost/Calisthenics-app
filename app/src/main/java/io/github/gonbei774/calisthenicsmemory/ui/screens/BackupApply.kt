package io.github.gonbei774.calisthenicsmemory.ui.screens

import android.content.Context
import app.calisthenics.data.history.RoomHistoryRepository
import app.calisthenics.data.settings.AndroidSettingsProvider
import app.calisthenics.data.settings.StoreBackupSettings
import app.calisthenics.domain.backup.BackupPayload
import io.github.gonbei774.calisthenicsmemory.data.AppDatabase
import java.time.LocalDate

/** Android composition for the shared backup implementation. */
object BackupApply {
    private fun shared(ctx: Context, day: Long) = app.calisthenics.data.history.BackupApply(
        RoomHistoryRepository(AppDatabase.getDatabase(ctx).historyDao()), StoreBackupSettings(AndroidSettingsProvider(ctx), day))
    suspend fun buildPayload(ctx: Context): BackupPayload {
        val day = LocalDate.now().toEpochDay()
        return shared(ctx, day).buildPayload(day)
    }
    suspend fun restore(ctx: Context, p: BackupPayload): Int {
        val day = LocalDate.now().toEpochDay()
        return shared(ctx, day).restore(p, day)
    }
}
