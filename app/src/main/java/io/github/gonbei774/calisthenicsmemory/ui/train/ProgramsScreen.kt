package io.github.gonbei774.calisthenicsmemory.ui.train

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.calisthenics.domain.planner.FixedProgramPlanResult
import app.calisthenics.domain.planner.buildFixedProgramPlan
import io.github.gonbei774.calisthenicsmemory.R
import io.github.gonbei774.calisthenicsmemory.ui.screens.ProfileStore

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProgramsScreen(modifier: Modifier = Modifier, onBack: () -> Unit, onStarted: () -> Unit) {
    val ctx = LocalContext.current
    val data by rememberTrainData()
    val profiles = remember { ProfileStore.load(ctx) }
    val settings = remember { TrainSettingsStore.load(ctx) }
    val profile = profiles.firstOrNull { it.id == settings.profileId } ?: profiles.first()
    var flowOverrides by remember { mutableStateOf(settings.fixedProgramFlowOverrides) }
    fun updateFlow(id: String, value: Boolean) {
        flowOverrides = flowOverrides + (id to value)
        TrainSettingsStore.save(ctx, settings.copy(fixedProgramId = id, fixedProgramFlowOverrides = flowOverrides))
    }
    Scaffold(topBar = {
        TopAppBar(title = { Text(stringResource(R.string.programs_title)) }, navigationIcon = {
            TextButton(onClick = onBack, modifier = Modifier.testTag("programs_back")) { Text(stringResource(R.string.back)) }
        })
    }) { pad ->
        Column(modifier.padding(pad).fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(stringResource(R.string.programs_intro), style = MaterialTheme.typography.bodyMedium)
            data.catalog.fixedPrograms.forEach { program ->
                val flow = flowOverrides[program.id] ?: program.flow
                val result = remember(data.catalog, program, profile, flow) { buildFixedProgramPlan(data.catalog, program, profile, flow) }
                Card(Modifier.fillMaxWidth().testTag("program_${program.id}")) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(program.name, style = MaterialTheme.typography.titleLarge)
                        Text(program.notes, style = MaterialTheme.typography.bodyMedium)
                        Text(stringResource(R.string.programs_item_count, program.items.size), style = MaterialTheme.typography.labelMedium)
                        program.items.forEachIndexed { index, item ->
                            val name = data.catalog.variation(item.exerciseId)?.name ?: item.exerciseId
                            val target = item.reps?.let { stringResource(R.string.programs_reps, it) }
                                ?: item.seconds?.let { stringResource(R.string.programs_seconds, it) }.orEmpty()
                            val side = if (item.perSide) stringResource(R.string.programs_per_side) else ""
                            Text(stringResource(R.string.programs_line, index + 1, name, listOf(target, side).filter { it.isNotBlank() }.joinToString(" · ")), style = MaterialTheme.typography.bodySmall)
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(stringResource(R.string.programs_flow))
                            Switch(checked = flow, onCheckedChange = { updateFlow(program.id, it) }, modifier = Modifier.testTag("program_flow_${program.id}"))
                        }
                        if (result is FixedProgramPlanResult.Unavailable) {
                            Text(stringResource(R.string.programs_unavailable, profile.name), color = MaterialTheme.colorScheme.error, modifier = Modifier.testTag("program_unavailable_${program.id}"))
                            result.items.forEach { missing ->
                                val name = data.catalog.variation(missing.exerciseId)?.name ?: missing.exerciseId
                                Text(stringResource(R.string.programs_missing_item, name, missing.missing), style = MaterialTheme.typography.bodySmall)
                            }
                        }
                        Button(
                            enabled = result is FixedProgramPlanResult.Ready,
                            onClick = {
                                if (result is FixedProgramPlanResult.Ready) {
                                    TrainSettingsStore.save(ctx, settings.copy(fixedProgramId = program.id, fixedProgramFlowOverrides = flowOverrides))
                                    startPlan(ctx, result.plan)
                                    onStarted()
                                }
                            }, modifier = Modifier.fillMaxWidth().testTag("program_start_${program.id}"),
                        ) { Text(stringResource(R.string.programs_start)) }
                    }
                }
            }
        }
    }
}
