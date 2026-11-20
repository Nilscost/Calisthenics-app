package app.calisthenics.data.settings

import app.calisthenics.domain.planner.TrainSettings
import app.calisthenics.domain.planner.draftKey
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.JsonPrimitive

object TrainSettingsStore {
    private fun p(ctx: SettingsProvider) = ctx.legacy("train")

    fun load(ctx: SettingsProvider, todayEpochDay: Long): TrainSettings {
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
        migrateOrdinaryRuleDefault(ctx, settings, todayEpochDay)
        return settings
    }

    private fun migrateOrdinaryRuleDefault(ctx: SettingsProvider, settings: TrainSettings, todayEpochDay: Long) {
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
            val draft = PreviewDraftStore.get(ctx, key, todayEpochDay)
            if (draft != null) {
                val rule = draft.edits.rule
                val edits = if (draft.ruleDefaultVersion == 0 && rule != null && rule.kind != app.calisthenics.domain.model.RuleKind.APP_LEVELS)
                    draft.edits.copy(rule = app.calisthenics.domain.model.ProgressionRule()) else draft.edits
                PreviewDraftStore.put(ctx, draft.copy(edits = edits, ruleDefaultVersion = 1), todayEpochDay)
            }
            store.edit().putInt("ordinary_draft_rule_default_version", 1).apply()
        }
    }

    fun save(ctx: SettingsProvider, s: TrainSettings) {
        GoalStore.save(ctx, s.goalId)
        ProfileStore.select(ctx, s.profileId)
        ModeStore.setTimed(ctx, s.timed)
        val mode = s.effectiveBetweenSetMode()
        PrefsStore.save(ctx, PrefsStore.load(ctx).copy(stretchOn = mode == app.calisthenics.domain.model.BetweenSetMode.STRETCH))
        val flowJson = kotlinx.serialization.json.buildJsonObject { s.fixedProgramFlowOverrides.forEach { (k, v) -> put(k, kotlinx.serialization.json.JsonPrimitive(v)) } }
        p(ctx).edit().putInt("rounds", s.rounds).putString("format", s.format.name).putString("routine", s.routineId).putBoolean("warmup", s.warmupOn).putInt("exercises", s.exercisesPerRound ?: -1).putBoolean("cooldown", s.cooldownOn).putString("cooldown_mode", s.cooldownMode.name).putInt("cooldown_minutes", s.cooldownMinutes).putString("fixed_program", s.fixedProgramId).putString("fixed_program_flow", flowJson.toString()).putStringSet("additional_pools", s.additionalPools).putBoolean("flow", mode == app.calisthenics.domain.model.BetweenSetMode.FLOW).putBoolean("conditioning", s.conditioningOn).putInt("conditioning_minutes", s.conditioningMinutes).putString("between_set_mode", mode.name).putInt("preparation_seconds", s.preparationSeconds.coerceIn(1, 30)).apply()
    }

    fun modeRevision(ctx: SettingsProvider, key: String): Long = p(ctx).getLong("between_set_revision:$key", 0L)

    fun setModeRevision(ctx: SettingsProvider, key: String, revision: Long) {
        p(ctx).edit().putLong("between_set_revision:$key", revision).apply()
    }
}
