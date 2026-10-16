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
import io.github.gonbei774.calisthenicsmemory.ui.components.AppTextButton
import app.calisthenics.domain.progression.storedForLevelsDone
import io.github.gonbei774.calisthenicsmemory.ui.nav.OpenExercise
import io.github.gonbei774.calisthenicsmemory.ui.screens.LevelStore
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
    var catalogQuery by rememberSaveable { mutableStateOf("") }
    var showAllProgressions by rememberSaveable { mutableStateOf(false) }
    var selectedRouteId by rememberSaveable { mutableStateOf("") }
    var showRouteExtras by rememberSaveable { mutableStateOf(false) }
    // R7-T11 (D6): the exercise detail pages open on a small stack, so Back returns to the detail you came from
    var detailStack by rememberSaveable { mutableStateOf(OpenExercise.take() ?: "") }   // ids joined by ","
    val stack = detailStack.split(',').filter { it.isNotEmpty() }
    fun openDetail(id: String) { detailStack = (stack + id).joinToString(",") }
    fun closeDetail() { detailStack = stack.dropLast(1).joinToString(",") }

    val detail = stack.lastOrNull()?.let { c.variation(it) }
    if (detail != null) {
        androidx.activity.compose.BackHandler { closeDetail() }
        ExerciseDetail(c, detail, data.progress, profile, TreeTabs.tabForVariation(c, detail.id)?.title.orEmpty(), modifier, onOpen = { openDetail(it) }, onLevelsDone = { n ->
            val levels = LevelStore.load(ctx).toMutableMap()
            if (n == null) levels.remove(detail.id) else levels[detail.id] = storedForLevelsDone(n)
            LevelStore.save(ctx, levels); refresh++
        }) { closeDetail() }
        return
    }

    Scaffold(modifier = modifier) { pad ->
        Column(Modifier.padding(pad).fillMaxSize()) {
            Text(stringResource(R.string.tab_progress).uppercase(), style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(horizontal = Spacing.l, vertical = Spacing.s).testTag("progress_title"))
            suggestion?.let { s ->
                SuggestionCard(data.catalog, s, onDismiss = { SuggestionStore.dismiss(ctx, s.id); dismissed = dismissed + s.id },
                    onAccept = when (s) {
                        is Suggestion.RaiseLevel -> { {
                            SuggestionStore.accept(ctx, s.variationId, s.toTier, java.time.LocalDate.now().toEpochDay().toInt()); SuggestionStore.dismiss(ctx, s.id); dismissed = dismissed + s.id; refresh++
                        } }
                        is Suggestion.NextExercise -> { {
                            SuggestionStore.acceptNext(ctx, s.variationId, s.toVariationId, java.time.LocalDate.now().toEpochDay().toInt()); SuggestionStore.dismiss(ctx, s.id); dismissed = dismissed + s.id; refresh++
                        } }
                        else -> null
                    })
            }
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().padding(horizontal = Spacing.l).height(48.dp)) {
                SegmentedButton(selected = mode == ProgressMode.TYPE, onClick = { modeName = ProgressMode.TYPE.name; selected = null; selectedRouteId = "" }, colors = appSegmentedColors(), shape = SegmentedButtonDefaults.itemShape(0, 2), icon = {}, modifier = Modifier.testTag("progress_mode_type")) { Text(stringResource(R.string.progress_by_type)) }
                SegmentedButton(selected = mode == ProgressMode.SKILL, onClick = { modeName = ProgressMode.SKILL.name; selected = null; selectedRouteId = "" }, colors = appSegmentedColors(), shape = SegmentedButtonDefaults.itemShape(1, 2), icon = {}, modifier = Modifier.testTag("progress_mode_skill")) { Text(stringResource(R.string.progress_by_skill)) }
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
                        selected = g.id == skill.id, onClick = { skillId = g.id; selected = null; selectedRouteId = "" },
                        label = { Text(g.name) },
                        border = FilterChipDefaults.filterChipBorder(true, g.id == skill.id, borderColor = MaterialTheme.colorScheme.outlineVariant, selectedBorderColor = AppAccentTheme.colors.accent, selectedBorderWidth = 2.dp),
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = MaterialTheme.colorScheme.surface, selectedLabelColor = AppAccentTheme.colors.text),
                        modifier = Modifier.testTag("skill_chip_${g.id}"),
                    )
                }
            }
            OutlinedTextField(
                value = catalogQuery, onValueChange = { catalogQuery = it }, singleLine = true,
                label = { Text(stringResource(R.string.progress_search)) },
                modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.l).testTag("progress_search"),
            )
            if (catalogQuery.isNotBlank()) {
                val matches = remember(c, catalogQuery) {
                    val q = catalogQuery.trim()
                    c.variations.filter { v -> !v.routineOnly || c.routesFor(v.id).any { route -> route.members.any { it.variationId == v.id && it.role in setOf(ProgressionRole.PROGRAM, ProgressionRole.LIBRARY) } } }
                        .filter { v -> v.name.contains(q, true) || v.aliases.any { it.contains(q, true) } }
                        .sortedBy { it.name.lowercase() }.take(12)
                }
                Column(Modifier.fillMaxWidth().heightIn(max = 180.dp).verticalScroll(rememberScrollState()).testTag("progress_search_results")) {
                    matches.forEach { v ->
                        Column(Modifier.fillMaxWidth().heightIn(min = 48.dp).clickable { detailStack = v.id; selected = null }
                            .padding(horizontal = Spacing.l, vertical = Spacing.s).testTag("search_result_${v.id}")) {
                            Text(v.name, style = MaterialTheme.typography.bodyLarge)
                            val roles = c.routesFor(v.id).flatMap { route -> route.members.filter { it.variationId == v.id }.map { it.role } }.toSet()
                            val libraryLabel = when {
                                ProgressionRole.PROGRAM in roles -> stringResource(R.string.progress_program_only)
                                ProgressionRole.LIBRARY in roles -> stringResource(R.string.progress_library_only)
                                else -> null
                            }
                            if (libraryLabel != null) Text(libraryLabel, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            if (v.demonstrationHold) Text(stringResource(R.string.progress_teaching_hold), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    if (matches.isEmpty()) Text(stringResource(R.string.progress_search_empty), Modifier.padding(horizontal = Spacing.l, vertical = Spacing.s), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            if (mode == ProgressMode.SKILL) Text(skill.description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = Spacing.l).testTag("skill_description"))
            if (mode == ProgressMode.TYPE && goal != null && goal.entryVariationId != null) Text(
                stringResource(R.string.progress_goal, goal.name), Modifier.padding(horizontal = Spacing.l), style = MaterialTheme.typography.labelLarge, color = AppAccentTheme.colors.text,
            )
            if (mode == ProgressMode.TYPE && tab.id == TreeTabs.STRETCHES) StretchList(c, Modifier.weight(1f)) { open = it }
            else {
                val routeGoalId = if (mode == ProgressMode.SKILL) skill.id else goalId
                val availableRoutes = remember(c, tab.id, showAllProgressions, routeGoalId, mode, profile) {
                    c.progressionRoutes.filter { route ->
                        val primaryFamily = route.mainPath.firstNotNullOfOrNull { c.variation(it)?.familyId }
                        val inTab = primaryFamily in tab.familyIds
                        val isGoal = routeGoalId in route.goalIds
                        val hasUsableEntry = route.mainPath.any { id -> c.variation(id)?.let { isAvailable(it, profile) } == true } ||
                            route.members.any { it.role == ProgressionRole.ALTERNATE && c.variation(it.variationId)?.let { v -> isAvailable(v, profile) } == true }
                        route.mainPath.isNotEmpty() && (if (mode == ProgressMode.SKILL) isGoal else inTab || showAllProgressions) &&
                            (mode == ProgressMode.SKILL || showAllProgressions || route.defaultPool || isGoal) && hasUsableEntry
                    }.distinctBy { it.id }
                }
                val activeRoute = availableRoutes.firstOrNull { it.id == selectedRouteId } ?:
                    availableRoutes.firstOrNull { routeGoalId in it.goalIds } ?: if (mode == ProgressMode.SKILL) null else availableRoutes.firstOrNull()
                if (mode == ProgressMode.TYPE && c.progressionRoutes.isNotEmpty()) {
                    Row(Modifier.fillMaxWidth().padding(horizontal = Spacing.l), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { showAllProgressions = !showAllProgressions; selectedRouteId = ""; selected = null }, modifier = Modifier.testTag("all_progressions")) {
                            Text(stringResource(if (showAllProgressions) R.string.progress_default_progressions else R.string.progress_all_progressions))
                        }
                    }
                    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = Spacing.l, vertical = Spacing.xs), horizontalArrangement = Arrangement.spacedBy(Spacing.s)) {
                        availableRoutes.forEach { route ->
                            FilterChip(selected = route.id == activeRoute?.id, onClick = { selectedRouteId = route.id; selected = null; showRouteExtras = false },
                                label = { Text(route.name) }, modifier = Modifier.testTag("route_chip_${route.id}"))
                        }
                    }
                } else if (mode == ProgressMode.SKILL && availableRoutes.size > 1) {
                    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = Spacing.l, vertical = Spacing.xs), horizontalArrangement = Arrangement.spacedBy(Spacing.s)) {
                        availableRoutes.forEach { route ->
                            FilterChip(selected = route.id == activeRoute?.id, onClick = { selectedRouteId = route.id; selected = null },
                                label = { Text(route.name) }, modifier = Modifier.testTag("skill_route_chip_${route.id}"))
                        }
                    }
                }
                val layout = remember(c, tab.id, mode, skill.id, activeRoute?.id) {
                    if (activeRoute != null) TreeLayout(activeRoute.mainPath.mapIndexed { index, id -> TreeNodeLayout(id, 0, index) }, activeRoute.mainPath.zipWithNext().map { (a, b) -> TreeEdge(a, b, EdgeKind.NEXT) }, if (activeRoute.mainPath.isEmpty()) 0 else 1, activeRoute.mainPath.size)
                    else if (c.progressionRoutes.isNotEmpty()) TreeLayout(emptyList(), emptyList(), 0, 0)
                    else layoutTreeVertical(c, tab.familyIds)
                }
                val now = remember(c, data.progress, goalId) { trainingNow(c, data.progress, goalId) }
                val goalChain = remember(c, goalId) { goal?.let { Goals.chain(c, it).toSet() } ?: emptySet() }
                val nodes = remember(layout, data.progress, profile, now, goalChain, mode, tab.id) {
                    layout.nodes.associate { pos ->
                        val v = c.variation(pos.variationId)!!
                        val state = nodeState(c, v.id, data.progress, profile, now)
                        val missing = v.equipmentAlternatives.minByOrNull { s -> s.needs.count { n -> !app.calisthenics.domain.equipment.needSatisfied(n, profile) } }
                            ?.needs?.firstOrNull { n -> !app.calisthenics.domain.equipment.needSatisfied(n, profile) }?.equipmentId
                        val needs = if (mode == ProgressMode.TYPE && v.id !in data.progress.variations) Goals.unmet(c, v.id, data.progress).map { it.variationId }.firstOrNull { c.variation(it)?.familyId !in tab.familyIds }?.let { it to (c.variation(it)?.name ?: it) } else null
                        v.id to NodeUi(v.id, v.name, v.familyId, state, displayStars(c, data.progress, v.id), v.id in goalChain, missing, data.progress.tierFor(v.id) ?: 1, needs)
                    }
                }
                val mastered = remember(nodes) { nodes.filterValues { it.state == TreeNodeState.MASTERED }.keys }
                if (c.progressionRoutes.isNotEmpty() && activeRoute == null) {
                    Text(stringResource(R.string.progress_no_routes), Modifier.weight(1f).fillMaxWidth().padding(Spacing.l).testTag("route_empty"), color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else SkillTreeCanvas(layout, nodes, mastered, selected, onNode = { selected = if (selected == it) null else it },
                    modifier = Modifier.weight(1f).fillMaxWidth().testTag("tree_area"), onNeed = { detailStack = it })
                if (activeRoute != null && selected != null) {
                    val extras = activeRoute.members.filter { it.atVariationId == selected && it.role in setOf(ProgressionRole.ALTERNATE, ProgressionRole.SUPPORT) }
                        .filter { member -> c.variation(member.variationId)?.let { isAvailable(it, profile) } == true }
                    if (extras.isNotEmpty()) {
                        TextButton(onClick = { showRouteExtras = !showRouteExtras }, modifier = Modifier.padding(horizontal = Spacing.l).testTag("route_extras_toggle")) {
                            Text(stringResource(if (showRouteExtras) R.string.progress_hide_extras else R.string.progress_show_extras))
                        }
                        if (showRouteExtras) Column(Modifier.fillMaxWidth().heightIn(max = 144.dp).verticalScroll(rememberScrollState())) {
                            extras.groupBy { it.role }.forEach { (role, members) ->
                                Text(stringResource(if (role == ProgressionRole.ALTERNATE) R.string.progress_other_ways else R.string.progress_helps_with), Modifier.padding(horizontal = Spacing.l), style = MaterialTheme.typography.labelLarge)
                                members.forEach { member -> c.variation(member.variationId)?.let { v ->
                                    Text(v.name, Modifier.fillMaxWidth().heightIn(min = 40.dp).clickable { detailStack = v.id }.padding(horizontal = Spacing.l, vertical = Spacing.xs).testTag("route_member_${v.id}"), style = MaterialTheme.typography.bodyMedium)
                                } }
                            }
                        }
                    }
                }
                selected?.let { id -> c.variation(id)?.let { v -> nodes[id]?.let { n -> NodePanel(c, v, n, data.progress, profile, onOpen = { detailStack = id }, onOpenOther = { o -> detailStack = o }, onClose = { selected = null }) } } }
            }
        }
    }
    open?.let { id -> c.variation(id)?.let { v -> NodeSheet(c, v, data.progress, profile) { open = null } } }
}

