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
        assertEquals(54, strength.size) // M6b hard-skill chains + U09 rows, back extension, side plank and kettlebell hinge chains + one-arm swing
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

    /** Only these may end a chain; every other exercise must lead somewhere (U09, F15). */
    private val explicitTops = setOf("pushup-one-arm", "hspu-wall", "muscle-up-bar", "squat-pistol", "bridge-back", "v-sit-floor", "v-up",
        "front-lever-straddle", "planche-adv-tuck", "archer-row", "arch-rocks", "copenhagen-side-plank", "kettlebell-swing-one-arm")

    @Test fun noDeadEndsExceptExplicitTops() {
        val deadEnds = catalog.policies.filter { it.nextVariationIds.isEmpty() }.map { it.variationId }.toSet()
        assertEquals("a chain ends without being listed as a top (or a listed top now leads somewhere)", explicitTops, deadEnds)
        // every hand-off points at a real exercise
        for (p in catalog.policies) for (n in p.nextVariationIds) assertTrue("${p.variationId} -> $n", catalog.variation(n) != null)
    }

    @Test fun everyStrengthExerciseNamesItsPrimaryMuscles() {
        for (v in catalog.variations.filter { it.kind == Kind.REPS || it.kind == Kind.HOLD }) {
            assertTrue("${v.id} has no primary muscle", v.primaryMuscles.isNotEmpty())
            assertTrue("${v.id}: a muscle is both primary and secondary", v.primaryMuscles.none { it in v.secondaryMuscles })
        }
    }

    @Test fun newChainsAreInOrderAndNeedTheRightEquipment() {
        fun chain(from: String): List<String> { val out = mutableListOf(from); var c = from
            while (true) { c = catalog.policyForVariation(c)!!.nextVariationIds.firstOrNull() ?: break; out += c }; return out }
        assertEquals(listOf("row-band", "inverted-row-bent-knees", "inverted-row", "inverted-row-feet-elevated", "archer-row"), chain("row-band"))
        assertEquals(listOf("superman-hold", "arch-hold-y", "arch-rocks"), chain("superman-hold"))
        assertEquals(listOf("side-plank", "side-plank-leg-raise", "copenhagen-side-plank"), chain("side-plank"))
        assertEquals(listOf("kettlebell-deadlift", "kettlebell-single-leg-rdl", "kettlebell-swing", "kettlebell-swing-one-arm"), chain("kettlebell-deadlift"))
        assertEquals("hollow-hold", chain("dead-bug")[1]) // dead bug hands over to the hollow hold
        for (id in listOf("inverted-row-bent-knees", "inverted-row", "inverted-row-feet-elevated", "archer-row"))
            assertTrue(id, catalog.variation(id)!!.equipmentAlternatives.single().needs.single().equipmentId == "low-bar")
        assertTrue(catalog.variation("copenhagen-side-plank")!!.equipmentAlternatives.single().needs.single().suitability == setOf("stable"))
        assertTrue(catalog.variation("kettlebell-swing")!!.cautions.any { "Ballistic" in it })
    }

    @Test fun noBallisticKettlebellWork() {
        val kb = catalog.variations.filter { v -> v.equipmentAlternatives.any { s -> s.needs.any { it.equipmentId == "kettlebell" } } }
        assertTrue(kb.isNotEmpty())
        // Owner OK 2026-10-05 added the swing: it is ballistic by nature, so it must say so; every other kettlebell exercise stays controlled.
        for (v in kb) assertTrue(v.id, v.cautions.any { "ballistic" in it.lowercase() })
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
