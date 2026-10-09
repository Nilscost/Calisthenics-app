// U07 (F10, F11, F13, F14): the live workout. Observes the service-owned state; sends commands only.
// Work block: big clip, name, target, ring timer, Next. Recovery/stretch/get-ready block: the same, with a compact
// logger at the top for the work that just ended. No block is ever called "Rest" when stretch is on (CueText).
package io.github.gonbei774.calisthenicsmemory.ui.session

import io.github.gonbei774.calisthenicsmemory.ui.components.AppTextButton
import io.github.gonbei774.calisthenicsmemory.ui.components.AppOutlinedButton

import android.os.SystemClock
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import io.github.gonbei774.calisthenicsmemory.ui.theme.*
import app.calisthenics.domain.feedback.Execution
import app.calisthenics.domain.model.BlockType
import app.calisthenics.domain.model.Side
import app.calisthenics.domain.model.Target
import app.calisthenics.domain.model.TargetType
import app.calisthenics.domain.model.TimelineBlock
import app.calisthenics.domain.session.CueText
import app.calisthenics.domain.session.Phase
import app.calisthenics.domain.session.SessionState
import io.github.gonbei774.calisthenicsmemory.R
import io.github.gonbei774.calisthenicsmemory.session.SessionBus
import io.github.gonbei774.calisthenicsmemory.session.WorkoutSessionService
import io.github.gonbei774.calisthenicsmemory.ui.components.BigStepper
import io.github.gonbei774.calisthenicsmemory.ui.history.RoomHistorySource
import io.github.gonbei774.calisthenicsmemory.ui.screens.DemoClips
import io.github.gonbei774.calisthenicsmemory.ui.screens.DemoPlayer
import io.github.gonbei774.calisthenicsmemory.ui.screens.ProgressLoader
import io.github.gonbei774.calisthenicsmemory.ui.theme.Radius
import io.github.gonbei774.calisthenicsmemory.ui.theme.Spacing
import io.github.gonbei774.calisthenicsmemory.ui.train.formatClock
import io.github.gonbei774.calisthenicsmemory.ui.train.loadCatalog
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDate

/** The finished WORK block the logger belongs to: the last one before [st]'s current block, only while a recovery-type block runs. */
fun loggerBlock(st: SessionState): TimelineBlock? {
    val cur = st.currentBlock ?: return null
    if (cur.type != BlockType.STRETCH && cur.type != BlockType.PASSIVE_RECOVERY && cur.type != BlockType.TRANSITION) return null
    val last = st.plan.blocks.take(st.blockIndex).lastOrNull { it.type == BlockType.WORK && it.variationId != null } ?: return null
    // nothing but recovery-type blocks may sit between the work and now
    val between = st.plan.blocks.subList(st.plan.blocks.indexOf(last) + 1, st.blockIndex)
    if (between.any { it.type == BlockType.WORK }) return null
    val ex = st.executions[last.id]
    return last.takeIf { ex == Execution.COMPLETED || ex == Execution.PARTIAL }
}

@Composable
fun SessionScreen(modifier: Modifier = Modifier, onExit: () -> Unit) {
    val ctx = LocalContext.current
    val s by SessionBus.state.collectAsState()
    var nowTick by remember { mutableLongStateOf(SystemClock.elapsedRealtime()) }
    LaunchedEffect(Unit) { while (true) { nowTick = SystemClock.elapsedRealtime(); delay(250) } }
    val st = s
    if (st == null) {
        Box(modifier.fillMaxSize().statusBarsPadding(), contentAlignment = Alignment.Center) { Text(stringResource(R.string.session_starting)) }
        return
    }
    if (st.isTerminal) { EndScreen(st, modifier, onExit); return }
    LiveScreen(st, nowTick, modifier)
}

