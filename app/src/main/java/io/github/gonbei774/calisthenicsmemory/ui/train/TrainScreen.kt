// U05 (F1–F6): the Train tab. One screen: goal, profile, rounds + style, focus, stretch, and one Preview button.
package io.github.gonbei774.calisthenicsmemory.ui.train

import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.ui.unit.dp
import app.calisthenics.domain.goals.Goal
import app.calisthenics.domain.goals.Goals
import app.calisthenics.domain.model.StrengthFocus
import app.calisthenics.domain.planner.*
import app.calisthenics.domain.routine.toggleFocus
import io.github.gonbei774.calisthenicsmemory.R
import io.github.gonbei774.calisthenicsmemory.session.CheckpointStore
import io.github.gonbei774.calisthenicsmemory.session.SessionBus
import io.github.gonbei774.calisthenicsmemory.ui.components.Stepper
import io.github.gonbei774.calisthenicsmemory.ui.screens.ProfileStore
import io.github.gonbei774.calisthenicsmemory.ui.screens.recoverWorkout
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
    val data by rememberTrainData()
    val profiles = remember { ProfileStore.load(ctx) }
    var settings by remember { mutableStateOf(TrainSettingsStore.load(ctx)) }
    fun change(s: TrainSettings) { settings = s; TrainSettingsStore.save(ctx, s) }
    val profile = profiles.firstOrNull { it.id == settings.profileId } ?: profiles.first()
    val result = remember(settings, data, profile) { buildTrainPlan(data.catalog, data.routine, data.progress, profile, settings) }
    val minutes = (result as? PlanResult.Ready)?.plan?.plannedDurationSeconds?.let { (it + 30) / 60 }

    Scaffold(modifier = modifier, topBar = { TopAppBar(title = { Text(stringResource(R.string.tab_train)) }) }) { pad ->
        Column(
            Modifier.padding(pad).verticalScroll(rememberScrollState()).padding(horizontal = Spacing.l, vertical = Spacing.s),
            verticalArrangement = Arrangement.spacedBy(Spacing.m),
        ) {
            UnfinishedBanner(onStarted)

            GoalDropdown(settings.goalId) { change(settings.copy(goalId = it, focus = null)) }

            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Row(Modifier.weight(1f).horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(Spacing.s), verticalAlignment = Alignment.CenterVertically) {
                    profiles.forEach { p ->
                        FilterChip(selected = p.id == profile.id, onClick = { change(settings.copy(profileId = p.id)) }, label = { Text(p.name) }, modifier = Modifier.testTag("profile_chip_${p.id}"))
                    }
                    AssistChip(onClick = { onEditProfile(null) }, label = { Icon(Icons.Filled.Add, stringResource(R.string.profile_add), Modifier.size(18.dp)) }, modifier = Modifier.testTag("profile_plus"))
                }
                IconButton(onClick = { onEditProfile(profile.id) }, modifier = Modifier.testTag("profile_edit")) {
                    Icon(Icons.Filled.Edit, stringResource(R.string.train_edit_profile, profile.name))
                }
            }

            Row(Modifier.fillMaxWidth().height(IntrinsicSize.Max), horizontalArrangement = Arrangement.spacedBy(Spacing.m)) {
                Card(Modifier.weight(1f).fillMaxHeight(), shape = MaterialTheme.shapes.large) {
                    Column(Modifier.padding(Spacing.m).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                        Text(stringResource(R.string.train_rounds), style = MaterialTheme.typography.labelLarge)
                        Stepper(settings.rounds, { change(settings.copy(rounds = it)) }, MIN_TRAIN_ROUNDS..MAX_EXPLICIT_ROUNDS, tag = "rounds")
                        Text(
                            minutes?.let { stringResource(R.string.train_minutes, it) } ?: stringResource(R.string.train_minutes_unknown),
                            style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center, modifier = Modifier.testTag("minutes"),
                        )
                    }
                }
                Card(Modifier.weight(1f).fillMaxHeight(), shape = MaterialTheme.shapes.large) {
                    Column(Modifier.padding(Spacing.m).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
                        Text(stringResource(R.string.train_style), style = MaterialTheme.typography.labelLarge)
                        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                            SegmentedButton(selected = !settings.timed, onClick = { change(settings.copy(timed = false)) }, shape = SegmentedButtonDefaults.itemShape(0, 2), icon = {}, modifier = Modifier.testTag("style_reps")) { Text(stringResource(R.string.style_reps)) }
                            SegmentedButton(selected = settings.timed, onClick = { change(settings.copy(timed = true)) }, shape = SegmentedButtonDefaults.itemShape(1, 2), icon = {}, modifier = Modifier.testTag("style_timed")) { Text(stringResource(R.string.style_timed)) }
                        }
                        Text(stringResource(if (settings.timed) R.string.style_timed_hint else R.string.style_reps_hint), style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
                    }
                }
            }

            val focus = settings.effectiveFocus()
            MultiChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                focusOrder.forEachIndexed { i, f ->
                    SegmentedButton(
                        checked = f in focus, onCheckedChange = { change(settings.copy(focus = toggleFocus(focus, f))) },
                        shape = SegmentedButtonDefaults.itemShape(i, focusOrder.size), icon = {}, modifier = Modifier.testTag("focus_${f.name}"),
                    ) { Text(stringResource(focusLabel(f))) }
                }
            }

            Row(Modifier.fillMaxWidth().heightIn(min = Spacing.touch), verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.train_stretch), Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                Switch(checked = settings.stretchOn, onCheckedChange = { change(settings.copy(stretchOn = it)) }, modifier = Modifier.testTag("stretch_switch"))
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

            Button(
                onClick = onPreview, enabled = result is PlanResult.Ready,
                modifier = Modifier.fillMaxWidth().height(56.dp).testTag("preview_button"), shape = androidx.compose.foundation.shape.RoundedCornerShape(Radius.button),
            ) {
                Text(stringResource(R.string.train_preview))
                Spacer(Modifier.width(Spacing.s))
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GoalDropdown(goalId: String, onPick: (String) -> Unit) {
    var open by remember { mutableStateOf(false) }
    val goal = Goals.byId(goalId) ?: Goals.all.first()
    ExposedDropdownMenuBox(expanded = open, onExpandedChange = { open = it }) {
        OutlinedTextField(
            value = goal.name, onValueChange = {}, readOnly = true, singleLine = true,
            label = { Text(stringResource(R.string.train_goal)) },
            supportingText = { Text(goal.description, maxLines = 1) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(open) },
            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth().testTag("goal_field"),
        )
        ExposedDropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            @Composable fun group(title: Int, goals: List<Goal>) {
                DropdownMenuItem(text = { Text(stringResource(title), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary) }, onClick = {}, enabled = false)
                goals.forEach { g ->
                    DropdownMenuItem(text = { Text(g.name) }, onClick = { onPick(g.id); open = false }, modifier = Modifier.testTag("goal_${g.id}"))
                }
            }
            group(R.string.goal_group_skills, skillGoals())
            group(R.string.goal_group_body, bodyGoals())
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
                    OutlinedButton(onClick = { recoverWorkout(ctx, false); onOpen() }) { Text(stringResource(R.string.train_save_end)) }
                }
            }
        }
    }
}
