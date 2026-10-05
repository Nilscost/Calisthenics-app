// U06 (F8, F6): the draft workout. One card per exercise of the circuit, swap sheet, collapsible timeline,
// one compact info row, and a sticky Start button.
package io.github.gonbei774.calisthenicsmemory.ui.train

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.calisthenics.domain.model.*
import app.calisthenics.domain.model.Target
import app.calisthenics.domain.planner.*
import app.calisthenics.domain.routine.saveSwapsToRoutine
import app.calisthenics.domain.session.CueText
import io.github.gonbei774.calisthenicsmemory.R
import io.github.gonbei774.calisthenicsmemory.ui.screens.ProfileStore
import io.github.gonbei774.calisthenicsmemory.ui.screens.RoutineStore
import io.github.gonbei774.calisthenicsmemory.ui.screens.startWorkout
import io.github.gonbei774.calisthenicsmemory.ui.theme.Radius
import io.github.gonbei774.calisthenicsmemory.ui.theme.Spacing
import kotlinx.serialization.json.Json
import java.util.UUID

fun formatClock(sec: Int) = "%d:%02d".format(sec / 60, sec % 60)

/** The circuit: the work of round 1, one entry per exercise (both sides of a unilateral exercise are one entry). */
data class CircuitEntry(val slotId: String, val variationId: String, val target: Target?, val perSide: Boolean)

fun circuitOf(plan: WorkoutPlan): List<CircuitEntry> =
    plan.blocks.filter { it.type == BlockType.WORK && (it.roundIndex ?: 1) == 1 && it.slotId != null && it.variationId != null }
        .groupBy { it.slotId!! }.values.map { bs -> CircuitEntry(bs.first().slotId!!, bs.first().variationId!!, bs.first().target, bs.size > 1) }

private fun patternLabel(p: Pattern) = when (p) {
    Pattern.PUSH_HORIZONTAL -> R.string.pattern_push_horizontal
    Pattern.PUSH_VERTICAL -> R.string.pattern_push_vertical
    Pattern.PULL_HORIZONTAL -> R.string.pattern_pull_horizontal
    Pattern.PULL_VERTICAL -> R.string.pattern_pull_vertical
    Pattern.SQUAT -> R.string.pattern_squat
    Pattern.LUNGE -> R.string.pattern_lunge
    Pattern.HINGE -> R.string.pattern_hinge
    Pattern.CORE_ANTI_EXTENSION, Pattern.CORE_ANTI_LATERAL -> R.string.pattern_core
    else -> R.string.pattern_other
}

@Composable
private fun targetText(t: Target?, perSide: Boolean): String {
    t ?: return ""
    val base = if (t.type == TargetType.REPS) stringResource(R.string.target_reps, t.value) else stringResource(R.string.target_seconds, t.value)
    return if (perSide) stringResource(R.string.target_per_side, base) else base
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PreviewScreen(modifier: Modifier = Modifier, onBack: () -> Unit, onStarted: () -> Unit) {
    val ctx = LocalContext.current
    val data by rememberTrainData()
    val settings = remember { TrainSettingsStore.load(ctx) }
    val profile = remember { ProfileStore.selected(ctx) }
    var routine by remember(data) { mutableStateOf(data.routine) }
    var swaps by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var swapSlot by remember { mutableStateOf<String?>(null) }
    var timelineOpen by remember { mutableStateOf(false) }
    val result = remember(data, routine, swaps) {
        buildTrainPlan(data.catalog, routine, data.progress, profile, settings, swaps, nowEpochMs = System.currentTimeMillis())
    }
    val names = remember(data) { data.catalog.variations.associate { it.id to it.name } }

    Scaffold(
        modifier = modifier,
        topBar = { TopAppBar(title = { Text(stringResource(R.string.preview_title)) }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back)) } }) },
        bottomBar = {
            Surface(tonalElevation = 3.dp) {
                Button(
                    onClick = {
                        val plan = (result as PlanResult.Ready).plan.copy(id = UUID.randomUUID().toString()) // one history snapshot per workout
                        val json = Json { encodeDefaults = true }.encodeToString(WorkoutPlan.serializer(), plan)
                        startWorkout(ctx, json, UUID.randomUUID().toString(), true)
                        onStarted()
                    },
                    enabled = result is PlanResult.Ready,
                    modifier = Modifier.fillMaxWidth().padding(Spacing.l).height(56.dp).testTag("start_button"),
                    shape = RoundedCornerShape(Radius.button),
                ) { Text(stringResource(R.string.preview_start)) }
            }
        },
    ) { pad ->
        Column(Modifier.padding(pad).verticalScroll(rememberScrollState()).padding(horizontal = Spacing.l, vertical = Spacing.s), verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
            when (val r = result) {
                is PlanResult.Infeasible -> {
                    Text(stringResource(R.string.train_infeasible), style = MaterialTheme.typography.titleMedium)
                    r.reasons.forEach { Text(it.message) }
                    r.alternatives.forEach { Text(stringResource(R.string.bullet_item, it.description), style = MaterialTheme.typography.bodySmall) }
                }
                is PlanResult.Ready -> {
                    val p = r.plan
                    Text(
                        stringResource(R.string.preview_header, p.rounds, (p.plannedDurationSeconds + 30) / 60, stringResource(if (p.timed) R.string.style_timed else R.string.style_reps)),
                        style = MaterialTheme.typography.titleLarge, modifier = Modifier.testTag("preview_header"),
                    )
                    InfoRow(p)
                    circuitOf(p).forEach { e -> ExerciseCard(e, data.catalog.variation(e.variationId), names[e.variationId] ?: e.variationId) { swapSlot = e.slotId } }
                    TimelineCard(p, names, timelineOpen) { timelineOpen = !timelineOpen }
                }
            }
        }
    }

    val slot = swapSlot
    if (slot != null && result is PlanResult.Ready) {
        val current = circuitOf(result.plan).firstOrNull { it.slotId == slot }?.variationId ?: ""
        SwapSheet(
            options = swapOptions(data.catalog, routine, slot, profile, current), currentId = current,
            onDismiss = { swapSlot = null },
            onPick = { id, keep ->
                swaps = swaps + (slot to id)
                if (keep) { routine = saveSwapsToRoutine(routine, mapOf(slot to id)); RoutineStore.save(ctx, routine); swaps = swaps - slot }
                swapSlot = null
            },
        )
    }
}

