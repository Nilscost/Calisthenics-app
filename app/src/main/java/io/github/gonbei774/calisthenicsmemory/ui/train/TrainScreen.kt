// U05 (F1–F6): the Train tab. One screen: goal, profile, rounds + style, focus, stretch, and one Preview button.
package io.github.gonbei774.calisthenicsmemory.ui.train

import io.github.gonbei774.calisthenicsmemory.ui.theme.AppAccentTheme

import io.github.gonbei774.calisthenicsmemory.ui.components.AppTextButton
import io.github.gonbei774.calisthenicsmemory.ui.components.AppOutlinedButton
import io.github.gonbei774.calisthenicsmemory.ui.components.appSegmentedColors

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.selection.toggleable
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import app.calisthenics.domain.goals.Goal
import app.calisthenics.domain.goals.Goals
import app.calisthenics.domain.model.BlockType
import app.calisthenics.domain.model.WorkoutFormat
import app.calisthenics.domain.model.StrengthFocus
import io.github.gonbei774.calisthenicsmemory.ui.components.Caption
import io.github.gonbei774.calisthenicsmemory.ui.components.ObjectivePicker
import io.github.gonbei774.calisthenicsmemory.ui.components.ProgramEntry
import app.calisthenics.domain.planner.*
import app.calisthenics.domain.routine.toggleFocus
import io.github.gonbei774.calisthenicsmemory.R
import io.github.gonbei774.calisthenicsmemory.session.CheckpointStore
import io.github.gonbei774.calisthenicsmemory.session.SessionBus
import io.github.gonbei774.calisthenicsmemory.ui.components.Stepper
import io.github.gonbei774.calisthenicsmemory.ui.screens.ProfileStore
import io.github.gonbei774.calisthenicsmemory.ui.screens.SavedRoutineStore
import io.github.gonbei774.calisthenicsmemory.ui.session.recoverWorkout
import io.github.gonbei774.calisthenicsmemory.ui.theme.Radius
import io.github.gonbei774.calisthenicsmemory.ui.theme.Spacing

/** Goals the user can pick, split like the plan says: skills first, body focus second. */
fun skillGoals(): List<Goal> = Goals.all.filter { it.entryVariationId != null }
fun bodyGoals(): List<Goal> = Goals.all.filter { it.entryVariationId == null }

