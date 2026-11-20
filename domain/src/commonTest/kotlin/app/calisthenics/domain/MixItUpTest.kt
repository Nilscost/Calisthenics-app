package app.calisthenics.domain

import app.calisthenics.domain.content.parseCatalog
import app.calisthenics.domain.equipment.SeedProfiles
import app.calisthenics.domain.model.EquipmentProfile
import app.calisthenics.domain.planner.*
import app.calisthenics.domain.progression.ProgressSnapshot
import app.calisthenics.domain.progression.VariationProgress
import kotlinx.serialization.json.Json
import kotlin.test.Test

/** 9.4 L4 (F7): Mix it up picks the 2 exercises with the least progress to lose and brings in a movement type missing from the plan. */
class MixItUpTest {
    private val c = parseCatalog(fixtureText("content/starter/catalog.json"))
    private val home = SeedProfiles.home
    private val today = 20_000L

    private fun vp(id: String, tier: Int, days: List<Int> = emptyList(), mastered: Boolean = false, last: Int? = null) = VariationProgress(
        id, c.variation(id)!!.familyId, tier = if (mastered) 5 else tier, enrolledAtDay = 100, baselineTier = null,
        achievedTiers = if (mastered) (1..5).toSet() else (1 until tier).toSet(), lastAutoAdvanceDay = null, streakDays = days, streakAssumed = 0,
        consecutiveBelow = 0, lowerTargetSuggested = false, autoPaused = false, lastTrainedDay = last ?: days.maxOrNull(), reentryResolvedDay = null)

    private fun snapshot(vararg v: VariationProgress, active: Map<String, String> = emptyMap()) =
        ProgressSnapshot(v.associateBy { it.variationId }, active, emptyMap(), emptyList())

    /** The owner's plan of 20 Nov, in plan order. */
    private val plan = listOf(
        MixSlot("push", "pushup-uneven"), MixSlot("squat", "split-squat"), MixSlot("pull", "chinup-full"), MixSlot("core", "plank-shoulder-tap"),
        MixSlot("bridge", "bridge-single-leg"), MixSlot("side", "side-plank"), MixSlot("dip", "dip-bench-bent-knee"), MixSlot("row", "inverted-row-bent-knees"))

    private val owner = snapshot(
        vp("pushup-uneven", 2), vp("split-squat", 2), vp("chinup-full", 2),                       // 0 of 3 at a new level
        vp("plank-shoulder-tap", 3, listOf(110, 118)), vp("bridge-single-leg", 2, listOf(112)),   // 2 and 1 good sessions
        vp("side-plank", 5, mastered = true, last = 119),                                           // nothing left to earn
        vp("dip-bench-bent-knee", 3, listOf(111, 117)), vp("inverted-row-bent-knees", 2, listOf(113)))

    private fun tap(n: Int, progress: ProgressSnapshot = owner, hand: Set<String> = emptySet(), profile: EquipmentProfile = home, p: List<MixSlot> = plan, excluded: Set<String> = emptySet()) =
        mixItUp(c, progress, profile, p, hand, n, excluded)

    @Test fun firstTapSwapsTheMasteredOneAndTheLastZeroOfThreeInPlanOrder() {
        val t = tap(0)
        assertEquals(listOf("side", "pull"), t.map { it.slotId })
        assertEquals(listOf("back-extension", "anti-rotation"), t.map { it.typeId })
        assertEquals(listOf("superman-hold", "pallof-hold"), t.map { it.toVariationId })
    }

    @Test fun secondTapGivesTheNextPairWithTheNextTypes() {
        val t = tap(1)
        assertEquals(listOf("squat", "push"), t.map { it.slotId })
        assertEquals(listOf("calves", "hip-hinge"), t.map { it.typeId })
        assertEquals(listOf("calf-raise", "rdl-bodyweight"), t.map { it.toVariationId })
    }

    @Test fun dipIsNotSwappedInTheFirstTapsAndEveryReplacementIsATypeMissingFromThePlan() {
        val taps = (0..2).map { tap(it) }
        assertTrue("dip stays", taps.flatten().none { it.slotId == "dip" })
        assertEquals(setOf("back-extension", "anti-rotation", "calves", "hip-hinge", "lateral"), taps.flatten().map { it.typeId }.toSet())
        assertTrue("two per tap", taps.all { it.size == 2 && it[0].typeId != it[1].typeId })
        // the third tap: one good session each (last in plan order first), then the types start again
        assertEquals(listOf("row", "bridge"), taps[2].map { it.slotId })
        assertEquals(listOf("lunge-lateral", "superman-hold"), taps[2].map { it.toVariationId })
        // and the pair after the last one starts at the top again
        assertEquals(tap(0).map { it.slotId }.take(1), tap(4).map { it.slotId }.take(1))
    }

