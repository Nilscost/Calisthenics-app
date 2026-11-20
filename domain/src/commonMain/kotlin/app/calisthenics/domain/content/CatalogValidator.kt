// T06 — catalog / skill-graph validation (spec §3, §8; UI-01, PROG-02).
package app.calisthenics.domain.content

import app.calisthenics.domain.model.Catalog
import app.calisthenics.domain.model.EdgeRelation
import app.calisthenics.domain.model.Kind
import app.calisthenics.domain.model.ReviewState
import app.calisthenics.domain.model.TargetType
import kotlinx.serialization.json.Json

private val KEBAB = Regex("^[a-z0-9]+(-[a-z0-9]+)*$")

val catalogJson = Json { ignoreUnknownKeys = false; prettyPrint = true; encodeDefaults = false }

fun parseCatalog(text: String): Catalog = catalogJson.decodeFromString(Catalog.serializer(), text)

data class ValidationReport(val errors: List<String>, val warnings: List<String>) {
    val ok: Boolean get() = errors.isEmpty()
}

/**
 * Structural validation. [production] = true additionally rejects DRAFT records
 * (spec §8: no production release on unreviewed content).
 */
fun validateCatalog(c: Catalog, production: Boolean = false): ValidationReport {
    val e = mutableListOf<String>()
    val w = mutableListOf<String>()

    fun dupes(ids: List<String>, what: String) =
        ids.groupBy { it }.filter { it.value.size > 1 }.keys.forEach { e += "duplicate $what id '$it'" }

    dupes(c.variations.map { it.id }, "variation")
    dupes(c.policies.map { it.id }, "policy")
    dupes(c.skillNodes.map { it.id }, "skill node")
    dupes(c.skillEdges.map { it.id }, "skill edge")
    dupes(c.progressionRoutes.map { it.id }, "progression route")
    (c.variations.map { it.id } + c.policies.map { it.id } + c.skillNodes.map { it.id } + c.skillEdges.map { it.id })
        .filterNot { KEBAB.matches(it) }.forEach { e += "id '$it' is not lowercase kebab-case" }

    val vIds = c.variations.map { it.id }.toSet()
    for (route in c.progressionRoutes) {
        if (route.name.isBlank()) e += "route ${route.id}: empty name"
        if (route.mainPath.isEmpty() && (route.goalVariationId != null || route.defaultPool || route.goalIds.isNotEmpty())) e += "route ${route.id}: a goal/default progression needs a main path"
        dupes(route.mainPath, "route ${route.id} main-path variation")
        dupes(route.members.map { it.variationId }, "route ${route.id} member")
        for (id in route.mainPath) if (id !in vIds) e += "route ${route.id}: unknown main-path variation '$id'"
        val mainMembers = route.members.filter { it.role == app.calisthenics.domain.model.ProgressionRole.MAIN }.map { it.variationId }.toSet()
        if (mainMembers != route.mainPath.toSet()) e += "route ${route.id}: MAIN members must exactly match mainPath"
        for (m in route.members) {
            if (m.variationId !in vIds) e += "route ${route.id}: unknown member '${m.variationId}'"
            when (m.role) {
                app.calisthenics.domain.model.ProgressionRole.ALTERNATE -> if (m.atVariationId == null || m.atVariationId !in route.mainPath)
                    e += "route ${route.id}: alternate '${m.variationId}' needs a main-path anchor"
                app.calisthenics.domain.model.ProgressionRole.SUPPORT -> if (m.atVariationId == null || m.atVariationId !in vIds)
                    e += "route ${route.id}: support '${m.variationId}' needs a valid exercise anchor"
                app.calisthenics.domain.model.ProgressionRole.MAIN -> if (m.atVariationId != null && (m.variationId != route.mainPath.firstOrNull() || m.atVariationId !in vIds))
                    e += "route ${route.id}: only the entry main step may reference an external entry anchor"
                else -> if (m.atVariationId != null) e += "route ${route.id}: program/library members cannot have an anchor"
            }
        }
        route.goalVariationId?.let { if (it !in route.mainPath) e += "route ${route.id}: goal '$it' is not on mainPath" }
        for (goal in route.goalIds) if (goal.isBlank()) e += "route ${route.id}: blank goal id"
        if (route.mainPath.size > 1) {
            for ((from, to) in route.mainPath.zipWithNext()) {
                if (c.variation(to) == null) continue
                // Legacy policy edges remain valid transition evidence, but route order is authoritative.
                if (from == to) e += "route ${route.id}: self-cycle at '$from'"
            }
        }
    }
    dupes(c.exerciseGroups.map { it.id }, "exercise group")
    dupes(c.fixedPrograms.map { it.id }, "fixed program")
    for (g in c.exerciseGroups) {
        val main = c.variation(g.mainVariationId)
        if (main == null) e += "group ${g.id}: unknown main exercise '${g.mainVariationId}'"
        if (g.mainVariationId in g.alternativeVariationIds) e += "group ${g.id}: main is also an alternative"
        for (id in g.alternativeVariationIds) {
            val alt = c.variation(id)
            if (alt == null) e += "group ${g.id}: unknown alternative '$id'"
            else if (main != null) {
                val explicitAnchor = c.progressionRoutes.any { route -> main.id in route.mainPath && route.members.any {
                    it.variationId == alt.id && it.role == app.calisthenics.domain.model.ProgressionRole.ALTERNATE && it.atVariationId == main.id
                } }
                val strengthKinds = setOf(app.calisthenics.domain.model.Kind.REPS, app.calisthenics.domain.model.Kind.HOLD)
                if ((c.progressionGroupOf(alt.id) != c.progressionGroupOf(main.id) && !explicitAnchor) || ((alt.kind in strengthKinds) != (main.kind in strengthKinds)))
                    e += "group ${g.id}: incompatible alternative '$id'"
            }
        }
    }
    for (r in c.fixedPrograms) {
        if (r.items.isEmpty()) e += "program ${r.id}: no items"
        for (item in r.items) {
            if (item.exerciseId !in vIds) e += "program ${r.id}: unknown exercise '${item.exerciseId}'"
            if ((item.reps == null) == (item.seconds == null)) e += "program ${r.id}: item '${item.exerciseId}' needs reps or seconds"
            if (item.reps?.let { it <= 0 } == true || item.seconds?.let { it <= 0 } == true) e += "program ${r.id}: nonpositive target"
            if (r.flow && (item.estSeconds ?: item.seconds ?: 0) <= 0) e += "program ${r.id}: flow item '${item.exerciseId}' needs duration"
        }
    }
    for (o in c.onboardingFamilies) {
        if (o.anchorVariationId !in o.ladder) e += "onboarding family '${o.id}': anchor '${o.anchorVariationId}' is not on its ladder"
        for (id in o.ladder) {
            val v = c.variation(id)
            if (v == null) e += "onboarding family '${o.id}': unknown exercise '$id'"
            else if (v.kind != Kind.REPS && v.kind != Kind.HOLD) e += "onboarding family '${o.id}': '$id' is not a strength exercise"
        }
    }
    val pById = c.policies.associateBy { it.id }

    for (v in c.variations) {
        if (v.name.isBlank()) e += "${v.id}: empty name"
        if (v.instructions.isEmpty()) w += "${v.id}: no instructions"
        val isStrength = v.kind == Kind.REPS || v.kind == Kind.HOLD
        if (isStrength) {
            if (v.progressionPolicyId == null) e += "${v.id}: strength variation without progression policy"
            if (v.areas.isEmpty()) e += "${v.id}: strength variation without strength area"
        } else {
            if (v.defaultSeconds == null || v.defaultSeconds <= 0) e += "${v.id}: ${v.kind} needs positive defaultSeconds"
            if (v.kind == Kind.STRETCH && v.stretchAreas.isEmpty()) e += "${v.id}: stretch without stretch areas"
        }
        v.progressionPolicyId?.let { pid ->
            val p = pById[pid]
            if (p == null) e += "${v.id}: unknown policy '$pid'"
            else if (p.variationId != v.id) e += "${v.id}: policy '$pid' belongs to '${p.variationId}'"
        }
        for (s in v.compatibleStretchIds) {
            val sv = c.variation(s)
            if (sv == null) e += "${v.id}: unknown compatible stretch '$s'"
            else if (sv.kind != Kind.STRETCH) e += "${v.id}: compatible stretch '$s' is not a STRETCH"
        }
        if (isStrength && v.compatibleStretchIds.isEmpty()) w += "${v.id}: no compatible stretch (stretch mode cannot fill its recovery)"
        if (v.equipmentAlternatives.isEmpty()) e += "${v.id}: equipmentAlternatives must contain at least one set (empty set = bodyweight)"
        if (v.reviewState == ReviewState.REVIEWED) {
            if (v.reviewer.isNullOrBlank() || v.reviewedAt.isNullOrBlank()) e += "${v.id}: REVIEWED without reviewer/date"
            if (v.sourceIds.isEmpty()) e += "${v.id}: REVIEWED without sources"
        }
        if (production && v.reviewState != ReviewState.REVIEWED) e += "${v.id}: DRAFT content cannot ship in production"
    }

    for (p in c.policies) {
        if (p.variationId !in vIds) e += "${p.id}: unknown variation '${p.variationId}'"
        if (p.tiers.map { it.index } != listOf(1, 2, 3, 4, 5)) e += "${p.id}: must have exactly five tiers 1..5 in order"
        if (p.tiers.map { it.target.type }.distinct().size > 1) e += "${p.id}: tiers mix target types"
        val kind = c.variation(p.variationId)?.kind
        if (kind == Kind.REPS && p.tiers.any { it.target.type != TargetType.REPS } ||
            kind == Kind.HOLD && p.tiers.any { it.target.type != TargetType.HOLD_SECONDS }) e += "${p.id}: target type does not match exercise kind"
        p.maxRepsPerSet?.let { cap ->
            if (cap <= 0 || kind != Kind.REPS) e += "${p.id}: invalid rep ceiling"
            if (p.tiers.any { (it.target.max ?: it.target.value) > cap }) e += "${p.id}: tier target exceeds rep ceiling"
        }
        p.mainSuccessorId?.let { if (it !in p.nextVariationIds) e += "${p.id}: main successor is not a next variation" }
        p.tiers.zipWithNext().forEach { (a, b) ->
            if (b.target.value < a.target.value) e += "${p.id}: tier ${b.index} target easier than tier ${a.index}"
        }
        for (t in p.tiers) t.earlyCompletionStretchId?.let { s ->
            if (c.variation(s)?.kind != Kind.STRETCH) e += "${p.id} tier ${t.index}: early-completion '$s' is not a known STRETCH"
        }
        for (n in p.nextVariationIds) if (n !in vIds) e += "${p.id}: unknown next variation '$n'"
        for (group in p.prerequisiteRule.allOf) for (pred in group) {
            pred.variationTierMet?.let { vt ->
                if (vt.variationId !in vIds) e += "${p.id}: prerequisite on unknown variation '${vt.variationId}'"
                if (vt.tier !in 1..5) e += "${p.id}: prerequisite tier ${vt.tier} out of range"
            }
        }
        if (production && !p.approved) e += "${p.id}: policy not approved"
    }

    // progression graph acyclic (nextVariationIds)
    val next = c.policies.associate { it.variationId to it.nextVariationIds }
    findCycle(vIds, next)?.let { e += "progression cycle: ${it.joinToString(" -> ")}" }

    // skill graph
    val nodeIds = c.skillNodes.map { it.id }.toSet()
    for (n in c.skillNodes) {
        if (n.variationIds.isEmpty()) e += "skill ${n.id}: no variations"
        n.variationIds.filter { it !in vIds }.forEach { e += "skill ${n.id}: unknown variation '$it'" }
    }
    for (ed in c.skillEdges) {
        if (ed.fromId !in nodeIds) e += "edge ${ed.id}: dangling from '${ed.fromId}'"
        if (ed.toId !in nodeIds) e += "edge ${ed.id}: dangling to '${ed.toId}'"
        if (ed.fromId == ed.toId) e += "edge ${ed.id}: self-loop"
    }
    val prereq = c.skillEdges.filter { it.relation == EdgeRelation.PREREQUISITE }
        .groupBy({ it.fromId }, { it.toId })
    findCycle(nodeIds, prereq)?.let { e += "skill prerequisite cycle: ${it.joinToString(" -> ")}" }

    for (id in c.warmupTemplate + c.cooldownTemplate) {
        val v = c.variation(id)
        if (v == null) e += "warm-up/cool-down template references unknown '$id'"
        else if (v.kind != Kind.MOBILITY && v.kind != Kind.STRETCH) e += "template entry '$id' must be MOBILITY or STRETCH"
    }
    return ValidationReport(e, w)
}

private fun findCycle(nodes: Set<String>, edges: Map<String, List<String>>): List<String>? {
    val state = HashMap<String, Int>() // 1 = visiting, 2 = done
    val stack = ArrayList<String>()
    fun dfs(n: String): List<String>? {
        state[n] = 1; stack += n
        for (m in edges[n].orEmpty()) {
            when (state[m]) {
                1 -> return stack.subList(stack.indexOf(m), stack.size) + m
                null -> dfs(m)?.let { return it }
            }
        }
        state[n] = 2; stack.removeAt(stack.size - 1)
        return null
    }
    for (n in nodes.sorted()) if (state[n] == null) dfs(n)?.let { return it }
    return null
}
