package app.calisthenics.domain

import app.calisthenics.domain.content.parseCatalog
import app.calisthenics.domain.content.validateCatalog
import app.calisthenics.domain.model.*
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Validates the real bundled starter catalog (content/starter/catalog.json). */
class StarterCatalogTest {
    private val root = File(System.getProperty("repo.root") ?: ".")
    private val catalog: Catalog by lazy { parseCatalog(File(root, "content/starter/catalog.json").readText()) }

    @Test fun parsesAndIsStructurallyValid() {
        val r = validateCatalog(catalog)
        assertTrue("errors: ${r.errors}", r.ok)
        assertEquals("warnings: ${r.warnings}", emptyList<String>(), r.warnings)
    }

    @Test fun productionBuildRejectsDraftContent() {
        val r = validateCatalog(catalog, production = true)
        assertTrue(!r.ok)
        assertTrue(r.errors.any { "DRAFT" in it })
    }

    @Test fun everythingIsDraftAndHeuristic() {
        assertTrue(catalog.variations.all { it.reviewState == ReviewState.DRAFT })
        assertTrue(catalog.policies.all { it.policyKind == PolicyKind.PRODUCT_HEURISTIC && !it.approved })
    }

    @Test fun coversPlannedScope() {
        val strength = catalog.variations.filter { it.kind == Kind.REPS || it.kind == Kind.HOLD }
        val stretches = catalog.variations.filter { it.kind == Kind.STRETCH || it.kind == Kind.MOBILITY }
        assertEquals(43, strength.size) // M6b: chains toward HSPU, muscle-up, pistol, L-sit, front lever, planche
        assertEquals(7, stretches.size)
        val areas = strength.flatMap { it.areas }.toSet()
        assertEquals(setOf(Area.UPPER_BODY, Area.LOWER_BODY, Area.CORE), areas)
    }

    @Test fun everyStrengthExerciseHasSourceAndStretch() {
        for (v in catalog.variations.filter { it.kind == Kind.REPS || it.kind == Kind.HOLD }) {
            assertTrue(v.id, v.sourceIds.isNotEmpty())
            assertTrue(v.id, v.compatibleStretchIds.isNotEmpty())
            assertTrue(v.id, v.instructions.size >= 3)
        }
    }

    @Test fun everyStretchAreaIsReachableByAtLeastOneStrengthExercise() {
        val reachable = catalog.variations.flatMap { it.compatibleStretchIds }.toSet()
        for (s in catalog.variations.filter { it.kind == Kind.STRETCH }) assertTrue("${s.id} unreachable", s.id in reachable)
    }

    @Test fun tiersAreMonotonicAndHoldsFitTheirWindow() {
        for (p in catalog.policies) {
            assertEquals(p.id, listOf(1, 2, 3, 4, 5), p.tiers.map { it.index })
            assertTrue(p.id, p.tiers.zipWithNext().all { (a, b) -> b.target.value > a.target.value })
        }
    }

    @Test fun noBallisticKettlebellWork() {
        val kb = catalog.variations.filter { v -> v.equipmentAlternatives.any { s -> s.needs.any { it.equipmentId == "kettlebell" } } }
        assertTrue(kb.isNotEmpty())
        for (v in kb) assertTrue(v.id, v.cautions.any { "ballistic" in it })
    }

    @Test fun plankAndEveryChainTopLeadsSomewhere() {
        assertEquals(listOf("hollow-hold"), catalog.policyForVariation("plank")!!.nextVariationIds)
        val succ = catalog.policies.flatMap { it.nextVariationIds }.toSet()
        // every exercise with a prerequisite is reachable as someone's successor (no orphan tree nodes)
        // Entry points of the skill chains are reached by choosing a goal, not by automatic progression.
        val goalEntries = setOf("pike-pushup", "planche-lean", "front-lever-tuck")
        for (p in catalog.policies.filter { it.prerequisiteRule.allOf.isNotEmpty() && it.variationId !in goalEntries })
            assertTrue("${p.variationId} unreachable", p.variationId in succ)
    }

    @Test fun hardSkillChainsExistInOrder() {
        fun chain(from: String): List<String> { val out = mutableListOf(from); var c = from
            while (true) { c = catalog.policyForVariation(c)!!.nextVariationIds.firstOrNull() ?: break; out += c }; return out }
        assertEquals(listOf("pike-pushup", "pike-pushup-elevated", "wall-handstand-hold", "hspu-wall-negative", "hspu-wall"), chain("pike-pushup"))
        assertEquals("muscle-up-bar", chain("pullup-band-assisted").last())
        assertEquals("squat-pistol", chain("squat-air").last())
        assertEquals("v-sit-floor", chain("plank").last().let { chain("hollow-hold").last() })
        assertEquals("planche-adv-tuck", chain("planche-lean").last())
        assertEquals("front-lever-straddle", chain("front-lever-tuck").last())
    }
}
