package app.calisthenics.domain

import app.calisthenics.domain.feedback.*
import app.calisthenics.domain.model.*
import app.calisthenics.domain.model.Target
import app.calisthenics.domain.progression.*
import org.junit.Assert.*
import org.junit.Test

/** V21 (O2, owner answer Q1): App levels / Rep range / Custom. */
class ProgressionRuleTest {
    private val rr = ProgressionRule(RuleKind.REP_RANGE, sets = 3, from = 5, to = 8)

    @Test fun unloggedSetsCountAsTheSameNumbersAsLastTime() {
        assertEquals(listOf(8, 6, 7), resolveSetValues(listOf(8, null, null), listOf(7, 6, 7), 5))
        assertEquals(listOf(5, 5, 5), resolveSetValues(listOf(null, null, null), null, 5)) // no last time: the bottom, never the top
        assertEquals(listOf(8, 8, 8), resolveSetValues(listOf(8, 8, 8), listOf(5, 5, 5), 5))
    }

    @Test fun aSessionMeetsTheRuleOnlyWhenEverySetReachesTheTop() {
        assertTrue(ruleMet(rr, false, listOf(8, 8, 8)))
        assertTrue(ruleMet(rr, false, listOf(9, 8, 10)))
        assertFalse(ruleMet(rr, false, listOf(8, 8, 7)))
        assertFalse(ruleMet(rr, false, listOf(8, 8)))                 // fewer sets than planned
        assertFalse(ruleMet(ProgressionRule(), false, listOf(8, 8, 8))) // App levels never uses this
        assertTrue(ruleMet(rr, true, listOf(30, 30, 31))); assertFalse(ruleMet(rr, true, listOf(30, 25, 30))) // holds go on at 30 s
    }

    @Test fun silenceAfterAGoodSessionDoesNotMoveYouOn() {
        // last time 5,5,5; this time nothing logged -> 5,5,5 -> not met; the alternative (the top) would have moved you on after one silent session
        val v = resolveSetValues(listOf(null, null, null), listOf(5, 5, 5), 5)
        assertFalse(ruleMet(rr, false, v))
    }

    @Test fun customHasEditableSessionsAndRangeRepRangeKeepsOne() {
        assertEquals(1, rr.sessions)
        val c = rr.copy(kind = RuleKind.CUSTOM, sessions = 2, from = 6, to = 10)
        assertEquals(10, ruleTop(c, false)); assertEquals(6, ruleBottom(c, false))
        assertEquals(1, c.withKind(RuleKind.REP_RANGE).sessions)
        assertFalse(ProgressionRule().isRange); assertTrue(c.isRange)
        try { ProgressionRule(from = 9, to = 5); fail() } catch (_: IllegalArgumentException) {}
        assertEquals("5-8 reps", Target(TargetType.REPS, 5, 8).toString()); assertEquals("10-30s hold", Target(TargetType.HOLD_SECONDS, 10, 30).toString()); assertEquals("8 reps", Target(TargetType.REPS, 8).toString())
    }

    // ---- the engine
    private val engine = ProgressionEngine(Fx.catalog())
    private val V = "pushup-knee"
    private fun ev(day: Int, met: Boolean?, sessions: Int = 1) = SessionEvidence("s$day", V, "pushup", day, 1, 3, false, false, false, null, false, 3, 0, 2, ruleMet = met, ruleSessions = sessions)
    private val start = UserAction.SelfAssessment(0, V, 1)

    @Test fun reachingTheTopMovesOnToTheNextExerciseAtItsBottom() {
        val s = engine.replay(listOf(ev(0, false), ev(3, true)), listOf(start))
        assertEquals("pushup-std", s.activeInFamily["pushup"])
        assertEquals(1, s.variations.getValue("pushup-std").tier)
        assertEquals(setOf(1, 2, 3, 4, 5), s.variations.getValue(V).achievedTiers)
        assertTrue(s.events.any { it.message.contains("top of the range") && it.message.contains("starting again at the bottom") })
    }

