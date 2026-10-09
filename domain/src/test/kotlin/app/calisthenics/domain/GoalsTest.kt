package app.calisthenics.domain

import app.calisthenics.domain.content.parseCatalog
import app.calisthenics.domain.goals.*
import app.calisthenics.domain.model.*
import app.calisthenics.domain.progression.ProgressionEngine
import app.calisthenics.domain.progression.UserAction
import app.calisthenics.domain.routine.StarterRoutine
import org.junit.Assert.*
import org.junit.Test
import java.io.File

class GoalsTest {
    private val c = parseCatalog(File(System.getProperty("repo.root"), "content/starter/catalog.json").readText())
    private fun snap(vararg a: Pair<String, Int>) = ProgressionEngine(c).replay(emptyList(), a.map { UserAction.SelfAssessment(0, it.first, it.second) })

    @Test fun everyGoalChainResolvesInTheCatalog() {
        for (g in Goals.all.filter { it.entryVariationId != null }) {
            val ch = Goals.chain(c, g)
            assertTrue(g.id, ch.size >= 3)
            assertEquals(g.targetVariationId, ch.last())
        }
        assertTrue(Goals.all.size >= 10)
    }
    @Test fun hspuGoalTrainsWhatUnlocksTheNextStep() {
        // standard push-up only at tier 1 -> pike push-up (needs pushup-standard tier 3) is locked -> train push-ups
        val r = Goals.routineFor(c, StarterRoutine.routine, "hspu", snap("pushup-standard" to 1))
        assertEquals("pushup-standard", r.slots.first().preferredVariationId)
        val r2 = Goals.routineFor(c, StarterRoutine.routine, "hspu", snap("pushup-standard" to 4))
        assertEquals("pike-pushup", r2.slots.first().preferredVariationId)
    }
    @Test fun goalSlotReplacesTheMatchingBaseSlotAndKeepsTheRest() {
        val r = Goals.routineFor(c, StarterRoutine.routine, "pistol", snap())
        assertEquals(StarterRoutine.routine.slots.size, r.slots.size)
        assertEquals("goal", r.slots.first().id)
    }
    @Test fun generalAndBodyPartGoalsLeaveRoutineAlone() {
        assertEquals(StarterRoutine.routine.slots, Goals.routineFor(c, StarterRoutine.routine, "general", snap()).slots)
        assertEquals(setOf(StrengthFocus.CORE), Goals.focusFor("body-core"))
    }
    @Test fun nodeStatesLockedAvailableMastered() {
        val s = snap("pushup-standard" to 4, "pike-pushup" to 5)
        val st = Goals.treeStates(c, Goals.byId("hspu")!!, s)
        assertEquals(NodeState.AVAILABLE, st["pike-pushup"]) // not mastered (tier 5 not achieved), prereqs met
        assertEquals(NodeState.LOCKED, st["wall-handstand-hold"])
    }
}

class ObjectiveTypesTest {
    @Test fun skillGoalsAndBodyPartsAreSeparatedAndRoutineIsHiddenUntilPresetsExist() {
        assertTrue(Goals.goalsOf(ObjectiveType.SKILL).all { it.entryVariationId != null })
        assertTrue(Goals.goalsOf(ObjectiveType.BODY_PART).map { it.id }.containsAll(listOf("general", "body-upper", "body-lower", "body-core")))
        assertEquals(Goals.all.size, Goals.goalsOf(ObjectiveType.SKILL).size + Goals.goalsOf(ObjectiveType.BODY_PART).size)
        assertTrue(Goals.goalsOf(ObjectiveType.ROUTINE).isEmpty())
        assertEquals(listOf(ObjectiveType.BODY_PART, ObjectiveType.SKILL), Goals.availableTypes(false))
        assertEquals(listOf(ObjectiveType.BODY_PART, ObjectiveType.SKILL, ObjectiveType.ROUTINE), Goals.availableTypes(true))
        assertEquals(ObjectiveType.SKILL, Goals.typeOf(Goals.byId("planche")!!))
    }
}

class AssumedStarsTest {
    @Test fun startingAtLevelThreeShowsTwoStarsAndEarningLevelThreeMakesThree() {
        val engine = app.calisthenics.domain.progression.ProgressionEngine(Fx.catalog())
        val s = engine.replay(emptyList(), listOf(UserAction.SelfAssessment(0, "pushup-knee", 3)))
        assertEquals(2, s.variations.getValue("pushup-knee").earnedStars())
        assertEquals(0, engine.replay(emptyList(), listOf(UserAction.SelfAssessment(0, "pushup-knee", 1))).variations.getValue("pushup-knee").earnedStars())
        val p = s.variations.getValue("pushup-knee")
        assertEquals(3, p.copy(achievedTiers = setOf(3)).earnedStars())
    }
}
