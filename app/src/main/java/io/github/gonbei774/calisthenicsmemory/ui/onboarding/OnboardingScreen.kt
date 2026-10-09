// U08 (F7): the one-time questionnaire. One question per page, progress dots, Back/Next, required pages cannot be skipped.
// Welcome · Goal · Equipment · one page per family · Workout style · Summary.
package io.github.gonbei774.calisthenicsmemory.ui.onboarding

import io.github.gonbei774.calisthenicsmemory.ui.theme.AppAccentTheme

import io.github.gonbei774.calisthenicsmemory.ui.components.AppTextButton
import io.github.gonbei774.calisthenicsmemory.ui.components.AppOutlinedButton
import io.github.gonbei774.calisthenicsmemory.ui.components.appSegmentedColors

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.toggleable
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.calisthenics.domain.equipment.EquipmentSelection
import app.calisthenics.domain.equipment.buildProfile
import app.calisthenics.domain.equipment.isAvailable
import app.calisthenics.domain.equipment.selectionOf
import app.calisthenics.domain.goals.Goal
import app.calisthenics.domain.goals.Goals
import app.calisthenics.domain.intake.*
import app.calisthenics.domain.model.*
import app.calisthenics.domain.model.Target
import app.calisthenics.domain.planner.MAX_EXPLICIT_ROUNDS
import io.github.gonbei774.calisthenicsmemory.R
import io.github.gonbei774.calisthenicsmemory.ui.components.Stepper
import io.github.gonbei774.calisthenicsmemory.ui.components.StarRow
import io.github.gonbei774.calisthenicsmemory.ui.screens.*
import io.github.gonbei774.calisthenicsmemory.ui.theme.Radius
import io.github.gonbei774.calisthenicsmemory.ui.theme.Spacing
import io.github.gonbei774.calisthenicsmemory.ui.train.loadCatalog
import io.github.gonbei774.calisthenicsmemory.ui.train.skillGoals
import io.github.gonbei774.calisthenicsmemory.ui.train.bodyGoals

private sealed interface Page {
    data object Welcome : Page
    data object GoalPage : Page
    data object Equipment : Page
    data class Family(val f: OnboardingFamily) : Page
    data object Style : Page
    data object Summary : Page
}

/** Everything the questionnaire collects. Plain state so the pages stay stateless. */
private class Form(val catalog: Catalog, start: EquipmentProfile, goal: String) {
    var goalId by mutableStateOf(goal)
    var profileName by mutableStateOf(start.name)
    var picked by mutableStateOf(selectionOf(start).map { it.id })
    var weights by mutableStateOf(selectionOf(start).filter { it.massGrams != null }.associate { it.id to it.massGrams!! })
    val answers = mutableStateMapOf<String, FamilyAnswer>()
    val knowMode = mutableStateMapOf<String, Boolean>() // family id -> "I know" (true) or "I don't know" (false)
    val chosen = mutableStateMapOf<String, String>()     // family id -> exercise tapped in the carousel
    val reps = mutableStateMapOf<String, Int>()          // variation id -> reps / seconds
    val roundsByFamily = mutableStateMapOf<String, Int>()
    var timed by mutableStateOf(false)
    var rounds by mutableStateOf<Int?>(null)
    var stretch by mutableStateOf(true)

    fun profile(id: String) = buildProfile(id, profileName.ifBlank { id }, picked.map { EquipmentSelection(it, weights[it]) })

    fun pages(): List<Page> {
        val push = answers["pushup"]
        return listOf(Page.Welcome, Page.GoalPage, Page.Equipment) +
            catalog.onboardingFamilies.filter { shouldAsk(catalog, it, push) }.map { Page.Family(it) } + listOf(Page.Style, Page.Summary)
    }

    fun levels(pages: List<Page>): List<StartLevel> = pages.filterIsInstance<Page.Family>().mapNotNull { p ->
        answers[p.f.id]?.let { levelFor(catalog, p.f, it) { v -> isAvailable(v, profile("home")) } }
    }
    fun effectiveRounds() = rounds ?: suggestedRounds(answers.values)
}

@Composable
private fun targetLabel(t: Target) = if (t.type == TargetType.REPS) stringResource(R.string.target_reps, t.value) else stringResource(R.string.target_seconds, t.value)

