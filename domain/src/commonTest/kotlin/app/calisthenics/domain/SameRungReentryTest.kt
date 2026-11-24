package app.calisthenics.domain

import app.calisthenics.domain.content.parseCatalog
import app.calisthenics.domain.equipment.SeedProfiles
import app.calisthenics.domain.feedback.SessionEvidence
import app.calisthenics.domain.model.*
import app.calisthenics.domain.planner.*
import app.calisthenics.domain.progression.ProgressionEngine
import app.calisthenics.domain.progression.UserAction
import app.calisthenics.domain.routine.SessionDraft
import kotlin.test.Test

class SameRungReentryTest {
    private val primary = ProgressionRoute("primary", "Primary", "push", mainPath = listOf("lower", "main"),
        members = listOf(
            ProgressionMember("alt-a", ProgressionRole.ALTERNATE, "main"),
            ProgressionMember("alt-b", ProgressionRole.ALTERNATE, "main"),
            ProgressionMember("support", ProgressionRole.SUPPORT, "main"),
        ))
    private val secondary = ProgressionRoute("secondary", "Secondary", "other", mainPath = listOf("other"),
        members = listOf(ProgressionMember("alt-a", ProgressionRole.ALTERNATE, "other")))
    private val ids = listOf("lower", "main", "alt-a", "alt-b", "support", "other", "unrelated")
    private val catalog = Catalog(1, ids.map { Fx.strength(it, "push") }, ids.map { Fx.policy(it) },
        progressionRoutes = listOf(primary, secondary),
        personalTargets = mapOf("alt-b" to PersonalLevelTargets(revision = 1, values = listOf(4, 8, 12, 16, 20))))
    private val engine = ProgressionEngine(catalog)

    private fun work(id: String, day: Int, tier: Int = 1, route: String? = primary.id, completed: Int = 1,
        reentry: Int? = null) = SessionEvidence(
        "session-$day-$id", id, "push", day, tier, completed, skippedOrPartial = true,
        anyBelow = false, discomfort = false, discomfortReportedDay = null, easierOverride = false,
        assumedBlocks = completed, confirmedBlocks = 0, minQualifyingBlocks = 2,
        progressionRouteId = route, reentryFromTier = reentry, targetRevision = if (id == "alt-b") 1 else 0,
    )

    private fun preview(id: String, evidence: List<SessionEvidence>, override: Int? = null): WorkoutPlan {
        val routine = Routine("test", 1, "Re-entry test", listOf(
            RoutineSlot("add-work", Pattern.PUSH_HORIZONTAL, Area.UPPER_BODY, id, progressionRouteId = primary.id),
        ))
        val draft = SessionDraft(600, false, emptySet(), false, false, setOf(StrengthFocus.FULL_BODY),
            SeedProfiles.home.id, rounds = 1, tierOverrides = override?.let { mapOf("add-work" to it) }.orEmpty())
        val progress = engine.replay(evidence, emptyList())
        return (generate(PlanInput("plan", 0, 20, catalog, routine, draft, SeedProfiles.home, progress = progress)) as PlanResult.Ready).plan
    }

    @Test fun recentWorkAtTheSameRungKeepsEachVariationsOwnPrescription() {
        for ((planned, trained) in listOf("main" to "alt-b", "alt-b" to "main", "alt-b" to "alt-a", "alt-a" to "alt-b")) {
            val plan = preview(planned, listOf(work(planned, 1, 4), work(trained, 7)))
            val block = plan.blocks.first { it.type == BlockType.WORK }
            assertEquals("$trained → $planned", 4, block.prescriptionTier)
            assertEquals(if (planned == "alt-b") 16 else 20, block.target?.value)
            assertNull(block.reentryFromTier)
            assertTrue(plan.notes.none { it.kind == NoteKind.RETURN })
        }
    }

    @Test fun staleAndUnrelatedWorkCannotKeepAnExerciseAtItsTier() {
        val old = work("alt-b", 1, 4)
        val nonProtectingWork = listOf(
            work("main", 6),
            work("lower", 19),
            work("unrelated", 19),
            work("support", 19),
            work("alt-a", 19, route = secondary.id),
            work("alt-a", 19, route = null),
            work("main", 19, completed = 0),
        )
        for (recent in nonProtectingWork) {
            val plan = preview("alt-b", listOf(old, recent))
            val block = plan.blocks.first { it.type == BlockType.WORK }
            assertEquals(recent.toString(), 3, block.prescriptionTier)
            assertEquals(4, block.reentryFromTier)
            assertTrue(plan.notes.any { it.kind == NoteKind.RETURN })
        }
        val historical = preview("alt-b", listOf(old, work("main", 19, route = null)))
        assertEquals(4, historical.blocks.first { it.type == BlockType.WORK }.prescriptionTier)
    }

