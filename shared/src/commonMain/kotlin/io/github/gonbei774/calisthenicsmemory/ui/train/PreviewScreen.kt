// U06 (F8, F6): the draft workout. One card per exercise of the circuit, swap sheet, collapsible timeline,
// one compact info row, and a sticky Start button.
package io.github.gonbei774.calisthenicsmemory.ui.train

import org.jetbrains.compose.resources.StringResource
import io.github.gonbei774.calisthenicsmemory.platform.formatClockText
import io.github.gonbei774.calisthenicsmemory.platform.todayEpochDay
import io.github.gonbei774.calisthenicsmemory.platform.nowEpochMs
import io.github.gonbei774.calisthenicsmemory.platform.newUuid
import io.github.gonbei774.calisthenicsmemory.platform.currentPlatformContext
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.onLongClick
import androidx.compose.foundation.layout.RowScope
import io.github.gonbei774.calisthenicsmemory.ui.icons.filled.Add
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import io.github.gonbei774.calisthenicsmemory.platform.exposeTestTagsAsResourceId
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.foundation.selection.toggleable
import io.github.gonbei774.calisthenicsmemory.ui.components.AppOutlinedButton
import io.github.gonbei774.calisthenicsmemory.ui.components.AppTextButton
import io.github.gonbei774.calisthenicsmemory.ui.components.Caption
import io.github.gonbei774.calisthenicsmemory.ui.components.Stepper
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
import io.github.gonbei774.calisthenicsmemory.ui.icons.Icons
import io.github.gonbei774.calisthenicsmemory.ui.icons.automirrored.filled.ArrowBack
import io.github.gonbei774.calisthenicsmemory.ui.icons.filled.Close
import io.github.gonbei774.calisthenicsmemory.ui.icons.filled.Info
import io.github.gonbei774.calisthenicsmemory.ui.icons.filled.KeyboardArrowDown
import io.github.gonbei774.calisthenicsmemory.ui.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import io.github.gonbei774.calisthenicsmemory.session.SessionBus
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.calisthenics.domain.model.*
import app.calisthenics.domain.model.Target
import app.calisthenics.domain.planner.*
import app.calisthenics.domain.routine.saveSwapsToRoutine
import app.calisthenics.domain.session.CueText
import app.calisthenics.domain.tree.TreeTabs
import androidx.compose.foundation.relocation.bringIntoViewRequester
import io.github.gonbei774.calisthenicsmemory.ui.icons.filled.KeyboardArrowUp
import io.github.gonbei774.calisthenicsmemory.shared.resources.*
import io.github.gonbei774.calisthenicsmemory.ui.components.MuscleChips
import io.github.gonbei774.calisthenicsmemory.ui.screens.ProfileStore
import io.github.gonbei774.calisthenicsmemory.ui.screens.RoutineStore
import io.github.gonbei774.calisthenicsmemory.ui.screens.SavedRoutineStore
import app.calisthenics.domain.routine.settingsFor
import app.calisthenics.domain.routine.updateSaved
import io.github.gonbei774.calisthenicsmemory.ui.session.startWorkout
import io.github.gonbei774.calisthenicsmemory.ui.theme.Radius
import io.github.gonbei774.calisthenicsmemory.ui.theme.Spacing
import kotlinx.serialization.json.Json

fun formatClock(sec: Int) = formatClockText(sec / 60, sec % 60)

/** The circuit: the work of round 1, one entry per exercise (both sides of a unilateral exercise are one entry). */
data class CircuitEntry(val slotId: String, val variationId: String, val target: Target?, val perSide: Boolean, val alternating: Boolean = false, val loadGrams: Int? = null, val tier: Int = 1, val free: Boolean = false, val levelFiveEligible: Boolean = false)

fun circuitOf(plan: WorkoutPlan, round: Int = 1): List<CircuitEntry> =
    plan.blocks.filter { it.type == BlockType.WORK && (it.roundIndex ?: 1) == round && it.slotId != null && it.variationId != null && !it.slotId!!.startsWith("cond") }   // R8.2-T03: the conditioning finisher has its own block
        .groupBy { it.slotId!! }.values.map { bs -> CircuitEntry(bs.first().slotId!!, bs.first().variationId!!, bs.first().target, bs.size > 1, bs.first().alternating, bs.first().loadGrams, bs.first().prescriptionTier ?: 1, bs.first().freeTarget, bs.first().levelFiveEligible) }

@Composable
fun targetText(t: Target?, perSide: Boolean, loadGrams: Int? = null, alternating: Boolean = false): String {
    t ?: return ""
    // SPLIT = two blocks, Left then Right, each with this number; ALTERNATING = one block showing the total with the per-side number.
    val base = io.github.gonbei774.calisthenicsmemory.ui.components.targetLabel(t, alternating)
    val sided = if (perSide) stringResource(Res.string.target_split, base) else base
    return withLoad(sided, loadGrams)
}

/** "10 reps" -> "10 reps · 12 kg" for kettlebell work. */
@Composable
fun withLoad(text: String, loadGrams: Int?): String =
    if (loadGrams == null || text.isEmpty()) text else stringResource(Res.string.target_with_load, text, app.calisthenics.domain.load.formatKg(loadGrams))

