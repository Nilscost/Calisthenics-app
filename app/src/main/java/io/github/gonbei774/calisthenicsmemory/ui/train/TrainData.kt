// What the Train and Preview screens both need: the bundled catalog, the usual plan and the progress snapshot.
package io.github.gonbei774.calisthenicsmemory.ui.train

import android.content.Context
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.ui.text.style.TextOverflow
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.JsonPrimitive
import app.calisthenics.domain.content.parseCatalog
import app.calisthenics.domain.model.Catalog
import app.calisthenics.domain.model.Routine
import app.calisthenics.domain.model.BetweenSetMode
import app.calisthenics.domain.planner.TrainSettings
import app.calisthenics.domain.progression.ProgressSnapshot
import io.github.gonbei774.calisthenicsmemory.ui.screens.*
import io.github.gonbei774.calisthenicsmemory.ui.components.appSegmentedColors

data class TrainData(val catalog: Catalog, val routine: Routine, val progress: ProgressSnapshot)

fun loadCatalog(ctx: Context): Catalog = PersonalTargetStore.effective(ctx, ctx.assets.open("catalog.json").bufferedReader().use { parseCatalog(it.readText()) })

/** Starts with the self-assessed levels (instant) and swaps in the history-based progress as soon as Room answers. */
@Composable
fun rememberTrainData(refresh: Int = 0): State<TrainData> {
    val ctx = LocalContext.current
    val catalog = remember(refresh) { loadCatalog(ctx) }
    val routine = remember { RoutineStore.load(ctx) }
    val data = remember { mutableStateOf(TrainData(catalog, routine, LevelStore.snapshot(catalog, LevelStore.load(ctx)))) }
    LaunchedEffect(refresh) {
        // History unreadable (or no SQLite, as in JVM UI tests) -> keep the self-assessed levels.
        val p = try { ProgressLoader.load(ctx, catalog) } catch (_: Throwable) { null }
        // no history database: the self-assessed levels are the progress, re-read so a level edited by hand shows at once (R7-T11)
        data.value = TrainData(catalog, routine, p ?: LevelStore.snapshot(catalog, LevelStore.load(ctx)))
    }
    return data
}

/** One place that reads and writes today's choices; the older stores stay the source of truth for what they already held. */
object TrainSettingsStore {
    private fun p(ctx: Context) = ctx.getSharedPreferences("train", Context.MODE_PRIVATE)

    fun load(ctx: Context): TrainSettings {
        val prefs = PrefsStore.load(ctx)
        // Specialist pools are now added manually from preview. Clear legacy selections so they
        // cannot silently widen future workouts while keeping the serialized field readable.
        val trainPrefs = p(ctx)
        if (!trainPrefs.getStringSet("additional_pools", emptySet()).isNullOrEmpty()) {
            trainPrefs.edit().putStringSet("additional_pools", emptySet()).apply()
        }
        val settings = TrainSettings(
            goalId = GoalStore.load(ctx),
            profileId = ProfileStore.selected(ctx).id,
            rounds = p(ctx).getInt("rounds", app.calisthenics.domain.planner.DEFAULT_TRAIN_ROUNDS),
            routineId = p(ctx).getString("routine", null),
            format = runCatching { app.calisthenics.domain.model.WorkoutFormat.valueOf(p(ctx).getString("format", "CIRCUIT")!!) }.getOrDefault(app.calisthenics.domain.model.WorkoutFormat.CIRCUIT),
            timed = ModeStore.timed(ctx),
            stretchOn = prefs.stretchOn,
            warmupOn = p(ctx).getBoolean("warmup", false),
            exercisesPerRound = p(ctx).getInt("exercises", -1).takeIf { it > 0 },
            cooldownOn = p(ctx).getBoolean("cooldown", false),
            cooldownMode = runCatching { app.calisthenics.domain.model.CooldownMode.valueOf(p(ctx).getString("cooldown_mode", "STRETCHING")!!) }.getOrDefault(app.calisthenics.domain.model.CooldownMode.STRETCHING),
            cooldownMinutes = p(ctx).getInt("cooldown_minutes", 5),
            fixedProgramId = p(ctx).getString("fixed_program", null),
            additionalPools = emptySet(),
            flow = p(ctx).getBoolean("flow", false),
            conditioningOn = p(ctx).getBoolean("conditioning", false),
            conditioningMinutes = p(ctx).getInt("conditioning_minutes", app.calisthenics.domain.planner.DEFAULT_CONDITIONING_MINUTES),
            betweenSetMode = p(ctx).getString("between_set_mode", null)?.let { runCatching { app.calisthenics.domain.model.BetweenSetMode.valueOf(it) }.getOrNull() },
            preparationSeconds = p(ctx).getInt("preparation_seconds", 5).coerceIn(1, 30),
            fixedProgramFlowOverrides = runCatching {
                kotlinx.serialization.json.Json.parseToJsonElement(p(ctx).getString("fixed_program_flow", "{}")!!).jsonObject.mapNotNull { (k, v) -> (v as? JsonPrimitive)?.booleanOrNull?.let { k to it } }.toMap()
            }.getOrDefault(emptyMap()),
        )
        migrateOrdinaryRuleDefault(ctx, settings)
        return settings
    }

