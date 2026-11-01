// U12 (§3.9): Settings. Profiles, workout defaults, voice cues, appearance, the starting questionnaire, backup, privacy, licences, about.
package io.github.gonbei774.calisthenicsmemory.ui.screens

import org.jetbrains.compose.resources.StringResource
import io.github.gonbei774.calisthenicsmemory.platform.appVersionName
import io.github.gonbei774.calisthenicsmemory.platform.currentPlatformContext
import io.github.gonbei774.calisthenicsmemory.platform.readPrivacyText
import io.github.gonbei774.calisthenicsmemory.ui.theme.AppAccentTheme

import io.github.gonbei774.calisthenicsmemory.ui.components.AppTextButton
import io.github.gonbei774.calisthenicsmemory.ui.components.appSegmentedColors

import androidx.compose.foundation.clickable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.unit.dp
import app.calisthenics.domain.planner.MAX_EXPLICIT_ROUNDS
import app.calisthenics.domain.planner.MIN_TRAIN_ROUNDS
import io.github.gonbei774.calisthenicsmemory.shared.resources.*
import app.calisthenics.data.settings.AppTheme
import io.github.gonbei774.calisthenicsmemory.ui.components.Stepper
import io.github.gonbei774.calisthenicsmemory.ui.theme.Spacing
import io.github.gonbei774.calisthenicsmemory.ui.train.TrainSettingsStore

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier, onLevels: () -> Unit, onBackup: () -> Unit, onLicenses: () -> Unit, onProfiles: () -> Unit = {},
    onPrivacy: () -> Unit = {}, theme: AppTheme = AppTheme.SYSTEM, onTheme: (AppTheme) -> Unit = {},
) {
    val ctx = currentPlatformContext()
    var confirmRedo by remember { mutableStateOf(false) }
    var defaults by remember { mutableStateOf(TrainSettingsStore.load(ctx)) }
    var voice by remember { mutableStateOf(PrefsStore.load(ctx).audioEnabled) }
    var appVolume by remember { mutableIntStateOf(PrefsStore.load(ctx).appVolumePercent.coerceIn(0, 100)) }
    var autoProg by remember { mutableStateOf(PrefsStore.load(ctx).autoProgression) }
    val version = remember { runCatching { ctx.appVersionName() }.getOrNull().orEmpty() }
    fun saveDefaults(s: app.calisthenics.domain.planner.TrainSettings) { defaults = s; TrainSettingsStore.save(ctx, s) }

    Scaffold(modifier = modifier, topBar = { TopAppBar(title = { Text(stringResource(Res.string.tab_settings)) }) }) { pad ->
        Column(Modifier.padding(pad).verticalScroll(rememberScrollState()).padding(Spacing.l), verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
            Section(Res.string.settings_section_training)
            NavRow(Res.string.settings_profiles, Res.string.settings_profiles_hint, "settings_profiles", onProfiles)
            Card(Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.large) {
                Column(Modifier.padding(Spacing.l), verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
                    Text(stringResource(Res.string.settings_defaults), style = MaterialTheme.typography.titleMedium)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(Res.string.onb_default_rounds), Modifier.weight(1f))
                        Stepper(defaults.rounds, { saveDefaults(defaults.copy(rounds = it)) }, MIN_TRAIN_ROUNDS..MAX_EXPLICIT_ROUNDS, tag = "set_rounds")
                    }
                    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                        SegmentedButton(selected = !defaults.timed, onClick = { saveDefaults(defaults.copy(timed = false)) }, colors = appSegmentedColors(), shape = SegmentedButtonDefaults.itemShape(0, 2), icon = {}, modifier = Modifier.testTag("set_reps")) { Text(stringResource(Res.string.style_reps)) }
                        SegmentedButton(selected = defaults.timed, onClick = { saveDefaults(defaults.copy(timed = true)) }, colors = appSegmentedColors(), shape = SegmentedButtonDefaults.itemShape(1, 2), icon = {}, modifier = Modifier.testTag("set_timed")) { Text(stringResource(Res.string.style_timed)) }
                    }
                    Text(stringResource(Res.string.train_between_caption), style = MaterialTheme.typography.bodyLarge)
                    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().testTag("set_between_sets")) {
                        app.calisthenics.domain.model.BetweenSetMode.entries.forEachIndexed { index, mode ->
                            SegmentedButton(
                                selected = defaults.effectiveBetweenSetMode() == mode,
                                onClick = { saveDefaults(defaults.copy(betweenSetMode = mode, flow = mode == app.calisthenics.domain.model.BetweenSetMode.FLOW, stretchOn = mode == app.calisthenics.domain.model.BetweenSetMode.STRETCH)) },
                                colors = appSegmentedColors(), shape = SegmentedButtonDefaults.itemShape(index, 3), icon = {},
                                modifier = Modifier.testTag("set_between_${mode.name.lowercase()}"),
                            ) { Text(mode.name.lowercase().replaceFirstChar { it.uppercase() }) }
                        }
                    }
                    SwitchRow(Res.string.settings_voice, voice, "set_voice") { voice = it; PrefsStore.save(ctx, PrefsStore.load(ctx).copy(audioEnabled = it)) }
                    Column(Modifier.fillMaxWidth().testTag("set_app_volume")) {
                        Text(stringResource(Res.string.feedback_app_volume, appVolume), style = MaterialTheme.typography.bodyLarge)
                        Slider(
                            value = appVolume.toFloat(),
                            onValueChange = { appVolume = it.toInt().coerceIn(0, 100) },
                            onValueChangeFinished = { PrefsStore.save(ctx, PrefsStore.load(ctx).copy(appVolumePercent = appVolume)) },
                            valueRange = 0f..100f,
                            steps = 99,
                        )
                        Text(stringResource(Res.string.feedback_app_volume_hint), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            Section(Res.string.settings_section_progression)
            SwitchRow(Res.string.settings_auto_progress, autoProg, "set_auto_progress") { autoProg = it; PrefsStore.save(ctx, PrefsStore.load(ctx).copy(autoProgression = it)) }
            Text(stringResource(if (autoProg) Res.string.settings_auto_progress_on else Res.string.settings_auto_progress_off), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.testTag("auto_progress_hint"))
            NavRow(Res.string.settings_levels, Res.string.settings_levels_hint, "settings_redo") { confirmRedo = true }

            Section(Res.string.settings_section_appearance)
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                listOf(AppTheme.SYSTEM to Res.string.theme_system, AppTheme.LIGHT to Res.string.theme_light, AppTheme.DARK to Res.string.theme_dark).forEachIndexed { i, (t, label) ->
                    SegmentedButton(selected = theme == t, onClick = { onTheme(t) }, colors = appSegmentedColors(), shape = SegmentedButtonDefaults.itemShape(i, 3), icon = {}, modifier = Modifier.testTag("theme_${t.code}")) { Text(stringResource(label)) }
                }
            }

            Section(Res.string.settings_section_data)
            NavRow(Res.string.settings_backup, Res.string.settings_backup_hint, "settings_backup", onBackup)

            Section(Res.string.settings_section_about)
            NavRow(Res.string.settings_privacy, null, "settings_privacy", onPrivacy)
            NavRow(Res.string.settings_licenses, null, "settings_licenses", onLicenses)
            Text(stringResource(Res.string.settings_about, stringResource(Res.string.app_name), version), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = Spacing.s).testTag("settings_about"))
        }
    }
    if (confirmRedo) AlertDialog(
        onDismissRequest = { confirmRedo = false },
        title = { Text(stringResource(Res.string.settings_redo_title)) },
        text = { Text(stringResource(Res.string.settings_redo_text)) },
        confirmButton = { AppTextButton(onClick = { confirmRedo = false; onLevels() }, modifier = Modifier.testTag("settings_redo_confirm")) { Text(stringResource(Res.string.settings_redo_confirm)) } },
        dismissButton = { AppTextButton(onClick = { confirmRedo = false }) { Text(stringResource(Res.string.cancel)) } },
    )
}

