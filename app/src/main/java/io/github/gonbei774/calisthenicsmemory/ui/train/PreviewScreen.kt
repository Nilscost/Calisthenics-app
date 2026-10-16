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
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
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
import app.calisthenics.domain.tree.TreeTabs
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.material.icons.filled.KeyboardArrowUp
import io.github.gonbei774.calisthenicsmemory.R
import io.github.gonbei774.calisthenicsmemory.ui.components.MuscleChips
import io.github.gonbei774.calisthenicsmemory.ui.screens.ProfileStore
import io.github.gonbei774.calisthenicsmemory.ui.screens.RoutineStore
import io.github.gonbei774.calisthenicsmemory.ui.screens.SavedRoutineStore
import app.calisthenics.domain.routine.savedFrom
import app.calisthenics.domain.routine.settingsFor
import app.calisthenics.domain.routine.updateSaved
import io.github.gonbei774.calisthenicsmemory.ui.session.startWorkout
import io.github.gonbei774.calisthenicsmemory.ui.theme.Radius
import io.github.gonbei774.calisthenicsmemory.ui.theme.Spacing
import kotlinx.serialization.json.Json
import java.util.UUID

fun formatClock(sec: Int) = "%d:%02d".format(sec / 60, sec % 60)

/** Apply the Preview's rest choices to every set and keep the displayed and started duration in sync. */
private fun withRestDurations(plan: WorkoutPlan, durations: Map<String, Int>): WorkoutPlan {
    if (durations.isEmpty()) return plan
    val blocks = plan.blocks.map { block ->
        if (block.type == BlockType.PASSIVE_RECOVERY && block.slotId in durations) block.copy(durationSeconds = durations.getValue(block.slotId!!)) else block
    }
    return plan.copy(blocks = blocks, plannedDurationSeconds = blocks.sumOf { it.durationSeconds })
}

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
private fun ExerciseRow(index: Int, e: CircuitEntry, v: ExerciseVariation?, name: String, levels: Int, onStep: (Int) -> Unit, onSwap: () -> Unit, onRemove: () -> Unit, sfx: String = "", onTypeNumber: (() -> Unit)? = null, badge: Int? = null, chain: Pair<Int, Int>? = null) {
    val removeLabel = stringResource(R.string.preview_remove)
    Card(Modifier.fillMaxWidth().pointerInput(Unit) { detectTapGestures(onLongPress = { onRemove() }) }
        .semantics { onLongClick(label = removeLabel) { onRemove(); true } }.testTag("exercise_${e.slotId}$sfx"), shape = MaterialTheme.shapes.large) {
        Column(Modifier.padding(Spacing.m), verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.m)) {
                if (badge != null) Box(Modifier.size(28.dp).clip(androidx.compose.foundation.shape.CircleShape).background(AppAccentTheme.colors.accent), contentAlignment = Alignment.Center) {
                    Text(badge.toString(), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.background, modifier = Modifier.testTag("round_badge_${e.slotId}$sfx"))
                }
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
                FilledTonalIconButton(onClick = { onStep(-1) }, enabled = if (chain != null) chain.first > 0 else e.tier > 1, modifier = Modifier.size(Spacing.touch).testTag("level_minus_${e.slotId}$sfx")) { Icon(io.github.gonbei774.calisthenicsmemory.ui.theme.AppIcons.Remove, stringResource(R.string.stepper_decrease)) }
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(targetText(e.target, e.perSide, e.loadGrams), style = MaterialTheme.typography.headlineMedium, color = if (e.free) AppAccentTheme.colors.text else MaterialTheme.colorScheme.onSurface,
                        modifier = (if (onTypeNumber != null) Modifier.clickable(onClick = onTypeNumber).heightIn(min = Spacing.touch).wrapContentHeight(Alignment.CenterVertically) else Modifier).testTag("exercise_target_${e.slotId}$sfx"))
                    Text(if (e.free) stringResource(R.string.preview_free_number) else if (chain != null) stringResource(R.string.preview_chain_pos, chain.first + 1, chain.second) else stringResource(R.string.preview_level, e.tier, levels), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline, modifier = Modifier.testTag("level_${e.slotId}$sfx"))
                }
                FilledTonalIconButton(onClick = { onStep(1) }, enabled = if (chain != null) chain.first < chain.second - 1 else e.tier < levels, modifier = Modifier.size(Spacing.touch).testTag("level_plus_${e.slotId}$sfx")) { Icon(Icons.Filled.Add, stringResource(R.string.stepper_increase)) }
            }
        }
    }
}

