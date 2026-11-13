// Settings > Your data > Routines from a chatbot: export the guide a chatbot reads, import the routine it writes (a file or pasted text).
// The parsing, checking and saving are domain code (RoutineExchange); this screen only moves text in and out.
package io.github.gonbei774.calisthenicsmemory.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import app.calisthenics.domain.model.Catalog
import app.calisthenics.domain.model.SideMode
import app.calisthenics.domain.routine.ExchangeExercise
import app.calisthenics.domain.routine.ExchangeRoutine
import app.calisthenics.domain.routine.GUIDE_FILE_NAME
import app.calisthenics.domain.routine.RoutineImportResult
import app.calisthenics.domain.routine.exportGuide
import app.calisthenics.domain.routine.parseRoutines
import app.calisthenics.domain.routine.savedFromExchange
import io.github.gonbei774.calisthenicsmemory.platform.currentPlatformContext
import io.github.gonbei774.calisthenicsmemory.platform.newUuid
import io.github.gonbei774.calisthenicsmemory.platform.nowEpochMs
import io.github.gonbei774.calisthenicsmemory.platform.rememberBackupFiles
import io.github.gonbei774.calisthenicsmemory.shared.resources.Res
import io.github.gonbei774.calisthenicsmemory.shared.resources.back
import io.github.gonbei774.calisthenicsmemory.shared.resources.chatbot_check
import io.github.gonbei774.calisthenicsmemory.shared.resources.chatbot_choose_file
import io.github.gonbei774.calisthenicsmemory.shared.resources.chatbot_empty_file
import io.github.gonbei774.calisthenicsmemory.shared.resources.chatbot_errors
import io.github.gonbei774.calisthenicsmemory.shared.resources.chatbot_errors_hint
import io.github.gonbei774.calisthenicsmemory.shared.resources.chatbot_export_button
import io.github.gonbei774.calisthenicsmemory.shared.resources.chatbot_export_caption
import io.github.gonbei774.calisthenicsmemory.shared.resources.chatbot_export_failed
import io.github.gonbei774.calisthenicsmemory.shared.resources.chatbot_export_title
import io.github.gonbei774.calisthenicsmemory.shared.resources.chatbot_exported
import io.github.gonbei774.calisthenicsmemory.shared.resources.chatbot_import_caption
import io.github.gonbei774.calisthenicsmemory.shared.resources.chatbot_import_title
import io.github.gonbei774.calisthenicsmemory.shared.resources.chatbot_level_current
import io.github.gonbei774.calisthenicsmemory.shared.resources.chatbot_paste_label
import io.github.gonbei774.calisthenicsmemory.shared.resources.chatbot_reps
import io.github.gonbei774.calisthenicsmemory.shared.resources.chatbot_reps_side
import io.github.gonbei774.calisthenicsmemory.shared.resources.chatbot_save
import io.github.gonbei774.calisthenicsmemory.shared.resources.chatbot_saved
import io.github.gonbei774.calisthenicsmemory.shared.resources.chatbot_seconds
import io.github.gonbei774.calisthenicsmemory.shared.resources.chatbot_seconds_side
import io.github.gonbei774.calisthenicsmemory.shared.resources.chatbot_summary
import io.github.gonbei774.calisthenicsmemory.shared.resources.chatbot_warnings
import io.github.gonbei774.calisthenicsmemory.shared.resources.settings_chatbot
import io.github.gonbei774.calisthenicsmemory.ui.components.AppOutlinedButton
import io.github.gonbei774.calisthenicsmemory.ui.components.AppTextButton
import io.github.gonbei774.calisthenicsmemory.ui.theme.AppAccentTheme
import io.github.gonbei774.calisthenicsmemory.ui.theme.Spacing
import io.github.gonbei774.calisthenicsmemory.ui.train.TrainSettingsStore
import io.github.gonbei774.calisthenicsmemory.ui.train.loadCatalog
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatbotRoutinesScreen(modifier: Modifier = Modifier, onBack: () -> Unit) {
    val ctx = currentPlatformContext()
    val scope = rememberCoroutineScope()
    val files = rememberBackupFiles()
    val catalog = remember { loadCatalog(ctx) }
    var pasted by remember { mutableStateOf("") }
    var result by remember { mutableStateOf<RoutineImportResult?>(null) }
    var status by remember { mutableStateOf("") }

    fun check(text: String) { status = ""; result = parseRoutines(text, catalog, ProfileStore.selected(ctx)) }
    fun exportGuideFile() = files.export(GUIDE_FILE_NAME) { store ->
        status = try { store(exportGuide(catalog)); getString(Res.string.chatbot_exported) } catch (e: Exception) { getString(Res.string.chatbot_export_failed, e.message.orEmpty()) }
    }
    fun importFile() = files.import { text ->
        if (text.isBlank()) { result = null; scope.launch { status = getString(Res.string.chatbot_empty_file) } } else check(text)
    }

    Scaffold(modifier = modifier, topBar = {
        TopAppBar(title = { Text(stringResource(Res.string.settings_chatbot)) }, navigationIcon = { AppTextButton(onClick = onBack) { Text(stringResource(Res.string.back)) } })
    }) { pad ->
        Column(Modifier.padding(pad).verticalScroll(rememberScrollState()).padding(Spacing.l), verticalArrangement = Arrangement.spacedBy(Spacing.m)) {
            ActionCard(Res.string.chatbot_export_title, Res.string.chatbot_export_caption) {
                Button(onClick = { exportGuideFile() }, modifier = Modifier.testTag("chatbot_export")) { Text(stringResource(Res.string.chatbot_export_button)) }
            }
            ActionCard(Res.string.chatbot_import_title, Res.string.chatbot_import_caption) {
                AppOutlinedButton(onClick = { importFile() }, modifier = Modifier.testTag("chatbot_pick")) { Text(stringResource(Res.string.chatbot_choose_file)) }
                OutlinedTextField(
                    value = pasted, onValueChange = { pasted = it; result = null; status = "" }, minLines = 4, maxLines = 8,
                    label = { Text(stringResource(Res.string.chatbot_paste_label)) }, modifier = Modifier.fillMaxWidth().testTag("chatbot_paste"),
                )
                Button(onClick = { check(pasted) }, enabled = pasted.isNotBlank(), modifier = Modifier.testTag("chatbot_check")) { Text(stringResource(Res.string.chatbot_check)) }
            }
            result?.let { r ->
                ImportSummary(r, catalog, onSave = {
                    val now = nowEpochMs()
                    val base = TrainSettingsStore.load(ctx)
                    r.routines.forEach { SavedRoutineStore.upsert(ctx, savedFromExchange(it, catalog, newUuid(), now, base)) }
                    val names = r.routines.joinToString(", ") { it.name }
                    scope.launch { status = getString(Res.string.chatbot_saved, names) }
                    result = null; pasted = ""
                })
            }
            if (status.isNotEmpty()) Text(status, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.testTag("chatbot_status"))
        }
    }
}