    private fun migrateOrdinaryRuleDefault(ctx: Context, settings: TrainSettings) {
        val store = p(ctx)
        if (settings.routineId != null || settings.fixedProgramId != null) return
        val ordinary = RoutineStore.load(ctx)
        val legacyOrdinaryRule = ordinary.rule.kind != app.calisthenics.domain.model.RuleKind.APP_LEVELS
        if (store.getInt("ordinary_rule_default_version", 0) < 1) {
            if (legacyOrdinaryRule) {
                // Older ordinary plans inherited Rep range. Preserve every plan edit except that
                // inherited default; saved routines and authored presets are stored elsewhere.
                RoutineStore.save(ctx, ordinary.copy(rule = app.calisthenics.domain.model.ProgressionRule()))
            }
            store.edit().putInt("ordinary_rule_default_version", 1).apply()
        }
        if (store.getInt("ordinary_draft_rule_default_version", 0) < 1) {
            // Draft-rule provenance was not recorded in older installs. Reset only a matching
            // ordinary version-0 draft rule; current explicit rules carry version 1 and survive restore.
            val key = draftKey(settings)
            val draft = PreviewDraftStore.get(ctx, key)
            if (draft != null) {
                val rule = draft.edits.rule
                val edits = if (draft.ruleDefaultVersion == 0 && rule != null && rule.kind != app.calisthenics.domain.model.RuleKind.APP_LEVELS)
                    draft.edits.copy(rule = app.calisthenics.domain.model.ProgressionRule()) else draft.edits
                PreviewDraftStore.put(ctx, draft.copy(edits = edits, ruleDefaultVersion = 1))
            }
            store.edit().putInt("ordinary_draft_rule_default_version", 1).apply()
        }
    }

    fun save(ctx: Context, s: TrainSettings) {
        GoalStore.save(ctx, s.goalId)
        ProfileStore.select(ctx, s.profileId)
        ModeStore.setTimed(ctx, s.timed)
        val mode = s.effectiveBetweenSetMode()
        PrefsStore.save(ctx, PrefsStore.load(ctx).copy(stretchOn = mode == app.calisthenics.domain.model.BetweenSetMode.STRETCH))
        val flowJson = kotlinx.serialization.json.buildJsonObject { s.fixedProgramFlowOverrides.forEach { (k, v) -> put(k, kotlinx.serialization.json.JsonPrimitive(v)) } }
        p(ctx).edit().putInt("rounds", s.rounds).putString("format", s.format.name).putString("routine", s.routineId).putBoolean("warmup", s.warmupOn).putInt("exercises", s.exercisesPerRound ?: -1).putBoolean("cooldown", s.cooldownOn).putString("cooldown_mode", s.cooldownMode.name).putInt("cooldown_minutes", s.cooldownMinutes).putString("fixed_program", s.fixedProgramId).putString("fixed_program_flow", flowJson.toString()).putStringSet("additional_pools", s.additionalPools).putBoolean("flow", mode == app.calisthenics.domain.model.BetweenSetMode.FLOW).putBoolean("conditioning", s.conditioningOn).putInt("conditioning_minutes", s.conditioningMinutes).putString("between_set_mode", mode.name).putInt("preparation_seconds", s.preparationSeconds.coerceIn(1, 30)).apply()
    }

    internal fun modeRevision(ctx: Context, key: String): Long = p(ctx).getLong("between_set_revision:$key", 0L)

    internal fun setModeRevision(ctx: Context, key: String, revision: Long) {
        p(ctx).edit().putLong("between_set_revision:$key", revision).apply()
    }
}