    @Test fun walkingLungeRotatesInAtItsOwnLevelAfterRecentSplitSquats() {
        val starter = parseCatalog(fixtureText("content/starter/catalog.json"))
        val route = starter.routeById("squat")!!
        val evidence = listOf(work("walking-lunge", 1, 3, route.id), work("split-squat", 10, 2, route.id),
            work("split-squat", 15, 2, route.id)).map { it.copy(familyId = starter.variation(it.variationId)!!.familyId) }
        val progress = ProgressionEngine(starter).replay(evidence, emptyList())
        val slot = RoutineSlot("legs", Pattern.SQUAT, Area.LOWER_BODY, "split-squat", progressionRouteId = route.id)
        val routine = Routine("usual", 1, "Legs", listOf(slot))
        val draft = SessionDraft(600, false, emptySet(), false, false, setOf(StrengthFocus.FULL_BODY),
            SeedProfiles.home.id, rounds = 2)
        val otherAlternatives = route.members.filter { it.role == ProgressionRole.ALTERNATE && it.variationId != "walking-lunge" }
            .map { it.variationId }.toSet()
        val plan = (generate(PlanInput("preview", 0, 20, starter, routine, draft, SeedProfiles.home,
            excludedVariationIds = otherAlternatives, progress = progress)) as PlanResult.Ready).plan
        assertEquals(listOf(PlanSwap("walking-lunge", "split-squat", SwapKind.ROTATED)), plan.swaps)
        val workBlocks = plan.blocks.filter { it.type == BlockType.WORK }
        assertTrue(workBlocks.isNotEmpty())
        assertTrue(workBlocks.all { it.variationId == "walking-lunge" && it.prescriptionTier == 3 && it.reentryFromTier == null })
        assertTrue(plan.notes.none { it.kind == NoteKind.RETURN })
    }

    @Test fun replayChecksTheSameRecencyBeforeApplyingAMarkedBreak() {
        val old = work("alt-b", 1, 4)
        val reentry = work("alt-b", 20, 3, reentry = 4)
        val protected = engine.replay(listOf(old, work("alt-a", 7), reentry), emptyList()).variations.getValue("alt-b")
        assertEquals(4, protected.tier)
        assertEquals(3, protected.earnedStars())
        assertNull(protected.backFromBreakDay)
        val dropped = engine.replay(listOf(old, work("alt-a", 6), reentry), emptyList()).variations.getValue("alt-b")
        assertEquals(3, dropped.tier)
        assertEquals(3, dropped.earnedStars())
        assertEquals(20, dropped.backFromBreakDay)
    }

    @Test fun markedVariationsInOneWorkoutUsePracticeFromBeforeThatWorkout() {
        val old = listOf(work("main", 1, 4), work("alt-b", 1, 4))
        val first = work("main", 20, 3, reentry = 4).copy(sessionId = "return")
        val second = work("alt-b", 20, 3, reentry = 4).copy(sessionId = "return")
        for (reentries in listOf(listOf(first, second), listOf(second, first))) {
            val progress = engine.replay(old + reentries, emptyList())
            for (id in listOf("main", "alt-b")) {
                assertEquals(3, progress.tierFor(id))
                assertEquals(20, progress.variations.getValue(id).backFromBreakDay)
            }
        }

        val earlierWorkout = work("alt-a", 20).copy(sessionId = "earlier")
        val protected = engine.replay(old + listOf(earlierWorkout, first, second), emptyList())
        assertEquals(4, protected.tierFor("main"))
        assertEquals(4, protected.tierFor("alt-b"))
    }

    @Test fun sameDayRecencyUsesRecordedWorkoutOrderRatherThanSessionIds() {
        val old = work("alt-b", 1, 4)
        val marked = work("alt-b", 20, 3, reentry = 4).copy(sessionId = "middle", recordedAtEpochMs = 200)
        val later = work("main", 20).copy(sessionId = "first-by-id", recordedAtEpochMs = 300)
        val earlier = work("main", 20).copy(sessionId = "z-last-by-id", recordedAtEpochMs = 100)

        val returned = engine.replay(listOf(later, marked, old), emptyList()).variations.getValue("alt-b")
        assertEquals(3, returned.tier)
        assertEquals(20, returned.backFromBreakDay)

        val protected = engine.replay(listOf(marked, earlier, old), emptyList()).variations.getValue("alt-b")
        assertEquals(4, protected.tier)
        assertNull(protected.backFromBreakDay)
    }

    @Test fun sameRungSelfAssessmentAndSkippedWorkDoNotRefreshPractice() {
        val old = work("alt-b", 1, 4)
        val skipped = work("main", 19, completed = 0)
        val progress = engine.replay(listOf(old, skipped), listOf(UserAction.SelfAssessment(18, "main", 3)))
        assertEquals(3, progress.reentryNeeded("alt-b", 20, catalog, primary.id))
    }

    @Test fun manualOverrideAtTheLowerTierDoesNotMarkAnAutomaticBreak() {
        val plan = preview("alt-b", listOf(work("alt-b", 1, 4)), override = 3)
        val block = plan.blocks.first { it.type == BlockType.WORK }
        assertEquals(3, block.prescriptionTier)
        assertNull(block.reentryFromTier)
        assertTrue(plan.notes.none { it.kind == NoteKind.RETURN })
    }
}