@Composable private fun ActionCard(title: StringResource, caption: StringResource, content: @Composable () -> Unit) {
    Card(Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.large) {
        Column(Modifier.padding(Spacing.l), verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
            Text(stringResource(title), style = MaterialTheme.typography.titleMedium)
            Text(stringResource(caption), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            content()
        }
    }
}

@Composable
private fun ImportSummary(r: RoutineImportResult, catalog: Catalog, onSave: () -> Unit) {
    Card(Modifier.fillMaxWidth().testTag("chatbot_summary"), shape = MaterialTheme.shapes.large) {
        Column(Modifier.padding(Spacing.l), verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
            r.routines.forEach { routine ->
                Text(routine.name, style = MaterialTheme.typography.titleMedium)
                Text(stringResource(Res.string.chatbot_summary, routine.sets, routine.format.name.lowercase().replaceFirstChar { it.uppercase() }),
                    style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                routine.exercises.forEach { e -> ExerciseLine(e, catalog) }
            }
            if (r.errors.isNotEmpty()) {
                Text(stringResource(Res.string.chatbot_errors), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.error)
                r.errors.forEach { Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error) }
                Text(stringResource(Res.string.chatbot_errors_hint), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (r.warnings.isNotEmpty()) {
                Text(stringResource(Res.string.chatbot_warnings), style = MaterialTheme.typography.labelLarge, color = AppAccentTheme.colors.text)
                r.warnings.forEach { Text(it, style = MaterialTheme.typography.bodyMedium) }
            }
            if (r.canSave) Button(onClick = onSave, modifier = Modifier.testTag("chatbot_save")) { Text(stringResource(Res.string.chatbot_save)) }
        }
    }
}

@Composable
private fun ExerciseLine(e: ExchangeExercise, catalog: Catalog) {
    val v = catalog.variation(e.id)
    val perSide = v != null && v.sideMode != SideMode.NONE
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(v?.name ?: e.id, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
        Text(
            when {
                e.reps != null -> stringResource(if (perSide) Res.string.chatbot_reps_side else Res.string.chatbot_reps, e.reps!!)
                e.seconds != null -> stringResource(if (perSide) Res.string.chatbot_seconds_side else Res.string.chatbot_seconds, e.seconds!!)
                else -> stringResource(Res.string.chatbot_level_current)
            },
            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
