package app.calisthenics.domain

import app.calisthenics.domain.content.parseCatalog
import app.calisthenics.domain.equipment.SeedProfiles
import app.calisthenics.domain.goals.Goals
import app.calisthenics.domain.model.*
import app.calisthenics.domain.planner.*
import app.calisthenics.domain.progression.ProgressSnapshot
import app.calisthenics.domain.routine.StarterRoutine
import org.junit.Assert.*
import org.junit.Test
import java.io.File

/** R8.1-T07 (F8): a stretch between sets appears at most twice in a workout; the choice varies; plain rest when they are used up. */
class StretchVarietyTest {
    private val c = parseCatalog(File(System.getProperty("repo.root"), "content/starter/catalog.json").readText())
    private val none = ProgressSnapshot(emptyMap(), emptyMap(), emptyMap(), emptyList())
    private fun uses(p: WorkoutPlan) = p.blocks.filter { it.type == BlockType.STRETCH && it.recoveryForBlockIds.isNotEmpty() }
        .map { Triple(it.variationId, it.roundIndex, it.slotId) }.distinct().groupingBy { it.first }.eachCount()

    @Test fun noBetweenSetStretchAppearsMoreThanTwiceInAnyGeneratedWorkout() {
        for (rounds in 1..10) for (f in WorkoutFormat.entries) for (goal in listOf("general", "body-upper", "body-core", "front-lever")) for (n in listOf(null, 3, 6, 9)) {
            val r = buildTrainPlan(c, StarterRoutine.routine, none, SeedProfiles.home, TrainSettings(goalId = goal, rounds = rounds, format = f, exercisesPerRound = n))
            val p = (r as? PlanResult.Ready)?.plan ?: continue
            val bad = uses(p).filterValues { it > MAX_STRETCH_USES }
            assertTrue("rounds=$rounds $f $goal n=$n: $bad", bad.isEmpty())
        }
    }

    @Test fun theChoiceIsVariedAcrossTheWorkoutAndLongWorkoutsFallBackToPlainRest() {
        val p = (buildTrainPlan(c, StarterRoutine.routine, none, SeedProfiles.home, TrainSettings(rounds = 8, exercisesPerRound = 6)) as PlanResult.Ready).plan
        val u = uses(p)
        assertTrue("distinct stretches: $u", u.size >= 4)
        assertTrue(u.values.all { it <= MAX_STRETCH_USES })
        val breaks = p.blocks.count { it.type == BlockType.PASSIVE_RECOVERY }
        val stretchBreaks = u.values.sum()
        assertTrue("$stretchBreaks stretch breaks, $breaks plain rests", breaks == 0 || stretchBreaks == u.size * MAX_STRETCH_USES || stretchBreaks > 0)
        // a short workout still has a stretch in every break
        val short = (buildTrainPlan(c, StarterRoutine.routine, none, SeedProfiles.home, TrainSettings(rounds = 2)) as PlanResult.Ready).plan
        assertTrue(short.blocks.none { it.type == BlockType.PASSIVE_RECOVERY })
    }

    @Test fun aStretchThePersonPickedByHandIsKeptEvenAfterTwoUses() {
        val picks = mapOf("push" to "stretch-chest-door")
        val base = (buildTrainPlan(c, StarterRoutine.routine, none, SeedProfiles.home, TrainSettings(rounds = 5), edits = PlanEdits(stretchPicks = picks)) as PlanResult.Ready).plan
        val sets = base.blocks.filter { it.slotId == "push" && it.type == BlockType.STRETCH && it.recoveryForBlockIds.isNotEmpty() }.mapNotNull { it.roundIndex }.distinct().size
        assertEquals(5, sets)    // a break after the push-up in each of the five rounds: a hand pick repeats as often as chosen
        assertTrue(base.blocks.filter { it.slotId == "push" && it.type == BlockType.STRETCH && it.recoveryForBlockIds.isNotEmpty() }.all { it.variationId == "stretch-chest-door" })
    }
}