/** The equipment the cheapest alternative still lacks, with the names the person sees in the profile editor (not the ids). */
@Composable
private fun missingLabel(v: ExerciseVariation, profile: EquipmentProfile): String {
    val best = v.equipmentAlternatives.minByOrNull { set -> set.needs.count { !app.calisthenics.domain.equipment.needSatisfied(it, profile) } + (set.capabilities - profile.capabilities).size } ?: return ""
    val ids = best.needs.filter { !app.calisthenics.domain.equipment.needSatisfied(it, profile) }.map { it.equipmentId } + (best.capabilities - profile.capabilities)
    val names = ids.distinct().map { stringResource(equipmentLabelShortPublic(it)) }
    return names.joinToString()
}

/** V27 (R29): at most one suggestion; it can be closed and never blocks anything (D9). A raise can be accepted with one tap. */
@Composable
internal fun SuggestionCard(c: Catalog, s: Suggestion, onDismiss: () -> Unit, onAccept: (() -> Unit)?) {
    fun name(id: String) = c.variation(id)?.name ?: id
    val text = when (s) {
        is Suggestion.RaiseLevel -> stringResource(R.string.suggest_raise, name(s.variationId), s.toTier)
        is Suggestion.HeavierWeight -> stringResource(if (s.round > 0) R.string.suggest_weight_easy else R.string.suggest_weight, name(s.variationId), formatKg(s.nextGrams), stringResource(equipmentLabelShortPublic(s.equipmentId)))
        is Suggestion.NextExercise -> stringResource(R.string.suggest_next, name(s.toVariationId))
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
private fun NodePanel(c: Catalog, v: ExerciseVariation, n: NodeUi, progress: app.calisthenics.domain.progression.ProgressSnapshot, profile: EquipmentProfile, onOpen: () -> Unit, onOpenOther: (String) -> Unit, onClose: () -> Unit) {
    val policy = c.policyFor(v)
    val tier = progress.tierFor(v.id) ?: 1
    val target = policy?.tiers?.firstOrNull { it.index == tier }?.target
    val unlocks = policy?.nextVariationIds.orEmpty().mapNotNull { id -> c.variation(id)?.let { id to it.name } }
    val unmet = Goals.unmet(c, v.id, progress).map { it.variationId to stringResource(R.string.sheet_step_name, c.variation(it.variationId)?.name ?: it.variationId, it.tier) }
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
            // R7-T11 (D6): the next exercises and the prerequisites (also from other tabs) are links
            if (unlocks.isNotEmpty()) LinkLine(stringResource(R.string.panel_unlocks_label), unlocks, "panel_unlocks", "panel_unlock_", MaterialTheme.colorScheme.onSurfaceVariant, onOpenOther)
            if (unmet.isNotEmpty() && v.id !in progress.variations) LinkLine(stringResource(R.string.panel_needs_label), unmet, "panel_needs", "panel_need_", MaterialTheme.colorScheme.error, onOpenOther)
            if (!isAvailable(v, profile)) Text(stringResource(R.string.sheet_missing, missingLabel(v, profile)), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
            AppOutlinedButton(onClick = onOpen, Modifier.fillMaxWidth().height(48.dp).testTag("panel_open_detail")) { Text(stringResource(R.string.panel_open_detail).uppercase()) }
        }
    }
}

/** R7-T11 (D6): "Label: A, B" where every name opens that exercise. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LinkLine(label: String, items: List<Pair<String, String>>, tag: String, itemTag: String, color: androidx.compose.ui.graphics.Color, onOpen: (String) -> Unit) {
    FlowRow(Modifier.fillMaxWidth().testTag(tag), horizontalArrangement = Arrangement.spacedBy(Spacing.xs), verticalArrangement = Arrangement.Center) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = color, modifier = Modifier.align(Alignment.CenterVertically))
        items.forEach { (id, name) ->
            Text(name, style = MaterialTheme.typography.bodyMedium.copy(textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline), color = AppAccentTheme.colors.text,
                modifier = Modifier.heightIn(min = 40.dp).wrapContentHeight(Alignment.CenterVertically).clickable(role = androidx.compose.ui.semantics.Role.Button) { onOpen(id) }.testTag(itemTag + id))
        }
    }
}

/** Doc 17 §2.7: the exercise detail, a sub-page of Progress. No action buttons here. */
@Composable
private fun ExerciseDetail(c: Catalog, v: ExerciseVariation, progress: app.calisthenics.domain.progression.ProgressSnapshot, profile: EquipmentProfile, familyTitle: String, modifier: Modifier, onOpen: (String) -> Unit, onLevelsDone: (Int?) -> Unit, onBack: () -> Unit) {
    val ctx = LocalContext.current
    var editing by remember(v.id) { mutableStateOf(false) }
    val policy = c.policyFor(v)
    val tier = progress.tierFor(v.id) ?: 1
    val achieved = progress.variations[v.id]?.achievedTiers ?: emptySet()
    val unlocks = policy?.nextVariationIds.orEmpty().mapNotNull { id -> c.variation(id)?.let { id to it.name } }
    val needs = Goals.unmet(c, v.id, progress).map { it.variationId to stringResource(R.string.sheet_step_name, c.variation(it.variationId)?.name ?: it.variationId, it.tier) }
    val dark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).statusBarsPadding().padding(horizontal = Spacing.l).padding(bottom = Spacing.xl).testTag("exercise_detail"), verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack, modifier = Modifier.testTag("detail_back")) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back)) }
            Caption(stringResource(R.string.detail_breadcrumb, familyTitle, tier, 5), Modifier.testTag("detail_breadcrumb"))
        }
        Text(v.name.uppercase(), style = MaterialTheme.typography.headlineMedium, modifier = Modifier.testTag("detail_title"))
        if (v.aliases.isNotEmpty()) Text(
            stringResource(R.string.detail_aliases, v.aliases.joinToString(", ")),
            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.testTag("detail_aliases"),
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            StarRow(displayStars(c, progress, v.id), size = 22.dp)
            Spacer(Modifier.weight(1f))
            if (policy != null) AppTextButton(onClick = { editing = !editing }, modifier = Modifier.testTag("detail_edit")) { Text(stringResource(if (editing) R.string.detail_edit_done else R.string.detail_edit)) }
        }
        // R7-T11 (D5): set the level by hand; the levels up to the chosen one count as done and the stars follow
        if (editing && policy != null) Column(Modifier.fillMaxWidth().testTag("detail_edit_panel"), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            Text(stringResource(R.string.detail_edit_hint), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s)) {
                for (n in 1..5) FilterChip(selected = displayStars(c, progress, v.id) == n, onClick = { onLevelsDone(n) }, label = { Text("$n") }, modifier = Modifier.testTag("detail_set_level_$n"))
                if (v.id in progress.variations) AppTextButton(onClick = { onLevelsDone(null) }, modifier = Modifier.testTag("detail_set_level_none")) { Text(stringResource(R.string.detail_edit_clear)) }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.m), verticalAlignment = Alignment.CenterVertically) {
            // the clip stays on its white background in both themes (doc 17)
            Box(Modifier.weight(1f).clip(RoundedCornerShape(12.dp)).background(Color.White).testTag(if (v.demonstrationHold) "detail_demo_held" else "detail_demo")) {
                if (v.demonstrationHold) Text(stringResource(R.string.progress_teaching_hold), Modifier.padding(Spacing.m), style = MaterialTheme.typography.bodyMedium)
                else DemoClips.file(ctx, v.id)?.let { DemoPlayer(it, controls = false) }
            }
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
        if (unlocks.isNotEmpty()) { Caption(stringResource(R.string.detail_unlocks)); LinkLine("", unlocks, "detail_unlocks", "detail_unlock_", MaterialTheme.colorScheme.onSurface, onOpen) }
        if (needs.isNotEmpty() || !isAvailable(v, profile)) {
            Caption(stringResource(R.string.detail_needs))
            LinkLine("", needs, "detail_needs", "detail_need_", MaterialTheme.colorScheme.onSurface, onOpen)
            if (!isAvailable(v, profile)) Text(missingLabel(v, profile), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.testTag("detail_missing"))
        }
        if (v.cautions.isNotEmpty()) {
            val strong = v.cautionStrong   // R7-T12 (D9): light red for the two exercises that need extra care
            val bg = if (strong) (if (dark) CautionRedDarkSurface else CautionRedLightSurface) else if (dark) CautionDarkSurface else CautionLightSurface
            val br = if (strong) (if (dark) CautionRedDarkBorder else CautionRedLightBorder) else if (dark) CautionDarkBorder else CautionLightBorder
            val tx = if (strong) (if (dark) CautionRedDarkText else CautionRedLightText) else if (dark) CautionDarkText else CautionLightText
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
        c.variations.filter { (it.kind == Kind.STRETCH || it.kind == Kind.MOBILITY) && !it.routineOnly }.forEach { v ->
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
            if (v.demonstrationHold) Text(stringResource(R.string.progress_teaching_hold), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            else DemoClips.file(ctx, v.id)?.let { DemoPlayer(it) }
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
            if (!isAvailable(v, profile)) Text(stringResource(R.string.sheet_missing, missingLabel(v, profile)), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
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