@Composable
private fun InfoRow(p: WorkoutPlan) {
    var open by remember { mutableStateOf(false) }
    val notes = p.warnings + p.changesExplained.filter { it.isNotBlank() }
    Card(
        Modifier.fillMaxWidth().clickable(enabled = notes.isNotEmpty()) { open = !open }.testTag("info_row"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
    ) {
        Column(Modifier.padding(Spacing.m), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.s)) {
                Icon(Icons.Filled.Info, contentDescription = null)
                Text(
                    stringResource(if (p.usesDraftContent) R.string.preview_draft_note else R.string.preview_note), Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyMedium,
                )
                if (notes.isNotEmpty()) Text(stringResource(R.string.preview_more_notes, notes.size), style = MaterialTheme.typography.labelLarge)
            }
            if (open) notes.forEach { Text(stringResource(R.string.bullet_item, it), style = MaterialTheme.typography.bodySmall) }
        }
    }
}

@Composable
private fun ExerciseCard(e: CircuitEntry, v: ExerciseVariation?, name: String, onSwap: () -> Unit) {
    Card(Modifier.fillMaxWidth().testTag("exercise_${e.slotId}"), shape = MaterialTheme.shapes.large) {
        Row(Modifier.padding(Spacing.m), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.m)) {
            // Family pictograms arrive with the skill tree (U10); until then the initial keeps the card scannable.
            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.size(44.dp)) {
                Box(contentAlignment = Alignment.Center) { Text(name.take(1), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onPrimaryContainer) }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                Text(name, style = MaterialTheme.typography.titleMedium, modifier = Modifier.testTag("exercise_name_${e.slotId}"))
                Text(targetText(e.target, e.perSide), style = MaterialTheme.typography.bodyLarge)
                v?.patterns?.firstOrNull()?.let {
                    Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
                        Text(stringResource(patternLabel(it)), Modifier.padding(horizontal = Spacing.s, vertical = 2.dp), style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
            IconButton(onClick = onSwap, modifier = Modifier.testTag("swap_${e.slotId}")) { Icon(Icons.Filled.Refresh, stringResource(R.string.preview_swap, name)) }
        }
    }
}

@Composable
private fun TimelineCard(p: WorkoutPlan, names: Map<String, String>, open: Boolean, toggle: () -> Unit) {
    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column {
            Row(Modifier.fillMaxWidth().clickable(onClick = toggle).heightIn(min = Spacing.touch).padding(horizontal = Spacing.m).testTag("timeline_toggle"), verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.preview_timeline, p.blocks.size), Modifier.weight(1f), style = MaterialTheme.typography.titleSmall)
                Icon(if (open) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown, contentDescription = null)
            }
            if (open) Column(Modifier.padding(horizontal = Spacing.m, vertical = Spacing.s).testTag("timeline_list"), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                p.blocks.forEach { b ->
                    val target = b.target?.let { if (it.type == TargetType.REPS) stringResource(R.string.target_reps, it.value) else stringResource(R.string.target_seconds, it.value) }.orEmpty()
                    Text("${formatClock(b.durationSeconds)}  ${CueText.label(p, b.id, names)} $target".trim(), style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwapSheet(options: List<ExerciseVariation>, currentId: String, onDismiss: () -> Unit, onPick: (String, Boolean) -> Unit) {
    var keep by remember { mutableStateOf(false) }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.padding(horizontal = Spacing.l).padding(bottom = Spacing.xl), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            Text(stringResource(R.string.preview_swap_title), style = MaterialTheme.typography.titleLarge)
            Text(stringResource(R.string.preview_swap_hint), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            options.forEach { o ->
                ListItem(
                    headlineContent = { Text(o.name) },
                    trailingContent = { if (o.id == currentId) Text(stringResource(R.string.preview_swap_current), style = MaterialTheme.typography.labelLarge) },
                    modifier = Modifier.clickable { onPick(o.id, keep) }.testTag("swap_option_${o.id}"),
                )
            }
            Row(Modifier.fillMaxWidth().heightIn(min = Spacing.touch).clickable { keep = !keep }.testTag("swap_keep"), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.s)) {
                Checkbox(checked = keep, onCheckedChange = null)
                Text(stringResource(R.string.preview_swap_keep))
            }
        }
    }
}
