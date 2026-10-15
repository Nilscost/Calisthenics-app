package app.calisthenics.domain.planner

import app.calisthenics.domain.equipment.isAvailable
import app.calisthenics.domain.model.*

data class ProgramItemUnavailable(val exerciseId: String, val missing: String)

sealed interface FixedProgramPlanResult {
    data class Ready(val plan: WorkoutPlan) : FixedProgramPlanResult
    data class Unavailable(val programId: String, val items: List<ProgramItemUnavailable>) : FixedProgramPlanResult
}

/** Builds exactly the catalog item order and prescriptions. It never substitutes or drops an item. */
fun buildFixedProgramPlan(
    catalog: Catalog,
    program: FixedProgram,
    profile: EquipmentProfile,
    flow: Boolean = program.flow,
    nowEpochMs: Long = 0L,
): FixedProgramPlanResult {
    val unavailable = program.items.mapNotNull { item ->
        val exercise = catalog.variation(item.exerciseId)
            ?: return@mapNotNull ProgramItemUnavailable(item.exerciseId, "missing from catalog")
        if (isAvailable(exercise, profile)) null
        else ProgramItemUnavailable(item.exerciseId, app.calisthenics.domain.equipment.missingFor(exercise, profile))
    }
    if (unavailable.isNotEmpty()) return FixedProgramPlanResult.Unavailable(program.id, unavailable)

    val blocks = withSideChangePreparation(program.items.flatMapIndexed { index, item ->
        val exercise = catalog.variation(item.exerciseId)!!
        val target = when {
            item.reps != null -> Target(TargetType.REPS, item.reps)
            item.seconds != null -> Target(TargetType.HOLD_SECONDS, item.seconds)
            else -> error("${program.id}/${item.exerciseId} has no prescription")
        }
        val duration = item.estSeconds ?: item.seconds ?: ((item.reps ?: 1) * 3)
        val sides = if (item.perSide) listOf(Side.LEFT, Side.RIGHT) else listOf(Side.NONE)
        sides.mapIndexed { sideIndex, side ->
            TimelineBlock(
                id = "fixed-${program.id}-$index-$sideIndex-${item.exerciseId}",
                type = if (exercise.kind == Kind.STRETCH || exercise.kind == Kind.MOBILITY) BlockType.STRETCH else BlockType.WORK,
                // The source estimate is per side when prescribed per side, so each side gets a full timed block.
                durationSeconds = duration.coerceAtLeast(1),
                slotId = "fixed-${program.id}-$index",
                variationId = item.exerciseId,
                side = side,
                target = target,
                // Fixed programs follow their written volume, never the app-level progression ladder.
                prescriptionTier = null,
                mediaId = exercise.mediaId,
                freeTarget = flow,
                note = item.notes.takeIf { it.isNotBlank() },
            )
        }
    })
    val seconds = blocks.sumOf { it.durationSeconds }
    return FixedProgramPlanResult.Ready(WorkoutPlan(
        id = "fixed-${program.id}",
        routineId = "fixed-program:${program.id}",
        routineRevision = 1,
        catalogVersion = catalog.catalogVersion,
        createdAtEpochMs = nowEpochMs,
        profileId = profile.id,
        requestedDurationSeconds = seconds,
        plannedDurationSeconds = seconds,
        focus = emptySet(),
        stretchOn = false,
        rounds = 1,
        changesExplained = emptyList(),
        warnings = emptyList(),
        needsAcceptance = false,
        usesDraftContent = blocks.any { catalog.variation(it.variationId ?: "")?.reviewState == ReviewState.DRAFT },
        blocks = blocks,
        fixedProgramId = program.id,
        flow = flow,
    ))
}
