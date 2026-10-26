// The end of a workout: the numbers of the session, what it unlocked (levels, mastery, the next exercise), and one card per exercise
// with its stars and how far it is from the next level. The progress is compared before / after this session (domain SessionProgressSummary);
// the header shows at once, the cards when the session is saved and the comparison is ready.
package io.github.gonbei774.calisthenicsmemory.ui.session

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import app.calisthenics.domain.feedback.Execution
import app.calisthenics.domain.history.overviewOf
import app.calisthenics.domain.history.recordOfState
import app.calisthenics.domain.model.BlockType
import app.calisthenics.domain.model.WorkoutFormat
import app.calisthenics.domain.session.*
import io.github.gonbei774.calisthenicsmemory.R
import io.github.gonbei774.calisthenicsmemory.session.SaveStatus
import io.github.gonbei774.calisthenicsmemory.session.SessionBus
import io.github.gonbei774.calisthenicsmemory.ui.components.AppTextButton
import io.github.gonbei774.calisthenicsmemory.ui.components.Caption
import io.github.gonbei774.calisthenicsmemory.ui.components.ExerciseThumb
import io.github.gonbei774.calisthenicsmemory.ui.components.StarRow
import io.github.gonbei774.calisthenicsmemory.ui.history.RoomHistorySource
import io.github.gonbei774.calisthenicsmemory.ui.screens.ProgressLoader
import io.github.gonbei774.calisthenicsmemory.ui.theme.*
import io.github.gonbei774.calisthenicsmemory.ui.train.loadCatalog
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.ZoneId

/** The comparison with the names it mentions (so the cards need no catalog). */
internal data class EndProgress(val summary: SessionProgressSummary, val names: Map<String, String>)

/** Replays history without and with this session. Throws when history cannot be read: the screen then shows only the header, editor and Done. */
internal suspend fun loadEndProgress(ctx: android.content.Context, st: app.calisthenics.domain.session.SessionState): EndProgress {
    val catalog = loadCatalog(ctx)
    val before = ProgressLoader.load(ctx, catalog, excludeSessionId = st.sessionId)
    val after = ProgressLoader.load(ctx, catalog)
    val summary = summarizeSessionProgress(catalog, before, after, doneVariationIds(st.plan, st.executions))
    val ids = summary.exercises.map { it.variationId } + summary.newExercises.map { it.variationId }
    return EndProgress(summary, ids.associateWith { id -> catalog.variation(id)?.name ?: id })
}

