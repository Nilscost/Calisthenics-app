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

/**
 * OBJECTIVE: a segmented control (Body part · Skill · Routine) and under it a dropdown with the choices of the picked type.
 * Tags: `objective_type_<TYPE>`, `goal_field`, `goal_<id>`. Routine is only offered when [presetsExist] (V22).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ObjectivePicker(goalId: String, onPick: (String) -> Unit, modifier: Modifier = Modifier, routines: List<SavedRoutine> = emptyList(), routineId: String? = null, onPickRoutine: (String) -> Unit = {}) {
    val goal = Goals.byId(goalId) ?: Goals.all.first()
    val saved = routines.firstOrNull { it.id == routineId }
    var type by remember(goalId, routineId) { mutableStateOf(if (saved != null) ObjectiveType.ROUTINE else Goals.typeOf(goal)) }
    val types = Goals.availableTypes(routines.isNotEmpty())
    var open by remember { mutableStateOf(false) }
    val fieldDescription = stringResource(R.string.field_description, stringResource(R.string.objective_caption), if (saved != null && type == ObjectiveType.ROUTINE) saved.name else goal.name)
    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Caption(stringResource(R.string.objective_caption))
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            types.forEachIndexed { i, t ->
                SegmentedButton(
                    selected = t == type, onClick = { if (t != type) { type = t; if (t == ObjectiveType.ROUTINE) routines.firstOrNull()?.let { onPickRoutine(it.id) } else Goals.goalsOf(t).firstOrNull()?.let { onPick(it.id) } } },
                    colors = appSegmentedColors(), shape = SegmentedButtonDefaults.itemShape(i, types.size), icon = {}, modifier = Modifier.testTag("objective_type_${t.name}"),
                ) { Text(stringResource(typeLabel(t))) }
            }
        }
        ExposedDropdownMenuBox(expanded = open, onExpandedChange = { open = it }) {
            OutlinedTextField(
                value = if (type == ObjectiveType.ROUTINE) saved?.name ?: "" else goal.name, onValueChange = {}, readOnly = true, singleLine = true,
                supportingText = { Text(if (type == ObjectiveType.ROUTINE) saved?.let { stringResource(if (it.credit != null) R.string.routine_summary_ready else R.string.routine_summary, it.routine.slots.size, stringResource(formatName(it.format))) }.orEmpty() else goal.description, maxLines = 1) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(open) },
                modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth().semantics { contentDescription = fieldDescription }.testTag("goal_field"),
            )
            ExposedDropdownMenu(expanded = open, onDismissRequest = { open = false }) {
                if (type == ObjectiveType.ROUTINE) routines.forEach { r -> DropdownMenuItem(text = { Text(r.name) }, onClick = { onPickRoutine(r.id); open = false }, modifier = Modifier.testTag("routine_${r.id}")) }
                else Goals.goalsOf(type).forEach { g: Goal ->
                    DropdownMenuItem(text = { Text(g.name) }, onClick = { onPick(g.id); open = false }, modifier = Modifier.testTag("goal_${g.id}"))
                }
            }
        }
    }
}

fun formatName(f: WorkoutFormat) = when (f) { WorkoutFormat.CIRCUIT -> R.string.format_circuit; WorkoutFormat.PAIRS -> R.string.format_pairs; WorkoutFormat.STRAIGHT -> R.string.format_straight }
