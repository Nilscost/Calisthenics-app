// U10 (F16): the skill tree. Pure layout (deterministic, no overlaps) and node states; the Compose canvas only draws it.
package app.calisthenics.domain.tree

import app.calisthenics.domain.equipment.isAvailable
import app.calisthenics.domain.goals.Goals
import app.calisthenics.domain.model.Catalog
import app.calisthenics.domain.model.EquipmentProfile
import app.calisthenics.domain.model.Kind
import app.calisthenics.domain.progression.ProgressSnapshot

/** One chip of the family picker. [familyIds] are catalog family ids; an empty list is the stretches tab. */
data class TreeTab(val id: String, val title: String, val familyIds: List<String>)

object TreeTabs {
    const val STRETCHES = "stretches"
    val all = listOf(
        TreeTab("push", "Push", listOf("pushup", "planche")),
        TreeTab("shoulders", "Shoulders", listOf("hspu")),
        TreeTab("pull", "Pull", listOf("pullup", "lever")),
        TreeTab("row", "Row", listOf("row")),
        TreeTab("squat", "Squat", listOf("squat")),
        TreeTab("hips", "Hips and back", listOf("bridge", "superman", "deadlift")),
        TreeTab("core", "Core", listOf("plank", "dead-bug", "side-plank", "legraise")),
        TreeTab(STRETCHES, "Stretches", emptyList()),
    )
    fun byId(id: String) = all.firstOrNull { it.id == id }
    /** The tab a goal lives in (its entry exercise's family). */
    fun tabForVariation(c: Catalog, variationId: String): TreeTab? {
        val fam = c.variation(variationId)?.familyId ?: return null
        return all.firstOrNull { fam in it.familyIds }
    }
}

enum class EdgeKind { NEXT, PREREQUISITE }
data class TreeEdge(val from: String, val to: String, val kind: EdgeKind)
data class TreeNodeLayout(val variationId: String, val col: Int, val row: Int)
data class TreeLayout(val nodes: List<TreeNodeLayout>, val edges: List<TreeEdge>, val cols: Int, val rows: Int) {
    fun node(id: String) = nodes.firstOrNull { it.variationId == id }
}

/**
 * Columns run easier -> harder (longest path from a root), rows separate exercises that share a column. Independent chains
 * (e.g. side plank next to the plank chain) are stacked as blocks, so two nodes never share a cell. Same catalog, same layout.
 */
fun layoutTree(c: Catalog, familyIds: Collection<String>): TreeLayout {
    val ex = c.variations.filter { (it.kind == Kind.REPS || it.kind == Kind.HOLD) && it.familyId in familyIds }
    val ids = ex.map { it.id }.toSet()
    val edges = linkedSetOf<TreeEdge>()
    for (p in c.policies) {
        if (p.variationId !in ids) continue
        for (n in p.nextVariationIds) if (n in ids) edges += TreeEdge(p.variationId, n, EdgeKind.NEXT)
    }
    for (p in c.policies) {
        if (p.variationId !in ids) continue
        for (group in p.prerequisiteRule.allOf) for (pred in group.mapNotNull { it.variationTierMet?.variationId }) {
            if (pred in ids && pred != p.variationId && edges.none { it.from == pred && it.to == p.variationId }) edges += TreeEdge(pred, p.variationId, EdgeKind.PREREQUISITE)
        }
    }
    val rank = ex.associate { it.id to it.difficultyRank }
    val order = compareBy<String>({ rank[it] ?: 0 }, { it })
    // longest-path depth (the graph is a DAG; the guard keeps a bad catalog from looping)
    val depth = ids.associateWith { 0 }.toMutableMap()
    repeat(ids.size) { edges.forEach { e -> if (depth.getValue(e.to) < depth.getValue(e.from) + 1) depth[e.to] = depth.getValue(e.from) + 1 } }
    // components
    val parent = ids.associateWith { it }.toMutableMap()
    fun find(x: String): String { var r = x; while (parent.getValue(r) != r) r = parent.getValue(r); return r }
    edges.forEach { e -> val a = find(e.from); val b = find(e.to); if (a != b) parent[maxOf(a, b)] = minOf(a, b) }
    val comps = ids.groupBy { find(it) }.values.map { it.sortedWith(order) }.sortedWith(compareBy({ rank[it.first()] ?: 0 }, { it.first() }))
    val out = mutableListOf<TreeNodeLayout>()
    var rowBase = 0
    var maxCol = 0
    for (comp in comps) {
        val byCol = comp.groupBy { depth.getValue(it) }.toSortedMap()
        var height = 0
        for ((col, members) in byCol) {
            members.sortedWith(order).forEachIndexed { r, id -> out += TreeNodeLayout(id, col, rowBase + r) }
            height = maxOf(height, members.size); maxCol = maxOf(maxCol, col)
        }
        rowBase += height
    }
    return TreeLayout(out, edges.toList(), if (out.isEmpty()) 0 else maxCol + 1, rowBase)
}

enum class TreeNodeState { MASTERED, CURRENT, AVAILABLE, LOCKED, NEEDS_EQUIPMENT }

/** Mastered (all five steps) wins; then missing prerequisites (unless already started); then missing equipment; then "training now" vs open. */
fun nodeState(c: Catalog, id: String, progress: ProgressSnapshot, profile: EquipmentProfile, trainingNow: Set<String>): TreeNodeState {
    val v = c.variation(id) ?: return TreeNodeState.LOCKED
    return when {
        5 in (progress.variations[id]?.achievedTiers ?: emptySet()) -> TreeNodeState.MASTERED
        // someone who started an exercise (self-assessed or trained) is not locked out of it by its prerequisites
        id !in progress.variations && Goals.unmet(c, id, progress).isNotEmpty() -> TreeNodeState.LOCKED
        !isAvailable(v, profile) -> TreeNodeState.NEEDS_EQUIPMENT
        id in trainingNow -> TreeNodeState.CURRENT
        else -> TreeNodeState.AVAILABLE
    }
}

/** What the user is training now: the active exercise of each family they have started, plus the next step toward the goal. */
fun trainingNow(c: Catalog, progress: ProgressSnapshot, goalId: String?): Set<String> {
    val active = progress.activeInFamily.values.filter { v -> 5 !in (progress.variations[v]?.achievedTiers ?: emptySet()) }.toMutableSet()
    Goals.byId(goalId)?.let { g -> Goals.nextToTrain(c, g, progress)?.let { active += it } }
    return active
}

fun stars(progress: ProgressSnapshot, id: String): Int = progress.variations[id]?.earnedStars() ?: 0
