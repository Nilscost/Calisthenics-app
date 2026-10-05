// U05 stand-in for the draft workout: header, block list, warnings and Start. U06 replaces the list with exercise cards and a swap sheet.
package io.github.gonbei774.calisthenicsmemory.ui.train

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.calisthenics.domain.model.TargetType
import app.calisthenics.domain.planner.PlanResult
import app.calisthenics.domain.planner.buildTrainPlan
import io.github.gonbei774.calisthenicsmemory.R
import io.github.gonbei774.calisthenicsmemory.ui.screens.ProfileStore
import io.github.gonbei774.calisthenicsmemory.ui.screens.startWorkout
import io.github.gonbei774.calisthenicsmemory.ui.theme.Radius
import io.github.gonbei774.calisthenicsmemory.ui.theme.Spacing
import kotlinx.serialization.json.Json
import java.util.UUID

fun formatClock(sec: Int) = "%d:%02d".format(sec / 60, sec % 60)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PreviewScreen(modifier: Modifier = Modifier, onBack: () -> Unit, onStarted: () -> Unit) {
    val ctx = LocalContext.current
    val data by rememberTrainData()
    val settings = remember { TrainSettingsStore.load(ctx) }
    val profile = remember { ProfileStore.selected(ctx) }
    val result = remember(data) { buildTrainPlan(data.catalog, data.routine, data.progress, profile, settings, nowEpochMs = System.currentTimeMillis()) }
    val names = remember(data) { data.catalog.variations.associate { it.id to it.name } }

    Scaffold(
        modifier = modifier,
        topBar = { TopAppBar(title = { Text(stringResource(R.string.preview_title)) }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back)) } }) },
        bottomBar = {
            Surface(tonalElevation = 3.dp) {
                Button(
                    onClick = {
                        val plan = (result as PlanResult.Ready).plan.copy(id = UUID.randomUUID().toString()) // one history snapshot per workout
                        val json = Json { encodeDefaults = true }.encodeToString(app.calisthenics.domain.model.WorkoutPlan.serializer(), plan)
                        startWorkout(ctx, json, UUID.randomUUID().toString(), true)
                        onStarted()
                    },
                    enabled = result is PlanResult.Ready,
                    modifier = Modifier.fillMaxWidth().padding(Spacing.l).height(56.dp).testTag("start_button"),
                    shape = RoundedCornerShape(Radius.button),
                ) { Text(stringResource(R.string.preview_start)) }
            }
        },
    ) { pad ->
        Column(Modifier.padding(pad).verticalScroll(rememberScrollState()).padding(Spacing.l), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            when (val r = result) {
                is PlanResult.Infeasible -> r.reasons.forEach { Text(it.message) }
                is PlanResult.Ready -> {
                    val p = r.plan
                    Text(stringResource(R.string.preview_header, p.rounds, p.plannedDurationSeconds / 60, stringResource(if (p.timed) R.string.style_timed else R.string.style_reps)), style = MaterialTheme.typography.titleMedium, modifier = Modifier.testTag("preview_header"))
                    p.warnings.forEach { Text(it, color = MaterialTheme.colorScheme.error) }
                    p.blocks.forEach { b ->
                        val name = b.variationId?.let { names[it] ?: it } ?: b.type.name.lowercase().replace('_', ' ')
                        val target = b.target?.let { if (it.type == TargetType.REPS) stringResource(R.string.target_reps, it.value) else stringResource(R.string.target_seconds, it.value) }.orEmpty()
                        Text("${formatClock(b.durationSeconds)}  $name $target".trim(), style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}