/** The state of the logger for the work block [work]: lifted out of the card so the DONE button in the bottom row can confirm it. */
@Stable
private class LoggerState(val ctx: android.content.Context, val work: TimelineBlock, saved: app.calisthenics.domain.session.BlockLog?) {
    val target: Target? = work.target
    val hold = target?.type == TargetType.HOLD_SECONDS
    var reps by mutableIntStateOf(saved?.reps ?: target?.value ?: 0)
    var typed by mutableStateOf(saved?.reps != null)
    var confirmed by mutableStateOf(false)
    // The ratings (Too hard / Too easy / Pain) are given on the end screen (doc 17 §2.3); what the logger keeps is passed through unchanged.
    private val tooHard = saved?.tooHard ?: false
    private val tooEasy = saved?.tooEasy ?: false
    private val pain = saved?.discomfort ?: false
    fun push() { logBlock(ctx, work.id, if (typed) reps else null, tooHard, pain, tooEasy) }
}

@Composable
private fun rememberLogger(st: SessionState, work: TimelineBlock?): LoggerState? {
    val ctx = LocalContext.current
    if (work == null) return null
    return remember(work.id) { LoggerState(ctx, work, st.logged[work.id]) }
}

private fun blockLabel(b: TimelineBlock): Int = when (b.type) {
    BlockType.WORK -> if (b.target?.type == TargetType.HOLD_SECONDS) R.string.block_hold else R.string.block_work
    BlockType.STRETCH -> R.string.block_stretch // with stretch on nothing is called "Rest" (CueText rule)
    BlockType.PASSIVE_RECOVERY -> R.string.block_rest
    BlockType.TRANSITION -> R.string.block_get_ready
    BlockType.WARMUP -> R.string.block_warmup
    BlockType.COOLDOWN -> R.string.block_cooldown
}

/**
 * V10 (R16, doc 17 §2.3): the clip on a white background fills the screen behind a transparent header (dark text); a dark bottom
 * sheet holds the big centred timer with its bar, the exercise and set, the logger (during recovery) and Pause · DONE · Skip.
 * No Too hard / Too easy / Pain here: they are given on the end screen.
 */
