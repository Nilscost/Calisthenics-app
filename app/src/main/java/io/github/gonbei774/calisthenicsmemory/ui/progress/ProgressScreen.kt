// U10 (F16, F15): the Progress tab. A family picker on top, one tree per family below (replaces the old library list),
// tap a node for the clip, how-to, steps, equipment and cautions. The selected goal's chain is outlined in the accent colour.
package io.github.gonbei774.calisthenicsmemory.ui.progress

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.calisthenics.domain.equipment.isAvailable
import app.calisthenics.domain.equipment.missingFor
import app.calisthenics.domain.goals.Goals
import app.calisthenics.domain.model.*
import app.calisthenics.domain.tree.*
import io.github.gonbei774.calisthenicsmemory.R
import io.github.gonbei774.calisthenicsmemory.ui.components.MuscleChips
import io.github.gonbei774.calisthenicsmemory.ui.components.StarRow
import app.calisthenics.domain.load.formatKg
import app.calisthenics.domain.load.isLoaded
import app.calisthenics.domain.load.loadGrams
import io.github.gonbei774.calisthenicsmemory.ui.screens.*
import io.github.gonbei774.calisthenicsmemory.ui.theme.FamilyIcons
import io.github.gonbei774.calisthenicsmemory.ui.theme.Spacing
import io.github.gonbei774.calisthenicsmemory.ui.train.rememberTrainData

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProgressScreen(modifier: Modifier = Modifier) {
    val ctx = LocalContext.current
    val data by rememberTrainData()
    val profile = remember { ProfileStore.selected(ctx) }
    val goalId = remember { GoalStore.load(ctx) }
    val goal = Goals.byId(goalId)
    val c = data.catalog
    val startTab = remember(c) { goal?.entryVariationId?.let { TreeTabs.tabForVariation(c, it) }?.id ?: "push" }
    var tabId by rememberSaveable { mutableStateOf(startTab) }
    val tab = TreeTabs.byId(tabId) ?: TreeTabs.all.first()
    var open by remember { mutableStateOf<String?>(null) }

    Scaffold(modifier = modifier, topBar = { TopAppBar(title = { Text(stringResource(R.string.tab_progress)) }) }) { pad ->
        Column(Modifier.padding(pad).fillMaxSize()) {
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = Spacing.l, vertical = Spacing.s), horizontalArrangement = Arrangement.spacedBy(Spacing.s)) {
                TreeTabs.all.forEach { t ->
                    FilterChip(
                        selected = t.id == tab.id, onClick = { tabId = t.id },
                        label = { Text(t.title) },
                        leadingIcon = { Icon(FamilyIcons.forFamily(t.familyIds.firstOrNull() ?: ""), null, Modifier.size(18.dp)) },
                        modifier = Modifier.testTag("tab_chip_${t.id}"),
                    )
                }
            }
            if (goal != null && goal.entryVariationId != null) Text(
                stringResource(R.string.progress_goal, goal.name), Modifier.padding(horizontal = Spacing.l), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary,
            )
            if (tab.id == TreeTabs.STRETCHES) StretchList(c, Modifier.weight(1f)) { open = it }
            else {
                val layout = remember(c, tab.id) { layoutTree(c, tab.familyIds) }
                val now = remember(c, data.progress, goalId) { trainingNow(c, data.progress, goalId) }
                val goalChain = remember(c, goalId) { goal?.let { Goals.chain(c, it).toSet() } ?: emptySet() }
                val nodes = remember(layout, data.progress, profile, now, goalChain) {
                    layout.nodes.associate { pos ->
                        val v = c.variation(pos.variationId)!!
                        val state = nodeState(c, v.id, data.progress, profile, now)
                        val missing = v.equipmentAlternatives.minByOrNull { s -> s.needs.count { n -> !app.calisthenics.domain.equipment.needSatisfied(n, profile) } }
                            ?.needs?.firstOrNull { n -> !app.calisthenics.domain.equipment.needSatisfied(n, profile) }?.equipmentId
                        v.id to NodeUi(v.id, v.name, v.familyId, state, stars(data.progress, v.id), v.id in goalChain, missing)
                    }
                }
                val mastered = remember(nodes) { nodes.filterValues { it.state == TreeNodeState.MASTERED }.keys }
                Box(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).horizontalScroll(rememberScrollState()).testTag("tree_area")) {
                    SkillTreeCanvas(layout, nodes, mastered, onNode = { open = it })
                }
            }
        }
    }
    open?.let { id -> c.variation(id)?.let { v -> NodeSheet(c, v, data.progress, profile) { open = null } } }
}

