// U10 (F16, F15): the Progress tab. A family picker on top, one tree per family below (replaces the old library list),
// tap a node for the clip, how-to, steps, equipment and cautions. The selected goal's chain is outlined in the accent colour.
package io.github.gonbei774.calisthenicsmemory.ui.progress

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.calisthenics.domain.equipment.isAvailable
import app.calisthenics.domain.equipment.missingFor
import app.calisthenics.domain.goals.Goals
import app.calisthenics.domain.model.*
import app.calisthenics.domain.tree.*
import io.github.gonbei774.calisthenicsmemory.R
import io.github.gonbei774.calisthenicsmemory.ui.components.MuscleChips
import io.github.gonbei774.calisthenicsmemory.ui.components.StarRow
import app.calisthenics.domain.load.formatKg
import app.calisthenics.domain.progression.Suggestion
import app.calisthenics.domain.progression.suggest
import app.calisthenics.domain.load.isLoaded
import app.calisthenics.domain.load.loadGrams
import app.calisthenics.domain.load.loadEquipmentId
import app.calisthenics.domain.load.weightTrack
import app.calisthenics.domain.load.WeightState
import app.calisthenics.domain.load.DUMBBELL_EQUIPMENT_ID
import io.github.gonbei774.calisthenicsmemory.ui.screens.*
import io.github.gonbei774.calisthenicsmemory.ui.theme.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import io.github.gonbei774.calisthenicsmemory.ui.components.AppOutlinedButton
import io.github.gonbei774.calisthenicsmemory.ui.components.BodyMap
import io.github.gonbei774.calisthenicsmemory.ui.components.Caption
import io.github.gonbei774.calisthenicsmemory.ui.components.appSegmentedColors
import io.github.gonbei774.calisthenicsmemory.ui.theme.Spacing
import io.github.gonbei774.calisthenicsmemory.ui.train.rememberTrainData

private enum class ProgressMode { TYPE, SKILL }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProgressScreen(modifier: Modifier = Modifier) {
    val ctx = LocalContext.current
    var refresh by remember { mutableIntStateOf(0) }
    val data by rememberTrainData(refresh)
    val profile = remember { ProfileStore.selected(ctx) }
    var dismissed by remember { mutableStateOf(SuggestionStore.dismissed(ctx)) }
    val suggestion = remember(data, profile, dismissed) { suggest(data.catalog, profile, data.progress, dismissed) }
    val goalId = remember { GoalStore.load(ctx) }
    val goal = Goals.byId(goalId)
    val c = data.catalog
    val startTab = remember(c) { goal?.entryVariationId?.let { TreeTabs.tabForVariation(c, it) }?.id ?: "push" }
    var modeName by rememberSaveable { mutableStateOf(ProgressMode.TYPE.name) }
    val mode = ProgressMode.valueOf(modeName)
    var tabId by rememberSaveable { mutableStateOf(startTab) }
    val tab = TreeTabs.byId(tabId) ?: TreeTabs.all.first()
    val skills = remember { Goals.goalsOf(app.calisthenics.domain.goals.ObjectiveType.SKILL) }
    var skillId by rememberSaveable { mutableStateOf(goal?.takeIf { it.entryVariationId != null }?.id ?: skills.first().id) }
    val skill = skills.firstOrNull { it.id == skillId } ?: skills.first()
    var open by remember { mutableStateOf<String?>(null) }          // a stretch sheet
    var selected by rememberSaveable { mutableStateOf<String?>(null) }  // the ringed node, its panel is open
    var detailId by rememberSaveable { mutableStateOf<String?>(null) }  // the exercise detail page

    val detail = detailId?.let { c.variation(it) }
    if (detail != null) {
        androidx.activity.compose.BackHandler { detailId = null }
        ExerciseDetail(c, detail, data.progress, profile, TreeTabs.tabForVariation(c, detail.id)?.title.orEmpty(), modifier) { detailId = null }
        return
    }

