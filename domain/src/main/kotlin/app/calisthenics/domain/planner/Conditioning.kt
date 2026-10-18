// R8.2-T03 (G3): the optional conditioning finisher of the Include row: 5 to 10 minutes at the end of the workout, a short circuit of timed
// intervals from the Conditioning category (burpee route, jacks, mountain climbers, jump rope, agility, crawls) at the person's level,
// filtered by equipment. Flow: no rest between its exercises, a short rest between rounds. It carries no target and never counts towards a level.
package app.calisthenics.domain.planner

import app.calisthenics.domain.equipment.isAvailable
import app.calisthenics.domain.goals.Goals
import app.calisthenics.domain.model.*
import app.calisthenics.domain.progression.ProgressSnapshot

const val CONDITIONING_INTERVAL_SECONDS = 30
const val CONDITIONING_ROUND_REST_SECONDS = 10

/** The exercises of the finisher for this person, in order: one per need, an easier variant until standard push-ups and air squats are reached. */
fun conditioningExercises(c: Catalog, progress: ProgressSnapshot, profile: EquipmentProfile, removed: Set<String> = emptySet()): List<ExerciseVariation> {
    val beginner = !Goals.reached(c, "pushup-standard", progress) || !Goals.reached(c, "squat-air", progress)
    fun burpee(): List<String> {
        if (beginner) return listOf("squat-thrust-step-back", "squat-thrust")
        val path = c.routeById("burpee")?.mainPath.orEmpty().ifEmpty { listOf("squat-thrust", "burpee", "burpee-pushup") }
        val current = path.lastOrNull { it in progress.variations && 5 !in progress.variations.getValue(it).achievedTiers }
            ?: path.lastOrNull { it in progress.variations }?.let { path.getOrNull(path.indexOf(it) + 1) } ?: "burpee"
        return listOf(current, "burpee", "squat-thrust")
    }
    val groups = listOf(
        burpee(),
        if (beginner) listOf("half-jack", "jumping-jack") else listOf("jumping-jack", "half-jack"),
        if (beginner) listOf("mountain-climber-slow", "mountain-climber") else listOf("mountain-climber", "mountain-climber-slow"),
        listOf("jump-rope", "high-knees"),
        listOf("lateral-shuffle"),
        listOf("bear-crawl"),
    )
    return groups.mapNotNull { g -> g.firstNotNullOfOrNull { id -> c.variation(id)?.takeIf { it.id !in removed && !it.demonstrationHold && isAvailable(it, profile) && Goals.unmet(c, it.id, progress).isEmpty() } } }.distinctBy { it.id }
}

/** The finisher's blocks, fitted to [minutes] (5 to 10): rounds of 30 s intervals with 10 s between rounds; never longer than asked. */
fun conditioningBlocks(c: Catalog, exercises: List<ExerciseVariation>, minutes: Int): List<TimelineBlock> {
    if (exercises.isEmpty()) return emptyList()
    val budget = minutes.coerceIn(MIN_CONDITIONING_MINUTES, MAX_CONDITIONING_MINUTES) * 60
    // the circuit (4 to 6 exercises, 1 to 4 rounds) that fills the time best without going over
    val candidates = (minOf(4, exercises.size)..minOf(6, exercises.size)).flatMap { n -> (1..4).map { r -> Triple(n, r, r * n * CONDITIONING_INTERVAL_SECONDS + (r - 1) * CONDITIONING_ROUND_REST_SECONDS) } }
    val best = candidates.filter { it.third <= budget }.maxWithOrNull(compareBy({ it.third }, { it.first })) ?: candidates.minByOrNull { it.third }!!
    val items = exercises.take(best.first); val rounds = best.second
    val out = mutableListOf<TimelineBlock>()
    for (r in 1..rounds) {
        items.forEachIndexed { i, v ->
            out += TimelineBlock("cond-r$r-$i", BlockType.WORK, CONDITIONING_INTERVAL_SECONDS, r, "cond-$i", v.id, Side.NONE, null, null,
                mediaId = v.mediaId, freeTarget = true, note = "Conditioning · $CONDITIONING_INTERVAL_SECONDS s at a steady, controlled pace")
        }
        if (r < rounds) out += TimelineBlock("cond-rest-$r", BlockType.PASSIVE_RECOVERY, CONDITIONING_ROUND_REST_SECONDS, r, "cond-rest")
    }
    return out
}

/** Inserts the finisher after the last strength block, before the cool-down. A plan that cannot be built (no usable exercise) is returned as it was. */
fun withConditioningFinisher(c: Catalog, result: PlanResult, progress: ProgressSnapshot, profile: EquipmentProfile, minutes: Int, removed: Set<String>): PlanResult {
    if (result !is PlanResult.Ready) return result
    val plan = result.plan
    val extra = conditioningBlocks(c, conditioningExercises(c, progress, profile, removed), minutes)
    if (extra.isEmpty()) return result
    val lastWork = plan.blocks.indexOfLast { it.type == BlockType.WORK && it.slotId?.startsWith("cond") != true }
    val at = if (lastWork >= 0) lastWork + 1 else plan.blocks.size
    // the stretch/rest that followed the last exercise stays before the finisher
    var insert = at
    while (insert < plan.blocks.size && plan.blocks[insert].type in setOf(BlockType.STRETCH, BlockType.PASSIVE_RECOVERY, BlockType.TRANSITION) && plan.blocks[insert].roundIndex != null) insert++
    val blocks = plan.blocks.take(insert) + extra + plan.blocks.drop(insert)
    return PlanResult.Ready(plan.copy(blocks = blocks, plannedDurationSeconds = blocks.sumOf { it.durationSeconds }, requestedDurationSeconds = maxOf(plan.requestedDurationSeconds, blocks.sumOf { it.durationSeconds }),
        changesExplained = plan.changesExplained + "Conditioning finisher: ${extra.count { it.type == BlockType.WORK }} intervals of $CONDITIONING_INTERVAL_SECONDS s with no rest between exercises, about ${(extra.sumOf { it.durationSeconds } + 30) / 60} min."))
}
