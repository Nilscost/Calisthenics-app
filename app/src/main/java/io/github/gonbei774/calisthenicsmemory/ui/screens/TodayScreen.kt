// T16 (first slice): Today / preview. Plans come from the pure-Kotlin domain planner and the bundled DRAFT catalog.
// Start hands the plan to the foreground service (M5).
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
import app.calisthenics.domain.routine.saveSwapsToRoutine
import app.calisthenics.domain.routine.toggleFocus

private fun fmt(sec: Int) = "%d:%02d".format(sec / 60, sec % 60)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodayScreen(onBack: () -> Unit, onLevels: () -> Unit, onStarted: () -> Unit = {}) {
    val ctx = LocalContext.current
    val catalog = remember { ctx.assets.open("catalog.json").bufferedReader().use { parseCatalog(it.readText()) } }
    var routine by remember { mutableStateOf(RoutineStore.load(ctx)) }
    var swaps by remember { mutableStateOf(emptyMap<String, String>()) }
    val saved = remember { PrefsStore.load(ctx) }
    var minutes by rememberSaveable { mutableIntStateOf(saved.defaultDurationSeconds / 60) }
    var roundsSel by rememberSaveable { mutableIntStateOf(0) } // 0 = auto from minutes
    var stretch by rememberSaveable { mutableStateOf(saved.stretchOn) }
    var profileId by rememberSaveable { mutableStateOf(saved.selectedProfileId) }
    var focusNames by rememberSaveable { mutableStateOf(listOf("FULL_BODY")) }
    val focus = focusNames.map { StrengthFocus.valueOf(it) }.toSet()
    val profile = SeedProfiles.all.first { it.id == profileId }

    var progress by remember { mutableStateOf(LevelStore.snapshot(catalog, LevelStore.load(ctx))) }
    LaunchedEffect(Unit) { progress = try { ProgressLoader.load(ctx, catalog) } catch (_: Exception) { progress } }
    val result = remember(minutes, roundsSel, stretch, profileId, focusNames, progress, routine, swaps) {
        val draft = SessionDraft.from(Preferences(defaultDurationSeconds = minutes * 60, stretchOn = stretch, selectedProfileId = profileId), routine)
            .copy(focus = focus, swaps = swaps, rounds = roundsSel.takeIf { it > 0 })
        generate(PlanInput("preview", System.currentTimeMillis(), 0, catalog, routine, draft, profile, progress = progress))
    }
    val names = remember(catalog) { catalog.variations.associate { it.id to it.name } }

    Scaffold(topBar = { TopAppBar(title = { Text("Today's workout") }, navigationIcon = { TextButton(onClick = onBack) { Text("Back") } }) }) { pad ->
        Column(Modifier.padding(pad).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Length: $minutes min", style = MaterialTheme.typography.titleMedium)
            Text("Rounds", style = MaterialTheme.typography.titleSmall)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                (0..5).forEach { n -> FilterChip(selected = roundsSel == n, onClick = { roundsSel = n }, label = { Text(if (n == 0) "Auto" else "$n") }) }
            }
            if (roundsSel > 0) Text("Length follows from the rounds; the minutes slider is ignored.", style = MaterialTheme.typography.bodySmall)
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
            OutlinedButton(onClick = onLevels) { Text("Set my starting level") }
            val unfinished = remember { io.github.gonbei774.calisthenicsmemory.session.CheckpointStore.read(ctx) }
            val live by io.github.gonbei774.calisthenicsmemory.session.SessionBus.state.collectAsState()
            if (live != null && !live!!.isTerminal) {
                Card { Column(Modifier.padding(12.dp)) { Text("A workout is running."); Button(onClick = onStarted) { Text("Open it") } } }
            } else if (unfinished != null) {
                Card { Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Unfinished workout found (the app was closed or crashed). Up to a second of work after the last save may be missing.")
                    Button(onClick = { recoverWorkout(ctx, true); onStarted() }) { Text("Continue it (paused)") }
                    OutlinedButton(onClick = { recoverWorkout(ctx, false); onStarted() }) { Text("Save what was done and end") }
                } }
            }
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
                    HorizontalDivider()
                    Text("Swap an exercise (today only)", style = MaterialTheme.typography.titleSmall)
                    routine.slots.forEach { slot ->
                        val cur = swaps[slot.id] ?: slot.preferredVariationId
                        val options = catalog.variations.filter { (it.kind == Kind.REPS || it.kind == Kind.HOLD) && slot.area in it.areas && slot.intent in it.patterns }
                        if (options.size > 1) Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            options.forEach { o -> FilterChip(selected = o.id == cur, onClick = { swaps = swaps + (slot.id to o.id) }, label = { Text(o.name) }) }
                        }
                    }
                    if (swaps.isNotEmpty()) {
                        OutlinedButton(onClick = { swaps = emptyMap() }) { Text("Undo swaps") }
                        OutlinedButton(onClick = { routine = saveSwapsToRoutine(routine, swaps); RoutineStore.save(ctx, routine); swaps = emptyMap() }) { Text("Save swaps to my usual plan") }
                    }
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(onClick = {
                        PrefsStore.save(ctx, app.calisthenics.domain.routine.rememberOnStart(saved,
                            SessionDraft.from(saved, routine).copy(durationSeconds = minutes * 60, stretchOn = stretch, profileId = profileId)))
                    }) { Text("Remember these settings") }
                    Button(onClick = {
                        val pj = kotlinx.serialization.json.Json { encodeDefaults = true }.encodeToString(app.calisthenics.domain.model.WorkoutPlan.serializer(), p)
                        startWorkout(ctx, pj, java.util.UUID.randomUUID().toString(), true)
                        onStarted()
                    }) { Text("Start workout") }
                }
            }
        }
    }
}
