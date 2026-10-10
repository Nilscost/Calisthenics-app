// U05 (F1–F6): the Train tab. One screen: goal, profile, rounds + style, focus, stretch, and one Preview button.
package io.github.gonbei774.calisthenicsmemory.ui.train

import io.github.gonbei774.calisthenicsmemory.ui.theme.AppAccentTheme

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
    val data by rememberTrainData()
    val profiles = remember { ProfileStore.load(ctx) }
    var settings by remember { mutableStateOf(TrainSettingsStore.load(ctx)) }
    fun change(s: TrainSettings) { settings = s; TrainSettingsStore.save(ctx, s) }
    val profile = profiles.firstOrNull { it.id == settings.profileId } ?: profiles.first()
    val saved = remember(settings.routineId, settings.profileId) { routinesFor(ctx, data.catalog) }
    val resolved = remember(settings, data, saved) { resolveTrain(ctx, data.routine, settings, data.catalog) }
    val result = remember(resolved, data, profile) { buildTrainPlan(data.catalog, resolved.routine, data.progress, profile, resolved.settings, edits = resolved.edits) }
    val plan = (result as? PlanResult.Ready)?.plan
    val minutes = plan?.plannedDurationSeconds?.let { (it + 30) / 60 }
    val exercises = plan?.blocks?.filter { it.type == BlockType.WORK }?.mapNotNull { it.variationId }?.distinct()?.size

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
            Text(stringResource(R.string.train_title).uppercase(), style = MaterialTheme.typography.headlineMedium, modifier = Modifier.testTag("train_title"))
            UnfinishedBanner(onStarted)

            ObjectivePicker(settings.goalId, { change(settings.copy(goalId = it, focus = null, routineId = null)) }, routines = saved, routineId = settings.routineId, onPickRoutine = { id -> saved.firstOrNull { it.id == id }?.let { change(app.calisthenics.domain.routine.settingsFor(it, settings)) } })

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
            val betweenCard: @Composable (Modifier) -> Unit = { m ->
                Card(m, shape = MaterialTheme.shapes.large) {
                    Column(Modifier.padding(Spacing.m).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
                        Caption(stringResource(R.string.train_between_caption))
                        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                            SegmentedButton(selected = settings.stretchOn, onClick = { change(settings.copy(stretchOn = true)) }, colors = appSegmentedColors(), shape = SegmentedButtonDefaults.itemShape(0, 2), icon = {}, modifier = Modifier.testTag("between_stretch")) { Text(stringResource(R.string.between_stretch), maxLines = 1) }
                            SegmentedButton(selected = !settings.stretchOn, onClick = { change(settings.copy(stretchOn = false)) }, colors = appSegmentedColors(), shape = SegmentedButtonDefaults.itemShape(1, 2), icon = {}, modifier = Modifier.testTag("between_rest")) { Text(stringResource(R.string.between_rest), maxLines = 1) }
                        }
                    }
                }
            }
            if (stacked) { setsCard(Modifier.fillMaxWidth()); betweenCard(Modifier.fillMaxWidth()) }
            else Row(Modifier.fillMaxWidth().height(IntrinsicSize.Max), horizontalArrangement = Arrangement.spacedBy(Spacing.m)) {
                setsCard(Modifier.weight(1f).fillMaxHeight()); betweenCard(Modifier.weight(1f).fillMaxHeight())
            }

            val warmupLabel = stringResource(R.string.train_warmup)
            // V23: a warm-up before any workout (the routine's own list when it has one, else the catalog's)
            Row(Modifier.fillMaxWidth().heightIn(min = Spacing.touch), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.train_warmup), style = MaterialTheme.typography.bodyLarge)
                    Text(stringResource(R.string.train_warmup_hint), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(checked = settings.warmupOn, onCheckedChange = { change(settings.copy(warmupOn = it)) }, modifier = Modifier.semantics { contentDescription = warmupLabel }.testTag("warmup_switch"))
            }

            Row(Modifier.fillMaxWidth().height(IntrinsicSize.Max), horizontalArrangement = Arrangement.spacedBy(Spacing.m)) {
                SummaryTile(R.string.train_time_caption, minutes?.let { stringResource(R.string.train_minutes, it) } ?: stringResource(R.string.train_minutes_unknown), null, "minutes", Modifier.weight(1f))
                SummaryTile(R.string.train_exercises_caption, exercises?.toString() ?: stringResource(R.string.train_minutes_unknown), stringResource(R.string.train_exercises_per_round), "exercise_count", Modifier.weight(1f))
            }

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
