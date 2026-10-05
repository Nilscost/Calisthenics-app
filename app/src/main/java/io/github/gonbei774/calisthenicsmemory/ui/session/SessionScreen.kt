// U07 (F10, F11, F13, F14): the live workout. Observes the service-owned state; sends commands only.
// Work block: big clip, name, target, ring timer, Next. Recovery/stretch/get-ready block: the same, with a compact
// logger at the top for the work that just ended. No block is ever called "Rest" when stretch is on (CueText).
package io.github.gonbei774.calisthenicsmemory.ui.session

import android.os.SystemClock
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
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
import io.github.gonbei774.calisthenicsmemory.ui.components.Stepper
import app.calisthenics.domain.history.ratingFor
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

    Column(modifier.fillMaxSize().statusBarsPadding().padding(horizontal = Spacing.l), verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                val r = b.roundIndex
                Text(
                    if (r != null) stringResource(R.string.session_round, r, st.plan.rounds) else stringResource(R.string.session_step, st.blockIndex + 1, st.plan.blocks.size),
                    style = MaterialTheme.typography.labelLarge, modifier = Modifier.testTag("session_round"),
                )
                LinearProgressIndicator(progress = { (st.blockIndex + 1f) / st.plan.blocks.size }, Modifier.fillMaxWidth().padding(top = Spacing.xs))
            }
            Box {
                IconButton(onClick = { menu = true }, modifier = Modifier.testTag("session_more")) { Icon(Icons.Filled.MoreVert, stringResource(R.string.session_more)) }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    DropdownMenuItem(text = { Text(stringResource(R.string.session_end)) }, onClick = { menu = false; confirmEnd = true }, modifier = Modifier.testTag("session_end"))
                }
            }
        }

        if (logFor != null) RepLogger(st, logFor)

        val clip = remember(b.variationId) { b.variationId?.let { DemoClips.file(ctx, it) } }
        if (clip != null) Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) { DemoPlayer(clip, controls = false) }
        else Spacer(Modifier.weight(1f))

        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            Text(title, style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center, modifier = Modifier.testTag("session_title"))
            val sideText = when (b.side) { Side.LEFT -> stringResource(R.string.side_left); Side.RIGHT -> stringResource(R.string.side_right); else -> "" }
            val targetText = b.target?.let { if (it.type == TargetType.REPS) stringResource(R.string.target_reps, it.value) else stringResource(R.string.target_seconds, it.value) }.orEmpty()
            val sub = listOf(targetText, sideText).filter { it.isNotEmpty() }.joinToString(" · ")
            if (sub.isNotEmpty()) Text(sub, style = MaterialTheme.typography.titleLarge, modifier = Modifier.testTag("session_target"))
            if (paused) Text(stringResource(R.string.session_paused), color = MaterialTheme.colorScheme.error, modifier = Modifier.testTag("session_paused"))
        }

        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            TimerRing(progress, Modifier.size(168.dp)) {
                Text(
                    formatClock(remSec), style = MaterialTheme.typography.displayLarge, maxLines = 1,
                    modifier = Modifier.semantics { contentDescription = ctx.getString(R.string.session_time_description, title, remSec / 60, remSec % 60) }.testTag("session_time"),
                )
            }
        }

        CueText.nextLabel(st.plan, b.id, names)?.let { Text(stringResource(R.string.session_next, it), Modifier.fillMaxWidth().testTag("session_next"), style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center) }

        Row(Modifier.fillMaxWidth().padding(bottom = Spacing.l), horizontalArrangement = Arrangement.spacedBy(Spacing.s), verticalAlignment = Alignment.CenterVertically) {
            if (paused) FilledTonalButton(onClick = { sessionCommand(ctx, WorkoutSessionService.ACTION_RESUME) }, Modifier.weight(1f).height(52.dp).testTag("session_resume")) { Text(stringResource(R.string.session_resume)) }
            else FilledTonalButton(onClick = { sessionCommand(ctx, WorkoutSessionService.ACTION_PAUSE) }, Modifier.weight(1f).height(52.dp).testTag("session_pause")) { Text(stringResource(R.string.session_pause)) }
            OutlinedButton(onClick = { sessionCommand(ctx, WorkoutSessionService.ACTION_SKIP) }, Modifier.weight(1f).height(52.dp).testTag("session_skip")) { Text(stringResource(R.string.session_skip)) }
            if (b.type == BlockType.WORK) Button(
                onClick = { sessionCommand(ctx, WorkoutSessionService.ACTION_DONE) }, enabled = st.phase == Phase.RUNNING,
                modifier = Modifier.weight(1f).height(52.dp).testTag("session_done"), shape = RoundedCornerShape(Radius.button),
            ) { Text(stringResource(R.string.session_done)) }
        }
    }

    if (confirmEnd) AlertDialog(
        onDismissRequest = { confirmEnd = false },
        title = { Text(stringResource(R.string.session_end_title)) },
        text = { Text(stringResource(R.string.session_end_text)) },
        confirmButton = { TextButton(onClick = { confirmEnd = false; sessionCommand(ctx, WorkoutSessionService.ACTION_FINISH) }, modifier = Modifier.testTag("session_end_confirm")) { Text(stringResource(R.string.session_end)) } },
        dismissButton = { TextButton(onClick = { confirmEnd = false }) { Text(stringResource(R.string.cancel)) } },
    )
}