    Scaffold(modifier = modifier) { pad ->
        Column(Modifier.padding(pad).fillMaxSize()) {
            Text(stringResource(R.string.tab_progress).uppercase(), style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(horizontal = Spacing.l, vertical = Spacing.s).testTag("progress_title"))
            suggestion?.let { s ->
                SuggestionCard(data.catalog, s, onDismiss = { SuggestionStore.dismiss(ctx, s.id); dismissed = dismissed + s.id },
                    onAccept = (s as? Suggestion.RaiseLevel)?.let { r -> {
                        SuggestionStore.accept(ctx, r.variationId, r.toTier, java.time.LocalDate.now().toEpochDay().toInt()); SuggestionStore.dismiss(ctx, r.id); dismissed = dismissed + r.id; refresh++
                    } })
            }
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().padding(horizontal = Spacing.l).height(48.dp)) {
                SegmentedButton(selected = mode == ProgressMode.TYPE, onClick = { modeName = ProgressMode.TYPE.name; selected = null }, colors = appSegmentedColors(), shape = SegmentedButtonDefaults.itemShape(0, 2), icon = {}, modifier = Modifier.testTag("progress_mode_type")) { Text(stringResource(R.string.progress_by_type)) }
                SegmentedButton(selected = mode == ProgressMode.SKILL, onClick = { modeName = ProgressMode.SKILL.name; selected = null }, colors = appSegmentedColors(), shape = SegmentedButtonDefaults.itemShape(1, 2), icon = {}, modifier = Modifier.testTag("progress_mode_skill")) { Text(stringResource(R.string.progress_by_skill)) }
            }
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = Spacing.l, vertical = Spacing.s), horizontalArrangement = Arrangement.spacedBy(Spacing.s)) {
                if (mode == ProgressMode.TYPE) TreeTabs.all.forEach { t ->
                    FilterChip(
                        selected = t.id == tab.id, onClick = { tabId = t.id; selected = null },
                        label = { Text(t.title) },
                        border = FilterChipDefaults.filterChipBorder(true, t.id == tab.id, borderColor = MaterialTheme.colorScheme.outlineVariant, selectedBorderColor = AppAccentTheme.colors.accent, selectedBorderWidth = 2.dp),
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = MaterialTheme.colorScheme.surface, selectedLabelColor = AppAccentTheme.colors.text),
                        modifier = Modifier.testTag("tab_chip_${t.id}"),
                    )
                } else skills.forEach { g ->
                    FilterChip(
                        selected = g.id == skill.id, onClick = { skillId = g.id; selected = null },
                        label = { Text(g.name) },
                        border = FilterChipDefaults.filterChipBorder(true, g.id == skill.id, borderColor = MaterialTheme.colorScheme.outlineVariant, selectedBorderColor = AppAccentTheme.colors.accent, selectedBorderWidth = 2.dp),
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = MaterialTheme.colorScheme.surface, selectedLabelColor = AppAccentTheme.colors.text),
                        modifier = Modifier.testTag("skill_chip_${g.id}"),
                    )
                }
            }
            if (mode == ProgressMode.SKILL) Text(skill.description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = Spacing.l).testTag("skill_description"))
            if (mode == ProgressMode.TYPE && goal != null && goal.entryVariationId != null) Text(
                stringResource(R.string.progress_goal, goal.name), Modifier.padding(horizontal = Spacing.l), style = MaterialTheme.typography.labelLarge, color = AppAccentTheme.colors.text,
            )
            if (mode == ProgressMode.TYPE && tab.id == TreeTabs.STRETCHES) StretchList(c, Modifier.weight(1f)) { open = it }
            else {
                val layout = remember(c, tab.id, mode, skill.id) { if (mode == ProgressMode.SKILL) skillLayout(c, skill) else layoutTreeVertical(c, tab.familyIds) }
                val now = remember(c, data.progress, goalId) { trainingNow(c, data.progress, goalId) }
                val goalChain = remember(c, goalId) { goal?.let { Goals.chain(c, it).toSet() } ?: emptySet() }
                val nodes = remember(layout, data.progress, profile, now, goalChain) {
                    layout.nodes.associate { pos ->
                        val v = c.variation(pos.variationId)!!
                        val state = nodeState(c, v.id, data.progress, profile, now)
                        val missing = v.equipmentAlternatives.minByOrNull { s -> s.needs.count { n -> !app.calisthenics.domain.equipment.needSatisfied(n, profile) } }
                            ?.needs?.firstOrNull { n -> !app.calisthenics.domain.equipment.needSatisfied(n, profile) }?.equipmentId
                        v.id to NodeUi(v.id, v.name, v.familyId, state, displayStars(c, data.progress, v.id), v.id in goalChain, missing, data.progress.tierFor(v.id) ?: 1)
                    }
                }
                val mastered = remember(nodes) { nodes.filterValues { it.state == TreeNodeState.MASTERED }.keys }
                Box(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).horizontalScroll(rememberScrollState()).testTag("tree_area")) {
                    SkillTreeCanvas(layout, nodes, mastered, selected, onNode = { selected = if (selected == it) null else it })
                }
                selected?.let { id -> c.variation(id)?.let { v -> nodes[id]?.let { n -> NodePanel(c, v, n, data.progress, profile, onOpen = { detailId = id }, onClose = { selected = null }) } } }
            }
        }
    }
    open?.let { id -> c.variation(id)?.let { v -> NodeSheet(c, v, data.progress, profile) { open = null } } }
}

