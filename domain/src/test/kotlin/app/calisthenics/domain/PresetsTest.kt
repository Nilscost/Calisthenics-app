package app.calisthenics.domain

import app.calisthenics.domain.equipment.SeedProfiles
import app.calisthenics.domain.model.*
import app.calisthenics.domain.planner.*
import app.calisthenics.domain.routine.*
import org.junit.Assert.*
import org.junit.Test
import java.io.File

/** V22: the ready-made routines match plan section 3 and docs/research/rr-live-check.md. */
class PresetsTest {
    private val catalog = app.calisthenics.domain.content.parseCatalog(File(System.getProperty("repo.root"), "content/starter/catalog.json").readText())
    private val none = app.calisthenics.domain.progression.ProgressSnapshot(emptyMap(), emptyMap(), emptyMap(), emptyList())
    private val gym = EquipmentProfile("gym", "Gym", listOf(
        EquipmentItem("pullup-bar"), EquipmentItem("low-bar"), EquipmentItem("dip-support"), EquipmentItem("foot-anchor"), EquipmentItem("mat"),
        EquipmentItem("resistance-band", strengthLabel = "assorted", suitability = setOf("stable-anchor"))), setOf("floor-space", "wall"))
    private val bare = EquipmentProfile("bare", "Bare", emptyList(), setOf("floor-space"))

    private fun plan(p: SavedRoutine, profile: EquipmentProfile): WorkoutPlan {
        val r = resolveSaved(p, settingsFor(p, TrainSettings()))
        return (buildTrainPlan(catalog, r.routine, none, profile, r.settings, edits = r.edits) as PlanResult.Ready).plan
    }

    @Test fun presetsExistAndAreCredited() {
        val all = Presets.all(catalog, gym)
        assertEquals(listOf(Presets.RR, Presets.MINIMALIST), all.map { it.id })
        assertTrue(all.all { Presets.isPreset(it.id) && !it.credit.isNullOrBlank() })
        assertTrue(Presets.byId("preset-nope", catalog, gym) == null && Presets.byId(null, catalog, gym) == null)
        // every exercise a preset names exists and has a policy
        for (p in all + Presets.all(catalog, bare)) for (s in p.routine.slots) assertNotNull(s.preferredVariationId, catalog.policyForVariation(s.preferredVariationId))
    }

    @Test fun recommendedRoutineIsThreePairsAndACoreTriplet() {
        val rr = Presets.recommended(catalog, gym)
        assertEquals(WorkoutFormat.PAIRS, rr.format); assertFalse(rr.stretchOn); assertTrue(rr.warmupOn); assertEquals(3, rr.sets)
        assertEquals(RuleKind.REP_RANGE, rr.rule.kind); assertEquals(5, rr.rule.from); assertEquals(8, rr.rule.to); assertEquals(30, rr.rule.holdTo)
        assertEquals(listOf(1, 1, 2, 2, 3, 3, 4, 4, 4), rr.routine.slots.map { it.group })
        assertEquals(listOf(90, 90, 90, 90, 90, 90, 60, 60, 60), rr.routine.slots.map { it.restSeconds })
        val p = plan(rr, gym)
        assertEquals(3, p.rounds); assertEquals(WorkoutFormat.PAIRS, p.format)
        // order: pair 1 alternates pull-up / squat for 3 sets, then pair 2, pair 3, then the three core exercises
        val work = p.blocks.filter { it.type == BlockType.WORK }
        assertEquals(listOf("rr-pull", "rr-squat", "rr-pull", "rr-squat", "rr-pull", "rr-squat"), work.take(6).map { it.slotId })
        assertEquals(listOf(1, 1, 1, 1, 1, 1, 2, 2, 2, 2, 2, 2, 3, 3, 3, 3, 3, 3) + List(12) { 4 }, work.map { it.groupIndex })
        assertEquals(listOf("rr-core-ext", "rr-core-rot", "rr-core-rot", "rr-core-back"), work.takeLast(12).take(4).map { it.slotId }) // the Pallof press is one side after the other
        // rests: 90 s after every pair set, 60 s in the triplet, plain rest (no stretches), none after the very last set
        val rests = p.blocks.filter { it.type == BlockType.PASSIVE_RECOVERY }
        assertTrue(p.blocks.none { it.type == BlockType.STRETCH })
        assertEquals(setOf(90, 60), rests.map { it.durationSeconds }.toSet())
        assertTrue(rests.filter { it.groupIndex == 4 }.all { it.durationSeconds == 60 } && rests.filter { it.groupIndex != 4 }.all { it.durationSeconds == 90 })
        assertEquals(9 * 3 - 1, rests.size) // after every set of every exercise except the very last (both sides of a one-sided exercise are one set)
        // targets: pairs 5-8, core triplet 8-12, holds 10-30
        assertTrue(work.filter { it.groupIndex != 4 && it.target?.type == TargetType.REPS }.all { it.target?.value == 5 && it.target?.max == 8 })
        assertTrue(work.filter { it.slotId == "rr-core-rot" || it.slotId == "rr-core-back" }.all { it.target?.value == 8 && it.target?.max == 12 })
        assertTrue(work.filter { it.target?.type == TargetType.HOLD_SECONDS }.all { it.target?.value == 10 && it.target?.max == 30 })
    }

