// The live workout. Observes the service-owned state; sends commands only.
// Work block: big clip, name, target, ring timer, Next. Recovery/stretch/get-ready block: the same, with a compact
// logger at the top for the work that just ended. No block is ever called "Rest" when stretch is on (CueText).
package io.github.gonbei774.calisthenicsmemory.ui.session

import org.jetbrains.compose.resources.StringResource
import io.github.gonbei774.calisthenicsmemory.platform.PlatformContext
import io.github.gonbei774.calisthenicsmemory.platform.elapsedRealtimeMs
import io.github.gonbei774.calisthenicsmemory.platform.currentPlatformContext
import io.github.gonbei774.calisthenicsmemory.ui.components.AppTextButton
import io.github.gonbei774.calisthenicsmemory.ui.components.AppOutlinedButton

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import io.github.gonbei774.calisthenicsmemory.ui.icons.Icons
import io.github.gonbei774.calisthenicsmemory.ui.icons.filled.Add
import io.github.gonbei774.calisthenicsmemory.ui.icons.filled.MoreVert
import io.github.gonbei774.calisthenicsmemory.ui.theme.AppIcons
import io.github.gonbei774.calisthenicsmemory.ui.components.targetLabel
import io.github.gonbei774.calisthenicsmemory.ui.train.withLoad
import io.github.gonbei774.calisthenicsmemory.ui.screens.DemoClip
import androidx.compose.ui.text.style.TextOverflow
import app.calisthenics.domain.session.BlockLog
import app.calisthenics.domain.session.carriedReps
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import io.github.gonbei774.calisthenicsmemory.platform.exposeTestTagsAsResourceId
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import io.github.gonbei774.calisthenicsmemory.ui.theme.*
import app.calisthenics.domain.feedback.Execution
import app.calisthenics.domain.model.BlockType
import app.calisthenics.domain.model.Catalog
import app.calisthenics.domain.model.ExerciseVariation
import app.calisthenics.domain.model.Side
import app.calisthenics.domain.model.Target
import app.calisthenics.domain.model.TargetType
import app.calisthenics.domain.model.TimelineBlock
import app.calisthenics.domain.session.CueText
import app.calisthenics.domain.session.HeaderKind
import app.calisthenics.domain.session.headerFor
import app.calisthenics.domain.session.Phase
import app.calisthenics.domain.session.SessionState
import io.github.gonbei774.calisthenicsmemory.shared.resources.*
import io.github.gonbei774.calisthenicsmemory.session.SessionBus
import io.github.gonbei774.calisthenicsmemory.session.SaveStatus
import app.calisthenics.domain.history.overviewOf
import app.calisthenics.domain.history.recordOfState
import io.github.gonbei774.calisthenicsmemory.ui.components.BigStepper
import io.github.gonbei774.calisthenicsmemory.ui.components.ExerciseThumb
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

/**
 * The finished WORK block the break logger belongs to: the last one before [st]'s current block, only while a recovery-type block runs.
 * R7-T03 (D3): rep blocks are logged on their own work screen; only holds are logged on the break that follows.
 */
fun loggerBlock(st: SessionState): TimelineBlock? {
    val cur = st.currentBlock ?: return null
    if (cur.type != BlockType.STRETCH && cur.type != BlockType.PASSIVE_RECOVERY && cur.type != BlockType.TRANSITION) return null
    val last = st.plan.blocks.take(st.blockIndex).lastOrNull { it.type == BlockType.WORK && it.variationId != null } ?: return null
    // nothing but recovery-type blocks may sit between the work and now
    val between = st.plan.blocks.subList(st.plan.blocks.indexOf(last) + 1, st.blockIndex)
    if (between.any { it.type == BlockType.WORK }) return null
    val ex = st.executions[last.id]
    if (st.plan.fixedProgramId != null && st.plan.flow && last.prescriptionTier == null) return null
    if (last.target?.type != TargetType.HOLD_SECONDS) return null
    return last.takeIf { ex == Execution.COMPLETED || ex == Execution.PARTIAL }
}

/** R7-T03 (D3): the rep work block that is running now carries its own logger (− number +, Too easy). */
fun repLoggerBlock(st: SessionState): TimelineBlock? =
    st.currentBlock?.takeIf { it.type == BlockType.WORK && it.variationId != null &&
            !(st.plan.fixedProgramId != null && st.plan.flow && it.prescriptionTier == null) &&
            (it.target?.type == TargetType.REPS || (st.plan.fixedProgramId != null && !st.plan.flow && it.target?.type == TargetType.HOLD_SECONDS))
    }

