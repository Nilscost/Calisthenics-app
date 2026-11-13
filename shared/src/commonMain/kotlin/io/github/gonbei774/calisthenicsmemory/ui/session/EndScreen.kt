// The end of a workout: the numbers of the session, what it unlocked (levels, mastery, the next exercise), and one card per exercise
// with its stars, why its level was reached or how far it is from the next one, how it felt (Too hard · Too easy · Pain, with the move that
// follows from it) and, folded away, the number of every round. The progress is compared before / after this session (domain
// SessionProgressSummary); the header shows at once, the cards when the session is saved and the comparison is ready.
package io.github.gonbei774.calisthenicsmemory.ui.session

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import app.calisthenics.domain.feedback.Execution
import app.calisthenics.domain.history.overviewOf
import app.calisthenics.domain.history.recordOfState
import app.calisthenics.domain.model.BlockType
import app.calisthenics.domain.model.Target
import app.calisthenics.domain.model.WorkoutFormat
import app.calisthenics.domain.progression.Suggestion
import app.calisthenics.domain.progression.moveOnFor
import app.calisthenics.domain.session.ExerciseProgress
import app.calisthenics.domain.session.LevelProgress
import app.calisthenics.domain.session.Phase
import app.calisthenics.domain.session.SessionProgressSummary
import app.calisthenics.domain.session.SessionState
import app.calisthenics.domain.progression.LevelUpBy
import app.calisthenics.domain.session.doneVariationIds
import app.calisthenics.domain.session.summarizeSessionProgress
import io.github.gonbei774.calisthenicsmemory.platform.IoDispatcher
import io.github.gonbei774.calisthenicsmemory.platform.PlatformContext
import io.github.gonbei774.calisthenicsmemory.platform.currentPlatformContext
import io.github.gonbei774.calisthenicsmemory.platform.nowEpochMs
import io.github.gonbei774.calisthenicsmemory.platform.systemTimeZone
import io.github.gonbei774.calisthenicsmemory.platform.todayEpochDay
import io.github.gonbei774.calisthenicsmemory.session.SaveStatus
import io.github.gonbei774.calisthenicsmemory.session.SessionBus
import io.github.gonbei774.calisthenicsmemory.shared.resources.Res
import io.github.gonbei774.calisthenicsmemory.shared.resources.end_acted_level
import io.github.gonbei774.calisthenicsmemory.shared.resources.end_acted_next
import io.github.gonbei774.calisthenicsmemory.shared.resources.end_chip_easier
import io.github.gonbei774.calisthenicsmemory.shared.resources.end_chip_kept
import io.github.gonbei774.calisthenicsmemory.shared.resources.end_chip_next_reps
import io.github.gonbei774.calisthenicsmemory.shared.resources.end_chip_next_seconds
import io.github.gonbei774.calisthenicsmemory.shared.resources.end_complete
import io.github.gonbei774.calisthenicsmemory.shared.resources.end_demonstrated
import io.github.gonbei774.calisthenicsmemory.shared.resources.end_done
import io.github.gonbei774.calisthenicsmemory.shared.resources.end_early
import io.github.gonbei774.calisthenicsmemory.shared.resources.end_edit_logged
import io.github.gonbei774.calisthenicsmemory.shared.resources.end_go_down
import io.github.gonbei774.calisthenicsmemory.shared.resources.end_go_next
import io.github.gonbei774.calisthenicsmemory.shared.resources.end_go_up
import io.github.gonbei774.calisthenicsmemory.shared.resources.end_mastered
import io.github.gonbei774.calisthenicsmemory.shared.resources.end_next_exercise
import io.github.gonbei774.calisthenicsmemory.shared.resources.end_next_exercise_name
import io.github.gonbei774.calisthenicsmemory.shared.resources.end_progress_done_sessions
import io.github.gonbei774.calisthenicsmemory.shared.resources.end_progress_done_weekly
import io.github.gonbei774.calisthenicsmemory.shared.resources.end_progress_mastery_sessions
import io.github.gonbei774.calisthenicsmemory.shared.resources.end_progress_mastery_weekly
import io.github.gonbei774.calisthenicsmemory.shared.resources.end_progress_sessions
import io.github.gonbei774.calisthenicsmemory.shared.resources.end_progress_weekly
import io.github.gonbei774.calisthenicsmemory.shared.resources.end_rate_caption
import io.github.gonbei774.calisthenicsmemory.shared.resources.end_save_failed
import io.github.gonbei774.calisthenicsmemory.shared.resources.end_saved
import io.github.gonbei774.calisthenicsmemory.shared.resources.end_saving
import io.github.gonbei774.calisthenicsmemory.shared.resources.end_section_also_done
import io.github.gonbei774.calisthenicsmemory.shared.resources.end_section_exercises
import io.github.gonbei774.calisthenicsmemory.shared.resources.end_section_unlocked
import io.github.gonbei774.calisthenicsmemory.shared.resources.end_tile_exercises
import io.github.gonbei774.calisthenicsmemory.shared.resources.end_tile_minutes
import io.github.gonbei774.calisthenicsmemory.shared.resources.end_tile_rounds
import io.github.gonbei774.calisthenicsmemory.shared.resources.end_tile_sets
import io.github.gonbei774.calisthenicsmemory.shared.resources.end_unlock_level
import io.github.gonbei774.calisthenicsmemory.shared.resources.end_unlock_mastered
import io.github.gonbei774.calisthenicsmemory.shared.resources.end_unlock_new_exercise
import io.github.gonbei774.calisthenicsmemory.shared.resources.history_level_of
import io.github.gonbei774.calisthenicsmemory.ui.components.AppOutlinedButton
import io.github.gonbei774.calisthenicsmemory.ui.components.AppTextButton
import io.github.gonbei774.calisthenicsmemory.ui.components.Caption
import io.github.gonbei774.calisthenicsmemory.ui.components.ExerciseThumb
import io.github.gonbei774.calisthenicsmemory.ui.components.StarRow
import io.github.gonbei774.calisthenicsmemory.ui.history.RoomHistorySource
import io.github.gonbei774.calisthenicsmemory.ui.history.RoundCorrection
import io.github.gonbei774.calisthenicsmemory.ui.screens.ProfileStore
import io.github.gonbei774.calisthenicsmemory.ui.screens.ProgressLoader
import io.github.gonbei774.calisthenicsmemory.ui.screens.SuggestionStore
import io.github.gonbei774.calisthenicsmemory.ui.theme.AppAccentTheme
import io.github.gonbei774.calisthenicsmemory.ui.theme.Gold
import io.github.gonbei774.calisthenicsmemory.ui.theme.GoldMarkOnLight
import io.github.gonbei774.calisthenicsmemory.ui.theme.Radius
import io.github.gonbei774.calisthenicsmemory.ui.theme.Spacing
import io.github.gonbei774.calisthenicsmemory.ui.train.loadCatalog
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

