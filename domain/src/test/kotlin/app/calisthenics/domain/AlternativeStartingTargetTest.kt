package app.calisthenics.domain

import app.calisthenics.domain.content.parseCatalog
import app.calisthenics.domain.equipment.SeedProfiles
import app.calisthenics.domain.feedback.*
import app.calisthenics.domain.model.*
import app.calisthenics.domain.planner.*
import app.calisthenics.domain.progression.*
import app.calisthenics.domain.routine.SessionDraft
import org.junit.Assert.*
import org.junit.Test
import java.io.File

class AlternativeStartingTargetTest {
    private val c = parseCatalog(File(System.getProperty("repo.root"), "content/starter/catalog.json").readText())
    private val main = c.variation("pushup-feet-elevated")!!
    private val alt = c.variation("pushup-diamond")!!
    private val actions = listOf(UserAction.SelfAssessment(0, "pushup-standard", 6), UserAction.SelfAssessment(0, main.id, 5))
    private val progress = ProgressionEngine(c).replay(emptyList(), actions).copy(routeWorkoutCounts = mapOf("pushup" to 1))
    private val routine = Routine("test", 1, "Test", listOf(RoutineSlot("push", Pattern.PUSH_HORIZONTAL, Area.UPPER_BODY, main.id, progressionRouteId = "pushup")))
    private val draft = SessionDraft(600, false, emptySet(), false, false, setOf(StrengthFocus.FULL_BODY), SeedProfiles.home.id, rounds = 2)
    private fun plan(d: SessionDraft = draft, p: ProgressSnapshot = progress, excluded: Set<String> = emptySet()) =
        (generate(PlanInput("test", 0, 1, c, routine, d, SeedProfiles.home, excludedVariationIds = excluded, progress = p)) as PlanResult.Ready).plan

    private fun atTier(variation: ExerciseVariation, tier: Int) = progress.copy(
        variations = mapOf(variation.id to progress.variations.getValue(main.id).copy(
            variationId = variation.id, familyId = variation.familyId, tier = tier, achievedTiers = emptySet())),
    )

    @Test fun twelveMainRepsSuggestEightForTheUntriedSameRungAlternative() {
        val blocks = plan().blocks.filter { it.type == BlockType.WORK }
        assertTrue(blocks.all { it.variationId == alt.id && it.target!!.value == 8 && it.prescriptionTier == 3 && it.inferredStartTier == 3 })
        assertFalse(progress.variations.containsKey(alt.id)) // preview is read-only
    }

    @Test fun firstAlternativeAfterMainAdvancementUsesItsOriginalRungHistory() {
        val progressed = ProgressionEngine(c).replay(emptyList(), listOf(
            UserAction.SelfAssessment(0, "pushup-standard", 6), UserAction.SelfAssessment(0, main.id, 6)))
            .copy(routeWorkoutCounts = mapOf("pushup" to 1))
        assertEquals("pushup-archer", progressed.activeInFamily[main.familyId])
        val otherAlternatives = c.routeById("pushup")!!.members.filter { it.role == ProgressionRole.ALTERNATE && it.variationId != alt.id }.map { it.variationId }.toSet()
        val generated = (generate(PlanInput("advanced", 0, 1, c, routine, draft, SeedProfiles.home,
            excludedVariationIds = otherAlternatives, progress = progressed)) as PlanResult.Ready).plan
        assertTrue(generated.blocks.filter { it.type == BlockType.WORK }.all { it.variationId == alt.id && it.target!!.value == 8 && it.inferredStartTier == 3 })
    }

    @Test fun completedWorkoutKeepsItsStartingLevelWithoutTransferringAchievements() {
        val plan = plan()
        val outcomes = resolveFeedback(plan, plan.blocks.associate { it.id to Execution.COMPLETED })
        val evidence = deriveEvidence("first-alt", 1, plan, outcomes, { c.variation(it)!!.familyId }, { _, _ -> 2 })
        assertEquals(3, evidence.single().inferredStartTier)
        val replay = ProgressionEngine(c).replay(evidence, actions)
        val p = replay.variations.getValue(alt.id)
        assertEquals(3, p.tier)
        assertEquals(3, p.baselineTier)
        assertTrue(p.achievedTiers.isEmpty())
        val otherAlternatives = c.routeById("pushup")!!.members.filter { it.role == ProgressionRole.ALTERNATE && it.variationId != alt.id }.map { it.variationId }.toSet()
        val nextAltWorkout = plan(p = replay.copy(routeWorkoutCounts = mapOf("pushup" to 3)), excluded = otherAlternatives)
        assertTrue(nextAltWorkout.blocks.filter { it.type == BlockType.WORK }.all { it.target!!.value == 8 && it.inferredStartTier == null })
    }