@Composable
fun SessionScreen(modifier: Modifier = Modifier, onExit: () -> Unit) {
    val ctx = currentPlatformContext()
    val s by SessionBus.state.collectAsState()
    var nowTick by remember { mutableLongStateOf(elapsedRealtimeMs()) }
    LaunchedEffect(Unit) { while (true) { nowTick = elapsedRealtimeMs(); delay(250) } }
    val st = s
    if (st == null) {
        // A restored SESSION route after process death has no live session and nothing starting: back to Train, which offers the unfinished workout.
        val starting by SessionBus.starting.collectAsState()
        if (!starting) { LaunchedEffect(Unit) { onExit() }; Box(modifier.fillMaxSize()); return }
        Box(modifier.fillMaxSize().statusBarsPadding(), contentAlignment = Alignment.Center) { Text(stringResource(Res.string.session_starting)) }
        return
    }
    if (st.isTerminal) { EndScreen(st, modifier, onExit); return }
    LiveScreen(st, nowTick, modifier)
}

private fun timerOnlyFlowBlock(st: SessionState, block: TimelineBlock): Boolean =
    st.plan.fixedProgramId != null && st.plan.flow && block.type in setOf(BlockType.WORK, BlockType.TRANSITION) && block.prescriptionTier == null

/** Written fallback used while a demonstration is held for teaching review. Kept separate so synthetic holds remain testable after all clips are approved. */
@Composable
fun HeldDemoInstructions(demonstration: ExerciseVariation, modifier: Modifier = Modifier) {
    if (!demonstration.demonstrationHold) return
    Column(modifier.testTag("session_demo_held"), verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
        Text(stringResource(Res.string.progress_teaching_hold), style = MaterialTheme.typography.labelLarge)
        demonstration.instructions.take(3).forEach { Text(it, style = MaterialTheme.typography.bodyMedium) }
    }
}

/** The state of the logger for the work block [work]: lifted out of the card so the DONE button in the bottom row can confirm it. */
@Stable
private class LoggerState(val ctx: PlatformContext, val work: TimelineBlock, saved: BlockLog?, /** R8.1-T03 (F3): the number of the previous set of this exercise. */ val carried: Int? = null) {
    val target: Target? = work.target
    val hold = target?.type == TargetType.HOLD_SECONDS
    /** The previous set's number is shown and accepted as typed, so DONE without editing records what the screen showed. */
    private val fromCarry = saved?.reps == null && carried != null
    var reps by mutableIntStateOf(saved?.reps ?: carried ?: target?.shownValue() ?: 0)
    var typed by mutableStateOf(saved?.reps != null || fromCarry)
    fun pushCarried() { if (fromCarry) push() }
    // The ratings (Too hard / Too easy / Pain) are given on the end screen (doc 17 §2.3); what the logger keeps is passed through unchanged.
    private val tooHard = saved?.tooHard ?: false
    var tooEasy by mutableStateOf(saved?.tooEasy ?: false)
    private val pain = saved?.discomfort ?: false
    fun push() { logBlock(ctx, work.id, if (typed) reps else null, tooHard, pain, tooEasy) }
}

@Composable
private fun rememberLogger(st: SessionState, work: TimelineBlock?): LoggerState? {
    val ctx = currentPlatformContext()
    if (work == null) return null
    val state = remember(work.id) { LoggerState(ctx, work, st.logged[work.id], carriedReps(st.plan, st.logged, work)) }
    LaunchedEffect(work.id) { state.pushCarried() }
    return state
}

private fun blockLabel(b: TimelineBlock): StringResource = when (b.type) {
    BlockType.WORK -> if (b.target?.type == TargetType.HOLD_SECONDS) Res.string.block_hold else Res.string.block_work
    BlockType.STRETCH -> Res.string.block_stretch // with stretch on nothing is called "Rest" (CueText rule)
    BlockType.PASSIVE_RECOVERY -> Res.string.block_rest
    BlockType.TRANSITION -> Res.string.block_get_ready
    BlockType.WARMUP -> Res.string.block_warmup
    BlockType.COOLDOWN -> Res.string.block_cooldown
}