/**
 * The comparison with the names it mentions (so the cards need no catalog), and for each exercise the move on a "Too easy" rating offers
 * (null entries are left out: no button then).
 */
data class EndProgress(val summary: SessionProgressSummary, val names: Map<String, String>, val moveOns: Map<String, Suggestion> = emptyMap())

/** Replays history without and with this session. Throws when history cannot be read: the screen then shows only the header, editor and Done. */
suspend fun loadEndProgress(ctx: PlatformContext, st: SessionState): EndProgress {
    val catalog = loadCatalog(ctx)
    val before = ProgressLoader.load(ctx, catalog, excludeSessionId = st.sessionId)
    val after = ProgressLoader.load(ctx, catalog)
    val summary = summarizeSessionProgress(catalog, before, after, doneVariationIds(st.plan, st.executions))
    val profile = ProfileStore.selected(ctx)
    val moveOns = summary.exercises.mapNotNull { e -> moveOnFor(catalog, profile, after, e.variationId)?.let { e.variationId to it } }.toMap()
    val ids = summary.exercises.map { it.variationId } + summary.newExercises.map { it.variationId } +
        summary.exercises.mapNotNull { it.nextVariationId } + moveOns.values.mapNotNull { (it as? Suggestion.NextExercise)?.toVariationId }
    return EndProgress(summary, ids.associateWith { id -> catalog.variation(id)?.name ?: id }, moveOns)
}

