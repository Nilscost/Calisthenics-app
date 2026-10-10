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
        assertEquals(86, strength.size) // V27: +11 (dumbbells, barbell, vest); V25: +5 (C-E); V21b: +16 (dips, hinge paths, core, Minimalist pieces); before: M6b hard-skill chains + U09 rows, back extension, side plank and kettlebell hinge chains + one-arm swing
        assertEquals(25, stretches.size) // V07: 7 + 11 new stretches (C-A); V21b: +7 warm-up items
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
        "front-lever-straddle", "planche-adv-tuck", "archer-row", "arch-rocks", "copenhagen-side-plank", "kettlebell-swing-one-arm",
        // V21b: ends of the RR paths and single exercises of the ready-made routines
        "dip-parallel", "nordic-curl", "slide-single-leg", "pallof-press", "reverse-hyperextension", "plank-shoulder-tap", "walking-lunge",
        "squat-shrimp" /* V25: the shrimp path is an alternative after the Bulgarian split squat */,
        // V27: extra-equipment exercises (dumbbells, barbell, vest)
        "goblet-squat", "dumbbell-single-leg-rdl", "weighted-glute-bridge", "dumbbell-row", "barbell-squat", "barbell-rdl", "weighted-pushup", "weighted-pullup", "weighted-dip", "weighted-squat")

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
        val goalEntries = setOf("pike-pushup", "planche-lean", "front-lever-tuck", "plank-shoulder-tap", "walking-lunge" /* Minimalist circuit steps */,
            "goblet-squat", "dumbbell-rdl", "weighted-glute-bridge", "dumbbell-row", "barbell-squat", "barbell-rdl", "weighted-pushup", "weighted-pullup", "weighted-dip", "weighted-squat" /* V27: unlocked by owning the equipment */)
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

    /** V07 (C-A): the stretch library covers hips, hamstrings, quads, lats, shoulders, wrists, spine, calves and ankles. */
    @Test fun stretchLibraryCoversTheAreasWithCluesAndCautions() {
        val stretches = catalog.variations.filter { it.kind == Kind.STRETCH }
        val areas = stretches.flatMap { it.stretchAreas }.toSet()
        for (a in listOf(StretchArea.HIP, StretchArea.HAMSTRING, StretchArea.QUAD, StretchArea.BACK, StretchArea.CHEST, StretchArea.SHOULDER, StretchArea.WRIST, StretchArea.CALF)) assertTrue("no stretch for $a", a in areas)
        for (id in listOf("stretch-pigeon", "stretch-90-90", "stretch-frog", "stretch-quad-couch", "stretch-quad-standing", "stretch-hamstring-standing",
            "stretch-lat-wall", "stretch-sleeper", "stretch-wrist-flexor", "stretch-wrist-extensor", "stretch-thoracic")) {
            val v = catalog.variation(id) ?: error("missing $id")
            assertTrue(id, v.defaultSeconds != null && v.stretchAreas.isNotEmpty() && v.instructions.size >= 3)
            assertTrue("$id needs cues", v.formCues.isNotEmpty())
            assertTrue("$id needs its own caution on top of the general one", v.cautions.size >= 2 || id.contains("standing") || id == "stretch-lat-wall")
            assertTrue("$id needs primary muscles", v.primaryMuscles.isNotEmpty())
        }
        assertTrue(stretches.filter { it.unilateral }.all { it.defaultSeconds != null })
        assertTrue(catalog.variation("stretch-wrist-flexor")!!.stretchAreas == setOf(StretchArea.WRIST))
    }

    @Test fun rrChainsAreConnected() {   // V21b: the r/bodyweightfitness paths from docs/research/rr-live-check.md
        fun chain(from: String): List<String> { val out = mutableListOf(from); var c = from
            while (true) { c = catalog.policyForVariation(c)!!.nextVariationIds.firstOrNull() ?: break; out += c }; return out }
        assertEquals(listOf("dip-support-hold", "dip-negative", "dip-parallel"), chain("dip-support-hold"))
        assertEquals(listOf("rdl-bodyweight", "single-leg-deadlift", "nordic-negative-banded", "nordic-banded", "nordic-curl"), chain("rdl-bodyweight"))
        assertEquals(listOf("slide-negative", "slide-hamstring", "slide-negative-single", "slide-single-leg"), chain("slide-negative"))
        assertEquals(setOf("nordic-negative-banded", "slide-negative"), catalog.policies.first { it.variationId == "single-leg-deadlift" }.nextVariationIds.toSet())
    }

    @Test fun rrItemsNeedTheirEquipmentAndAreTagged() {
        fun needs(id: String) = catalog.variation(id)!!.equipmentAlternatives.flatMap { it.needs }.map { it.equipmentId }.toSet()
        assertEquals(setOf("dip-support"), needs("dip-negative"))
        assertEquals(setOf("foot-anchor"), needs("nordic-curl"))
        assertEquals(setOf("foot-anchor", "resistance-band"), needs("nordic-banded"))
        assertEquals(setOf("resistance-band"), needs("pallof-press"))
        assertTrue(needs("slide-hamstring").isEmpty() && needs("rdl-bodyweight").isEmpty() && needs("reverse-hyperextension").isEmpty())
        for (id in listOf("dip-parallel", "nordic-curl", "slide-single-leg", "pallof-press", "plank-shoulder-tap", "walking-lunge", "warmup-arch-hang"))
            assertTrue(id, "r/bodyweightfitness wiki, live check 2026-10-09" in catalog.variation(id)!!.sourceIds)
        // no bench dips: the dip chain is parallel bars, two chairs or a counter corner
        assertTrue(catalog.variations.none { "bench dip" in it.name.lowercase() })
    }

    @Test fun warmUpItemsAreThirtySecondMobilityBlocks() {
        val ids = listOf("warmup-shoulder-band", "warmup-shoulder-towel", "warmup-squat-sky-reach", "warmup-wrist-prep", "warmup-dead-bug", "warmup-arch-hang", "warmup-support-hold")
        for (id in ids) { val v = catalog.variation(id)!!; assertEquals(id, Kind.MOBILITY, v.kind); assertEquals(id, 30, v.defaultSeconds) }
    }

    @Test fun v25FillsTheReviewGaps() {   // C-E
        fun next(id: String) = catalog.policyForVariation(id)!!.nextVariationIds
        fun prereq(id: String) = catalog.policyForVariation(id)!!.prerequisiteRule.allOf.map { g -> g.mapNotNull { it.variationTierMet?.variationId } }
        // pull-up path without a band (wiki main path); the band path stays an alternative way to the full pull-up
        assertEquals(listOf("arch-hang"), next("scapular-pull")); assertEquals(listOf("pullup-negative"), next("arch-hang")); assertEquals(listOf("pullup-full"), next("pullup-negative"))
        assertEquals(listOf(listOf("pullup-band-assisted", "pullup-negative")), prereq("pullup-full"))
        for (id in listOf("scapular-pull", "arch-hang", "pullup-negative")) assertEquals(id, setOf("pullup-bar"), catalog.variation(id)!!.equipmentAlternatives.flatMap { it.needs }.map { it.equipmentId }.toSet())
        // tuck L-sit sits between the hollow hold and the floor L-sit
        assertEquals(listOf("l-sit-tuck"), next("hollow-hold")); assertEquals(listOf("l-sit-floor"), next("l-sit-tuck")); assertEquals(listOf(listOf("l-sit-tuck")), prereq("l-sit-floor"))
        // the pistol path and the shrimp path are alternatives after the Bulgarian split squat (wiki), the pistol path first
        assertEquals(listOf("squat-pistol-assisted", "squat-shrimp"), next("split-squat-bulgarian")); assertEquals(listOf(listOf("split-squat-bulgarian")), prereq("squat-pistol-assisted")); assertTrue(next("squat-shrimp").isEmpty())
        // a middle step before the full back bridge
        assertEquals(listOf("bridge-incline"), next("bridge-single-leg")); assertEquals(listOf("bridge-back"), next("bridge-incline")); assertEquals(listOf(listOf("bridge-incline")), prereq("bridge-back"))
    }
}