    @Test fun aSlotSwappedOrAddedByHandIsNeverPicked() {
        val t = tap(0, hand = setOf("side", "pull"), p = plan + MixSlot("add-x", "dip-negative"))
        assertEquals(listOf("squat", "push"), t.map { it.slotId })
        assertTrue("added slot", tap(0, p = plan + MixSlot("add-pushup-std", "pushup-standard")).none { it.slotId.startsWith("add-") })
    }

    @Test fun theTypeTrainedLongestAgoComesFirstAndNeverTrainedCountsAsOldest() {
        val p = snapshot(*owner.variations.values.toTypedArray(), vp("superman-hold", 1, last = 95), vp("pallof-hold", 1, last = 96), vp("lunge-lateral", 1, last = 90))
        assertEquals(listOf("calves", "hip-hinge"), tap(0, p).map { it.typeId })
        assertEquals(listOf("lateral", "back-extension"), tap(1, p).map { it.typeId })
        assertEquals(listOf("anti-rotation", "calves"), tap(2, p).map { it.typeId })
    }

    @Test fun aTypeAlreadyInThePlanIsSkipped() {
        val p = plan.dropLast(1) + MixSlot("cf", "calf-raise") + MixSlot("hinge", "rdl-bodyweight")
        val types = (0..3).flatMap { tap(it, p = p) }.map { it.typeId }.toSet()
        assertFalse("calves", "calves" in types); assertFalse("hip-hinge", "hip-hinge" in types)
    }

    @Test fun withinATypeThePersonsOwnStepIsUsedElseTheEntryExercise() {
        val archHold = c.variation("arch-hold-y")!!
        val p = snapshot(*owner.variations.values.toTypedArray(), vp("arch-hold-y", 2, last = 118), active = mapOf(archHold.familyId to "arch-hold-y"))
        assertEquals("arch-hold-y", (0..4).flatMap { tap(it, p) }.first { it.typeId == "back-extension" }.toVariationId)
        assertEquals("superman-hold", tap(0).first { it.typeId == "back-extension" }.toVariationId)
        // the person trained the single-leg step of the hinge route: that one is brought in, not the bodyweight entry
        val sl = snapshot(*owner.variations.values.toTypedArray(), vp("rdl-bodyweight", 5, mastered = true, last = 100), vp("single-leg-deadlift", 1, last = 118), active = mapOf("rdl" to "single-leg-deadlift"))
        assertEquals("single-leg-deadlift", (0..4).flatMap { tap(it, sl) }.first { it.typeId == "hip-hinge" }.toVariationId)
    }

    @Test fun equipmentAndTheExcludedListApply() {
        val none = EquipmentProfile("none", "None", emptyList(), emptySet())
        val types = (0..4).flatMap { tap(it, profile = none, p = plan.filter { s -> s.slotId != "pull" }) }.map { it.typeId }.toSet()
        assertFalse("pallof needs a band", "anti-rotation" in types)
        val ex = (0..4).flatMap { tap(it, excluded = setOf("calf-raise")) }.map { it.typeId }.toSet()
        assertFalse("calf-raise excluded", "calves" in ex)
    }

    @Test fun aSmallPlanOrFewTypesSwapsFewer() {
        assertEquals(1, tap(0, p = plan.take(1)).size)
        assertTrue("empty plan", tap(0, p = emptyList()).isEmpty())
    }

    // ---- what is stored ----

    @Test fun mixIsStoredAsSwapsAndUndoRemovesOnlyThem() {
        val hand = PlanEdits(swaps = mapOf("row" to "inverted-row"), tierOverrides = mapOf("dip" to 2))
        val mixed = hand.withMix(tap(0, hand = hand.handSlotIds()), today)
        assertEquals(mapOf("row" to "inverted-row", "side" to "superman-hold", "pull" to "pallof-hold"), mixed.swaps)
        assertEquals(1, mixed.mixTaps); assertEquals(today, mixed.mixEpochDay)
        assertEquals(setOf("row"), mixed.handSlotIds())
        // a second tap replaces the first pair, it does not pile up
        val again = mixed.withMix(tap(mixed.mixTaps, hand = mixed.handSlotIds()), today)
        assertEquals(mapOf("row" to "inverted-row", "squat" to "calf-raise", "push" to "rdl-bodyweight"), again.swaps)
        assertEquals(2, again.mixTaps)
        // undo: the plan before any mix, hand edits stay
        assertEquals(hand, again.withoutMix())
        assertEquals(PlanEdits(), PlanEdits().withMix(tap(0), today).withoutMix())
    }

