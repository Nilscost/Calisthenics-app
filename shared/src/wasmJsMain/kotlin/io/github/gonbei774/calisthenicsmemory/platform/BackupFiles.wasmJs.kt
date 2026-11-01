package io.github.gonbei774.calisthenicsmemory.platform

import androidx.compose.runtime.Composable

// Compile-only for Phase 3b: Phase 4 adds download + <input type=file>.
@Composable
actual fun rememberBackupFiles(): BackupFileLauncher = object : BackupFileLauncher {
    override fun export(suggestedName: String, write: suspend (store: (String) -> Unit) -> Unit) = Unit
    override fun import(onText: (String) -> Unit) = Unit
}

actual fun PlatformContext.writeSafetyBackup(name: String, text: String) = Unit
