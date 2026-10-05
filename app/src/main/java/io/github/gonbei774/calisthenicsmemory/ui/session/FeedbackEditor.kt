// "Reps you did / Too hard / Pain" for one exercise: the correction editor of the History detail and of the end screen.
package io.github.gonbei774.calisthenicsmemory.ui.session

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import app.calisthenics.domain.model.Target
import app.calisthenics.domain.model.TargetType
import io.github.gonbei774.calisthenicsmemory.R
import io.github.gonbei774.calisthenicsmemory.ui.components.Stepper
import io.github.gonbei774.calisthenicsmemory.ui.theme.Spacing

/** [onChange] gets the typed number, "too hard" and "pain"; every call is meant to be saved as a new revision. */
@Composable
fun FeedbackEditor(target: Target?, reps0: Int?, tooHard0: Boolean, pain0: Boolean, tag: String, onChange: (reps: Int?, tooHard: Boolean, pain: Boolean) -> Unit) {
    val hold = target?.type == TargetType.HOLD_SECONDS
    var reps by remember(tag) { mutableIntStateOf(reps0 ?: target?.value ?: 0) }
    var typed by remember(tag) { mutableStateOf(reps0 != null) }
    var tooHard by remember(tag) { mutableStateOf(tooHard0) }
    var pain by remember(tag) { mutableStateOf(pain0) }
    fun push() = onChange(if (typed) reps else null, tooHard, pain)
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(if (hold) R.string.logger_seconds_held else R.string.logger_reps_done), Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
            Stepper(reps, { reps = it; typed = true; push() }, 0..999, step = if (hold && (target?.value ?: 0) >= 30) 5 else 1, tag = "${tag}_reps")
        }
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s)) {
            FilterChip(selected = tooHard, onClick = { tooHard = !tooHard; push() }, label = { Text(stringResource(R.string.logger_too_hard)) }, modifier = Modifier.testTag("${tag}_too_hard"))
            FilterChip(selected = pain, onClick = { pain = !pain; push() }, label = { Text(stringResource(R.string.logger_pain)) }, modifier = Modifier.testTag("${tag}_pain"))
        }
    }
}