    @Test fun ownHistoryManualTargetsAndRangesTakePrecedence() {
        val own = ProgressionEngine(c).replay(emptyList(), actions + UserAction.SelfAssessment(0, alt.id, 2))
            .copy(routeWorkoutCounts = mapOf("pushup" to 1))
        assertTrue(plan(p = own).blocks.filter { it.type == BlockType.WORK }.all { it.target!!.value == 6 && it.inferredStartTier == null })
        assertTrue(plan(draft.copy(tierOverrides = mapOf("push" to 1))).blocks.filter { it.type == BlockType.WORK }.all { it.target!!.value == 4 && it.inferredStartTier == null })
        assertTrue(plan(draft.copy(rule = ProgressionRule(RuleKind.REP_RANGE))).blocks.filter { it.type == BlockType.WORK }.all { it.inferredStartTier == null })
    }

    @Test fun aSkippedFirstWorkoutKeepsTheEstimateWithoutCompletingAnyTier() {
        val plan = plan()
        val outcomes = resolveFeedback(plan, plan.blocks.associate { it.id to Execution.SKIPPED })
        val evidence = deriveEvidence("skipped-alt", 1, plan, outcomes, { c.variation(it)!!.familyId }, { _, _ -> 2 })
        val p = ProgressionEngine(c).replay(evidence, actions).variations.getValue(alt.id)
        assertEquals(3, p.tier)
        assertEquals(3, p.baselineTier)
        assertTrue(p.achievedTiers.isEmpty())
        assertTrue(p.streakDays.isEmpty())
    }

    @Test fun transferRequiresMatchingRepMovementAndExplicitSameRungMembership() {
        assertNull(inferredAlternativeTier(c, main, alt.copy(kind = Kind.HOLD), progress, "pushup"))
        assertNull(inferredAlternativeTier(c, main, alt.copy(progressionGroup = "other"), progress, "pushup"))
        assertNull(inferredAlternativeTier(c, main, alt.copy(unilateral = true), progress, "pushup"))
        assertNull(inferredAlternativeTier(c, main, alt, progress, "row"))
        val held = progress.copy(holds = mapOf(main.familyId to FamilyHold(main.familyId, 0, null, null)))
        assertNull(inferredAlternativeTier(c, main, alt, held, "pushup"))
    }

    @Test fun reviewedSameRungAlternativesTransferAcrossPushPullLowerAndCore() {
        val cases = listOf(
            Triple("pushup", "pushup-feet-elevated", "pushup-diamond"),
            Triple("pullup", "pullup-negative", "pullup-band-assisted"),
            // These authored route alternatives cross legacy family IDs, and therefore exercise the broadened membership rule.
            Triple("squat", "split-squat", "step-up"),
            Triple("floor-legraise", "v-up", "v-up-tuck"),
        )
        cases.forEach { (routeId, mainId, alternativeId) ->
            val source = c.variation(mainId)!!
            val alternative = c.variation(alternativeId)!!
            assertNotNull("$mainId to $alternativeId", inferredAlternativeTier(c, source, alternative, atTier(source, 5), routeId))
        }
    }

    @Test fun comparableTimedHoldsUseSecondsAndKeepBothSidesIndependentOfRepTargets() {
        val source = c.variation("side-plank")!!
        val alternative = c.variation("side-plank-knee")!!
        val p = ProgressionEngine(c).replay(emptyList(), listOf(UserAction.SelfAssessment(0, source.id, 5)))
            .copy(routeWorkoutCounts = mapOf("side-plank" to 1))
        val route = c.routeById("side-plank")!!
        val otherAlternatives = route.members.filter { it.role == ProgressionRole.ALTERNATE && it.variationId != alternative.id }.map { it.variationId }.toSet()
        val r = routine.copy(slots = listOf(RoutineSlot("side", Pattern.CORE_ANTI_LATERAL, Area.CORE, source.id, progressionRouteId = route.id)))
        val result = (generate(PlanInput("holds", 0, 1, c, r, draft, SeedProfiles.home,
            excludedVariationIds = otherAlternatives, progress = p)) as PlanResult.Ready).plan
        val work = result.blocks.filter { it.type == BlockType.WORK }
        assertTrue(work.isNotEmpty())
        assertTrue(work.all { it.variationId == alternative.id && it.target!!.type == TargetType.HOLD_SECONDS && it.target!!.value == 30 && it.inferredStartTier == 4 })
        assertEquals(setOf(Side.LEFT, Side.RIGHT), work.map { it.side }.toSet())
        assertNull(inferredAlternativeTier(c, source, alt, p, route.id)) // never seconds to reps
    }