    @Test fun notReachingTheTopChangesNothing() {
        val s = engine.replay(listOf(ev(0, false), ev(3, false)), listOf(start))
        assertEquals(V, s.activeInFamily["pushup"]); assertTrue(s.variations.getValue(V).achievedTiers.isEmpty())
    }

    @Test fun customNeedsSeveralSessionsInARow() {
        assertEquals(V, engine.replay(listOf(ev(0, true, 2)), listOf(start)).activeInFamily["pushup"])
        assertEquals("pushup-std", engine.replay(listOf(ev(0, true, 2), ev(3, true, 2)), listOf(start)).activeInFamily["pushup"])
        assertEquals(V, engine.replay(listOf(ev(0, true, 2), ev(3, false, 2), ev(6, true, 2)), listOf(start)).activeInFamily["pushup"]) // a miss breaks the row
    }

    @Test fun theGlobalSwitchOffFreezesTheRuleToo() {
        assertEquals(V, engine.replay(listOf(ev(0, true), ev(3, true)), listOf(start), autoProgression = false).activeInFamily["pushup"])
    }

    @Test fun appLevelsEvidenceIsUntouched() {
        val s = engine.replay(listOf(ev(0, null), ev(3, null), ev(7, null)), listOf(start))
        assertEquals(2, s.tierFor(V)) // normal tier progression, not the rule
    }

    // ---- the planner
    @Test fun aRangeRulePutsTheRangeIntoTheTargetsAndRemembersTheRule() {
        val c = app.calisthenics.domain.content.parseCatalog(java.io.File(System.getProperty("repo.root"), "content/starter/catalog.json").readText())
        val none = ProgressSnapshot(emptyMap(), emptyMap(), emptyMap(), emptyList())
        val p = (app.calisthenics.domain.planner.buildTrainPlan(c, app.calisthenics.domain.routine.StarterRoutine.routine, none, app.calisthenics.domain.equipment.SeedProfiles.home,
            app.calisthenics.domain.planner.TrainSettings(rounds = 3), edits = app.calisthenics.domain.planner.PlanEdits(rule = rr)) as app.calisthenics.domain.planner.PlanResult.Ready).plan
        assertEquals(rr, p.rule); assertEquals(3, p.rounds) // the rule's sets
        val reps = p.blocks.filter { it.type == BlockType.WORK && it.target?.type == TargetType.REPS }
        assertTrue(reps.isNotEmpty() && reps.all { it.target!!.value == 5 && it.target!!.max == 8 })
        val holds = p.blocks.filter { it.type == BlockType.WORK && it.target?.type == TargetType.HOLD_SECONDS }
        assertTrue(holds.all { it.target!!.value == 10 && it.target!!.max == 30 && it.durationSeconds >= 33 })
        val plain = (app.calisthenics.domain.planner.buildTrainPlan(c, app.calisthenics.domain.routine.StarterRoutine.routine, none, app.calisthenics.domain.equipment.SeedProfiles.home, app.calisthenics.domain.planner.TrainSettings(rounds = 3)) as app.calisthenics.domain.planner.PlanResult.Ready).plan
        assertTrue(plain.blocks.all { it.target?.max == null }); assertEquals(RuleKind.APP_LEVELS, plain.rule.kind)
    }

    @Test fun oldRoutinesAndPlansWithoutARuleDecodeAsAppLevels() {
        val j = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }
        assertEquals(RuleKind.APP_LEVELS, j.decodeFromString(Routine.serializer(), """{"id":"r","revision":1,"name":"x","slots":[]}""").rule.kind)
        assertEquals(RuleKind.APP_LEVELS, j.decodeFromString(Target.serializer(), """{"type":"REPS","value":5}""").let { ProgressionRule().kind })
        assertNull(j.decodeFromString(Target.serializer(), """{"type":"REPS","value":5}""").max)
    }
}
