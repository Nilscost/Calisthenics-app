// Late edit of what was logged: optional rating, reps and discomfort per exercise. Untouched rows stay "assumed met".
// Each tap adds a new feedback revision (the earlier one is kept). Used by History and by the end screen's "Edit what I logged".
package io.github.gonbei774.calisthenicsmemory.ui.session

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import app.calisthenics.domain.model.TargetType
import io.github.gonbei774.calisthenicsmemory.session.SessionBus
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun FeedbackForm(sessionId: String, vids: List<String>, waitSaved: Boolean, targets: Map<String, app.calisthenics.domain.model.Target> = emptyMap()) {
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
            targets[v]?.let { tg ->
                val target = tg.value; val hold = tg.type == TargetType.HOLD_SECONDS
                val step = if (hold && target >= 30) 5 else 1
                val unit = if (hold) "s" else ""
                val n = reps[v] ?: target
                fun set(x: Int) { val c = x.coerceIn(0, 999); reps = reps + (v to c); val r = if (c < target) "BELOW" else (rating[v]?.takeIf { it != "BELOW" } ?: "MET"); rating = rating + (v to r); save(v, r, v in pain, c) }
                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(if (hold) "Hold time" else "Reps done", style = MaterialTheme.typography.bodyMedium)
                    OutlinedButton(onClick = { set(n - step) }) { Text("−") }
                    Text("$n$unit", style = MaterialTheme.typography.titleLarge, modifier = Modifier.semantics { contentDescription = "$n ${if (hold) "seconds held" else "reps done"}, target $target" })
                    OutlinedButton(onClick = { set(n + step) }) { Text("+") }
                    Text("(target $target$unit)", style = MaterialTheme.typography.bodySmall)
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

