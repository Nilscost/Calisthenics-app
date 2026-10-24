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
        tier != null && tier.target.type == TargetType.HOLD_SECONDS -> FixedProgramItem(v.id, seconds = tier.target.value, perSide = unilateral, estSeconds = tier.target.value, appLevel = tier.index)
        tier != null -> FixedProgramItem(v.id, reps = tier.target.value, perSide = unilateral, estSeconds = (tier.target.value * 3).coerceAtLeast(20), appLevel = tier.index)
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
    betweenSetMode: BetweenSetMode? = null, preparationSeconds: Int = 5,
): FixedProgramPlanResult {
    val mode = effectiveBetweenSetMode(betweenSetMode, flow, false)
    val prep = preparationSeconds.coerceIn(1, 30)
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
        val policy = catalog.policyFor(exercise)
        val appTier = item.appLevel?.coerceIn(1, 5)?.let { level -> policy?.tiers?.firstOrNull { it.index == level } }
        val extensionEligible = item.levelFiveTarget != null && item.appLevel == 5 && appTier != null
        val requestedTarget = when {
            extensionEligible -> Target(appTier!!.target.type, maxOf(item.levelFiveTarget!!.coerceIn(1, 999), appTier!!.target.value))
            appTier != null -> appTier.target
            item.reps != null -> Target(TargetType.REPS, item.reps)
            item.seconds != null -> Target(TargetType.HOLD_SECONDS, item.seconds)
            else -> error("${content.programId}/${item.exerciseId} has no prescription")
        }
        val target = policy?.let { cappedTarget(requestedTarget, it) } ?: requestedTarget
        val duration = if (appTier == null) itemSeconds(item) else when (target.type) {
            TargetType.HOLD_SECONDS -> maxOf(itemSeconds(item), target.shownValue())
            TargetType.REPS -> maxOf(itemSeconds(item), target.shownValue() * 3)
        }
        val sides = if (item.perSide) listOf(Side.LEFT, Side.RIGHT) else listOf(Side.NONE)
        return sides.mapIndexed { sideIndex, side ->
            TimelineBlock(
                id = "fixed-${content.programId}-$index-$sideIndex-${item.exerciseId}" + if (n > 1) "-r$set" else "",
                type = if (exercise.kind == Kind.STRETCH || exercise.kind == Kind.MOBILITY) BlockType.STRETCH else BlockType.WORK,
                // The source estimate is per side when prescribed per side, so each side gets a full timed block.
                durationSeconds = duration,
                roundIndex = if (n > 1) set else null,
                slotId = "fixed-${content.programId}-$index",
                variationId = item.exerciseId,
                side = side,
                target = target,
                // Programs follow their written volume, never the app-level progression ladder.
                prescriptionTier = appTier?.index ?: if (extensionEligible) 5 else null,
                mediaId = exercise.mediaId.takeUnless { exercise.demonstrationHold },
                freeTarget = item.appLevel == null && flow,
                levelFiveEligible = extensionEligible,
                targetRevision = catalog.personalTargets[exercise.id]?.revision ?: 0,
                note = listOfNotNull(item.notes.takeIf { it.isNotBlank() },
                    if (exercise.demonstrationHold) "Demonstration video is unavailable; follow the written instructions: ${exercise.instructions.joinToString(" ")}" else null)
                    .joinToString("\n").ifBlank { null },
            )
        }
    }

    val order = if (items.isEmpty()) emptyList() else orderedEntries(format, items.size, n)
    val blocks = mutableListOf<TimelineBlock>()
    val stretchUses = mutableMapOf<String, Int>()
    var stretchFallbackUsed = false
    for ((pos, entry) in order.withIndex()) {
        val (set, index, group) = entry
        val own = itemBlocks(index, set).map { it.copy(groupIndex = group) }
        blocks += own
        val next = order.getOrNull(pos + 1) ?: continue
        if (content.restSeconds > 0 && mode == BetweenSetMode.REST) {
            blocks += TimelineBlock("fixed-${content.programId}-$index-rest-r$set-$pos", BlockType.PASSIVE_RECOVERY, content.restSeconds, if (n > 1) set else null,
                "fixed-${content.programId}-$index", recoveryForBlockIds = own.map { it.id }, groupIndex = group)
        } else if (content.restSeconds > 0 && mode == BetweenSetMode.STRETCH) {
            val source = catalog.variation(items[index].exerciseId)
            val stretch = source?.compatibleStretchIds.orEmpty().mapNotNull(catalog::variation)
                .filter { it.kind == Kind.STRETCH && !it.demonstrationHold && !it.optIn && isAvailable(it, profile) }
                .filter { it.stretchUse in setOf(StretchUse.BETWEEN_SETS, StretchUse.BOTH) }
                .sortedBy { it.id }.firstOrNull { (stretchUses[it.id] ?: 0) < app.calisthenics.domain.planner.MAX_STRETCH_USES }
            if (stretch == null) {
                stretchFallbackUsed = true
                blocks += TimelineBlock("fixed-${content.programId}-$index-rest-r$set-$pos", BlockType.PASSIVE_RECOVERY, content.restSeconds, if (n > 1) set else null,
                    "fixed-${content.programId}-$index", recoveryForBlockIds = own.map { it.id }, groupIndex = group)
            } else {
                stretchUses[stretch.id] = (stretchUses[stretch.id] ?: 0) + 1
                val sides = if (stretch.unilateral) listOf(Side.LEFT, Side.RIGHT) else listOf(Side.BOTH)
                val each = if (stretch.unilateral) maxOf((content.restSeconds + 1) / 2, stretch.defaultSeconds ?: 0) else maxOf(content.restSeconds, stretch.defaultSeconds ?: 0)
                sides.forEach { side -> blocks += TimelineBlock("fixed-${content.programId}-$index-stretch-${side.name.first()}-r$set-$pos", BlockType.STRETCH, each, if (n > 1) set else null,
                    "fixed-${content.programId}-$index", stretch.id, side, recoveryForBlockIds = own.map { it.id }, mediaId = stretch.mediaId, groupIndex = group) }
            }
        }
    }
    val finalBlocks = buildList {
        var workSeen = false
        var nPrep = 0
        for (block in blocks) {
            if (mode == BetweenSetMode.FLOW && block.type == BlockType.WORK && workSeen) {
                add(TimelineBlock("fixed-${content.programId}-prepare-${++nPrep}", BlockType.TRANSITION, prep,
                    block.roundIndex, block.slotId, block.variationId, block.side, block.target, prescriptionTier = block.prescriptionTier, mediaId = block.mediaId, groupIndex = block.groupIndex))
            }
            add(block)
            if (block.type == BlockType.WORK) workSeen = true
        }
    }.let { withSideChangePreparation(it).map { b -> if (b.type == BlockType.TRANSITION) b.copy(durationSeconds = prep) else b } }
    val seconds = finalBlocks.sumOf { it.durationSeconds }
    return FixedProgramPlanResult.Ready(WorkoutPlan(
        progressionEvidenceVersion = 1,
        id = "fixed-${content.programId}",
        routineId = "fixed-program:${content.programId}",
        routineRevision = 1,
        catalogVersion = catalog.catalogVersion,
        createdAtEpochMs = nowEpochMs,
        profileId = profile.id,
        progressionPolicies = finalBlocks.mapNotNull { it.variationId }.distinct().mapNotNull { id -> catalog.policyForVariation(id)?.let { id to it } }.toMap(),
        requestedDurationSeconds = seconds,
        plannedDurationSeconds = seconds,
        focus = emptySet(),
        stretchOn = mode == BetweenSetMode.STRETCH,
        rounds = n,
        changesExplained = if (stretchFallbackUsed) listOf("Some program breaks use passive rest because no compatible stretch is available or its repeat limit was reached.") else emptyList(),
        warnings = emptyList(),
        needsAcceptance = false,
        usesDraftContent = finalBlocks.any { catalog.variation(it.variationId ?: "")?.reviewState == ReviewState.DRAFT },
        blocks = finalBlocks,
        format = format,
        fixedProgramId = content.programId,
        flow = mode == BetweenSetMode.FLOW,
        betweenSetMode = mode,
        preparationSeconds = prep,
    ))
}