/** What one exercise card needs to let the person correct the log: its rounds and the ratings given so far. */
internal class CardEditor(
    val target: Target?, val rounds: List<EditorRound>, val sets: Boolean, val tooHard: Boolean, val tooEasy: Boolean, val pain: Boolean, val alternating: Boolean,
    /** The number a round must reach: the level's own target when the shown one is ramped above it (L4). */
    val judged: Int? = target?.value,
)

/** What a tap on the card's move button does; each one is the existing accepted action that the progression replay already understands. */
internal sealed interface EndAction {
    data class Easier(val variationId: String, val tier: Int) : EndAction
    data class Harder(val variationId: String, val tier: Int) : EndAction
    data class Next(val variationId: String, val toVariationId: String) : EndAction
}

private const val SAVE_WAIT_MS = 5000L

/** Waits (at most five seconds) for the session row to be in Room. True when it was saved: a failed save has nothing to compare or correct. */
private suspend fun awaitSaved(): Boolean {
    val deadline = nowEpochMs() + SAVE_WAIT_MS
    while (SessionBus.saveStatus.value.let { it == SaveStatus.NONE || it == SaveStatus.SAVING } && nowEpochMs() < deadline) delay(100)
    return SessionBus.saved
}

private fun cardEditor(st: SessionState, vid: String): CardEditor? {
    val blocks = st.plan.blocks.filter { it.type == BlockType.WORK && it.variationId == vid && st.executions[it.id] == Execution.COMPLETED }
    if (blocks.isEmpty()) return null
    val logs = blocks.mapNotNull { st.logged[it.id] }
    return CardEditor(
        blocks.first().target, blocks.map { EditorRound(it.id, it.roundIndex ?: 1, it.side, st.logged[it.id]?.reps) }, st.plan.format != WorkoutFormat.CIRCUIT,
        logs.any { it.tooHard }, logs.any { it.tooEasy }, logs.any { it.discomfort }, blocks.first().alternating, blocks.first().judgedValue,
    )
}

