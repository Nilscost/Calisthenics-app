package io.github.gonbei774.calisthenicsmemory.platform

import androidx.compose.runtime.Composable

/** Backup file open/save. Android: Storage Access Framework pickers; web: download + file input (Phase 4). */
interface BackupFileLauncher {
    /**
     * Asks where to save. Once the person has chosen, [write] runs and hands the file text to `store`;
     * [write] owns its error handling (anything it throws is not caught here).
     */
    fun export(suggestedName: String, write: suspend (store: (String) -> Unit) -> Unit)

    /** Asks for a file and reports its text (empty when it cannot be read). */
    fun import(onText: (String) -> Unit)
}

@Composable
expect fun rememberBackupFiles(): BackupFileLauncher

/** Keeps the pre-restore safety copy in app-private storage (Android: `filesDir/backups`). */
expect fun PlatformContext.writeSafetyBackup(name: String, text: String)
