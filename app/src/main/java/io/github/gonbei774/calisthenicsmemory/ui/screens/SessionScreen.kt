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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.calisthenics.domain.model.BlockType
import app.calisthenics.domain.model.TargetType
import app.calisthenics.domain.session.Phase
import io.github.gonbei774.calisthenicsmemory.session.SessionBus
import io.github.gonbei774.calisthenicsmemory.session.WorkoutSessionService
import kotlinx.coroutines.delay

@Composable
fun SessionScreen(onExit: () -> Unit) {
    val ctx = LocalContext.current
    val s by SessionBus.state.collectAsState()
    var nowTick by remember { mutableStateOf(android.os.SystemClock.elapsedRealtime()) }
    LaunchedEffect(Unit) { while (true) { nowTick = android.os.SystemClock.elapsedRealtime(); delay(250) } }
    fun cmd(a: String) { ctx.startService(Intent(ctx, WorkoutSessionService::class.java).setAction(a)) }
    Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.spacedBy(14.dp), horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
        val st = s
        if (st == null) { Text("Starting…"); return@Column }
        val b = st.currentBlock
        if (st.isTerminal) {
            Text(if (st.phase == Phase.COMPLETED) "Workout complete" else "Workout finished early", style = MaterialTheme.typography.headlineMedium)
            Text("Saved to your history (blocks done: ${st.executions.values.count { it.name == "COMPLETED" }}).")
            Button(onClick = { SessionBus.clear(); onExit() }) { Text("Done") }
            return@Column
        }
        if (b != null) {
            val name = b.variationId?.let { SessionBus.names[it] ?: it } ?: when (b.type) { BlockType.PASSIVE_RECOVERY, BlockType.TRANSITION -> "Rest"; else -> b.type.name.lowercase() }
            Text("Block ${st.blockIndex + 1} of ${st.plan.blocks.size}", style = MaterialTheme.typography.labelLarge)
            Text(name, style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
            b.target?.let { Text(if (it.type == TargetType.REPS) "${it.value} reps" else "${it.value}s hold", style = MaterialTheme.typography.titleLarge) }
            val rem = (st.remainingAt(nowTick) + 999) / 1000
            Text("%d:%02d".format(rem / 60, rem % 60), fontSize = 72.sp)
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