fun effectiveTrainSettings(stored: TrainSettings, draft: PreviewDraft?, storedModeRevision: Long = 0L): TrainSettings {
    val mode = draft?.takeIf { it.modeRevision > storedModeRevision }?.betweenSetMode ?: stored.effectiveBetweenSetMode()
    return stored.copy(
        flow = mode == BetweenSetMode.FLOW,
        stretchOn = mode == BetweenSetMode.STRETCH,
        betweenSetMode = mode,
        preparationSeconds = stored.preparationSeconds.coerceIn(1, 30),
    )
}

/** Old per-break stretch additions lose to a newer global mode choice; a later explicit add is revision-stamped and remains active. */
fun effectivePlanEdits(edits: app.calisthenics.domain.planner.PlanEdits, currentModeRevision: Long): app.calisthenics.domain.planner.PlanEdits =
    if (edits.extraStretchModeRevision < currentModeRevision) edits.copy(extraStretches = emptyMap()) else edits

fun updateBetweenSetMode(ctx: Context, settings: TrainSettings, draft: PreviewDraft?, mode: BetweenSetMode): Pair<TrainSettings, PreviewDraft> {
    val key = draft?.key ?: draftKey(settings)
    val revision = maxOf(
        TrainSettingsStore.modeRevision(ctx, key), draft?.modeRevision ?: 0L,
        draft?.edits?.extraStretchModeRevision ?: 0L,
    ) + 1L
    val updatedSettings = settings.copy(flow = mode == BetweenSetMode.FLOW, stretchOn = mode == BetweenSetMode.STRETCH, betweenSetMode = mode)
    TrainSettingsStore.save(ctx, updatedSettings)
    TrainSettingsStore.setModeRevision(ctx, key, revision)
    val current = PreviewDraftStore.get(ctx, key) ?: draft ?: PreviewDraft(key)
    val updatedDraft = current.copy(betweenSetMode = mode, modeRevision = revision)
    PreviewDraftStore.put(ctx, updatedDraft)
    return updatedSettings to updatedDraft
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BetweenSetSelector(mode: BetweenSetMode, onModeChange: (BetweenSetMode) -> Unit) {
    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().testTag("between_set_selector")) {
        BetweenSetMode.entries.forEachIndexed { i, option ->
            SegmentedButton(
                selected = mode == option,
                onClick = { onModeChange(option) },
                shape = SegmentedButtonDefaults.itemShape(i, BetweenSetMode.entries.size),
                icon = {},
                colors = appSegmentedColors(),
                modifier = Modifier.weight(1f).testTag("between_${option.name.lowercase()}"),
            ) {
                Text(
                    stringResource(when (option) {
                        BetweenSetMode.FLOW -> io.github.gonbei774.calisthenicsmemory.R.string.feedback_between_flow
                        BetweenSetMode.REST -> io.github.gonbei774.calisthenicsmemory.R.string.between_rest
                        BetweenSetMode.STRETCH -> io.github.gonbei774.calisthenicsmemory.R.string.between_stretch
                    }), maxLines = 1, overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }
    }
}

/** Starts [plan] as a workout (one history snapshot per workout): used by the Train tab's START and by the Preview. */
fun startPlan(ctx: Context, plan: app.calisthenics.domain.model.WorkoutPlan) {
    val p = plan.copy(id = java.util.UUID.randomUUID().toString())
    val json = kotlinx.serialization.json.Json { encodeDefaults = true }.encodeToString(app.calisthenics.domain.model.WorkoutPlan.serializer(), p)
    io.github.gonbei774.calisthenicsmemory.ui.session.startWorkout(ctx, json, java.util.UUID.randomUUID().toString(), PrefsStore.load(ctx).audioEnabled)
}

/** V22: the ready-made routines (built for the selected equipment profile) followed by the person's saved ones. */
fun routinesFor(ctx: Context, catalog: Catalog): List<app.calisthenics.domain.routine.SavedRoutine> =
    app.calisthenics.domain.routine.Presets.all(catalog, ProfileStore.selected(ctx)) + SavedRoutineStore.load(ctx)

/** V18: the routine, settings and edits the plan is built from: the usual plan, or the saved routine chosen as the objective. */
fun resolveTrain(ctx: Context, base: Routine, s: TrainSettings, catalog: Catalog): app.calisthenics.domain.routine.ResolvedRoutine {
    val saved = routinesFor(ctx, catalog).firstOrNull { it.id == s.routineId } ?: return app.calisthenics.domain.routine.ResolvedRoutine(base, s.copy(routineId = null), app.calisthenics.domain.planner.PlanEdits())
    return app.calisthenics.domain.routine.resolveSaved(saved, s)
}

/** Apply the Preview's rest choices to every set and keep the displayed and started duration in sync. */
fun withRestDurations(plan: app.calisthenics.domain.model.WorkoutPlan, durations: Map<String, Int>): app.calisthenics.domain.model.WorkoutPlan {
    if (durations.isEmpty()) return plan
    val blocks = plan.blocks.map { block ->
        if (block.type == app.calisthenics.domain.model.BlockType.PASSIVE_RECOVERY && block.slotId in durations) block.copy(durationSeconds = durations.getValue(block.slotId!!)) else block
    }
    return plan.copy(blocks = blocks, plannedDurationSeconds = blocks.sumOf { it.durationSeconds })
}

/**
 * R8.1-T08 (F9): what Train and the Preview work on when the objective is a program: a catalog program picked as the starting point,
 * or a routine saved from one. [content] is the working copy (the Preview draft when there is one); [base] is the catalog program it started from.
 */
data class ProgramMode(val content: app.calisthenics.domain.planner.ProgramContent, val saved: app.calisthenics.domain.routine.SavedRoutine?, val base: app.calisthenics.domain.model.FixedProgram?)

fun resolveProgramMode(ctx: Context, catalog: Catalog, s: TrainSettings, draft: PreviewDraft?): ProgramMode? {
    val saved = routinesFor(ctx, catalog).firstOrNull { it.id == s.routineId }?.takeIf { it.program != null }
    if (saved != null) return ProgramMode(draft?.program ?: saved.program!!, saved, catalog.fixedPrograms.firstOrNull { it.id == saved.program!!.programId })
    val base = catalog.fixedPrograms.firstOrNull { it.id == s.fixedProgramId } ?: return null
    return ProgramMode(draft?.program ?: app.calisthenics.domain.planner.programContentOf(base), null, base)
}

/** The compact description of a catalog program in the dropdown: about how long it takes, and the equipment it needs. */
fun programSummaryText(ctx: Context, catalog: Catalog, program: app.calisthenics.domain.model.FixedProgram, profile: app.calisthenics.domain.model.EquipmentProfile): String {
    val seconds = fixedProgramDurationSeconds(catalog, program, app.calisthenics.domain.planner.buildFixedProgramPlan(catalog, program, profile))
    val minutes = ((seconds + 59) / 60).coerceAtLeast(1)
    val equipment = program.items.mapNotNull { catalog.variation(it.exerciseId) }.flatMap { v -> v.equipmentAlternatives.firstOrNull()?.needs.orEmpty().map { it.equipmentId } }.distinct().sortedBy { app.calisthenics.domain.equipment.EquipmentOptions.orderOf(it) }
    val value = if (equipment.isEmpty()) ctx.getString(io.github.gonbei774.calisthenicsmemory.R.string.programs_no_equipment) else equipment.joinToString(" + ") { ctx.getString(io.github.gonbei774.calisthenicsmemory.ui.screens.equipmentLabelShortPublic(it)) }
    return ctx.getString(io.github.gonbei774.calisthenicsmemory.R.string.programs_summary, minutes, ctx.getString(io.github.gonbei774.calisthenicsmemory.R.string.programs_equipment_label, value))
}

internal fun fixedProgramDurationSeconds(catalog: Catalog, program: app.calisthenics.domain.model.FixedProgram, result: app.calisthenics.domain.planner.FixedProgramPlanResult): Int =
    (result as? app.calisthenics.domain.planner.FixedProgramPlanResult.Ready)?.plan?.plannedDurationSeconds ?: run {
        var total = 0
        program.items.forEach { item -> total += app.calisthenics.domain.planner.itemSeconds(item) * (if (item.perSide) 2 else 1) }
        total
    }

/** Save exactly the effective configuration used by the preview/start paths. */
internal fun savedCurrentWorkout(
    id: String, name: String, revision: Int, base: Routine,
    edits: app.calisthenics.domain.planner.PlanEdits, settings: TrainSettings,
    modeRevision: Long, rest: Map<String, Int>, now: Long,
): app.calisthenics.domain.routine.SavedRoutine {
    val effective = effectivePlanEdits(edits, modeRevision)
    return app.calisthenics.domain.routine.savedFrom(id, name, revision, base, effective, settings,
        effective.rule ?: base.rule, now).copy(restDurations = rest)
}
