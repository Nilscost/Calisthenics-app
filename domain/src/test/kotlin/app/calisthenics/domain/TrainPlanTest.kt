package app.calisthenics.domain

import app.calisthenics.domain.content.parseCatalog
import app.calisthenics.domain.equipment.SeedProfiles
import app.calisthenics.domain.model.*
import app.calisthenics.domain.planner.*
import app.calisthenics.domain.progression.ProgressSnapshot
import app.calisthenics.domain.routine.StarterRoutine
import org.junit.Assert.*
import org.junit.Test
import java.io.File

class TrainPlanTest {
    private val catalog = parseCatalog(File(System.getProperty("repo.root"), "content/starter/catalog.json").readText())
    private val none = ProgressSnapshot(emptyMap(), emptyMap(), emptyMap(), emptyList())
    private fun plan(s: TrainSettings = TrainSettings(), swaps: Map<String, String> = emptyMap()) =
        buildTrainPlan(catalog, StarterRoutine.routine, none, SeedProfiles.home, s, swaps)

    @Test fun defaultIsFourRoundsAndAboutAClassSessionLong() {
        val p = (plan() as PlanResult.Ready).plan
        assertEquals(4, p.rounds)
        assertTrue("4 rounds = ${p.plannedDurationSeconds / 60} min", p.plannedDurationSeconds in 40 * 60..50 * 60)
    }

    @Test fun moreRoundsMeansMoreMinutes() {
        val mins = (1..6).map { r -> (plan(TrainSettings(rounds = r)) as PlanResult.Ready).plan.plannedDurationSeconds }
        assertTrue(mins.zipWithNext().all { (a, b) -> b > a })
    }

    @Test fun roundsAreClampedToWhatThePlannerAllows() {
        assertEquals(1, (plan(TrainSettings(rounds = 0)) as PlanResult.Ready).plan.rounds)
        assertEquals(MAX_EXPLICIT_ROUNDS, (plan(TrainSettings(rounds = 99)) as PlanResult.Ready).plan.rounds)
    }

    @Test fun timedStyleIsReflectedInThePlan() {
        assertTrue((plan(TrainSettings(timed = true)) as PlanResult.Ready).plan.timed)
        assertFalse((plan(TrainSettings(timed = false)) as PlanResult.Ready).plan.timed)
    }

    @Test fun focusFollowsTheGoalUnlessTheUserPicksOne() {
        assertEquals(setOf(StrengthFocus.UPPER_BODY), TrainSettings(goalId = "body-upper").effectiveFocus())
        assertEquals(setOf(StrengthFocus.CORE), TrainSettings(goalId = "body-upper", focus = setOf(StrengthFocus.CORE)).effectiveFocus())
        val upper = (plan(TrainSettings(goalId = "body-upper")) as PlanResult.Ready).plan
        assertEquals(setOf(StrengthFocus.UPPER_BODY), upper.focus)
        val slots = upper.blocks.filter { it.type == BlockType.WORK }.mapNotNull { it.variationId }.map { catalog.variation(it)!! }
        assertTrue(slots.all { Area.UPPER_BODY in it.areas })
    }

    @Test fun aSkillGoalPutsTheStepTowardItFirst() {
        val p = (plan(TrainSettings(goalId = "hspu")) as PlanResult.Ready).plan
        assertEquals("hspu", p.goalId)
        assertEquals("goal", p.blocks.first { it.type == BlockType.WORK }.slotId)
        // with no assessed level the next step is the prerequisite that unlocks the chain, not a skill that is still locked
        assertNotEquals("hspu-wall", p.blocks.first { it.type == BlockType.WORK }.variationId)
    }

    @Test fun swapsApplyToTodayOnly() {
        val base = (plan() as PlanResult.Ready).plan.blocks.first { it.slotId == "push" && it.type == BlockType.WORK }.variationId
        val swapped = (plan(swaps = mapOf("push" to "pushup-knee")) as PlanResult.Ready).plan.blocks.first { it.slotId == "push" && it.type == BlockType.WORK }.variationId
        assertEquals("pushup-incline", base); assertEquals("pushup-knee", swapped)
    }

