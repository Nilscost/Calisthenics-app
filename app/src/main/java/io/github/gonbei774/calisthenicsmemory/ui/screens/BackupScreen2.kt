// T20 (first slice): export all new-model data to a user-chosen file; import validates fully before anything changes.
// Restore: merge history (never deletes/overwrites), replace settings + usual plan, safety copy first, refused while a workout is active.
package io.github.gonbei774.calisthenicsmemory.ui.screens

import io.github.gonbei774.calisthenicsmemory.ui.components.AppTextButton
import io.github.gonbei774.calisthenicsmemory.ui.components.AppOutlinedButton

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import io.github.gonbei774.calisthenicsmemory.BuildConfig
import io.github.gonbei774.calisthenicsmemory.R
import androidx.compose.ui.unit.dp
import app.calisthenics.domain.backup.*
import app.calisthenics.domain.history.BlockRecord
import app.calisthenics.domain.history.SessionRecord
import io.github.gonbei774.calisthenicsmemory.data.*
import io.github.gonbei774.calisthenicsmemory.session.CheckpointStore
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupScreen2(modifier: Modifier = Modifier, onBack: () -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var status by remember { mutableStateOf("") }
    var pending by remember { mutableStateOf<BackupPayload?>(null) }

    suspend fun buildPayload(): BackupPayload = BackupApply.buildPayload(ctx)
    val exporter = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            try {
                val payload = buildPayload()
                val text = exportBackup(payload, BuildConfig.VERSION_NAME, System.currentTimeMillis())
                ctx.contentResolver.openOutputStream(uri)?.use { it.write(text.toByteArray()) }
                status = ctx.getString(R.string.backup_exported, payload.sessions.size)
            } catch (e: Exception) { status = ctx.getString(R.string.backup_export_failed, e.message.orEmpty()) }
        }
    }
    val importer = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val text = try { ctx.contentResolver.openInputStream(uri)?.use { it.readBytes().take(20_000_000).toByteArray().decodeToString() } ?: "" } catch (e: Exception) { "" }
        when (val r = importBackup(text)) {
            is ImportResult.Rejected -> { pending = null; status = ctx.getString(R.string.backup_not_imported, r.reason) }
            is ImportResult.Ok -> { pending = r.payload; status = ctx.getString(R.string.backup_valid, r.payload.sessions.size) }
        }
    }
    val workoutActive = CheckpointStore.read(ctx) != null
    Scaffold(modifier = modifier, topBar = { TopAppBar(title = { Text(stringResource(R.string.settings_backup)) }, navigationIcon = { AppTextButton(onClick = onBack) { Text(stringResource(R.string.back)) } }) }) { pad ->
        Column(Modifier.padding(pad).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(stringResource(R.string.backup_intro))
            Button(onClick = { exporter.launch("calisthenics-backup.json") }) { Text(stringResource(R.string.backup_export)) }
            AppOutlinedButton(onClick = { importer.launch(arrayOf("application/json", "text/*", "*/*")) }) { Text(stringResource(R.string.backup_check)) }
            pending?.let { p ->
                Text(stringResource(R.string.backup_restore_warning), color = MaterialTheme.colorScheme.error)
                if (workoutActive) Text(stringResource(R.string.backup_workout_active))
                Button(enabled = !workoutActive, onClick = {
                    scope.launch {
                        try {
                            val dir = java.io.File(ctx.filesDir, "backups").apply { mkdirs() }
                            val safety = java.io.File(dir, "before-restore-${System.currentTimeMillis()}.json")
                            safety.writeText(exportBackup(buildPayload(), BuildConfig.VERSION_NAME, System.currentTimeMillis()))
                            val added = BackupApply.restore(ctx, p)
                            pending = null
                            status = ctx.getString(R.string.backup_restored, added, p.sessions.size - added, safety.name)
                        } catch (e: Exception) { status = ctx.getString(R.string.backup_restore_failed, e.message.orEmpty()) }
                    }
                }) { Text(stringResource(R.string.backup_confirm)) }
            }
            if (status.isNotEmpty()) Text(status)
        }
    }
}
