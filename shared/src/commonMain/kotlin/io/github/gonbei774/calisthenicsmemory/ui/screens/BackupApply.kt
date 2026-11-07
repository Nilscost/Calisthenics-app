package io.github.gonbei774.calisthenicsmemory.ui.screens

import app.calisthenics.data.settings.StoreBackupSettings
import app.calisthenics.domain.backup.BackupPayload
import io.github.gonbei774.calisthenicsmemory.platform.PlatformContext
import io.github.gonbei774.calisthenicsmemory.platform.historyRepository
import io.github.gonbei774.calisthenicsmemory.platform.settingsProvider
import io.github.gonbei774.calisthenicsmemory.platform.todayEpochDay

/** Composition of the shared backup implementation over the platform stores. */
object BackupApply {
    private fun shared(ctx: PlatformContext, day: Long) = app.calisthenics.data.history.BackupApply(
        ctx.historyRepository(), StoreBackupSettings(ctx.settingsProvider(), day))
    suspend fun buildPayload(ctx: PlatformContext): BackupPayload {
        val day = todayEpochDay()
        return shared(ctx, day).buildPayload(day)
    }
    suspend fun restore(ctx: PlatformContext, p: BackupPayload): Int {
        val day = todayEpochDay()
        return shared(ctx, day).restore(p, day)
    }
}
