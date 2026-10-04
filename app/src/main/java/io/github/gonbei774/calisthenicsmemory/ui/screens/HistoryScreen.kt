// T19 (first slice): reads the append-only history tables, shows weekly summaries. Empty until real sessions exist (M5).
package io.github.gonbei774.calisthenicsmemory.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import app.calisthenics.domain.history.BlockRecord
import app.calisthenics.domain.history.SessionRecord
import app.calisthenics.domain.history.weeklySummaries
import io.github.gonbei774.calisthenicsmemory.data.AppDatabase
import java.time.ZoneId

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(onBack: () -> Unit) {
    val ctx = LocalContext.current
    var sessions by remember { mutableStateOf<List<SessionRecord>?>(null) }
    LaunchedEffect(Unit) {
        val dao = AppDatabase.getDatabase(ctx).historyDao()
        sessions = dao.sessions().map { s ->
            SessionRecord(s.sessionId, s.planId, s.startedAtEpochMs, s.endedAtEpochMs, s.status,
                dao.blockResults(s.sessionId).map { BlockRecord(it.blockId, null, "WORK", it.outcome, it.actualSeconds) })
        }
    }
    Scaffold(topBar = { TopAppBar(title = { Text("History") }, navigationIcon = { TextButton(onClick = onBack) { Text("Back") } }) }) { pad ->
        Column(Modifier.padding(pad).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            val list = sessions
            when {
                list == null -> Text("Loading…")
                list.isEmpty() -> Text("No finished workouts yet. Missed weeks are never penalised.")
                else -> {
                    val zone = ZoneId.systemDefault()
                    weeklySummaries(list, zone).forEach { w ->
                        Card(Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(12.dp)) {
                                Text("Week of ${w.weekStart}", style = MaterialTheme.typography.titleSmall)
                                Text("${w.sessions} session(s): ${w.completed} complete, ${w.partial} partial")
                                Text("Work ${w.workSeconds / 60} min, stretch ${w.stretchSeconds / 60} min, skipped blocks ${w.skippedBlocks}")
                            }
                        }
                    }
                }
            }
        }
    }
}
