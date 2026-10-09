// U06 (F8, F6): the draft workout. One card per exercise of the circuit, swap sheet, collapsible timeline,
// one compact info row, and a sticky Start button.
package io.github.gonbei774.calisthenicsmemory.ui.train

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.onLongClick
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material.icons.filled.Add
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.foundation.selection.toggleable
import io.github.gonbei774.calisthenicsmemory.ui.components.AppOutlinedButton
import io.github.gonbei774.calisthenicsmemory.ui.components.AppTextButton
import io.github.gonbei774.calisthenicsmemory.ui.components.Caption
import io.github.gonbei774.calisthenicsmemory.ui.components.appSegmentedColors
import io.github.gonbei774.calisthenicsmemory.ui.theme.AppAccentTheme
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
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
import io.github.gonbei774.calisthenicsmemory.ui.components.MuscleChips
import io.github.gonbei774.calisthenicsmemory.ui.screens.ProfileStore
import io.github.gonbei774.calisthenicsmemory.ui.screens.RoutineStore
import io.github.gonbei774.calisthenicsmemory.ui.session.startWorkout
import io.github.gonbei774.calisthenicsmemory.ui.theme.Radius
import io.github.gonbei774.calisthenicsmemory.ui.theme.Spacing
import kotlinx.serialization.json.Json
import java.util.UUID

fun formatClock(sec: Int) = "%d:%02d".format(sec / 60, sec % 60)

/** The circuit: the work of round 1, one entry per exercise (both sides of a unilateral exercise are one entry). */
data class CircuitEntry(val slotId: String, val variationId: String, val target: Target?, val perSide: Boolean, val loadGrams: Int? = null, val tier: Int = 1, val free: Boolean = false)

fun circuitOf(plan: WorkoutPlan, round: Int = 1): List<CircuitEntry> =
    plan.blocks.filter { it.type == BlockType.WORK && (it.roundIndex ?: 1) == round && it.slotId != null && it.variationId != null }
        .groupBy { it.slotId!! }.values.map { bs -> CircuitEntry(bs.first().slotId!!, bs.first().variationId!!, bs.first().target, bs.size > 1, bs.first().loadGrams, bs.first().prescriptionTier ?: 1, bs.first().freeTarget) }

@Composable
private fun targetText(t: Target?, perSide: Boolean, loadGrams: Int? = null): String {
    t ?: return ""
    val base = io.github.gonbei774.calisthenicsmemory.ui.components.targetLabel(t)
    val sided = if (perSide) stringResource(R.string.target_per_side, base) else base
    return withLoad(sided, loadGrams)
}