    @Test fun aHandSwapOnAMixedSlotTakesItOver() {
        val mixed = PlanEdits().withMix(tap(0), today)
        val handed = mixed.copy(swaps = mixed.swaps + ("side" to "side-plank-knee"))
        assertEquals(setOf("side"), handed.handSlotIds())
        assertEquals(mapOf("pull" to "pallof-hold"), handed.mixOwned())
        assertEquals(mapOf("side" to "side-plank-knee"), handed.withoutMix().swaps)
    }

    @Test fun aMixIsForThatDayOnly() {
        val mixed = PlanEdits(tierOverrides = mapOf("dip" to 2)).withMix(tap(0), today)
        assertSame(mixed, mixed.expireMix(today))
        val next = mixed.expireMix(today + 1)
        assertEquals(PlanEdits(tierOverrides = mapOf("dip" to 2)), next)
        assertEquals(PlanEdits(), PlanEdits().expireMix(today + 5))
    }

    @Test fun savedRoutineKeepsTheExercisesButNotTheMixMarks() {
        val mixed = PlanEdits().withMix(tap(0), today)
        val base = app.calisthenics.domain.routine.StarterRoutine.routine
        val saved = app.calisthenics.domain.routine.savedFrom("id", "Mixed", 1, base, mixed, TrainSettings(), base.rule, 0L)
        assertTrue("no mix marks", saved.edits.mixSwaps.isEmpty() && saved.edits.mixTaps == 0 && saved.edits.mixEpochDay == 0L && saved.edits.swaps.isEmpty())
    }

    @Test fun draftsWrittenBeforeThisFieldStillDecodeAndTheMixSurvivesARoundTrip() {
        val json = Json { ignoreUnknownKeys = true }
        assertEquals(PlanEdits(), json.decodeFromString(PlanEdits.serializer(), "{}"))
        val mixed = PlanEdits().withMix(tap(0), today)
        assertEquals(mixed, json.decodeFromString(PlanEdits.serializer(), json.encodeToString(PlanEdits.serializer(), mixed)))
    }

    @Test fun aMixedSwapIsPlannedWithoutTheCheckItWarning() {
        val base = app.calisthenics.domain.routine.StarterRoutine.routine
        val settings = TrainSettings(exercisesPerRound = 6)
        val usual = buildTrainPlan(c, base, ProgressSnapshot(emptyMap(), emptyMap(), emptyMap(), emptyList()), home, settings) as PlanResult.Ready
        val slots = usual.plan.blocks.filter { it.type == app.calisthenics.domain.model.BlockType.WORK && (it.roundIndex ?: 1) == 1 && it.slotId != null && it.variationId != null }
            .distinctBy { it.slotId }.map { MixSlot(it.slotId!!, it.variationId!!) }
        val picks = mixItUp(c, ProgressSnapshot(emptyMap(), emptyMap(), emptyMap(), emptyList()), home, slots, emptySet(), 0)
        assertEquals(2, picks.size)
        val mixed = PlanEdits().withMix(picks, today)
        val r = buildTrainPlan(c, base, ProgressSnapshot(emptyMap(), emptyMap(), emptyMap(), emptyList()), home, settings, edits = mixed) as PlanResult.Ready
        val ids = r.plan.blocks.mapNotNull { it.variationId }
        picks.forEach { pick -> assertTrue("${pick.toVariationId} is planned", pick.toVariationId in ids); assertFalse("${pick.fromVariationId} is gone", pick.fromVariationId in ids) }
        assertTrue("no cross-type warning: ${r.plan.warnings}", r.plan.warnings.none { it.contains("different kind of exercise") })
        // the same swaps by hand do warn (unchanged behaviour)
        val byHand = buildTrainPlan(c, base, ProgressSnapshot(emptyMap(), emptyMap(), emptyMap(), emptyList()), home, settings, edits = PlanEdits(swaps = mixed.swaps)) as PlanResult.Ready
        assertTrue("hand swap still warns", byHand.plan.warnings.any { it.contains("different kind of exercise") })
    }
}
