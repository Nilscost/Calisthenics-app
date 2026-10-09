// V04b (R21, R22): one number per round, plus Too hard · Too easy · Pain for the exercise. Used by the end screen and the History detail.
// Every change is reported as a correction for EVERY round of the exercise (see feedback/Corrections.kt), saved as new revisions.
package io.github.gonbei774.calisthenicsmemory.ui.session

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import app.calisthenics.domain.history.ratingFor
import app.calisthenics.domain.model.Side
import app.calisthenics.domain.model.Target
import app.calisthenics.domain.model.TargetType
import io.github.gonbei774.calisthenicsmemory.R
import io.github.gonbei774.calisthenicsmemory.ui.components.Stepper
import io.github.gonbei774.calisthenicsmemory.ui.history.RoundCorrection
import io.github.gonbei774.calisthenicsmemory.ui.theme.Spacing

/** One round to edit. [value] null = not typed ("as planned"). */
data class EditorRound(val blockId: String, val round: Int, val side: Side, val value: Int?, val corrected: Boolean = false)

/**
 * Tags: `<tag>_round<i>` (the label), `<tag>_round<i>_value|minus|plus` (i = 1..n in list order),
 * `<tag>_too_hard`, `<tag>_too_easy`, `<tag>_pain`. [onChange] gets all rounds each time.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RoundEditor(
    target: Target?, rounds: List<EditorRound>, tooHard0: Boolean, tooEasy0: Boolean, pain0: Boolean, tag: String,
    onChange: (List<RoundCorrection>) -> Unit,
) {
    val hold = target?.type == TargetType.HOLD_SECONDS
    val values = remember(tag, rounds.map { it.blockId }) { mutableStateListOf<Int>().apply { rounds.forEach { add(it.value ?: target?.value ?: 0) } } }
    val typed = remember(tag, rounds.map { it.blockId }) { mutableStateListOf<Boolean>().apply { rounds.forEach { add(it.value != null) } } }
    var tooHard by remember(tag) { mutableStateOf(tooHard0) }
    var tooEasy by remember(tag) { mutableStateOf(tooEasy0) }
    var pain by remember(tag) { mutableStateOf(pain0) }
    fun push() = onChange(rounds.mapIndexed { i, r ->
        val reps = if (typed[i]) values[i] else null
        RoundCorrection(r.blockId, reps, ratingFor(reps, target, tooHard, tooEasy), pain, hold)
    })
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        rounds.forEachIndexed { i, r ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f).semantics(mergeDescendants = true) {}.testTag("${tag}_round${i + 1}")) {
                    val side = when (r.side) { Side.LEFT -> stringResource(R.string.side_left); Side.RIGHT -> stringResource(R.string.side_right); else -> "" }
                    Text(if (side.isEmpty()) stringResource(R.string.round_label, r.round) else stringResource(R.string.round_label_side, r.round, side), style = MaterialTheme.typography.bodyLarge)
                    val note = listOfNotNull(
                        if (!typed[i]) stringResource(R.string.round_as_planned) else null,
                        if (r.corrected) stringResource(R.string.round_corrected) else null,
                    ).joinToString(" · ")
                    if (note.isNotEmpty()) Text(note, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Stepper(values[i], { values[i] = it; typed[i] = true; push() }, 0..999, step = if (hold && (target?.value ?: 0) >= 30) 5 else 1, tag = "${tag}_round${i + 1}")
            }
        }
        Text(stringResource(if (hold) R.string.logger_seconds_held else R.string.logger_reps_done), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.s)) {
            FilterChip(selected = tooHard, onClick = { tooHard = !tooHard; if (tooHard) tooEasy = false; push() }, label = { Text(stringResource(R.string.logger_too_hard)) }, modifier = Modifier.testTag("${tag}_too_hard"))
            FilterChip(selected = tooEasy, onClick = { tooEasy = !tooEasy; if (tooEasy) tooHard = false; push() }, label = { Text(stringResource(R.string.logger_too_easy)) }, modifier = Modifier.testTag("${tag}_too_easy"))
            FilterChip(selected = pain, onClick = { pain = !pain; push() }, label = { Text(stringResource(R.string.logger_pain)) }, modifier = Modifier.testTag("${tag}_pain"))
        }
    }
}