/** "10 reps" -> "10 reps · 12 kg" for kettlebell work. */
@Composable
fun withLoad(text: String, loadGrams: Int?): String =
    if (loadGrams == null || text.isEmpty()) text else stringResource(R.string.target_with_load, text, app.calisthenics.domain.load.formatKg(loadGrams))

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
private fun TimelineCard(p: WorkoutPlan, names: Map<String, String>, open: Boolean, toggle: () -> Unit) {
    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column {
            Row(Modifier.fillMaxWidth().clickable(onClick = toggle).heightIn(min = Spacing.touch).padding(horizontal = Spacing.m).testTag("timeline_toggle"), verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.preview_timeline, p.blocks.size), Modifier.weight(1f), style = MaterialTheme.typography.titleSmall)
                Icon(if (open) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown, contentDescription = null)
            }
            if (open) Column(Modifier.padding(horizontal = Spacing.m, vertical = Spacing.s).testTag("timeline_list"), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                p.blocks.forEach { b ->
                    val target = b.target?.let { io.github.gonbei774.calisthenicsmemory.ui.components.targetLabel(it) }.orEmpty()
                    Text("${formatClock(b.durationSeconds)}  ${CueText.label(p, b.id, names)} ${withLoad(target, b.loadGrams)}".trim(), style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

private fun movementTag(v: ExerciseVariation?): Int = when (v?.patterns?.firstOrNull()) {
    Pattern.PUSH_HORIZONTAL, Pattern.PUSH_VERTICAL -> R.string.tag_push
    Pattern.PULL_HORIZONTAL, Pattern.PULL_VERTICAL -> R.string.tag_pull
    Pattern.SQUAT, Pattern.LUNGE -> R.string.tag_squat
    Pattern.HINGE -> R.string.tag_hinge
    Pattern.CALF_RAISE -> R.string.tag_calf
    Pattern.CORE_ANTI_EXTENSION, Pattern.CORE_ANTI_ROTATION, Pattern.CORE_ANTI_LATERAL -> R.string.tag_core
    else -> R.string.tag_other
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ExerciseRow(index: Int, e: CircuitEntry, v: ExerciseVariation?, name: String, levels: Int, onLevel: (Int) -> Unit, onSwap: () -> Unit, onRemove: () -> Unit, sfx: String = "", onTypeNumber: (() -> Unit)? = null) {
    val removeLabel = stringResource(R.string.preview_remove)
    Card(Modifier.fillMaxWidth().pointerInput(Unit) { detectTapGestures(onLongPress = { onRemove() }) }
        .semantics { onLongClick(label = removeLabel) { onRemove(); true } }.testTag("exercise_${e.slotId}$sfx"), shape = MaterialTheme.shapes.large) {
        Column(Modifier.padding(Spacing.m), verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.m)) {
                io.github.gonbei774.calisthenicsmemory.ui.components.ExerciseThumb(e.variationId, name, 56.dp)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(stringResource(R.string.preview_slot_tag, index + 1, stringResource(movementTag(v))), style = MaterialTheme.typography.labelLarge, color = AppAccentTheme.colors.text, modifier = Modifier.testTag("slot_tag_${e.slotId}$sfx"))
                    Text(name, style = MaterialTheme.typography.titleMedium, modifier = Modifier.testTag("exercise_name_${e.slotId}$sfx"))
                }
                if (v != null && v.primaryMuscles.isNotEmpty()) io.github.gonbei774.calisthenicsmemory.ui.components.BodyMap(v.primaryMuscles, v.secondaryMuscles, showNamesOnTap = false, tag = "muscles_${e.slotId}$sfx", viewWidth = 24.dp)
                IconButton(onClick = onSwap, modifier = Modifier.testTag("swap_${e.slotId}$sfx")) { Icon(Icons.Filled.Refresh, stringResource(R.string.preview_swap, name)) }
            }
            // D3: the level stepper; the number between - and + is the target of that level
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                FilledTonalIconButton(onClick = { onLevel(stepLevel(e.tier, -1, levels)) }, enabled = e.tier > 1, modifier = Modifier.size(Spacing.touch).testTag("level_minus_${e.slotId}$sfx")) { Icon(io.github.gonbei774.calisthenicsmemory.ui.theme.AppIcons.Remove, stringResource(R.string.stepper_decrease)) }
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(targetText(e.target, e.perSide, e.loadGrams), style = MaterialTheme.typography.headlineMedium, color = if (e.free) AppAccentTheme.colors.text else MaterialTheme.colorScheme.onSurface,
                        modifier = (if (onTypeNumber != null) Modifier.clickable(onClick = onTypeNumber).heightIn(min = Spacing.touch).wrapContentHeight(Alignment.CenterVertically) else Modifier).testTag("exercise_target_${e.slotId}$sfx"))
                    Text(if (e.free) stringResource(R.string.preview_free_number) else stringResource(R.string.preview_level, e.tier, levels), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline, modifier = Modifier.testTag("level_${e.slotId}$sfx"))
                }
                FilledTonalIconButton(onClick = { onLevel(stepLevel(e.tier, 1, levels)) }, enabled = e.tier < levels, modifier = Modifier.size(Spacing.touch).testTag("level_plus_${e.slotId}$sfx")) { Icon(Icons.Filled.Add, stringResource(R.string.stepper_increase)) }
            }
        }
    }
}

