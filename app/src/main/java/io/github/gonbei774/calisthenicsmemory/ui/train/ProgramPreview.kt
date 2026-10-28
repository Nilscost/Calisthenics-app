// R8.1-T08 (F9): a program loaded into the Preview as a starting point. Same controls as the workout Preview (set format, sets, Flow, rest),
// its own item list (target, swap, add, remove); saving makes a new "My routine" and never changes the program in the catalog.
package io.github.gonbei774.calisthenicsmemory.ui.train

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import io.github.gonbei774.calisthenicsmemory.session.SessionBus
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.calisthenics.domain.equipment.isAvailable
import app.calisthenics.domain.model.*
import app.calisthenics.domain.planner.*
import app.calisthenics.domain.routine.savedFromProgram
import app.calisthenics.domain.routine.settingsFor
import app.calisthenics.domain.routine.updateSaved
import io.github.gonbei774.calisthenicsmemory.R
import io.github.gonbei774.calisthenicsmemory.ui.components.AppOutlinedButton
import io.github.gonbei774.calisthenicsmemory.ui.components.AppTextButton
import io.github.gonbei774.calisthenicsmemory.ui.components.Caption
import io.github.gonbei774.calisthenicsmemory.ui.components.ExerciseThumb
import io.github.gonbei774.calisthenicsmemory.ui.components.Stepper
import io.github.gonbei774.calisthenicsmemory.ui.components.appSegmentedColors
import io.github.gonbei774.calisthenicsmemory.ui.screens.ProfileStore
import io.github.gonbei774.calisthenicsmemory.ui.screens.SavedRoutineStore
import io.github.gonbei774.calisthenicsmemory.ui.theme.AppAccentTheme
import io.github.gonbei774.calisthenicsmemory.ui.theme.Radius
import io.github.gonbei774.calisthenicsmemory.ui.theme.Spacing
import java.util.UUID

/** What to offer when an item is swapped: exercises of the same kind of work (strength or stretch/mobility) that the equipment allows, same area first. */
internal fun programSwapOptions(catalog: Catalog, current: ExerciseVariation, profile: EquipmentProfile, inList: Set<String>): List<ExerciseVariation> {
    val stretchLike = current.kind == Kind.STRETCH || current.kind == Kind.MOBILITY
    return catalog.variations.filter { v ->
        v.id == current.id || (v.id !in inList && !v.demonstrationHold && isAvailable(v, profile) &&
            if (stretchLike) (v.kind == Kind.STRETCH || v.kind == Kind.MOBILITY) && (!v.optIn || v.id == current.id)
            else (v.kind == Kind.REPS || v.kind == Kind.HOLD) && !v.routineOnly && catalog.policyFor(v) != null)
    }.sortedWith(compareBy({ if (it.id == current.id) 0 else 1 }, { if (it.areas.any { a -> a in current.areas } || it.stretchAreas.any { a -> a in current.stretchAreas }) 0 else 1 }, { it.name }))
}

private fun programItemLevel(item: FixedProgramItem, catalog: Catalog): Int? {
    item.appLevel?.let { return it.coerceIn(1, 5) }
    val current = item.reps ?: item.seconds ?: return null
    val policy = catalog.policyForVariation(item.exerciseId) ?: return null
    return policy.tiers.filter { it.target.type == if (item.reps != null) TargetType.REPS else TargetType.HOLD_SECONDS }
        .minByOrNull { kotlin.math.abs(it.target.value - current) }?.index
}

