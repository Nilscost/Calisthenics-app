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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(onBack: () -> Unit) {
    val ctx = LocalContext.current
    val catalog = remember { ctx.assets.open("catalog.json").bufferedReader().use { parseCatalog(it.readText()) } }
    var levels by remember { mutableStateOf(LevelStore.load(ctx)) }
    val strength = catalog.variations.filter { it.kind == Kind.REPS || it.kind == Kind.HOLD }
    Scaffold(topBar = { TopAppBar(title = { Text("Your starting level") }, navigationIcon = { TextButton(onClick = onBack) { Text("Back") } }) }) { pad ->
        Column(Modifier.padding(pad).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("For each exercise pick the step you can do comfortably for a full set. Not sure? Leave it: you start at step 1 and the app only moves up after repeated comfortable sessions.")
            strength.forEach { v ->
                val pol = catalog.policyFor(v) ?: return@forEach
                val cur = levels[v.id] ?: 1
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp)) {
                        Text(v.name, style = MaterialTheme.typography.titleSmall)
                        Text("Start at step $cur: ${pol.tier(cur).target}")
                        Slider(value = cur.toFloat(), onValueChange = { levels = levels + (v.id to it.toInt().coerceIn(1, pol.tiers.size)) },
                            valueRange = 1f..pol.tiers.size.toFloat(), steps = (pol.tiers.size - 2).coerceAtLeast(0))
                    }
                }
            }
            Button(onClick = { LevelStore.save(ctx, levels); onBack() }) { Text("Save") }
        }
    }
}
