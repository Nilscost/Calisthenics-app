// U04 (F5): profile list (Settings) and the profile editor with an equipment checklist.
package io.github.gonbei774.calisthenicsmemory.ui.screens

import org.jetbrains.compose.resources.StringResource
import io.github.gonbei774.calisthenicsmemory.platform.formatOneDecimal
import io.github.gonbei774.calisthenicsmemory.platform.currentPlatformContext
import io.github.gonbei774.calisthenicsmemory.platform.PlatformBackHandler
import io.github.gonbei774.calisthenicsmemory.ui.components.AppTextButton
import io.github.gonbei774.calisthenicsmemory.ui.components.AppOutlinedButton

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import io.github.gonbei774.calisthenicsmemory.ui.icons.Icons
import io.github.gonbei774.calisthenicsmemory.ui.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import app.calisthenics.domain.equipment.*
import app.calisthenics.domain.model.EquipmentProfile
import io.github.gonbei774.calisthenicsmemory.shared.resources.*
import io.github.gonbei774.calisthenicsmemory.ui.components.Caption
import io.github.gonbei774.calisthenicsmemory.ui.components.Stepper
import io.github.gonbei774.calisthenicsmemory.ui.theme.EquipmentIcons
import io.github.gonbei774.calisthenicsmemory.ui.theme.Spacing

fun equipmentGroupLabel(g: EquipmentGroup): StringResource = when (g) {
    EquipmentGroup.PULL -> Res.string.eq_group_pull; EquipmentGroup.PUSH -> Res.string.eq_group_push; EquipmentGroup.LEGS -> Res.string.eq_group_legs
    EquipmentGroup.CORE -> Res.string.eq_group_core; EquipmentGroup.WEIGHTS -> Res.string.eq_group_weights; EquipmentGroup.OTHER -> Res.string.eq_group_other
}

fun equipmentLabel(id: String): StringResource = when (id) {
    "pullup-bar" -> Res.string.eq_pullup_bar
    "high-bar" -> Res.string.eq_high_bar
    "low-bar" -> Res.string.eq_low_bar
    "resistance-band" -> Res.string.eq_band
    "kettlebell" -> Res.string.eq_kettlebell
    "weight" -> Res.string.eq_dumbbells
    "chair" -> Res.string.eq_chair
    "wall" -> Res.string.eq_wall
    "mat" -> Res.string.eq_mat
    "dip-support" -> Res.string.eq_dip_support
    "barbell" -> Res.string.eq_barbell
    "weighted-vest" -> Res.string.eq_vest
    "foot-anchor" -> Res.string.eq_foot_anchor
    "parallettes" -> Res.string.eq_parallettes
    "rings" -> Res.string.eq_rings
    "ab-wheel" -> Res.string.eq_ab_wheel
    "step" -> Res.string.eq_step
    "box" -> Res.string.eq_box
    "bench" -> Res.string.eq_bench
    "pole" -> Res.string.eq_pole
    "sliders" -> Res.string.eq_sliders
    "jump-rope" -> Res.string.eq_jump_rope
    else -> Res.string.eq_other
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfilesScreen(modifier: Modifier = Modifier, onBack: () -> Unit, onEdit: (String?) -> Unit) {
    val ctx = currentPlatformContext()
    val profiles = remember { ProfileStore.load(ctx) }
    Scaffold(modifier = modifier, topBar = {
        TopAppBar(title = { Text(stringResource(Res.string.profiles_title)) },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(Res.string.back)) } })
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
            Button(onClick = { onEdit(null) }, modifier = Modifier.fillMaxWidth().testTag("profile_add")) { Text(stringResource(Res.string.profile_add)) }
        }
    }
}

@Composable
private fun equipmentSummary(p: EquipmentProfile): String {
    // R8.1-T11 (F14): in the shared movement-area order
    val ids = (p.items.map { it.equipmentId } + p.capabilities.filter { EquipmentOptions.byId(it)?.kind == OptionKind.CAPABILITY }).sortedBy { EquipmentOptions.orderOf(it) }
    val labels = ids.map { stringResource(equipmentLabelShort(it)) }
    return if (labels.isEmpty()) stringResource(Res.string.profile_no_equipment) else labels.joinToString(", ")
}

fun equipmentLabelShortPublic(id: String): StringResource = equipmentLabelShort(id)

