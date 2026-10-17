// What the Train and Preview screens both need: the bundled catalog, the usual plan and the progress snapshot.
package io.github.gonbei774.calisthenicsmemory.ui.train

import android.content.Context
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.JsonPrimitive
import app.calisthenics.domain.content.parseCatalog
import app.calisthenics.domain.model.Catalog
import app.calisthenics.domain.model.Routine
import app.calisthenics.domain.planner.TrainSettings
import app.calisthenics.domain.progression.ProgressSnapshot
import io.github.gonbei774.calisthenicsmemory.ui.screens.*

data class TrainData(val catalog: Catalog, val routine: Routine, val progress: ProgressSnapshot)

fun loadCatalog(ctx: Context): Catalog = ctx.assets.open("catalog.json").bufferedReader().use { parseCatalog(it.readText()) }

/** Starts with the self-assessed levels (instant) and swaps in the history-based progress as soon as Room answers. */
@Composable
fun rememberTrainData(refresh: Int = 0): State<TrainData> {
    val ctx = LocalContext.current
    val catalog = remember { loadCatalog(ctx) }
    val routine = remember { RoutineStore.load(ctx) }
    val data = remember { mutableStateOf(TrainData(catalog, routine, LevelStore.snapshot(catalog, LevelStore.load(ctx)))) }
    LaunchedEffect(refresh) {
        // History unreadable (or no SQLite, as in JVM UI tests) -> keep the self-assessed levels.
        val p = try { ProgressLoader.load(ctx, catalog) } catch (_: Throwable) { null }
        // no history database: the self-assessed levels are the progress, re-read so a level edited by hand shows at once (R7-T11)
        data.value = data.value.copy(progress = p ?: LevelStore.snapshot(catalog, LevelStore.load(ctx)))
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
        return TrainSettings(
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
            fixedProgramFlowOverrides = runCatching {
                kotlinx.serialization.json.Json.parseToJsonElement(p(ctx).getString("fixed_program_flow", "{}")!!).jsonObject.mapNotNull { (k, v) -> (v as? JsonPrimitive)?.booleanOrNull?.let { k to it } }.toMap()
            }.getOrDefault(emptyMap()),
        )
    }

    fun save(ctx: Context, s: TrainSettings) {
        GoalStore.save(ctx, s.goalId)
        ProfileStore.select(ctx, s.profileId)
        ModeStore.setTimed(ctx, s.timed)
        PrefsStore.save(ctx, PrefsStore.load(ctx).copy(stretchOn = s.stretchOn))
        val flowJson = kotlinx.serialization.json.buildJsonObject { s.fixedProgramFlowOverrides.forEach { (k, v) -> put(k, kotlinx.serialization.json.JsonPrimitive(v)) } }
        p(ctx).edit().putInt("rounds", s.rounds).putString("format", s.format.name).putString("routine", s.routineId).putBoolean("warmup", s.warmupOn).putInt("exercises", s.exercisesPerRound ?: -1).putBoolean("cooldown", s.cooldownOn).putString("cooldown_mode", s.cooldownMode.name).putInt("cooldown_minutes", s.cooldownMinutes).putString("fixed_program", s.fixedProgramId).putString("fixed_program_flow", flowJson.toString()).putStringSet("additional_pools", s.additionalPools).putBoolean("flow", s.flow).apply()
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
    val equipment = program.items.mapNotNull { catalog.variation(it.exerciseId) }.flatMap { v -> v.equipmentAlternatives.firstOrNull()?.needs.orEmpty().map { it.equipmentId } }.distinct().sorted()
    val value = if (equipment.isEmpty()) ctx.getString(io.github.gonbei774.calisthenicsmemory.R.string.programs_no_equipment) else equipment.joinToString(" + ") { ctx.getString(io.github.gonbei774.calisthenicsmemory.ui.screens.equipmentLabelShortPublic(it)) }
    return ctx.getString(io.github.gonbei774.calisthenicsmemory.R.string.programs_summary, minutes, ctx.getString(io.github.gonbei774.calisthenicsmemory.R.string.programs_equipment_label, value))
}

internal fun fixedProgramDurationSeconds(catalog: Catalog, program: app.calisthenics.domain.model.FixedProgram, result: app.calisthenics.domain.planner.FixedProgramPlanResult): Int =
    (result as? app.calisthenics.domain.planner.FixedProgramPlanResult.Ready)?.plan?.plannedDurationSeconds ?: run {
        var total = 0
        program.items.forEach { item -> total += app.calisthenics.domain.planner.itemSeconds(item) * (if (item.perSide) 2 else 1) }
        total
    }
