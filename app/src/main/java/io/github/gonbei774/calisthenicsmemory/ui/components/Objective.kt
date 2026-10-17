// Doc 17 §1/§2.1: the small UPPER-CASE caption above every choice, and the objective control shared by Train and the questionnaire
// (R1: three types chosen with buttons, then the choice inside the picked type in a dropdown).
package io.github.gonbei774.calisthenicsmemory.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import app.calisthenics.domain.goals.Goal
import app.calisthenics.domain.goals.Goals
import app.calisthenics.domain.goals.ObjectiveType
import io.github.gonbei774.calisthenicsmemory.R
import app.calisthenics.domain.routine.SavedRoutine
import app.calisthenics.domain.model.WorkoutFormat

@Composable
fun Caption(text: String, modifier: Modifier = Modifier) =
    Text(text.uppercase(), modifier, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)

private fun typeLabel(t: ObjectiveType) = when (t) {
    ObjectiveType.BODY_PART -> R.string.objective_body_part
    ObjectiveType.SKILL -> R.string.objective_skill
    ObjectiveType.ROUTINE -> R.string.objective_routine
}

/** A pre-registered program of the catalog as a choice in the Routine dropdown (R8.1-T08, F9). [summary] is the compact description. */
data class ProgramEntry(val id: String, val name: String, val summary: String)

/**
 * OBJECTIVE: a segmented control (Body part · Skill · Routine) and under it a dropdown with the choices of the picked type.
 * Under Routine the one dropdown has two labelled sections (R8.1-T08, F9): Programs (the pre-registered ones: the ready-made routines
 * and the catalog programs) and My routines (the saved ones). Picking a program loads it into the Preview as a starting point.
 * Tags: `objective_type_<TYPE>`, `goal_field`, `goal_<id>`, `routine_<id>`, `program_<id>`, `section_programs`, `section_my_routines`.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ObjectivePicker(
    goalId: String, onPick: (String) -> Unit, modifier: Modifier = Modifier, routines: List<SavedRoutine> = emptyList(), routineId: String? = null,
    onPickRoutine: (String) -> Unit = {}, fixedPrograms: List<ProgramEntry> = emptyList(), programId: String? = null, onPickProgram: (String) -> Unit = {},
) {
    val goal = Goals.byId(goalId) ?: Goals.all.first()
    val readyMade = routines.filter { it.credit != null }
    val mine = routines.filter { it.credit == null }
    val savedChoice = routines.firstOrNull { it.id == routineId }
    val programChoice = fixedPrograms.firstOrNull { it.id == programId }
    val inRoutine = savedChoice != null || programChoice != null
    var type by remember(goalId, routineId, programId) { mutableStateOf(if (inRoutine) ObjectiveType.ROUTINE else Goals.typeOf(goal)) }
    val types = Goals.availableTypes(routines.isNotEmpty() || fixedPrograms.isNotEmpty())
    var open by remember { mutableStateOf(false) }
    val shownName = if (type == ObjectiveType.ROUTINE) programChoice?.name ?: savedChoice?.name ?: "" else goal.name
    val fieldDescription = stringResource(R.string.field_description, stringResource(R.string.objective_caption), shownName.ifEmpty { goal.name })
    val firstRoutineTarget = { readyMade.firstOrNull()?.let { onPickRoutine(it.id) } ?: fixedPrograms.firstOrNull()?.let { onPickProgram(it.id) } ?: routines.firstOrNull()?.let { onPickRoutine(it.id) } }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Caption(stringResource(R.string.objective_caption))
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            types.forEachIndexed { i, t ->
                SegmentedButton(
                    selected = t == type, onClick = { if (t != type) { type = t; if (t == ObjectiveType.ROUTINE) firstRoutineTarget() else Goals.goalsOf(t).firstOrNull()?.let { onPick(it.id) } } },
                    colors = appSegmentedColors(), shape = SegmentedButtonDefaults.itemShape(i, types.size), icon = {}, modifier = Modifier.testTag("objective_type_${t.name}"),
                ) { Text(stringResource(typeLabel(t))) }
            }
        }
        ExposedDropdownMenuBox(expanded = open, onExpandedChange = { open = it }) {
            OutlinedTextField(
                value = shownName, onValueChange = {}, readOnly = true, singleLine = true,
                supportingText = { Text(if (type == ObjectiveType.ROUTINE) programChoice?.summary ?: savedChoice?.let { stringResource(if (it.credit != null) R.string.routine_summary_ready else R.string.routine_summary, it.routine.slots.size, stringResource(formatName(it.format))) }.orEmpty() else goal.description, maxLines = 1) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(open) },
                modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth().semantics { contentDescription = fieldDescription }.testTag("goal_field"),
            )
            ExposedDropdownMenu(expanded = open, onDismissRequest = { open = false }) {
                if (type == ObjectiveType.ROUTINE) {
                    // Programs: the pre-registered ones; My routines: the person's saved ones
                    Text(stringResource(R.string.section_programs).uppercase(), Modifier.padding(horizontal = 16.dp, vertical = 6.dp).testTag("section_programs"), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                    readyMade.forEach { r -> DropdownMenuItem(text = { Column { Text(r.name); Text(stringResource(R.string.routine_summary_ready, r.routine.slots.size, stringResource(formatName(r.format))), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1) } }, onClick = { onPickRoutine(r.id); open = false }, modifier = Modifier.testTag("routine_${r.id}")) }
                    fixedPrograms.forEach { p -> DropdownMenuItem(text = { Column { Text(p.name); Text(p.summary, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1) } }, onClick = { onPickProgram(p.id); open = false }, modifier = Modifier.testTag("program_${p.id}")) }
                    HorizontalDivider()
                    Text(stringResource(R.string.section_my_routines).uppercase(), Modifier.padding(horizontal = 16.dp, vertical = 6.dp).testTag("section_my_routines"), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                    if (mine.isEmpty()) Text(stringResource(R.string.section_my_routines_empty), Modifier.padding(horizontal = 16.dp, vertical = 8.dp).testTag("my_routines_empty"), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    mine.forEach { r -> DropdownMenuItem(text = { Column { Text(r.name); Text(stringResource(R.string.routine_summary, r.program?.items?.size ?: r.routine.slots.size, stringResource(formatName(r.format))), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1) } }, onClick = { onPickRoutine(r.id); open = false }, modifier = Modifier.testTag("routine_${r.id}")) }
                } else Goals.goalsOf(type).forEach { g: Goal ->
                    DropdownMenuItem(text = { Text(g.name) }, onClick = { onPick(g.id); open = false }, modifier = Modifier.testTag("goal_${g.id}"))
                }
            }
        }
    }
}

fun formatName(f: WorkoutFormat) = when (f) { WorkoutFormat.CIRCUIT -> R.string.format_circuit; WorkoutFormat.PAIRS -> R.string.format_pairs; WorkoutFormat.STRAIGHT -> R.string.format_straight }