@Composable
private fun TimerRing(progress: Float, modifier: Modifier, content: @Composable () -> Unit) {
    val track = MaterialTheme.colorScheme.surfaceVariant
    val arc = MaterialTheme.colorScheme.primary
    Box(modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.matchParentSize()) {
            val w = 12.dp.toPx()
            val inset = w / 2
            val sz = Size(size.width - w, size.height - w)
            drawArc(track, 0f, 360f, false, Offset(inset, inset), sz, style = Stroke(w))
            drawArc(arc, -90f, 360f * progress, false, Offset(inset, inset), sz, style = Stroke(w, cap = StrokeCap.Round))
        }
        content()
    }
}

/** "Push-up   − 10 +   [Too hard] [Pain]": prefilled with the target, saved the moment it changes. Untouched = as planned. */
@Composable
private fun RepLogger(st: SessionState, work: TimelineBlock) {
    val ctx = LocalContext.current
    val target: Target? = work.target
    val hold = target?.type == TargetType.HOLD_SECONDS
    val saved = st.logged[work.id]
    var reps by remember(work.id) { mutableIntStateOf(saved?.reps ?: target?.value ?: 0) }
    var tooHard by remember(work.id) { mutableStateOf(saved?.tooHard ?: false) }
    var pain by remember(work.id) { mutableStateOf(saved?.discomfort ?: false) }
    var typed by remember(work.id) { mutableStateOf(saved?.reps != null) }
    fun push() { logBlock(ctx, work.id, if (typed) reps else null, tooHard, pain) }
    val name = SessionBus.names[work.variationId] ?: work.variationId.orEmpty()
    Card(Modifier.fillMaxWidth().testTag("logger"), shape = MaterialTheme.shapes.large, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
        Column(Modifier.padding(Spacing.m), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(name, style = MaterialTheme.typography.titleMedium, maxLines = 1, modifier = Modifier.testTag("logger_name"))
                    Text(stringResource(if (hold) R.string.logger_seconds_held else R.string.logger_reps_done), style = MaterialTheme.typography.bodySmall)
                }
                Stepper(reps, { reps = it; typed = true; push() }, 0..999, step = if (hold && (target?.value ?: 0) >= 30) 5 else 1, tag = "logger")
            }
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s)) {
                FilterChip(selected = tooHard, onClick = { tooHard = !tooHard; push() }, label = { Text(stringResource(R.string.logger_too_hard)) }, modifier = Modifier.testTag("logger_too_hard"))
                FilterChip(selected = pain, onClick = { pain = !pain; push() }, label = { Text(stringResource(R.string.logger_pain)) }, modifier = Modifier.testTag("logger_pain"))
            }
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
            TextButton(onClick = { editing = !editing }, modifier = Modifier.testTag("end_edit")) { Text(stringResource(R.string.end_edit_logged)) }
            if (editing) exercises.forEach { vid ->
                val target = st.plan.blocks.firstOrNull { it.type == BlockType.WORK && it.variationId == vid }?.target
                val log = st.plan.blocks.filter { it.type == BlockType.WORK && it.variationId == vid }.mapNotNull { st.logged[it.id] }
                Card(Modifier.fillMaxWidth().testTag("end_fix_$vid"), shape = MaterialTheme.shapes.large) {
                    Column(Modifier.padding(Spacing.l), verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
                        Text(SessionBus.names[vid] ?: vid, style = MaterialTheme.typography.titleMedium)
                        FeedbackEditor(target, log.mapNotNull { it.reps }.minOrNull(), log.any { it.tooHard }, log.any { it.discomfort }, tag = "end_$vid") { reps, tooHard, pain ->
                            scope.launch {
                                val deadline = System.currentTimeMillis() + 5000
                                while (!SessionBus.saved && System.currentTimeMillis() < deadline) delay(100) // the session row must exist first
                                RoomHistorySource(ctx).revise(st.sessionId, vid, ratingFor(reps, target, tooHard), pain, reps)
                            }
                        }
                    }
                }
            }
        }
        Button(onClick = { SessionBus.clear(); onExit() }, Modifier.fillMaxWidth().height(56.dp).testTag("end_done"), shape = RoundedCornerShape(Radius.button)) { Text(stringResource(R.string.end_done)) }
    }
}
