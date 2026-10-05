// T18 (first slice): exercise library grouped by progression family, plus skill links split into
// prerequisites vs recommended preparation. Reads the same catalog the planner uses.
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
import app.calisthenics.domain.model.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(onBack: () -> Unit) {
    val ctx = LocalContext.current
    val catalog = remember { ctx.assets.open("catalog.json").bufferedReader().use { parseCatalog(it.readText()) } }
    val strength = catalog.variations.filter { it.kind == Kind.REPS || it.kind == Kind.HOLD }
    val stretches = catalog.variations.filter { it.kind == Kind.STRETCH || it.kind == Kind.MOBILITY }
    val names = catalog.variations.associate { it.id to it.name }
    var open by remember { mutableStateOf<String?>(null) }

    Scaffold(topBar = { TopAppBar(title = { Text("Exercise library") }, navigationIcon = { TextButton(onClick = onBack) { Text("Back") } }) }) { pad ->
        Column(Modifier.padding(pad).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("All content is an unreviewed draft. Numbers are starting heuristics, not medical advice.", color = MaterialTheme.colorScheme.error)
            Text("Strength ladders (easier to harder)", style = MaterialTheme.typography.titleMedium)
            strength.groupBy { it.familyId }.forEach { (family, list) ->
                Text(family, style = MaterialTheme.typography.titleSmall)
                list.sortedBy { it.difficultyRank }.forEach { v -> Entry(v, catalog, open == v.id) { open = if (open == v.id) null else v.id } }
            }
            HorizontalDivider()
            Text("Stretches & mobility", style = MaterialTheme.typography.titleMedium)
            stretches.forEach { v -> Entry(v, catalog, open == v.id) { open = if (open == v.id) null else v.id } }
            if (catalog.skillEdges.isNotEmpty()) {
                HorizontalDivider()
                Text("How exercises connect", style = MaterialTheme.typography.titleMedium)
                val nodes = catalog.skillNodes.associate { it.id to it.name }
                catalog.skillEdges.groupBy { it.relation }.forEach { (rel, edges) ->
                    Text(if (rel == EdgeRelation.PREREQUISITE) "Required first" else "Helpful preparation (not required)", style = MaterialTheme.typography.titleSmall)
                    edges.forEach { Text("• ${nodes[it.fromId] ?: names[it.fromId] ?: it.fromId} → ${nodes[it.toId] ?: names[it.toId] ?: it.toId}" + if (it.criterion.isNotBlank()) " (${it.criterion})" else "") }
                }
            }
        }
    }
}

@Composable
private fun Entry(v: ExerciseVariation, catalog: Catalog, expanded: Boolean, toggle: () -> Unit) {
    Card(onClick = toggle, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(v.name, style = MaterialTheme.typography.titleSmall)
            if (expanded) {
                if (v.instructions.isNotEmpty()) { Text("How:"); v.instructions.forEach { Text("• $it") } }
                if (v.formCues.isNotEmpty()) { Text("Form cues:"); v.formCues.forEach { Text("• $it") } }
                if (v.cautions.isNotEmpty()) { Text("Cautions:", color = MaterialTheme.colorScheme.error); v.cautions.forEach { Text("• $it") } }
                catalog.policyFor(v)?.let { pol ->
                    Text("Targets by step:")
                    pol.tiers.forEach { Text("  ${it.index}: ${it.target}") }
                }
                Text("Needs: " + v.equipmentAlternatives.joinToString(" or ") { set -> if (set.needs.isEmpty()) "no equipment" else set.needs.joinToString(" + ") { it.equipmentId } })
                if (v.sourceIds.isNotEmpty()) Text("Ladder order from: ${v.sourceIds.joinToString()}")
                val clip = androidx.compose.ui.platform.LocalContext.current.let { DemoClips.file(it, v.id) }
                if (clip != null) DemoPlayer(clip) else Text("No demo clip for this one")
                Text("Status: ${v.reviewState.name.lowercase()}")
            }
        }
    }
}