/** V27 (R29): at most one suggestion; it can be closed and never blocks anything (D9). A raise can be accepted with one tap. */
@Composable
private fun SuggestionCard(c: Catalog, s: Suggestion, onDismiss: () -> Unit, onAccept: (() -> Unit)?) {
    fun name(id: String) = c.variation(id)?.name ?: id
    val text = when (s) {
        is Suggestion.RaiseLevel -> stringResource(R.string.suggest_raise, name(s.variationId), s.toTier)
        is Suggestion.HeavierWeight -> stringResource(R.string.suggest_weight, name(s.variationId), formatKg(s.nextGrams), stringResource(equipmentLabelShortPublic(s.equipmentId)))
        is Suggestion.GetEquipment -> stringResource(R.string.suggest_equipment, stringResource(equipmentLabelShortPublic(s.equipmentId)), name(s.unlocksVariationId))
    }
    Card(Modifier.fillMaxWidth().padding(horizontal = Spacing.l, vertical = Spacing.xs).testTag("suggestion_card"), shape = MaterialTheme.shapes.large) {
        Row(Modifier.padding(start = Spacing.m, top = Spacing.xs, bottom = Spacing.xs), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                Caption(stringResource(R.string.suggest_caption))
                Text(text, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.testTag("suggestion_text"))
                if (onAccept != null) AppOutlinedButton(onClick = onAccept, Modifier.height(40.dp).testTag("suggestion_accept")) { Text(stringResource(R.string.suggest_accept).uppercase()) }
            }
            IconButton(onClick = onDismiss, modifier = Modifier.testTag("suggestion_dismiss")) { Icon(Icons.Filled.Close, stringResource(R.string.suggest_dismiss)) }
        }
    }
}

/** Doc 17 §2.6: the panel above the tab bar after the first tap on a node: details, OPEN EXERCISE DETAIL, close. */
@Composable
private fun NodePanel(c: Catalog, v: ExerciseVariation, n: NodeUi, progress: app.calisthenics.domain.progression.ProgressSnapshot, profile: EquipmentProfile, onOpen: () -> Unit, onClose: () -> Unit) {
    val policy = c.policyFor(v)
    val tier = progress.tierFor(v.id) ?: 1
    val target = policy?.tiers?.firstOrNull { it.index == tier }?.target
    val unlocks = policy?.nextVariationIds.orEmpty().mapNotNull { c.variation(it)?.name }
    val unmet = Goals.unmet(c, v.id, progress).map { stringResource(R.string.sheet_step_name, c.variation(it.variationId)?.name ?: it.variationId, it.tier) }
    Card(Modifier.fillMaxWidth().padding(horizontal = Spacing.l, vertical = Spacing.s).testTag("node_panel"), shape = MaterialTheme.shapes.large) {
        Column(Modifier.padding(Spacing.m), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(v.name, style = MaterialTheme.typography.titleLarge, modifier = Modifier.testTag("panel_title"))
                    Text(stateLabel(n.state), style = MaterialTheme.typography.labelLarge, color = AppAccentTheme.colors.text, modifier = Modifier.testTag("panel_state"))
                }
                IconButton(onClick = onClose, modifier = Modifier.testTag("panel_close")) { Icon(Icons.Filled.Close, stringResource(R.string.close)) }
            }
            if (target != null) Text(stringResource(R.string.panel_level, tier, 5, if (target.type == TargetType.REPS) stringResource(R.string.target_reps, target.value) else stringResource(R.string.target_seconds, target.value)), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.testTag("panel_level"))
            if (unlocks.isNotEmpty()) Text(stringResource(R.string.panel_unlocks, unlocks.joinToString()), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.testTag("panel_unlocks"))
            if (unmet.isNotEmpty() && v.id !in progress.variations) Text(stringResource(R.string.panel_needs, unmet.joinToString()), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error, modifier = Modifier.testTag("panel_needs"))
            if (!isAvailable(v, profile)) Text(stringResource(R.string.sheet_missing, missingFor(v, profile)), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
            AppOutlinedButton(onClick = onOpen, Modifier.fillMaxWidth().height(48.dp).testTag("panel_open_detail")) { Text(stringResource(R.string.panel_open_detail).uppercase()) }
        }
    }
}