    @Test fun legacyGroupMembershipCannotAuthorizeAnAlternativeOnAnotherRoute() {
        val source = c.variation("leg-raise-lying")!!
        val crunch = c.variation("crunch")!!
        // The legacy ExerciseGroup includes crunch, but its route membership is attached to core-flexion instead.
        assertNull(inferredAlternativeTier(c, source, crunch, atTier(source, 5), "floor-legraise"))
    }

    @Test fun differentProgressionGroupsAndAlternativeFamilyHoldsBlockTransfer() {
        val source = c.variation("leg-raise-lying")!!
        val crunch = c.variation("crunch")!!
        assertNull(inferredAlternativeTier(c, source, crunch, atTier(source, 5), "core-flexion"))

        val stepMain = c.variation("split-squat")!!
        val stepUp = c.variation("step-up")!!
        val heldAlternativeFamily = atTier(stepMain, 5).copy(
            holds = mapOf(stepUp.familyId to FamilyHold(stepUp.familyId, 0, null, null)),
        )
        assertNull(inferredAlternativeTier(c, stepMain, stepUp, heldAlternativeFamily, "squat"))
    }

    @Test fun crossFamilyLowerRouteAlternativeIsRotatedAndGetsAnInferredStartMarker() {
        val source = c.variation("split-squat")!!
        val alternative = c.variation("step-up")!!
        val started = atTier(source, 5)
        val squatAirProgress = started.variations.getValue(source.id).copy(
            variationId = "squat-air", familyId = "squat", tier = 4, achievedTiers = setOf(1, 2, 3),
        )
        val lowerProgress = started.copy(
            variations = started.variations + ("squat-air" to squatAirProgress),
            routeWorkoutCounts = mapOf("squat" to 1),
        )
        val lowerRoutine = Routine("lower-test", 1, "Lower test", listOf(
            RoutineSlot("legs", Pattern.LUNGE, Area.LOWER_BODY, source.id, progressionRouteId = "squat"),
        ))
        val profileWithStep = SeedProfiles.home.copy(items = SeedProfiles.home.items + EquipmentItem("step"))
        val otherAlternatives = c.routeById("squat")!!.members
            .filter { it.role == ProgressionRole.ALTERNATE && it.variationId != alternative.id }
            .map { it.variationId }.toSet()
        val generated = generate(PlanInput(
            "lower-test", 0, 1, c, lowerRoutine, draft, profileWithStep,
            excludedVariationIds = otherAlternatives, progress = lowerProgress,
        ))
        assertTrue("expected a ready plan, got $generated", generated is PlanResult.Ready)
        val work = (generated as PlanResult.Ready).plan.blocks.filter { it.type == BlockType.WORK }
        assertTrue(work.isNotEmpty())
        assertTrue(work.all { it.variationId == alternative.id && it.inferredStartTier == 4 })
    }

    @Test fun rotationBlocksBothFamilyHoldsAndLoadedAlternativesDoNotGetTransferredTargets() {
        val source = c.variation("split-squat")!!
        val alternative = c.variation("step-up")!!
        val started = ProgressionEngine(c).replay(emptyList(), listOf(
            UserAction.SelfAssessment(0, "squat-air", 4), UserAction.SelfAssessment(0, source.id, 5)))
            .copy(routeWorkoutCounts = mapOf("squat" to 1))
        val isolated = c.copy(progressionRoutes = c.progressionRoutes.map { route ->
            if (route.id != "squat") route else route.copy(members = route.members.filter { it.role != ProgressionRole.ALTERNATE || it.variationId == alternative.id })
        })
        val profile = SeedProfiles.home.copy(items = SeedProfiles.home.items + EquipmentItem("step"))
        assertEquals(alternative.id, rotatedExercise(isolated, source, started, profile, progressionRouteId = "squat").id)
        val heldHost = started.copy(holds = mapOf(source.familyId to FamilyHold(source.familyId, 0, null, null)))
        val heldAlternative = started.copy(holds = mapOf(alternative.familyId to FamilyHold(alternative.familyId, 0, null, null)))
        assertEquals(source.id, rotatedExercise(isolated, source, heldHost, profile, progressionRouteId = "squat").id)
        assertEquals(source.id, rotatedExercise(isolated, source, heldAlternative, profile, progressionRouteId = "squat").id)

        val loadedAlternative = alternative.copy(equipmentAlternatives = listOf(
            RequirementSet(needs = listOf(EquipmentNeed("kettlebell"))),
        ))
        assertNull(inferredAlternativeTier(c, source, loadedAlternative, started, "squat"))
    }
}