@Composable
private fun LiveScreen(st: SessionState, nowTick: Long, modifier: Modifier) {
    val ctx = LocalContext.current
    val b = st.currentBlock ?: return
    val names = SessionBus.names
    val title = CueText.label(st.plan, b.id, names)
    val remSec = ((st.remainingAt(nowTick) + 999) / 1000).toInt()
    val progress = (st.remainingAt(nowTick) / (b.durationSeconds * 1000f)).coerceIn(0f, 1f)
    val paused = st.phase == Phase.PAUSED
    var menu by remember { mutableStateOf(false) }
    var confirmEnd by remember { mutableStateOf(false) }
    val logFor = loggerBlock(st)
    val logger = rememberLogger(st, logFor)
    val haptic = LocalHapticFeedback.current
    val clip = remember(b.variationId) { b.variationId?.let { DemoClips.file(ctx, it) } }
    val elapsed = (st.activeElapsedMs / 1000).toInt()

    Column(modifier.fillMaxSize().background(Color.White)) {
        // ---- clip area with the transparent header (always the light palette: dark text on the white clip)
        MaterialTheme(colorScheme = appColorScheme(false), typography = AppTypography, shapes = AppShapes) {
            CompositionLocalProvider(LocalAppAccent provides LightAccent) {
                CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onBackground) { Box(Modifier.weight(1f).fillMaxWidth().background(Color.White)) {
                    if (clip != null) Box(Modifier.fillMaxSize().padding(top = 132.dp), contentAlignment = Alignment.Center) { DemoPlayer(clip, controls = false, fit = true) }
                    Column(Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = Spacing.l, vertical = Spacing.s), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Row(verticalAlignment = Alignment.Top) {
                            Column(Modifier.weight(1f)) {
                                val r = b.roundIndex
                                Text(
                                    (if (r != null) stringResource(R.string.session_round, r, st.plan.rounds) else stringResource(R.string.session_step, st.blockIndex + 1, st.plan.blocks.size)).uppercase(),
                                    style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onBackground, modifier = Modifier.testTag("session_round"),
                                )
                                SetMarkers(st.plan.rounds, b.roundIndex ?: 0)
                            }
                            Text(formatClock(elapsed), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.outline, modifier = Modifier.padding(top = 6.dp).testTag("session_elapsed"))
                            Box {
                                IconButton(onClick = { menu = true }, modifier = Modifier.testTag("session_more")) { Icon(Icons.Filled.MoreVert, stringResource(R.string.session_more), tint = MaterialTheme.colorScheme.onBackground) }
                                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                                    DropdownMenuItem(text = { Text(stringResource(R.string.session_end)) }, onClick = { menu = false; confirmEnd = true }, modifier = Modifier.testTag("session_end"))
                                }
                            }
                        }
                        Text(title, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onBackground, modifier = Modifier.testTag("session_title"))
                        CueText.nextLabel(st.plan, b.id, names)?.let { Text(stringResource(R.string.session_next, it), Modifier.testTag("session_next"), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    }
                } }
            }
        }

        // ---- the dark bottom sheet (always the dark palette)
        MaterialTheme(colorScheme = appColorScheme(true), typography = AppTypography, shapes = AppShapes) {
            CompositionLocalProvider(LocalAppAccent provides DarkAccent) {
                CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onBackground) { Box(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.background, RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp))) {
                    Column(Modifier.navigationBarsPadding().padding(horizontal = Spacing.l).padding(top = Spacing.m, bottom = Spacing.m), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                        Text(stringResource(blockLabel(b)).uppercase(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                        Text(
                            formatClock(remSec), style = MaterialTheme.typography.displayLarge.copy(fontSize = 60.sp), color = AppAccentTheme.colors.accent, maxLines = 1,
                            modifier = Modifier.semantics { contentDescription = ctx.getString(R.string.session_time_description, title, remSec / 60, remSec % 60) }.testTag("session_time"),
                        )
                        LinearProgressIndicator(progress = { progress }, Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)), color = AppAccentTheme.colors.accent, trackColor = MaterialTheme.colorScheme.surfaceVariant)
                        if (paused) Text(stringResource(R.string.session_paused), color = MaterialTheme.colorScheme.error, modifier = Modifier.testTag("session_paused"))
                        if (logger != null && logFor != null) RepLogger(logger, logFor, st)
                        else {
                            val sideText = when (b.side) { Side.LEFT -> stringResource(R.string.side_left); Side.RIGHT -> stringResource(R.string.side_right); else -> "" }
                            val targetText = b.target?.let { if (it.type == TargetType.REPS) stringResource(R.string.target_reps, it.value) else stringResource(R.string.target_seconds, it.value) }.orEmpty()
                            val sub = listOf(io.github.gonbei774.calisthenicsmemory.ui.train.withLoad(targetText, b.loadGrams), sideText).filter { it.isNotEmpty() }.joinToString(" · ")
                            if (b.type == BlockType.WORK) Text(names[b.variationId] ?: "", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground, textAlign = TextAlign.Center)
                            if (sub.isNotEmpty()) Text(sub, style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onBackground, modifier = Modifier.testTag("session_target"))
                        }
                        Row(Modifier.fillMaxWidth().padding(top = Spacing.xs), horizontalArrangement = Arrangement.spacedBy(Spacing.s), verticalAlignment = Alignment.CenterVertically) {
                            if (paused) AppOutlinedButton(onClick = { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); sessionCommand(ctx, WorkoutSessionService.ACTION_RESUME) }, Modifier.weight(1f).height(54.dp).testTag("session_resume")) { Text(stringResource(R.string.session_resume)) }
                            else AppOutlinedButton(onClick = { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); sessionCommand(ctx, WorkoutSessionService.ACTION_PAUSE) }, Modifier.weight(1f).height(54.dp).testTag("session_pause")) { Text(stringResource(R.string.session_pause)) }
                            // DONE in the middle, gold and a little wider: ends the set during work, confirms the logged number during recovery.
                            if (b.type == BlockType.WORK) Button(
                                onClick = { haptic.performHapticFeedback(HapticFeedbackType.LongPress); sessionCommand(ctx, WorkoutSessionService.ACTION_DONE) }, enabled = st.phase == Phase.RUNNING,
                                modifier = Modifier.weight(1.3f).height(58.dp).testTag("session_done"), shape = RoundedCornerShape(Radius.button),
                            ) { Text(stringResource(R.string.session_done).uppercase(), style = MaterialTheme.typography.titleLarge) }
                            else if (logger != null) Button(
                                onClick = { haptic.performHapticFeedback(HapticFeedbackType.LongPress); logger.push(); logger.confirmed = true }, enabled = !logger.confirmed,
                                modifier = Modifier.weight(1.3f).height(58.dp).testTag("logger_done"), shape = RoundedCornerShape(Radius.button),
                            ) { Text(stringResource(R.string.logger_done).uppercase(), style = MaterialTheme.typography.titleLarge) }
                            AppOutlinedButton(onClick = { haptic.performHapticFeedback(HapticFeedbackType.LongPress); sessionCommand(ctx, WorkoutSessionService.ACTION_SKIP) }, Modifier.weight(1f).height(54.dp).testTag("session_skip")) { Text(stringResource(R.string.session_skip)) }
                        }
                    }
                } }
            }
        }
    }

    if (confirmEnd) AlertDialog(
        onDismissRequest = { confirmEnd = false },
        title = { Text(stringResource(R.string.session_end_title)) },
        text = { Text(stringResource(R.string.session_end_text)) },
        confirmButton = { AppTextButton(onClick = { confirmEnd = false; sessionCommand(ctx, WorkoutSessionService.ACTION_FINISH) }, modifier = Modifier.testTag("session_end_confirm")) { Text(stringResource(R.string.session_end)) } },
        dismissButton = { AppTextButton(onClick = { confirmEnd = false }) { Text(stringResource(R.string.cancel)) } },
    )
}

