package io.github.gonbei774.calisthenicsmemory.platform

import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch
import java.io.File

private class Pending {
    var export: (suspend ((String) -> Unit) -> Unit)? = null
    var import: ((String) -> Unit)? = null
}

@Composable
actual fun rememberBackupFiles(): BackupFileLauncher {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    // The pending request lives across recompositions; the launchers read it when the picker returns.
    val pending = remember { Pending() }
    val exporter = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        val body = pending.export
        if (uri == null || body == null) return@rememberLauncherForActivityResult
        scope.launch { body { text -> ctx.contentResolver.openOutputStream(uri)?.use { it.write(text.toByteArray()) } } }
    }
    val importer = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        val body = pending.import
        if (uri == null || body == null) return@rememberLauncherForActivityResult
        val text = try { ctx.contentResolver.openInputStream(uri)?.use { it.readBytes().take(20_000_000).toByteArray().decodeToString() } ?: "" } catch (e: Exception) { "" }
        body(text)
    }
    return object : BackupFileLauncher {
        override fun export(suggestedName: String, write: suspend (store: (String) -> Unit) -> Unit) {
            pending.export = write
            exporter.launch(suggestedName)
        }
        override fun import(onText: (String) -> Unit) {
            pending.import = onText
            importer.launch(arrayOf("application/json", "text/*", "*/*"))
        }
    }
}

actual fun Context.writeSafetyBackup(name: String, text: String) {
    val dir = File(filesDir, "backups").apply { mkdirs() }
    File(dir, name).writeText(text)
}