/**
 * The live workout: the clip on a white background fills the screen behind a transparent header (dark text); a bottom sheet
 * holds the big centred timer with its bar, the exercise and its target (SessionInfo), and Pause - DONE - Skip.
 * No Too hard / Too easy / Pain here: they are given on the end screen.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun LiveScreen(st: SessionState, nowTick: Long, modifier: Modifier, catalogOverride: Catalog? = null) {
    val ctx = currentPlatformContext()
    val b = st.currentBlock ?: return
    val names = remember(st.plan, SessionBus.names) { sessionDisplayNames(st.plan, SessionBus.names) }
    val title = CueText.label(st.plan, b.id, names)
    val remSec = ((st.remainingAt(nowTick) + 999) / 1000).toInt()
    val progress = (st.remainingAt(nowTick) / (b.durationSeconds * 1000f)).coerceIn(0f, 1f)
    val paused = st.phase == Phase.PAUSED
    val logFor = loggerBlock(st)
    val repWork = repLoggerBlock(st)
    val logger = rememberLogger(st, logFor ?: repWork)
    val catalog = remember(catalogOverride) { catalogOverride ?: loadCatalog(ctx) }
    val heldIds = remember(catalogOverride) { catalogOverride?.variations?.filter { it.demonstrationHold }?.map { it.id }?.toSet() }
    val upcomingBlock = remember(st.plan, st.blockIndex, b) {
        if (b.type != BlockType.TRANSITION) b else b.takeIf { it.variationId != null }
            ?: st.plan.blocks.drop(st.blockIndex + 1).firstOrNull { it.type == BlockType.WORK && it.variationId != null }
    }
    val demoVariationId = if (b.type == BlockType.TRANSITION) upcomingBlock?.variationId else b.variationId
    val demonstration = remember(demoVariationId, catalog) { demoVariationId?.let { catalog.variation(it) } }
    val clip = remember(demoVariationId, catalogOverride) {
        demoVariationId?.let { id -> if (heldIds == null) DemoClips.file(ctx, id) else DemoClips.file(ctx, id, heldIds) }
    }
    val sessionScheme = MaterialTheme.colorScheme
    val sessionAccent = AppAccentTheme.colors
    var confirmEnd by remember { mutableStateOf(false) }

    Column(modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        // the clip area with the transparent header: always the light palette (dark text on the white clip)
        MaterialTheme(colorScheme = appColorScheme(false), typography = AppTypography, shapes = AppShapes) {
            CompositionLocalProvider(LocalAppAccent provides LightAccent) {
                CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onBackground) { Box(Modifier.weight(1f).fillMaxWidth().background(Color.White)) {
                    ClipArea(clip, demonstration)
                    SessionHeader(st, b, title, names, demonstration, onEnd = { confirmEnd = true })
                } }
            }
        }

        // the controls follow the saved app appearance; the bundled media above keeps its white canvas
        MaterialTheme(colorScheme = sessionScheme, typography = AppTypography, shapes = AppShapes) {
            CompositionLocalProvider(LocalAppAccent provides sessionAccent) {
                CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onBackground) { Box(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.background, RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp))) {
                    Column(Modifier.navigationBarsPadding().padding(horizontal = Spacing.l).padding(top = Spacing.m, bottom = Spacing.m), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                        // the block caption is readable (titleMedium, full contrast) and a stretch block names its stretch
                        val caption = (if (b.type == BlockType.TRANSITION) stringResource(Res.string.feedback_prepare_upcoming) else stringResource(blockLabel(b))).uppercase() + if (b.type == BlockType.STRETCH) (names[b.variationId]?.let { " · $it" } ?: "") else ""
                        Text(caption, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground, textAlign = TextAlign.Center, modifier = Modifier.testTag("session_caption"))
                        val timeDescription = stringResource(Res.string.session_time_description, title, remSec / 60, remSec % 60)
                        Text(
                            formatClock(remSec), style = MaterialTheme.typography.displayLarge.copy(fontSize = 60.sp), color = AppAccentTheme.colors.accent, maxLines = 1,
                            modifier = Modifier.semantics { contentDescription = timeDescription }.testTag("session_time"),
                        )
                        LinearProgressIndicator(progress = { progress }, Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)), color = AppAccentTheme.colors.accent, trackColor = MaterialTheme.colorScheme.surfaceVariant)
                        if (paused) Text(stringResource(Res.string.session_paused), color = MaterialTheme.colorScheme.error, modifier = Modifier.testTag("session_paused"))
                        SessionInfo(b, upcomingBlock, names, logger, logFor, repWork)
                        SessionButtons(st, b, logger, repWork, paused)
                    }
                } }
            }
        }
    }

    if (confirmEnd) AlertDialog(
        modifier = Modifier.exposeTestTagsAsResourceId(),
        onDismissRequest = { confirmEnd = false },
        title = { Text(stringResource(Res.string.session_end_title)) },
        text = { Text(stringResource(Res.string.session_end_text)) },
        confirmButton = { AppTextButton(onClick = { confirmEnd = false; sessionCommand(ctx, SessionCommand.FINISH) }, modifier = Modifier.testTag("session_end_confirm")) { Text(stringResource(Res.string.session_end)) } },
        dismissButton = { AppTextButton(onClick = { confirmEnd = false }) { Text(stringResource(Res.string.cancel)) } },
    )
}

/** The demonstration clip, or its written fallback while a clip is held for review, or the thumbnail with the first cues. */
@Composable
private fun BoxScope.ClipArea(clip: DemoClip?, demonstration: ExerciseVariation?) {
    if (clip != null) Box(Modifier.fillMaxSize().padding(top = 132.dp), contentAlignment = Alignment.Center) { DemoPlayer(clip, controls = false, fit = true) }
    else if (demonstration?.demonstrationHold == true) HeldDemoInstructions(
        demonstration,
        Modifier.align(Alignment.Center).padding(horizontal = Spacing.l).padding(top = 132.dp),
    )
    else if (demonstration != null) Column(Modifier.align(Alignment.Center).padding(horizontal = Spacing.l).padding(top = 132.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
        ExerciseThumb(demonstration.id, demonstration.name, 180.dp)
        Text(demonstration.name, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
        demonstration.instructions.take(2).forEach { Text(it, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center) }
    }
}

/** The transparent header over the clip: round, clock, menu, the block title, the red cautions and what comes next. */
@Composable
private fun SessionHeader(st: SessionState, b: TimelineBlock, title: String, names: Map<String, String>, demonstration: ExerciseVariation?, onEnd: () -> Unit) {
    var menu by remember { mutableStateOf(false) }
    val elapsed = (st.activeElapsedMs / 1000).toInt()
    Column(Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = Spacing.l, vertical = Spacing.s), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f)) {
                val hd = headerFor(st.plan, b, st.blockIndex)
                Text(
                    when (hd.kind) {
                        HeaderKind.ROUND -> stringResource(Res.string.session_round, hd.set, hd.sets)
                        HeaderKind.PAIR -> stringResource(Res.string.session_pair_set, hd.group ?: 1, hd.set, hd.sets)
                        HeaderKind.EXERCISE -> stringResource(Res.string.session_exercise_set, hd.group ?: 1, hd.set, hd.sets)
                        HeaderKind.STEP -> stringResource(Res.string.session_step, hd.set, hd.sets)
                        HeaderKind.WARMUP -> stringResource(Res.string.session_warmup, hd.set, hd.sets)
                        HeaderKind.COOLDOWN -> stringResource(Res.string.session_cooldown, hd.set, hd.sets)
                    }.uppercase(),
                    style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onBackground, modifier = Modifier.testTag("session_round"),
                )
                SetMarkers(st.plan.rounds, b.roundIndex ?: 0)
            }
            Text(formatClock(elapsed), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.outline, modifier = Modifier.padding(top = 6.dp).testTag("session_elapsed"))
            Box {
                IconButton(onClick = { menu = true }, modifier = Modifier.testTag("session_more")) { Icon(Icons.Filled.MoreVert, stringResource(Res.string.session_more), tint = MaterialTheme.colorScheme.onBackground) }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    DropdownMenuItem(text = { Text(stringResource(Res.string.session_end)) }, onClick = { menu = false; onEnd() }, modifier = Modifier.testTag("session_end"))
                }
            }
        }
        Text(title, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onBackground, modifier = Modifier.testTag("session_title"))
        // the light-red "pay attention to" points of Jefferson curl and German hang, on the workout screen too
        val strongCautions = demonstration?.takeIf { it.cautionStrong }?.cautions.orEmpty()
        if (strongCautions.isNotEmpty() && b.type != BlockType.TRANSITION) Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(CautionRedLightSurface).border(1.dp, CautionRedLightBorder, RoundedCornerShape(10.dp)).padding(Spacing.s).testTag("session_caution_red")) {
            strongCautions.forEach { Text(it, style = MaterialTheme.typography.bodySmall, color = CautionRedLightText) }
        }
        CueText.nextLabel(st.plan, b.id, names)?.takeIf { it != title }?.let { Text(stringResource(Res.string.session_next, it), Modifier.testTag("session_next"), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
    }
}