/** One small bar per round; the rounds up to the current one are gold. */
@Composable
private fun SetMarkers(total: Int, current: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.padding(top = 4.dp).testTag("session_markers")) {
        for (i in 1..total.coerceAtMost(12)) Box(Modifier.width(22.dp).height(5.dp).clip(RoundedCornerShape(3.dp)).background(if (i <= current) AppAccentTheme.colors.text else MaterialTheme.colorScheme.surfaceVariant))
    }
}

/**
 * V03/V10 (R17): the logger during the recovery, in the bottom sheet: the exercise and set, then - [big number] +.
 * Prefilled with the target; every change is saved at once and untouched = as planned. DONE (in the bottom row) only confirms:
 * it folds the logger into one "Logged: N · Change" line (the timer is not touched).
 */
@Composable
private fun RepLogger(l: LoggerState, work: TimelineBlock, st: SessionState) {
    val name = SessionBus.names[work.variationId] ?: work.variationId.orEmpty()
    val setNo = work.roundIndex
    Column(Modifier.fillMaxWidth().testTag("logger"), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        Text(if (setNo != null) stringResource(R.string.logger_name_set, name, setNo) else name, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground, maxLines = 1, modifier = Modifier.testTag("logger_name"))
        if (l.confirmed) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.logger_logged, l.reps), style = MaterialTheme.typography.titleMedium, modifier = Modifier.testTag("logger_summary"))
                AppTextButton(onClick = { l.confirmed = false }, modifier = Modifier.testTag("logger_change")) { Text(stringResource(R.string.logger_edit)) }
            }
        } else {
            Text(stringResource(if (l.hold) R.string.logger_seconds_held else R.string.logger_reps_done), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
            BigStepper(l.reps, { l.reps = it; l.typed = true; l.push() }, 0..999, Modifier.fillMaxWidth(), step = if (l.hold && (l.target?.value ?: 0) >= 30) 5 else 1, tag = "logger")
        }
    }
}