/** The break and its adjacent dashed +; plain rest can be adjusted directly in this row. */
@Composable
private fun BreakRow(slotId: String, p: WorkoutPlan, round: Int, names: Map<String, String>, onChange: () -> Unit, onRestChange: (Int) -> Unit, onAdd: () -> Unit, sfx: String = "") {
    val blocks = p.blocks.filter { it.slotId == slotId && it.roundIndex == round && (it.type == BlockType.STRETCH || it.type == BlockType.PASSIVE_RECOVERY) }
    val secs = blocks.sumOf { it.durationSeconds }
    val stretches = blocks.filter { it.type == BlockType.STRETCH }.mapNotNull { it.variationId }.distinct().mapNotNull { names[it] }
    // The break fills the row beside its adjacent + affordance.
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.s)) {
        if (blocks.isNotEmpty()) {
            val changeLabel = stringResource(R.string.preview_change)
            @Composable fun BreakContents() {
                if (stretches.isEmpty()) Row(
                    Modifier.fillMaxWidth().padding(start = Spacing.m, end = Spacing.xs, top = Spacing.xs, bottom = Spacing.xs),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(stringResource(R.string.block_rest), Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    io.github.gonbei774.calisthenicsmemory.ui.components.Stepper(
                        value = secs, onValueChange = onRestChange, range = 15..300, step = 15,
                        valueText = ::formatClock, tag = "rest_$slotId$sfx",
                    )
                    IconButton(onClick = onChange, modifier = Modifier.testTag("break_change_$slotId$sfx")) { Icon(Icons.Filled.Refresh, changeLabel) }
                } else Row(
                    Modifier.padding(start = Spacing.m, top = Spacing.xs, bottom = Spacing.xs),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(stringResource(R.string.preview_break_stretch, stretches.joinToString(" + "), formatClock(secs)), Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2)
                    Box(Modifier.size(Spacing.touch), contentAlignment = Alignment.Center) { Icon(Icons.Filled.Refresh, changeLabel) }
                }
            }
            val breakModifier = Modifier.weight(1f).testTag("break_$slotId$sfx")
            if (stretches.isNotEmpty()) {
                OutlinedCard(onClick = onChange, modifier = breakModifier, shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface), border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant)) {
                    BreakContents()
                }
            } else {
                Card(modifier = breakModifier, shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant)) {
                    BreakContents()
                }
            }
        }
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
    val stored = remember { TrainSettingsStore.load(ctx) }
    val resolved = remember(data) { resolveTrain(ctx, data.routine, stored, data.catalog) }   // V18: the usual plan, or the saved routine chosen on Train
    val profile = remember { ProfileStore.selected(ctx) }
    // R7-T08 (D1): frozen when the Preview opens, so removing or adding an exercise here does not re-fill the list
    val settings = remember(data) { withDefaultLength(data.catalog, resolved.routine, data.progress, profile, resolved.settings, edits = resolved.edits) }
    var routine by remember(data) { mutableStateOf(resolved.routine) }
    var edits by remember(data) { mutableStateOf(resolved.edits) }
    var restDurations by remember(data) { mutableStateOf<Map<String, Int>>(emptyMap()) }
    var saveDialog by remember { mutableStateOf(false) }
    var savedNote by remember { mutableStateOf<String?>(null) }
    val editing = remember { SavedRoutineStore.get(ctx, stored.routineId) }
    var swapSlot by remember { mutableStateOf<String?>(null) }
    var coolAdd by remember { mutableStateOf(false) }   // R7.2-T04: the picker for a stretch added to the cool-down
    var coolSwap by remember { mutableStateOf<Int?>(null) }   // R7-T13: the cool-down stretch being swapped
    var removeSlot by remember { mutableStateOf<String?>(null) }
    var breakSlot by remember { mutableStateOf<Pair<String, Int?>?>(null) }   // slot and, in the Detailed edit, the one set it is for
    var typeFor by remember { mutableStateOf<Pair<String, Int>?>(null) }      // slot and set of the number being typed
    var addAfter by remember { mutableStateOf<String?>("") }   // "" = closed, null = at the start, else after that slot
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
                Row(Modifier.fillMaxWidth().padding(Spacing.l), horizontalArrangement = Arrangement.spacedBy(Spacing.s), verticalAlignment = Alignment.CenterVertically) {
                    AppOutlinedButton(onClick = { saveDialog = true }, enabled = result is PlanResult.Ready, modifier = Modifier.weight(1f).heightIn(min = 56.dp).testTag("save_routine_button")) { Text(stringResource(R.string.routine_save), textAlign = TextAlign.Center) }
                    Button(
                        onClick = { haptic.performHapticFeedback(HapticFeedbackType.LongPress); startPlan(ctx, withRestDurations((result as PlanResult.Ready).plan, restDurations)); onStarted() },
                        enabled = result is PlanResult.Ready,
                        modifier = Modifier.weight(1.6f).heightIn(min = 56.dp).testTag("start_button"),
                        shape = RoundedCornerShape(Radius.button),
                    ) { Text(stringResource(R.string.train_start).uppercase(), style = MaterialTheme.typography.titleLarge) }
                }
            }
        },
    ) { pad ->
        Column(Modifier.padding(pad).verticalScroll(rememberScrollState()).padding(horizontal = Spacing.l, vertical = Spacing.s), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            when (val r = result) {
                is PlanResult.Infeasible -> {
                    Text(stringResource(R.string.train_infeasible), style = MaterialTheme.typography.titleMedium)
                    r.reasons.forEach { Text(it.message) }
                    r.alternatives.forEach { Text(stringResource(R.string.bullet_item, it.description), style = MaterialTheme.typography.bodySmall) }
                    if (!edits.isEmpty() || restDurations.isNotEmpty()) AppOutlinedButton(onClick = { edits = PlanEdits(); restDurations = emptyMap() }, Modifier.testTag("edits_reset")) { Text(stringResource(R.string.preview_reset)) }
                }
                is PlanResult.Ready -> {
                    val p = withRestDurations(r.plan, restDurations)
                    if (routine.stretchSession.isNotEmpty()) {   // L11b: a stretch-only session has no sets, levels or rule: just the list and the protocol
                        Text(stringResource(R.string.stretch_session_header, p.blocks.mapNotNull { it.variationId }.filter { names.containsKey(it) }.distinct().size, (p.plannedDurationSeconds + 30) / 60), style = MaterialTheme.typography.titleMedium, modifier = Modifier.testTag("preview_header"))
                        Text(stringResource(R.string.stretch_session_protocol), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.testTag("stretch_protocol"))
                        p.blocks.filter { it.type == BlockType.STRETCH }.mapNotNull { it.variationId }.distinct().forEachIndexed { i, id ->
                            Card(Modifier.fillMaxWidth().padding(vertical = 2.dp).testTag("stretch_row_$id"), shape = MaterialTheme.shapes.large) { Text("${i + 1} · ${names[id] ?: id}", Modifier.padding(Spacing.m), style = MaterialTheme.typography.titleMedium) }
                        }
                        return@Column
                    }
                    val set = 1
                    Text(
                        stringResource(R.string.preview_header, p.rounds, (p.plannedDurationSeconds + 30) / 60, stringResource(if (p.timed) R.string.style_timed else R.string.style_reps)),
                        style = MaterialTheme.typography.titleMedium, modifier = Modifier.testTag("preview_header"),
                    )
                    InfoRow(p)
                    savedNote?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = AppAccentTheme.colors.text, modifier = Modifier.testTag("routine_saved_note")) }
                    val rule = edits.rule ?: routine.rule
                    RulePicker(rule) { edits = edits.copy(rule = it) }
                    // R7-T10 (O1): under a range rule the numbers are the range, so the stepper steps through the exercises of the chain
                    if (rule.isRange) Text(stringResource(R.string.rule_range_note), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline, modifier = Modifier.testTag("rule_range_note"))
                    // one tile: the same design in the round view and in the full timeline
                    @Composable fun Tile(i: Int, e: CircuitEntry, st: Int, sfx: String, badge: Int?, typed: Boolean) {
                        val options = if (rule.isRange) mainPathOptions(data.catalog, e.variationId, profile, p.goalId) else emptyList()
                        val idx = options.indexOfFirst { it.id == e.variationId }
                        ExerciseRow(i, e, data.catalog.variation(e.variationId), names[e.variationId] ?: e.variationId, data.catalog.policyForVariation(e.variationId)?.tiers?.size ?: 5,
                            onStep = { d ->
                                if (rule.isRange) options.getOrNull(idx + d)?.let { o -> edits = edits.copy(swaps = edits.swaps + (e.slotId to o.id), tierOverrides = edits.tierOverrides - e.slotId, freeTargets = edits.freeTargets.filterKeys { k -> k != e.slotId && !k.endsWith(":" + e.slotId) }) }
                                else edits = edits.copy(tierOverrides = edits.tierOverrides + (e.slotId to stepLevel(e.tier, d, data.catalog.policyForVariation(e.variationId)?.tiers?.size ?: 5)), freeTargets = edits.freeTargets.filterKeys { k -> k != e.slotId && !k.endsWith(":" + e.slotId) })
                            },
                            onSwap = { swapSlot = e.slotId }, onRemove = { removeSlot = e.slotId }, sfx = sfx,
                            onTypeNumber = if (typed) ({ typeFor = e.slotId to st }) else null, badge = badge,
                            chain = if (rule.isRange && idx >= 0) idx to options.size else null)
                        BreakRow(e.slotId, p, st, names, onChange = { breakSlot = e.slotId to (if (typed) st else null) }, onRestChange = { restDurations = restDurations + (e.slotId to it) }, onAdd = { addAfter = e.slotId }, sfx = sfx)
                    }
                    // R7.2-T01 (F1): one round view for all sets (the edits apply to every set), no set tabs; the full timeline below lists every set
                    Text(stringResource(R.string.preview_all_sets, (p.blocks.filter { it.roundIndex == set }.sumOf { it.durationSeconds } + 30) / 60), style = MaterialTheme.typography.titleMedium, modifier = Modifier.testTag("all_sets_header").padding(top = Spacing.xs))
                    Caption(stringResource(R.string.preview_applies_to_all))
                    AddButton({ addAfter = null }, Modifier.align(Alignment.End).testTag("add_start"))
                    circuitOf(p, set).forEachIndexed { i, e -> Tile(i, e, set, "", null, false) }
                    // R7-T13 (D10): the cool-down after the last set: timed minutes with - / +, or stretches that can be swapped
                    val cool = p.blocks.filter { it.type == BlockType.COOLDOWN }
                    if (settings.cooldownOn) {
                        Caption(stringResource(R.string.preview_cooldown_caption), Modifier.padding(top = Spacing.s).testTag("cooldown_caption"))
                        if (cool.isNotEmpty() && cool.all { it.variationId == null }) {
                            val minutes = (cool.sumOf { it.durationSeconds } + 30) / 60
                            Card(Modifier.fillMaxWidth().testTag("cooldown_timed"), shape = MaterialTheme.shapes.large) {
                                Row(Modifier.padding(Spacing.m), verticalAlignment = Alignment.CenterVertically) {
                                    Text(stringResource(R.string.preview_cooldown_timed, minutes), Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                                    io.github.gonbei774.calisthenicsmemory.ui.components.Stepper(minutes, { edits = edits.copy(cooldownMinutes = it) }, 1..30, tag = "cooldown_minutes")
                                }
                            }
                        } else {
                            val ids = cool.mapNotNull { it.variationId }.distinct()
                            val removeLabel = stringResource(R.string.preview_remove)
                            ids.forEachIndexed { i, id ->
                                val secs = cool.filter { it.variationId == id }.sumOf { it.durationSeconds }
                                val removeThis = { edits = edits.copy(cooldownRemoved = edits.cooldownRemoved + id, cooldownAdded = edits.cooldownAdded - id) }
                                // R7.2-T04 (F5): a long press removes the stretch; the visible x does the same
                                Card(Modifier.fillMaxWidth().pointerInput(id) { detectTapGestures(onLongPress = { removeThis() }) }
                                    .semantics { onLongClick(label = removeLabel) { removeThis(); true } }.testTag("cooldown_stretch_$i"), shape = MaterialTheme.shapes.large) {
                                    Row(Modifier.padding(Spacing.m), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.m)) {
                                        io.github.gonbei774.calisthenicsmemory.ui.components.ExerciseThumb(id, names[id] ?: id, 44.dp)
                                        Column(Modifier.weight(1f)) {
                                            Text(names[id] ?: id, style = MaterialTheme.typography.titleMedium)
                                            Text(formatClock(secs), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                                        }
                                        IconButton(onClick = { coolSwap = i }, modifier = Modifier.testTag("cooldown_swap_$i")) { Icon(Icons.Filled.Refresh, stringResource(R.string.preview_swap, names[id] ?: id)) }
                                        IconButton(onClick = removeThis, modifier = Modifier.testTag("cooldown_remove_$i")) { Icon(Icons.Filled.Close, stringResource(R.string.preview_cooldown_remove, names[id] ?: id)) }
                                    }
                                }
                            }
                            AddButton({ coolAdd = true }, Modifier.align(Alignment.End).testTag("cooldown_add"))
                        }
                    }
                    if (!edits.isEmpty() || restDurations.isNotEmpty()) AppOutlinedButton(onClick = { edits = PlanEdits(); restDurations = emptyMap() }, Modifier.testTag("edits_reset")) { Text(stringResource(R.string.preview_reset)) }
                    // R7-T10 (D4): the full timeline is the editor: every set's tiles, each with its set number; a stretch or a typed number can differ per set
                    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                        Row(Modifier.fillMaxWidth().clickable { timelineOpen = !timelineOpen }.heightIn(min = Spacing.touch).padding(horizontal = Spacing.m).testTag("timeline_toggle"), verticalAlignment = Alignment.CenterVertically) {
                            Text(stringResource(R.string.preview_timeline, p.blocks.size), Modifier.weight(1f), style = MaterialTheme.typography.titleSmall)
                            Icon(if (timelineOpen) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown, contentDescription = null)
                        }
                    }
                    if (timelineOpen) Column(Modifier.fillMaxWidth().testTag("timeline_list"), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                        Caption(stringResource(R.string.preview_detailed_caption))
                        for (st in 1..p.rounds) {
                            Caption(stringResource(R.string.preview_set, st), Modifier.padding(top = Spacing.s).testTag("set_header_$st"))
                            circuitOf(p, st).forEachIndexed { i, e -> Tile(i, e, st, "_s$st", st, true) }
                        }
                    }
                }
            }
        }
    }

    if (saveDialog) {
        var name by remember { mutableStateOf(editing?.name ?: "") }
        fun save(asNew: Boolean) {
            val now = System.currentTimeMillis()
            val rule = edits.rule ?: routine.rule
            val countNow = (result as? PlanResult.Ready)?.plan?.let { exercisesPerRoundOf(it) }   // the saved list already holds the hand edits
            val fresh = savedFrom(java.util.UUID.randomUUID().toString(), name, 1, routineForPlan(data.catalog, routine, data.progress, profile, settings), edits, settings.copy(exercisesPerRound = countNow ?: settings.exercisesPerRound), rule, now)
            val toStore = if (!asNew && editing != null) updateSaved(editing, fresh.copy(name = name.trim().ifBlank { editing.name })) else fresh
            SavedRoutineStore.upsert(ctx, toStore)
            TrainSettingsStore.save(ctx, settingsFor(toStore, stored))
            savedNote = ctx.getString(if (!asNew && editing != null) R.string.routine_updated else R.string.routine_saved, toStore.name)
            saveDialog = false
        }
        AlertDialog(
            onDismissRequest = { saveDialog = false },
            title = { Text(stringResource(R.string.routine_save_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
                    Text(stringResource(R.string.routine_save_text))
                    OutlinedTextField(value = name, onValueChange = { name = it.take(40) }, singleLine = true, label = { Text(stringResource(R.string.routine_name)) }, modifier = Modifier.testTag("routine_name_field"))
                }
            },
            confirmButton = {
                Column(horizontalAlignment = Alignment.End) {
                    if (editing != null) AppTextButton(onClick = { save(false) }, modifier = Modifier.testTag("routine_update")) { Text(stringResource(R.string.routine_update)) }
                    AppTextButton(onClick = { save(true) }, enabled = name.isNotBlank(), modifier = Modifier.testTag("routine_save_new")) { Text(stringResource(if (editing != null) R.string.routine_save_as_new else R.string.routine_save_ok)) }
                }
            },
            dismissButton = { AppTextButton(onClick = { saveDialog = false }) { Text(stringResource(R.string.cancel)) } },
        )
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
    coolSwap?.let { idx ->
        val current = (result as? PlanResult.Ready)?.plan?.blocks?.filter { it.type == BlockType.COOLDOWN }?.mapNotNull { it.variationId }?.distinct().orEmpty()
        StretchPickerSheet(title = R.string.preview_pick_cooldown, options = stretchChoices(data.catalog, profile).filter { it.id !in current || it.id == current.getOrNull(idx) }, autoLabel = false, onDismiss = { coolSwap = null }, hint = R.string.preview_pick_cooldown_hint) { pick ->
            if (pick != null && idx < current.size) edits = edits.copy(cooldownStretches = current.toMutableList().also { it[idx] = pick })
            coolSwap = null
        }
    }
    if (coolAdd) {
        val current = (result as? PlanResult.Ready)?.plan?.blocks?.filter { it.type == BlockType.COOLDOWN }?.mapNotNull { it.variationId }?.toSet().orEmpty()
        StretchPickerSheet(title = R.string.preview_pick_cooldown, options = stretchChoices(data.catalog, profile).filter { it.id !in current }, autoLabel = false, onDismiss = { coolAdd = false }, hint = R.string.preview_pick_cooldown_hint) { pick ->
            if (pick != null) edits = edits.copy(cooldownAdded = (edits.cooldownAdded - pick) + pick, cooldownRemoved = edits.cooldownRemoved - pick)
            coolAdd = false
        }
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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
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
            // R7.2-T02 (F2): the button is a header (▾/▴); opening it scrolls the sheet to the first newly shown item
            AppOutlinedButton(onClick = { showOthers = !showOthers }, Modifier.fillMaxWidth().height(52.dp).testTag("swap_other_types")) {
                Text(stringResource(R.string.preview_other_types), Modifier.weight(1f))
                Icon(if (showOthers) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown, contentDescription = null)
            }
            val firstShown = remember { androidx.compose.foundation.relocation.BringIntoViewRequester() }
            LaunchedEffect(showOthers) { if (showOthers) { androidx.compose.runtime.withFrameNanos { }; firstShown.bringIntoView() } }
            if (showOthers) {
                // O3: the current exercise's own tab first, in a light accent box; every other tab stays plain below it
                val ownTab = TreeTabs.tabForVariation(catalog, currentId)
                others.forEachIndexed { gi, (tab, list) ->
                    val same = tab.id == ownTab?.id
                    val groupMod = if (same) Modifier.fillMaxWidth().padding(top = Spacing.s).clip(RoundedCornerShape(Radius.card)).background(AppAccentTheme.colors.accent.copy(alpha = 0.14f)).padding(vertical = Spacing.xs).testTag("swap_same_family") else Modifier.fillMaxWidth()
                    Column(groupMod.then(if (gi == 0) Modifier.bringIntoViewRequester(firstShown) else Modifier)) {
                        if (gi == 0) Text(stringResource(R.string.preview_other_hint), Modifier.padding(horizontal = if (same) Spacing.m else 0.dp), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                        Caption(if (same) stringResource(R.string.preview_same_family, tab.title) else tab.title, Modifier.padding(top = Spacing.s, start = if (same) Spacing.m else 0.dp).testTag(if (same) "swap_same_family_caption" else "swap_group_${tab.id}"))
                        list.forEach { o -> SwapItem(o, catalog, false, onPick = { onPick(o.id, false) }, transparent = same) }
                    }
                }
            }
        }
    }
}

@Composable
private fun SwapItem(o: ExerciseVariation, catalog: Catalog, current: Boolean, onPick: () -> Unit, transparent: Boolean = false) {
    val tiers = catalog.policyFor(o)?.tiers.orEmpty()
    ListItem(
        headlineContent = { Text(o.name) },
        supportingContent = if (tiers.isNotEmpty()) ({ Text(stringResource(R.string.preview_levels_line, tiers.first().target.value, tiers.last().target.value, stringResource(if (tiers.first().target.type == TargetType.REPS) R.string.unit_reps else R.string.unit_seconds))) }) else null,
        leadingContent = { io.github.gonbei774.calisthenicsmemory.ui.components.ExerciseThumb(o.id, o.name, 48.dp) },
        trailingContent = { if (current) Text(stringResource(R.string.preview_swap_current), style = MaterialTheme.typography.labelLarge) },
        colors = if (transparent) ListItemDefaults.colors(containerColor = androidx.compose.ui.graphics.Color.Transparent) else ListItemDefaults.colors(),
        modifier = Modifier.clickable { onPick() }.testTag("swap_option_${o.id}"),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StretchPickerSheet(title: Int, options: List<ExerciseVariation>, autoLabel: Boolean, onDismiss: () -> Unit, hint: Int = R.string.preview_pick_stretch_hint, onPick: (String?) -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.padding(horizontal = Spacing.l).padding(bottom = Spacing.xl).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            Text(stringResource(title), style = MaterialTheme.typography.titleLarge)
            Text(stringResource(hint), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (autoLabel) ListItem(headlineContent = { Text(stringResource(R.string.preview_automatic)) }, modifier = Modifier.clickable { onPick(null) }.testTag("break_option_auto"))
            options.forEach { o ->
                ListItem(headlineContent = { Text(o.name) }, leadingContent = { io.github.gonbei774.calisthenicsmemory.ui.components.ExerciseThumb(o.id, o.name, 44.dp) }, modifier = Modifier.clickable { onPick(o.id) }.testTag("break_option_${o.id}"))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun AddSheet(exercises: List<Pair<app.calisthenics.domain.tree.TreeTab, List<ExerciseVariation>>>, stretches: List<ExerciseVariation>, onDismiss: () -> Unit, onExercise: (ExerciseVariation) -> Unit, onStretch: (String) -> Unit) {
    // R7-T10 (D11): the category first (the Progress tab names), then only that category's list
    var category by remember { mutableStateOf("") }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.padding(horizontal = Spacing.l).padding(bottom = Spacing.xl).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            Text(stringResource(R.string.preview_add_title), style = MaterialTheme.typography.titleLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.s), modifier = Modifier.testTag("add_categories")) {
                exercises.forEach { (tab, _) -> FilterChip(selected = category == tab.id, onClick = { category = tab.id }, label = { Text(tab.title) }, modifier = Modifier.testTag("add_cat_${tab.id}")) }
                FilterChip(selected = category == "stretches", onClick = { category = "stretches" }, label = { Text(stringResource(R.string.preview_add_stretches)) }, modifier = Modifier.testTag("add_cat_stretches"))
            }
            if (category.isEmpty()) Text(stringResource(R.string.preview_add_pick_category), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            exercises.firstOrNull { it.first.id == category }?.second?.forEach { o ->
                ListItem(headlineContent = { Text(o.name) }, leadingContent = { io.github.gonbei774.calisthenicsmemory.ui.components.ExerciseThumb(o.id, o.name, 44.dp) }, modifier = Modifier.clickable { onExercise(o) }.testTag("add_option_${o.id}"))
            }
            if (category == "stretches") stretches.forEach { o ->
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
