package app.calisthenics.domain

import app.calisthenics.domain.content.parseCatalog
import app.calisthenics.domain.content.validateCatalog
import app.calisthenics.domain.load.isLoaded
import app.calisthenics.domain.load.loadEquipmentId
import app.calisthenics.domain.model.*
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Test
import kotlinx.serialization.json.*

/** Validates the real bundled starter catalog (content/starter/catalog.json). */
class StarterCatalogTest {
    private val root = File(System.getProperty("repo.root") ?: ".")
    private val catalog: Catalog by lazy { parseCatalog(File(root, "content/starter/catalog.json").readText()) }
    /** Contract input defines the imported IDs independently of the generated catalog. */
    private val contractExercises by lazy { Json.parseToJsonElement(File(root, "docs/23-new-exercises.json").readText()).jsonObject.getValue("exercises").jsonArray.map { it.jsonObject } }
    private val r8Ids: Set<String> by lazy { contractExercises.map { it.getValue("id").jsonPrimitive.content }.toSet() }
    private val legacyVariations by lazy { catalog.variations.filter { it.id !in r8Ids } }
    private fun assertLegacySuccessors(id: String, expected: List<String>) {
        val actual = catalog.policyForVariation(id)!!.nextVariationIds
        assertEquals("$id legacy successor prefix", expected, actual.take(expected.size))
        assertTrue("$id contains a non-contract appended child", actual.drop(expected.size).all { it in r8Ids })
    }
    private fun assertContractExtensions(id: String, legacyPrefix: List<String>) {
        val expected = contractExercises.filter { it["insertAfter"]?.jsonPrimitive?.content == id }
            .map { it.getValue("id").jsonPrimitive.content }
        assertEquals("$id independently declared R8 children", expected,
            catalog.policyForVariation(id)!!.nextVariationIds.drop(legacyPrefix.size))
    }

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
        assertEquals(119, legacyVariations.count { it.kind == Kind.REPS || it.kind == Kind.HOLD })
        assertEquals(35, legacyVariations.count { it.kind == Kind.STRETCH || it.kind == Kind.MOBILITY })
        assertEquals(379, r8Ids.size)
        assertEquals(475, strength.size)
        assertEquals(356, strength.count { it.id in r8Ids })
        assertEquals(44, stretches.count { it.kind == Kind.STRETCH })
        val areas = legacyVariations.filter { it.kind == Kind.REPS || it.kind == Kind.HOLD }.flatMap { it.areas }.toSet()
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
        val approvedPlateaus = setOf(
            "pushup-pop-up-incline", "pushup-kneeling-push-off", "pushup-pop-up", "pushup-pop-up-wide-close", "pushup-clap", "pushup-chest-strike",
            "jump-squat", "jump-vertical", "jump-butt-kick", "jump-tuck-slap", "jump-tuck", "jump-tuck-catch", "jump-split", "jump-split-cycle",
            "jump-block", "jump-lateral", "jump-broad", "jump-broad-consecutive", "box-push-off-single", "box-jump", "box-jump-rebound", "depth-jump",
        )
        for (p in catalog.policies) {
            assertEquals(p.id, listOf(1, 2, 3, 4, 5), p.tiers.map { it.index })
            val values = p.tiers.map { it.target.value }
            assertTrue(p.id, values.zipWithNext().all { (a, b) -> if (p.variationId in approvedPlateaus) b >= a else b > a })
            if (p.variationId in approvedPlateaus) assertEquals(p.variationId, listOf(1, 2, 3, 3, 3), values)
        }
        val caps = catalog.policies.filter { it.maxRepsPerSet != null }.associate { it.variationId to it.maxRepsPerSet }
        assertEquals(setOf("pushup-pop-up-incline", "pushup-kneeling-push-off", "pushup-pop-up", "pushup-pop-up-wide-close", "pushup-clap", "pushup-chest-strike"), caps.keys)
        assertTrue(caps.values.all { it == 3 })
    }

    /** Only these may end a chain; every other exercise must lead somewhere (U09, F15). */
    private val explicitTops = setOf("jefferson-curl", "german-hang", "pushup-one-arm", "hspu-wall", "muscle-up-bar", "squat-pistol", "bridge-back", "v-sit-floor", "v-up",
        "front-lever-straddle", "planche-adv-tuck", "archer-row", "arch-rocks", "copenhagen-side-plank", "kettlebell-swing-one-arm",
        // V21b: ends of the RR paths and single exercises of the ready-made routines
        "dip-parallel", "nordic-curl", "slide-single-leg", "pallof-press", "reverse-hyperextension", "plank-shoulder-tap", "walking-lunge",
        "calf-raise-single-loaded", "tibialis-raise-single", "bridge-single-leg", "hollow-rocks", "kettlebell-suitcase-carry", "squat-shrimp-advanced" /* V25/L05: the shrimp path is an alternative end point next to the pistol */, "chinup-full" /* L03: the chin-up is a parallel option to the pull-up */, "kettlebell-one-arm-row",
        // V27: extra-equipment exercises (dumbbells, barbell, vest)
        "goblet-squat", "dumbbell-single-leg-rdl", "weighted-glute-bridge", "dumbbell-row", "barbell-squat", "barbell-rdl", "weighted-pushup", "weighted-pullup", "weighted-dip", "weighted-squat")

    @Test fun noDeadEndsExceptExplicitTops() {
        val r8Parents = contractExercises.mapNotNull { it["insertAfter"]?.jsonPrimitive?.content }.toSet()
        // Preserve the frozen pre-R8 terminal invariant. R8 intentionally adds reviewed branches
        // below some former tops; those parents cease being terminal without changing old paths.
        val legacyDeadEnds = catalog.policies.filter { it.variationId !in r8Ids && it.nextVariationIds.isEmpty() }.map { it.variationId }.toSet()
        assertEquals("legacy terminal exercises", explicitTops - r8Parents, legacyDeadEnds)
        val r8StrengthIds = contractExercises.filter { it.getValue("kind").jsonPrimitive.content in setOf("REPS", "HOLD") }
            .map { it.getValue("id").jsonPrimitive.content }.toSet()
        val importedChildren = contractExercises.filter { it.getValue("kind").jsonPrimitive.content in setOf("REPS", "HOLD") }
            .mapNotNull { it["insertAfter"]?.jsonPrimitive?.content }.toSet()
        val expectedR8Tops = r8StrengthIds - importedChildren
        val actualR8Tops = catalog.policies.filter { it.variationId in r8Ids && it.nextVariationIds.isEmpty() }.map { it.variationId }.toSet()
        assertEquals("R8 terminal exercises from the independent contract", expectedR8Tops, actualR8Tops)
        for (id in listOf("pushup-chest-strike", "front-lever", "planche-full", "ring-muscle-up", "depth-jump", "flag-press")) assertTrue("expected terminal: $id", id in actualR8Tops)
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
        assertEquals(listOf("row-band", "inverted-row-bent-knees", "inverted-row", "inverted-row-wide", "inverted-row-feet-elevated", "archer-row"), chain("row-band").take(6))
        assertEquals(listOf("front-lever-row-tuck", "front-lever-row-adv-tuck", "ice-cream-maker"), chain("row-band").drop(6))
        assertEquals(listOf("superman-hold", "arch-hold-y", "arch-rocks"), chain("superman-hold"))
        val copenhagen = chain("side-plank")
        assertEquals(listOf("side-plank", "side-plank-leg-raise", "copenhagen-side-plank-short", "copenhagen-side-plank"), copenhagen.take(4))
        assertEquals(listOf("copenhagen-adduction"), copenhagen.drop(4))
        assertContractExtensions("copenhagen-side-plank", emptyList())
        assertEquals(listOf("kettlebell-deadlift", "kettlebell-single-leg-rdl", "kettlebell-swing", "kettlebell-swing-one-arm"), chain("kettlebell-deadlift"))
        assertEquals(listOf("dead-bug", "plank", "hollow-tuck", "hollow-one-leg", "hollow-hold"), chain("dead-bug").take(5)) // L08: the dead bug comes before the plank, the hollow hold has two easier steps
        for (id in listOf("inverted-row-bent-knees", "inverted-row", "inverted-row-feet-elevated", "archer-row"))
            assertTrue(id, catalog.variation(id)!!.equipmentAlternatives.single().needs.single().equipmentId == "low-bar")
        assertTrue(catalog.variation("copenhagen-side-plank")!!.equipmentAlternatives.single().needs.single().suitability == setOf("stable"))
        assertTrue(catalog.variation("kettlebell-swing")!!.cautions.any { "Ballistic" in it })
    }

    @Test fun noBallisticKettlebellWork() {
        val kb = catalog.variations.filter { v -> v.equipmentAlternatives.any { s -> s.needs.any { it.equipmentId == "kettlebell" } } }
        assertTrue(kb.isNotEmpty())
        // Preserve the original controlled kettlebell guarantees for the old loaded exercises.
        for (v in kb.filter { it.id !in r8Ids }) assertTrue(v.id, v.cautions.any { "ballistic" in it.lowercase() })
    }

    @Test fun plankAndEveryChainTopLeadsSomewhere() {
        assertLegacySuccessors("plank", listOf("hollow-tuck"))
        val succ = catalog.policies.flatMap { it.nextVariationIds }.toSet()
        // every exercise with a prerequisite is reachable as someone's successor (no orphan tree nodes)
        // Entry points of the skill chains are reached by choosing a goal, not by automatic progression.
        val goalEntries = setOf("pike-pushup", "planche-lean", "front-lever-tuck", "plank-shoulder-tap", "walking-lunge" /* Minimalist circuit steps */,
            "goblet-squat", "dumbbell-rdl", "weighted-glute-bridge", "dumbbell-row", "barbell-squat", "barbell-rdl", "weighted-pushup", "weighted-pullup", "weighted-dip", "weighted-squat" /* V27: unlocked by owning the equipment */, "chinup-full", "kettlebell-one-arm-row")
        val independentlyGatedRoots = contractExercises.filter { it["insertAfter"] == JsonNull && it["alsoRequires"] is JsonArray }
            .map { it.getValue("id").jsonPrimitive.content }.toSet()
        for (id in independentlyGatedRoots) {
            val expected = contractExercises.single { it.getValue("id").jsonPrimitive.content == id }
                .getValue("alsoRequires").jsonArray.map { it.jsonPrimitive.content }.toSet()
            val actual = catalog.policyForVariation(id)!!.prerequisiteRule.allOf.flatMap { group -> group.mapNotNull { it.variationTierMet?.variationId } }.toSet()
            assertEquals("gated root $id", expected, actual)
        }
        for (p in catalog.policies.filter { it.prerequisiteRule.allOf.isNotEmpty() && it.variationId !in goalEntries && it.variationId !in independentlyGatedRoots })
            assertTrue("${p.variationId} unreachable", p.variationId in succ)
    }

    @Test fun hardSkillChainsExistInOrder() {
        fun chain(from: String): List<String> { val out = mutableListOf(from); var c = from
            while (true) { c = catalog.policyForVariation(c)!!.nextVariationIds.firstOrNull() ?: break; out += c }; return out }
        assertEquals(listOf("pike-pushup", "pike-pushup-elevated", "wall-handstand-hold", "hspu-wall-negative", "hspu-wall"), chain("pike-pushup").take(5))
        assertEquals(setOf("hspu-wall-deficit", "hspu-free-head", "hspu-free"), chain("pike-pushup").drop(5).toSet())
        assertEquals("muscle-up-bar", chain("pullup-band-assisted").firstOrNull { it == "muscle-up-bar" })
        assertEquals("squat-pistol", chain("squat-air").last())
        assertEquals("v-sit-floor", chain("plank").last().let { chain("hollow-hold").last() })
        assertEquals(listOf("planche-lean", "planche-tuck", "planche-adv-tuck"), chain("planche-lean").take(3))
        assertEquals(listOf("planche-straddle", "planche-full"), chain("planche-lean").drop(3))
        assertEquals(listOf("front-lever-tuck", "front-lever-adv-tuck", "front-lever-straddle"), chain("front-lever-tuck").take(3))
        assertEquals(listOf("front-lever-one-leg", "front-lever"), chain("front-lever-tuck").drop(3))
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
        assertEquals(listOf("dip-support-hold", "dip-negative", "dip-parallel"), chain("dip-support-hold").take(3))
        assertEquals(listOf("dip-straight-bar"), catalog.policyForVariation("dip-parallel")!!.nextVariationIds.take(1))
        assertEquals(setOf("dip-l-sit", "dip-russian"), catalog.policyForVariation("dip-parallel")!!.nextVariationIds.drop(1).toSet())
        assertEquals(listOf("rdl-bodyweight", "single-leg-deadlift", "nordic-negative-banded", "nordic-banded", "nordic-curl"), chain("rdl-bodyweight").take(5))
        assertEquals(listOf("harop-curl"), chain("rdl-bodyweight").drop(5))
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
        val benchDips = listOf("dip-bench-bent-knee", "dip-bench-straight-leg", "dip-bench-feet-elevated").map { catalog.variation(it)!! }
        assertTrue(benchDips.all { it.reviewState == ReviewState.DRAFT && it.cautions.isNotEmpty() })
    }

    @Test fun warmUpItemsAreThirtySecondMobilityBlocks() {
        val ids = listOf("warmup-shoulder-band", "warmup-shoulder-towel", "warmup-squat-sky-reach", "warmup-wrist-prep", "warmup-dead-bug", "warmup-arch-hang", "warmup-support-hold")
        for (id in ids) { val v = catalog.variation(id)!!; assertEquals(id, Kind.MOBILITY, v.kind); assertEquals(id, 30, v.defaultSeconds) }
    }

    @Test fun v25FillsTheReviewGaps() {   // C-E
        fun next(id: String) = catalog.policyForVariation(id)!!.nextVariationIds
        fun prereq(id: String) = catalog.policyForVariation(id)!!.prerequisiteRule.allOf.map { g -> g.mapNotNull { it.variationTierMet?.variationId } }
        // pull-up path without a band (wiki main path); the band path stays an alternative way to the full pull-up
        assertLegacySuccessors("scapular-pull", listOf("arch-hang")); assertLegacySuccessors("arch-hang", listOf("pullup-negative", "chair-assisted-pullup", "flexed-arm-hang")); assertLegacySuccessors("pullup-negative", listOf("pullup-full"))
        assertEquals(listOf("pullup-jumping"), next("scapular-pull").drop(1))
        assertEquals(listOf(listOf("pullup-band-assisted", "pullup-negative", "chair-assisted-pullup", "flexed-arm-hang")), prereq("pullup-full"))
        for (id in listOf("scapular-pull", "arch-hang", "pullup-negative")) assertEquals(id, setOf("pullup-bar"), catalog.variation(id)!!.equipmentAlternatives.flatMap { it.needs }.map { it.equipmentId }.toSet())
        // tuck L-sit sits between the hollow hold and the floor L-sit
        assertEquals(listOf("l-sit-foot-supported", "hollow-rocks"), next("hollow-hold")); assertEquals(listOf("l-sit-advanced-tuck"), next("l-sit-tuck")); assertEquals(listOf(listOf("l-sit-one-leg")), prereq("l-sit-floor"))
        // the pistol path and the shrimp path are alternatives after the Bulgarian split squat (wiki), the pistol path first
        assertEquals(listOf("squat-pistol-assisted", "squat-shrimp"), next("split-squat-bulgarian")); assertEquals(listOf(listOf("split-squat-bulgarian")), prereq("squat-pistol-assisted")); assertEquals(listOf("squat-shrimp-intermediate"), next("squat-shrimp"))
        // a middle step before the full back bridge
        assertEquals(listOf("bridge-head"), next("bridge-incline")); assertEquals(listOf(listOf("bridge-wall-walkdown")), prereq("bridge-back"))   // L09 extended the ladder
    }

    @Test fun l02PushUpChainFollowsTheLoadCurve() {   // Ebben 2011: wall < high incline < knee / low incline < full < diamond / 30 cm decline < archer < one-arm
        fun next(id: String) = catalog.policyForVariation(id)!!.nextVariationIds
        fun prereq(id: String) = catalog.policyForVariation(id)!!.prerequisiteRule.allOf.map { g -> g.mapNotNull { it.variationTierMet?.variationId } }
        assertLegacySuccessors("pushup-wall", listOf("pushup-incline-high")); assertLegacySuccessors("pushup-incline-high", listOf("pushup-incline", "pushup-knee"))
        assertEquals(listOf(listOf("pushup-incline", "pushup-knee")), prereq("pushup-standard"))
        assertLegacySuccessors("pushup-standard", listOf("pushup-diamond", "pushup-feet-elevated"))
        assertContractExtensions("pushup-standard", listOf("pushup-diamond", "pushup-feet-elevated"))
        assertEquals(listOf(listOf("pushup-standard")), prereq("pushup-diamond")); assertEquals(listOf(listOf("pushup-standard")), prereq("pushup-feet-elevated"))
        assertLegacySuccessors("pushup-diamond", listOf("pushup-archer")); assertContractExtensions("pushup-diamond", listOf("pushup-archer"))
        assertEquals(listOf("pushup-archer"), next("pushup-feet-elevated"))
        assertEquals(listOf(listOf("pushup-diamond"), listOf("pushup-feet-elevated")), prereq("pushup-archer"))   // both siblings
        val rank = { id: String -> catalog.variation(id)!!.difficultyRank }
        assertTrue(rank("pushup-wall") < rank("pushup-incline-high") && rank("pushup-incline-high") < rank("pushup-incline") && rank("pushup-standard") < rank("pushup-diamond"))
    }

    @Test fun l03PullUpPathWithoutABand() {
        fun next(id: String) = catalog.policyForVariation(id)!!.nextVariationIds
        fun needs(id: String) = catalog.variation(id)!!.equipmentAlternatives.flatMap { it.needs }.map { it.equipmentId }.toSet()
        assertEquals(listOf("scapular-pull", "german-hang"), next("dead-hang").take(2))   // R7-T12: the German hang follows as a second successor
        assertTrue(next("dead-hang").drop(2).all { it in r8Ids })
        assertEquals(listOf("pullup-full"), next("flexed-arm-hang")); assertEquals(listOf("pullup-full"), next("chair-assisted-pullup"))
        assertEquals(setOf("pullup-bar", "chair"), needs("chair-assisted-pullup")); assertEquals(setOf("pullup-bar"), needs("dead-hang")); assertEquals(Kind.HOLD, catalog.variation("flexed-arm-hang")!!.kind)
        // chin-up and pull-up are parallel: same way in, neither leads to the other
        assertEquals(catalog.policyForVariation("pullup-full")!!.prerequisiteRule, catalog.policyForVariation("chinup-full")!!.prerequisiteRule)
        assertTrue(next("chinup-full").isEmpty() && "chinup-full" !in next("pullup-full"))
        // the questionnaire's pull ladder has a hold step between the assisted and the full pull-up (placement for people who can hang with the chin over the bar)
        assertEquals(listOf("row-band", "pullup-band-assisted", "flexed-arm-hang", "pullup-full"), catalog.onboardingFamilies.first { it.id == "pull" }.ladder)
    }

    @Test fun l04RowsWithoutALowBar() {
        fun next(id: String) = catalog.policyForVariation(id)!!.nextVariationIds
        fun prereq(id: String) = catalog.policyForVariation(id)!!.prerequisiteRule.allOf.map { g -> g.mapNotNull { it.variationTierMet?.variationId } }
        fun needs(id: String) = catalog.variation(id)!!.equipmentAlternatives.flatMap { it.needs }.map { it.equipmentId }.toSet()
        // a door-and-towel row needs no equipment from the profile and carries the safety copy; the inverted rows need the low bar (or a sturdy table)
        assertTrue(needs("towel-door-row").isEmpty()); assertTrue(catalog.variation("towel-door-row")!!.cautions.any { "knot" in it && "door" in it })
        assertEquals(setOf("low-bar"), needs("inverted-row")); assertEquals(setOf("low-bar"), needs("inverted-row-wide")); assertEquals(setOf("kettlebell"), needs("kettlebell-one-arm-row"))
        assertEquals(listOf(listOf("row-band", "towel-door-row")), prereq("inverted-row-bent-knees"))
        // inverted row -> wide row -> feet elevated -> archer; the front lever needs 3 x 8 wide rows (tier 4)
        assertLegacySuccessors("inverted-row", listOf("inverted-row-wide")); assertLegacySuccessors("inverted-row-wide", listOf("inverted-row-feet-elevated"))
        assertEquals(listOf(listOf("inverted-row-wide")), prereq("inverted-row-feet-elevated"))
        assertTrue(listOf("inverted-row-wide") in prereq("front-lever-tuck"))
    }

    @Test fun l05SingleLegSquatRestructure() {
        fun next(id: String) = catalog.policyForVariation(id)!!.nextVariationIds
        fun prereq(id: String) = catalog.policyForVariation(id)!!.prerequisiteRule.allOf.map { g -> g.mapNotNull { it.variationTierMet?.variationId } }
        // beginner shrimp || band/frame-assisted pistol -> intermediate shrimp || box pistol || counterbalance pistol -> advanced shrimp || pistol
        assertEquals(listOf("squat-shrimp-intermediate"), next("squat-shrimp")); assertEquals(listOf("squat-shrimp-advanced"), next("squat-shrimp-intermediate"))
        assertLegacySuccessors("squat-pistol-assisted", listOf("squat-pistol-box", "squat-pistol-counterbalance"))
        assertLegacySuccessors("squat-pistol-box", listOf("squat-pistol")); assertContractExtensions("squat-pistol-box", listOf("squat-pistol"))
        assertLegacySuccessors("squat-pistol-counterbalance", listOf("squat-pistol"))
        assertEquals(listOf(listOf("squat-pistol-box", "squat-pistol-counterbalance")), prereq("squat-pistol"))
        assertContractExtensions("squat-shrimp-advanced", emptyList()) // R8 adds the deficit shrimp.
        assertTrue(next("squat-pistol").isEmpty())
        // the ankle check is information, in every single-leg step
        for (id in listOf("squat-shrimp", "squat-shrimp-intermediate", "squat-shrimp-advanced", "squat-pistol-assisted", "squat-pistol-box", "squat-pistol-counterbalance"))
            assertTrue(id, catalog.variation(id)!!.instructions.any { "Ankle check" in it && "information only" in it })
        assertEquals(setOf("chair"), catalog.variation("squat-pistol-box")!!.equipmentAlternatives.flatMap { it.needs }.map { it.equipmentId }.toSet())
    }


    @Test fun l06HingeHasFourPathsWithSafeNordicDefaults() {
        fun next(id: String) = catalog.policyForVariation(id)!!.nextVariationIds
        fun tiers(id: String) = catalog.policyForVariation(id)!!.tiers.map { it.target.value }
        // the four paths: bridge, single-leg deadlift, sliding leg curl (floor slides), Nordic; and the extension exercise on a table or bed edge
        assertEquals(setOf("nordic-negative-banded", "slide-negative"), next("single-leg-deadlift").toSet())
        assertEquals("bridge", catalog.variation("glute-bridge")!!.familyId); assertEquals("slide", catalog.variation("slide-hamstring")!!.familyId); assertEquals("nordic", catalog.variation("nordic-curl")!!.familyId)
        assertTrue(catalog.variation("reverse-hyperextension")!!.instructions.any { "table" in it && "bed" in it })
        // Nordic: band from the bar for the banded steps, a couch warning on every Nordic step, low volume (the top tier never above 6 reps)
        for (id in listOf("nordic-negative-banded", "nordic-banded", "nordic-curl")) {
            val v = catalog.variation(id)!!
            assertTrue(id, v.instructions.any { "Couch anchor" in it }); assertTrue(id, tiers(id).max() <= 6)
        }
        for (id in listOf("nordic-negative-banded", "nordic-banded")) assertTrue(id, catalog.variation(id)!!.instructions.any { "Band from the bar" in it && "pull-up bar" in it })
    }

    @Test fun l07LSitStepsUseTheAccumulatedMinuteProfile() {
        val steps = listOf("l-sit-foot-supported", "l-sit-one-foot", "l-sit-tuck", "l-sit-advanced-tuck", "l-sit-one-leg", "l-sit-floor", "v-sit-floor")
        fun next(id: String) = catalog.policyForVariation(id)!!.nextVariationIds
        for ((a, b) in steps.zipWithNext()) assertLegacySuccessors(a, listOf(b))
        for (id in steps.dropLast(1)) {
            val p = catalog.policyForVariation(id)!!
            assertEquals(id, app.calisthenics.domain.model.TierProfile.H60, p.tierProfile); assertEquals(id, listOf(10, 15, 20, 30, 60), p.tiers.map { it.target.value })
            assertEquals(Kind.HOLD, catalog.variation(id)!!.kind); assertTrue(id, p.tierNote!!.contains("accumulated minute"))
        }
    }

    @Test fun l08CorePlanes() {
        fun next(id: String) = catalog.policyForVariation(id)!!.nextVariationIds
        fun prereq(id: String) = catalog.policyForVariation(id)!!.prerequisiteRule.allOf.map { g -> g.mapNotNull { it.variationTierMet?.variationId } }
        assertEquals(listOf(listOf("dead-bug")), prereq("plank"))                                  // dead bug before the plank
        assertEquals(listOf("l-sit-foot-supported", "hollow-rocks"), next("hollow-hold")); assertTrue(next("hollow-rocks").isEmpty())
        assertEquals(listOf("pallof-press"), next("pallof-hold")); assertTrue(catalog.variation("pallof-press")!!.instructions.any { "Stance rule" in it })
        assertEquals(listOf(listOf("copenhagen-side-plank-short")), prereq("copenhagen-side-plank"))
        assertEquals(listOf("superman-hold", "arch-hold-y", "arch-rocks"), listOf("superman-hold").let { var c = it.last(); val out = it.toMutableList(); while (true) { c = next(c).firstOrNull() ?: break; out += c }; out })
        val carry = catalog.variation("kettlebell-suitcase-carry")!!
        assertEquals(setOf("kettlebell"), carry.equipmentAlternatives.flatMap { it.needs }.map { it.equipmentId }.toSet()); assertTrue(carry.unilateral && carry.kind == Kind.HOLD)
    }

    @Test fun l09BackBridgeLadder() {
        fun next(id: String) = catalog.policyForVariation(id)!!.nextVariationIds
        fun prereq(id: String) = catalog.policyForVariation(id)!!.prerequisiteRule.allOf.map { g -> g.mapNotNull { it.variationTierMet?.variationId } }
        // glute bridge -> table bridge -> chair (incline) bridge -> head-supported -> wall walk-down (the middle step) -> full back bridge; the single-leg bridge is a side branch
        var c = "glute-bridge"; val out = mutableListOf(c); while (true) { c = next(c).firstOrNull() ?: break; out += c }
        assertEquals(listOf("glute-bridge", "bridge-table", "bridge-incline", "bridge-head", "bridge-wall-walkdown", "bridge-back"), out)
        assertLegacySuccessors("glute-bridge", listOf("bridge-table", "bridge-single-leg")); assertEquals(listOf(listOf("glute-bridge")), prereq("bridge-single-leg"))
        assertEquals(setOf("wall"), catalog.variation("bridge-wall-walkdown")!!.equipmentAlternatives.flatMap { it.capabilities }.toSet())
        assertTrue(catalog.variation("bridge-head")!!.cautions.any { "neck" in it })
    }

    @Test fun l10DipsCarryTheDepthCapAndShoulderPainCopy() {
        for (id in listOf("dip-support-hold", "dip-negative", "dip-parallel", "weighted-dip")) {
            val v = catalog.variation(id)!!
            assertTrue(id, v.instructions.any { "Depth cap" in it && "right angle" in it && "front of the shoulder" in it })
            assertTrue(id, v.instructions.any { "two sturdy chairs" in it && "counter" in it.lowercase() })
        }
        assertEquals(30, catalog.policyForVariation("dip-support-hold")!!.tiers.last().target.value)            // support hold 3 x 30 s
        val benchDips = listOf("dip-bench-bent-knee", "dip-bench-straight-leg", "dip-bench-feet-elevated").map { catalog.variation(it)!! }
        assertTrue(benchDips.all { it.reviewState == ReviewState.DRAFT && it.cautions.isNotEmpty() })
    }

    @Test fun l11StretchLibraryIsTaggedForBetweenSetsAndCoolDown() {
        val stretches = catalog.variations.filter { it.kind == Kind.STRETCH }
        assertEquals(25, legacyVariations.count { it.kind == Kind.STRETCH })       // frozen pre-R8 library size
        assertEquals(44, stretches.size)
        val cd = stretches.filter { it.stretchUse == StretchUse.COOL_DOWN }.map { it.id }.toSet()
        assertTrue(cd.containsAll(setOf("stretch-pigeon", "stretch-90-90", "stretch-frog", "stretch-quad-couch", "stretch-butterfly", "stretch-pancake", "stretch-cobra", "stretch-sleeper")))
        assertTrue(catalog.variation("stretch-sleeper")!!.optIn)
        // R7-T12: the old rule "between sets: 30 s at most" for every automatic stretch is split: the gentle ones keep 30 s, the deep ones (now also allowed between sets) hold 45-60 s
        assertTrue(stretches.filter { it.stretchUse != StretchUse.COOL_DOWN && !it.optIn }.all { (it.defaultSeconds ?: 30) <= 30 })
        assertTrue(stretches.filter { it.stretchUse == StretchUse.COOL_DOWN && !it.optIn }.all { (it.defaultSeconds ?: 0) in 45..60 })
        // every strength exercise keeps at least one stretch that may go between sets (else the recovery would be passive rest)
        for (v in catalog.variations.filter { it.kind == Kind.REPS || it.kind == Kind.HOLD }) {
            val bs = v.compatibleStretchIds.mapNotNull { catalog.variation(it) }.filter { it.stretchUse != StretchUse.COOL_DOWN && !it.optIn }
            assertTrue(v.id, bs.isNotEmpty())
        }
        // warm-up items (static holds before strength work) are 30 s each
        assertTrue(catalog.warmupTemplate.mapNotNull { catalog.variation(it)?.defaultSeconds }.all { it <= 30 })
        assertTrue(catalog.variations.filter { it.kind == Kind.STRETCH && "stretch-butterfly" == it.id }.single().sourceIds.any { "r/flexibility" in it })
    }

    @Test fun l13CalvesAndTibialisUseTheEnduranceProfileAndTheDumbbellsAsLoad() {
        fun next(id: String) = catalog.policyForVariation(id)!!.nextVariationIds
        assertLegacySuccessors("calf-raise", listOf("calf-raise-single")); assertLegacySuccessors("calf-raise-single", listOf("calf-raise-single-loaded")); assertLegacySuccessors("tibialis-raise", listOf("tibialis-raise-single"))
        for (id in listOf("calf-raise", "calf-raise-single", "calf-raise-single-loaded", "tibialis-raise", "tibialis-raise-single")) {
            val p = catalog.policyForVariation(id)!!
            assertEquals(id, app.calisthenics.domain.model.TierProfile.E, p.tierProfile); assertEquals(id, listOf(15, 18, 20, 22, 25), p.tiers.map { it.target.value })
        }
        val loaded = catalog.variation("calf-raise-single-loaded")!!
        assertEquals("weight", loaded.loadEquipmentId()); assertTrue(loaded.isLoaded())
        assertEquals(setOf("wall"), catalog.variation("tibialis-raise")!!.equipmentAlternatives.flatMap { it.capabilities }.toSet())
    }

    // R7-T12 (D9): Jefferson curl and German hang are usable (not opt-in), DRAFT, with the light-red pay-attention points
    @Test fun r7JeffersonCurlAndGermanHangAreUsableWithRedCautions() {
        for ((id, must) in listOf("jefferson-curl" to listOf("no weight", "back pain", "disc"), "german-hang" to listOf("dead hang", "feet", "shoulders", "shoulder pain"))) {
            val v = catalog.variation(id)!!
            assertEquals(ReviewState.DRAFT, v.reviewState); assertTrue(id, v.cautionStrong); assertFalse(v.optIn)
            assertNotNull(catalog.policyFor(v))
            val text = v.cautions.joinToString(" ").lowercase()
            for (m in must) assertTrue("$id: $m in $text", m in text)
            assertTrue(id, v.cautions.all { it.startsWith("Pay attention to") })
        }
        assertEquals(1, catalog.variations.count { it.id == "jefferson-curl" })
        assertFalse(legacyVariations.filter { it.id != "jefferson-curl" && it.id != "german-hang" }.any { it.cautionStrong })
    }
}
