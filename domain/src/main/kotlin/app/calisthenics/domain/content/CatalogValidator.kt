// T06 — catalog / skill-graph validation (spec §3, §8; UI-01, PROG-02).
package app.calisthenics.domain.content

import app.calisthenics.domain.model.Catalog
import app.calisthenics.domain.model.EdgeRelation
import app.calisthenics.domain.model.Kind
import app.calisthenics.domain.model.ReviewState
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
    (c.variations.map { it.id } + c.policies.map { it.id } + c.skillNodes.map { it.id } + c.skillEdges.map { it.id })
        .filterNot { KEBAB.matches(it) }.forEach { e += "id '$it' is not lowercase kebab-case" }

    val vIds = c.variations.map { it.id }.toSet()
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
