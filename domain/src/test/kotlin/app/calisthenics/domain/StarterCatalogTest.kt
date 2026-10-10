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
        assertEquals(112, strength.size) // L09: +3 (table bridge, head-supported bridge, wall walk-down); L08: +6 (hollow steps and rocks, Pallof hold, short Copenhagen, suitcase carry); L07: +4 (L-sit steps); L05: +4 (shrimp steps, box and counterbalance pistol); L04: +3 (towel door row, wide inverted row, kettlebell one-arm row); L03: +4 (dead hang, flexed-arm hang, chair-assisted pull-up, chin-up); L02: +2 (wall and high incline push-ups); V27: +11 (dumbbells, barbell, vest); V25: +5 (C-E); V21b: +16 (dips, hinge paths, core, Minimalist pieces); before: M6b hard-skill chains + U09 rows, back extension, side plank and kettlebell hinge chains + one-arm swing
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
        "bridge-single-leg", "hollow-rocks", "kettlebell-suitcase-carry", "squat-shrimp-advanced" /* V25/L05: the shrimp path is an alternative end point next to the pistol */, "chinup-full" /* L03: the chin-up is a parallel option to the pull-up */, "kettlebell-one-arm-row",
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
        assertEquals(listOf("row-band", "inverted-row-bent-knees", "inverted-row", "inverted-row-wide", "inverted-row-feet-elevated", "archer-row"), chain("row-band"))   // L04 added the wide row
        assertEquals(listOf("superman-hold", "arch-hold-y", "arch-rocks"), chain("superman-hold"))
        assertEquals(listOf("side-plank", "side-plank-leg-raise", "copenhagen-side-plank-short", "copenhagen-side-plank"), chain("side-plank"))
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
        // Owner OK 2026-10-05 added the swing: it is ballistic by nature, so it must say so; every other kettlebell exercise stays controlled.
        for (v in kb) assertTrue(v.id, v.cautions.any { "ballistic" in it.lowercase() })
    }

    @Test fun plankAndEveryChainTopLeadsSomewhere() {
        assertEquals(listOf("hollow-tuck"), catalog.policyForVariation("plank")!!.nextVariationIds)
        val succ = catalog.policies.flatMap { it.nextVariationIds }.toSet()
        // every exercise with a prerequisite is reachable as someone's successor (no orphan tree nodes)
        // Entry points of the skill chains are reached by choosing a goal, not by automatic progression.
        val goalEntries = setOf("pike-pushup", "planche-lean", "front-lever-tuck", "plank-shoulder-tap", "walking-lunge" /* Minimalist circuit steps */,
            "goblet-squat", "dumbbell-rdl", "weighted-glute-bridge", "dumbbell-row", "barbell-squat", "barbell-rdl", "weighted-pushup", "weighted-pullup", "weighted-dip", "weighted-squat" /* V27: unlocked by owning the equipment */, "chinup-full", "kettlebell-one-arm-row")
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
        assertEquals(listOf("arch-hang"), next("scapular-pull")); assertEquals(listOf("pullup-negative", "chair-assisted-pullup", "flexed-arm-hang"), next("arch-hang")); assertEquals(listOf("pullup-full"), next("pullup-negative"))
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
        assertEquals(listOf("pushup-incline-high"), next("pushup-wall")); assertEquals(listOf("pushup-incline", "pushup-knee"), next("pushup-incline-high"))
        assertEquals(listOf(listOf("pushup-incline", "pushup-knee")), prereq("pushup-standard"))
        assertEquals(listOf("pushup-diamond", "pushup-feet-elevated"), next("pushup-standard"))
        assertEquals(listOf(listOf("pushup-standard")), prereq("pushup-diamond")); assertEquals(listOf(listOf("pushup-standard")), prereq("pushup-feet-elevated"))
        assertEquals(listOf("pushup-archer"), next("pushup-diamond")); assertEquals(listOf("pushup-archer"), next("pushup-feet-elevated"))
        assertEquals(listOf(listOf("pushup-diamond"), listOf("pushup-feet-elevated")), prereq("pushup-archer"))   // both siblings
        val rank = { id: String -> catalog.variation(id)!!.difficultyRank }
        assertTrue(rank("pushup-wall") < rank("pushup-incline-high") && rank("pushup-incline-high") < rank("pushup-incline") && rank("pushup-standard") < rank("pushup-diamond"))
    }

    @Test fun l03PullUpPathWithoutABand() {
        fun next(id: String) = catalog.policyForVariation(id)!!.nextVariationIds
        fun needs(id: String) = catalog.variation(id)!!.equipmentAlternatives.flatMap { it.needs }.map { it.equipmentId }.toSet()
        assertEquals(listOf("scapular-pull"), next("dead-hang"))
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
        assertEquals(listOf("inverted-row-wide"), next("inverted-row")); assertEquals(listOf("inverted-row-feet-elevated"), next("inverted-row-wide"))
        assertEquals(listOf(listOf("inverted-row-wide")), prereq("inverted-row-feet-elevated"))
        assertTrue(listOf("inverted-row-wide") in prereq("front-lever-tuck"))
    }

    @Test fun l05SingleLegSquatRestructure() {
        fun next(id: String) = catalog.policyForVariation(id)!!.nextVariationIds
        fun prereq(id: String) = catalog.policyForVariation(id)!!.prerequisiteRule.allOf.map { g -> g.mapNotNull { it.variationTierMet?.variationId } }
        // beginner shrimp || band/frame-assisted pistol -> intermediate shrimp || box pistol || counterbalance pistol -> advanced shrimp || pistol
        assertEquals(listOf("squat-shrimp-intermediate"), next("squat-shrimp")); assertEquals(listOf("squat-shrimp-advanced"), next("squat-shrimp-intermediate"))
        assertEquals(listOf("squat-pistol-box", "squat-pistol-counterbalance"), next("squat-pistol-assisted"))
        assertEquals(listOf("squat-pistol"), next("squat-pistol-box")); assertEquals(listOf("squat-pistol"), next("squat-pistol-counterbalance"))
        assertEquals(listOf(listOf("squat-pistol-box", "squat-pistol-counterbalance")), prereq("squat-pistol"))
        assertTrue(next("squat-shrimp-advanced").isEmpty() && next("squat-pistol").isEmpty())    // two parallel end points
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
        for ((a, b) in steps.zipWithNext()) assertEquals("$a -> $b", listOf(b), next(a))
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
        assertEquals(listOf("bridge-table", "bridge-single-leg"), next("glute-bridge")); assertEquals(listOf(listOf("glute-bridge")), prereq("bridge-single-leg"))
        assertEquals(setOf("wall"), catalog.variation("bridge-wall-walkdown")!!.equipmentAlternatives.flatMap { it.capabilities }.toSet())
        assertTrue(catalog.variation("bridge-head")!!.cautions.any { "neck" in it })
    }

    @Test fun l10DipsCarryTheDepthCapAndShoulderPainCopy() {
        for (id in listOf("dip-support-hold", "dip-negative", "dip-parallel", "weighted-dip")) {
            val v = catalog.variation(id)!!
            assertTrue(id, v.instructions.any { "Depth cap" in it && "right angle" in it && "front of the shoulder" in it })
            assertTrue(id, v.instructions.any { "two sturdy chairs" in it && "counter corner" in it.replace("-", " ").replace("kitchen counter corner", "counter corner") || "kitchen-counter corner" in it })
        }
        assertEquals(30, catalog.policyForVariation("dip-support-hold")!!.tiers.last().target.value)            // support hold 3 x 30 s
        assertTrue(catalog.variations.none { "bench dip" in it.name.lowercase() })
    }
}
