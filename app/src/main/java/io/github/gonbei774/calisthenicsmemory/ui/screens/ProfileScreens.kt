// U04 (F5): profile list (Settings) and the profile editor with an equipment checklist.
package io.github.gonbei774.calisthenicsmemory.ui.screens

import io.github.gonbei774.calisthenicsmemory.ui.components.AppTextButton
import io.github.gonbei774.calisthenicsmemory.ui.components.AppOutlinedButton

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import app.calisthenics.domain.equipment.*
import app.calisthenics.domain.model.EquipmentProfile
import io.github.gonbei774.calisthenicsmemory.R
import io.github.gonbei774.calisthenicsmemory.ui.components.Stepper
import io.github.gonbei774.calisthenicsmemory.ui.theme.EquipmentIcons
import io.github.gonbei774.calisthenicsmemory.ui.theme.Spacing

fun equipmentLabel(id: String): Int = when (id) {
    "pullup-bar" -> R.string.eq_pullup_bar
    "high-bar" -> R.string.eq_high_bar
    "low-bar" -> R.string.eq_low_bar
    "resistance-band" -> R.string.eq_band
    "kettlebell" -> R.string.eq_kettlebell
    "weight" -> R.string.eq_dumbbells
    "chair" -> R.string.eq_chair
    "wall" -> R.string.eq_wall
    "mat" -> R.string.eq_mat
    "dip-support" -> R.string.eq_dip_support
    "foot-anchor" -> R.string.eq_foot_anchor
    "parallettes" -> R.string.eq_parallettes
    else -> R.string.eq_other
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfilesScreen(modifier: Modifier = Modifier, onBack: () -> Unit, onEdit: (String?) -> Unit) {
    val ctx = LocalContext.current
    val profiles = remember { ProfileStore.load(ctx) }
    Scaffold(modifier = modifier, topBar = {
        TopAppBar(title = { Text(stringResource(R.string.profiles_title)) },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back)) } })
    }) { pad ->
        Column(Modifier.padding(pad).verticalScroll(rememberScrollState()).padding(Spacing.l), verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
            profiles.forEach { p ->
                Card(onClick = { onEdit(p.id) }, modifier = Modifier.fillMaxWidth().testTag("profile_${p.id}")) {
                    Column(Modifier.padding(Spacing.l)) {
                        Text(p.name, style = MaterialTheme.typography.titleMedium)
                        Text(equipmentSummary(p), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            Button(onClick = { onEdit(null) }, modifier = Modifier.fillMaxWidth().testTag("profile_add")) { Text(stringResource(R.string.profile_add)) }
        }
    }
}

@Composable
private fun equipmentSummary(p: EquipmentProfile): String {
    val labels = p.items.map { stringResource(equipmentLabelShort(it.equipmentId)) } +
        p.capabilities.filter { EquipmentOptions.byId(it)?.kind == OptionKind.CAPABILITY }.map { stringResource(equipmentLabelShort(it)) }
    return if (labels.isEmpty()) stringResource(R.string.profile_no_equipment) else labels.joinToString(", ")
}

fun equipmentLabelShortPublic(id: String): Int = equipmentLabelShort(id)

private fun equipmentLabelShort(id: String): Int = when (id) {
    "pullup-bar" -> R.string.eq_pullup_bar_short
    "high-bar" -> R.string.eq_high_bar_short
    "low-bar" -> R.string.eq_low_bar_short
    "resistance-band" -> R.string.eq_band_short
    "kettlebell" -> R.string.eq_kettlebell_short
    "weight" -> R.string.eq_dumbbells_short
    "chair" -> R.string.eq_chair_short
    "wall" -> R.string.eq_wall_short
    "mat" -> R.string.eq_mat_short
    "dip-support" -> R.string.eq_dip_support_short
    "foot-anchor" -> R.string.eq_foot_anchor_short
    "parallettes" -> R.string.eq_parallettes_short
    else -> R.string.eq_other
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileEditScreen(modifier: Modifier = Modifier, profileId: String?, onDone: () -> Unit) {
    val ctx = LocalContext.current
    val profiles = remember { ProfileStore.load(ctx) }
    val existing = remember(profileId) { profiles.firstOrNull { it.id == profileId } }
    var name by rememberSaveable { mutableStateOf(existing?.name ?: "") }
    var picked by remember { mutableStateOf(existing?.let { selectionOf(it).map { s -> s.id } } ?: listOf("mat")) }
    var weights by remember { mutableStateOf<Map<String, Int>>(existing?.let { selectionOf(it).filter { s -> s.massGrams != null }.associate { s -> s.id to s.massGrams!! } } ?: emptyMap()) }
    var confirmDelete by remember { mutableStateOf(false) }
    val problem = ProfileOps.nameProblem(name, profiles, existing?.id)
    var showProblem by remember { mutableStateOf(false) }

    fun save() {
        if (problem != null) { showProblem = true; return }
        val sel = picked.map { EquipmentSelection(it, weights[it]) }
        val p = buildProfile(existing?.id ?: ProfileOps.newId(name, profiles), name, sel)
        ProfileStore.upsert(ctx, p)
        if (existing == null) ProfileStore.select(ctx, p.id)
        onDone()
    }

    Scaffold(modifier = modifier, topBar = {
        TopAppBar(
            title = { Text(stringResource(if (existing == null) R.string.profile_new else R.string.profile_edit)) },
            navigationIcon = { IconButton(onClick = onDone) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back)) } },
            actions = { AppTextButton(onClick = { save() }, modifier = Modifier.testTag("profile_save")) { Text(stringResource(R.string.save)) } },
        )
    }) { pad ->
        Column(Modifier.padding(pad).verticalScroll(rememberScrollState()).padding(Spacing.l), verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
            OutlinedTextField(
                value = name, onValueChange = { name = it.take(30); showProblem = false }, singleLine = true,
                label = { Text(stringResource(R.string.profile_name)) },
                isError = showProblem && problem != null,
                supportingText = {
                    if (showProblem && problem != null) Text(stringResource(if (problem == NameProblem.EMPTY) R.string.profile_name_empty else R.string.profile_name_duplicate))
                },
                modifier = Modifier.fillMaxWidth().testTag("profile_name"),
            )
            Text(stringResource(R.string.profile_equipment), style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = Spacing.s))
            EquipmentChecklist(picked, { picked = it }, weights, { weights = it })
            if (existing != null && profiles.size > 1) {
                AppOutlinedButton(onClick = { confirmDelete = true }, modifier = Modifier.fillMaxWidth().padding(top = Spacing.l).testTag("profile_delete")) {
                    Text(stringResource(R.string.profile_delete))
                }
            }
        }
    }
    if (confirmDelete && existing != null) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(stringResource(R.string.profile_delete_title, existing.name)) },
            text = { Text(stringResource(R.string.profile_delete_text)) },
            confirmButton = { AppTextButton(onClick = { ProfileStore.delete(ctx, existing.id); confirmDelete = false; onDone() }) { Text(stringResource(R.string.profile_delete)) } },
            dismissButton = { AppTextButton(onClick = { confirmDelete = false }) { Text(stringResource(R.string.cancel)) } },
        )
    }
}