@Composable
fun EndScreen(
    st: SessionState, modifier: Modifier, onExit: () -> Unit,
    /** Replaces the history replay (tests); null = load from history, off the main thread. */
    loadProgress: (suspend () -> EndProgress?)? = null,
) {
    val ctx = currentPlatformContext()
    val saveStatus by SessionBus.saveStatus.collectAsState()
    // The numbers are History's own (overviewOf over the record the service saves), so the two screens cannot disagree.
    val overview = remember(st, saveStatus) {
        overviewOf(st.plan, recordOfState(st, SessionBus.startedAtEpochMs, SessionBus.endedAtEpochMs.takeIf { it > 0L }), systemTimeZone())
    }
    // A Flow workout still logs actual reps, so its cards can be corrected; only a flow program has no per-item results.
    val editors = remember(st) {
        st.plan.blocks.filter {
            it.type == BlockType.WORK && st.executions[it.id] == Execution.COMPLETED && !(st.plan.fixedProgramId != null && st.plan.flow && it.prescriptionTier == null)
        }.mapNotNull { it.variationId }.distinct().mapNotNull { vid -> cardEditor(st, vid)?.let { vid to it } }.toMap()
    }
    var progress by remember { mutableStateOf<EndProgress?>(null) }
    var reload by remember { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()
    // The comparison appears once the session row is in Room; the progress is replayed from history, never stored. A correction or move reloads it.
    LaunchedEffect(reload) {
        if (!awaitSaved()) return@LaunchedEffect
        progress = try {
            (loadProgress ?: { withContext(IoDispatcher) { loadEndProgress(ctx, st) } }).invoke()
        } catch (_: Throwable) {
            progress
        }
    }
    val saved = saveStatus == SaveStatus.SAVED
    val onSave: (String, List<RoundCorrection>) -> Unit = { vid, list ->
        scope.launch { if (awaitSaved()) { RoomHistorySource(ctx).reviseRounds(st.sessionId, vid, list); reload++ } }
    }
    val onAct: (EndAction) -> Unit = { action ->
        val day = todayEpochDay().toInt()
        when (action) {
            is EndAction.Easier -> SuggestionStore.acceptEasier(ctx, action.variationId, action.tier, day)
            is EndAction.Harder -> SuggestionStore.accept(ctx, action.variationId, action.tier, day)
            is EndAction.Next -> SuggestionStore.acceptNext(ctx, action.variationId, action.toVariationId, day)
        }
        reload++
    }
    val titleRes = if (st.phase == Phase.COMPLETED) Res.string.end_complete else Res.string.end_early
    Column(
        modifier.fillMaxSize().statusBarsPadding().verticalScroll(rememberScrollState()).padding(Spacing.l),
        verticalArrangement = Arrangement.spacedBy(Spacing.m),
    ) {
        Text(stringResource(titleRes), style = MaterialTheme.typography.headlineMedium, modifier = Modifier.testTag("end_title"))
        EndTiles(overview.minutes, overview.rounds, overview.exercises, overview.format)
        val (statusRes, statusColor) = when (saveStatus) {
            SaveStatus.SAVED -> Res.string.end_saved to MaterialTheme.colorScheme.outline
            SaveStatus.FAILED -> Res.string.end_save_failed to MaterialTheme.colorScheme.error
            else -> Res.string.end_saving to MaterialTheme.colorScheme.outline
        }
        Text(stringResource(statusRes), style = MaterialTheme.typography.bodySmall, color = statusColor, modifier = Modifier.testTag("end_save_status"))
        val shown = if (saved) progress else null
        shown?.let { EndProgressCards(it, editors, onSave, onAct) }
        // Exercises without a card (history unreadable, or not compared yet) still get their rating and numbers to correct.
        if (saveStatus != SaveStatus.FAILED) {
            val covered = shown?.summary?.exercises?.map { it.variationId }.orEmpty().toSet()
            editors.filterKeys { it !in covered }.forEach { (vid, editor) ->
                key(vid) { EditOnlyCard(vid, SessionBus.names[vid] ?: vid, editor, onSave) }
            }
        }
        Button(onClick = { SessionBus.clear(); onExit() }, Modifier.fillMaxWidth().height(56.dp).testTag("end_done"), shape = RoundedCornerShape(Radius.button)) {
            Text(stringResource(Res.string.end_done))
        }
    }
}

@Composable
private fun EndTiles(minutes: Int, rounds: Int, exercises: Int, format: WorkoutFormat) {
    val roundsWord = pluralStringResource(if (format == WorkoutFormat.CIRCUIT) Res.plurals.end_tile_rounds else Res.plurals.end_tile_sets, rounds)
    Row(Modifier.fillMaxWidth().height(IntrinsicSize.Max).testTag("end_summary"), horizontalArrangement = Arrangement.spacedBy(Spacing.s)) {
        EndTile(minutes, pluralStringResource(Res.plurals.end_tile_minutes, minutes), "end_tile_minutes", Modifier.weight(1f).fillMaxHeight())
        EndTile(rounds, roundsWord, "end_tile_rounds", Modifier.weight(1f).fillMaxHeight())
        EndTile(exercises, pluralStringResource(Res.plurals.end_tile_exercises, exercises), "end_tile_exercises", Modifier.weight(1f).fillMaxHeight())
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
internal fun EndProgressCards(
    p: EndProgress, editors: Map<String, CardEditor> = emptyMap(), onSave: (String, List<RoundCorrection>) -> Unit = { _, _ -> }, onAct: (EndAction) -> Unit = {},
) {
    val s = p.summary
    fun name(id: String) = p.names[id] ?: SessionBus.names[id] ?: id
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.m)) {
        // a level-up or mastery is folded into that exercise's own card below; only new exercises are announced here
        if (s.newExercises.isNotEmpty()) {
            Caption(stringResource(Res.string.end_section_unlocked), Modifier.testTag("end_unlocked_caption"))
            s.newExercises.forEach { n -> NewExerciseCard(n.variationId, name(n.variationId)) }
        }
        val leveled = s.exercises.filter { it.hasProgression }
        if (leveled.isNotEmpty()) {
            Caption(stringResource(Res.string.end_section_exercises), Modifier.testTag("end_exercises_caption"))
            leveled.forEach { e ->
                key(e.variationId) {
                    // the next exercise only needs its thumbnail when it has no card of its own in this session
                    val next = e.nextVariationId
                    ExerciseCard(
                        e, name(e.variationId), next?.let { NextStep(it, name(it), hasOwnCard = s.exercises.any { x -> x.variationId == it }) },
                        editors[e.variationId], p.moveOns[e.variationId], ::name, onSave, onAct,
                    )
                }
            }
        }
        val plain = s.exercises.filter { !it.hasProgression }
        if (plain.isNotEmpty()) {
            if (leveled.isEmpty()) Caption(stringResource(Res.string.end_section_exercises), Modifier.testTag("end_exercises_caption"))
            else Caption(stringResource(Res.string.end_section_also_done))
            Card(Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.large) {
                Column(Modifier.padding(Spacing.m), verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
                    plain.forEach { e ->
                        key(e.variationId) {
                            Column(Modifier.testTag("end_plain_${e.variationId}"), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.m)) {
                                    ExerciseThumb(e.variationId, name(e.variationId), size = 36.dp)
                                    Text(name(e.variationId), style = MaterialTheme.typography.bodyLarge)
                                }
                                editors[e.variationId]?.let { CardCorrection(e.variationId, 0, it, null, ::name, onSave, onAct) }
                            }
                        }
                    }
                }
            }
        }
    }
}

