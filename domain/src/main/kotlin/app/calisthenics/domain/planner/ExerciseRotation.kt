package app.calisthenics.domain.planner

import app.calisthenics.domain.equipment.isAvailable
import app.calisthenics.domain.goals.Goals
import app.calisthenics.domain.load.isLoaded
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
    // R8.1-T01 (F1, owner 2026-10-17): only alternates of the main step's own rung rotate in. An alternate anchored at a lower rung
    // (knee push-up next to incline, when the person works at feet-elevated) is below what they have shown and is never prescribed.
    val eligibleAnchors = route?.let { r -> catalog.routeAnchorFor(main.id, r)?.let(::setOf) }.orEmpty()
    val legacyAncestors = if (route == null) catalog.exerciseGroups.filter { group ->
            var id: String? = group.mainVariationId
            val seen = mutableSetOf<String>()
            var found = false
            while (id != null && seen.add(id)) {
                if (id == main.id) { found = true; break }
                id = catalog.policyForVariation(id)?.progressionSuccessors()?.firstOrNull()
            }
            found
        } else emptyList()
    val alternativeIds = if (route != null) {
        route.members.filter { it.role == ProgressionRole.ALTERNATE && it.atVariationId != null && it.atVariationId in eligibleAnchors }
            .map { it.variationId }
    } else legacyAncestors.flatMap { it.alternativeVariationIds }
    val alternatives = alternativeIds.distinct().mapNotNull(catalog::variation)
        .filter { v ->
            (route != null || catalog.progressionGroupOf(v.id) == catalog.progressionGroupOf(main.id)) &&
                v.id !in excluded && !v.routineOnly && !v.demonstrationHold &&
                catalog.policyFor(v) != null && isAvailable(v, profile) &&
                Goals.unmet(catalog, v.id, progress).isEmpty() &&
                progress.holdFor(main.familyId) == null && progress.holdFor(v.familyId) == null
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

/** Product starting estimate, not an achievement or a claim of physiological equivalence. */
fun inferredAlternativeTier(catalog: Catalog, main: ExerciseVariation, alternative: ExerciseVariation,
    progress: ProgressSnapshot, routeId: String?): Int? {
    if (alternative.id in progress.variations ||
        (main.progressionGroup ?: main.familyId) != (alternative.progressionGroup ?: alternative.familyId) ||
        main.kind !in setOf(Kind.REPS, Kind.HOLD) || main.kind != alternative.kind || main.isLoaded() || alternative.isLoaded() ||
        main.unilateral != alternative.unilateral || main.patterns.toSet() != alternative.patterns.toSet() ||
        progress.holdFor(main.familyId) != null || progress.holdFor(alternative.familyId) != null) return null
    val route = catalog.routeById(routeId) ?: return null
    if (main.id !in route.mainPath || route.members.none { it.variationId == alternative.id && it.role == ProgressionRole.ALTERNATE && it.atVariationId == main.id }) return null
    val source = catalog.policyFor(main)?.tiers?.firstOrNull { it.index == progress.tierFor(main.id) }?.target ?: return null
    if (source.type !in setOf(TargetType.REPS, TargetType.HOLD_SECONDS)) return null
    val tiers = catalog.policyFor(alternative)?.tiers.orEmpty().filter { it.target.type == source.type }
    val top = tiers.maxOfOrNull { it.index } ?: return null
    return tiers.filter { it.index < top && it.target.value <= source.value * 3 / 4 }
        .maxByOrNull { it.target.value }?.index?.takeIf { it > 1 }
}