    @Test fun placeholderPlanIdMustBeReplacedByTheCaller() {
        assertEquals("draft", (plan() as PlanResult.Ready).plan.id)
    }

    @Test fun swapOptionsAreSamePatternUsableAndStartWithTheCurrentOne() {
        val routine = trainRoutine(catalog, StarterRoutine.routine, none, "general")
        val opts = swapOptions(catalog, routine, "push", SeedProfiles.home, "pushup-incline")
        assertEquals("pushup-incline", opts.first().id)
        assertTrue(opts.size > 1)
        val slot = routine.slots.first { it.id == "push" }
        assertTrue(opts.all { slot.area in it.areas && slot.intent in it.patterns })
        assertTrue(opts.all { app.calisthenics.domain.equipment.isAvailable(it, SeedProfiles.home) })
        assertTrue(opts.none { it.kind == Kind.STRETCH })
        // equipment decides: a bar-less profile never offers a pull-up for the pull slot
        val noBar = SeedProfiles.travel
        assertTrue(swapOptions(catalog, routine, "pull", noBar, "row-band").drop(1).none { "pullup" in it.id })
        assertTrue(swapOptions(catalog, routine, "nope", SeedProfiles.home, "x").isEmpty())
    }
}

class PlanEditsTest {
    private val catalog = app.calisthenics.domain.content.parseCatalog(java.io.File(System.getProperty("repo.root"), "content/starter/catalog.json").readText())
    private val none = app.calisthenics.domain.progression.ProgressSnapshot(emptyMap(), emptyMap(), emptyMap(), emptyList())
    private val home = app.calisthenics.domain.equipment.SeedProfiles.home
    private val routine = app.calisthenics.domain.routine.StarterRoutine.routine
    private fun plan(edits: PlanEdits = PlanEdits(), stretch: Boolean = true, rounds: Int = 2) =
        (buildTrainPlan(catalog, routine, none, home, TrainSettings(rounds = rounds, stretchOn = stretch), edits = edits) as PlanResult.Ready).plan
    private fun work(p: WorkoutPlan, round: Int = 1) = p.blocks.filter { it.type == BlockType.WORK && it.roundIndex == round }

    @Test fun theLevelStepperChangesTheTargetOfOneSlotOnly() {
        val base = plan(); val up = plan(PlanEdits(tierOverrides = mapOf("push" to 4)))
        val bp = work(base).first { it.slotId == "push" }; val up4 = work(up).first { it.slotId == "push" }
        assertEquals(4, up4.prescriptionTier); assertTrue(up4.target!!.value > bp.target!!.value)
        for (s in listOf("squat", "pull", "core")) assertEquals(work(base).first { it.slotId == s }.target, work(up).first { it.slotId == s }.target)
        assertTrue(up.changesExplained.any { it.contains("level set to 4") })
        assertEquals(1, stepLevel(1, -1)); assertEquals(5, stepLevel(5, 1)); assertEquals(3, stepLevel(2, 1))
    }

    @Test fun removingAndAddingExercisesGivesValidPlans() {
        val removed = plan(PlanEdits(removed = setOf("core")))
        assertTrue(work(removed).none { it.slotId == "core" })
        val v = catalog.variation("pushup-diamond")!!
        val added = plan(PlanEdits(added = listOf(AddedSlot("squat", slotFor(v)))))
        val slots = work(added).mapNotNull { it.slotId }.distinct()
        assertEquals(slots.indexOf("squat") + 1, slots.indexOf("add-pushup-diamond"))   // inserted right after the slot it was added under
        assertTrue(added.blocks.sumOf { it.durationSeconds } == added.plannedDurationSeconds)
        val first = plan(PlanEdits(added = listOf(AddedSlot(null, slotFor(v)))))
        assertEquals("add-pushup-diamond", work(first).first().slotId)
        assertTrue(plan().blocks.sumOf { it.durationSeconds } == plan().plannedDurationSeconds)
    }