/** Doc 17 §2.7: the exercise detail, a sub-page of Progress. No action buttons here. */
@Composable
private fun ExerciseDetail(c: Catalog, v: ExerciseVariation, progress: app.calisthenics.domain.progression.ProgressSnapshot, profile: EquipmentProfile, familyTitle: String, modifier: Modifier, onBack: () -> Unit) {
    val ctx = LocalContext.current
    val policy = c.policyFor(v)
    val tier = progress.tierFor(v.id) ?: 1
    val achieved = progress.variations[v.id]?.achievedTiers ?: emptySet()
    val unlocks = policy?.nextVariationIds.orEmpty().mapNotNull { c.variation(it)?.name }
    val needs = Goals.unmet(c, v.id, progress).map { stringResource(R.string.sheet_step_name, c.variation(it.variationId)?.name ?: it.variationId, it.tier) }
    val dark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).statusBarsPadding().padding(horizontal = Spacing.l).padding(bottom = Spacing.xl).testTag("exercise_detail"), verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack, modifier = Modifier.testTag("detail_back")) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back)) }
            Caption(stringResource(R.string.detail_breadcrumb, familyTitle, tier, 5), Modifier.testTag("detail_breadcrumb"))
        }
        Text(v.name.uppercase(), style = MaterialTheme.typography.headlineMedium, modifier = Modifier.testTag("detail_title"))
        StarRow(displayStars(c, progress, v.id), size = 22.dp)
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.m), verticalAlignment = Alignment.CenterVertically) {
            // the clip stays on its white background in both themes (doc 17)
            Box(Modifier.weight(1f).clip(RoundedCornerShape(12.dp)).background(Color.White)) { DemoClips.file(ctx, v.id)?.let { DemoPlayer(it, controls = false) } }
            if (v.primaryMuscles.isNotEmpty()) BodyMap(v.primaryMuscles, v.secondaryMuscles, showNamesOnTap = true, tag = "detail_body_map", viewWidth = 44.dp)
        }
        if (policy != null) {
            Caption(stringResource(R.string.detail_levels))
            policy.tiers.forEach { t ->
                val state = when { t.index in achieved || t.index < tier -> R.string.detail_done; t.index == tier -> R.string.detail_now; else -> R.string.detail_next }
                val value = if (t.target.type == TargetType.REPS) stringResource(R.string.target_reps, t.target.value) else stringResource(R.string.target_seconds, t.target.value)
                Row(Modifier.fillMaxWidth().heightIn(min = 40.dp).semantics(mergeDescendants = true) {}.testTag("detail_level_${t.index}"), verticalAlignment = Alignment.CenterVertically) {
                    Text("${t.index}", style = MaterialTheme.typography.titleLarge, modifier = Modifier.width(32.dp), color = if (t.index == tier) AppAccentTheme.colors.text else MaterialTheme.colorScheme.onSurface)
                    Text(value, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                    Text(stringResource(state).uppercase(), style = MaterialTheme.typography.labelLarge, color = if (t.index == tier) AppAccentTheme.colors.text else MaterialTheme.colorScheme.outline)
                }
            }
        }
        if (v.isLoaded()) WeightTrack(v, progress, profile)
        if (unlocks.isNotEmpty()) { Caption(stringResource(R.string.detail_unlocks)); Text(unlocks.joinToString(), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.testTag("detail_unlocks")) }
        if (needs.isNotEmpty() || !isAvailable(v, profile)) {
            Caption(stringResource(R.string.detail_needs))
            Text((needs + if (!isAvailable(v, profile)) listOf(missingFor(v, profile)) else emptyList()).joinToString(), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.testTag("detail_needs"))
        }
        if (v.cautions.isNotEmpty()) {
            val bg = if (dark) CautionDarkSurface else CautionLightSurface; val br = if (dark) CautionDarkBorder else CautionLightBorder; val tx = if (dark) CautionDarkText else CautionLightText
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(bg).border(1.dp, br, RoundedCornerShape(12.dp)).padding(Spacing.m).testTag("detail_caution"), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(stringResource(R.string.sheet_cautions).uppercase(), style = MaterialTheme.typography.labelLarge, color = tx)
                v.cautions.forEach { Text(it, style = MaterialTheme.typography.bodyMedium, color = tx) }
            }
        }
    }
}