/**
 * The exercise name and its target in the sheet. Prepare shows what comes next (name, target and side) and, only when the block before was a hold,
 * one compact row to correct that hold. Rest and stretch blocks keep the hold logger as the main element; a rep work block carries its own logger.
 */
@Composable
private fun SessionInfo(
    b: TimelineBlock, upcoming: TimelineBlock?, names: Map<String, String>, logger: LoggerState?, logFor: TimelineBlock?, repWork: TimelineBlock?,
) {
    val prepare = b.type == BlockType.TRANSITION
    val infoBlock = if (prepare) upcoming ?: b else b
    val sideText = when (infoBlock.side) { Side.LEFT -> stringResource(Res.string.side_left); Side.RIGHT -> stringResource(Res.string.side_right); else -> "" }
    val targetText = infoBlock.target?.let { targetLabel(it, infoBlock.alternating) }.orEmpty()
    val sub = listOf(withLoad(targetText, infoBlock.loadGrams), sideText).filter { it.isNotEmpty() }.joinToString(" · ")
    val name = names[infoBlock.variationId].orEmpty()
    if (prepare) {
        if (name.isNotEmpty()) Text(name, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground, textAlign = TextAlign.Center, modifier = Modifier.testTag("session_name"))
        if (sub.isNotEmpty()) Text(sub, style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onBackground, modifier = Modifier.testTag("session_target"))
        if (logger != null && logFor != null) RepLogger(logger, logFor, showName = true, compact = true)
        b.note?.let { SessionNote(it) }
    } else if (logger != null && logFor != null) {
        RepLogger(logger, logFor, showName = true)
    } else if (logger != null && repWork != null) {
        // the rep work block: name, target, then the number to log
        Text(name, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground, textAlign = TextAlign.Center)
        if (sub.isNotEmpty()) Text(sub, style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onBackground, modifier = Modifier.testTag("session_target"))
        RepLogger(logger, repWork, showName = false)
    } else {
        if ((b.type == BlockType.WORK || b.note != null) && name.isNotEmpty()) Text(name, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground, textAlign = TextAlign.Center)
        if (sub.isNotEmpty()) Text(sub, style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onBackground, modifier = Modifier.testTag("session_target"))
        b.note?.let { SessionNote(it) }
    }
}