    @Test fun aPickedStretchRepeatsInEverySetAndExtraStretchesAddBlocks() {
        val pick = plan(PlanEdits(stretchPicks = mapOf("push" to "stretch-pigeon")), rounds = 3)
        for (r in 1..3) assertTrue(pick.blocks.any { it.roundIndex == r && it.slotId == "push" && it.type == BlockType.STRETCH && it.variationId == "stretch-pigeon" })
        val base = plan(); val extra = plan(PlanEdits(extraStretches = mapOf("push" to listOf("stretch-90-90"))))
        assertEquals(base.blocks.count { it.type == BlockType.STRETCH } + 4, extra.blocks.count { it.type == BlockType.STRETCH }) // 2 sides x 2 rounds
        assertTrue(extra.plannedDurationSeconds > base.plannedDurationSeconds)
    }

    @Test fun swappingToAnotherMovementTypeWarnsAndSameFamilyDoesNot() {
        val other = plan(PlanEdits(swaps = mapOf("push" to "squat-air")))
        assertTrue(other.warnings.any { it.contains("not the same movement") }); assertTrue(other.needsAcceptance)
        val same = plan(PlanEdits(swaps = mapOf("push" to "pushup-diamond")))
        assertTrue(same.warnings.none { it.contains("not the same movement") })
    }

    @Test fun theSwapSheetListsTheWholeFamilyAndOtherTypes() {
        val fam = swapFamilyOptions(catalog, "pushup-standard", home)
        assertTrue(fam.map { it.id }.containsAll(listOf("pushup-incline", "pushup-knee", "pushup-standard", "pushup-diamond", "pushup-archer")))
        assertEquals(fam.sortedBy { it.difficultyRank }.map { it.id }, fam.map { it.id })
        val others = swapOtherTypes(catalog, "pushup-standard", home)
        assertTrue(others.flatMap { it.second }.none { it.familyId == "pushup" })
        assertTrue(others.any { (tab, list) -> tab.id == "squat" && list.any { it.id == "squat-air" } })
        assertTrue(others.flatMap { it.second }.all { app.calisthenics.domain.equipment.isAvailable(it, home) })
    }

    @Test fun addableExercisesAndStretchChoicesAreOfferedForThisEquipmentWithoutDuplicates() {
        val inPlan = work(plan()).mapNotNull { it.variationId }.toSet()
        val add = addableExercises(catalog, home, inPlan)
        assertTrue(add.flatMap { it.second }.none { it.id in inPlan })
        assertTrue(add.isNotEmpty())
        assertTrue(stretchChoices(catalog, home).map { it.id }.containsAll(listOf("stretch-pigeon", "stretch-lat-wall")))
        assertTrue(PlanEdits().isEmpty())
    }
}

class DetailedEditTest {
    private val catalog = app.calisthenics.domain.content.parseCatalog(java.io.File(System.getProperty("repo.root"), "content/starter/catalog.json").readText())
    private val none = app.calisthenics.domain.progression.ProgressSnapshot(emptyMap(), emptyMap(), emptyMap(), emptyList())
    private val home = app.calisthenics.domain.equipment.SeedProfiles.home
    private val routine = app.calisthenics.domain.routine.StarterRoutine.routine
    private fun plan(edits: PlanEdits = PlanEdits()) =
        (buildTrainPlan(catalog, routine, none, home, TrainSettings(rounds = 3, stretchOn = true), edits = edits) as PlanResult.Ready).plan

    @Test fun aStretchForOneSetOnlyLeavesTheOtherSetsAlone() {
        val p = plan(PlanEdits(roundStretchPicks = mapOf("2:push" to "stretch-frog")))
        val of = { r: Int -> p.blocks.filter { it.roundIndex == r && it.slotId == "push" && it.type == BlockType.STRETCH }.mapNotNull { it.variationId }.toSet() }
        assertEquals(setOf("stretch-frog"), of(2))
        assertTrue("stretch-frog" !in of(1) && "stretch-frog" !in of(3))
        val both = plan(PlanEdits(stretchPicks = mapOf("push" to "stretch-pigeon"), roundStretchPicks = mapOf("3:push" to "stretch-frog")))
        assertEquals(setOf("stretch-pigeon"), both.blocks.filter { it.roundIndex == 1 && it.slotId == "push" && it.type == BlockType.STRETCH }.mapNotNull { it.variationId }.toSet())
        assertEquals(setOf("stretch-frog"), both.blocks.filter { it.roundIndex == 3 && it.slotId == "push" && it.type == BlockType.STRETCH }.mapNotNull { it.variationId }.toSet())
    }