/** The exercise a mastered one leads on to. */
internal data class NextStep(val variationId: String, val name: String, val hasOwnCard: Boolean)

@Composable
private fun goldBorder() = BorderStroke(1.5.dp, if (MaterialTheme.colorScheme.surface.luminance() > 0.5f) GoldMarkOnLight else Gold)

@Composable
private fun NewExerciseCard(vid: String, name: String) {
    Card(Modifier.fillMaxWidth().testTag("end_unlock_$vid"), shape = MaterialTheme.shapes.large, border = BorderStroke(1.5.dp, AppAccentTheme.colors.mark)) {
        Row(Modifier.padding(Spacing.l), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.m)) {
            ExerciseThumb(vid, name, size = 56.dp)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                Caption(stringResource(Res.string.end_unlock_new_exercise))
                Text(name, style = MaterialTheme.typography.titleLarge)
            }
        }
    }
}

/** One card per exercise: what happened, what comes next, and the place to say how it felt and correct what was logged. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ExerciseCard(
    e: ExerciseProgress, name: String, next: NextStep?, editor: CardEditor?, move: Suggestion?, nameOf: (String) -> String,
    onSave: (String, List<RoundCorrection>) -> Unit, onAct: (EndAction) -> Unit,
) {
    val unlocked = e.mastered || e.leveledUpTo != null
    Card(Modifier.fillMaxWidth().testTag("end_card_${e.variationId}"), shape = MaterialTheme.shapes.large, border = if (unlocked) goldBorder() else null) {
        Column(Modifier.padding(Spacing.l), verticalArrangement = Arrangement.spacedBy(Spacing.m)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.m)) {
                ExerciseThumb(e.variationId, name, size = if (unlocked) 56.dp else 52.dp)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                    if (unlocked) Text(
                        if (e.mastered) stringResource(Res.string.end_unlock_mastered) else stringResource(Res.string.end_unlock_level, e.leveledUpTo ?: e.tier),
                        Modifier.testTag("end_unlock_${e.variationId}"), style = MaterialTheme.typography.titleLarge,
                    )
                    Text(
                        name, style = if (unlocked) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.titleMedium,
                        color = if (unlocked) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                    )
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.s)) {
                        StarRow(
                            e.stars, size = if (unlocked) 24.dp else 18.dp, popFrom = if (unlocked) e.starsBefore.coerceAtMost(e.stars) else null,
                            current = !e.masteredAlready && !e.mastered,
                        )
                        Text(
                            if (e.masteredAlready) stringResource(Res.string.end_mastered) else stringResource(Res.string.history_level_of, e.tier),
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    if (e.levelUpBy == LevelUpBy.DEMONSTRATED) Text(
                        stringResource(Res.string.end_demonstrated, e.leveledUpTo ?: e.tier), Modifier.testTag("end_demonstrated_${e.variationId}"),
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    e.progress?.let { ProgressDots(e.variationId, it) }
                    val chips = buildList {
                        if (e.easierNextTime) add("easier" to stringResource(Res.string.end_chip_easier))
                        e.nextTarget?.let { add("target" to stringResource(if (e.nextTargetIsHold) Res.string.end_chip_next_seconds else Res.string.end_chip_next_reps, it)) }
                        if (e.levelKept) add("kept" to stringResource(Res.string.end_chip_kept))
                    }
                    if (chips.isNotEmpty()) FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.xs), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                        chips.forEach { (key, label) -> StatusChip(label, Modifier.testTag("end_chip_${e.variationId}_$key")) }
                    }
                }
            }
            next?.let { NextStepRow(e.variationId, it) }
            editor?.let { CardCorrection(e.variationId, e.tier, it, move, nameOf, onSave, onAct) }
        }
    }
}

@Composable
private fun NextStepRow(vid: String, next: NextStep) {
    HorizontalDivider()
    Row(Modifier.semantics(mergeDescendants = true) {}.testTag("end_next_$vid"), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.m)) {
        if (!next.hasOwnCard) ExerciseThumb(next.variationId, next.name, size = 44.dp)
        Text(
            stringResource(if (next.hasOwnCard) Res.string.end_next_exercise_name else Res.string.end_next_exercise, next.name),
            style = MaterialTheme.typography.titleMedium,
        )
    }
}

/** A card for an exercise whose comparison is not available: its name and the same place to say how it felt and correct what was logged. */
@Composable
private fun EditOnlyCard(vid: String, name: String, editor: CardEditor, onSave: (String, List<RoundCorrection>) -> Unit) {
    Card(Modifier.fillMaxWidth().testTag("end_fix_$vid"), shape = MaterialTheme.shapes.large) {
        Column(Modifier.padding(Spacing.l), verticalArrangement = Arrangement.spacedBy(Spacing.m)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.m)) {
                ExerciseThumb(vid, name, size = 44.dp)
                Text(name, style = MaterialTheme.typography.titleMedium)
            }
            CardCorrection(vid, 0, editor, null, { it }, onSave) {}
        }
    }
}

