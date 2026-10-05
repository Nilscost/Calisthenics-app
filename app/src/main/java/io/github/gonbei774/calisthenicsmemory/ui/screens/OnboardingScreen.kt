// T16: choose where to start per exercise family. Conservative: nothing preselected = easiest step. No max-effort test.
package io.github.gonbei774.calisthenicsmemory.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import app.calisthenics.domain.content.parseCatalog
import app.calisthenics.domain.model.Kind
import app.calisthenics.domain.intake.*
import app.calisthenics.domain.goals.Goals
import androidx.compose.foundation.horizontalScroll

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(onBack: () -> Unit) {
    val ctx = LocalContext.current
    val catalog = remember { ctx.assets.open("catalog.json").bufferedReader().use { parseCatalog(it.readText()) } }
    var levels by remember { mutableStateOf(LevelStore.load(ctx)) }
    var goalId by remember { mutableStateOf(GoalStore.load(ctx)) }
    val strength = catalog.variations.filter { it.kind == Kind.REPS || it.kind == Kind.HOLD }
    Scaffold(topBar = { TopAppBar(title = { Text("Your starting level") }, navigationIcon = { TextButton(onClick = onBack) { Text("Back") } }) }) { pad ->
        Column(Modifier.padding(pad).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Your goal (stays until you change it)", style = MaterialTheme.typography.titleSmall)
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Goals.all.forEach { g -> FilterChip(selected = goalId == g.id, onClick = { goalId = g.id; GoalStore.save(ctx, g.id) }, label = { Text(g.name) }) }
            }
            Goals.byId(goalId)?.let { Text(it.description, style = MaterialTheme.typography.bodySmall) }
            Text("Already training? Type what you do today in ONE comfortable set (reps, or seconds for holds) and the app picks the matching step. You can still move the slider. Rounds and weights are not used: the app does not credit added weight, so a weighted version starts at the step of the plain one.")
            Text("For each exercise pick the step you can do comfortably for a full set. Not sure? Leave it: you start at step 1 and the app only moves up after repeated comfortable sessions.")
            strength.forEach { v ->
                val pol = catalog.policyFor(v) ?: return@forEach
                val cur = levels[v.id] ?: 1
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp)) {
                        Text(v.name, style = MaterialTheme.typography.titleSmall)
                        Text("Start at step $cur: ${pol.tier(cur).target}")
                        var typed by remember { mutableStateOf("") }
                        OutlinedTextField(value = typed, singleLine = true,
                            label = { Text(if (v.kind == Kind.HOLD) "I hold for (seconds)" else "I do (reps)") },
                            onValueChange = { t ->
                                typed = t.filter { it.isDigit() }.take(3)
                                typed.toIntOrNull()?.takeIf { it > 0 }?.let { n -> levels = levels + (v.id to suggestStartingStep(pol.tiers, n)) }
                            })
                        typed.toIntOrNull()?.let { n -> if (exceedsLadder(pol.tiers, n)) Text("You are above the top step here: it starts at the top and, once earned, moves you on to the next exercise in the progression.") }
                        Slider(value = cur.toFloat(), onValueChange = { levels = levels + (v.id to it.toInt().coerceIn(1, pol.tiers.size)) },
                            valueRange = 1f..pol.tiers.size.toFloat(), steps = (pol.tiers.size - 2).coerceAtLeast(0))
                    }
                }
            }
            Button(onClick = { LevelStore.save(ctx, levels); onBack() }) { Text("Save") }
        }
    }
}
