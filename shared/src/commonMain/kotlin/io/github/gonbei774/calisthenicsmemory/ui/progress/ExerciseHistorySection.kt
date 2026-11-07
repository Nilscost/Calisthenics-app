// The history of one exercise, below its levels on the exercise detail page (Progress): the past sessions newest first, from the shared history repository.
package io.github.gonbei774.calisthenicsmemory.ui.progress

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import app.calisthenics.domain.history.ExerciseHistoryEntry
import app.calisthenics.domain.history.SessionWithPlan
import app.calisthenics.domain.history.exerciseHistory
import io.github.gonbei774.calisthenicsmemory.platform.currentPlatformContext
import io.github.gonbei774.calisthenicsmemory.platform.formatMediumDate
import io.github.gonbei774.calisthenicsmemory.platform.systemTimeZone
import io.github.gonbei774.calisthenicsmemory.shared.resources.*
import io.github.gonbei774.calisthenicsmemory.ui.components.Caption
import io.github.gonbei774.calisthenicsmemory.ui.history.RoomHistorySource
import io.github.gonbei774.calisthenicsmemory.ui.theme.AppAccentTheme
import io.github.gonbei774.calisthenicsmemory.ui.theme.Spacing
import org.jetbrains.compose.resources.stringResource

/** The page reads the history once per exercise. */
@Composable
internal fun ExerciseHistorySection(variationId: String, modifier: Modifier = Modifier) {
    val ctx = currentPlatformContext()
    var entries by remember(variationId) { mutableStateOf<List<ExerciseHistoryEntry>?>(null) }
    LaunchedEffect(variationId) {
        entries = try {
            exerciseHistory(RoomHistorySource(ctx).sessions().map { SessionWithPlan(it.record, it.plan, it.rows) }, variationId, systemTimeZone())
        } catch (_: Throwable) { emptyList() }
    }
    Column(modifier.fillMaxWidth().testTag("detail_history"), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        Caption(stringResource(Res.string.detail_history), Modifier.padding(top = Spacing.s))
        val list = entries ?: return@Column
        if (list.isEmpty()) {
            Text(stringResource(Res.string.detail_history_empty), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.testTag("detail_history_empty"))
            return@Column
        }
        list.forEach { e -> HistoryRow(e) }
    }
}

@Composable
private fun HistoryRow(e: ExerciseHistoryEntry) {
    val numbers = e.values.joinToString(" · ")
    val unit = stringResource(if (e.isHold) Res.string.exercise_history_secs else Res.string.exercise_history_reps, numbers)
    val sets = if (e.values.isEmpty()) "" else if (e.alternating) stringResource(Res.string.exercise_history_per_side, unit) else unit
    Row(Modifier.fillMaxWidth().heightIn(min = 44.dp).semantics(mergeDescendants = true) {}.testTag("detail_history_${e.sessionId}"), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.m)) {
        Column(Modifier.weight(1f)) {
            Text(formatMediumDate(e.day), style = MaterialTheme.typography.titleMedium)
            Text(listOfNotNull(e.level?.let { stringResource(Res.string.exercise_history_level, it) }, sets.ifEmpty { null }).joinToString(" · "), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (e.good) Text(stringResource(Res.string.exercise_history_good).uppercase(), style = MaterialTheme.typography.labelLarge, color = AppAccentTheme.colors.text, modifier = Modifier.testTag("detail_history_good_${e.sessionId}"))
    }
}