@Composable
fun OnboardingScreen(modifier: Modifier = Modifier, onBack: (() -> Unit)?, onDone: () -> Unit) {
    val ctx = LocalContext.current
    val catalog = remember { loadCatalog(ctx) }
    val form = remember {
        val start = ProfileStore.load(ctx).let { l -> l.firstOrNull { it.id == "home" } ?: l.first() }
        Form(catalog, start, GoalStore.load(ctx)).also { it.stretch = PrefsStore.load(ctx).stretchOn; it.timed = ModeStore.timed(ctx) }
    }
    var index by rememberSaveable { mutableIntStateOf(0) }
    val pages = form.pages()
    val i = index.coerceIn(0, pages.lastIndex)
    val page = pages[i]
    val canNext = when (page) { is Page.Family -> form.answers[page.f.id] != null; Page.Equipment -> form.profileName.isNotBlank(); else -> true }

    fun finish() {
        val lv = form.levels(pages)
        val pullLevel = lv.firstOrNull { l -> catalog.variation(l.variationId)?.familyId.let { it == "pullup" || it == "row" } }
        OnboardingSave.save(ctx, catalog, OnboardingResult(form.goalId, form.profile(ProfileStore.load(ctx).firstOrNull { it.id == "home" }?.id ?: "home"), lv, pullLevel,
            form.timed, form.effectiveRounds(), form.stretch))
        onDone()
    }

    Column(modifier.fillMaxSize().statusBarsPadding().padding(horizontal = Spacing.l)) {
        Row(Modifier.fillMaxWidth().padding(vertical = Spacing.m), verticalAlignment = Alignment.CenterVertically) {
            if (page != Page.Welcome && page != Page.Summary) StepHeader(page, Modifier.weight(1f).testTag("onb_header")) else Spacer(Modifier.weight(1f))
            if (onBack != null) AppTextButton(onClick = onBack, modifier = Modifier.testTag("onb_cancel")) { Text(stringResource(R.string.cancel)) }
        }
        AnimatedContent(
            targetState = i, modifier = Modifier.weight(1f).fillMaxWidth(),
            transitionSpec = { (slideInHorizontally { w -> if (targetState > initialState) w else -w }) togetherWith (slideOutHorizontally { w -> if (targetState > initialState) -w else w }) },
            label = "page",
        ) { idx ->
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(Spacing.m)) {
                when (val p = pages.getOrNull(idx) ?: Page.Welcome) {
                    Page.Welcome -> WelcomePage()
                    Page.GoalPage -> GoalPage(form)
                    Page.Equipment -> EquipmentPage(form)
                    is Page.Family -> FamilyPage(form, p.f)
                    Page.Style -> StylePage(form)
                    Page.Summary -> SummaryPage(form, pages)
                }
            }
        }
        Row(Modifier.fillMaxWidth().padding(vertical = Spacing.l), horizontalArrangement = Arrangement.spacedBy(Spacing.s), verticalAlignment = Alignment.CenterVertically) {
            if (i > 0) AppOutlinedButton(onClick = { index = i - 1 }, Modifier.height(52.dp).testTag("onb_back")) { Text(stringResource(R.string.back)) }
            Spacer(Modifier.weight(1f))
            if (page == Page.Summary) Button(onClick = { finish() }, Modifier.height(56.dp).testTag("onb_finish"), shape = RoundedCornerShape(Radius.button)) { Text(stringResource(R.string.onb_start)) }
            else Button(onClick = { index = i + 1 }, enabled = canNext, modifier = Modifier.height(56.dp).testTag("onb_next"), shape = RoundedCornerShape(Radius.button)) { Text(stringResource(R.string.onb_next)) }
        }
    }
}

/** R6 / D2: the three parts Goal · Equipment · Level, the current one highlighted. No step dots (R7). */
@Composable
private fun StepHeader(page: Page, modifier: Modifier) {
    val current = when (page) { Page.GoalPage -> 0; Page.Equipment -> 1; else -> 2 }
    val names = listOf(R.string.onb_part_goal, R.string.onb_part_equipment, R.string.onb_part_level)
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
        names.forEachIndexed { n, r ->
            val here = n == current
            Column(Modifier.semantics(mergeDescendants = true) {}.testTag("onb_part_$n")) {
                Text(stringResource(r).uppercase(), style = MaterialTheme.typography.labelLarge, color = if (here) AppAccentTheme.colors.text else MaterialTheme.colorScheme.outline)
                Box(Modifier.padding(top = 3.dp).width(28.dp).height(3.dp).clip(RoundedCornerShape(2.dp)).background(if (here) AppAccentTheme.colors.accent else MaterialTheme.colorScheme.outlineVariant))
            }
        }
    }
}