/**
 * How it felt (Too hard · Too easy · Pain, always visible), the move that follows from it (one tap applies it; nothing changes without the tap)
 * and, folded away, one number per round. Tags: `end_<vid>_too_hard|too_easy|pain`, `end_<vid>_act`, `end_<vid>_acted`, `end_edit_<vid>`, `end_<vid>_round<i>_value`.
 * [tier] 0 = no level to move (an exercise without levels, or no comparison).
 */
@Composable
private fun CardCorrection(
    vid: String, tier: Int, editor: CardEditor, move: Suggestion?, nameOf: (String) -> String,
    onSave: (String, List<RoundCorrection>) -> Unit, onAct: (EndAction) -> Unit,
) {
    var expanded by remember(vid) { mutableStateOf(false) }
    var tooHard by remember(vid) { mutableStateOf(editor.tooHard) }
    var tooEasy by remember(vid) { mutableStateOf(editor.tooEasy) }
    var acted by remember(vid) { mutableStateOf<EndAction?>(null) }
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        Caption(stringResource(Res.string.end_rate_caption))
        RoundEditor(
            editor.target, editor.rounds, sets = editor.sets, tooHard0 = editor.tooHard, tooEasy0 = editor.tooEasy, pain0 = editor.pain, tag = "end_$vid",
            alternating = editor.alternating, judged = editor.judged, showRounds = expanded, chipsFirst = true,
            onRating = { hard, easy ->
                if (hard != tooHard || easy != tooEasy) acted = null // a changed rating is a new question
                tooHard = hard; tooEasy = easy
            },
            afterChips = {
                val offered = when {
                    acted != null -> null
                    tooHard && tier > 1 -> EndAction.Easier(vid, tier - 1)
                    tooEasy -> when (move) {
                        is Suggestion.RaiseLevel -> EndAction.Harder(vid, move.toTier)
                        is Suggestion.NextExercise -> EndAction.Next(vid, move.toVariationId)
                        else -> null
                    }
                    else -> null
                }
                offered?.let { action ->
                    AppOutlinedButton({ acted = action; onAct(action) }, Modifier.testTag("end_${vid}_act")) {
                        Text(
                            when (action) {
                                is EndAction.Easier -> stringResource(Res.string.end_go_down, action.tier)
                                is EndAction.Harder -> stringResource(Res.string.end_go_up, action.tier)
                                is EndAction.Next -> stringResource(Res.string.end_go_next, nameOf(action.toVariationId))
                            },
                        )
                    }
                }
                acted?.let { action ->
                    Text(
                        when (action) {
                            is EndAction.Easier -> stringResource(Res.string.end_acted_level, action.tier)
                            is EndAction.Harder -> stringResource(Res.string.end_acted_level, action.tier)
                            is EndAction.Next -> stringResource(Res.string.end_acted_next, nameOf(action.toVariationId))
                        },
                        Modifier.testTag("end_${vid}_acted"), style = MaterialTheme.typography.bodyMedium, color = AppAccentTheme.colors.text,
                    )
                }
                AppTextButton(onClick = { expanded = !expanded }, modifier = Modifier.testTag("end_edit_$vid")) { Text(stringResource(Res.string.end_edit_logged)) }
            },
        ) { list -> onSave(vid, list) }
    }
}

