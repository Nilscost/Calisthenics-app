package io.github.gonbei774.calisthenicsmemory.ui.train

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import io.github.gonbei774.calisthenicsmemory.ui.components.AppTextButton
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.calisthenics.domain.planner.FixedProgramPlanResult
import app.calisthenics.domain.planner.buildFixedProgramPlan
import app.calisthenics.domain.equipment.setSatisfied
import app.calisthenics.domain.model.Catalog
import app.calisthenics.domain.model.FixedProgram
import app.calisthenics.domain.model.RequirementSet
import io.github.gonbei774.calisthenicsmemory.R
import io.github.gonbei774.calisthenicsmemory.ui.screens.ProfileStore
import io.github.gonbei774.calisthenicsmemory.ui.screens.equipmentLabelShortPublic

internal fun fixedProgramDurationSeconds(catalog: Catalog, program: FixedProgram, result: FixedProgramPlanResult): Int =
    (result as? FixedProgramPlanResult.Ready)?.plan?.plannedDurationSeconds ?: run {
        data class Prior(val variationId: String, val side: Int, val sidePreparation: Boolean)
        var prior: Prior? = null
        var total = 0
        program.items.forEach { item ->
            val duration = (item.estSeconds ?: item.seconds ?: ((item.reps ?: 1) * 3)).coerceAtLeast(1)
            val variation = catalog.variation(item.exerciseId)
            val sidePreparation = item.perSide && variation?.kind in setOf(app.calisthenics.domain.model.Kind.HOLD, app.calisthenics.domain.model.Kind.STRETCH, app.calisthenics.domain.model.Kind.MOBILITY)
            (if (item.perSide) listOf(1, 2) else listOf(0)).forEach { side ->
                val previous = prior
                if (previous != null && previous.variationId == item.exerciseId && previous.side != 0 && side != 0 &&
                    previous.side != side && previous.sidePreparation && sidePreparation) total += 5
                total += duration
                prior = Prior(item.exerciseId, side, sidePreparation)
            }
        }
        total
    }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProgramsScreen(modifier: Modifier = Modifier, onBack: () -> Unit, onStarted: () -> Unit) {
    val ctx = LocalContext.current
    val data by rememberTrainData()
    val profiles = remember { ProfileStore.load(ctx) }
    val settings = remember { TrainSettingsStore.load(ctx) }
    val profile = profiles.firstOrNull { it.id == settings.profileId } ?: profiles.first()
    var flowOverrides by remember { mutableStateOf(settings.fixedProgramFlowOverrides) }
    var expandedProgram by remember { mutableStateOf<String?>(null) }
    fun updateFlow(id: String, value: Boolean) {
        flowOverrides = flowOverrides + (id to value)
        TrainSettingsStore.save(ctx, settings.copy(fixedProgramId = id, fixedProgramFlowOverrides = flowOverrides))
    }
    Scaffold(topBar = {
        TopAppBar(title = { Text(stringResource(R.string.programs_title)) }, navigationIcon = {
            AppTextButton(onClick = onBack, modifier = Modifier.testTag("programs_back")) { Text(stringResource(R.string.back)) }
        })
    }) { pad ->
        Column(modifier.padding(pad).fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(stringResource(R.string.programs_intro), style = MaterialTheme.typography.bodyMedium)
            data.catalog.fixedPrograms.forEach { program ->
                val flow = flowOverrides[program.id] ?: program.flow
                val result = remember(data.catalog, program, profile, flow) { buildFixedProgramPlan(data.catalog, program, profile, flow) }
                val durationSeconds = fixedProgramDurationSeconds(data.catalog, program, result)
                val durationMinutes = ((durationSeconds + 59) / 60).coerceAtLeast(1)
                @Composable fun requirementLabel(set: RequirementSet): String {
                    val ids = (set.needs.map { it.equipmentId } + set.capabilities).distinct().sorted()
                    if (ids.isEmpty()) return stringResource(R.string.programs_no_equipment)
                    val labels = mutableListOf<String>()
                    for (id in ids) labels += stringResource(equipmentLabelShortPublic(id))
                    return labels.joinToString(" + ")
                }
                val equipmentOptions = mutableListOf<String>()
                for (item in program.items) {
                    val variation = data.catalog.variation(item.exerciseId) ?: continue
                    val sets = variation.equipmentAlternatives.ifEmpty { listOf(RequirementSet()) }
                    val chosen = if (result is FixedProgramPlanResult.Ready) {
                        sets.filter { setSatisfied(it, profile) }.minByOrNull { it.needs.size + it.capabilities.size }?.let { listOf(it) }.orEmpty()
                    } else sets
                    val labels = mutableListOf<String>()
                    for (set in chosen) {
                        val label = requirementLabel(set)
                        if (label !in labels) labels += label
                    }
                    if (labels.isNotEmpty()) {
                        var options = labels.first()
                        for (option in labels.drop(1)) options = stringResource(R.string.programs_equipment_or, options, option)
                        equipmentOptions += options
                    }
                }
                val distinctEquipmentOptions = equipmentOptions.distinct()
                val equipmentValue = if (distinctEquipmentOptions.isEmpty()) stringResource(R.string.programs_no_equipment) else distinctEquipmentOptions.joinToString(" · ")
                val equipmentSummary = stringResource(R.string.programs_equipment_label, equipmentValue)
                Card(Modifier.fillMaxWidth().testTag("program_${program.id}")) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(Modifier.fillMaxWidth().heightIn(min = 56.dp).testTag("program_summary_${program.id}"), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column(Modifier.weight(1f)) {
                                Text(program.name, style = MaterialTheme.typography.titleMedium)
                            Text(stringResource(R.string.programs_summary, durationMinutes, equipmentSummary), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.testTag("program_summary_meta_${program.id}"))
                            }
                            AppTextButton(onClick = { expandedProgram = if (expandedProgram == program.id) null else program.id }, modifier = Modifier.testTag("program_open_${program.id}")) {
                                Text(stringResource(if (expandedProgram == program.id) R.string.programs_close else R.string.programs_details))
                            }
                        }
                        if (expandedProgram == program.id) {
                        Text(program.notes, style = MaterialTheme.typography.bodyMedium)
                        Text(stringResource(R.string.programs_item_count, program.items.size), style = MaterialTheme.typography.labelMedium)
                        program.items.forEachIndexed { index, item ->
                            val name = data.catalog.variation(item.exerciseId)?.name ?: item.exerciseId
                            val target = item.reps?.let { stringResource(R.string.programs_reps, it) }
                                ?: item.seconds?.let { stringResource(R.string.programs_seconds, it) }.orEmpty()
                            val side = if (item.perSide) stringResource(R.string.programs_per_side) else ""
                            Text(stringResource(R.string.programs_line, index + 1, name, listOf(target, side).filter { it.isNotBlank() }.joinToString(" · ")), style = MaterialTheme.typography.bodySmall, modifier = Modifier.testTag("program_line_${program.id}_$index"))
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
}