/** The equipment rows shared by the profile editor and the questionnaire. [weights] are grams for weighted rows. */
@Composable
fun EquipmentChecklist(picked: List<String>, onPicked: (List<String>) -> Unit, weights: Map<String, Int>, onWeights: (Map<String, Int>) -> Unit) {
    EquipmentOptions.all.forEach { opt ->
        val on = opt.id in picked
        Row(
            Modifier.fillMaxWidth().heightIn(min = Spacing.touch).clickable(role = Role.Checkbox) {
                onPicked(if (on) picked - opt.id else picked + opt.id)
            }.testTag("eq_${opt.id}"),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.m),
        ) {
            Checkbox(checked = on, onCheckedChange = null)
            EquipmentIcons.forId(opt.id)?.let { Icon(it, contentDescription = null, modifier = Modifier.size(24.dp)) }
            Text(stringResource(equipmentLabel(opt.id)), Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
        }
        if (on && opt.weighted) {
            val grams = weights[opt.id] ?: opt.defaultMassGrams ?: 1000
            Row(Modifier.padding(start = 56.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(if (opt.id == "weight") R.string.eq_weight_each else R.string.eq_weight), Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                Stepper(grams / 500, { onWeights(weights + (opt.id to it * 500)) }, 1..80, valueText = { "%.1f kg".format(it * 0.5) }, tag = "weight_${opt.id}")
            }
        }
    }
}
