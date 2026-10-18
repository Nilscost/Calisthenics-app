// U10 (F16): the skill tree. Pure layout (deterministic, no overlaps) and node states; the Compose canvas only draws it.
package app.calisthenics.domain.tree

import app.calisthenics.domain.equipment.isAvailable
import app.calisthenics.domain.goals.Goal
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
        // R7-T11 (D7, O3): the weighted exercises sit in their movement's tab, next to the bodyweight one they build on (the prerequisite edge draws the side branch)
        TreeTab("push", "Push", listOf("pushup", "dip", "planche", "vest-push", "vest-dip")),
        TreeTab("shoulders", "Handstand", listOf("hspu", "handstand", "arm-balance")),
        TreeTab("pull", "Pull", listOf("pullup", "lever", "german-hang", "grip", "vest-pull")),
        TreeTab("row", "Row", listOf("row", "kb-row", "db-row", "arms")),
        // R8.2-T01 (G1): the lower-body power families sit with squat and lunge work; the power push-ups stay in Push (their family is the push-up one)
        TreeTab("squat", "Squat", listOf("squat", "lunge", "step-up", "quad", "db-squat", "bb-squat", "vest-squat", "plyo", "plyo-lateral", "plyo-box")),
        TreeTab("hips", "Hips and back", listOf("bridge", "superman", "reverse-hyper", "deadlift", "rdl", "nordic", "slide", "jefferson", "db-hinge", "bb-hinge", "db-bridge")),
        TreeTab("core", "Core", listOf("plank", "plank-tap", "dead-bug", "side-plank", "pallof", "carry", "legraise", "rollout", "hanging-legraise", "dragon-flag", "bird-dog", "crunch", "flag")),
        TreeTab("calves", "Lower legs", listOf("calf", "tibialis", "balance")),
        TreeTab("rings", "Rings", listOf("ring-support", "ring-dip", "ring-pushup", "ring-mu")),
        TreeTab("conditioning", "Conditioning", listOf("conditioning", "agility", "crawl")),
        TreeTab("prehab", "Prehab", listOf("prehab-shoulder", "prehab-wrist", "prehab-hip", "prehab-knee", "neck")),
        TreeTab("pilates", "Pilates", listOf("pilates")),
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

private class Graph(val ids: Set<String>, val edges: List<TreeEdge>, val order: Comparator<String>, val depth: Map<String, Int>, val comps: List<List<String>>)

private fun graphOf(c: Catalog, familyIds: Collection<String>): Graph {
    val ex = c.variations.filter { (it.kind == Kind.REPS || it.kind == Kind.HOLD) && !it.routineOnly && it.familyId in familyIds }
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
    return Graph(ids, edges.toList(), order, depth, comps)
}

/** Case-insensitive name and alias lookup for catalog browsing. Drill-only routine rows stay private to programs. */
fun searchVariations(c: Catalog, query: String): List<app.calisthenics.domain.model.ExerciseVariation> {
    val needle = query.trim().lowercase()
    if (needle.isEmpty()) return emptyList()
    return c.variations.asSequence().filter { !it.routineOnly }
        .filter { it.name.contains(needle, ignoreCase = true) || it.aliases.any { alias -> alias.contains(needle, ignoreCase = true) } }
        .sortedBy { it.name.lowercase() }.toList()
}

/**
 * Columns run easier -> harder (longest path from a root), rows separate exercises that share a column. Independent chains
 * (e.g. side plank next to the plank chain) are stacked as blocks, so two nodes never share a cell. Same catalog, same layout.
 */
fun layoutTree(c: Catalog, familyIds: Collection<String>): TreeLayout {
    val g = graphOf(c, familyIds)
    val out = mutableListOf<TreeNodeLayout>()
    var rowBase = 0
    var maxCol = 0
    for (comp in g.comps) {
        val byCol = comp.groupBy { g.depth.getValue(it) }.toSortedMap()
        var height = 0
        for ((col, members) in byCol) {
            members.sortedWith(g.order).forEachIndexed { r, id -> out += TreeNodeLayout(id, col, rowBase + r) }
            height = maxOf(height, members.size); maxCol = maxOf(maxCol, col)
        }
        rowBase += height
    }
    return TreeLayout(out, g.edges, if (out.isEmpty()) 0 else maxCol + 1, rowBase)
}

