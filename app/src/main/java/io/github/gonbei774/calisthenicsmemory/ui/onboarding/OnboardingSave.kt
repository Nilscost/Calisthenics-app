// U08: what the questionnaire writes. Kept apart from the UI so it can be tested; it overwrites levels on a redo.
package io.github.gonbei774.calisthenicsmemory.ui.onboarding

import android.content.Context
import app.calisthenics.domain.intake.StartLevel
import app.calisthenics.domain.intake.routineFromAnswers
import app.calisthenics.domain.model.Catalog
import app.calisthenics.domain.model.EquipmentProfile
import io.github.gonbei774.calisthenicsmemory.ui.screens.*
import io.github.gonbei774.calisthenicsmemory.ui.train.TrainSettingsStore

data class OnboardingResult(
    val goalId: String,
    /** Replaces the profile with the same id (the first-run profile is "home"). */
    val profile: EquipmentProfile,
    val levels: List<StartLevel>,
    val pull: StartLevel?,
    val timed: Boolean,
    val rounds: Int,
    val stretchOn: Boolean,
    /** V22: a ready-made routine chosen as the objective (the questionnaire's levels still say where each exercise starts). */
    val routineId: String? = null,
)

object OnboardingSave {
    fun save(ctx: Context, catalog: Catalog, r: OnboardingResult) {
        LevelStore.save(ctx, r.levels.associate { it.variationId to it.tier }) // overwrites: a redo replaces the old levels
        ProfileStore.upsert(ctx, r.profile)
        ProfileStore.select(ctx, r.profile.id)
        RoutineStore.save(ctx, routineFromAnswers(catalog, RoutineStore.load(ctx), r.pull))
        val base = TrainSettingsStore.load(ctx).copy(goalId = r.goalId, profileId = r.profile.id, rounds = r.rounds, timed = r.timed, stretchOn = r.stretchOn, routineId = null)
        val preset = app.calisthenics.domain.routine.Presets.byId(r.routineId, catalog, r.profile)
        TrainSettingsStore.save(ctx, if (preset != null) app.calisthenics.domain.routine.settingsFor(preset, base) else base)
        OnboardingStore.setDone(ctx)
    }
}
