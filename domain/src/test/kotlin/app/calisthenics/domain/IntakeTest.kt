package app.calisthenics.domain

import app.calisthenics.domain.intake.*
import app.calisthenics.domain.model.*
import org.junit.Assert.*
import org.junit.Test

class IntakeTest {
    private fun t(i: Int, v: Int) = Tier(i, Target(TargetType.REPS, v), 60, 60)
    private val tiers = listOf(t(1, 6), t(2, 8), t(3, 10), t(4, 12), t(5, 15))
    @Test fun below_first_gives_step_1() = assertEquals(1, suggestStartingStep(tiers, 3))
    @Test fun exact_match() = assertEquals(4, suggestStartingStep(tiers, 12))
    @Test fun between_rounds_down() = assertEquals(3, suggestStartingStep(tiers, 11))
    @Test fun above_top_caps_and_flags() { assertEquals(5, suggestStartingStep(tiers, 40)); assertTrue(exceedsLadder(tiers, 40)); assertFalse(exceedsLadder(tiers, 15)) }
}

class QuestionnaireTest {
    private val catalog = app.calisthenics.domain.content.parseCatalog(java.io.File(System.getProperty("repo.root"), "content/starter/catalog.json").readText())
    private fun fam(id: String) = catalog.onboardingFamilies.first { it.id == id }
    private fun reps(v: String, tier: Int) = catalog.policyForVariation(v)!!.tiers.first { it.index == tier }.target.value

    @Test fun everyFamilyHasAnAnchorOnItsLadderAndNoHardSkill() {
        assertEquals(listOf("pushup", "squat", "pull", "core", "hinge", "shoulders"), catalog.onboardingFamilies.map { it.id })
        val hard = setOf("planche-lean", "front-lever-tuck", "muscle-up-bar", "squat-pistol", "hspu-wall", "pushup-one-arm", "l-sit-floor")
        for (f in catalog.onboardingFamilies) {
            assertTrue(f.anchorVariationId in f.ladder)
            assertTrue("${f.id} ladder reaches a hard skill", f.ladder.none { it in hard })
        }
        assertTrue(app.calisthenics.domain.content.validateCatalog(catalog).ok)
    }

    @Test fun normalForPushupIsTheStandardPushupAtStep3Eightreps() {
        val l = levelFor(catalog, fam("pushup"), Guess.NORMAL)
        assertEquals(StartLevel("pushup-standard", 3), l)
        assertEquals(8, reps(l.variationId, l.tier))
    }

    @Test fun easyAndHardAreTheNeighbourExerciseAtStep3() {
        assertEquals(StartLevel("pushup-knee", 3), levelFor(catalog, fam("pushup"), Guess.EASY))
        assertEquals(StartLevel("pushup-feet-elevated", 3), levelFor(catalog, fam("pushup"), Guess.HARD))
        assertEquals(StartLevel("squat-air", 3), levelFor(catalog, fam("squat"), Guess.EASY))
        assertEquals(StartLevel("split-squat", 3), levelFor(catalog, fam("squat"), Guess.NORMAL))
        assertEquals(StartLevel("split-squat-bulgarian", 3), levelFor(catalog, fam("squat"), Guess.HARD))
    }

    @Test fun withNoNeighbourItFallsBackToStep1OrStep5OfTheAnchor() {
        assertEquals(StartLevel("plank", 1), levelFor(catalog, fam("core"), Guess.EASY))
        assertEquals(StartLevel("glute-bridge", 1), levelFor(catalog, fam("hinge"), Guess.EASY))
        assertEquals(StartLevel("pike-pushup", 1), levelFor(catalog, fam("shoulders"), Guess.EASY))
        // the top of a ladder: Hard of the last exercise is that exercise at step 5
        val top = fam("core").copy(anchorVariationId = "hollow-hold")
        assertEquals(StartLevel("hollow-hold", 5), levelFor(catalog, top, Guess.HARD))
    }

    @Test fun pullFollowsTheEquipmentNoBarMeansBandRow() {
        val noBar = levelFor(catalog, fam("pull"), Guess.NORMAL, usable = { it.id == "row-band" })
        assertEquals("row-band", noBar.variationId)
        assertEquals(StartLevel("pullup-band-assisted", 3), levelFor(catalog, fam("pull"), Guess.NORMAL))
        assertEquals(StartLevel("row-band", 3), levelFor(catalog, fam("pull"), Guess.EASY))
    }

    @Test fun iDoThisMapsRepsToTheMatchingStep() {
        assertEquals(StartLevel("pushup-feet-elevated", 3), levelFromDoes(catalog, FamilyAnswer.Does("pushup-feet-elevated", 8, 4)))
        assertEquals(StartLevel("plank", 4), levelFromDoes(catalog, FamilyAnswer.Does("plank", 45, 3)))
        assertEquals(StartLevel("pushup-standard", 1), levelFromDoes(catalog, FamilyAnswer.Does("pushup-standard", 1, 3))) // below step 1 -> step 1
    }

    @Test fun shouldersOnlyWhenPushupIsAtLeastStandard() {
        val sh = fam("shoulders")
        assertFalse(shouldAsk(catalog, sh, null))
        assertFalse(shouldAsk(catalog, sh, FamilyAnswer.NotSure(Guess.EASY)))
        assertTrue(shouldAsk(catalog, sh, FamilyAnswer.NotSure(Guess.NORMAL)))
        assertFalse(shouldAsk(catalog, sh, FamilyAnswer.Does("pushup-knee", 10, 3)))
        assertTrue(shouldAsk(catalog, sh, FamilyAnswer.Does("pushup-standard", 6, 3)))
        assertTrue(shouldAsk(catalog, fam("squat"), null))
    }

    @Test fun aPullUpAnswerTurnsThePullSlotIntoAVerticalPull() {
        val base = app.calisthenics.domain.routine.StarterRoutine.routine
        val r = routineFromAnswers(catalog, base, StartLevel("pullup-band-assisted", 3))
        val slot = r.slots.first { it.id == "pull" }
        assertEquals(Pattern.PULL_VERTICAL, slot.intent); assertEquals("pullup-band-assisted", slot.preferredVariationId); assertEquals(2, r.revision)
        assertSame(base, routineFromAnswers(catalog, base, StartLevel("row-band", 2)))
        assertSame(base, routineFromAnswers(catalog, base, null))
    }

    @Test fun suggestedRoundsIsTheRoundedMeanOfWhatWasSaid() {
        assertEquals(4, suggestedRounds(listOf(null, FamilyAnswer.NotSure(Guess.EASY))))
        assertEquals(3, suggestedRounds(listOf(FamilyAnswer.Does("a", 5, 3), FamilyAnswer.Does("b", 5, 3), FamilyAnswer.Does("c", 5, 4))))
        assertEquals(10, suggestedRounds(listOf(FamilyAnswer.Does("a", 5, 40))))
    }
}
