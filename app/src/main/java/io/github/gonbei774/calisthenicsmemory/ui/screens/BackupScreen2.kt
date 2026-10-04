// T20 (first slice): export all new-model data to a user-chosen file; import validates fully before anything changes.
// Import currently restores settings + usual plan only; restoring history rows into Room is NOT built yet.
package io.github.gonbei774.calisthenicsmemory.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import app.calisthenics.domain.backup.*
import app.calisthenics.domain.history.BlockRecord
import app.calisthenics.domain.history.SessionRecord
import io.github.gonbei774.calisthenicsmemory.data.AppDatabase
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupScreen2(onBack: () -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var status by remember { mutableStateOf("") }
    var pending by remember { mutableStateOf<BackupPayload?>(null) }

    val exporter = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            try {
                val dao = AppDatabase.getDatabase(ctx).historyDao()
                val sessions = dao.sessions()
                val payload = BackupPayload(
                    preferences = PrefsStore.load(ctx), routines = listOf(RoutineStore.load(ctx)),
                    plans = sessions.mapNotNull { dao.plan(it.planId) }.distinctBy { it.planId }.map { PlanRecord(it.planId, it.planJson) },
                    sessions = sessions.map { s -> SessionRecord(s.sessionId, s.planId, s.startedAtEpochMs, s.endedAtEpochMs, s.status,
                        dao.blockResults(s.sessionId).map { BlockRecord(it.blockId, null, "WORK", it.outcome, it.actualSeconds) }) },
                    feedback = emptyList(), events = emptyList())
                val text = exportBackup(payload, "0.2.0-m0", System.currentTimeMillis())
                ctx.contentResolver.openOutputStream(uri)?.use { it.write(text.toByteArray()) }
                status = "Exported ${payload.sessions.size} session(s). The file is NOT encrypted; keep it private."
            } catch (e: Exception) { status = "Export failed: ${e.message}" }
        }
    }
    val importer = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val text = try { ctx.contentResolver.openInputStream(uri)?.use { it.readBytes().take(20_000_000).toByteArray().decodeToString() } ?: "" } catch (e: Exception) { "" }
        when (val r = importBackup(text)) {
            is ImportResult.Rejected -> { pending = null; status = "Not imported: ${r.reason} Nothing was changed." }
            is ImportResult.Ok -> { pending = r.payload; status = "File is valid: ${r.payload.sessions.size} session(s). Confirm to restore settings and usual plan." }
        }
    }
    Scaffold(topBar = { TopAppBar(title = { Text("Backup") }, navigationIcon = { TextButton(onClick = onBack) { Text("Back") } }) }) { pad ->
        Column(Modifier.padding(pad).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Backups are plain files you choose where to save. Demo videos are not included.")
            Button(onClick = { exporter.launch("calisthenics-backup.json") }) { Text("Export backup") }
            OutlinedButton(onClick = { importer.launch(arrayOf("application/json", "text/*", "*/*")) }) { Text("Check a backup file") }
            pending?.let { p ->
                Text("Restoring replaces your current settings and usual plan. Workout history restore is not built yet.", color = MaterialTheme.colorScheme.error)
                Button(onClick = {
                    PrefsStore.save(ctx, p.preferences); p.routines.firstOrNull()?.let { RoutineStore.save(ctx, it) }
                    pending = null; status = "Settings and usual plan restored."
                }) { Text("Confirm restore (settings + plan)") }
            }
            if (status.isNotEmpty()) Text(status)
        }
    }
}
