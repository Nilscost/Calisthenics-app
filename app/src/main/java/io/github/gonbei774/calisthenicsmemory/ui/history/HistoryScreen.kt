// U12 (§3.8): the History tab. A week strip with a dot on every day you trained, the workouts of the week, and a detail
// screen with what was logged in each round. A correction adds a new feedback revision; nothing is overwritten.
package io.github.gonbei774.calisthenicsmemory.ui.history

import io.github.gonbei774.calisthenicsmemory.ui.theme.AppAccentTheme

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.background
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.calisthenics.domain.feedback.Rating
import app.calisthenics.domain.history.*
import app.calisthenics.domain.model.Side
import app.calisthenics.domain.model.TargetType
import io.github.gonbei774.calisthenicsmemory.R
import app.calisthenics.domain.feedback.effectiveRounds
import io.github.gonbei774.calisthenicsmemory.ui.session.EditorRound
import io.github.gonbei774.calisthenicsmemory.ui.session.RoundEditor
import io.github.gonbei774.calisthenicsmemory.ui.theme.Spacing
import io.github.gonbei774.calisthenicsmemory.ui.train.loadCatalog
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.format.TextStyle
import java.util.Locale

private val dateFmt: DateTimeFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(modifier: Modifier = Modifier, source: HistorySource? = null, today: LocalDate = LocalDate.now(), onOpen: (String) -> Unit) {
    val ctx = LocalContext.current
    val src = source ?: remember { RoomHistorySource(ctx) }
    val zone = remember { ZoneId.systemDefault() }
    var all by remember { mutableStateOf<List<StoredSession>?>(null) }
    LaunchedEffect(Unit) { all = try { src.sessions() } catch (_: Throwable) { emptyList() } }
    var weekStart by remember { mutableStateOf(weekStartOf(today)) }
    var selected by remember { mutableStateOf<LocalDate?>(null) }

    Scaffold(modifier = modifier, topBar = { TopAppBar(title = { Text(stringResource(R.string.tab_history)) }) }) { pad ->
        Column(Modifier.padding(pad).fillMaxSize().verticalScroll(rememberScrollState()).padding(Spacing.l), verticalArrangement = Arrangement.spacedBy(Spacing.m)) {
            val list = all
            if (list == null) { Text(stringResource(R.string.history_loading)); return@Column }
            val finished = finishedSessions(list.map { it.record }).map { it.sessionId }.toSet()
            val overviews = list.filter { it.record.sessionId in finished }.map { overviewOf(it.plan, it.record, zone) }
            val days = weekDays(weekStart)
            val trained = overviews.map { it.day }.toSet()
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { weekStart = weekStart.minusWeeks(1); selected = null }, modifier = Modifier.testTag("week_prev")) { Icon(Icons.Filled.KeyboardArrowLeft, stringResource(R.string.history_prev_week)) }
                Text(stringResource(R.string.history_week_range, dateFmt.format(days.first()), dateFmt.format(days.last())), Modifier.weight(1f).testTag("week_label"), textAlign = TextAlign.Center, style = MaterialTheme.typography.titleMedium)
                IconButton(onClick = { weekStart = weekStart.plusWeeks(1); selected = null }, enabled = weekStart < weekStartOf(today), modifier = Modifier.testTag("week_next")) { Icon(Icons.Filled.KeyboardArrowRight, stringResource(R.string.history_next_week)) }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                days.forEach { d ->
                    val on = d in trained; val sel = selected == d
                    val dayDesc = dateFmt.format(d) + if (on) " · " + stringResource(R.string.history_day_trained) else ""
                    Column(
                        Modifier.weight(1f).clip(MaterialTheme.shapes.medium).background(if (sel) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface)
                            .clickable(enabled = on) { selected = if (sel) null else d }.padding(vertical = Spacing.s).semantics(mergeDescendants = true) { contentDescription = dayDesc }.testTag("day_$d"),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(d.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()), style = MaterialTheme.typography.labelSmall)
                        Text(d.dayOfMonth.toString(), style = MaterialTheme.typography.titleMedium)
                        Box(Modifier.size(8.dp).clip(CircleShape).background(if (on) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface).testTag(if (on) "dot_$d" else "nodot_$d"))
                    }
                }
            }
            val inWeek = overviews.filter { it.day in days }.sortedByDescending { it.startedAtEpochMs }
            val shown = selected?.let { s -> inWeek.filter { it.day == s } } ?: inWeek
            if (inWeek.isNotEmpty()) {
                val week = weeklySummaries(list.filter { it.record.sessionId in finished }.map { it.record }, zone).firstOrNull { it.weekStart == weekStart }
                if (week != null) Text(stringResource(R.string.history_week_summary, week.sessions, week.workSeconds / 60, week.stretchSeconds / 60), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.testTag("week_summary"))
            }
            if (shown.isEmpty()) Text(stringResource(if (overviews.isEmpty()) R.string.history_empty else R.string.history_empty_week), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.testTag("history_empty"))
            shown.forEach { o ->
                Card(Modifier.fillMaxWidth().clickable { onOpen(o.sessionId) }.testTag("session_${o.sessionId}"), shape = MaterialTheme.shapes.large) {
                    Column(Modifier.padding(Spacing.l), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                        Text(dateFmt.format(o.day), style = MaterialTheme.typography.titleMedium)
                        Text(stringResource(R.string.history_session_line, o.minutes, o.rounds, o.exercises), style = MaterialTheme.typography.bodyLarge)
                        if (o.finishedEarly) Text(stringResource(R.string.history_finished_early), style = MaterialTheme.typography.labelLarge, color = AppAccentTheme.colors.text)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionDetailScreen(sessionId: String, modifier: Modifier = Modifier, source: HistorySource? = null, onBack: () -> Unit) {
    val ctx = LocalContext.current
    val src = source ?: remember { RoomHistorySource(ctx) }
    val scope = rememberCoroutineScope()
    val catalog = remember { runCatching { loadCatalog(ctx) }.getOrNull() }
    var stored by remember { mutableStateOf<StoredSession?>(null) }
    var loaded by remember { mutableStateOf(false) }
    var corrected by remember { mutableStateOf(false) }
    LaunchedEffect(sessionId) { stored = try { src.sessions().firstOrNull { it.record.sessionId == sessionId } } catch (_: Throwable) { null }; loaded = true }

    Scaffold(modifier = modifier, topBar = {
        TopAppBar(title = { Text(stringResource(R.string.history_detail_title)) }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back)) } })
    }) { pad ->
        Column(Modifier.padding(pad).fillMaxSize().verticalScroll(rememberScrollState()).padding(Spacing.l), verticalArrangement = Arrangement.spacedBy(Spacing.m)) {
            val s = stored
            if (s == null) { Text(stringResource(if (loaded) R.string.history_not_found else R.string.history_loading)); return@Column }
            val o = overviewOf(s.plan, s.record, ZoneId.systemDefault())
            Text(dateFmt.format(o.day), style = MaterialTheme.typography.headlineMedium, modifier = Modifier.testTag("detail_title"))
            Text(stringResource(R.string.history_session_line, o.minutes, o.rounds, o.exercises), style = MaterialTheme.typography.titleMedium, modifier = Modifier.testTag("detail_summary"))
            if (s.plan == null) Text(stringResource(R.string.history_no_plan), style = MaterialTheme.typography.bodyMedium)
            val details = s.plan?.let { exerciseDetails(it, s.record.blocks) }.orEmpty()
            val achieved = s.record.blocks.filter { it.outcome == "MET" || it.outcome == "PARTIAL" }.associate { it.blockId to it.achievedValue }
            if (details.isNotEmpty()) Text(stringResource(R.string.history_correct), style = MaterialTheme.typography.labelLarge)
            details.forEach { d ->
                val name = catalog?.variation(d.variationId)?.name ?: d.variationId
                // V04b (R22): every round has its own number; a correction is saved as new revisions, the history stays append-only.
                val rounds = effectiveRounds(s.plan!!, d.variationId, achieved, s.rows)
                val tv = d.target?.value
                Card(Modifier.fillMaxWidth().testTag("detail_${d.variationId}"), shape = MaterialTheme.shapes.large) {
                    Column(Modifier.padding(Spacing.l), verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.m)) {
                            io.github.gonbei774.calisthenicsmemory.ui.components.ExerciseThumb(d.variationId, name, 48.dp)
                            Text(name, style = MaterialTheme.typography.titleMedium)
                        }
                        RoundEditor(
                            d.target, rounds.map { EditorRound(it.blockId, it.round, it.side, it.value, it.corrected) },
                            tooHard0 = rounds.any { it.rating == Rating.BELOW && !(it.value.let { v -> v != null && tv != null && v < tv }) },
                            tooEasy0 = rounds.any { it.rating == Rating.ABOVE }, pain0 = rounds.any { it.discomfort }, tag = "fix_${d.variationId}",
                        ) { list ->
                            corrected = true
                            scope.launch { src.reviseRounds(sessionId, d.variationId, list) }
                        }
                    }
                }
            }
            if (corrected) Text(stringResource(R.string.history_corrected), style = MaterialTheme.typography.bodyMedium, color = AppAccentTheme.colors.text, modifier = Modifier.testTag("corrected_note"))
        }
    }
}