private fun stepProgramItem(item: FixedProgramItem, delta: Int, catalog: Catalog): FixedProgramItem {
    val policy = catalog.policyForVariation(item.exerciseId)
    val kind = if (item.reps != null) TargetType.REPS else TargetType.HOLD_SECONDS
    val tiers = policy?.tiers.orEmpty().filter { it.target.type == kind }.sortedBy { it.index }
    if (tiers.isEmpty()) return adjustProgramItem(item, delta)
    val level = programItemLevel(item, catalog)?.coerceIn(1, tiers.size) ?: return adjustProgramItem(item, delta)
    val increment = if (kind == TargetType.REPS) 1 else 5
    val top = tiers.last()
    val extension = item.levelFiveTarget
    if (extension != null) {
        val next = extension + delta * increment
        if (delta < 0 && next <= top.target.value) {
            if (next == top.target.value) return programItemWith(item, top.target.value).copy(appLevel = top.index, levelFiveTarget = null)
            val previous = tiers.getOrNull(tiers.lastIndex - 1) ?: return item.copy(levelFiveTarget = null, appLevel = top.index)
            return programItemWith(item, previous.target.value).copy(appLevel = previous.index, levelFiveTarget = null)
        }
        return programItemWith(item, next.coerceIn(top.target.value + increment, 999)).copy(appLevel = top.index, levelFiveTarget = next.coerceIn(top.target.value + increment, 999))
    }
    if (level >= top.index) {
        return if (delta > 0) programItemWith(item, top.target.value + increment).copy(appLevel = top.index, levelFiveTarget = top.target.value + increment)
        else tiers.getOrNull(tiers.lastIndex - 1)?.let { programItemWith(item, it.target.value).copy(appLevel = it.index, levelFiveTarget = null) } ?: item
    }
    val nextLevel = (level + delta).coerceIn(1, top.index)
    val nextTier = tiers.firstOrNull { it.index == nextLevel } ?: return item
    return programItemWith(item, nextTier.target.value).copy(appLevel = nextTier.index, levelFiveTarget = null)
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class, ExperimentalComposeUiApi::class)
@Composable
@android.annotation.SuppressLint("LocalContextGetResourceValueCall")
internal fun ProgramPreview(modifier: Modifier, data: TrainData, mode: ProgramMode, stored: TrainSettings, onBack: () -> Unit, onStarted: () -> Unit) {
    val ctx = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val profile = remember { ProfileStore.selected(ctx) }
    val key = remember { draftKey(stored) }
    var content by remember { mutableStateOf(mode.content) }
    var format by remember { mutableStateOf(stored.format) }
    var sets by remember { mutableStateOf(stored.rounds) }
    val savedDraft = remember { PreviewDraftStore.get(ctx, draftKey(stored)) }
    var betweenMode by remember { mutableStateOf(stored.effectiveBetweenSetMode()) }
    var preparationSeconds by remember { mutableIntStateOf(stored.preparationSeconds.coerceIn(1, 30)) }
    var swapIndex by remember { mutableStateOf<Int?>(null) }
    var addAfter by remember { mutableStateOf<Int?>(Int.MIN_VALUE) }   // MIN_VALUE = closed, -1 = at the start, else after that item
    var saveDialog by remember { mutableStateOf(false) }
    var savedNote by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(content, preparationSeconds, betweenMode) {
        val latest = PreviewDraftStore.get(ctx, key) ?: savedDraft ?: PreviewDraft(key)
        PreviewDraftStore.put(ctx, latest.copy(program = content, preparationSeconds = preparationSeconds.coerceIn(1, 30)))
    }
    fun chooseBetween(mode: BetweenSetMode) {
        val latest = PreviewDraftStore.get(ctx, key) ?: savedDraft ?: PreviewDraft(key, program = content, ruleDefaultVersion = 1)
        updateBetweenSetMode(ctx, TrainSettingsStore.load(ctx), latest, mode)
        betweenMode = mode
    }
    val names = remember(data) { data.catalog.variations.associate { it.id to it.name } }
    val original = remember { mode.saved?.program ?: mode.base?.let(::programContentOf) }
    val result = remember(content, format, sets, betweenMode, preparationSeconds, profile) { buildProgramPlan(data.catalog, content, profile, betweenMode == BetweenSetMode.FLOW, sets, format, System.currentTimeMillis(), betweenMode, preparationSeconds) }
    val plan = (result as? FixedProgramPlanResult.Ready)?.plan
    val live by SessionBus.state.collectAsState()
    val running = live?.takeIf { !it.isTerminal } != null   // a workout is running: START would be ignored, so the button opens it
    val unavailable = (result as? FixedProgramPlanResult.Unavailable)?.items.orEmpty().associateBy { it.exerciseId }

    Scaffold(
        modifier = modifier,
        topBar = { TopAppBar(title = { Text(stringResource(R.string.preview_title).uppercase(), style = MaterialTheme.typography.headlineMedium) }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back)) } }) },
        bottomBar = {
            Column {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Row(Modifier.fillMaxWidth().padding(Spacing.l), horizontalArrangement = Arrangement.spacedBy(Spacing.s), verticalAlignment = Alignment.CenterVertically) {
                    AppOutlinedButton(onClick = { saveDialog = true }, enabled = plan != null, modifier = Modifier.weight(1f).heightIn(min = 56.dp).testTag("save_routine_button")) { Text(stringResource(R.string.routine_save), textAlign = TextAlign.Center) }
                    Button(
                        onClick = { haptic.performHapticFeedback(HapticFeedbackType.LongPress); if (running) onStarted() else plan?.let { startPlan(ctx, it); onStarted() } }, enabled = running || (plan != null && content.items.isNotEmpty()),
                        modifier = Modifier.weight(1.6f).heightIn(min = 56.dp).testTag("start_button"), shape = RoundedCornerShape(Radius.button),
                    ) { Text(stringResource(if (running) R.string.train_open_workout else R.string.train_start).uppercase(), style = MaterialTheme.typography.titleLarge) }
                }
            }
        },
    ) { pad ->
        Column(Modifier.padding(pad).verticalScroll(rememberScrollState()).padding(horizontal = Spacing.l, vertical = Spacing.s), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            // the compact description of the program: name, how many exercises and how long, and what it is
            val minutes = ((plan?.plannedDurationSeconds ?: 0) + 30) / 60
            Text(content.name, style = MaterialTheme.typography.titleLarge, modifier = Modifier.testTag("program_title"))
            Text(stringResource(R.string.program_preview_header, io.github.gonbei774.calisthenicsmemory.ui.components.countText(R.plurals.count_exercises, content.items.size), minutes), style = MaterialTheme.typography.titleMedium, modifier = Modifier.testTag("preview_header"))
            if (plan?.changesExplained?.isNotEmpty() == true) Text(stringResource(R.string.program_stretch_fallback), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.testTag("program_stretch_fallback"))
            mode.base?.notes?.takeIf { it.isNotBlank() }?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 3, modifier = Modifier.testTag("program_description")) }
            Text(stringResource(R.string.program_preview_start_point), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline, modifier = Modifier.testTag("program_start_point"))
            savedNote?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = AppAccentTheme.colors.text, modifier = Modifier.testTag("routine_saved_note")) }
            if (unavailable.isNotEmpty()) Text(stringResource(R.string.programs_unavailable, profile.name), color = MaterialTheme.colorScheme.error, modifier = Modifier.testTag("program_unavailable"))

            EquipmentNeededRow(app.calisthenics.domain.equipment.planEquipment(data.catalog, plan?.blocks?.mapNotNull { it.variationId } ?: content.items.map { it.exerciseId }, profile))
            Column(Modifier.fillMaxWidth().testTag("format_picker"), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Caption(stringResource(R.string.train_format_caption))
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    WorkoutFormat.entries.forEachIndexed { i, f ->
                        SegmentedButton(selected = format == f, onClick = { format = f; TrainSettingsStore.save(ctx, TrainSettingsStore.load(ctx).copy(format = f)) }, colors = appSegmentedColors(), shape = SegmentedButtonDefaults.itemShape(i, WorkoutFormat.entries.size), icon = {}, modifier = Modifier.testTag("preview_format_${f.name}")) {
                            Text(stringResource(when (f) { WorkoutFormat.CIRCUIT -> R.string.format_circuit; WorkoutFormat.PAIRS -> R.string.format_pairs; WorkoutFormat.STRAIGHT -> R.string.format_straight }), maxLines = 1)
                        }
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(if (format == WorkoutFormat.CIRCUIT) R.string.train_rounds_caption else R.string.train_sets_caption), Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                    Stepper(sets, { sets = it; TrainSettingsStore.save(ctx, TrainSettingsStore.load(ctx).copy(rounds = it)) }, MIN_TRAIN_ROUNDS..MAX_EXPLICIT_ROUNDS, tag = "preview_rounds")
                }
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.feedback_preparation_time), Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                Stepper(preparationSeconds, { preparationSeconds = it.coerceIn(1, 30); val m = betweenMode; TrainSettingsStore.save(ctx, TrainSettingsStore.load(ctx).copy(preparationSeconds = it.coerceIn(1, 30), betweenSetMode = m, flow = m == BetweenSetMode.FLOW, stretchOn = m == BetweenSetMode.STRETCH)) }, 1..30, tag = "preparation_seconds", valueText = { "$it s" })
            }
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                Caption(stringResource(R.string.train_between_caption))
                BetweenSetSelector(betweenMode, ::chooseBetween)
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.block_rest), Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                Stepper(content.restSeconds, { content = content.copy(restSeconds = it) }, 0..300, step = 5, valueText = ::formatClock, tag = "program_rest")
            }
            if (original != null && content != original) AppOutlinedButton(onClick = { content = original }, Modifier.testTag("edits_reset")) { Text(stringResource(R.string.preview_reset)) }

            AddButton({ addAfter = -1 }, Modifier.align(Alignment.End).testTag("program_add_start"))
            content.items.forEachIndexed { i, item ->
                val v = data.catalog.variation(item.exerciseId)
                val name = names[item.exerciseId] ?: item.exerciseId
                Card(Modifier.fillMaxWidth().testTag("program_item_$i"), shape = MaterialTheme.shapes.large) {
                    Column(Modifier.padding(Spacing.m), verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.m)) {
                            ExerciseThumb(item.exerciseId, name, 56.dp)
                            Column(Modifier.weight(1f)) {
                                Text("${i + 1}", style = MaterialTheme.typography.labelLarge, color = AppAccentTheme.colors.text)
                                Text(name, style = MaterialTheme.typography.titleMedium, modifier = Modifier.testTag("program_item_name_$i"))
                                unavailable[item.exerciseId]?.let { Text(stringResource(R.string.programs_missing_item, name, it.missing), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error, modifier = Modifier.testTag("program_item_unavailable_$i")) }
                            }
                            IconButton(onClick = { swapIndex = i }, modifier = Modifier.testTag("program_swap_$i")) { Icon(Icons.Filled.Refresh, stringResource(R.string.preview_swap, name)) }
                            IconButton(onClick = { content = content.copy(items = content.items.filterIndexed { k, _ -> k != i }) }, modifier = Modifier.testTag("program_remove_$i")) { Icon(Icons.Filled.Close, stringResource(R.string.preview_remove)) }
                        }
                        val seconds = item.reps == null
                        val levelFiveTarget = item.levelFiveTarget
                        val explicitAppLevel = item.appLevel
                        val effectiveTier = explicitAppLevel?.let { level -> data.catalog.policyForVariation(item.exerciseId)?.tiers?.firstOrNull { it.index == level } }
                        val value = if (effectiveTier != null) maxOf(effectiveTier.target.shownValue(), levelFiveTarget ?: 0)
                            else levelFiveTarget ?: item.reps ?: item.seconds ?: 1
                        val appLevel = programItemLevel(item, data.catalog)
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                            Column(Modifier.weight(1f)) {
                                Text(programSideText(v, item, value), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                if (levelFiveTarget != null) Text(stringResource(R.string.preview_level_extension), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline, modifier = Modifier.testTag("program_level_$i"))
                                else if (explicitAppLevel != null) Text(stringResource(R.string.preview_level, explicitAppLevel, 5), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline, modifier = Modifier.testTag("program_level_$i"))
                                else if (appLevel != null) Text(stringResource(R.string.preview_level, appLevel, 5), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline, modifier = Modifier.testTag("program_level_$i"))
                                else Text(stringResource(R.string.program_level_authored), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline, modifier = Modifier.testTag("program_level_$i"))
                            }
                            if (data.catalog.policyForVariation(item.exerciseId) != null && v?.kind != Kind.STRETCH && v?.kind != Kind.MOBILITY) {
                                FilledTonalIconButton(onClick = { content = content.copy(items = content.items.mapIndexed { k, it -> if (k == i) stepProgramItem(it, -1, data.catalog) else it }) }, enabled = appLevel == null || appLevel > 1 || item.levelFiveTarget != null, modifier = Modifier.size(Spacing.touch).testTag("program_target_${i}_minus")) { Icon(io.github.gonbei774.calisthenicsmemory.ui.theme.AppIcons.Remove, stringResource(R.string.stepper_decrease)) }
                                Text(if (seconds) "$value s" else "$value x", style = MaterialTheme.typography.titleLarge, modifier = Modifier.testTag("program_target_${i}_value"))
                                FilledTonalIconButton(onClick = { content = content.copy(items = content.items.mapIndexed { k, it -> if (k == i) stepProgramItem(it, 1, data.catalog) else it }) }, modifier = Modifier.size(Spacing.touch).testTag("program_target_${i}_plus")) { Icon(Icons.Filled.Add, stringResource(R.string.stepper_increase)) }
                            } else Stepper(value, { nv -> content = content.copy(items = content.items.mapIndexed { k, it -> if (k == i) programItemWith(it, nv) else it }) },
                                (if (seconds) 5 else 1)..999, tag = "program_target_$i", step = if (seconds) 5 else 1, valueText = { if (seconds) "$it s" else "$it x" })
                        }
                    }
                }
                AddButton({ addAfter = i }, Modifier.align(Alignment.End).testTag("program_add_after_$i"))
            }
            Spacer(Modifier.height(Spacing.l))
        }
    }

    swapIndex?.let { i ->
        val current = content.items.getOrNull(i)?.let { data.catalog.variation(it.exerciseId) }
        if (current != null) {
            val options = programSwapOptions(data.catalog, current, profile, content.items.map { it.exerciseId }.toSet())
            ModalBottomSheet(onDismissRequest = { swapIndex = null }, modifier = Modifier.semantics { testTagsAsResourceId = true }, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
                Column(Modifier.padding(horizontal = Spacing.l).padding(bottom = Spacing.xl).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                    Text(stringResource(R.string.preview_swap_title), style = MaterialTheme.typography.titleLarge)
                    options.forEach { o ->
                        ListItem(headlineContent = { Text(o.name) }, leadingContent = { ExerciseThumb(o.id, o.name, 44.dp) },
                            trailingContent = { if (o.id == current.id) Text(stringResource(R.string.preview_swap_current), style = MaterialTheme.typography.labelLarge) },
                            modifier = Modifier.clickable { if (o.id != current.id) content = content.copy(items = content.items.mapIndexed { k, it -> if (k == i) defaultProgramItem(data.catalog, o) else it }); swapIndex = null }.testTag("program_swap_option_${o.id}"))
                    }
                }
            }
        }
    }
    if (addAfter != Int.MIN_VALUE) {
        val after = addAfter ?: -1
        AddSheet(
            exercises = addableExercises(data.catalog, profile, content.items.map { it.exerciseId }.toSet()), stretches = stretchChoices(data.catalog, profile),
            onDismiss = { addAfter = Int.MIN_VALUE },
            onExercise = { v -> content = content.copy(items = content.items.toMutableList().also { it.add((after + 1).coerceIn(0, it.size), defaultProgramItem(data.catalog, v)) }); addAfter = Int.MIN_VALUE },
            onStretch = { id -> data.catalog.variation(id)?.let { v -> content = content.copy(items = content.items.toMutableList().also { it.add((after + 1).coerceIn(0, it.size), defaultProgramItem(data.catalog, v)) }) }; addAfter = Int.MIN_VALUE },
        )
    }
    if (saveDialog) {
        var name by remember { mutableStateOf(mode.saved?.name ?: "") }
        fun save(asNew: Boolean) {
            val now = System.currentTimeMillis()
            val current = TrainSettingsStore.load(ctx)
            val fresh = savedFromProgram(UUID.randomUUID().toString(), name.ifBlank { content.name }, 1, content, current, now)
            val toStore = if (!asNew && mode.saved != null) updateSaved(mode.saved, fresh.copy(name = name.trim().ifBlank { mode.saved.name })) else fresh
            SavedRoutineStore.upsert(ctx, toStore)
            TrainSettingsStore.save(ctx, settingsFor(toStore, current))
            savedNote = ctx.getString(if (!asNew && mode.saved != null) R.string.routine_updated else R.string.routine_saved, toStore.name)
            saveDialog = false
        }
        AlertDialog(
            onDismissRequest = { saveDialog = false },
            title = { Text(stringResource(R.string.routine_save_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
                    Text(stringResource(R.string.program_save_text))
                    OutlinedTextField(value = name, onValueChange = { name = it.take(40) }, singleLine = true, label = { Text(stringResource(R.string.routine_name)) }, modifier = Modifier.testTag("routine_name_field"))
                }
            },
            confirmButton = {
                Column(horizontalAlignment = Alignment.End) {
                    if (mode.saved != null) AppTextButton(onClick = { save(false) }, modifier = Modifier.testTag("routine_update")) { Text(stringResource(R.string.routine_update)) }
                    AppTextButton(onClick = { save(true) }, enabled = name.isNotBlank(), modifier = Modifier.testTag("routine_save_new")) { Text(stringResource(if (mode.saved != null) R.string.routine_save_as_new else R.string.routine_save_ok)) }
                }
            },
            dismissButton = { AppTextButton(onClick = { saveDialog = false }) { Text(stringResource(R.string.cancel)) } },
        )
    }
}

/** Under a program item's stepper: what its number means. SPLIT = two blocks, Left then Right; ALTERNATING = one block, shown as the total with the per-side number; never a lone "per side". */
@Composable
internal fun programSideText(v: app.calisthenics.domain.model.ExerciseVariation?, item: app.calisthenics.domain.model.FixedProgramItem, value: Int): String = when {
    v == null -> ""
    v.sideMode == app.calisthenics.domain.model.SideMode.SPLIT -> stringResource(R.string.programs_split_each)
    v.sideMode == app.calisthenics.domain.model.SideMode.ALTERNATING ->
        if (item.reps == null) stringResource(R.string.programs_alt_seconds, value * 2, value) else stringResource(R.string.programs_alt_reps, value * 2, value)
    else -> ""
}
