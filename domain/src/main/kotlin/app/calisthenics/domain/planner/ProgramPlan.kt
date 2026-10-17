// R8.1-T08 (F9): a fixed program is a starting point. It opens in the Preview as a list of items the person can change (target, swap, add, remove),
// run in any set format with sets, Flow and rest like every other workout, and saved as a new "My routine" without touching the program.
package app.calisthenics.domain.planner

import app.calisthenics.domain.equipment.isAvailable
import app.calisthenics.domain.equipment.missingFor
import app.calisthenics.domain.model.*
import kotlinx.serialization.Serializable

/** Rest between sets (and between exercises when Flow is off) of a program workout. */
const val DEFAULT_PROGRAM_REST_SECONDS = 20

/** The working copy of a program: its items as edited, and the rest. [programId] is the catalog program it started from. */
@Serializable
data class ProgramContent(
    val programId: String,
    val name: String,
    val items: List<FixedProgramItem>,
    val restSeconds: Int = DEFAULT_PROGRAM_REST_SECONDS,
)

fun programContentOf(p: FixedProgram) = ProgramContent(p.id, p.name, p.items)

/** Seconds one block of [item] lasts: the source estimate, else the held seconds, else about 3 s per rep. */
fun itemSeconds(item: FixedProgramItem): Int = (item.estSeconds ?: item.seconds ?: ((item.reps ?: 1) * 3)).coerceAtLeast(1)

/** A new item for an exercise added in the Preview: a middle level of its policy (strength), or its own stretch time. */
fun defaultProgramItem(catalog: Catalog, v: ExerciseVariation): FixedProgramItem {
    val policy = catalog.policyFor(v)
    val tier = policy?.tiers?.firstOrNull { it.index == 2 } ?: policy?.tiers?.firstOrNull()
    val unilateral = v.unilateral
    return when {
        v.kind == Kind.STRETCH || v.kind == Kind.MOBILITY -> FixedProgramItem(v.id, seconds = v.defaultSeconds ?: 30, perSide = unilateral, estSeconds = v.defaultSeconds ?: 30)
        tier != null && tier.target.type == TargetType.HOLD_SECONDS -> FixedProgramItem(v.id, seconds = tier.target.value, perSide = unilateral, estSeconds = tier.target.value)
        tier != null -> FixedProgramItem(v.id, reps = tier.target.value, perSide = unilateral, estSeconds = (tier.target.value * 3).coerceAtLeast(20))
        else -> FixedProgramItem(v.id, seconds = 30, perSide = unilateral, estSeconds = 30)
    }
}

/** The same item with its prescription moved by [delta] (reps by 1, seconds by 5), never below 1; the estimate follows. */
fun adjustProgramItem(item: FixedProgramItem, delta: Int): FixedProgramItem = when {
    item.reps != null -> { val r = (item.reps + delta).coerceIn(1, 999); item.copy(reps = r, estSeconds = if (item.estSeconds != null) (item.estSeconds + (r - item.reps) * 3).coerceAtLeast(1) else null) }
    item.seconds != null -> { val sec = (item.seconds + delta * 5).coerceIn(5, 999); item.copy(seconds = sec, estSeconds = if (item.estSeconds != null) (item.estSeconds + (sec - item.seconds)).coerceAtLeast(1) else null) }
    else -> item
}

/** The same item with its reps (or held seconds) set to [value]; the time estimate follows (about 3 s per rep, one for one per second). */
fun programItemWith(item: FixedProgramItem, value: Int): FixedProgramItem = when {
    item.reps != null -> { val r = value.coerceIn(1, 999); item.copy(reps = r, estSeconds = item.estSeconds?.let { (it + (r - item.reps) * 3).coerceAtLeast(1) }) }
    item.seconds != null -> { val sec = value.coerceIn(1, 999); item.copy(seconds = sec, estSeconds = item.estSeconds?.let { (it + (sec - item.seconds)).coerceAtLeast(1) }) }
    else -> item
}

/**
 * The program as a workout: the items in the order of [format] over [sets] sets. With one set this is exactly the catalog order and
 * prescriptions (never substituted or dropped). Between sets there is [ProgramContent.restSeconds] of rest; between exercises only when [flow] is off.
 */