@Composable
internal fun EndScreen(
    st: app.calisthenics.domain.session.SessionState, modifier: Modifier, onExit: () -> Unit,
    /** Replaces the history replay (tests); null = load from history, off the main thread. */
    loadProgress: (suspend () -> EndProgress?)? = null,
) {
    val ctx = LocalContext.current
    val saveStatus by SessionBus.saveStatus.collectAsState()
    // The numbers are History's own (overviewOf over the record the service saves), so the two screens cannot disagree.
    val overview = remember(st, saveStatus) { overviewOf(st.plan, recordOfState(st, SessionBus.startedAtEpochMs, SessionBus.endedAtEpochMs.takeIf { it > 0L }), ZoneId.systemDefault()) }
    val editableExercises = st.plan.blocks.filter { it.type == BlockType.WORK && st.executions[it.id] == Execution.COMPLETED &&
        !(st.plan.fixedProgramId != null && st.plan.flow && it.prescriptionTier == null) }.mapNotNull { it.variationId }.distinct()
    var progress by remember { mutableStateOf<EndProgress?>(null) }
    var reload by remember { mutableIntStateOf(0) }
    var editing by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    // The comparison appears once the session row is in Room; the progress is replayed from history, never stored. A correction below reloads it.
    LaunchedEffect(reload) {
        val deadline = System.currentTimeMillis() + 5000
        while (SessionBus.saveStatus.value == SaveStatus.NONE || SessionBus.saveStatus.value == SaveStatus.SAVING) { if (System.currentTimeMillis() >= deadline) break; delay(100) }
        if (!SessionBus.saved) return@LaunchedEffect
        progress = try { (loadProgress ?: { kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { loadEndProgress(ctx, st) } }).invoke() } catch (_: Throwable) { progress }
    }
    Column(modifier.fillMaxSize().statusBarsPadding().verticalScroll(rememberScrollState()).padding(Spacing.l), verticalArrangement = Arrangement.spacedBy(Spacing.m)) {
        Text(stringResource(if (st.phase == app.calisthenics.domain.session.Phase.COMPLETED) R.string.end_complete else R.string.end_early), style = MaterialTheme.typography.headlineMedium, modifier = Modifier.testTag("end_title"))
        EndTiles(overview.minutes, overview.rounds, overview.exercises, overview.format)
        when (saveStatus) {
            SaveStatus.SAVED -> Text(stringResource(R.string.end_saved), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline, modifier = Modifier.testTag("end_save_status"))
            SaveStatus.FAILED -> Text(stringResource(R.string.end_save_failed), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error, modifier = Modifier.testTag("end_save_status"))
            else -> Text(stringResource(R.string.end_saving), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline, modifier = Modifier.testTag("end_save_status"))
        }
        if (saveStatus == SaveStatus.SAVED) progress?.let { EndProgressCards(it) }
        // R8.1-T06 (F7): a Flow workout still logs actual reps, so the editor is there to correct DONE; only a flow program has no per-item results
        if (saveStatus != SaveStatus.FAILED && editableExercises.isNotEmpty()) {
            AppTextButton(onClick = { editing = !editing }, modifier = Modifier.testTag("end_edit")) { Text(stringResource(R.string.end_edit_logged)) }
            if (editing) editableExercises.forEach { vid ->
                val blocks = st.plan.blocks.filter { it.type == BlockType.WORK && it.variationId == vid && st.executions[it.id] == Execution.COMPLETED }
                val target = blocks.firstOrNull()?.target
                val logs = blocks.mapNotNull { st.logged[it.id] }
                Card(Modifier.fillMaxWidth().testTag("end_fix_$vid"), shape = MaterialTheme.shapes.large) {
                    Column(Modifier.padding(Spacing.l), verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
                        Text(SessionBus.names[vid] ?: vid, style = MaterialTheme.typography.titleMedium)
                        // V04b (R21): one number per round, plus Too hard / Too easy / Pain for the exercise.
                        RoundEditor(target, blocks.map { EditorRound(it.id, it.roundIndex ?: 1, it.side, st.logged[it.id]?.reps) }, sets = st.plan.format != WorkoutFormat.CIRCUIT,
                            tooHard0 = logs.any { it.tooHard }, tooEasy0 = logs.any { it.tooEasy }, pain0 = logs.any { it.discomfort }, tag = "end_$vid", alternating = blocks.firstOrNull()?.alternating == true) { list ->
                            scope.launch {
                                val deadline = System.currentTimeMillis() + 5000
                                while (SessionBus.saveStatus.value.let { it == SaveStatus.NONE || it == SaveStatus.SAVING } && System.currentTimeMillis() < deadline) delay(100) // the session row must exist first
                                if (SessionBus.saved) { RoomHistorySource(ctx).reviseRounds(st.sessionId, vid, list); reload++ } // a failed save has no session to correct
                            }
                        }
                    }
                }
            }
        }
        Button(onClick = { SessionBus.clear(); onExit() }, Modifier.fillMaxWidth().height(56.dp).testTag("end_done"), shape = RoundedCornerShape(Radius.button)) { Text(stringResource(R.string.end_done)) }
    }
}

@Composable
private fun EndTiles(minutes: Int, rounds: Int, exercises: Int, format: WorkoutFormat) {
    val roundsWord = androidx.compose.ui.res.pluralStringResource(if (format == WorkoutFormat.CIRCUIT) R.plurals.end_tile_rounds else R.plurals.end_tile_sets, rounds)
    Row(Modifier.fillMaxWidth().height(IntrinsicSize.Max).testTag("end_summary"), horizontalArrangement = Arrangement.spacedBy(Spacing.s)) {
        EndTile(minutes, androidx.compose.ui.res.pluralStringResource(R.plurals.end_tile_minutes, minutes), "end_tile_minutes", Modifier.weight(1f).fillMaxHeight())
        EndTile(rounds, roundsWord, "end_tile_rounds", Modifier.weight(1f).fillMaxHeight())
        EndTile(exercises, androidx.compose.ui.res.pluralStringResource(R.plurals.end_tile_exercises, exercises), "end_tile_exercises", Modifier.weight(1f).fillMaxHeight())
    }
}

@Composable
private fun EndTile(value: Int, caption: String, tag: String, modifier: Modifier) {
    Card(modifier.semantics(mergeDescendants = true) {}.testTag(tag), shape = MaterialTheme.shapes.large) {
        Column(Modifier.padding(Spacing.m).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            Text(value.toString(), style = MaterialTheme.typography.headlineMedium)
            Caption(caption)
        }
    }
}