    @Test fun recommendedRoutineUsesTheEquipmentTheProfileHas() {
        val withGear = Presets.recommended(catalog, gym).routine.slots.associate { it.id to it.preferredVariationId }
        assertEquals("pullup-band-assisted", withGear["rr-pull"]); assertEquals("dip-support-hold", withGear["rr-dip"])
        assertEquals("inverted-row-bent-knees", withGear["rr-row"]); assertEquals("pallof-press", withGear["rr-core-rot"])
        val noBand = gym.copy(items = gym.items.filter { it.equipmentId != "resistance-band" })
        assertEquals("side-plank", Presets.recommended(catalog, noBand).routine.slots.first { it.id == "rr-core-rot" }.preferredVariationId) // Copenhagen path without a band
        assertEquals(Pattern.CORE_ANTI_LATERAL, Presets.recommended(catalog, noBand).routine.slots.first { it.id == "rr-core-rot" }.intent)
        val bandOnly = bare.copy(items = listOf(EquipmentItem("resistance-band", strengthLabel = "assorted", suitability = setOf("stable-anchor"))))
        assertEquals("row-band", Presets.recommended(catalog, bandOnly).routine.slots.first { it.id == "rr-row" }.preferredVariationId)
        // bodyweight only still gives a plan (the planner substitutes and says so)
        assertTrue(plan(Presets.recommended(catalog, bare), bare).blocks.any { it.type == BlockType.WORK })
    }

    @Test fun warmUpListsTheRrItemsAndOnlyWhatTheEquipmentAllows() {
        val full = Presets.recommended(catalog, gym).routine.warmup
        assertEquals(listOf("warmup-shoulder-band", "warmup-squat-sky-reach", "warmup-wrist-prep", "warmup-dead-bug", "warmup-arch-hang", "warmup-support-hold"), full)
        assertEquals(4, Presets.recommended(catalog, bare).routine.warmup.size)
        val p = plan(Presets.recommended(catalog, gym), gym)
        assertEquals(full, p.blocks.filter { it.type == BlockType.WARMUP }.mapNotNull { it.variationId })
        assertTrue(p.blocks.indexOfFirst { it.type == BlockType.WARMUP } < p.blocks.indexOfFirst { it.type == BlockType.WORK })
    }

    @Test fun minimalistIsACircuitWithLittleRest() {
        val m = Presets.minimalist(catalog, gym)
        assertEquals(WorkoutFormat.CIRCUIT, m.format); assertFalse(m.stretchOn); assertFalse(m.warmupOn)
        assertEquals(listOf("walking-lunge", "pushup-incline", "inverted-row-bent-knees", "plank-shoulder-tap"), m.routine.slots.map { it.preferredVariationId })
        assertEquals(8, m.rule.from); assertEquals(10, m.rule.to); assertEquals(3, m.rule.sets)
        val p = plan(m, gym)
        val work = p.blocks.filter { it.type == BlockType.WORK }
        assertEquals(3, p.rounds); assertEquals(12, work.size)
        assertEquals(listOf("min-lunge", "min-push", "min-row", "min-tap"), work.take(4).map { it.slotId })
        assertTrue(p.blocks.none { it.type == BlockType.PASSIVE_RECOVERY || it.type == BlockType.STRETCH }) // no rest between exercises, no stretch blocks
        // 2-6 circuits through the Custom rule
        for (n in 2..6) {
            val r = resolveSaved(m, settingsFor(m, TrainSettings()))
            val rule = m.rule.copy(kind = RuleKind.CUSTOM, sets = n)
            val q = (buildTrainPlan(catalog, r.routine, none, gym, r.settings, edits = r.edits.copy(rule = rule)) as PlanResult.Ready).plan
            assertEquals(n, q.rounds)
        }
    }

    @Test fun rangesAreJudgedAgainstWhatThePlanAsked() {
        val p = plan(Presets.recommended(catalog, gym), gym)
        val (bottom, top) = app.calisthenics.domain.progression.plannedRange(p, "pallof-press", false)
        assertEquals(8 to 12, bottom to top)
        assertEquals(5 to 8, app.calisthenics.domain.progression.plannedRange(p, "squat-air", false))
        assertTrue(app.calisthenics.domain.progression.ruleMet(p.rule, false, listOf(12, 12, 12), 12))
        assertFalse(app.calisthenics.domain.progression.ruleMet(p.rule, false, listOf(8, 8, 8), 12)) // the pairs' top is not enough in the triplet
    }

    @Test fun oldSlotsAndRoutinesStillDecode() {
        val j = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }
        val s = j.decodeFromString(RoutineSlot.serializer(), """{"id":"a","intent":"SQUAT","area":"LOWER_BODY","preferredVariationId":"squat-air"}""")
        assertTrue(s.group == null && s.restSeconds == null && s.repFrom == null)
        assertTrue(j.decodeFromString(Routine.serializer(), """{"id":"r","revision":1,"name":"x","slots":[]}""").warmup.isEmpty())
    }
}
