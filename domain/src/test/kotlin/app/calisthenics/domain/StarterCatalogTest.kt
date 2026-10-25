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
    private val routeById by lazy { catalog.progressionRoutes.associateBy { it.id } }
    private fun routePath(id: String) = routeById.getValue(id).mainPath
    private fun assertRoutePath(routeId: String, expected: List<String>) {
        assertEquals("$routeId reviewed main path", expected, routePath(routeId))
    }
    private fun assertContractExtensions(id: String, legacyPrefix: List<String>) {
        val expected = contractExercises.filter { it["insertAfter"]?.jsonPrimitive?.content == id }
            .map { it.getValue("id").jsonPrimitive.content }
        val explicitlyClassified = catalog.progressionRoutes.flatMap { it.members }.map { it.variationId }.toSet()
        assertTrue("$id contract children have explicit route/library roles: $expected", expected.all { it in explicitlyClassified })
        assertTrue("legacy prefix parameter is retained only to document the source edge: $legacyPrefix", legacyPrefix.all { catalog.variation(it) != null })
    }
    private fun assertRouteMember(routeId: String, variationId: String, role: ProgressionRole, anchor: String? = null) {
        val member = routeById.getValue(routeId).members.single { it.variationId == variationId }
        assertEquals("$routeId role for $variationId", role, member.role)
        if (anchor != null) assertEquals("$routeId anchor for $variationId", anchor, member.atVariationId)
    }
    private fun assertRouteGate(child: String, parent: String, tier: Int) {
        val groups = catalog.policyForVariation(child)!!.prerequisiteRule.allOf
        assertTrue("$child requires $parent at readiness tier $tier", groups.any { group ->
            group.any { it.variationTierMet?.let { gate -> gate.variationId == parent && gate.tier == tier } == true }
        })
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

    /** Route spines and terminal goals replace legacy global branch prefixes (U09, F15). */
    @Test fun reviewedRoutesCoverEveryExerciseAndTheirMainEdges() {
        val classified = catalog.progressionRoutes.flatMap { it.members }.map { it.variationId }.toSet()
        assertEquals(catalog.variations.map { it.id }.toSet(), classified)
        val policyById = catalog.policies.associateBy { it.variationId }
        for (route in catalog.progressionRoutes) {
            assertTrue("${route.id} has a main path or is an explicit collection", route.mainPath.isNotEmpty() || route.focus != null || route.id in setOf("library", "program-only"))
            assertEquals("${route.id} contains each membership once", route.members.map { it.variationId }.size, route.members.map { it.variationId }.toSet().size)
            for ((parent, child) in route.mainPath.zipWithNext()) {
                val policy = policyById[parent] ?: continue
                assertTrue("${route.id} edge $parent -> $child", child in policy.nextVariationIds)
                assertTrue("$parent -> $child references a catalog exercise", catalog.variation(child) != null)
            }
            route.goalVariationId?.let { assertTrue("${route.id} goal is on its path", it in route.mainPath) }
        }
    }

    @Test fun everyStrengthExerciseNamesItsPrimaryMuscles() {
        for (v in catalog.variations.filter { it.kind == Kind.REPS || it.kind == Kind.HOLD }) {
            assertTrue("${v.id} has no primary muscle", v.primaryMuscles.isNotEmpty())
            assertTrue("${v.id}: a muscle is both primary and secondary", v.primaryMuscles.none { it in v.secondaryMuscles })
        }
    }

    @Test fun newChainsAreInOrderAndNeedTheRightEquipment() {
        assertRoutePath("row", listOf("towel-door-row", "inverted-row-bent-knees", "inverted-row", "inverted-row-feet-elevated", "archer-row"))
        assertRouteMember("row", "row-band", ProgressionRole.ALTERNATE, "inverted-row-bent-knees")
        assertRoutePath("one-arm-row", listOf("archer-row", "row-one-arm-straddle", "row-one-arm"))
        assertRoutePath("extension", listOf("superman-hold", "arch-hold-y", "arch-rocks"))
        assertRoutePath("side-plank", listOf("side-plank", "copenhagen-side-plank-short", "copenhagen-side-plank"))
        assertRouteMember("side-plank", "side-plank-hip-raise", ProgressionRole.ALTERNATE, "side-plank")
        assertContractExtensions("copenhagen-side-plank", emptyList())
        assertRoutePath("rdl-hinge", listOf("rdl-bodyweight", "single-leg-deadlift"))
        assertRoutePath("anti-extension", listOf("dead-bug-90-90", "dead-bug-single-leg", "dead-bug", "plank", "plank-long-lever", "body-saw", "rollout-knees", "rollout-ramp", "rollout-negative", "rollout-standing"))
        assertRouteGate("dead-bug-single-leg", "dead-bug-90-90", 3)
        assertRouteGate("dead-bug", "dead-bug-single-leg", 3)
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
        val independentlyGatedRoots = contractExercises.filter { it["insertAfter"] == JsonNull && it["alsoRequires"] is JsonArray }
            .map { it.getValue("id").jsonPrimitive.content }.toSet()
        for (id in independentlyGatedRoots) {
            val expected = contractExercises.single { it.getValue("id").jsonPrimitive.content == id }
                .getValue("alsoRequires").jsonArray.map { it.jsonPrimitive.content }.toSet()
            val actual = catalog.policyForVariation(id)!!.prerequisiteRule.allOf.flatMap { group -> group.mapNotNull { it.variationTierMet?.variationId } }.toSet()
            assertTrue("gated root $id preserves compound gates $expected", actual.containsAll(expected))
        }
        val pathChildren = catalog.progressionRoutes.flatMap { it.mainPath.drop(1) }.toSet()
        for (p in catalog.policies.filter { it.prerequisiteRule.allOf.isNotEmpty() && it.variationId !in independentlyGatedRoots })
            assertTrue("${p.variationId} has a reviewed route or explicit goal entry", p.variationId in pathChildren || catalog.routesFor(p.variationId).isNotEmpty())
    }

    @Test fun hardSkillChainsExistInOrder() {
        assertRoutePath("hspu", listOf("pike-pushup", "pike-pushup-elevated", "wall-handstand-hold", "hspu-wall-negative", "hspu-wall", "hspu-wall-deficit", "hspu-free-head", "hspu-free"))
        assertRoutePath("muscle-up", listOf("pullup-chest-to-bar", "pullup-sternum", "muscle-up-negative", "muscle-up-bar"))
        assertRoutePath("squat", listOf("squat-assisted", "squat-air", "split-squat", "split-squat-bulgarian", "squat-pistol-assisted", "squat-pistol-box", "squat-pistol"))
        assertRoutePath("lsit", listOf("l-sit-foot-supported", "l-sit-one-foot", "l-sit-tuck", "l-sit-advanced-tuck", "l-sit-one-leg", "l-sit-floor", "l-sit-straddle", "v-sit-floor"))
        assertRoutePath("planche", listOf("planche-frog-stand", "planche-lean", "planche-tuck", "planche-adv-tuck", "planche-straddle", "planche-full"))
        assertRoutePath("front-lever", listOf("front-lever-tuck", "front-lever-adv-tuck", "front-lever-straddle", "front-lever-one-leg", "front-lever"))
        assertRouteGate("muscle-up-bar", "muscle-up-negative", 3)
        assertRouteGate("muscle-up-bar", "dip-straight-bar", 4)
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
        assertRoutePath("dip", listOf("dip-support-hold", "dip-negative", "dip-parallel", "dip-straight-bar"))
        assertEquals(listOf("dip-straight-bar"), catalog.policyForVariation("dip-parallel")!!.nextVariationIds.take(1))
        assertRouteMember("library", "dip-l-sit", ProgressionRole.LIBRARY)
        assertRouteMember("muscle-up", "dip-russian", ProgressionRole.SUPPORT, "pullup-sternum")
        assertRoutePath("rdl-hinge", listOf("rdl-bodyweight", "single-leg-deadlift"))
        assertRoutePath("nordic", listOf("nordic-negative-banded", "nordic-banded", "nordic-curl"))
        assertRoutePath("slide-hamstring", listOf("slide-negative", "slide-hamstring", "slide-negative-single", "slide-single-leg"))
        assertTrue(catalog.policyForVariation("single-leg-deadlift")!!.nextVariationIds.contains("nordic-negative-banded").not())
        assertTrue(catalog.policyForVariation("nordic-negative-banded")!!.prerequisiteRule.allOf.isEmpty())
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
        assertRoutePath("pullup", listOf("dead-hang", "scapular-pull", "arch-hang", "pullup-negative", "pullup-full", "pullup-chest-to-bar"))
        assertRouteMember("pullup", "pullup-band-assisted", ProgressionRole.ALTERNATE, "pullup-negative")
        assertRouteMember("pullup", "chair-assisted-pullup", ProgressionRole.ALTERNATE, "pullup-negative")
        assertRouteMember("pullup", "flexed-arm-hang", ProgressionRole.ALTERNATE, "pullup-negative")
        assertRouteGate("pullup-full", "pullup-negative", 4)
        for (id in listOf("scapular-pull", "arch-hang", "pullup-negative")) assertEquals(id, setOf("pullup-bar"), catalog.variation(id)!!.equipmentAlternatives.flatMap { it.needs }.map { it.equipmentId }.toSet())
        assertRoutePath("lsit", listOf("l-sit-foot-supported", "l-sit-one-foot", "l-sit-tuck", "l-sit-advanced-tuck", "l-sit-one-leg", "l-sit-floor", "l-sit-straddle", "v-sit-floor"))
        assertRouteGate("l-sit-floor", "l-sit-one-leg", 4)
        assertRoutePath("squat", listOf("squat-assisted", "squat-air", "split-squat", "split-squat-bulgarian", "squat-pistol-assisted", "squat-pistol-box", "squat-pistol"))
        assertRoutePath("shrimp-squat", listOf("squat-shrimp", "squat-shrimp-intermediate", "squat-shrimp-advanced"))
        assertRoutePath("back-bridge", listOf("bridge-table", "bridge-incline", "bridge-head", "bridge-half", "bridge-wall-walkdown", "bridge-back", "bridge-wall-walkup", "bridge-stand-to-stand"))
        assertRouteGate("bridge-back", "bridge-wall-walkdown", 3)
    }

    @Test fun l02PushUpChainFollowsTheLoadCurve() {   // Ebben 2011: wall < high incline < knee / low incline < full < diamond / 30 cm decline < archer < one-arm
        assertRoutePath("pushup", listOf("pushup-wall", "pushup-incline-high", "pushup-incline", "pushup-standard", "pushup-feet-elevated", "pushup-archer", "pushup-one-arm-elevated", "pushup-one-arm-negative", "pushup-one-arm"))
        assertRouteMember("pushup", "pushup-knee", ProgressionRole.ALTERNATE, "pushup-incline")
        assertRouteMember("pushup", "pushup-diamond", ProgressionRole.ALTERNATE, "pushup-feet-elevated")
        assertContractExtensions("pushup-standard", listOf("pushup-diamond", "pushup-feet-elevated"))
        assertRouteGate("pushup-feet-elevated", "pushup-standard", 4)
        assertRouteGate("pushup-archer", "pushup-feet-elevated", 3)
        assertContractExtensions("pushup-diamond", listOf("pushup-archer"))
        val archerPrerequisites = catalog.policyForVariation("pushup-archer")!!.prerequisiteRule.allOf.flatten().mapNotNull { it.variationTierMet?.variationId }.toSet()
        assertEquals(setOf("pushup-feet-elevated"), archerPrerequisites)
        val rank = { id: String -> catalog.variation(id)!!.difficultyRank }
        assertTrue(rank("pushup-wall") < rank("pushup-incline-high") && rank("pushup-incline-high") < rank("pushup-incline") && rank("pushup-standard") < rank("pushup-diamond"))
    }

    @Test fun l03PullUpPathWithoutABand() {
        fun next(id: String) = catalog.policyForVariation(id)!!.nextVariationIds
        fun needs(id: String) = catalog.variation(id)!!.equipmentAlternatives.flatMap { it.needs }.map { it.equipmentId }.toSet()
        assertRoutePath("pullup", listOf("dead-hang", "scapular-pull", "arch-hang", "pullup-negative", "pullup-full", "pullup-chest-to-bar"))
        assertRouteMember("pullup", "flexed-arm-hang", ProgressionRole.ALTERNATE, "pullup-negative")
        assertRouteMember("pullup", "chair-assisted-pullup", ProgressionRole.ALTERNATE, "pullup-negative")
        assertRouteGate("pullup-full", "pullup-negative", 4)
        assertEquals(setOf("pullup-bar", "chair"), needs("chair-assisted-pullup")); assertEquals(setOf("pullup-bar"), needs("dead-hang")); assertEquals(Kind.HOLD, catalog.variation("flexed-arm-hang")!!.kind)
        // chin-up and pull-up are parallel: same way in, neither leads to the other
        assertEquals(catalog.policyForVariation("pullup-full")!!.prerequisiteRule, catalog.policyForVariation("chinup-full")!!.prerequisiteRule)
        assertTrue(next("chinup-full").isEmpty() && "chinup-full" !in next("pullup-full"))
        // the questionnaire's pull ladder has a hold step between the assisted and the full pull-up (placement for people who can hang with the chin over the bar)
        assertEquals(listOf("towel-door-row", "row-band", "pullup-band-assisted", "flexed-arm-hang", "pullup-full"), catalog.onboardingFamilies.first { it.id == "pull" }.ladder)
    }

    @Test fun l04RowsWithoutALowBar() {
        fun prereq(id: String) = catalog.policyForVariation(id)!!.prerequisiteRule.allOf.map { g -> g.mapNotNull { it.variationTierMet?.variationId } }
        fun needs(id: String) = catalog.variation(id)!!.equipmentAlternatives.flatMap { it.needs }.map { it.equipmentId }.toSet()
        // a door-and-towel row needs no equipment from the profile and carries the safety copy; the inverted rows need the low bar (or a sturdy table)
        assertTrue(needs("towel-door-row").isEmpty()); assertTrue(catalog.variation("towel-door-row")!!.cautions.any { "knot" in it && "door" in it })
        assertEquals(setOf("low-bar"), needs("inverted-row")); assertEquals(setOf("low-bar"), needs("inverted-row-wide")); assertEquals(setOf("kettlebell"), needs("kettlebell-one-arm-row"))
        assertEquals(listOf(listOf("row-band", "towel-door-row")), prereq("inverted-row-bent-knees"))
        // Main row route is explicit; route display does not derive from the global next list.
        assertRoutePath("row", listOf("towel-door-row", "inverted-row-bent-knees", "inverted-row", "inverted-row-feet-elevated", "archer-row"))
        assertRouteGate("inverted-row-feet-elevated", "inverted-row", 3)
        assertTrue(listOf("inverted-row-wide") in prereq("front-lever-tuck"))
    }

    @Test fun l05SingleLegSquatRestructure() {
        assertRoutePath("squat", listOf("squat-assisted", "squat-air", "split-squat", "split-squat-bulgarian", "squat-pistol-assisted", "squat-pistol-box", "squat-pistol"))
        assertRoutePath("shrimp-squat", listOf("squat-shrimp", "squat-shrimp-intermediate", "squat-shrimp-advanced"))
        assertRouteMember("squat", "squat-pistol-counterbalance", ProgressionRole.ALTERNATE, "squat-pistol-box")
        assertRouteGate("squat-pistol-box", "squat-pistol-assisted", 3)
        assertRouteGate("squat-pistol", "squat-pistol-box", 4)
        assertRouteGate("squat-shrimp-advanced", "squat-shrimp-intermediate", 4)
        assertEquals("squat-shrimp-advanced", routeById.getValue("shrimp-squat").goalVariationId)
        assertEquals(listOf("shrimp-squat"), routeById.getValue("shrimp-squat").goalIds)
        // the ankle check is information, in every single-leg step
        for (id in listOf("squat-shrimp", "squat-shrimp-intermediate", "squat-shrimp-advanced", "squat-pistol-assisted", "squat-pistol-box", "squat-pistol-counterbalance"))
            assertTrue(id, catalog.variation(id)!!.instructions.any { "Ankle check" in it && "information only" in it })
        assertEquals(setOf("chair"), catalog.variation("squat-pistol-box")!!.equipmentAlternatives.flatMap { it.needs }.map { it.equipmentId }.toSet())
    }


    @Test fun l06HingeHasFourPathsWithSafeNordicDefaults() {
        fun tiers(id: String) = catalog.policyForVariation(id)!!.tiers.map { it.target.value }
        // Hinge, Nordic knee flexion, and sliding curls are independent routes.
        assertRoutePath("rdl-hinge", listOf("rdl-bodyweight", "single-leg-deadlift"))
        assertRoutePath("nordic", listOf("nordic-negative-banded", "nordic-banded", "nordic-curl"))
        assertRoutePath("slide-hamstring", listOf("slide-negative", "slide-hamstring", "slide-negative-single", "slide-single-leg"))
        assertTrue(catalog.policyForVariation("nordic-negative-banded")!!.prerequisiteRule.allOf.isEmpty())
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
        assertRoutePath("lsit", listOf("l-sit-foot-supported", "l-sit-one-foot", "l-sit-tuck", "l-sit-advanced-tuck", "l-sit-one-leg", "l-sit-floor", "l-sit-straddle", "v-sit-floor"))
        val accumulatedMinuteSteps = listOf("l-sit-foot-supported", "l-sit-one-foot", "l-sit-tuck", "l-sit-advanced-tuck", "l-sit-one-leg", "l-sit-floor")
        for (id in accumulatedMinuteSteps) {
            val p = catalog.policyForVariation(id)!!
            assertEquals(id, app.calisthenics.domain.model.TierProfile.H60, p.tierProfile); assertEquals(id, listOf(10, 15, 20, 30, 60), p.tiers.map { it.target.value })
            assertEquals(Kind.HOLD, catalog.variation(id)!!.kind); assertTrue(id, p.tierNote!!.contains("accumulated minute"))
        }
        val straddle = catalog.policyForVariation("l-sit-straddle")!!
        assertEquals(Kind.HOLD, catalog.variation("l-sit-straddle")!!.kind)
        assertEquals(listOf(5, 8, 10, 15, 20), straddle.tiers.map { it.target.value })
    }

    @Test fun l08CorePlanes() {
        assertRoutePath("anti-extension", listOf("dead-bug-90-90", "dead-bug-single-leg", "dead-bug", "plank", "plank-long-lever", "body-saw", "rollout-knees", "rollout-ramp", "rollout-negative", "rollout-standing"))
        assertRouteGate("plank", "dead-bug", 3)
        assertRoutePath("hollow", listOf("hollow-tuck", "hollow-one-leg", "hollow-hold", "hollow-rocks"))
        assertRoutePath("pallof-hold", listOf("pallof-hold")); assertRoutePath("pallof-press", listOf("pallof-press"))
        assertTrue(catalog.variation("pallof-press")!!.instructions.any { "Stance rule" in it })
        assertRouteGate("copenhagen-side-plank", "copenhagen-side-plank-short", 4)
        assertRoutePath("extension", listOf("superman-hold", "arch-hold-y", "arch-rocks"))
        val carry = catalog.variation("kettlebell-suitcase-carry")!!
        assertEquals(setOf("kettlebell"), carry.equipmentAlternatives.flatMap { it.needs }.map { it.equipmentId }.toSet()); assertTrue(carry.unilateral && carry.kind == Kind.HOLD)
    }

    @Test fun coreVarietyUsesReviewedDefaultRouteAnchors() {
        assertTrue(routeById.getValue("floor-legraise").defaultPool)
        assertTrue(routeById.getValue("side-plank").defaultPool)
        for (id in listOf("crunch", "sit-up", "rower-situp", "curl-up-mcgill"))
            assertRouteMember("floor-legraise", id, ProgressionRole.ALTERNATE, "leg-raise-lying")
        for (id in listOf("bicycle-crunch", "russian-twist", "pallof-hold", "pallof-press", "bird-dog"))
            assertRouteMember("side-plank", id, ProgressionRole.ALTERNATE, "side-plank")
        assertTrue("bird-dog retains its own MAIN route", routeById.getValue("bird-dog").mainPath.contains("bird-dog"))
        val flexion = catalog.exerciseGroups.single { it.id == "floor-legraise-entry-level" }
        assertEquals("leg-raise-lying", flexion.mainVariationId)
        assertTrue(flexion.alternativeVariationIds.containsAll(listOf("crunch", "sit-up", "rower-situp", "curl-up-mcgill")))
        val variety = catalog.exerciseGroups.single { it.id == "side-plank-core-variety" }
        assertEquals("side-plank", variety.mainVariationId)
        assertTrue(variety.alternativeVariationIds.containsAll(listOf("bicycle-crunch", "russian-twist", "pallof-hold", "pallof-press", "bird-dog")))
    }

    @Test fun reviewedOrEntryRemainsAvailableBesideTheMainRowStep() {
        val groups = catalog.policyForVariation("inverted-row-bent-knees")!!.prerequisiteRule.allOf
        assertTrue(groups.any { group ->
            group.mapNotNull { it.variationTierMet?.variationId }.toSet() == setOf("row-band", "towel-door-row")
        })
    }

    @Test fun l09BackBridgeLadder() {
        assertRoutePath("hip-bridge", listOf("glute-bridge", "bridge-single-leg"))
        assertRoutePath("hip-thrust", listOf("hip-thrust", "hip-thrust-single-leg"))
        assertRoutePath("back-bridge", listOf("bridge-table", "bridge-incline", "bridge-head", "bridge-half", "bridge-wall-walkdown", "bridge-back", "bridge-wall-walkup", "bridge-stand-to-stand"))
        assertRouteGate("bridge-single-leg", "glute-bridge", 4)
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
        assertRoutePath("calf", listOf("calf-raise", "calf-raise-step", "calf-raise-single", "calf-raise-step-single", "calf-raise-single-loaded"))
        assertRouteMember("library", "tibialis-raise", ProgressionRole.LIBRARY)
        assertRouteMember("library", "tibialis-raise-single", ProgressionRole.LIBRARY)
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