private fun equipmentLabelShort(id: String): StringResource = when (id) {
    "pullup-bar" -> Res.string.eq_pullup_bar_short
    "high-bar" -> Res.string.eq_high_bar_short
    "low-bar" -> Res.string.eq_low_bar_short
    "resistance-band" -> Res.string.eq_band_short
    "kettlebell" -> Res.string.eq_kettlebell_short
    "weight" -> Res.string.eq_dumbbells_short
    "chair" -> Res.string.eq_chair_short
    "wall" -> Res.string.eq_wall_short
    "mat" -> Res.string.eq_mat_short
    "dip-support" -> Res.string.eq_dip_support_short
    "barbell" -> Res.string.eq_barbell_short
    "weighted-vest" -> Res.string.eq_vest_short
    "foot-anchor" -> Res.string.eq_foot_anchor_short
    "parallettes" -> Res.string.eq_parallettes_short
    "rings" -> Res.string.eq_rings_short
    "ab-wheel" -> Res.string.eq_ab_wheel_short
    "step" -> Res.string.eq_step_short
    "box" -> Res.string.eq_box_short
    "bench" -> Res.string.eq_bench_short
    "pole" -> Res.string.eq_pole_short
    "sliders" -> Res.string.eq_sliders_short
    "jump-rope" -> Res.string.eq_jump_rope_short
    else -> Res.string.eq_other
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileEditScreen(modifier: Modifier = Modifier, profileId: String?, onDone: () -> Unit) {
    val ctx = currentPlatformContext()
    val profiles = remember { ProfileStore.load(ctx) }
    val existing = remember(profileId) { profiles.firstOrNull { it.id == profileId } }
    var name by rememberSaveable { mutableStateOf(existing?.name ?: "") }
    var picked by remember { mutableStateOf(existing?.let { selectionOf(it).map { s -> s.id } } ?: listOf("mat")) }
    var weights by remember { mutableStateOf<Map<String, Int>>(existing?.let { selectionOf(it).filter { s -> s.massGrams != null }.associate { s -> s.id to s.massGrams!! } } ?: emptyMap()) }
    var confirmDelete by remember { mutableStateOf(false) }
    var confirmLeave by remember { mutableStateOf(false) }
    val initialName = existing?.name ?: ""
    val initialPicked = existing?.let { selectionOf(it).map { s -> s.id } } ?: listOf("mat")
    val initialWeights = existing?.let { selectionOf(it).filter { s -> s.massGrams != null }.associate { s -> s.id to s.massGrams!! } } ?: emptyMap()
    // Unsaved edits: name, ticked equipment, or the weights of ticked items.
    val dirty = name != initialName || picked.toSet() != initialPicked.toSet() ||
        picked.any { weights[it] != initialWeights[it] }
    fun back() { if (dirty) confirmLeave = true else onDone() }
    // Innermost enabled handler wins over AppNav's route-level one.
    PlatformBackHandler(enabled = dirty) { confirmLeave = true }
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
            title = { Text(stringResource(if (existing == null) Res.string.profile_new else Res.string.profile_edit)) },
            navigationIcon = { IconButton(onClick = { back() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(Res.string.back)) } },
            actions = { AppTextButton(onClick = { save() }, modifier = Modifier.testTag("profile_save")) { Text(stringResource(Res.string.save)) } },
        )
    }) { pad ->
        Column(Modifier.padding(pad).verticalScroll(rememberScrollState()).padding(Spacing.l), verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
            OutlinedTextField(
                value = name, onValueChange = { name = it.take(30); showProblem = false }, singleLine = true,
                label = { Text(stringResource(Res.string.profile_name)) },
                isError = showProblem && problem != null,
                supportingText = {
                    if (showProblem && problem != null) Text(stringResource(if (problem == NameProblem.EMPTY) Res.string.profile_name_empty else Res.string.profile_name_duplicate))
                },
                modifier = Modifier.fillMaxWidth().testTag("profile_name"),
            )
            Text(stringResource(Res.string.profile_equipment), style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = Spacing.s))
            EquipmentChecklist(picked, { picked = it }, weights, { weights = it })
            if (existing != null && profiles.size > 1) {
                AppOutlinedButton(onClick = { confirmDelete = true }, modifier = Modifier.fillMaxWidth().padding(top = Spacing.l).testTag("profile_delete")) {
                    Text(stringResource(Res.string.profile_delete))
                }
            }
        }
    }
    if (confirmLeave) {
        AlertDialog(
            onDismissRequest = { confirmLeave = false },
            title = { Text(if (existing == null) stringResource(Res.string.profile_unsaved_new) else stringResource(Res.string.profile_unsaved_title, existing.name)) },
            text = { Text(stringResource(Res.string.profile_unsaved_text)) },
            confirmButton = { AppTextButton(onClick = { confirmLeave = false; save() }, modifier = Modifier.testTag("profile_leave_save")) { Text(stringResource(Res.string.save)) } },
            dismissButton = { AppTextButton(onClick = { confirmLeave = false; onDone() }, modifier = Modifier.testTag("profile_leave_discard")) { Text(stringResource(Res.string.profile_discard)) } },
        )
    }
    if (confirmDelete && existing != null) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(stringResource(Res.string.profile_delete_title, existing.name)) },
            text = { Text(stringResource(Res.string.profile_delete_text)) },
            confirmButton = { AppTextButton(onClick = { ProfileStore.delete(ctx, existing.id); confirmDelete = false; onDone() }) { Text(stringResource(Res.string.profile_delete)) } },
            dismissButton = { AppTextButton(onClick = { confirmDelete = false }) { Text(stringResource(Res.string.cancel)) } },
        )
    }
}

/** The equipment rows shared by the profile editor and the questionnaire. [weights] are grams for weighted rows. */
@Composable
fun EquipmentChecklist(picked: List<String>, onPicked: (List<String>) -> Unit, weights: Map<String, Int>, onWeights: (Map<String, Int>) -> Unit) {
    var lastGroup: EquipmentGroup? = null
    EquipmentOptions.all.forEach { opt ->
        // R8.1-T11 (F14): listed by movement area, with the same names as the groups of the Progress tabs
        if (opt.group != lastGroup) {
            lastGroup = opt.group
            Caption(stringResource(equipmentGroupLabel(opt.group)), Modifier.padding(top = Spacing.s).testTag("eq_group_${opt.group.name}"))
        }
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
                Text(stringResource(if (opt.id == "weight") Res.string.eq_weight_each else Res.string.eq_weight), Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                Stepper(grams / 500, { onWeights(weights + (opt.id to it * 500)) }, 1..80, valueText = { formatOneDecimal(it * 0.5) + " kg" }, tag = "weight_${opt.id}")
            }
        }
    }
}