private val focusOrder = listOf(StrengthFocus.FULL_BODY, StrengthFocus.UPPER_BODY, StrengthFocus.LOWER_BODY, StrengthFocus.CORE)
private fun focusLabel(f: StrengthFocus) = when (f) {
    StrengthFocus.FULL_BODY -> R.string.focus_full
    StrengthFocus.UPPER_BODY -> R.string.focus_upper
    StrengthFocus.LOWER_BODY -> R.string.focus_lower
    StrengthFocus.CORE -> R.string.focus_core
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrainScreen(
    modifier: Modifier = Modifier,
    onPreview: () -> Unit,
    onStarted: () -> Unit,
    onEditProfile: (String?) -> Unit,
) {
    val ctx = LocalContext.current
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    val initialSettings = remember { TrainSettingsStore.load(ctx) }
    val data by rememberTrainData()
    val profiles = remember { ProfileStore.load(ctx) }
    var settings by remember { mutableStateOf(initialSettings) }
    val currentKey = draftKey(settings)
    var draft by remember(currentKey) { mutableStateOf(PreviewDraftStore.get(ctx, currentKey)) }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(ctx, lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                val refreshed = TrainSettingsStore.load(ctx)
                settings = refreshed
                draft = PreviewDraftStore.get(ctx, draftKey(refreshed))
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    fun change(s: TrainSettings) {
        // R8.1-T05 (F6): the Preview's edits belong to the objective; another objective, routine or program starts clean
        if (draftKey(s) != draftKey(settings)) {
            PreviewDraftStore.clear(ctx)
            TrainSettingsStore.setModeRevision(ctx, draftKey(s), 0L)
            draft = null
        }
        else if (s.exercisesPerRound != settings.exercisesPerRound) {
            // A new count is a fresh automatic selection, not an extension of
            // today's manual swaps/additions. Keep the latest global mode.
            val revision = TrainSettingsStore.modeRevision(ctx, currentKey)
            val fresh = effectiveTrainSettings(s, draft, revision)
            draft = PreviewDraft(currentKey, betweenSetMode = fresh.effectiveBetweenSetMode(),
                modeRevision = maxOf(revision, draft?.modeRevision ?: 0L),
                preparationSeconds = fresh.preparationSeconds, ruleDefaultVersion = 1)
            PreviewDraftStore.put(ctx, draft!!)
            settings = fresh; TrainSettingsStore.save(ctx, fresh)
            return
        }
        settings = s; TrainSettingsStore.save(ctx, s)
    }
    val currentModeRevision = TrainSettingsStore.modeRevision(ctx, currentKey)
    val currentSettings = effectiveTrainSettings(settings, draft, currentModeRevision)
    val profile = profiles.firstOrNull { it.id == currentSettings.profileId } ?: profiles.first()
    val saved = remember(currentSettings.routineId, currentSettings.profileId) { routinesFor(ctx, data.catalog) }
    val selectedSaved = saved.firstOrNull { it.id == currentSettings.routineId }
    val resolved = remember(currentSettings, data, saved) { resolveTrain(ctx, data.routine, currentSettings, data.catalog) }
    // R8.1-T05/T08: what was changed in the Preview belongs to the objective and also applies when the workout is started from here
    val programMode = remember(currentSettings, data, saved, draft) { resolveProgramMode(ctx, data.catalog, currentSettings, draft) }
    val editsNow = effectivePlanEdits(draft?.edits ?: resolved.edits, currentModeRevision)
    // R7-T08 (D1): the default number of exercises per round (30 minutes at least) unless the person set one
    val eff = remember(resolved, data, profile, editsNow) { withDefaultLength(data.catalog, resolved.routine, data.progress, profile, resolved.settings, edits = editsNow) }
    val result: PlanResult = remember(eff, resolved, data, profile, editsNow, programMode) {
        if (programMode != null) when (val r = buildProgramPlan(data.catalog, programMode.content, profile, currentSettings.effectiveBetweenSetMode() == app.calisthenics.domain.model.BetweenSetMode.FLOW, currentSettings.rounds, currentSettings.format, betweenSetMode = currentSettings.effectiveBetweenSetMode(), preparationSeconds = currentSettings.preparationSeconds)) {
            is FixedProgramPlanResult.Ready -> PlanResult.Ready(r.plan)
            is FixedProgramPlanResult.Unavailable -> PlanResult.Infeasible(r.items.map { ConstraintFailure("unavailable", ctx.getString(R.string.programs_missing_item, data.catalog.variation(it.exerciseId)?.name ?: it.exerciseId, it.missing)) }, emptyList())
        } else buildTrainPlan(data.catalog, resolved.routine, data.progress, profile, eff, edits = editsNow)
    }
    val restDurations = selectedSaved?.restDurations.orEmpty() + draft?.rest.orEmpty()
    val plan = (result as? PlanResult.Ready)?.plan?.let { withRestDurations(it, restDurations) }
    val minutes = plan?.plannedDurationSeconds?.let { (it + 30) / 60 }

    // Doc 17 §2.1: choices only; START WORKOUT and Workout preview stay at the bottom.
    Scaffold(
        modifier = modifier,
        bottomBar = {
            Column {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Row(Modifier.fillMaxWidth().padding(horizontal = Spacing.l, vertical = Spacing.m), horizontalArrangement = Arrangement.spacedBy(Spacing.s), verticalAlignment = Alignment.CenterVertically) {
                    Button(
                        onClick = { haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress); plan?.let { startPlan(ctx, it); onStarted() } }, enabled = plan != null,
                        modifier = Modifier.weight(2f).heightIn(min = 56.dp).testTag("train_start_button"), shape = androidx.compose.foundation.shape.RoundedCornerShape(Radius.button),
                    ) { Text(stringResource(R.string.train_start).uppercase(), style = MaterialTheme.typography.titleLarge) }
                    AppOutlinedButton(onClick = onPreview, enabled = plan != null, modifier = Modifier.weight(1f).heightIn(min = 56.dp).testTag("preview_button")) {
                        Text(stringResource(R.string.train_preview_short), textAlign = TextAlign.Center)
                    }
                }
            }
        },
    ) { pad ->
        Column(
            Modifier.padding(pad).verticalScroll(rememberScrollState()).padding(horizontal = Spacing.l, vertical = Spacing.s),
            verticalArrangement = Arrangement.spacedBy(Spacing.m),
        ) {
            Column(Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.train_title).uppercase(), style = MaterialTheme.typography.headlineMedium, modifier = Modifier.testTag("train_title"))
            }
            UnfinishedBanner(onStarted)

            // Flow belongs to the objective: a program's start value must not leak into the next workout
            val catalogPrograms = remember(data.catalog, profile) { data.catalog.fixedPrograms.filter { it.listed }.map { p -> ProgramEntry(p.id, p.name, programSummaryText(ctx, data.catalog, p, profile)) } }
            ObjectivePicker(settings.goalId, { change(settings.copy(goalId = it, focus = null, routineId = null, fixedProgramId = null, exercisesPerRound = null, flow = false, betweenSetMode = null)) }, routines = saved, routineId = settings.routineId,
                onPickRoutine = { id -> saved.firstOrNull { it.id == id }?.let { change(app.calisthenics.domain.routine.settingsFor(it, settings)) } },
                fixedPrograms = catalogPrograms, programId = settings.fixedProgramId,
                onPickProgram = { id -> data.catalog.fixedPrograms.firstOrNull { it.id == id }?.let { change(app.calisthenics.domain.routine.settingsForProgram(it, settings)) } })

            // R7-T09 (D2): a skill brings in its own exercises; show which
            val skillGoal = Goals.byId(settings.goalId)?.takeIf { it.entryVariationId != null && settings.routineId == null }
            if (skillGoal != null && plan != null) {
                val names = plan.blocks.filter { it.type == BlockType.WORK && it.roundIndex == 1 && it.slotId != null && Goals.isSkillSlot(it.slotId!!) }.mapNotNull { data.catalog.variation(it.variationId ?: "")?.name }.distinct()
                if (names.isNotEmpty()) Text(stringResource(R.string.train_skill_exercises, skillGoal.name, names.joinToString(", ")), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.testTag("skill_exercises"))
            }

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Caption(stringResource(R.string.train_equipment_caption))
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    var open by remember { mutableStateOf(false) }
                    val profileDescription = stringResource(R.string.field_description, stringResource(R.string.train_equipment_caption), profile.name)
                    ExposedDropdownMenuBox(expanded = open, onExpandedChange = { open = it }, modifier = Modifier.weight(1f)) {
                        OutlinedTextField(
                            value = profile.name, onValueChange = {}, readOnly = true, singleLine = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(open) },
                            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth().semantics { contentDescription = profileDescription }.testTag("profile_field"),
                        )
                        ExposedDropdownMenu(expanded = open, onDismissRequest = { open = false }) {
                            profiles.forEach { p -> DropdownMenuItem(text = { Text(p.name) }, onClick = { change(settings.copy(profileId = p.id)); open = false }, modifier = Modifier.testTag("profile_chip_${p.id}")) }
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.train_new_profile)) }, leadingIcon = { Icon(Icons.Filled.Add, null) },
                                onClick = { open = false; onEditProfile(null) }, modifier = Modifier.testTag("profile_plus"),
                            )
                        }
                    }
                    IconButton(onClick = { onEditProfile(profile.id) }, modifier = Modifier.testTag("profile_edit")) {
                        Icon(Icons.Filled.Edit, stringResource(R.string.train_edit_profile, profile.name))
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Caption(stringResource(R.string.train_format_caption))
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    WorkoutFormat.entries.forEachIndexed { i, f ->
                        SegmentedButton(selected = settings.format == f, onClick = { change(settings.copy(format = f)) }, colors = appSegmentedColors(), shape = SegmentedButtonDefaults.itemShape(i, WorkoutFormat.entries.size), icon = {}, modifier = Modifier.testTag("format_${f.name}")) {
                            Text(stringResource(when (f) { WorkoutFormat.CIRCUIT -> R.string.format_circuit; WorkoutFormat.PAIRS -> R.string.format_pairs; WorkoutFormat.STRAIGHT -> R.string.format_straight }), maxLines = 1)
                        }
                    }
                }
            }

            // V27b: with a large font the two cards stack, so "Stretch" and "Rest" are not cut
            val stacked = androidx.compose.ui.platform.LocalDensity.current.fontScale > 1.15f
            val setsCard: @Composable (Modifier) -> Unit = { m ->
                Card(m, shape = MaterialTheme.shapes.large) {
                    Column(Modifier.padding(Spacing.m).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
                        Caption(stringResource(if (settings.format == WorkoutFormat.CIRCUIT) R.string.train_rounds_caption else R.string.train_sets_caption))
                        Stepper(settings.rounds, { change(settings.copy(rounds = it)) }, MIN_TRAIN_ROUNDS..MAX_EXPLICIT_ROUNDS, tag = "rounds")
                    }
                }
            }
            val selectedMode = currentSettings.effectiveBetweenSetMode()
            fun chooseBetween(mode: app.calisthenics.domain.model.BetweenSetMode) {
                // Read at the explicit event boundary: a cached UI callback must never restore
                // the objective/configuration from an earlier composition.
                val latestSettings = TrainSettingsStore.load(ctx)
                val latestKey = draftKey(latestSettings)
                val latestSaved = routinesFor(ctx, data.catalog).firstOrNull { it.id == latestSettings.routineId }
                val latestDraft = PreviewDraftStore.get(ctx, latestKey)
                val latestResolved = resolveTrain(ctx, data.routine, latestSettings, data.catalog)
                val latestProgram = resolveProgramMode(ctx, data.catalog, latestSettings, latestDraft)
                val startingDraft = latestDraft ?: PreviewDraft(latestKey, edits = latestResolved.edits,
                    rest = latestSaved?.restDurations.orEmpty(), program = latestProgram?.content, ruleDefaultVersion = 1)
                val (updatedSettings, updatedDraft) = updateBetweenSetMode(ctx, latestSettings, startingDraft, mode)
                settings = updatedSettings
                draft = updatedDraft
            }
            val betweenCard: @Composable (Modifier) -> Unit = { m ->
                Card(m, shape = MaterialTheme.shapes.large) {
                    Column(Modifier.padding(Spacing.m).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
                        Caption(stringResource(R.string.train_between_caption))
                        BetweenSetSelector(selectedMode, ::chooseBetween)
                    }
                }
            }
            // R7-T08 (D1): exercises per round, next to the rounds; the default makes the session at least 30 minutes, a tap overrides it
            val exercisesCard: @Composable (Modifier) -> Unit = { m ->
                val maxEx = remember(resolved, data, profile) { val r = routineForPlan(data.catalog, resolved.routine, data.progress, profile, resolved.settings.copy(exercisesPerRound = null)); maxExercisesPerRound(data.catalog, r, fillFocusOf(r, resolved.settings, data.catalog), profile, data.progress, resolved.settings.effectiveBetweenSetMode() == app.calisthenics.domain.model.BetweenSetMode.STRETCH) }
                val shown = (eff.exercisesPerRound ?: plan?.let { exercisesPerRoundOf(it) } ?: 1).coerceIn(1, maxOf(maxEx, 1))
                Card(m, shape = MaterialTheme.shapes.large) {
                    Column(Modifier.padding(Spacing.m).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
                        Caption(stringResource(R.string.train_exercises_stepper_caption))
                        Stepper(shown, { change(settings.copy(exercisesPerRound = it)) }, 1..maxOf(maxEx, shown), tag = "exercises")
                    }
                }
            }
            if (programMode != null) { setsCard(Modifier.fillMaxWidth()); betweenCard(Modifier.fillMaxWidth()) }   // program-authored mode is the editable starting choice
            else if (stacked) { setsCard(Modifier.fillMaxWidth()); exercisesCard(Modifier.fillMaxWidth()); betweenCard(Modifier.fillMaxWidth()) }
            else {
                Row(Modifier.fillMaxWidth().height(IntrinsicSize.Max), horizontalArrangement = Arrangement.spacedBy(Spacing.m)) {
                    setsCard(Modifier.weight(1f).fillMaxHeight()); exercisesCard(Modifier.weight(1f).fillMaxHeight())
                }
                betweenCard(Modifier.fillMaxWidth())
            }

            if (programMode == null) {
            // R8.2-T03 (G3): one horizontal Include row: Warm-up, Conditioning, Cool-down (gold = in the workout)
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Caption(stringResource(R.string.train_include_caption))
                io.github.gonbei774.calisthenicsmemory.ui.components.IncludeRow(listOf(
                    io.github.gonbei774.calisthenicsmemory.ui.components.IncludeOption(stringResource(R.string.train_warmup), settings.warmupOn, "warmup_switch") { change(settings.copy(warmupOn = it)) },
                    io.github.gonbei774.calisthenicsmemory.ui.components.IncludeOption(stringResource(R.string.train_conditioning), settings.conditioningOn, "conditioning_switch") { change(settings.copy(conditioningOn = it)) },
                    io.github.gonbei774.calisthenicsmemory.ui.components.IncludeOption(stringResource(R.string.train_cooldown), settings.cooldownOn, "cooldown_switch") { change(settings.copy(cooldownOn = it)) },
                ))
            }
            if (settings.cooldownOn) SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                app.calisthenics.domain.model.CooldownMode.entries.forEachIndexed { i, m ->
                    SegmentedButton(selected = settings.cooldownMode == m, onClick = { change(settings.copy(cooldownMode = m)) }, colors = appSegmentedColors(), shape = SegmentedButtonDefaults.itemShape(i, 2), icon = {}, modifier = Modifier.testTag("cooldown_mode_${m.name}")) {
                        Text(stringResource(if (m == app.calisthenics.domain.model.CooldownMode.STRETCHING) R.string.cooldown_stretching else R.string.cooldown_timed), maxLines = 1)
                    }
                }
            }

            }

            // R8.2-T04 (G4): only the time; the exercises-per-round count lives in its stepper above
            SummaryTile(R.string.train_time_caption, minutes?.let { stringResource(R.string.train_minutes, it) } ?: stringResource(R.string.train_minutes_unknown), null, "minutes", Modifier.fillMaxWidth())

            if (result is PlanResult.Infeasible) {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer), modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(Spacing.m), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                        Text(stringResource(R.string.train_infeasible), style = MaterialTheme.typography.titleSmall)
                        result.reasons.forEach { Text(it.message, style = MaterialTheme.typography.bodyMedium) }
                        result.alternatives.forEach { Text(stringResource(R.string.bullet_item, it.description), style = MaterialTheme.typography.bodySmall) }
                    }
                }
            }
        }
    }
}

