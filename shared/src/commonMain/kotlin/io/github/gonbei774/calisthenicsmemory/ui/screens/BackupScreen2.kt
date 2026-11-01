// T20 (first slice): export all new-model data to a user-chosen file; import validates fully before anything changes.
// Restore: merge history (never deletes/overwrites), replace settings + usual plan, safety copy first, refused while a workout is active.
package io.github.gonbei774.calisthenicsmemory.ui.screens

import io.github.gonbei774.calisthenicsmemory.platform.nowEpochMs
import io.github.gonbei774.calisthenicsmemory.platform.currentPlatformContext
import io.github.gonbei774.calisthenicsmemory.ui.components.AppTextButton
import io.github.gonbei774.calisthenicsmemory.ui.components.AppOutlinedButton

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.stringResource
import io.github.gonbei774.calisthenicsmemory.shared.resources.*
import androidx.compose.ui.unit.dp
import app.calisthenics.domain.backup.*
import app.calisthenics.domain.history.BlockRecord
import app.calisthenics.domain.history.SessionRecord
import io.github.gonbei774.calisthenicsmemory.session.CheckpointStore
import io.github.gonbei774.calisthenicsmemory.platform.*
import org.jetbrains.compose.resources.getString
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupScreen2(modifier: Modifier = Modifier, onBack: () -> Unit) {
    val ctx = currentPlatformContext()
    val scope = rememberCoroutineScope()
    var status by remember { mutableStateOf("") }
    var pending by remember { mutableStateOf<BackupPayload?>(null) }
    val version = remember { ctx.appVersionName() }

    suspend fun buildPayload(): BackupPayload = BackupApply.buildPayload(ctx)
    val files = rememberBackupFiles()
    fun exportBackupFile() = files.export("calisthenics-backup.json") { store ->
        try {
            val payload = buildPayload()
            val text = exportBackup(payload, version, nowEpochMs())
            store(text)
            status = getString(Res.string.backup_exported, payload.sessions.size)
        } catch (e: Exception) { status = getString(Res.string.backup_export_failed, e.message.orEmpty()) }
    }
    fun checkBackupFile() = files.import { text ->
        scope.launch {
            when (val r = importBackup(text)) {
                is ImportResult.Rejected -> { pending = null; status = getString(Res.string.backup_not_imported, r.reason) }
                is ImportResult.Ok -> { pending = r.payload; status = getString(Res.string.backup_valid, r.payload.sessions.size) }
            }
        }
    }
    val workoutActive = CheckpointStore.read(ctx) != null
    Scaffold(modifier = modifier, topBar = { TopAppBar(title = { Text(stringResource(Res.string.settings_backup)) }, navigationIcon = { AppTextButton(onClick = onBack) { Text(stringResource(Res.string.back)) } }) }) { pad ->
        Column(Modifier.padding(pad).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(stringResource(Res.string.backup_intro))
            Button(onClick = { exportBackupFile() }) { Text(stringResource(Res.string.backup_export)) }
            AppOutlinedButton(onClick = { checkBackupFile() }) { Text(stringResource(Res.string.backup_check)) }
            pending?.let { p ->
                Text(stringResource(Res.string.backup_restore_warning), color = MaterialTheme.colorScheme.error)
                if (workoutActive) Text(stringResource(Res.string.backup_workout_active))
                Button(enabled = !workoutActive, onClick = {
                    scope.launch {
                        try {
                            val name = "before-restore-${nowEpochMs()}.json"
                            ctx.writeSafetyBackup(name, exportBackup(buildPayload(), version, nowEpochMs()))
                            val added = BackupApply.restore(ctx, p)
                            pending = null
                            status = getString(Res.string.backup_restored, added, p.sessions.size - added, name)
                        } catch (e: Exception) { status = getString(Res.string.backup_restore_failed, e.message.orEmpty()) }
                    }
                }) { Text(stringResource(Res.string.backup_confirm)) }
            }
            if (status.isNotEmpty()) Text(status)
        }
    }
}