    @Test fun aFreeNumberIsMarkedAndNeverCountsTowardsProgression() {
        val p = plan(PlanEdits(freeTargets = mapOf("2:push" to 13)))
        val w = p.blocks.filter { it.type == BlockType.WORK && it.slotId == "push" }
        assertEquals(listOf(false, true, false), w.sortedBy { it.roundIndex }.map { it.freeTarget })
        assertEquals(13, w.first { it.roundIndex == 2 }.target!!.value)
        val outcomes = app.calisthenics.domain.feedback.resolveFeedback(p, p.blocks.filter { it.type == BlockType.WORK }.associate { it.id to app.calisthenics.domain.feedback.Execution.COMPLETED })
        val ev = app.calisthenics.domain.feedback.deriveEvidence("S", 1, p, outcomes, { catalog.variation(it)?.familyId ?: it }, { _, _ -> 2 })
        assertFalse(ev.first { it.variationId == routine.slots.first { s -> s.id == "push" }.preferredVariationId }.qualifying)
        assertTrue(ev.first { it.variationId == "squat-air" }.qualifying)               // the other exercises still count
        val plain = app.calisthenics.domain.feedback.deriveEvidence("S", 1, plan(), app.calisthenics.domain.feedback.resolveFeedback(plan(), plan().blocks.filter { it.type == BlockType.WORK }.associate { it.id to app.calisthenics.domain.feedback.Execution.COMPLETED }), { catalog.variation(it)?.familyId ?: it }, { _, _ -> 2 })
        assertTrue(plain.all { it.qualifying })
    }

    @Test fun aFreeHoldKeepsItsGetReadyAndOldPlansDecodeWithoutTheFlag() {
        val hold = plan(PlanEdits(freeTargets = mapOf("core" to 70))).blocks.first { it.slotId == "core" && it.type == BlockType.WORK }
        assertEquals(70, hold.target!!.value); assertTrue(hold.durationSeconds >= 73)
        val j = kotlinx.serialization.json.Json { ignoreUnknownKeys = false }
        val old = j.decodeFromString(TimelineBlock.serializer(), """{"id":"b","type":"WORK","durationSeconds":60}""")
        assertFalse(old.freeTarget)
    }
}

/** V19 (D11, O1): Circuit, Pairs and Straight sets, each with the stretch-or-rest switch. */
class WorkoutFormatTest {
    private val catalog = app.calisthenics.domain.content.parseCatalog(java.io.File(System.getProperty("repo.root"), "content/starter/catalog.json").readText())
    private val none = app.calisthenics.domain.progression.ProgressSnapshot(emptyMap(), emptyMap(), emptyMap(), emptyList())
    private val home = app.calisthenics.domain.equipment.SeedProfiles.home
    private val routine = app.calisthenics.domain.routine.StarterRoutine.routine
    private fun plan(f: WorkoutFormat, stretch: Boolean = true, sets: Int = 3) =
        (buildTrainPlan(catalog, routine, none, home, TrainSettings(rounds = sets, stretchOn = stretch, format = f)) as PlanResult.Ready).plan
    private fun slots(p: WorkoutPlan) = p.blocks.filter { it.type == BlockType.WORK && it.side != Side.RIGHT }.mapNotNull { it.slotId } // one entry per set (the right side of a one-sided exercise is the same set)
    private val six = listOf("push", "squat", "pull", "core", "hinge", "core2")