@Composable
private fun StretchList(c: Catalog, modifier: Modifier, onOpen: (String) -> Unit) {
    Column(modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(Spacing.l), verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
        c.variations.filter { it.kind == Kind.STRETCH || it.kind == Kind.MOBILITY }.forEach { v ->
            Card(Modifier.fillMaxWidth().clickable { onOpen(v.id) }.testTag("stretch_${v.id}"), shape = MaterialTheme.shapes.large) {
                Row(Modifier.padding(Spacing.m), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.m)) {
                    Icon(FamilyIcons.Stretch, null, Modifier.size(28.dp))
                    Text(v.name, Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NodeSheet(c: Catalog, v: ExerciseVariation, progress: app.calisthenics.domain.progression.ProgressSnapshot, profile: EquipmentProfile, onDismiss: () -> Unit) {
    val ctx = LocalContext.current
    val policy = c.policyFor(v)
    val tier = progress.tierFor(v.id)
    val strength = v.kind == Kind.REPS || v.kind == Kind.HOLD
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.padding(horizontal = Spacing.l).padding(bottom = Spacing.xl).verticalScroll(rememberScrollState()).testTag("node_sheet"), verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
            Text(v.name, style = MaterialTheme.typography.titleLarge, modifier = Modifier.testTag("sheet_title"))
            if (strength) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.s)) {
                    StarRow(stars(progress, v.id), size = 20.dp)
                    Text(stateLabel(nodeState(c, v.id, progress, profile, emptySet())), style = MaterialTheme.typography.labelLarge)
                }
                if (v.primaryMuscles.isNotEmpty()) MuscleChips(v.primaryMuscles, v.secondaryMuscles)
                // Kettlebell: levels count per weight. Show the weight in use and stars kept from lighter bells.
                if (v.isLoaded()) {
                    val vp = progress.variations[v.id]
                    val kg = (vp?.loadGrams ?: profile.loadGrams())?.let { formatKg(it) }
                    if (kg != null) Text(stringResource(R.string.sheet_load_now, kg), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.testTag("sheet_load"))
                    vp?.starsByLoad?.toSortedMap()?.forEach { (g, n) ->
                        Text(stringResource(R.string.sheet_load_earlier, formatKg(g), n), style = MaterialTheme.typography.bodySmall, modifier = Modifier.testTag("sheet_load_${g}"))
                    }
                }
            }
            DemoClips.file(ctx, v.id)?.let { DemoPlayer(it) }
            SheetSection(R.string.sheet_how, v.instructions.mapIndexed { i, t -> stringResource(R.string.numbered_item, i + 1, t) })
            SheetSection(R.string.sheet_cues, v.formCues.map { stringResource(R.string.bullet_item, it) })
            if (policy != null) {
                Text(stringResource(R.string.sheet_steps), style = MaterialTheme.typography.titleMedium)
                policy.tiers.forEach { t ->
                    val here = tier == t.index
                    Text(
                        stringResource(if (here) R.string.sheet_step_here else R.string.sheet_step, t.index, if (t.target.type == TargetType.REPS) stringResource(R.string.target_reps, t.target.value) else stringResource(R.string.target_seconds, t.target.value)),
                        style = if (here) MaterialTheme.typography.titleSmall else MaterialTheme.typography.bodyMedium, modifier = Modifier.testTag("sheet_step_${t.index}"),
                    )
                }
            }
            val unmet = Goals.unmet(c, v.id, progress)
            if (unmet.isNotEmpty() && v.id !in progress.variations) Text(
                stringResource(R.string.sheet_needs_first, unmet.map { stringResource(R.string.sheet_step_name, c.variation(it.variationId)?.name ?: it.variationId, it.tier) }.joinToString()),
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error,
            )
            Text(stringResource(R.string.sheet_equipment), style = MaterialTheme.typography.titleMedium)
            val noEquipment = stringResource(R.string.sheet_no_equipment)
            val need = v.equipmentAlternatives.map { set ->
                val items = set.needs.map { stringResource(equipmentLabelShortPublic(it.equipmentId)) } + set.capabilities.map { stringResource(equipmentLabelShortPublic(it)) }
                if (items.isEmpty()) noEquipment else items.joinToString(" + ")
            }.joinToString(stringResource(R.string.sheet_or))
            Text(need, style = MaterialTheme.typography.bodyMedium)
            if (!isAvailable(v, profile)) Text(stringResource(R.string.sheet_missing, missingFor(v, profile)), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
            SheetSection(R.string.sheet_cautions, v.cautions.map { stringResource(R.string.bullet_item, it) }, error = true)
            if (v.sourceIds.isNotEmpty()) Text(stringResource(R.string.sheet_sources, v.sourceIds.joinToString()), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(stringResource(R.string.sheet_draft), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun SheetSection(title: Int, lines: List<String>, error: Boolean = false) {
    if (lines.isEmpty()) return
    Text(stringResource(title), style = MaterialTheme.typography.titleMedium, color = if (error) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface)
    lines.forEach { Text(it, style = MaterialTheme.typography.bodyMedium) }
}
