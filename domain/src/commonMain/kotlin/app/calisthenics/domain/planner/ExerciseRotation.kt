package app.calisthenics.domain.planner

import app.calisthenics.domain.equipment.isAvailable
import app.calisthenics.domain.goals.Goals
import app.calisthenics.domain.load.isLoaded
import app.calisthenics.domain.model.*
import app.calisthenics.domain.model.Target
import app.calisthenics.domain.progression.ProgressSnapshot
import app.calisthenics.domain.progression.specs

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

/**
 * Conservative N-1 starting estimate for an untried choice at the same route rung, but only after another choice
 * at that rung has been demonstrated with complete actual results. This remains an estimate: it does not transfer
 * baseline stars, achievements, or progression state to [candidate].
 */
fun demonstratedRungStartingTier(catalog: Catalog, candidate: ExerciseVariation, progress: ProgressSnapshot, routeId: String?): Int? {
    if (candidate.id in progress.variations || candidate.kind !in setOf(Kind.REPS, Kind.HOLD) || candidate.isLoaded() ||
        progress.holdFor(candidate.familyId) != null) return null
    val route = catalog.routeById(routeId) ?: return null
    val anchorId = catalog.routeAnchorFor(candidate.id, route) ?: return null
    if (candidate.id != anchorId && route.members.none {
            it.variationId == candidate.id && it.role == ProgressionRole.ALTERNATE && it.atVariationId == anchorId
        }) return null
    val sameRung = (listOf(anchorId) + route.members.filter {
        it.role == ProgressionRole.ALTERNATE && it.atVariationId == anchorId
    }.map { it.variationId }).distinct()
    val candidatePolicy = catalog.policyFor(candidate) ?: return null
    var best: Int? = null

    for (sourceId in sameRung.filter { it != candidate.id }) {
        val source = catalog.variation(sourceId) ?: continue
        if (source.kind != candidate.kind || source.isLoaded() || source.unilateral != candidate.unilateral ||
            source.patterns.toSet() != candidate.patterns.toSet() ||
            (source.progressionGroup ?: source.familyId) != (candidate.progressionGroup ?: candidate.familyId) ||
            progress.holdFor(source.familyId) != null) continue
        val state = progress.variations[sourceId] ?: continue
        if (state.autoPaused) continue
        val demonstrated = state.demonstratedTier ?: continue
        if (demonstrated !in 2..5 || state.demonstratedTargetRevision != state.targetRevision ||
            state.targetRevision != (catalog.personalTargets[sourceId]?.revision ?: 0)) continue
        val sourcePolicy = state.effectivePolicySnapshot ?: catalog.policyFor(source) ?: continue
        if (sourcePolicy != catalog.policyFor(source)) continue
        val sourceTier = sourcePolicy.tiers.firstOrNull { it.index == demonstrated } ?: continue
        val candidateTier = candidatePolicy.tiers.firstOrNull { it.index == demonstrated - 1 } ?: continue
        if (candidateTier.target.type != sourceTier.target.type || candidateTier.target.shownValue() > sourceTier.target.shownValue()) continue
        if (candidateTier.minQualifyingBlocks > sourceTier.minQualifyingBlocks) continue

        val sourceSpecs = sourcePolicy.tierProfile?.specs()
        val candidateSpecs = candidatePolicy.tierProfile?.specs()
        if ((sourceSpecs == null) != (candidateSpecs == null)) continue
        if (sourceSpecs != null && candidateSpecs != null) {
            val sourceSpec = sourceSpecs.getOrNull(demonstrated - 1) ?: continue
            val candidateSpec = candidateSpecs.getOrNull(demonstrated - 2) ?: continue
            if (candidateSpec.sets > sourceSpec.sets ||
                (candidateSpec.seconds != null && (sourceSpec.seconds == null || candidateSpec.seconds > sourceSpec.seconds))) continue
        }
        best = maxOf(best ?: 0, demonstrated - 1)
    }
    return best
}