/** V26 (R27, O5): the weight track of a kettlebell or dumbbell exercise: the weights from the one in use upward, what is kept, what is locked. */
@Composable
private fun WeightTrack(v: ExerciseVariation, progress: app.calisthenics.domain.progression.ProgressSnapshot, profile: EquipmentProfile) {
    val eq = v.loadEquipmentId() ?: return
    val vp = progress.variations[v.id]
    val steps = weightTrack(eq, profile.loadGrams(eq), vp?.loadGrams, vp?.earnedStars() ?: 0, vp?.starsByLoad ?: emptyMap())
    if (steps.isEmpty()) return
    Caption(stringResource(R.string.detail_weight), Modifier.padding(top = Spacing.s))
    steps.forEach { st ->
        val kg = formatKg(st.grams)
        val label = when (st.state) {
            WeightState.NOW -> stringResource(R.string.weight_now)
            WeightState.DONE -> stringResource(R.string.weight_done)
            WeightState.READY -> stringResource(R.string.weight_ready)
            WeightState.LOCKED -> stringResource(if (eq == DUMBBELL_EQUIPMENT_ID) R.string.weight_locked_db else R.string.weight_locked_kb, kg)
        }
        Row(Modifier.fillMaxWidth().heightIn(min = 40.dp).semantics(mergeDescendants = true) {}.testTag("weight_step_${st.grams}"), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.s)) {
            Text(stringResource(R.string.weight_kg, kg), Modifier.width(72.dp), style = MaterialTheme.typography.titleMedium,
                color = if (st.state == WeightState.NOW) AppAccentTheme.colors.text else MaterialTheme.colorScheme.onSurface)
            if (st.state != WeightState.LOCKED) StarRow(st.stars, size = 16.dp)
            Text(label.uppercase(), Modifier.weight(1f), style = MaterialTheme.typography.labelLarge, textAlign = androidx.compose.ui.text.style.TextAlign.End,
                color = if (st.state == WeightState.NOW) AppAccentTheme.colors.text else MaterialTheme.colorScheme.outline)
        }
    }
}