    @Test fun circuitIsUnchangedAndOldPlansAreCircuits() {
        val p = plan(WorkoutFormat.CIRCUIT)
        assertEquals(WorkoutFormat.CIRCUIT, p.format)
        assertEquals(six + six + six, slots(p))
        assertTrue(p.blocks.all { it.groupIndex == null })
        val j = kotlinx.serialization.json.Json { ignoreUnknownKeys = true; encodeDefaults = true }
        val text = j.encodeToString(WorkoutPlan.serializer(), p).replace(Regex(""","format":"[A-Z]+""""), "")
        assertEquals(WorkoutFormat.CIRCUIT, j.decodeFromString(WorkoutPlan.serializer(), text).format)
    }

    @Test fun pairsAlternateTwoExercisesForAllTheirSetsThenMoveOn() {
        val p = plan(WorkoutFormat.PAIRS)
        assertEquals(WorkoutFormat.PAIRS, p.format)
        val expected = listOf("push", "squat").let { a -> List(3) { a }.flatten() } + listOf("pull", "core").let { a -> List(3) { a }.flatten() } + listOf("hinge", "core2").let { a -> List(3) { a }.flatten() }
        assertEquals(expected, slots(p))
        val w = p.blocks.filter { it.type == BlockType.WORK }
        assertEquals(setOf(1, 2, 3), w.mapNotNull { it.groupIndex }.toSet())
        assertEquals(1, w.first { it.slotId == "squat" }.groupIndex); assertEquals(2, w.first { it.slotId == "core" }.groupIndex)
        for (s in six) assertEquals(listOf(1, 2, 3), w.filter { it.slotId == s }.mapNotNull { it.roundIndex }.distinct())
    }

    @Test fun pairsWithAnOddNumberOfExercisesEndWithOneOnItsOwn() {
        val removed = (buildTrainPlan(catalog, routine, none, home, TrainSettings(rounds = 2, format = WorkoutFormat.PAIRS), edits = PlanEdits(removed = setOf("core2"))) as PlanResult.Ready).plan
        assertEquals(listOf("hinge", "hinge"), slots(removed).takeLast(2))
        assertEquals(3, removed.blocks.filter { it.type == BlockType.WORK }.mapNotNull { it.groupIndex }.max())
    }

    @Test fun straightSetsDoAllSetsOfOneExerciseThenTheNext() {
        val p = plan(WorkoutFormat.STRAIGHT)
        assertEquals(six.flatMap { listOf(it, it, it) }, slots(p))
        val w = p.blocks.filter { it.type == BlockType.WORK }
        assertEquals((1..6).toList(), w.mapNotNull { it.groupIndex }.distinct())
    }

    @Test fun everyFormatHasTheSameWorkAndABreakAfterEverySetExceptTheLast() {
        for (f in WorkoutFormat.entries) for (stretch in listOf(true, false)) {
            val p = plan(f, stretch)
            val work = p.blocks.filter { it.type == BlockType.WORK }
            assertEquals("$f has the same work as the circuit", plan(WorkoutFormat.CIRCUIT, stretch).blocks.count { it.type == BlockType.WORK }, work.size)
            val breaks = p.blocks.filter { it.type == BlockType.STRETCH || it.type == BlockType.PASSIVE_RECOVERY }
            if (stretch) assertTrue("$f: stretch on means no plain rest", breaks.none { it.type == BlockType.PASSIVE_RECOVERY } && breaks.isNotEmpty())
            else assertTrue("$f: stretch off means plain rest only", breaks.all { it.type == BlockType.PASSIVE_RECOVERY } && breaks.size == 17)
            assertEquals(p.plannedDurationSeconds, p.blocks.sumOf { it.durationSeconds })
            assertEquals("$f: nothing after the very last set", BlockType.WORK, p.blocks.last().type)
        }
        assertEquals(plan(WorkoutFormat.CIRCUIT, false).blocks.filter { it.type == BlockType.PASSIVE_RECOVERY }.sumOf { it.durationSeconds },
            plan(WorkoutFormat.STRAIGHT, false).blocks.filter { it.type == BlockType.PASSIVE_RECOVERY }.sumOf { it.durationSeconds })
    }

    @Test fun blockIdsStayUniqueInEveryFormat() {
        for (f in WorkoutFormat.entries) { val ids = plan(f).blocks.map { it.id }; assertEquals("$f", ids.size, ids.toSet().size) }
    }
}