/** Owner 2026-10-26: what to prepare before the session, from the exercises actually in the plan (and so live as they change). */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EquipmentNeededRow(ids: List<String>, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth().testTag("equipment_needed"), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Caption(stringResource(Res.string.preview_equipment_caption))
        if (ids.isEmpty()) Text(stringResource(Res.string.programs_no_equipment), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.testTag("equipment_none"))
        else FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.m), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            ids.forEach { id ->
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.testTag("equipment_$id")) {
                    io.github.gonbei774.calisthenicsmemory.ui.theme.EquipmentIcons.forId(id)?.let { Icon(it, contentDescription = null, modifier = Modifier.size(20.dp)) }
                    Text(stringResource(io.github.gonbei774.calisthenicsmemory.ui.screens.equipmentLabelShortPublic(id)), style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
        Text(stringResource(Res.string.preview_equipment_hint), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
    }
}

/** The plan's notes: the one or two most important as plain lines, the rest behind a compact "More (n)". Nothing when there are none. */
fun planNotesOf(p: WorkoutPlan): List<PlanNote> =
    p.notes.sortedBy { it.priority }.ifEmpty { (p.warnings.map { PlanNote(it, NoteKind.INFO) } + p.changesExplained.filter { it.isNotBlank() }.map { PlanNote(it, NoteKind.INFO) }) }

@Composable
private fun InfoRow(p: WorkoutPlan) {
    var open by remember { mutableStateOf(false) }
    val notes = planNotesOf(p)
    if (notes.isEmpty()) return
    val shown = notes.take(2); val rest = notes.drop(2)
    Column(Modifier.fillMaxWidth().testTag("info_row"), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        (if (open) notes else shown).forEachIndexed { i, n -> Text(n.text, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.testTag("plan_note_$i")) }
        if (rest.isNotEmpty()) Text(
            stringResource(if (open) Res.string.preview_less_notes else Res.string.preview_more_notes, rest.size),
            style = MaterialTheme.typography.labelLarge, color = AppAccentTheme.colors.text,
            modifier = Modifier.clickable { open = !open }.padding(vertical = Spacing.xs).testTag("notes_more"),
        )
    }
}

private fun movementTag(v: ExerciseVariation?): StringResource = when (v?.patterns?.firstOrNull()) {
    Pattern.PUSH_HORIZONTAL, Pattern.PUSH_VERTICAL -> Res.string.tag_push
    Pattern.PULL_HORIZONTAL, Pattern.PULL_VERTICAL -> Res.string.tag_pull
    Pattern.SQUAT, Pattern.LUNGE -> Res.string.tag_squat
    Pattern.HINGE -> Res.string.tag_hinge
    Pattern.CALF_RAISE -> Res.string.tag_calf
    Pattern.CORE_ANTI_EXTENSION, Pattern.CORE_ANTI_ROTATION, Pattern.CORE_ANTI_LATERAL -> Res.string.tag_core
    else -> Res.string.tag_other
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ExerciseRow(index: Int, e: CircuitEntry, v: ExerciseVariation?, name: String, levels: Int, onStep: (Int) -> Unit, onSwap: () -> Unit, onRemove: () -> Unit, sfx: String = "", onTypeNumber: (() -> Unit)? = null, badge: Int? = null, chain: Pair<Int, Int>? = null) {
    val removeLabel = stringResource(Res.string.preview_remove)
    val largeText = LocalDensity.current.fontScale > 1.2f
    val removeNow by rememberUpdatedState(onRemove)   // the pointer handler outlives recompositions: always call the current slot's callback
    Card(Modifier.fillMaxWidth().pointerInput(e.slotId) { detectTapGestures(onLongPress = { removeNow() }) }
        .semantics { onLongClick(label = removeLabel) { onRemove(); true } }.testTag("exercise_${e.slotId}$sfx"), shape = MaterialTheme.shapes.large) {
        Column(Modifier.padding(Spacing.m), verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.m)) {
                if (badge != null) Box(Modifier.size(28.dp).clip(androidx.compose.foundation.shape.CircleShape).background(AppAccentTheme.colors.accent), contentAlignment = Alignment.Center) {
                    Text(badge.toString(), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.background, modifier = Modifier.testTag("round_badge_${e.slotId}$sfx"))
                }
                io.github.gonbei774.calisthenicsmemory.ui.components.ExerciseThumb(e.variationId, name, 56.dp)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(stringResource(Res.string.preview_slot_tag, index + 1, stringResource(movementTag(v))), style = MaterialTheme.typography.labelLarge, color = AppAccentTheme.colors.text, modifier = Modifier.testTag("slot_tag_${e.slotId}$sfx"))
                    if (!largeText) Text(name, style = MaterialTheme.typography.titleMedium, modifier = Modifier.testTag("exercise_name_${e.slotId}$sfx"))
                }
                if (v != null && v.primaryMuscles.isNotEmpty()) io.github.gonbei774.calisthenicsmemory.ui.components.BodyMap(v.primaryMuscles, v.secondaryMuscles, showNamesOnTap = false, tag = "muscles_${e.slotId}$sfx", viewWidth = 24.dp)
                IconButton(onClick = onSwap, modifier = Modifier.testTag("swap_${e.slotId}$sfx")) { Icon(Icons.Filled.Refresh, stringResource(Res.string.preview_swap, name)) }
            }
            // Give long exercise names the card width when system text is enlarged.
            if (largeText) Text(name, style = MaterialTheme.typography.titleMedium, modifier = Modifier.testTag("exercise_name_${e.slotId}$sfx"))
            // D3: the level stepper; the number between - and + is the target of that level
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                FilledTonalIconButton(onClick = { onStep(-1) }, enabled = if (chain != null) chain.first > 0 else e.tier > 1, modifier = Modifier.size(Spacing.touch).testTag("level_minus_${e.slotId}$sfx")) { Icon(io.github.gonbei774.calisthenicsmemory.ui.theme.AppIcons.Remove, stringResource(Res.string.stepper_decrease)) }
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(targetText(e.target, e.perSide, e.loadGrams, e.alternating), style = MaterialTheme.typography.headlineMedium, color = if (e.free) AppAccentTheme.colors.text else MaterialTheme.colorScheme.onSurface,
                        modifier = (if (onTypeNumber != null) Modifier.clickable(onClick = onTypeNumber).heightIn(min = Spacing.touch).wrapContentHeight(Alignment.CenterVertically) else Modifier).testTag("exercise_target_${e.slotId}$sfx"))
                Text(if (e.free) stringResource(Res.string.preview_free_number) else if (e.levelFiveEligible) stringResource(Res.string.preview_level_extension) else if (chain != null) stringResource(Res.string.preview_chain_pos, chain.first + 1, chain.second) else stringResource(Res.string.preview_level, e.tier, levels), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline, modifier = Modifier.testTag("level_${e.slotId}$sfx"))
                }
                FilledTonalIconButton(onClick = { onStep(1) }, enabled = if (chain != null) chain.first < chain.second - 1 else e.tier < levels || e.tier == levels, modifier = Modifier.size(Spacing.touch).testTag("level_plus_${e.slotId}$sfx")) { Icon(Icons.Filled.Add, stringResource(Res.string.stepper_increase)) }
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
            val changeLabel = stringResource(Res.string.preview_change)
            @Composable fun BreakContents() {
                if (stretches.isEmpty()) Row(
                    Modifier.fillMaxWidth().padding(start = Spacing.m, end = Spacing.xs, top = Spacing.xs, bottom = Spacing.xs),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(stringResource(Res.string.block_rest), Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    io.github.gonbei774.calisthenicsmemory.ui.components.Stepper(
                        value = secs, onValueChange = onRestChange, range = 15..300, step = 15,
                        valueText = ::formatClock, tag = "rest_$slotId$sfx",
                    )
                    IconButton(onClick = onChange, modifier = Modifier.testTag("break_change_$slotId$sfx")) { Icon(Icons.Filled.Refresh, changeLabel) }
                } else Row(
                    Modifier.padding(start = Spacing.m, top = Spacing.xs, bottom = Spacing.xs),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(stringResource(Res.string.preview_break_stretch, stretches.joinToString(" + "), formatClock(secs)), Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2)
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
fun AddButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val addHere = stringResource(Res.string.preview_add_here)
    val line = MaterialTheme.colorScheme.outline
    Box(modifier.size(Spacing.touch).clickable(onClick = onClick, role = androidx.compose.ui.semantics.Role.Button).semantics { contentDescription = addHere }, contentAlignment = Alignment.Center) {
        Box(Modifier.size(32.dp).drawBehind { drawRoundRect(line, cornerRadius = androidx.compose.ui.geometry.CornerRadius(8.dp.toPx()), style = androidx.compose.ui.graphics.drawscope.Stroke(1.5f.dp.toPx(), pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(10f, 8f)))) }, contentAlignment = Alignment.Center) {
            Icon(Icons.Filled.Add, null, Modifier.size(18.dp), tint = line)
        }
    }
}

/**
 * The Preview: the workout as a list of exercises to change, or (R8.1-T08, F9) a program loaded as a starting point: the same controls
 * (format, sets, Flow, rest), its own item list. Everything is edited here; the program in the catalog never changes.
 */
@Composable
fun PreviewScreen(modifier: Modifier = Modifier, onBack: () -> Unit, onStarted: () -> Unit, dataOverride: TrainData? = null) {
    val ctx = currentPlatformContext()
    val rawSettings = remember { TrainSettingsStore.load(ctx) }
    val loadedData by rememberTrainData()
    val data = dataOverride ?: loadedData
    val currentDraftKey = remember { draftKey(rawSettings) }
    val draft = remember { PreviewDraftStore.get(ctx, currentDraftKey) }
    val stored = effectiveTrainSettings(rawSettings, draft, TrainSettingsStore.modeRevision(ctx, currentDraftKey))
    val programMode = remember(data.catalog, stored, draft) { resolveProgramMode(ctx, data.catalog, stored, draft) }
    if (programMode != null) ProgramPreview(modifier, data, programMode, stored, onBack, onStarted)
    else WorkoutPreview(modifier, data, stored, draft, onBack, onStarted)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WorkoutPreview(modifier: Modifier, data: TrainData, stored: TrainSettings, draft: PreviewDraft?, onBack: () -> Unit, onStarted: () -> Unit) {
    val ctx = currentPlatformContext()
    val trainStrings = rememberTrainStrings()
    val haptic = LocalHapticFeedback.current
    val resolved = remember(data) { resolveTrain(ctx, data.routine, stored, data.catalog) }   // V18: the usual plan, or the saved routine chosen on Train
    val profile = remember { ProfileStore.selected(ctx) }
    val selectedSaved = remember(data.catalog, stored.routineId, stored.profileId) {
        routinesFor(ctx, data.catalog).firstOrNull { it.id == stored.routineId }
    }
    // R8.1-T05 (F6): the edits made before survive going back to Train and returning; they belong to the objective
    val draftKey = remember { draftKey(stored) }
    // R7-T08 (D1): frozen when the Preview opens, so removing or adding an exercise here does not re-fill the list
    val openingSettings = remember { withDefaultLength(data.catalog, resolved.routine, data.progress, profile, resolved.settings, edits = draft?.edits ?: resolved.edits) }
    // R8.1-T05 (F5): the set format and the number of sets are chosen here too; they are today's settings, so Train shows the same
    var format by remember { mutableStateOf(openingSettings.format) }
    var sets by remember { mutableStateOf(openingSettings.rounds) }
    var betweenMode by remember { mutableStateOf(openingSettings.effectiveBetweenSetMode()) }
    var modeRevision by remember { mutableLongStateOf(maxOf(TrainSettingsStore.modeRevision(ctx, draftKey), draft?.modeRevision ?: 0L)) }
    var preparationSeconds by remember { mutableIntStateOf(openingSettings.preparationSeconds.coerceIn(1, 30)) }
    val settings = remember(format, sets, betweenMode, preparationSeconds) { openingSettings.copy(format = format, rounds = sets, flow = betweenMode == BetweenSetMode.FLOW, stretchOn = betweenMode == BetweenSetMode.STRETCH, betweenSetMode = betweenMode, preparationSeconds = preparationSeconds) }
    // A Room progress refresh may replace TrainData while the Preview is open. Preserve the user's draft edits.
    var routine by remember { mutableStateOf(resolved.routine) }
    var edits by remember { mutableStateOf(draft?.edits ?: resolved.edits) }
    var restDurations by remember { mutableStateOf(selectedSaved?.restDurations.orEmpty() + draft?.rest.orEmpty()) }
    LaunchedEffect(edits, restDurations, preparationSeconds, betweenMode, modeRevision) {
        val latest = PreviewDraftStore.get(ctx, draftKey) ?: draft ?: PreviewDraft(draftKey)
        PreviewDraftStore.put(ctx, latest.copy(edits = edits, rest = restDurations, preparationSeconds = preparationSeconds.coerceIn(1, 30), ruleDefaultVersion = 1))
    }
    fun chooseBetween(mode: BetweenSetMode) {
        val latest = PreviewDraftStore.get(ctx, draftKey) ?: draft
        val (_, updatedDraft) = updateBetweenSetMode(ctx, TrainSettingsStore.load(ctx), latest, mode)
        modeRevision = updatedDraft.modeRevision
        betweenMode = mode
    }
    var saveDialog by remember { mutableStateOf(false) }
    var savedNote by remember { mutableStateOf<String?>(null) }
    val editing = remember { SavedRoutineStore.get(ctx, stored.routineId) }
    var swapSlot by remember { mutableStateOf<String?>(null) }
    var coolAdd by remember { mutableStateOf(false) }   // R7.2-T04: the picker for a stretch added to the cool-down
    var coolSwap by remember { mutableStateOf<Int?>(null) }   // R7-T13: the cool-down stretch being swapped
    var removeSlot by remember { mutableStateOf<String?>(null) }
    var beforeLastRemoval by remember { mutableStateOf<PlanEdits?>(null) }
    var breakSlot by remember { mutableStateOf<Pair<String, Int?>?>(null) }   // slot and, in the Detailed edit, the one set it is for
    var typeFor by remember { mutableStateOf<Pair<String, Int>?>(null) }      // slot and set of the number being typed
    var addAfter by remember { mutableStateOf<String?>("") }   // "" = closed, null = at the start, else after that slot
    var timelineOpen by remember { mutableStateOf(false) }
    val live by SessionBus.state.collectAsState()
    val running = live?.takeIf { !it.isTerminal } != null   // a workout is already running: START would be ignored by the service, so the button opens it
    val planEdits = effectivePlanEdits(edits, modeRevision)
    val result = remember(data, routine, planEdits, settings, modeRevision) {
        buildTrainPlan(data.catalog, routine, data.progress, profile, settings, edits = planEdits, nowEpochMs = nowEpochMs(), nowDay = todayEpochDay().toInt())
    }
    val emptyAfterRemoval = result is PlanResult.Infeasible && edits.removed.isNotEmpty() &&
        result.reasons.any { it.code == "no-slots" }
    val names = remember(data) { data.catalog.variations.associate { it.id to it.name } }

    Scaffold(
        modifier = modifier,
        topBar = { TopAppBar(title = { Text(stringResource(Res.string.preview_title).uppercase(), style = MaterialTheme.typography.headlineMedium) }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(Res.string.back)) } }) },
        bottomBar = {
            Column {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Row(Modifier.fillMaxWidth().padding(Spacing.l), horizontalArrangement = Arrangement.spacedBy(Spacing.s), verticalAlignment = Alignment.CenterVertically) {
                    AppOutlinedButton(onClick = { saveDialog = true }, enabled = result is PlanResult.Ready, modifier = Modifier.weight(1f).heightIn(min = 56.dp).testTag("save_routine_button")) { Text(stringResource(Res.string.routine_save), textAlign = TextAlign.Center) }
                    Button(
                        onClick = { haptic.performHapticFeedback(HapticFeedbackType.LongPress); if (!running) startPlan(ctx, withRestDurations((result as PlanResult.Ready).plan, restDurations)); onStarted() },
                        enabled = running || result is PlanResult.Ready,
                        modifier = Modifier.weight(1.6f).heightIn(min = 56.dp).testTag("start_button"),
                        shape = RoundedCornerShape(Radius.button),
                    ) { Text(stringResource(if (running) Res.string.train_open_workout else Res.string.train_start).uppercase(), style = MaterialTheme.typography.titleLarge) }
                }
            }
        },
    ) { pad ->
        Column(Modifier.padding(pad).verticalScroll(rememberScrollState()).padding(horizontal = Spacing.l, vertical = Spacing.s), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            when (val r = result) {
                is PlanResult.Infeasible -> {
                    if (emptyAfterRemoval) {
                        Text(stringResource(Res.string.preview_empty), style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.testTag("preview_empty"))
                        Text(stringResource(Res.string.preview_empty_hint))
                        AddButton({ addAfter = null }, Modifier.testTag("add_empty"))
                        beforeLastRemoval?.let { previous ->
                            AppOutlinedButton(onClick = { edits = previous; beforeLastRemoval = null },
                                Modifier.testTag("remove_undo")) { Text(stringResource(Res.string.preview_undo_remove)) }
                        }
                    } else {
                        Text(stringResource(Res.string.train_infeasible), style = MaterialTheme.typography.titleMedium)
                        r.reasons.forEach { Text(it.message) }
                        r.alternatives.forEach { Text(stringResource(Res.string.bullet_item, it.description), style = MaterialTheme.typography.bodySmall) }
                    }
                    if (edits != resolved.edits || restDurations.isNotEmpty()) AppOutlinedButton(onClick = { edits = resolved.edits; restDurations = emptyMap() }, Modifier.testTag("edits_reset")) { Text(stringResource(Res.string.preview_reset)) }
                }
                is PlanResult.Ready -> {
                    val p = withRestDurations(r.plan, restDurations)
                    if (routine.stretchSession.isNotEmpty()) {   // L11b: a stretch-only session has no sets, levels or rule: just the list and the protocol
                        Text(stringResource(Res.string.stretch_session_header, p.blocks.mapNotNull { it.variationId }.filter { names.containsKey(it) }.distinct().size, (p.plannedDurationSeconds + 30) / 60), style = MaterialTheme.typography.titleMedium, modifier = Modifier.testTag("preview_header"))
                        Text(stringResource(Res.string.stretch_session_protocol), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.testTag("stretch_protocol"))
                        p.blocks.filter { it.type == BlockType.STRETCH }.mapNotNull { it.variationId }.distinct().forEachIndexed { i, id ->
                            Card(Modifier.fillMaxWidth().padding(vertical = 2.dp).testTag("stretch_row_$id"), shape = MaterialTheme.shapes.large) { Text("${i + 1} · ${names[id] ?: id}", Modifier.padding(Spacing.m), style = MaterialTheme.typography.titleMedium) }
                        }
                        return@Column
                    }
                    val set = 1
                    Text(
                        stringResource(Res.string.preview_header, io.github.gonbei774.calisthenicsmemory.ui.components.countText(Res.plurals.count_rounds, p.rounds), (p.plannedDurationSeconds + 30) / 60, stringResource(if (p.timed) Res.string.style_timed else Res.string.style_reps)),
                        style = MaterialTheme.typography.titleMedium, modifier = Modifier.testTag("preview_header"),
                    )
                    EquipmentNeededRow(app.calisthenics.domain.equipment.planEquipment(data.catalog, p.blocks.mapNotNull { it.variationId }, profile))
                    InfoRow(p)
                    // R8.1-T05 (F5): Straight sets / Pairs / Circuit, and the number of sets, in the Preview
                    Column(Modifier.fillMaxWidth().testTag("format_picker"), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Caption(stringResource(Res.string.train_format_caption))
                        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                            WorkoutFormat.entries.forEachIndexed { i, f ->
                                SegmentedButton(selected = format == f, onClick = { format = f; TrainSettingsStore.save(ctx, TrainSettingsStore.load(ctx).copy(format = f)) }, colors = appSegmentedColors(), shape = SegmentedButtonDefaults.itemShape(i, WorkoutFormat.entries.size), icon = {}, modifier = Modifier.testTag("preview_format_${f.name}")) {
                                    Text(stringResource(when (f) { WorkoutFormat.CIRCUIT -> Res.string.format_circuit; WorkoutFormat.PAIRS -> Res.string.format_pairs; WorkoutFormat.STRAIGHT -> Res.string.format_straight }), maxLines = 1)
                                }
                            }
                        }
                        if (!(edits.rule ?: routine.rule).isRange) Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(stringResource(if (format == WorkoutFormat.CIRCUIT) Res.string.train_rounds_caption else Res.string.train_sets_caption), Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                            io.github.gonbei774.calisthenicsmemory.ui.components.Stepper(sets, { sets = it; TrainSettingsStore.save(ctx, TrainSettingsStore.load(ctx).copy(rounds = it)) }, MIN_TRAIN_ROUNDS..MAX_EXPLICIT_ROUNDS, tag = "preview_rounds")
                        }
                    }
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(Res.string.feedback_preparation_time), Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                        Stepper(preparationSeconds, { preparationSeconds = it.coerceIn(1, 30); TrainSettingsStore.save(ctx, TrainSettingsStore.load(ctx).copy(preparationSeconds = it.coerceIn(1, 30))) }, 1..30, tag = "preparation_seconds", valueText = { "$it s" })
                    }
                    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                        Caption(stringResource(Res.string.train_between_caption))
                        BetweenSetSelector(betweenMode, ::chooseBetween)
                    }
                    savedNote?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = AppAccentTheme.colors.text, modifier = Modifier.testTag("routine_saved_note")) }
                    val rule = edits.rule ?: routine.rule
                    RulePicker(rule) { edits = edits.copy(rule = it) }
                    // R7-T10 (O1): under a range rule the numbers are the range, so the stepper steps through the exercises of the chain
                    if (rule.isRange) Text(stringResource(Res.string.rule_range_note), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline, modifier = Modifier.testTag("rule_range_note"))
                    // one tile: the same design in the round view and in the full timeline
                    @Composable fun Tile(i: Int, e: CircuitEntry, st: Int, sfx: String, badge: Int?, typed: Boolean) {
                        val options = if (rule.isRange) mainPathOptions(data.catalog, e.variationId, profile, p.goalId) else emptyList()
                        val idx = options.indexOfFirst { it.id == e.variationId }
                        ExerciseRow(i, e, data.catalog.variation(e.variationId), names[e.variationId] ?: e.variationId, data.catalog.policyForVariation(e.variationId)?.tiers?.size ?: 5,
                            onStep = { d ->
                                if (rule.isRange) options.getOrNull(idx + d)?.let { o -> edits = edits.copy(swaps = edits.swaps + (e.slotId to o.id), tierOverrides = edits.tierOverrides - e.slotId, levelFiveTargets = edits.levelFiveTargets - e.slotId, freeTargets = edits.freeTargets.filterKeys { k -> k != e.slotId && !k.endsWith(":" + e.slotId) }) }
                                else {
                                    val tiers = data.catalog.policyForVariation(e.variationId)?.tiers.orEmpty().sortedBy { it.index }
                                    val top = tiers.lastOrNull()?.target
                                    val increment = if (e.target?.type == TargetType.HOLD_SECONDS) 5 else 1
                                    val levelFiveBase = top?.value
                                    val extension = edits.levelFiveTargets[e.slotId]?.takeIf { levelFiveBase != null && it > levelFiveBase }
                                    val extensionStepDown = d < 0 && e.tier >= 5 && extension != null && levelFiveBase != null
                                    val nextExtension = extension?.let { if (d < 0) maxOf(levelFiveBase ?: it, it - increment) else it + increment }
                                    val updatedExtensions = when {
                                        d > 0 && e.tier >= 5 -> edits.levelFiveTargets + (e.slotId to ((extension ?: e.target?.value ?: levelFiveBase ?: 0) + increment).coerceAtMost(999))
                                        extensionStepDown && nextExtension != null && nextExtension > levelFiveBase!! -> edits.levelFiveTargets + (e.slotId to nextExtension)
                                        else -> edits.levelFiveTargets - e.slotId
                                    }
                                    val nextTier = if (extensionStepDown) 5 else stepLevel(e.tier, d, tiers.size.coerceAtLeast(5))
                                    edits = edits.copy(tierOverrides = edits.tierOverrides + (e.slotId to nextTier), levelFiveTargets = updatedExtensions,
                                        freeTargets = edits.freeTargets.filterKeys { k -> k != e.slotId && !k.endsWith(":" + e.slotId) }).stampOverride(e.slotId, e.variationId, todayEpochDay())
                                }
                            },
                            onSwap = { swapSlot = e.slotId }, onRemove = { removeSlot = e.slotId }, sfx = sfx,
                            onTypeNumber = if (typed) ({ typeFor = e.slotId to st }) else null, badge = badge,
                            chain = if (rule.isRange && idx >= 0) idx to options.size else null)
                        BreakRow(e.slotId, p, st, names, onChange = { breakSlot = e.slotId to (if (typed) st else null) }, onRestChange = { restDurations = restDurations + (e.slotId to it) }, onAdd = { addAfter = e.slotId }, sfx = sfx)
                    }
                    // R8.2-T03 (G3): the included warm-up as a block, its items removable
                    val warmBlocks = p.blocks.filter { it.type == BlockType.WARMUP }
                    if (settings.warmupOn && warmBlocks.isNotEmpty()) {
                        Caption(stringResource(Res.string.preview_warmup_caption, (warmBlocks.sumOf { it.durationSeconds } + 30) / 60), Modifier.padding(top = Spacing.s).testTag("warmup_caption"))
                        warmBlocks.mapNotNull { it.variationId }.distinct().forEachIndexed { i, id ->
                            Card(Modifier.fillMaxWidth().testTag("warmup_item_$i"), shape = MaterialTheme.shapes.large) {
                                Row(Modifier.padding(Spacing.m), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.m)) {
                                    io.github.gonbei774.calisthenicsmemory.ui.components.ExerciseThumb(id, names[id] ?: id, 44.dp)
                                    Column(Modifier.weight(1f)) { Text(names[id] ?: id, style = MaterialTheme.typography.titleMedium); Text(formatClock(warmBlocks.filter { it.variationId == id }.sumOf { it.durationSeconds }), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline) }
                                    IconButton(onClick = { edits = edits.copy(warmupRemoved = edits.warmupRemoved + id) }, modifier = Modifier.testTag("warmup_remove_$i")) { Icon(Icons.Filled.Close, stringResource(Res.string.preview_remove)) }
                                }
                            }
                        }
                    }
                    // R7.2-T01 (F1): one round view for all sets (the edits apply to every set), no set tabs; the full timeline below lists every set
                    Text(stringResource(Res.string.preview_all_sets, (p.blocks.filter { it.roundIndex == set }.sumOf { it.durationSeconds } + 30) / 60), style = MaterialTheme.typography.titleMedium, modifier = Modifier.testTag("all_sets_header").padding(top = Spacing.xs))
                    Caption(stringResource(Res.string.preview_applies_to_all))
                    AddButton({ addAfter = null }, Modifier.align(Alignment.End).testTag("add_start"))
                    circuitOf(p, set).forEachIndexed { i, e -> Tile(i, e, set, "", null, false) }
                    // R7-T13 (D10): the cool-down after the last set: timed minutes with - / +, or stretches that can be swapped
                    // R8.2-T03 (G3): the included conditioning finisher: minutes with - / + and its exercises removable
                    val condBlocks = p.blocks.filter { it.slotId?.startsWith("cond") == true }
                    if (settings.conditioningOn && condBlocks.isNotEmpty()) {
                        val condMinutes = edits.conditioningMinutes ?: settings.conditioningMinutes
                        Row(Modifier.fillMaxWidth().padding(top = Spacing.s), verticalAlignment = Alignment.CenterVertically) {
                            Caption(stringResource(Res.string.preview_conditioning_caption, (condBlocks.sumOf { it.durationSeconds } + 30) / 60), Modifier.weight(1f).testTag("conditioning_caption"))
                            io.github.gonbei774.calisthenicsmemory.ui.components.Stepper(condMinutes, { edits = edits.copy(conditioningMinutes = it) }, MIN_CONDITIONING_MINUTES..MAX_CONDITIONING_MINUTES, tag = "conditioning_minutes")
                        }
                        condBlocks.filter { it.type == BlockType.WORK }.mapNotNull { it.variationId }.distinct().forEachIndexed { i, id ->
                            Card(Modifier.fillMaxWidth().testTag("conditioning_item_$i"), shape = MaterialTheme.shapes.large) {
                                Row(Modifier.padding(Spacing.m), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.m)) {
                                    io.github.gonbei774.calisthenicsmemory.ui.components.ExerciseThumb(id, names[id] ?: id, 44.dp)
                                    Text(names[id] ?: id, Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                                    IconButton(onClick = { edits = edits.copy(conditioningRemoved = edits.conditioningRemoved + id) }, modifier = Modifier.testTag("conditioning_remove_$i")) { Icon(Icons.Filled.Close, stringResource(Res.string.preview_remove)) }
                                }
                            }
                        }
                    }
                    val cool = p.blocks.filter { it.type == BlockType.COOLDOWN }
                    if (settings.cooldownOn) {
                        Caption(stringResource(Res.string.preview_cooldown_caption), Modifier.padding(top = Spacing.s).testTag("cooldown_caption"))
                        if (cool.isNotEmpty() && cool.all { it.variationId == null }) {
                            val minutes = (cool.sumOf { it.durationSeconds } + 30) / 60
                            Card(Modifier.fillMaxWidth().testTag("cooldown_timed"), shape = MaterialTheme.shapes.large) {
                                Row(Modifier.padding(Spacing.m), verticalAlignment = Alignment.CenterVertically) {
                                    Text(stringResource(Res.string.preview_cooldown_timed, minutes), Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                                    io.github.gonbei774.calisthenicsmemory.ui.components.Stepper(minutes, { edits = edits.copy(cooldownMinutes = it) }, 1..30, tag = "cooldown_minutes")
                                }
                            }
                        } else {
                            val ids = cool.mapNotNull { it.variationId }.distinct()
                            val removeLabel = stringResource(Res.string.preview_remove)
                            ids.forEachIndexed { i, id ->
                                val secs = cool.filter { it.variationId == id }.sumOf { it.durationSeconds }
                                val removeThis = { edits = edits.copy(cooldownRemoved = edits.cooldownRemoved + id, cooldownAdded = edits.cooldownAdded - id) }
                                val removeNow by rememberUpdatedState(removeThis)
                                // R7.2-T04 (F5): a long press removes the stretch; the visible x does the same
                                Card(Modifier.fillMaxWidth().pointerInput(id) { detectTapGestures(onLongPress = { removeNow() }) }
                                    .semantics { onLongClick(label = removeLabel) { removeThis(); true } }.testTag("cooldown_stretch_$i"), shape = MaterialTheme.shapes.large) {
                                    Row(Modifier.padding(Spacing.m), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.m)) {
                                        io.github.gonbei774.calisthenicsmemory.ui.components.ExerciseThumb(id, names[id] ?: id, 44.dp)
                                        Column(Modifier.weight(1f)) {
                                            Text(names[id] ?: id, style = MaterialTheme.typography.titleMedium)
                                            Text(formatClock(secs), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                                        }
                                        IconButton(onClick = { coolSwap = i }, modifier = Modifier.testTag("cooldown_swap_$i")) { Icon(Icons.Filled.Refresh, stringResource(Res.string.preview_swap, names[id] ?: id)) }
                                        IconButton(onClick = removeThis, modifier = Modifier.testTag("cooldown_remove_$i")) { Icon(Icons.Filled.Close, stringResource(Res.string.preview_cooldown_remove, names[id] ?: id)) }
                                    }
                                }
                            }
                            AddButton({ coolAdd = true }, Modifier.align(Alignment.End).testTag("cooldown_add"))
                        }
                    }
                    if (edits != resolved.edits || restDurations.isNotEmpty()) AppOutlinedButton(onClick = { edits = resolved.edits; restDurations = emptyMap() }, Modifier.testTag("edits_reset")) { Text(stringResource(Res.string.preview_reset)) }
                    // R7-T10 (D4): the full timeline is the editor: every set's tiles, each with its set number; a stretch or a typed number can differ per set
                    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                        Row(Modifier.fillMaxWidth().clickable { timelineOpen = !timelineOpen }.heightIn(min = Spacing.touch).padding(horizontal = Spacing.m).testTag("timeline_toggle"), verticalAlignment = Alignment.CenterVertically) {
                            Text(stringResource(Res.string.preview_timeline, p.blocks.size), Modifier.weight(1f), style = MaterialTheme.typography.titleSmall)
                            Icon(if (timelineOpen) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown, contentDescription = null)
                        }
                    }
                    if (timelineOpen) Column(Modifier.fillMaxWidth().testTag("timeline_list"), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                        Caption(stringResource(Res.string.preview_detailed_caption))
                        for (st in 1..p.rounds) {
                            Caption(stringResource(Res.string.preview_set, st), Modifier.padding(top = Spacing.s).testTag("set_header_$st"))
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
            val now = nowEpochMs()
            val countNow = (result as? PlanResult.Ready)?.plan?.let { exercisesPerRoundOf(it) }   // the saved list already holds the hand edits
            val fresh = savedCurrentWorkout(newUuid(), name, 1,
                routineForPlan(data.catalog, routine, data.progress, profile, settings), edits,
                settings.copy(exercisesPerRound = countNow ?: settings.exercisesPerRound), modeRevision, restDurations, now)
            val toStore = if (!asNew && editing != null) updateSaved(editing, fresh.copy(name = name.trim().ifBlank { editing.name })) else fresh
            SavedRoutineStore.upsert(ctx, toStore)
            TrainSettingsStore.save(ctx, settingsFor(toStore, stored))
            savedNote = trainStrings.format(if (!asNew && editing != null) Res.string.routine_updated else Res.string.routine_saved, toStore.name)
            saveDialog = false
        }
        AlertDialog(
            onDismissRequest = { saveDialog = false },
            title = { Text(stringResource(Res.string.routine_save_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
                    Text(stringResource(Res.string.routine_save_text))
                    OutlinedTextField(value = name, onValueChange = { name = it.take(40) }, singleLine = true, label = { Text(stringResource(Res.string.routine_name)) }, modifier = Modifier.testTag("routine_name_field"))
                }
            },
            confirmButton = {
                Column(horizontalAlignment = Alignment.End) {
                    if (editing != null) AppTextButton(onClick = { save(false) }, modifier = Modifier.testTag("routine_update")) { Text(stringResource(Res.string.routine_update)) }
                    AppTextButton(onClick = { save(true) }, enabled = name.isNotBlank(), modifier = Modifier.testTag("routine_save_new")) { Text(stringResource(if (editing != null) Res.string.routine_save_as_new else Res.string.routine_save_ok)) }
                }
            },
            dismissButton = { AppTextButton(onClick = { saveDialog = false }) { Text(stringResource(Res.string.cancel)) } },
        )
    }

    val slot = swapSlot
    if (slot != null && result is PlanResult.Ready) {
        val current = circuitOf(result.plan).firstOrNull { it.slotId == slot }?.variationId ?: ""
        SwapSheet(
            family = swapFamilyOptions(data.catalog, current, profile), others = swapOtherTypes(data.catalog, current, profile), currentId = current, catalog = data.catalog,
            onDismiss = { swapSlot = null },
            onPick = { id, keep ->
                edits = edits.copy(swaps = edits.swaps + (slot to id), tierOverrides = edits.tierOverrides - slot, levelFiveTargets = edits.levelFiveTargets - slot,
                    freeTargets = edits.freeTargets.filterKeys { k -> k != slot && !k.endsWith(":" + slot) })
                if (keep && stored.routineId == null) { routine = saveSwapsToRoutine(routine, mapOf(slot to id)); RoutineStore.save(ctx, routine); edits = edits.copy(swaps = edits.swaps - slot) }
                else if (keep && editing != null) {   // a saved routine keeps the swap in its own slots; the usual plan is not touched
                    routine = saveSwapsToRoutine(routine, mapOf(slot to id))
                    SavedRoutineStore.upsert(ctx, editing.copy(routine = routine, revision = routine.revision, updatedAtEpochMs = nowEpochMs()))
                    edits = edits.copy(swaps = edits.swaps - slot)
                }
                swapSlot = null
            },
        )
    }
    removeSlot?.let { id ->
        val nm = (result as? PlanResult.Ready)?.plan?.let { pl -> circuitOf(pl).firstOrNull { it.slotId == id }?.variationId }?.let { names[it] } ?: id
        AlertDialog(
            onDismissRequest = { removeSlot = null },
            title = { Text(stringResource(Res.string.preview_remove_title, nm)) }, text = { Text(stringResource(Res.string.preview_remove_text)) },
            confirmButton = { AppTextButton(onClick = { beforeLastRemoval = edits; edits = edits.copy(removed = edits.removed + id, added = edits.added.filter { it.slot.id != id }); removeSlot = null }, modifier = Modifier.testTag("remove_confirm")) { Text(stringResource(Res.string.preview_remove)) } },
            dismissButton = { AppTextButton(onClick = { removeSlot = null }) { Text(stringResource(Res.string.cancel)) } },
        )
    }
    coolSwap?.let { idx ->
        val current = (result as? PlanResult.Ready)?.plan?.blocks?.filter { it.type == BlockType.COOLDOWN }?.mapNotNull { it.variationId }?.distinct().orEmpty()
        StretchPickerSheet(title = Res.string.preview_pick_cooldown, options = stretchChoices(data.catalog, profile).filter { it.id !in current || it.id == current.getOrNull(idx) }, autoLabel = false, onDismiss = { coolSwap = null }, hint = Res.string.preview_pick_cooldown_hint) { pick ->
            if (pick != null && idx < current.size) edits = edits.swapCooldownStretch(current, idx, pick)
            coolSwap = null
        }
    }
    if (coolAdd) {
        val current = (result as? PlanResult.Ready)?.plan?.blocks?.filter { it.type == BlockType.COOLDOWN }?.mapNotNull { it.variationId }?.toSet().orEmpty()
        StretchPickerSheet(title = Res.string.preview_pick_cooldown, options = stretchChoices(data.catalog, profile).filter { it.id !in current }, autoLabel = false, onDismiss = { coolAdd = false }, hint = Res.string.preview_pick_cooldown_hint) { pick ->
            if (pick != null) edits = edits.copy(cooldownAdded = (edits.cooldownAdded - pick) + pick, cooldownRemoved = edits.cooldownRemoved - pick)
            coolAdd = false
        }
    }
    breakSlot?.let { (id, onlySet) ->
        StretchPickerSheet(title = Res.string.preview_pick_stretch, options = stretchChoices(data.catalog, profile), autoLabel = true, onDismiss = { breakSlot = null }) { pick ->
            edits = if (onlySet != null) edits.copy(roundStretchPicks = if (pick == null) edits.roundStretchPicks - "$onlySet:$id" else edits.roundStretchPicks + ("$onlySet:$id" to pick))
                else edits.copy(stretchPicks = if (pick == null) edits.stretchPicks - id else edits.stretchPicks + (id to pick), roundStretchPicks = edits.roundStretchPicks.filterKeys { k -> !k.endsWith(":$id") })
            breakSlot = null
        }
    }
    typeFor?.let { (id, st) ->
        var text by remember(id, st) { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { typeFor = null },
            title = { Text(stringResource(Res.string.preview_type_title, st)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
                    Text(stringResource(Res.string.preview_type_text))
                    OutlinedTextField(value = text, onValueChange = { text = it.filter { c -> c.isDigit() }.take(3) }, singleLine = true, keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number), modifier = Modifier.testTag("free_number_field"))
                }
            },
            confirmButton = { AppTextButton(onClick = { text.toIntOrNull()?.takeIf { it > 0 }?.let { n -> edits = edits.copy(freeTargets = edits.freeTargets + ("$st:$id" to n)).stampOverride(id, (result as? PlanResult.Ready)?.plan?.let { pl -> circuitOf(pl).firstOrNull { it.slotId == id }?.variationId }, todayEpochDay()) }; typeFor = null }, modifier = Modifier.testTag("free_number_ok")) { Text(stringResource(Res.string.preview_type_ok)) } },
            dismissButton = { AppTextButton(onClick = { edits = edits.copy(freeTargets = edits.freeTargets - "$st:$id"); typeFor = null }, modifier = Modifier.testTag("free_number_clear")) { Text(stringResource(Res.string.preview_type_clear)) } },
        )
    }
    if (addAfter != "" && (result is PlanResult.Ready || emptyAfterRemoval)) {
        val after = addAfter
        val currentPlan = (result as? PlanResult.Ready)?.plan
        AddSheet(
            exercises = addableExercises(data.catalog, profile, currentPlan?.blocks.orEmpty().mapNotNull { it.variationId }.toSet()),
            stretches = if (emptyAfterRemoval) emptyList() else stretchChoices(data.catalog, profile),
            onDismiss = { addAfter = "" },
            onExercise = { v -> edits = edits.copy(added = edits.added + AddedSlot(after, slotFor(v))); addAfter = "" },
            onStretch = { sId -> val key = after ?: currentPlan?.let { circuitOf(it).firstOrNull()?.slotId }; if (key != null) edits = edits.copy(extraStretches = edits.extraStretches + (key to (edits.extraStretches[key].orEmpty() + sId)), extraStretchModeRevision = modeRevision); addAfter = "" },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class, ExperimentalComposeUiApi::class)
@Composable
private fun SwapSheet(family: List<ExerciseVariation>, others: List<Pair<app.calisthenics.domain.tree.TreeTab, List<ExerciseVariation>>>, currentId: String, catalog: Catalog, onDismiss: () -> Unit, onPick: (String, Boolean) -> Unit) {
    var keep by remember { mutableStateOf(false) }
    var showOthers by remember { mutableStateOf(false) }
    ModalBottomSheet(onDismissRequest = onDismiss, modifier = Modifier.exposeTestTagsAsResourceId(), sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.padding(horizontal = Spacing.l).padding(bottom = Spacing.xl).testTag("swap_sheet_body").verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            Text(stringResource(Res.string.preview_swap_title), style = MaterialTheme.typography.titleLarge)
            Text(stringResource(Res.string.preview_swap_hint), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            family.forEach { o -> SwapItem(o, catalog, o.id == currentId, onPick = { onPick(o.id, keep) }) }
            Row(Modifier.fillMaxWidth().heightIn(min = Spacing.touch).clickable { keep = !keep }.testTag("swap_keep"), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.s)) {
                Checkbox(checked = keep, onCheckedChange = null)
                Text(stringResource(Res.string.preview_swap_keep))
            }
            // R7.2-T02 (F2): the button is a header (▾/▴); opening it scrolls the sheet to the first newly shown item
            AppOutlinedButton(onClick = { showOthers = !showOthers }, Modifier.fillMaxWidth().height(52.dp).testTag("swap_other_types")) {
                Text(stringResource(Res.string.preview_other_types), Modifier.weight(1f))
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
                        if (gi == 0) Text(stringResource(Res.string.preview_other_hint), Modifier.padding(horizontal = if (same) Spacing.m else 0.dp), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                        Caption(if (same) stringResource(Res.string.preview_same_family, tab.title) else tab.title, Modifier.padding(top = Spacing.s, start = if (same) Spacing.m else 0.dp).testTag(if (same) "swap_same_family_caption" else "swap_group_${tab.id}"))
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
        supportingContent = if (tiers.isNotEmpty()) ({ Text(stringResource(Res.string.preview_levels_line, tiers.first().target.value, tiers.last().target.value, stringResource(if (tiers.first().target.type == TargetType.REPS) Res.string.unit_reps else Res.string.unit_seconds))) }) else null,
        leadingContent = { io.github.gonbei774.calisthenicsmemory.ui.components.ExerciseThumb(o.id, o.name, 48.dp) },
        trailingContent = { if (current) Text(stringResource(Res.string.preview_swap_current), style = MaterialTheme.typography.labelLarge) },
        colors = if (transparent) ListItemDefaults.colors(containerColor = androidx.compose.ui.graphics.Color.Transparent) else ListItemDefaults.colors(),
        modifier = Modifier.clickable { onPick() }.testTag("swap_option_${o.id}"),
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
fun StretchPickerSheet(title: StringResource, options: List<ExerciseVariation>, autoLabel: Boolean, onDismiss: () -> Unit, hint: StringResource = Res.string.preview_pick_stretch_hint, onPick: (String?) -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss, modifier = Modifier.exposeTestTagsAsResourceId(), sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.padding(horizontal = Spacing.l).padding(bottom = Spacing.xl).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            Text(stringResource(title), style = MaterialTheme.typography.titleLarge)
            Text(stringResource(hint), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (autoLabel) ListItem(headlineContent = { Text(stringResource(Res.string.preview_automatic)) }, modifier = Modifier.clickable { onPick(null) }.testTag("break_option_auto"))
            options.forEach { o ->
                ListItem(headlineContent = { Text(o.name) }, leadingContent = { io.github.gonbei774.calisthenicsmemory.ui.components.ExerciseThumb(o.id, o.name, 44.dp) }, modifier = Modifier.clickable { onPick(o.id) }.testTag("break_option_${o.id}"))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class, ExperimentalComposeUiApi::class)
@Composable
fun AddSheet(exercises: List<Pair<app.calisthenics.domain.tree.TreeTab, List<ExerciseVariation>>>, stretches: List<ExerciseVariation>, onDismiss: () -> Unit, onExercise: (ExerciseVariation) -> Unit, onStretch: (String) -> Unit) {
    // R7-T10 (D11): the category first (the Progress tab names), then only that category's list
    var category by remember { mutableStateOf("") }
    ModalBottomSheet(onDismissRequest = onDismiss, modifier = Modifier.exposeTestTagsAsResourceId(), sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.padding(horizontal = Spacing.l).padding(bottom = Spacing.xl).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            Text(stringResource(Res.string.preview_add_title), style = MaterialTheme.typography.titleLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.s), modifier = Modifier.testTag("add_categories")) {
                exercises.forEach { (tab, _) -> FilterChip(selected = category == tab.id, onClick = { category = tab.id }, label = { Text(tab.title) }, modifier = Modifier.testTag("add_cat_${tab.id}")) }
                if (stretches.isNotEmpty()) FilterChip(selected = category == "stretches", onClick = { category = "stretches" }, label = { Text(stringResource(Res.string.preview_add_stretches)) }, modifier = Modifier.testTag("add_cat_stretches"))
            }
            if (category.isEmpty()) Text(stringResource(Res.string.preview_add_pick_category), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
        Caption(stringResource(Res.string.rule_caption))
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            RuleKind.entries.forEachIndexed { i, k ->
                SegmentedButton(selected = rule.kind == k, onClick = { onChange(rule.withKind(k)) }, colors = appSegmentedColors(), shape = SegmentedButtonDefaults.itemShape(i, RuleKind.entries.size), icon = {}, modifier = Modifier.testTag("rule_${k.name}")) {
                    Text(stringResource(when (k) { RuleKind.APP_LEVELS -> Res.string.rule_app_levels; RuleKind.REP_RANGE -> Res.string.rule_rep_range; RuleKind.CUSTOM -> Res.string.rule_custom }), maxLines = 1)
                }
            }
        }
        Text(
            stringResource(when (rule.kind) { RuleKind.APP_LEVELS -> Res.string.rule_app_levels_hint; RuleKind.REP_RANGE -> Res.string.rule_rep_range_hint; RuleKind.CUSTOM -> Res.string.rule_custom_hint }, rule.sets, rule.from, rule.to, rule.holdFrom, rule.holdTo, rule.sessions),
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline, modifier = Modifier.testTag("rule_hint"),
        )
        if (rule.kind == RuleKind.CUSTOM) {
            @Composable fun line(label: StringResource, value: Int, range: IntRange, tag: String, set: (Int) -> ProgressionRule?) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(label), Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                    io.github.gonbei774.calisthenicsmemory.ui.components.Stepper(value, { v -> set(v)?.let(onChange) }, range, tag = tag)
                }
            }
            line(Res.string.rule_sets, rule.sets, 1..10, "rule_sets") { v -> runCatching { rule.copy(sets = v) }.getOrNull() }
            line(Res.string.rule_from, rule.from, 1..rule.to, "rule_from") { v -> runCatching { rule.copy(from = v) }.getOrNull() }
            line(Res.string.rule_to, rule.to, rule.from..99, "rule_to") { v -> runCatching { rule.copy(to = v) }.getOrNull() }
            line(Res.string.rule_sessions, rule.sessions, 1..10, "rule_sessions") { v -> runCatching { rule.copy(sessions = v) }.getOrNull() }
        }
    }
}
