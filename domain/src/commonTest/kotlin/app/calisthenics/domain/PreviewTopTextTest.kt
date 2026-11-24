package app.calisthenics.domain

import app.calisthenics.domain.content.parseCatalog
import app.calisthenics.domain.equipment.SeedProfiles
import app.calisthenics.domain.feedback.SessionEvidence
import app.calisthenics.domain.model.*
import app.calisthenics.domain.planner.*
import app.calisthenics.domain.progression.*
import app.calisthenics.domain.routine.StarterRoutine
import kotlin.test.Test

/**
 * 9.4 L3 (docs/44 F6, wording of F4): the lines above the Preview list are few and short. Real starter catalog; the progress is rebuilt from
 * self-assessments and workout counts, never from the owner's private backup.
 */
class PreviewTopTextTest {
    private val catalog = parseCatalog(fixtureText("content/starter/catalog.json"))
    private val profile = SeedProfiles.home.copy(items = SeedProfiles.home.items + EquipmentItem("low-bar") + EquipmentItem("dip-support"))

    /** The owner's situation of 20 Nov: Front Plank and Glute Bridge finished earlier, the alternates rotating in (odd workout counts). */
    private fun ownerProgress(sessionsOf: List<String> = emptyList()): ProgressSnapshot {
        val actions = listOf("plank" to 6, "glute-bridge" to 6, "pushup-archer" to 4, "pullup-full" to 3, "inverted-row" to 4, "dip-parallel" to 3, "side-plank" to 5).map { selfAssessmentFor(it.first, it.second) }
        val evidence = sessionsOf.map { id -> SessionEvidence("s-$id", id, catalog.variation(id)!!.familyId, 1, 2, 2, false, false, false, null, false, 2, 0, 2) }
        val base = ProgressionEngine(catalog).replay(evidence, actions)
        return base.copy(routeWorkoutCounts = catalog.progressionRoutes.associate { it.id to 5 }, familyWorkoutCounts = catalog.variations.associate { it.familyId to 5 })
    }

    private fun plan(progress: ProgressSnapshot, day: Int = 2) =
        (buildTrainPlan(catalog, StarterRoutine.routine, progress, profile, TrainSettings(), nowDay = day) as PlanResult.Ready).plan

    @Test fun `20 Nov example, before and after`() {
        // before (9.3), as the owner saw it, one sentence each:
        //   You finished every level of Front Plank, so today you do Long-Lever (RKC) Plank.   (Front Plank was finished weeks ago)
        //   You finished every level of Glute Bridge, so today you do Single-Leg Glute Bridge (per side).
        //   Uneven Push-Up (per side) is today's variation instead of Archer Push-Up. Each exercise keeps its own level.
        //   Plank Shoulder Taps is today's variation instead of Long-Lever (RKC) Plank. Each exercise keeps its own level.   (and one more per rotation)
        val first = plan(ownerProgress())
        val firstTexts = first.notes.map { it.text }
        assertTrue(firstTexts.toString(), "New: Single-Leg Glute Bridge (per side), now that you finished Glute Bridge." in firstTexts)
        assertTrue(firstTexts.toString(), firstTexts.none { it.contains("finished every level") || it.contains("Each exercise keeps its own level") })
        // after: every rotation in ONE line
        val rotation = first.notes.filter { it.kind == NoteKind.ROTATION }.map { it.text }
        assertEquals(listOf("Swapped in today: Uneven Push-Up (per side) (for Archer Push-Up), Plank Shoulder Taps (for Long-Lever (RKC) Plank), " +
            "Russian Twist (for Side Plank (per side))."), rotation)
        assertEquals(listOf("pushup-uneven", "plank-shoulder-tap", "russian-twist"), first.swaps.map { it.variationId })
        assertTrue(first.swaps.all { it.kind == SwapKind.ROTATED })
        assertEquals(rotation.single(), "${SwapKind.ROTATED.lineLabel}: " + first.swaps.joinToString { "${catalog.variation(it.variationId)!!.name} (for ${catalog.variation(it.replacedId)!!.name})" } + ".")
    }

    @Test fun `the finished-every-level note appears only in the first plan after the move-on`() {
        val first = plan(ownerProgress())
        assertEquals(1, first.notes.count { it.text.startsWith("New: ") })
        // one session of the new exercise later, the note is gone
        val later = plan(ownerProgress(listOf("bridge-single-leg")))
        assertTrue(later.notes.map { it.text }.toString(), later.notes.none { it.text.startsWith("New: ") })
    }

    @Test fun `a move-on to an exercise that is rotated away says nothing about it`() {
        // Front Plank finished: today's slot is an alternate, so there is no "New: Long-Lever" claim either
        val p = plan(ownerProgress())
        assertTrue(p.notes.none { it.text.contains("Front Plank") })
    }

    @Test fun `notes are ordered actions first, then re-entry, rotation, rest, and the Preview shows at most three`() {
        val kinds = NoteKind.entries
        assertTrue(kinds.indexOf(NoteKind.EQUIPMENT) < kinds.indexOf(NoteKind.RETURN))
        assertTrue(kinds.indexOf(NoteKind.PAIN) < kinds.indexOf(NoteKind.RETURN))
        assertTrue(kinds.indexOf(NoteKind.MISSING_MOVEMENT) < kinds.indexOf(NoteKind.RETURN))
        assertTrue(kinds.indexOf(NoteKind.RETURN) < kinds.indexOf(NoteKind.ROTATION))
        assertTrue(kinds.indexOf(NoteKind.ROTATION) < kinds.indexOf(NoteKind.INFO))
        // a plan with a missing item (no low bar), a rotation and a move-on: equipment first, rotation next, info last
        val noBar = SeedProfiles.home.copy(items = SeedProfiles.home.items + EquipmentItem("dip-support"))
        val p = (buildTrainPlan(catalog, StarterRoutine.routine, ownerProgress(), noBar, TrainSettings(), nowDay = 2) as PlanResult.Ready).plan
        val order = p.notes.map { it.kind }
        assertEquals(order.sortedBy { it.priority }, order)
        assertEquals(NoteKind.EQUIPMENT, order.first())
        assertTrue(order.toString(), order.indexOf(NoteKind.ROTATION) < order.indexOf(NoteKind.INFO))
        assertEquals(3, p.topNotes().size)
        assertEquals(NoteKind.ROTATION, p.topNotes()[1].kind)
    }
}
