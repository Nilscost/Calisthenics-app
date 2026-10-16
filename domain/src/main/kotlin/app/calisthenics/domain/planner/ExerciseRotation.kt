package app.calisthenics.domain.planner

import app.calisthenics.domain.equipment.isAvailable
import app.calisthenics.domain.goals.Goals
import app.calisthenics.domain.model.*
import app.calisthenics.domain.model.Target
import app.calisthenics.domain.progression.ProgressSnapshot

/** Rotation is driven by completed history, so reopening the preview never changes the exercise. */
fun rotatedExercise(
    catalog: Catalog, main: ExerciseVariation, progress: ProgressSnapshot,
    profile: EquipmentProfile, excluded: Set<String> = emptySet(), progressionRouteId: String? = null,
): ExerciseVariation {
    val route = catalog.routeById(progressionRouteId)
        ?.takeIf { main.id in it.mainPath }
        ?: catalog.mainRouteFor(main.id)
    val mainIndex = route?.mainPath?.indexOf(main.id) ?: -1
    val eligibleAnchors = route?.mainPath?.take(if (mainIndex < 0) route.mainPath.size else mainIndex + 1)?.toSet().orEmpty()
    val ancestors = catalog.exerciseGroups.filter { group ->
        if (route != null) group.mainVariationId in route.mainPath.take(route.mainPath.indexOf(main.id).let { if (it < 0) route.mainPath.size else it + 1 })
        else {
            var id: String? = group.mainVariationId
            val seen = mutableSetOf<String>()
            var found = false
            while (id != null && seen.add(id)) {
                if (id == main.id) { found = true; break }
                id = catalog.policyForVariation(id)?.progressionSuccessors()?.firstOrNull()
            }
            found
        }
    }
    val alternatives = ancestors.flatMap { it.alternativeVariationIds }.distinct().mapNotNull(catalog::variation)
        .filter { v ->
            (if (route == null) catalog.progressionGroupOf(v.id) == catalog.progressionGroupOf(main.id)
             else route.members.any { it.variationId == v.id && it.role == ProgressionRole.ALTERNATE && it.atVariationId != null && it.atVariationId in eligibleAnchors }) &&
                v.id !in excluded && !v.routineOnly && !v.demonstrationHold &&
                catalog.policyFor(v) != null && isAvailable(v, profile) &&
                Goals.unmet(catalog, v.id, progress).isEmpty() &&
                progress.holdFor(v.familyId) == null
        }
    if (alternatives.isEmpty()) return main
    val completed = route?.let { progress.routeWorkoutCounts[it.id] } ?: progress.familyWorkoutCounts[main.familyId] ?: 0
    // The current main exercise keeps every other workout; alternatives rotate in the others.
    return if (completed % 2 == 0) main else alternatives[(completed / 2) % alternatives.size]
}

/** Limits prescriptions, not recorded actual performance: history must remain truthful. */
fun cappedTarget(target: Target, policy: ProgressionPolicy): Target {
    val cap = policy.maxRepsPerSet ?: return target
    if (target.type != TargetType.REPS) return target
    return target.copy(value = minOf(target.value, cap), max = target.max?.let { minOf(it, cap) })
}