@Composable
private fun SessionNote(note: String) =
    Text(note, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onBackground, textAlign = TextAlign.Center, modifier = Modifier.testTag("session_note"))

/** Pause (or Resume) - DONE - Skip. DONE ends the set during work; in any other block it saves the pending log and moves on. */
@Composable
private fun SessionButtons(st: SessionState, b: TimelineBlock, logger: LoggerState?, repWork: TimelineBlock?, paused: Boolean) {
    val ctx = currentPlatformContext()
    val haptic = LocalHapticFeedback.current
    Row(Modifier.fillMaxWidth().padding(top = Spacing.xs), horizontalArrangement = Arrangement.spacedBy(Spacing.s), verticalAlignment = Alignment.CenterVertically) {
        // while paused, Resume is the filled gold button on one line (smaller label and padding so it fits at font 1.3); Pause stays outlined
        if (paused) Button(onClick = { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); sessionCommand(ctx, SessionCommand.RESUME) }, Modifier.weight(1f).height(54.dp).testTag("session_resume"),
            shape = RoundedCornerShape(Radius.button), contentPadding = PaddingValues(horizontal = 8.dp)) { Text(stringResource(Res.string.session_resume), maxLines = 1, softWrap = false, style = MaterialTheme.typography.labelLarge) }
        else AppOutlinedButton(onClick = { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); sessionCommand(ctx, SessionCommand.PAUSE) }, Modifier.weight(1f).height(54.dp).testTag("session_pause")) { Text(stringResource(Res.string.session_pause)) }
        // Flow programs advance on their timers and expose no per-item logging or skip controls.
        if (timerOnlyFlowBlock(st, b)) Unit
        else if (b.type == BlockType.WORK) Button(
            onClick = { haptic.performHapticFeedback(HapticFeedbackType.LongPress); if (repWork != null) logger?.push(); sessionCommand(ctx, SessionCommand.DONE) }, enabled = st.phase == Phase.RUNNING,
            modifier = Modifier.weight(1.3f).height(58.dp).testTag("session_done"), shape = RoundedCornerShape(Radius.button),
        ) { Text(stringResource(Res.string.session_done).uppercase(), style = MaterialTheme.typography.titleLarge) }
        else Button(
            onClick = { haptic.performHapticFeedback(HapticFeedbackType.LongPress); logger?.push(); sessionCommand(ctx, SessionCommand.SKIP) }, enabled = st.phase == Phase.RUNNING,
            modifier = Modifier.weight(1.3f).height(58.dp).testTag(if (logger != null) "logger_done" else "break_done"), shape = RoundedCornerShape(Radius.button),
        ) { Text(stringResource(Res.string.logger_done).uppercase(), style = MaterialTheme.typography.titleLarge) }
        if (!timerOnlyFlowBlock(st, b)) AppOutlinedButton(onClick = { haptic.performHapticFeedback(HapticFeedbackType.LongPress); sessionCommand(ctx, SessionCommand.SKIP) }, Modifier.weight(1f).height(54.dp).testTag("session_skip")) { Text(stringResource(Res.string.session_skip)) }
    }
}