@Composable
private fun SummaryTile(caption: Int, value: String, sub: String?, tag: String, modifier: Modifier) {
    Card(modifier.fillMaxHeight(), shape = MaterialTheme.shapes.large) {
        Column(Modifier.padding(Spacing.m).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Caption(stringResource(caption))
            Text(value, style = MaterialTheme.typography.headlineMedium, modifier = Modifier.testTag(tag))
            if (sub != null) Text(sub, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
        }
    }
}

/** A live workout, or one the app lost (crash/kill) with a choice to continue or save what was done. */
@Composable
private fun UnfinishedBanner(onOpen: () -> Unit) {
    val ctx = LocalContext.current
    val live by SessionBus.state.collectAsState()
    val unfinished = remember { CheckpointStore.read(ctx) }
    val running = live?.takeIf { !it.isTerminal }
    if (running != null) {
        Card(Modifier.fillMaxWidth().testTag("banner_running"), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
            Row(Modifier.padding(Spacing.m), verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.train_running), Modifier.weight(1f))
                Button(onClick = onOpen) { Text(stringResource(R.string.train_open)) }
            }
        }
    } else if (unfinished != null) {
        Card(Modifier.fillMaxWidth().testTag("banner_unfinished"), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)) {
            Column(Modifier.padding(Spacing.m), verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
                Text(stringResource(R.string.train_unfinished))
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s)) {
                    Button(onClick = { recoverWorkout(ctx, true); onOpen() }) { Text(stringResource(R.string.train_continue)) }
                    AppOutlinedButton(onClick = { recoverWorkout(ctx, false); onOpen() }) { Text(stringResource(R.string.train_save_end)) }
                }
            }
        }
    }
}