/** The slim row between two exercises: what the break is (the stretch or the rest) and a dashed + to add something here. */
@Composable
private fun BreakRow(slotId: String, p: WorkoutPlan, round: Int, names: Map<String, String>, onChange: () -> Unit, onAdd: () -> Unit, sfx: String = "") {
    val blocks = p.blocks.filter { it.slotId == slotId && it.roundIndex == round && (it.type == BlockType.STRETCH || it.type == BlockType.PASSIVE_RECOVERY) }
    val secs = blocks.sumOf { it.durationSeconds }
    val stretches = blocks.filter { it.type == BlockType.STRETCH }.mapNotNull { it.variationId }.distinct().mapNotNull { names[it] }
    Row(Modifier.fillMaxWidth().heightIn(min = Spacing.touch), verticalAlignment = Alignment.CenterVertically) {
        if (blocks.isNotEmpty()) Row(Modifier.weight(1f).clickable(onClick = onChange).heightIn(min = Spacing.touch).testTag("break_$slotId$sfx"), verticalAlignment = Alignment.CenterVertically) {
            Text(
                if (stretches.isNotEmpty()) stringResource(R.string.preview_break_stretch, stretches.joinToString(" + "), formatClock(secs)) else stringResource(R.string.preview_break_rest, formatClock(secs)),
                Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2,
            )
            Text(stringResource(R.string.preview_change), style = MaterialTheme.typography.labelLarge, color = AppAccentTheme.colors.text)
        } else Spacer(Modifier.weight(1f))
        AddButton(onAdd, Modifier.testTag("add_after_$slotId$sfx"))
    }
}

