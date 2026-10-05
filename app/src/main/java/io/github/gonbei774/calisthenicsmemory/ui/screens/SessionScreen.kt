// T15/T17 (first slice): live workout screen. Observes the service-owned state; sends commands only.
package io.github.gonbei774.calisthenicsmemory.ui.screens

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import app.calisthenics.domain.model.BlockType
import app.calisthenics.domain.model.TargetType
import app.calisthenics.domain.session.Phase
import io.github.gonbei774.calisthenicsmemory.session.SessionBus
import io.github.gonbei774.calisthenicsmemory.session.WorkoutSessionService
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SessionScreen(onExit: () -> Unit) {
    val ctx = LocalContext.current
    val s by SessionBus.state.collectAsState()
    var nowTick by remember { mutableStateOf(android.os.SystemClock.elapsedRealtime()) }
    LaunchedEffect(Unit) { while (true) { nowTick = android.os.SystemClock.elapsedRealtime(); delay(250) } }
    fun cmd(a: String) { ctx.startService(Intent(ctx, WorkoutSessionService::class.java).setAction(a)) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.spacedBy(14.dp), horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
        val st = s
        if (st == null) { Text("Starting…"); return@Column }
        val b = st.currentBlock
        if (st.isTerminal) {
            Text(if (st.phase == Phase.COMPLETED) "Workout complete" else "Workout finished early", style = MaterialTheme.typography.headlineMedium)
            Text("Saved to your history (blocks done: ${st.executions.values.count { it.name == "COMPLETED" }}).")
            FeedbackForm(st.sessionId, st.plan.blocks.filter { it.type == BlockType.WORK && it.variationId != null && st.executions[it.id]?.name == "COMPLETED" }.mapNotNull { it.variationId }.distinct(), waitSaved = true,
                targets = st.plan.blocks.filter { it.type == BlockType.WORK && it.target?.type == TargetType.REPS && it.variationId != null }.associate { it.variationId!! to it.target!!.value })
            Button(onClick = { SessionBus.clear(); onExit() }) { Text("Done") }
            return@Column
        }
        if (b != null) {
            val name = b.variationId?.let { SessionBus.names[it] ?: it } ?: when (b.type) { BlockType.PASSIVE_RECOVERY, BlockType.TRANSITION -> "Rest"; else -> b.type.name.lowercase() }
            Text("Block ${st.blockIndex + 1} of ${st.plan.blocks.size}", style = MaterialTheme.typography.labelLarge)
            Text(name, style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
            b.target?.let { Text(if (it.type == TargetType.REPS) "${it.value} reps" else "${it.value}s hold", style = MaterialTheme.typography.titleLarge) }
            b.variationId?.let { v -> DemoClips.file(ctx, v)?.let { DemoPlayer(it) } }
            val rem = (st.remainingAt(nowTick) + 999) / 1000
            Text("%d:%02d".format(rem / 60, rem % 60), style = MaterialTheme.typography.displayLarge, maxLines = 1, modifier = Modifier.semantics { contentDescription = "$name, ${rem / 60} minutes ${rem % 60} seconds remaining" })
            st.plan.blocks.getOrNull(st.blockIndex + 1)?.let { n ->
                Text("Next: " + (n.variationId?.let { SessionBus.names[it] ?: it } ?: "Rest"), style = MaterialTheme.typography.bodyMedium)
            }
            if (st.phase == Phase.PAUSED) Text("Paused", color = MaterialTheme.colorScheme.error)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            if (st.phase == Phase.PAUSED) Button(onClick = { cmd(WorkoutSessionService.ACTION_RESUME) }) { Text("Resume") }
            else Button(onClick = { cmd(WorkoutSessionService.ACTION_PAUSE) }) { Text("Pause") }
            OutlinedButton(onClick = { cmd(WorkoutSessionService.ACTION_SKIP) }) { Text("Skip") }
        }
        OutlinedButton(onClick = { cmd(WorkoutSessionService.ACTION_FINISH) }) { Text("Finish now") }
    }
}

fun startWorkout(ctx: Context, planJson: String, sessionId: String, speak: Boolean) {
    val i = Intent(ctx, WorkoutSessionService::class.java).setAction(WorkoutSessionService.ACTION_START)
        .putExtra(WorkoutSessionService.EXTRA_PLAN, planJson).putExtra(WorkoutSessionService.EXTRA_SESSION_ID, sessionId)
        .putExtra(WorkoutSessionService.EXTRA_SPEAK, speak)
    androidx.core.content.ContextCompat.startForegroundService(ctx, i)
}


/** Optional feedback per exercise. Untouched rows stay "assumed met" (no row written). Each tap adds a new revision. */
@Composable
fun FeedbackForm(sessionId: String, vids: List<String>, waitSaved: Boolean, targets: Map<String, Int> = emptyMap()) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    if (vids.isEmpty()) return
    var rating by remember { mutableStateOf(mapOf<String, String>()) }
    var pain by remember { mutableStateOf(setOf<String>()) }
    var reps by remember { mutableStateOf(mapOf<String, Int>()) }
    fun save(v: String, r: String, d: Boolean, n: Int? = reps[v]) {
        scope.launch {
            val deadline = System.currentTimeMillis() + 5000
            while (waitSaved && !SessionBus.saved && System.currentTimeMillis() < deadline) delay(100)
            io.github.gonbei774.calisthenicsmemory.data.AppDatabase.getDatabase(ctx).historyDao()
                .reviseFeedback(sessionId, v, r, d, false, System.currentTimeMillis(), n)
        }
    }
    Text("How did it go? (optional — skipping counts as met)", style = MaterialTheme.typography.titleSmall)
    vids.forEach { v ->
        Column {
            Text(SessionBus.names[v] ?: v)
            targets[v]?.let { target ->
                val n = reps[v] ?: target
                fun set(x: Int) { val c = x.coerceIn(0, 999); reps = reps + (v to c); val r = if (c < target) "BELOW" else (rating[v]?.takeIf { it != "BELOW" } ?: "MET"); rating = rating + (v to r); save(v, r, v in pain, c) }
                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Reps done", style = MaterialTheme.typography.bodyMedium)
                    OutlinedButton(onClick = { set(n - 1) }) { Text("−") }
                    Text("$n", style = MaterialTheme.typography.titleLarge, modifier = Modifier.semantics { contentDescription = "$n reps done, target $target" })
                    OutlinedButton(onClick = { set(n + 1) }) { Text("+") }
                    Text("(target $target)", style = MaterialTheme.typography.bodySmall)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("BELOW" to "Too hard", "MET" to "As planned", "ABOVE" to "Easy").forEach { (k, label) ->
                    FilterChip(selected = rating[v] == k, onClick = { rating = rating + (v to k); save(v, k, v in pain) }, label = { Text(label) })
                }
                FilterChip(selected = v in pain, onClick = {
                    val on = v !in pain; pain = if (on) pain + v else pain - v; save(v, rating[v] ?: "MET", on)
                }, label = { Text("Discomfort") })
            }
        }
    }
}

fun recoverWorkout(ctx: Context, resume: Boolean) {
    val i = Intent(ctx, WorkoutSessionService::class.java).setAction(WorkoutSessionService.ACTION_RECOVER)
        .putExtra(WorkoutSessionService.EXTRA_RESUME, resume)
    androidx.core.content.ContextCompat.startForegroundService(ctx, i)
}