@Composable
private fun StatusChip(text: String, modifier: Modifier) {
    Text(
        text, modifier.background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(50)).padding(horizontal = Spacing.s, vertical = 3.dp),
        style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

/**
 * Filled / empty dots: the good sessions so far against the ones needed, with the same sentence as text. A [LevelProgress.finished] streak is the one that
 * just raised the level (all filled, "at Level N" with the level it was earned at).
 */
@Composable
private fun ProgressDots(vid: String, p: LevelProgress) {
    val mastery = p.toLevel > 5
    val text = when {
        p.finished -> stringResource(if (p.weekly) Res.string.end_progress_done_weekly else Res.string.end_progress_done_sessions, p.have, p.need, p.toLevel - 1)
        mastery -> stringResource(if (p.weekly) Res.string.end_progress_mastery_weekly else Res.string.end_progress_mastery_sessions, p.have, p.need, p.toLevel)
        else -> stringResource(if (p.weekly) Res.string.end_progress_weekly else Res.string.end_progress_sessions, p.have, p.need, p.toLevel)
    }
    Row(
        Modifier.semantics(mergeDescendants = true) {}.testTag("end_progress_$vid"), verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.s),
    ) {
        Row(Modifier.clearAndSetSemantics {}, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            repeat(p.need) { i ->
                Box(Modifier.size(10.dp).background(if (i < p.have) AppAccentTheme.colors.mark else MaterialTheme.colorScheme.outlineVariant, CircleShape))
            }
        }
        Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
