// Skill tree view: one card per step, joined by arrows; colour + icon show mastered / available / locked.
package io.github.gonbei774.calisthenicsmemory.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.calisthenics.domain.equipment.isAvailable
import app.calisthenics.domain.equipment.missingFor
import app.calisthenics.domain.goals.*
import app.calisthenics.domain.model.Catalog
import app.calisthenics.domain.model.EquipmentProfile
import app.calisthenics.domain.progression.ProgressSnapshot

@Composable
fun SkillTree(catalog: Catalog, goal: Goal, progress: ProgressSnapshot, profile: EquipmentProfile) {
    val states = Goals.treeStates(catalog, goal, progress)
    val next = Goals.nextToTrain(catalog, goal, progress)
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        states.entries.forEachIndexed { i, (v, st) ->
            val ex = catalog.variation(v) ?: return@forEachIndexed
            val colors = when (st) {
                NodeState.MASTERED -> CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                NodeState.AVAILABLE -> CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                NodeState.LOCKED -> CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            }
            val tier = progress.tierFor(v)
            Card(Modifier.fillMaxWidth(), colors = colors, border = if (v == next) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null) {
                Column(Modifier.padding(10.dp)) {
                    Text("${when (st) { NodeState.MASTERED -> "★"; NodeState.AVAILABLE -> "▶"; NodeState.LOCKED -> "🔒" }} ${ex.name}", style = MaterialTheme.typography.titleSmall)
                    val line = when (st) {
                        NodeState.MASTERED -> "Mastered"
                        NodeState.AVAILABLE -> if (tier != null) "Training: step $tier of 5" else "Ready to start"
                        NodeState.LOCKED -> Goals.unmet(catalog, v, progress).joinToString("; ", "Needs: ") { "${catalog.variation(it.variationId)?.name ?: it.variationId} step ${it.tier}" }
                    }
                    Text(line, style = MaterialTheme.typography.bodySmall)
                    if (v == next) Text("Training this now", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    if (!isAvailable(ex, profile)) Text("Needs equipment: ${missingFor(ex, profile)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                }
            }
            if (i < states.size - 1) Text("↓", style = MaterialTheme.typography.titleMedium)
        }
    }
}
