// T16 (first slice): Today / preview. Plans come from the pure-Kotlin domain planner and the bundled DRAFT catalog.
// Start is disabled until the production foreground runtime (T15/M5) exists; nothing here fakes a workout.
package io.github.gonbei774.calisthenicsmemory.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import app.calisthenics.domain.content.parseCatalog
import app.calisthenics.domain.equipment.SeedProfiles
import app.calisthenics.domain.model.*
import app.calisthenics.domain.planner.*
import app.calisthenics.domain.routine.SessionDraft
import app.calisthenics.domain.routine.StarterRoutine
import app.calisthenics.domain.routine.toggleFocus

private fun fmt(sec: Int) = "%d:%02d".format(sec / 60, sec % 60)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodayScreen(onBack: () -> Unit) {
    val ctx = LocalContext.current
    val catalog = remember { ctx.assets.open("catalog.json").bufferedReader().use { parseCatalog(it.readText()) } }
    val saved = remember { PrefsStore.load(ctx) }
    var minutes by rememberSaveable { mutableIntStateOf(saved.defaultDurationSeconds / 60) }
    var stretch by rememberSaveable { mutableStateOf(saved.stretchOn) }
    var profileId by rememberSaveable { mutableStateOf(saved.selectedProfileId) }
    var focusNames by rememberSaveable { mutableStateOf(listOf("FULL_BODY")) }
    val focus = focusNames.map { StrengthFocus.valueOf(it) }.toSet()
    val profile = SeedProfiles.all.first { it.id == profileId }

    val result = remember(minutes, stretch, profileId, focusNames) {
        val draft = SessionDraft.from(Preferences(defaultDurationSeconds = minutes * 60, stretchOn = stretch, selectedProfileId = profileId), StarterRoutine.routine)
            .copy(focus = focus)
        generate(PlanInput("preview", System.currentTimeMillis(), 0, catalog, StarterRoutine.routine, draft, profile))
    }
    val names = remember(catalog) { catalog.variations.associate { it.id to it.name } }

    Scaffold(topBar = { TopAppBar(title = { Text("Today's workout") }, navigationIcon = { TextButton(onClick = onBack) { Text("Back") } }) }) { pad ->
        Column(Modifier.padding(pad).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Length: $minutes min", style = MaterialTheme.typography.titleMedium)
            Slider(value = minutes.toFloat(), onValueChange = { minutes = (it / 5).toInt() * 5 }, valueRange = 10f..90f)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SeedProfiles.all.forEach { p -> FilterChip(selected = profileId == p.id, onClick = { profileId = p.id }, label = { Text(p.name) }) }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StrengthFocus.values().forEach { f ->
                    FilterChip(selected = f in focus, onClick = { focusNames = toggleFocus(focus, f).map { it.name } }, label = { Text(f.name.lowercase().replace('_', ' ')) })
                }
            }
            Row { Checkbox(stretch, { stretch = it }); Text("Stretch between sets", Modifier.padding(top = 12.dp)) }
            HorizontalDivider()
            when (val r = result) {
                is PlanResult.Infeasible -> {
                    Text("Can't build this workout", style = MaterialTheme.typography.titleMedium)
                    r.reasons.forEach { Text("• ${it.message}") }
                    if (r.alternatives.isNotEmpty()) Text("Try instead:"); r.alternatives.forEach { Text("• ${it.description}") }
                }
                is PlanResult.Ready -> {
                    val p = r.plan
                    Text("${fmt(p.plannedDurationSeconds)} planned, ${p.rounds} round(s)", style = MaterialTheme.typography.titleMedium)
                    if (p.usesDraftContent) Text("Exercise content is an unreviewed draft.", color = MaterialTheme.colorScheme.error)
                    p.warnings.forEach { Text("⚠ $it", color = MaterialTheme.colorScheme.error) }
                    p.changesExplained.forEach { Text("• $it") }
                    HorizontalDivider()
                    p.blocks.forEach { b ->
                        val name = b.variationId?.let { names[it] ?: it } ?: b.type.name.lowercase().replace('_', ' ')
                        val tgt = b.target?.let { if (it.type == TargetType.REPS) "${it.value} reps" else "${it.value}s hold" } ?: ""
                        Text("${fmt(b.durationSeconds)}  $name ${if (b.side != Side.NONE) "(${b.side.name.lowercase()})" else ""} $tgt".trim())
                    }
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(onClick = {
                        PrefsStore.save(ctx, app.calisthenics.domain.routine.rememberOnStart(saved,
                            SessionDraft.from(saved, StarterRoutine.routine).copy(durationSeconds = minutes * 60, stretchOn = stretch, profileId = profileId)))
                    }) { Text("Remember these settings") }
                    Button(onClick = {}, enabled = false) { Text("Start (needs the G1 phone test first)") }
                }
            }
        }
    }
}