@Composable private fun Title(text: String, sub: String? = null) {
    Text(text, style = MaterialTheme.typography.headlineMedium, modifier = Modifier.testTag("onb_title"))
    if (sub != null) Text(sub, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable private fun WelcomePage() {
    Title(stringResource(R.string.onb_welcome_title), stringResource(R.string.onb_welcome_text))
    Text(stringResource(R.string.onb_welcome_privacy), style = MaterialTheme.typography.bodyMedium)
}

@Composable private fun GoalPage(form: Form) {
    Title(stringResource(R.string.onb_goal_title), stringResource(R.string.onb_goal_text))
    io.github.gonbei774.calisthenicsmemory.ui.components.ObjectivePicker(form.goalId, { form.goalId = it })
}

@Composable private fun EquipmentPage(form: Form) {
    Title(stringResource(R.string.onb_equipment_title), stringResource(R.string.onb_equipment_text))
    OutlinedTextField(value = form.profileName, onValueChange = { form.profileName = it.take(30) }, singleLine = true, label = { Text(stringResource(R.string.profile_name)) },
        isError = form.profileName.isBlank(), modifier = Modifier.fillMaxWidth().testTag("onb_profile_name"))
    EquipmentChecklist(form.picked, { form.picked = it }, form.weights, { form.weights = it })
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable private fun FamilyPage(form: Form, f: OnboardingFamily) {
    val cat = form.catalog
    val know = form.knowMode[f.id] ?: true
    var sheet by remember { mutableStateOf<ExerciseVariation?>(null) }
    val profile = form.profile("home")
    Title(stringResource(R.string.onb_family_title, f.title), stringResource(R.string.onb_family_text))
    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
        SegmentedButton(selected = know, onClick = { form.knowMode[f.id] = true; form.answers.remove(f.id) }, colors = appSegmentedColors(), shape = SegmentedButtonDefaults.itemShape(0, 2), icon = {}, modifier = Modifier.testTag("onb_know")) { Text(stringResource(R.string.onb_i_know)) }
        SegmentedButton(selected = !know, onClick = { form.knowMode[f.id] = false; form.answers.remove(f.id) }, colors = appSegmentedColors(), shape = SegmentedButtonDefaults.itemShape(1, 2), icon = {}, modifier = Modifier.testTag("onb_dont_know")) { Text(stringResource(R.string.onb_i_dont_know)) }
    }
    if (know) {
        LazyRow(Modifier.testTag("onb_carousel"), horizontalArrangement = Arrangement.spacedBy(Spacing.s)) {
            items(fullProgression(cat, f), key = { it.id }) { v ->
                val selected = form.chosen[f.id] == v.id
                Card(onClick = { sheet = v }, modifier = Modifier.width(200.dp).testTag("onb_ex_${v.id}"), shape = MaterialTheme.shapes.large,
                    border = if (selected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null) {
                    Column(Modifier.padding(Spacing.m), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                        io.github.gonbei774.calisthenicsmemory.ui.components.ExerciseThumb(v.id, v.name, 64.dp)
                        Text(v.name, style = MaterialTheme.typography.titleMedium, maxLines = 2)
                        val tiers = cat.policyForVariation(v.id)?.tiers.orEmpty()
                        if (tiers.isNotEmpty()) Text(stringResource(R.string.onb_levels_line, tiers.joinToString(" · ") { it.target.value.toString() }, stringResource(if (tiers.first().target.type == TargetType.REPS) R.string.unit_reps else R.string.unit_seconds)), style = MaterialTheme.typography.bodySmall, maxLines = 2, modifier = Modifier.testTag("onb_levels_${v.id}"))
                        if (!isAvailable(v, profile)) Text(stringResource(R.string.onb_needs_equipment), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
        val vid = form.chosen[f.id]
        val v = vid?.let { cat.variation(it) }
        if (v != null) {
            val tiers = cat.policyForVariation(v.id)?.tiers.orEmpty()
            val hold = tiers.firstOrNull()?.target?.type == TargetType.HOLD_SECONDS
            val value = form.reps[v.id] ?: tiers.firstOrNull { it.index == 3 }?.target?.value ?: 5
            val rounds = form.roundsByFamily[f.id] ?: 4
            fun sync(r: Int, n: Int) { form.reps[v.id] = r; form.roundsByFamily[f.id] = n; form.answers[f.id] = FamilyAnswer.Does(v.id, r, n) }
            Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
                Column(Modifier.padding(Spacing.m), verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
                    Text(v.name, style = MaterialTheme.typography.titleMedium)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(if (hold) R.string.onb_seconds_per_set else R.string.onb_reps_per_set), Modifier.weight(1f))
                        Stepper(value, { sync(it, rounds) }, 1..999, step = if (hold && value >= 30) 5 else 1, tag = "onb_reps")
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(R.string.train_rounds), Modifier.weight(1f))
                        Stepper(rounds, { sync(value, it) }, 1..MAX_EXPLICIT_ROUNDS, tag = "onb_rounds")
                    }
                }
            }
        } else Text(stringResource(R.string.onb_pick_one), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    } else {
        Text(stringResource(R.string.onb_guess_text), style = MaterialTheme.typography.bodyMedium)
        Guess.entries.forEach { g ->
            val l = levelFor(cat, f, g) { isAvailable(it, profile) }
            val name = cat.variation(l.variationId)?.name.orEmpty()
            val target = cat.policyForVariation(l.variationId)?.tiers?.firstOrNull { it.index == l.tier }?.target
            val selected = (form.answers[f.id] as? FamilyAnswer.NotSure)?.guess == g
            Card(onClick = { form.answers[f.id] = FamilyAnswer.NotSure(g) }, modifier = Modifier.fillMaxWidth().testTag("onb_guess_${g.name}"), shape = MaterialTheme.shapes.large,
                border = if (selected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null) {
                Column(Modifier.padding(Spacing.m)) {
                    Text(stringResource(when (g) { Guess.EASY -> R.string.onb_easy; Guess.NORMAL -> R.string.onb_normal; Guess.HARD -> R.string.onb_hard }), style = MaterialTheme.typography.titleMedium)
                    Text(if (target != null) stringResource(R.string.onb_guess_line, name, if (target.type == TargetType.REPS) stringResource(R.string.target_reps, target.value) else stringResource(R.string.target_seconds, target.value)) else name, style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
    }
    sheet?.let { v ->
        ModalBottomSheet(onDismissRequest = { sheet = null }, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
            Column(Modifier.padding(horizontal = Spacing.l).padding(bottom = Spacing.xl).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
                Text(v.name, style = MaterialTheme.typography.titleLarge)
                val ctx = LocalContext.current
                DemoClips.file(ctx, v.id)?.let { DemoPlayer(it) }
                v.instructions.forEach { Text(stringResource(R.string.bullet_item, it), style = MaterialTheme.typography.bodyMedium) }
                Button(onClick = {
                    form.chosen[f.id] = v.id
                    val tiers = cat.policyForVariation(v.id)?.tiers.orEmpty()
                    val r = form.reps[v.id] ?: tiers.firstOrNull { it.index == 3 }?.target?.value ?: 5
                    val n = form.roundsByFamily[f.id] ?: 4
                    form.reps[v.id] = r; form.roundsByFamily[f.id] = n; form.answers[f.id] = FamilyAnswer.Does(v.id, r, n)
                    sheet = null
                }, modifier = Modifier.fillMaxWidth().height(52.dp).testTag("onb_i_do_this")) { Text(stringResource(R.string.onb_i_do_this)) }
            }
        }
    }
}

@Composable private fun StylePage(form: Form) {
    Title(stringResource(R.string.onb_style_title), stringResource(R.string.onb_style_text))
    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
        SegmentedButton(selected = !form.timed, onClick = { form.timed = false }, colors = appSegmentedColors(), shape = SegmentedButtonDefaults.itemShape(0, 2), icon = {}, modifier = Modifier.testTag("onb_style_reps")) { Text(stringResource(R.string.style_reps)) }
        SegmentedButton(selected = form.timed, onClick = { form.timed = true }, colors = appSegmentedColors(), shape = SegmentedButtonDefaults.itemShape(1, 2), icon = {}, modifier = Modifier.testTag("onb_style_timed")) { Text(stringResource(R.string.style_timed)) }
    }
    Text(stringResource(if (form.timed) R.string.style_timed_hint else R.string.style_reps_hint), style = MaterialTheme.typography.bodyMedium)
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(stringResource(R.string.onb_default_rounds), Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
        Stepper(form.effectiveRounds(), { form.rounds = it }, 1..MAX_EXPLICIT_ROUNDS, tag = "onb_default_rounds")
    }
    Row(Modifier.fillMaxWidth().heightIn(min = Spacing.touch).toggleable(form.stretch, role = Role.Switch) { form.stretch = it }.testTag("onb_stretch"), verticalAlignment = Alignment.CenterVertically) {
        Text(stringResource(R.string.train_stretch), Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
        Switch(checked = form.stretch, onCheckedChange = null)
    }
}

@Composable private fun SummaryPage(form: Form, pages: List<Page>) {
    Title(stringResource(R.string.onb_summary_title), stringResource(R.string.onb_summary_text))
    form.levels(pages).forEach { l ->
        val v = form.catalog.variation(l.variationId)
        val target = form.catalog.policyForVariation(l.variationId)?.tiers?.firstOrNull { it.index == l.tier }?.target
        Card(Modifier.fillMaxWidth().testTag("onb_sum_${l.variationId}"), shape = MaterialTheme.shapes.large) {
            Row(Modifier.padding(Spacing.m), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(v?.name.orEmpty(), style = MaterialTheme.typography.titleMedium)
                    Text(stringResource(R.string.onb_summary_line, l.tier, target?.let { targetLabel(it) }.orEmpty()), style = MaterialTheme.typography.bodyMedium)
                }
                StarRow(l.tier - 1) // D8: the levels below the chosen one are shown as stars
            }
        }
    }
}