@Composable
private fun AddButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val addHere = stringResource(R.string.preview_add_here)
    val line = MaterialTheme.colorScheme.outline
    Box(modifier.size(Spacing.touch).clickable(onClick = onClick, role = androidx.compose.ui.semantics.Role.Button).semantics { contentDescription = addHere }, contentAlignment = Alignment.Center) {
        Box(Modifier.size(32.dp).drawBehind { drawRoundRect(line, cornerRadius = androidx.compose.ui.geometry.CornerRadius(8.dp.toPx()), style = androidx.compose.ui.graphics.drawscope.Stroke(1.5f.dp.toPx(), pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(10f, 8f)))) }, contentAlignment = Alignment.Center) {
            Icon(Icons.Filled.Add, null, Modifier.size(18.dp), tint = line)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PreviewScreen(modifier: Modifier = Modifier, onBack: () -> Unit, onStarted: () -> Unit) {
    val ctx = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val data by rememberTrainData()
    val settings = remember { TrainSettingsStore.load(ctx) }
    val profile = remember { ProfileStore.selected(ctx) }
    var routine by remember(data) { mutableStateOf(data.routine) }
    var edits by remember { mutableStateOf(PlanEdits()) }
    var swapSlot by remember { mutableStateOf<String?>(null) }
    var removeSlot by remember { mutableStateOf<String?>(null) }
    var breakSlot by remember { mutableStateOf<Pair<String, Int?>?>(null) }   // slot and, in the Detailed edit, the one set it is for
    var detailed by rememberSaveable { mutableStateOf(false) }
    var typeFor by remember { mutableStateOf<Pair<String, Int>?>(null) }      // slot and set of the number being typed
    var addAfter by remember { mutableStateOf<String?>("") }   // "" = closed, null = at the start, else after that slot
    var setTab by rememberSaveable { mutableIntStateOf(1) }
    var timelineOpen by remember { mutableStateOf(false) }
    val result = remember(data, routine, edits) {
        buildTrainPlan(data.catalog, routine, data.progress, profile, settings, edits = edits, nowEpochMs = System.currentTimeMillis())
    }
    val names = remember(data) { data.catalog.variations.associate { it.id to it.name } }

    Scaffold(
        modifier = modifier,
        topBar = { TopAppBar(title = { Text(stringResource(R.string.preview_title).uppercase(), style = MaterialTheme.typography.headlineMedium) }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back)) } }) },
        bottomBar = {
            Column {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Button(
                    onClick = { haptic.performHapticFeedback(HapticFeedbackType.LongPress); startPlan(ctx, (result as PlanResult.Ready).plan); onStarted() },
                    enabled = result is PlanResult.Ready,
                    modifier = Modifier.fillMaxWidth().padding(Spacing.l).height(56.dp).testTag("start_button"),
                    shape = RoundedCornerShape(Radius.button),
                ) { Text(stringResource(R.string.train_start).uppercase(), style = MaterialTheme.typography.titleLarge) }
            }
        },
    ) { pad ->
        Column(Modifier.padding(pad).verticalScroll(rememberScrollState()).padding(horizontal = Spacing.l, vertical = Spacing.s), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            when (val r = result) {
                is PlanResult.Infeasible -> {
                    Text(stringResource(R.string.train_infeasible), style = MaterialTheme.typography.titleMedium)
                    r.reasons.forEach { Text(it.message) }
                    r.alternatives.forEach { Text(stringResource(R.string.bullet_item, it.description), style = MaterialTheme.typography.bodySmall) }
                    if (!edits.isEmpty()) AppOutlinedButton(onClick = { edits = PlanEdits() }, Modifier.testTag("edits_reset")) { Text(stringResource(R.string.preview_reset)) }
                }
                is PlanResult.Ready -> {
                    val p = r.plan
                    val set = setTab.coerceIn(1, p.rounds)
                    Text(
                        stringResource(R.string.preview_header, p.rounds, (p.plannedDurationSeconds + 30) / 60, stringResource(if (p.timed) R.string.style_timed else R.string.style_reps)),
                        style = MaterialTheme.typography.titleMedium, modifier = Modifier.testTag("preview_header"),
                    )
                    InfoRow(p)
                    Row(Modifier.fillMaxWidth().heightIn(min = Spacing.touch).toggleable(detailed, role = androidx.compose.ui.semantics.Role.Switch) { detailed = it }.testTag("detailed_switch"), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(stringResource(R.string.preview_detailed), style = MaterialTheme.typography.bodyLarge)
                            Text(stringResource(R.string.preview_detailed_hint), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                        }
                        Switch(checked = detailed, onCheckedChange = null)
                    }
                    RulePicker(edits.rule ?: routine.rule) { edits = edits.copy(rule = it) }
                    // one set at a time (doc 17 §2.2); the Detailed edit lists every set
                    if (!detailed) Row(verticalAlignment = Alignment.CenterVertically) {
                        SingleChoiceSegmentedButtonRow(Modifier.weight(1f)) {
                            for (i in 1..p.rounds.coerceAtMost(6)) SegmentedButton(selected = i == set, onClick = { setTab = i }, colors = appSegmentedColors(), shape = SegmentedButtonDefaults.itemShape(i - 1, p.rounds.coerceAtMost(6)), icon = {}, modifier = Modifier.testTag("set_tab_$i")) { Text(stringResource(R.string.preview_set, i), maxLines = 1) }
                        }
                        Text(stringResource(R.string.train_minutes, (p.blocks.filter { it.roundIndex == set }.sumOf { it.durationSeconds } + 30) / 60), Modifier.padding(start = Spacing.s).testTag("set_time"), style = MaterialTheme.typography.labelLarge)
                    }
                    Caption(stringResource(if (detailed) R.string.preview_detailed_caption else R.string.preview_applies_to_all))
                    AddButton({ addAfter = null }, Modifier.align(Alignment.End).testTag("add_start"))
                    for (st in if (detailed) 1..p.rounds else set..set) {
                        if (detailed) Caption(stringResource(R.string.preview_set, st), Modifier.padding(top = Spacing.s).testTag("set_header_$st"))
                        val sfx = if (detailed) "_s$st" else ""
                        circuitOf(p, st).forEachIndexed { i, e ->
                            val v = data.catalog.variation(e.variationId)
                            ExerciseRow(i, e, v, names[e.variationId] ?: e.variationId, data.catalog.policyForVariation(e.variationId)?.tiers?.size ?: 5,
                                onLevel = { t -> edits = edits.copy(tierOverrides = edits.tierOverrides + (e.slotId to t), freeTargets = edits.freeTargets.filterKeys { k -> k != e.slotId && !k.endsWith(":" + e.slotId) }) },
                                onSwap = { swapSlot = e.slotId }, onRemove = { removeSlot = e.slotId }, sfx = sfx,
                                onTypeNumber = if (detailed) ({ typeFor = e.slotId to st }) else null)
                            BreakRow(e.slotId, p, st, names, onChange = { breakSlot = e.slotId to (if (detailed) st else null) }, onAdd = { addAfter = e.slotId }, sfx = sfx)
                        }
                    }
                    if (!edits.isEmpty()) AppOutlinedButton(onClick = { edits = PlanEdits() }, Modifier.testTag("edits_reset")) { Text(stringResource(R.string.preview_reset)) }
                    TimelineCard(p, names, timelineOpen) { timelineOpen = !timelineOpen }
                }
            }
        }
    }

    val slot = swapSlot
    if (slot != null && result is PlanResult.Ready) {
        val current = circuitOf(result.plan).firstOrNull { it.slotId == slot }?.variationId ?: ""
        SwapSheet(
            family = swapFamilyOptions(data.catalog, current, profile), others = swapOtherTypes(data.catalog, current, profile), currentId = current, catalog = data.catalog,
            onDismiss = { swapSlot = null },
            onPick = { id, keep ->
                edits = edits.copy(swaps = edits.swaps + (slot to id), tierOverrides = edits.tierOverrides - slot)
                if (keep) { routine = saveSwapsToRoutine(routine, mapOf(slot to id)); RoutineStore.save(ctx, routine); edits = edits.copy(swaps = edits.swaps - slot) }
                swapSlot = null
            },
        )
    }
    removeSlot?.let { id ->
        val nm = (result as? PlanResult.Ready)?.plan?.let { pl -> circuitOf(pl).firstOrNull { it.slotId == id }?.variationId }?.let { names[it] } ?: id
        AlertDialog(
            onDismissRequest = { removeSlot = null },
            title = { Text(stringResource(R.string.preview_remove_title, nm)) }, text = { Text(stringResource(R.string.preview_remove_text)) },
            confirmButton = { AppTextButton(onClick = { edits = edits.copy(removed = edits.removed + id, added = edits.added.filter { it.slot.id != id }); removeSlot = null }, modifier = Modifier.testTag("remove_confirm")) { Text(stringResource(R.string.preview_remove)) } },
            dismissButton = { AppTextButton(onClick = { removeSlot = null }) { Text(stringResource(R.string.cancel)) } },
        )
    }
    breakSlot?.let { (id, onlySet) ->
        StretchPickerSheet(title = R.string.preview_pick_stretch, options = stretchChoices(data.catalog, profile), autoLabel = true, onDismiss = { breakSlot = null }) { pick ->
            edits = if (onlySet != null) edits.copy(roundStretchPicks = if (pick == null) edits.roundStretchPicks - "$onlySet:$id" else edits.roundStretchPicks + ("$onlySet:$id" to pick))
                else edits.copy(stretchPicks = if (pick == null) edits.stretchPicks - id else edits.stretchPicks + (id to pick), roundStretchPicks = edits.roundStretchPicks.filterKeys { k -> !k.endsWith(":$id") })
            breakSlot = null
        }
    }
    typeFor?.let { (id, st) ->
        var text by remember(id, st) { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { typeFor = null },
            title = { Text(stringResource(R.string.preview_type_title, st)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
                    Text(stringResource(R.string.preview_type_text))
                    OutlinedTextField(value = text, onValueChange = { text = it.filter { c -> c.isDigit() }.take(3) }, singleLine = true, keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number), modifier = Modifier.testTag("free_number_field"))
                }
            },
            confirmButton = { AppTextButton(onClick = { text.toIntOrNull()?.takeIf { it > 0 }?.let { n -> edits = edits.copy(freeTargets = edits.freeTargets + ("$st:$id" to n)) }; typeFor = null }, modifier = Modifier.testTag("free_number_ok")) { Text(stringResource(R.string.preview_type_ok)) } },
            dismissButton = { AppTextButton(onClick = { edits = edits.copy(freeTargets = edits.freeTargets - "$st:$id"); typeFor = null }, modifier = Modifier.testTag("free_number_clear")) { Text(stringResource(R.string.preview_type_clear)) } },
        )
    }
    if (addAfter != "" && result is PlanResult.Ready) {
        val after = addAfter
        AddSheet(
            exercises = addableExercises(data.catalog, profile, result.plan.blocks.mapNotNull { it.variationId }.toSet()), stretches = stretchChoices(data.catalog, profile),
            onDismiss = { addAfter = "" },
            onExercise = { v -> edits = edits.copy(added = edits.added + AddedSlot(after, slotFor(v))); addAfter = "" },
            onStretch = { sId -> val key = after ?: circuitOf(result.plan).firstOrNull()?.slotId; if (key != null) edits = edits.copy(extraStretches = edits.extraStretches + (key to (edits.extraStretches[key].orEmpty() + sId))); addAfter = "" },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwapSheet(family: List<ExerciseVariation>, others: List<Pair<app.calisthenics.domain.tree.TreeTab, List<ExerciseVariation>>>, currentId: String, catalog: Catalog, onDismiss: () -> Unit, onPick: (String, Boolean) -> Unit) {
    var keep by remember { mutableStateOf(false) }
    var showOthers by remember { mutableStateOf(false) }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.padding(horizontal = Spacing.l).padding(bottom = Spacing.xl).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            Text(stringResource(R.string.preview_swap_title), style = MaterialTheme.typography.titleLarge)
            Text(stringResource(R.string.preview_swap_hint), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            family.forEach { o -> SwapItem(o, catalog, o.id == currentId, onPick = { onPick(o.id, keep) }) }
            Row(Modifier.fillMaxWidth().heightIn(min = Spacing.touch).clickable { keep = !keep }.testTag("swap_keep"), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.s)) {
                Checkbox(checked = keep, onCheckedChange = null)
                Text(stringResource(R.string.preview_swap_keep))
            }
            AppOutlinedButton(onClick = { showOthers = !showOthers }, Modifier.fillMaxWidth().height(52.dp).testTag("swap_other_types")) { Text(stringResource(R.string.preview_other_types)) }
            if (showOthers) {
                Text(stringResource(R.string.preview_other_hint), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                others.forEach { (tab, list) ->
                    Caption(tab.title, Modifier.padding(top = Spacing.s))
                    list.forEach { o -> SwapItem(o, catalog, false, onPick = { onPick(o.id, false) }) }
                }
            }
        }
    }
}

@Composable
private fun SwapItem(o: ExerciseVariation, catalog: Catalog, current: Boolean, onPick: () -> Unit) {
    val tiers = catalog.policyFor(o)?.tiers.orEmpty()
    ListItem(
        headlineContent = { Text(o.name) },
        supportingContent = if (tiers.isNotEmpty()) ({ Text(stringResource(R.string.preview_levels_line, tiers.first().target.value, tiers.last().target.value, stringResource(if (tiers.first().target.type == TargetType.REPS) R.string.unit_reps else R.string.unit_seconds))) }) else null,
        leadingContent = { io.github.gonbei774.calisthenicsmemory.ui.components.ExerciseThumb(o.id, o.name, 48.dp) },
        trailingContent = { if (current) Text(stringResource(R.string.preview_swap_current), style = MaterialTheme.typography.labelLarge) },
        modifier = Modifier.clickable { onPick() }.testTag("swap_option_${o.id}"),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StretchPickerSheet(title: Int, options: List<ExerciseVariation>, autoLabel: Boolean, onDismiss: () -> Unit, onPick: (String?) -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.padding(horizontal = Spacing.l).padding(bottom = Spacing.xl).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            Text(stringResource(title), style = MaterialTheme.typography.titleLarge)
            Text(stringResource(R.string.preview_pick_stretch_hint), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (autoLabel) ListItem(headlineContent = { Text(stringResource(R.string.preview_automatic)) }, modifier = Modifier.clickable { onPick(null) }.testTag("break_option_auto"))
            options.forEach { o ->
                ListItem(headlineContent = { Text(o.name) }, leadingContent = { io.github.gonbei774.calisthenicsmemory.ui.components.ExerciseThumb(o.id, o.name, 44.dp) }, modifier = Modifier.clickable { onPick(o.id) }.testTag("break_option_${o.id}"))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddSheet(exercises: List<Pair<app.calisthenics.domain.tree.TreeTab, List<ExerciseVariation>>>, stretches: List<ExerciseVariation>, onDismiss: () -> Unit, onExercise: (ExerciseVariation) -> Unit, onStretch: (String) -> Unit) {
    var mode by remember { mutableStateOf("") }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.padding(horizontal = Spacing.l).padding(bottom = Spacing.xl).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            Text(stringResource(R.string.preview_add_title), style = MaterialTheme.typography.titleLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s)) {
                AppOutlinedButton(onClick = { mode = "exercise" }, Modifier.weight(1f).height(52.dp).testTag("add_exercise")) { Text(stringResource(R.string.preview_add_exercise)) }
                AppOutlinedButton(onClick = { mode = "stretch" }, Modifier.weight(1f).height(52.dp).testTag("add_stretch")) { Text(stringResource(R.string.preview_add_stretch)) }
            }
            if (mode == "exercise") exercises.forEach { (tab, list) ->
                Caption(tab.title, Modifier.padding(top = Spacing.s))
                list.forEach { o -> ListItem(headlineContent = { Text(o.name) }, leadingContent = { io.github.gonbei774.calisthenicsmemory.ui.components.ExerciseThumb(o.id, o.name, 44.dp) }, modifier = Modifier.clickable { onExercise(o) }.testTag("add_option_${o.id}")) }
            }
            if (mode == "stretch") stretches.forEach { o ->
                ListItem(headlineContent = { Text(o.name) }, leadingContent = { io.github.gonbei774.calisthenicsmemory.ui.components.ExerciseThumb(o.id, o.name, 44.dp) }, modifier = Modifier.clickable { onStretch(o.id) }.testTag("add_stretch_${o.id}"))
            }
        }
    }
}

/** O2: App levels / Rep range / Custom. Rep range = sets x from-to with RR defaults; Custom edits every number. */
@Composable
private fun RulePicker(rule: ProgressionRule, onChange: (ProgressionRule) -> Unit) {
    Column(Modifier.fillMaxWidth().testTag("rule_picker"), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Caption(stringResource(R.string.rule_caption))
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            RuleKind.entries.forEachIndexed { i, k ->
                SegmentedButton(selected = rule.kind == k, onClick = { onChange(rule.withKind(k)) }, colors = appSegmentedColors(), shape = SegmentedButtonDefaults.itemShape(i, RuleKind.entries.size), icon = {}, modifier = Modifier.testTag("rule_${k.name}")) {
                    Text(stringResource(when (k) { RuleKind.APP_LEVELS -> R.string.rule_app_levels; RuleKind.REP_RANGE -> R.string.rule_rep_range; RuleKind.CUSTOM -> R.string.rule_custom }), maxLines = 1)
                }
            }
        }
        Text(
            stringResource(when (rule.kind) { RuleKind.APP_LEVELS -> R.string.rule_app_levels_hint; RuleKind.REP_RANGE -> R.string.rule_rep_range_hint; RuleKind.CUSTOM -> R.string.rule_custom_hint }, rule.sets, rule.from, rule.to, rule.holdFrom, rule.holdTo, rule.sessions),
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline, modifier = Modifier.testTag("rule_hint"),
        )
        if (rule.kind == RuleKind.CUSTOM) {
            @Composable fun line(label: Int, value: Int, range: IntRange, tag: String, set: (Int) -> ProgressionRule?) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(label), Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                    io.github.gonbei774.calisthenicsmemory.ui.components.Stepper(value, { v -> set(v)?.let(onChange) }, range, tag = tag)
                }
            }
            line(R.string.rule_sets, rule.sets, 1..10, "rule_sets") { v -> runCatching { rule.copy(sets = v) }.getOrNull() }
            line(R.string.rule_from, rule.from, 1..rule.to, "rule_from") { v -> runCatching { rule.copy(from = v) }.getOrNull() }
            line(R.string.rule_to, rule.to, rule.from..99, "rule_to") { v -> runCatching { rule.copy(to = v) }.getOrNull() }
            line(R.string.rule_sessions, rule.sessions, 1..10, "rule_sessions") { v -> runCatching { rule.copy(sessions = v) }.getOrNull() }
        }
    }
}