/** Unlocked (only when something happened), then one card per exercise done. */
@Composable
internal fun EndProgressCards(p: EndProgress) {
    val s = p.summary
    fun name(id: String) = p.names[id] ?: SessionBus.names[id] ?: id
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.m)) {
        if (s.hasUnlocks) {
            Caption(stringResource(R.string.end_section_unlocked), Modifier.testTag("end_unlocked_caption"))
            (s.levelUps + s.mastered).forEach { e ->
                UnlockCard(e.variationId, name(e.variationId), e.stars, e.starsBefore,
                    if (e.mastered) stringResource(R.string.end_unlock_mastered) else stringResource(R.string.end_unlock_level, e.leveledUpTo ?: e.tier))
            }
            s.newExercises.forEach { n -> NewExerciseCard(n.variationId, name(n.variationId)) }
        }
        val leveled = s.exercises.filter { it.hasProgression }
        if (leveled.isNotEmpty()) {
            Caption(stringResource(R.string.end_section_exercises), Modifier.testTag("end_exercises_caption"))
            leveled.forEach { ExerciseCard(it, name(it.variationId)) }
        }
        val plain = s.exercises.filter { !it.hasProgression }
        if (plain.isNotEmpty()) {
            if (leveled.isEmpty()) Caption(stringResource(R.string.end_section_exercises), Modifier.testTag("end_exercises_caption"))
            else Caption(stringResource(R.string.end_section_also_done))
            Card(Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.large) {
                Column(Modifier.padding(Spacing.m), verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
                    plain.forEach { e ->
                        Row(Modifier.testTag("end_plain_${e.variationId}"), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.m)) {
                            ExerciseThumb(e.variationId, name(e.variationId), size = 36.dp)
                            Text(name(e.variationId), style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun goldBorder() = BorderStroke(1.5.dp, if (MaterialTheme.colorScheme.surface.luminance() > 0.5f) GoldMarkOnLight else Gold)

/** A level reached or an exercise mastered: the stars pop in, the newly lit ones first. */
@Composable
private fun UnlockCard(vid: String, name: String, stars: Int, starsBefore: Int, title: String) {
    Card(Modifier.fillMaxWidth().testTag("end_unlock_$vid"), shape = MaterialTheme.shapes.large, border = goldBorder()) {
        Row(Modifier.padding(Spacing.l), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.m)) {
            ExerciseThumb(vid, name, size = 56.dp)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                Text(title, style = MaterialTheme.typography.titleLarge)
                Text(name, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(Spacing.xs))
                StarRow(stars, size = 24.dp, popFrom = starsBefore.coerceAtMost(stars))
            }
        }
    }
}

@Composable
private fun NewExerciseCard(vid: String, name: String) {
    Card(Modifier.fillMaxWidth().testTag("end_unlock_$vid"), shape = MaterialTheme.shapes.large, border = BorderStroke(1.5.dp, AppAccentTheme.colors.mark)) {
        Row(Modifier.padding(Spacing.l), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.m)) {
            ExerciseThumb(vid, name, size = 56.dp)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                Caption(stringResource(R.string.end_unlock_new_exercise))
                Text(name, style = MaterialTheme.typography.titleLarge)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ExerciseCard(e: ExerciseProgress, name: String) {
    Card(Modifier.fillMaxWidth().testTag("end_card_${e.variationId}"), shape = MaterialTheme.shapes.large) {
        Row(Modifier.padding(Spacing.l), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.m)) {
            ExerciseThumb(e.variationId, name, size = 52.dp)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                Text(name, style = MaterialTheme.typography.titleMedium)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.s)) {
                    StarRow(e.stars, size = 18.dp)
                    Text(if (e.masteredAlready) stringResource(R.string.end_mastered) else stringResource(R.string.history_level_of, e.tier),
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                e.progress?.let { ProgressDots(e.variationId, it) }
                val chips = buildList {
                    if (e.easierNextTime) add("easier" to stringResource(R.string.end_chip_easier))
                    e.nextTarget?.let { add("target" to stringResource(if (e.nextTargetIsHold) R.string.end_chip_next_seconds else R.string.end_chip_next_reps, it)) }
                    if (e.levelKept) add("kept" to stringResource(R.string.end_chip_kept))
                }
                if (chips.isNotEmpty()) FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.xs), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                    chips.forEach { (key, label) -> StatusChip(label, Modifier.testTag("end_chip_${e.variationId}_$key")) }
                }
            }
        }
    }
}

@Composable
private fun StatusChip(text: String, modifier: Modifier) {
    Text(text, modifier.background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(50)).padding(horizontal = Spacing.s, vertical = 3.dp),
        style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

/** Filled / empty dots: the good sessions so far against the ones needed, with the same sentence as text. */
@Composable
private fun ProgressDots(vid: String, p: LevelProgress) {
    val mastery = p.toLevel > 5
    val text = stringResource(
        when { mastery && p.weekly -> R.string.end_progress_mastery_weekly; mastery -> R.string.end_progress_mastery_sessions; p.weekly -> R.string.end_progress_weekly; else -> R.string.end_progress_sessions },
        p.have, p.need, p.toLevel)
    Row(Modifier.semantics(mergeDescendants = true) {}.testTag("end_progress_$vid"), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.s)) {
        Row(Modifier.clearAndSetSemantics {}, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            repeat(p.need) { i ->
                Box(Modifier.size(10.dp).background(if (i < p.have) AppAccentTheme.colors.mark else MaterialTheme.colorScheme.outlineVariant, CircleShape))
            }
        }
        Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