/** One small bar per round; the rounds up to the current one are gold. */
@Composable
private fun SetMarkers(total: Int, current: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.padding(top = 4.dp).testTag("session_markers")) {
        for (i in 1..total.coerceAtMost(12)) Box(Modifier.width(22.dp).height(5.dp).clip(RoundedCornerShape(3.dp)).background(if (i <= current) AppAccentTheme.colors.text else MaterialTheme.colorScheme.surfaceVariant))
    }
}

/**
 * The logger for a finished hold, in the bottom sheet: the exercise and set, then - [big number] +.
 * Prefilled with the target; every change is saved at once and untouched = as planned. DONE (in the bottom row) saves the number and moves on.
 * [compact] is the one-row form of the Prepare sheet: "Side Plank held  - 40 s +", same tags and the same values written.
 */
@Composable
private fun RepLogger(l: LoggerState, work: TimelineBlock, showName: Boolean, compact: Boolean = false) {
    val name = sessionDisplayName(SessionBus.names[work.variationId] ?: work.variationId.orEmpty(), work.side)
    val setNo = work.roundIndex
    val step = if (l.hold && (l.target?.shownValue() ?: 0) >= 30) 5 else 1
    val suffix = stringResource(if (l.hold) Res.string.unit_suffix_s else Res.string.unit_suffix_x)
    if (compact) {
        Row(Modifier.fillMaxWidth().testTag("logger"), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.s)) {
            Text(
                stringResource(Res.string.prepare_held, name), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f).testTag("logger_name"),
            )
            FilledTonalIconButton(onClick = { l.reps = (l.reps - step).coerceAtLeast(0); l.typed = true; l.push() }, enabled = l.reps > 0, modifier = Modifier.size(40.dp).testTag("logger_minus")) {
                Icon(AppIcons.Remove, contentDescription = stringResource(Res.string.stepper_decrease), modifier = Modifier.size(20.dp))
            }
            Text(l.reps.toString(), style = MaterialTheme.typography.titleLarge, maxLines = 1, modifier = Modifier.semantics { contentDescription = l.reps.toString() }.testTag("logger_value"))
            Text(suffix, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, modifier = Modifier.testTag("logger_unit"))
            FilledTonalIconButton(onClick = { l.reps = (l.reps + step).coerceAtMost(999); l.typed = true; l.push() }, enabled = l.reps < 999, modifier = Modifier.size(40.dp).testTag("logger_plus")) {
                Icon(Icons.Filled.Add, contentDescription = stringResource(Res.string.stepper_increase), modifier = Modifier.size(20.dp))
            }
        }
        return
    }
    Column(Modifier.fillMaxWidth().testTag("logger"), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        if (showName) Text(if (setNo != null) stringResource(Res.string.logger_name_set, name, setNo) else name, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground, maxLines = 1, modifier = Modifier.testTag("logger_name"))
        Text(stringResource(if (l.hold) (if (work.alternating) Res.string.logger_seconds_held_per_side else Res.string.logger_seconds_held) else (if (work.alternating) Res.string.logger_reps_done_per_side else Res.string.logger_reps_done)), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.s)) {
            BigStepper(l.reps, { l.reps = it; l.typed = true; l.push() }, 0..999, Modifier.weight(1f), step = step, tag = "logger", suffix = suffix)
        }
    }
}