fun buildProgramPlan(
    catalog: Catalog, content: ProgramContent, profile: EquipmentProfile, flow: Boolean,
    sets: Int = 1, format: WorkoutFormat = WorkoutFormat.CIRCUIT, nowEpochMs: Long = 0L,
): FixedProgramPlanResult {
    val unavailable = content.items.mapNotNull { item ->
        val exercise = catalog.variation(item.exerciseId) ?: return@mapNotNull ProgramItemUnavailable(item.exerciseId, "missing from catalog")
        if (isAvailable(exercise, profile)) null else ProgramItemUnavailable(item.exerciseId, missingFor(exercise, profile))
    }
    if (unavailable.isNotEmpty()) return FixedProgramPlanResult.Unavailable(content.programId, unavailable)
    val n = sets.coerceIn(1, MAX_EXPLICIT_ROUNDS)
    val items = content.items

    fun itemBlocks(index: Int, set: Int): List<TimelineBlock> {
        val item = items[index]
        val exercise = catalog.variation(item.exerciseId)!!
        val target = when {
            item.reps != null -> Target(TargetType.REPS, item.reps)
            item.seconds != null -> Target(TargetType.HOLD_SECONDS, item.seconds)
            else -> error("${content.programId}/${item.exerciseId} has no prescription")
        }
        val sides = if (item.perSide) listOf(Side.LEFT, Side.RIGHT) else listOf(Side.NONE)
        return sides.mapIndexed { sideIndex, side ->
            TimelineBlock(
                id = "fixed-${content.programId}-$index-$sideIndex-${item.exerciseId}" + if (n > 1) "-r$set" else "",
                type = if (exercise.kind == Kind.STRETCH || exercise.kind == Kind.MOBILITY) BlockType.STRETCH else BlockType.WORK,
                // The source estimate is per side when prescribed per side, so each side gets a full timed block.
                durationSeconds = itemSeconds(item),
                roundIndex = if (n > 1) set else null,
                slotId = "fixed-${content.programId}-$index",
                variationId = item.exerciseId,
                side = side,
                target = target,
                // Programs follow their written volume, never the app-level progression ladder.
                prescriptionTier = null,
                mediaId = exercise.mediaId.takeUnless { exercise.demonstrationHold },
                freeTarget = flow,
                note = listOfNotNull(item.notes.takeIf { it.isNotBlank() },
                    if (exercise.demonstrationHold) "Demonstration video is unavailable; follow the written instructions: ${exercise.instructions.joinToString(" ")}" else null)
                    .joinToString("\n").ifBlank { null },
            )
        }
    }

    val order = if (items.isEmpty()) emptyList() else orderedEntries(format, items.size, n)
    val blocks = mutableListOf<TimelineBlock>()
    for ((pos, entry) in order.withIndex()) {
        val (set, index, group) = entry
        val own = itemBlocks(index, set).map { it.copy(groupIndex = group) }
        blocks += own
        val next = order.getOrNull(pos + 1) ?: continue
        val between = isBetweenExercises(format, set, index, group, next)
        if (content.restSeconds > 0 && !(flow && between)) {
            blocks += TimelineBlock("fixed-${content.programId}-$index-rest-r$set-$pos", BlockType.PASSIVE_RECOVERY, content.restSeconds, if (n > 1) set else null,
                "fixed-${content.programId}-$index", recoveryForBlockIds = own.map { it.id }, groupIndex = group)
        }
    }
    val finalBlocks = withSideChangePreparation(blocks)
    val seconds = finalBlocks.sumOf { it.durationSeconds }
    return FixedProgramPlanResult.Ready(WorkoutPlan(
        id = "fixed-${content.programId}",
        routineId = "fixed-program:${content.programId}",
        routineRevision = 1,
        catalogVersion = catalog.catalogVersion,
        createdAtEpochMs = nowEpochMs,
        profileId = profile.id,
        requestedDurationSeconds = seconds,
        plannedDurationSeconds = seconds,
        focus = emptySet(),
        stretchOn = false,
        rounds = n,
        changesExplained = emptyList(),
        warnings = emptyList(),
        needsAcceptance = false,
        usesDraftContent = finalBlocks.any { catalog.variation(it.variationId ?: "")?.reviewState == ReviewState.DRAFT },
        blocks = finalBlocks,
        format = format,
        fixedProgramId = content.programId,
        flow = flow,
    ))
}
