package app.calisthenics.domain

import app.calisthenics.domain.content.parseCatalog
import app.calisthenics.domain.equipment.SeedProfiles
import app.calisthenics.domain.model.*
import app.calisthenics.domain.planner.*
import app.calisthenics.domain.routine.SessionDraft
import app.calisthenics.domain.routine.StarterRoutine
import app.calisthenics.domain.session.CueText
import org.junit.Assert.*
import org.junit.Test
import java.io.File

/** U03 (F11, F13, Q4): hold blocks last target + 3 s; with stretch on there is never a "Rest". */
class HoldTimingTest {
    private val catalog = parseCatalog(File(System.getProperty("repo.root"), "content/starter/catalog.json").readText())
    private val names = catalog.variations.associate { it.id to it.name }

    private fun plan(timed: Boolean = false, stretch: Boolean = true, rounds: Int = 2): WorkoutPlan = (generate(PlanInput(
        "p", 0L, 0, catalog, StarterRoutine.routine,
        SessionDraft.from(Preferences(stretchOn = stretch), StarterRoutine.routine).copy(rounds = rounds, timed = timed), SeedProfiles.home)) as PlanResult.Ready).plan

    private fun holdWork(p: WorkoutPlan) = p.blocks.filter { it.type == BlockType.WORK && it.target?.type == TargetType.HOLD_SECONDS }

    @Test fun everyHoldTierIsTargetPlusThreeSeconds() {
        for (pol in catalog.policies) for (t in pol.tiers) if (t.target.type == TargetType.HOLD_SECONDS)
            assertEquals("${pol.variationId} tier ${t.index}", t.target.value + 3, t.workWindowSeconds)
    }

    @Test fun fifteenSecondPlankBlockIsEighteenSeconds() {
        for (stretch in listOf(true, false)) {
            val plank = plan(stretch = stretch).blocks.first { it.variationId == "plank" && it.type == BlockType.WORK }
            assertEquals(Target(TargetType.HOLD_SECONDS, 15), plank.target)
            assertEquals(18, plank.durationSeconds)
        }
    }

    @Test fun timedModeDoesNotPadHoldsAndKeepsTheSixtySecondCycle() {
        val p = plan(timed = true, stretch = false)
        val holds = holdWork(p)
        assertTrue(holds.isNotEmpty())
        for (h in holds) {
            assertEquals(h.target!!.value + 3, h.durationSeconds)
            // the part of the 60 s the hold does not use is added to the recovery that follows it
            val rec = p.blocks.firstOrNull { it.recoveryForBlockIds.contains(h.id) } ?: continue
            val used = p.blocks.filter { it.id in rec.recoveryForBlockIds }.sumOf { it.durationSeconds }
            assertEquals(TIMED_REST_SECONDS + (TIMED_WORK_SECONDS - used).coerceAtLeast(0), rec.durationSeconds)
        }
        // rep blocks still fill 60 s
        assertTrue(p.blocks.filter { it.type == BlockType.WORK && it.target?.type == TargetType.REPS && it.side == Side.NONE }.all { it.durationSeconds == TIMED_WORK_SECONDS })
    }

    @Test fun withStretchOnNoBlockOrCueIsCalledRest() {
        for (timed in listOf(false, true)) {
            val p = plan(timed = timed, stretch = true)
            assertTrue(p.blocks.none { it.type == BlockType.PASSIVE_RECOVERY })
            for (b in p.blocks) {
                val l = CueText.label(p, b.id, names)
                assertFalse("'${l}' for ${b.type}", l.contains("rest", ignoreCase = true))
                CueText.nextLabel(p, b.id, names)?.let { assertFalse("next after ${b.id}: $it", it.contains("rest", ignoreCase = true) || it.startsWith("Get ready")) }
            }
        }
    }

    @Test fun transitionsReadGetReadyWithTheNextExerciseAndRestOnlyExistsWithoutStretch() {
        val on = plan(stretch = true)
        val tr = on.blocks.firstOrNull { it.type == BlockType.TRANSITION }
        assertNotNull("this routine has a position change", tr)
        val next = on.blocks.drop(on.blocks.indexOf(tr) + 1).first { it.variationId != null }
        assertEquals("Get ready: ${names[next.variationId]}", CueText.label(on, tr!!.id, names))
        val off = plan(stretch = false)
        assertTrue(off.blocks.any { it.type == BlockType.PASSIVE_RECOVERY })
        assertEquals("Rest", CueText.label(off, off.blocks.first { it.type == BlockType.PASSIVE_RECOVERY }.id, names))
    }
}
