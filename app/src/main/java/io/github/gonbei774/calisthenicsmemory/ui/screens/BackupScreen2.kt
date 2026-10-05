// T20 (first slice): export all new-model data to a user-chosen file; import validates fully before anything changes.
// Restore: merge history (never deletes/overwrites), replace settings + usual plan, safety copy first, refused while a workout is active.
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
import io.github.gonbei774.calisthenicsmemory.data.*
import io.github.gonbei774.calisthenicsmemory.session.CheckpointStore
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupScreen2(onBack: () -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var status by remember { mutableStateOf("") }
    var pending by remember { mutableStateOf<BackupPayload?>(null) }

    suspend fun buildPayload(): BackupPayload {
        val dao = AppDatabase.getDatabase(ctx).historyDao()
        val sessions = dao.sessions()
        return BackupPayload(
            preferences = PrefsStore.load(ctx), routines = listOf(RoutineStore.load(ctx)),
            plans = sessions.mapNotNull { dao.plan(it.planId) }.distinctBy { it.planId }.map { PlanRecord(it.planId, it.planJson) },
            sessions = sessions.map { s -> SessionRecord(s.sessionId, s.planId, s.startedAtEpochMs, s.endedAtEpochMs, s.status,
                dao.blockResults(s.sessionId).map { BlockRecord(it.blockId, null, "WORK", it.outcome, it.actualSeconds, it.achievedValue) }) },
            feedback = dao.allFeedback().map { FeedbackRecord(it.sessionId, it.variationId, it.revision, it.rating, it.discomfort, it.assumedMet, it.createdAtEpochMs) },
            events = dao.allEvents().map { ProgressionEventRecord(it.eventId, it.variationId, it.kind, it.fromTier, it.toTier, it.reason, it.atEpochMs) })
    }
    val exporter = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            try {
                val payload = buildPayload()
                val text = exportBackup(payload, "0.2.0-m5", System.currentTimeMillis())
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
            is ImportResult.Ok -> { pending = r.payload; status = "File is valid: ${r.payload.sessions.size} session(s). Confirm to restore." }
        }
    }
    val workoutActive = CheckpointStore.read(ctx) != null
    Scaffold(topBar = { TopAppBar(title = { Text("Backup") }, navigationIcon = { TextButton(onClick = onBack) { Text("Back") } }) }) { pad ->
        Column(Modifier.padding(pad).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Backups are plain files you choose where to save. Demo clips ship inside the app and are not part of backups.")
            Button(onClick = { exporter.launch("calisthenics-backup.json") }) { Text("Export backup") }
            OutlinedButton(onClick = { importer.launch(arrayOf("application/json", "text/*", "*/*")) }) { Text("Check a backup file") }
            pending?.let { p ->
                Text("Restore replaces your settings and usual plan with the file's. Workout history from the file is ADDED to what you have; nothing existing is deleted or overwritten. A safety copy of your current data is saved first.", color = MaterialTheme.colorScheme.error)
                if (workoutActive) Text("A workout is in progress or waiting for recovery. Finish or save it on the Today screen first.")
                Button(enabled = !workoutActive, onClick = {
                    scope.launch {
                        try {
                            val dir = java.io.File(ctx.filesDir, "backups").apply { mkdirs() }
                            val safety = java.io.File(dir, "before-restore-${System.currentTimeMillis()}.json")
                            safety.writeText(exportBackup(buildPayload(), "0.2.0-m5", System.currentTimeMillis()))
                            val dao = AppDatabase.getDatabase(ctx).historyDao()
                            val pj = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }
                            val plans = p.plans.map { pr ->
                                val wp = pj.decodeFromString(app.calisthenics.domain.model.WorkoutPlan.serializer(), pr.planJson)
                                PlanSnapshotEntity(pr.planId, wp.createdAtEpochMs, wp.routineId, wp.routineRevision, wp.catalogVersion, wp.profileId, pr.planJson)
                            }
                            val sessions = p.sessions.map { WorkoutSessionEntity(it.sessionId, it.planId, it.startedAtEpochMs, it.endedAtEpochMs, it.status) }
                            val blocks = p.sessions.flatMap { s -> s.blocks.map { BlockResultEntity(sessionId = s.sessionId, blockId = it.blockId, outcome = it.outcome, actualSeconds = it.actualSeconds, achievedValue = it.achievedValue) } }
                            val fb = p.feedback.map { FeedbackRevisionEntity(sessionId = it.sessionId, variationId = it.variationId, revision = it.revision, rating = it.rating, discomfort = it.discomfort, assumedMet = it.assumedMet, createdAtEpochMs = it.createdAtEpochMs) }
                            val ev = p.events.map { ProgressionEventEntity(it.eventId, it.variationId, it.kind, it.fromTier, it.toTier, it.reason, it.atEpochMs) }
                            val added = dao.restoreMerge(plans, sessions, blocks, fb, ev)
                            PrefsStore.save(ctx, p.preferences); p.routines.firstOrNull()?.let { RoutineStore.save(ctx, it) }
                            pending = null
                            status = "Restored. $added new workout(s) added; ${p.sessions.size - added} already on this phone. Safety copy: ${safety.name} (in the app's private storage)."
                        } catch (e: Exception) { status = "Restore failed, nothing half-applied: ${e.message}" }
                    }
                }) { Text("Confirm restore") }
            }
            if (status.isNotEmpty()) Text(status)
        }
    }
}