/**
 * V13 (R23, doc 17 §2.6): the same graph laid out vertically: easier at the top, harder below (row = depth). Exercises that share a
 * depth sit side by side (column = position among them), which is where the paths branch. Independent chains are stacked one after
 * the other. `col` is the horizontal index, `row` the vertical one.
 */
fun layoutTreeVertical(c: Catalog, familyIds: Collection<String>): TreeLayout {
    val g = graphOf(c, familyIds)
    val out = mutableListOf<TreeNodeLayout>()
    var rowBase = 0
    var maxCols = 0
    for (comp in g.comps) {
        val byDepth = comp.groupBy { g.depth.getValue(it) }.toSortedMap()
        for ((d, members) in byDepth) {
            members.sortedWith(g.order).forEachIndexed { i, id -> out += TreeNodeLayout(id, i, rowBase + d) }
            maxCols = maxOf(maxCols, members.size)
        }
        rowBase += (byDepth.keys.maxOrNull() ?: 0) + 1
    }
    return TreeLayout(out, g.edges, maxCols, rowBase)
}

/** V13 "By skill": the exercises that lead to a skill, in order, as one vertical chain. */
fun skillLayout(c: Catalog, goal: Goal): TreeLayout {
    val chain = Goals.chain(c, goal).filter { c.variation(it) != null }
    return TreeLayout(chain.mapIndexed { i, id -> TreeNodeLayout(id, 0, i) }, chain.zipWithNext().map { (a, b) -> TreeEdge(a, b, EdgeKind.NEXT) }, if (chain.isEmpty()) 0 else 1, chain.size)
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

/**
 * R26 / D8: what the tree shows. Your own stars, but an exercise you have clearly moved past (a harder exercise that follows it has
 * been started) shows all five, because the level is below your current one.
 */
fun displayStars(c: Catalog, progress: ProgressSnapshot, id: String): Int {
    val own = stars(progress, id)
    if (own >= 5) return own
    val seen = mutableSetOf(id)
    val queue = ArrayDeque(listOf(id))
    while (queue.isNotEmpty()) {
        val cur = queue.removeFirst()
        for (n in c.policyForVariation(cur)?.nextVariationIds.orEmpty()) {
            if (!seen.add(n)) continue
            if (progress.variations.containsKey(n)) return 5
            queue += n
        }
    }
    return own
}

/**
 * R8.1-T10 (F13): the tree of one route: the main spine in column 0, and every same-rung alternate right beside its rung (columns 1 and 2,
 * more rows when a rung has more than two), so "diamond next to feet-elevated" is on the screen without opening anything.
 * Support drills are not drawn here: they stay behind "Helps with".
 */
fun routeTreeLayout(route: app.calisthenics.domain.model.ProgressionRoute, exists: (String) -> Boolean = { true }): TreeLayout {
    val nodes = mutableListOf<TreeNodeLayout>()
    var row = 0
    var cols = if (route.mainPath.isEmpty()) 0 else 1
    for (id in route.mainPath) {
        nodes += TreeNodeLayout(id, 0, row)
        val alternates = route.members.filter { it.role == app.calisthenics.domain.model.ProgressionRole.ALTERNATE && it.atVariationId == id && it.variationId !in route.mainPath && exists(it.variationId) }
            .map { it.variationId }.distinct()
        alternates.chunked(2).forEachIndexed { k, chunk ->
            chunk.forEachIndexed { j, alt -> nodes += TreeNodeLayout(alt, 1 + j, row + k) }
            cols = maxOf(cols, 1 + chunk.size)
        }
        row += maxOf(1, (alternates.size + 1) / 2)
    }
    return TreeLayout(nodes, route.mainPath.zipWithNext().map { (a, b) -> TreeEdge(a, b, EdgeKind.NEXT) }, cols, row)
}