@Composable
private fun StretchList(c: Catalog, modifier: Modifier, onOpen: (String) -> Unit) {
    Column(modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(Spacing.l), verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
        c.variations.filter { it.kind == Kind.STRETCH || it.kind == Kind.MOBILITY }.forEach { v ->
            Card(Modifier.fillMaxWidth().clickable { onOpen(v.id) }.testTag("stretch_${v.id}"), shape = MaterialTheme.shapes.large) {
                Row(Modifier.padding(Spacing.m), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.m)) {
                    io.github.gonbei774.calisthenicsmemory.ui.components.ExerciseThumb(v.id, v.name, 44.dp)
                    Text(v.name, Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }
}

/** V08: the body figure with the muscle chips next to it. */
@Composable
private fun MusclesRow(v: ExerciseVariation) {
    if (v.primaryMuscles.isEmpty()) return
    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.m), verticalAlignment = Alignment.CenterVertically) {
        io.github.gonbei774.calisthenicsmemory.ui.components.BodyMap(v.primaryMuscles, v.secondaryMuscles, showNamesOnTap = false, tag = "sheet_body_map")
        MuscleChips(v.primaryMuscles, v.secondaryMuscles, Modifier.weight(1f))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NodeSheet(c: Catalog, v: ExerciseVariation, progress: app.calisthenics.domain.progression.ProgressSnapshot, profile: EquipmentProfile, onDismiss: () -> Unit) {
    val ctx = LocalContext.current
    val policy = c.policyFor(v)
    val tier = progress.tierFor(v.id)
    val strength = v.kind == Kind.REPS || v.kind == Kind.HOLD
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.padding(horizontal = Spacing.l).padding(bottom = Spacing.xl).verticalScroll(rememberScrollState()).testTag("node_sheet"), verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
            Text(v.name, style = MaterialTheme.typography.titleLarge, modifier = Modifier.testTag("sheet_title"))
            if (strength) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.s)) {
                    StarRow(stars(progress, v.id), size = 20.dp)
                    Text(stateLabel(nodeState(c, v.id, progress, profile, emptySet())), style = MaterialTheme.typography.labelLarge)
                }
                MusclesRow(v)
                // Kettlebell: levels count per weight. Show the weight in use and stars kept from lighter bells.
                if (v.isLoaded()) {
                    val vp = progress.variations[v.id]
                    val kg = (vp?.loadGrams ?: profile.loadGrams())?.let { formatKg(it) }
                    if (kg != null) Text(stringResource(R.string.sheet_load_now, kg), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.testTag("sheet_load"))
                    vp?.starsByLoad?.toSortedMap()?.forEach { (g, n) ->
                        Text(stringResource(R.string.sheet_load_earlier, formatKg(g), n), style = MaterialTheme.typography.bodySmall, modifier = Modifier.testTag("sheet_load_${g}"))
                    }
                }
            }
            if (!strength) MusclesRow(v)
            DemoClips.file(ctx, v.id)?.let { DemoPlayer(it) }
            SheetSection(R.string.sheet_how, v.instructions.mapIndexed { i, t -> stringResource(R.string.numbered_item, i + 1, t) })
            SheetSection(R.string.sheet_cues, v.formCues.map { stringResource(R.string.bullet_item, it) })
            if (policy != null) {
                Text(stringResource(R.string.sheet_steps), style = MaterialTheme.typography.titleMedium)
                policy.tiers.forEach { t ->
                    val here = tier == t.index
                    Text(
                        stringResource(if (here) R.string.sheet_step_here else R.string.sheet_step, t.index, if (t.target.type == TargetType.REPS) stringResource(R.string.target_reps, t.target.value) else stringResource(R.string.target_seconds, t.target.value)),
                        style = if (here) MaterialTheme.typography.titleSmall else MaterialTheme.typography.bodyMedium, modifier = Modifier.testTag("sheet_step_${t.index}"),
                    )
                }
            }
            val unmet = Goals.unmet(c, v.id, progress)
            if (unmet.isNotEmpty() && v.id !in progress.variations) Text(
                stringResource(R.string.sheet_needs_first, unmet.map { stringResource(R.string.sheet_step_name, c.variation(it.variationId)?.name ?: it.variationId, it.tier) }.joinToString()),
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error,
            )
            Text(stringResource(R.string.sheet_equipment), style = MaterialTheme.typography.titleMedium)
            val noEquipment = stringResource(R.string.sheet_no_equipment)
            val need = v.equipmentAlternatives.map { set ->
                val items = set.needs.map { stringResource(equipmentLabelShortPublic(it.equipmentId)) } + set.capabilities.map { stringResource(equipmentLabelShortPublic(it)) }
                if (items.isEmpty()) noEquipment else items.joinToString(" + ")
            }.joinToString(stringResource(R.string.sheet_or))
            Text(need, style = MaterialTheme.typography.bodyMedium)
            if (!isAvailable(v, profile)) Text(stringResource(R.string.sheet_missing, missingFor(v, profile)), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
            SheetSection(R.string.sheet_cautions, v.cautions.map { stringResource(R.string.bullet_item, it) }, error = true)
            if (v.sourceIds.isNotEmpty()) Text(stringResource(R.string.sheet_sources, v.sourceIds.joinToString()), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(stringResource(R.string.sheet_draft), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun SheetSection(title: Int, lines: List<String>, error: Boolean = false) {
    if (lines.isEmpty()) return
    Text(stringResource(title), style = MaterialTheme.typography.titleMedium, color = if (error) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface)
    lines.forEach { Text(it, style = MaterialTheme.typography.bodyMedium) }
}