@Composable private fun Section(title: StringResource) {
    Text(stringResource(title), style = MaterialTheme.typography.labelLarge, color = AppAccentTheme.colors.text, modifier = Modifier.padding(top = Spacing.m))
}

@Composable private fun NavRow(title: StringResource, hint: StringResource?, tag: String, onClick: () -> Unit) {
    Card(Modifier.fillMaxWidth().clickable(onClick = onClick).testTag(tag), shape = MaterialTheme.shapes.large) {
        Column(Modifier.padding(Spacing.l).heightIn(min = 24.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(stringResource(title), style = MaterialTheme.typography.titleMedium)
            if (hint != null) Text(stringResource(hint), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable private fun SwitchRow(label: StringResource, checked: Boolean, tag: String, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().heightIn(min = Spacing.touch).toggleable(checked, role = Role.Switch, onValueChange = onChange).testTag(tag), verticalAlignment = Alignment.CenterVertically) {
        Text(stringResource(label), Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
        Switch(checked = checked, onCheckedChange = null)
    }
}

/** The privacy policy shipped with the app (assets/privacy.md, the same text as PRIVACY.md). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyScreen(modifier: Modifier = Modifier, onBack: () -> Unit) {
    val ctx = currentPlatformContext()
    val text = remember { runCatching { ctx.readPrivacyText() }.getOrDefault("") }
    val lines = remember(text) { text.lines().map { it.removePrefix("# ").removePrefix("- ").replace("**", "") }.filter { it.isNotBlank() } }
    Scaffold(modifier = modifier, topBar = {
        TopAppBar(title = { Text(stringResource(Res.string.settings_privacy)) }, navigationIcon = { AppTextButton(onClick = onBack) { Text(stringResource(Res.string.back)) } })
    }) { pad ->
        Column(Modifier.padding(pad).verticalScroll(rememberScrollState()).padding(Spacing.l).testTag("privacy_text"), verticalArrangement = Arrangement.spacedBy(Spacing.m)) {
            lines.forEachIndexed { i, l -> Text(l, style = if (i == 0) MaterialTheme.typography.titleLarge else MaterialTheme.typography.bodyLarge) }
        }
    }
}