@Composable
private fun EndScreen(st: SessionState, modifier: Modifier, onExit: () -> Unit) {
    val ctx = LocalContext.current
    val done = st.plan.blocks.filter { it.type == BlockType.WORK && st.executions[it.id] == Execution.COMPLETED }
    val exercises = done.mapNotNull { it.variationId }.distinct()
    val rounds = done.mapNotNull { it.roundIndex }.distinct().size
    val minutes = (st.activeElapsedMs / 60_000L).toInt()
    var events by remember { mutableStateOf<List<String>>(emptyList()) }
    var editing by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    // New level-ups appear once the session row is in Room; the progress is replayed from history, never stored.
    LaunchedEffect(Unit) {
        val deadline = System.currentTimeMillis() + 5000
        while (!SessionBus.saved && System.currentTimeMillis() < deadline) delay(100)
        val today = LocalDate.now().toEpochDay().toInt()
        events = try { ProgressLoader.load(ctx, loadCatalog(ctx)).events.filter { it.day == today }.map { it.message } } catch (_: Throwable) { emptyList() }
    }
    Column(modifier.fillMaxSize().statusBarsPadding().verticalScroll(rememberScrollState()).padding(Spacing.l), verticalArrangement = Arrangement.spacedBy(Spacing.m)) {
        Text(stringResource(if (st.phase == Phase.COMPLETED) R.string.end_complete else R.string.end_early), style = MaterialTheme.typography.headlineMedium, modifier = Modifier.testTag("end_title"))
        Card(Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.large) {
            Column(Modifier.padding(Spacing.l), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                Text(stringResource(R.string.end_summary, minutes, rounds, exercises.size), style = MaterialTheme.typography.titleMedium, modifier = Modifier.testTag("end_summary"))
                if (events.isNotEmpty()) events.forEach { Text(it, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.testTag("end_event")) }
                else Text(stringResource(R.string.end_saved), style = MaterialTheme.typography.bodyMedium)
            }
        }
        if (exercises.isNotEmpty()) {
            AppTextButton(onClick = { editing = !editing }, modifier = Modifier.testTag("end_edit")) { Text(stringResource(R.string.end_edit_logged)) }
            if (editing) exercises.forEach { vid ->
                val blocks = st.plan.blocks.filter { it.type == BlockType.WORK && it.variationId == vid && st.executions[it.id] == Execution.COMPLETED }
                val target = blocks.firstOrNull()?.target
                val logs = blocks.mapNotNull { st.logged[it.id] }
                Card(Modifier.fillMaxWidth().testTag("end_fix_$vid"), shape = MaterialTheme.shapes.large) {
                    Column(Modifier.padding(Spacing.l), verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
                        Text(SessionBus.names[vid] ?: vid, style = MaterialTheme.typography.titleMedium)
                        // V04b (R21): one number per round, plus Too hard / Too easy / Pain for the exercise.
                        RoundEditor(target, blocks.map { EditorRound(it.id, it.roundIndex ?: 1, it.side, st.logged[it.id]?.reps) },
                            tooHard0 = logs.any { it.tooHard }, tooEasy0 = logs.any { it.tooEasy }, pain0 = logs.any { it.discomfort }, tag = "end_$vid") { list ->
                            scope.launch {
                                val deadline = System.currentTimeMillis() + 5000
                                while (!SessionBus.saved && System.currentTimeMillis() < deadline) delay(100) // the session row must exist first
                                RoomHistorySource(ctx).reviseRounds(st.sessionId, vid, list)
                            }
                        }
                    }
                }
            }
        }
        Button(onClick = { SessionBus.clear(); onExit() }, Modifier.fillMaxWidth().height(56.dp).testTag("end_done"), shape = RoundedCornerShape(Radius.button)) { Text(stringResource(R.string.end_done)) }
    }
}
