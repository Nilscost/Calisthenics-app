package io.github.gonbei774.calisthenicsmemory.ui

import android.content.Context
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ApplicationProvider
import app.calisthenics.domain.equipment.SeedProfiles
import io.github.gonbei774.calisthenicsmemory.ui.nav.AppNav
import io.github.gonbei774.calisthenicsmemory.ui.onboarding.OnboardingScreen
import io.github.gonbei774.calisthenicsmemory.ui.screens.*
import io.github.gonbei774.calisthenicsmemory.ui.theme.CalisthenicsMemoryTheme
import io.github.gonbei774.calisthenicsmemory.ui.train.TrainSettingsStore
import io.github.gonbei774.calisthenicsmemory.ui.train.loadCatalog
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w411dp-h891dp-xxhdpi")
class OnboardingTest {
    @get:Rule val rule = createComposeRule()
    private val ctx: Context get() = ApplicationProvider.getApplicationContext()

    @Before fun clean() {
        for (n in listOf("profiles", "mode", "planner_prefs", "train", "goal", "levels", "onboarding", "routine"))
            ctx.getSharedPreferences(n, Context.MODE_PRIVATE).edit().clear().commit()
    }

    private fun next() { rule.onNodeWithTag("onb_next").performClick(); rule.waitForIdle(); rule.mainClock.advanceTimeBy(600); rule.waitForIdle() }
    private fun title() = rule.onNodeWithTag("onb_title").fetchSemanticsNode().config.first { it.key.name == "Text" }.value.let { (it as List<*>).first().toString() }

    private fun runThroughWithGuesses(vararg guesses: String): MutableList<String> {
        val seen = mutableListOf<String>()
        var done = false
        rule.setContent { CalisthenicsMemoryTheme(darkTheme = false) { OnboardingScreen(onBack = null, onDone = { done = true }) } }
        rule.waitForIdle()
        seen += title()
        next(); seen += title()   // goal
        next(); seen += title()   // equipment
        next()
        var g = 0
        while (true) {
            val t = title(); seen += t
            if (t == "How do you like to train?") break
            // a family page: Next is disabled until something is answered
            rule.onNodeWithTag("onb_next").assertIsNotEnabled()
            rule.onNodeWithTag("onb_dont_know").performClick()
            rule.onNodeWithTag("onb_guess_${guesses[g++ % guesses.size]}").performClick()
            rule.onNodeWithTag("onb_next").assertIsEnabled()
            next()
        }
        next(); seen += title()   // summary
        rule.onNodeWithTag("onb_finish").performClick(); rule.waitForIdle()
        assertTrue(done)
        return seen
    }

    @Test fun normalAnswersWalkThroughAllPagesAndStoreTheLevels() {
        val seen = runThroughWithGuesses("NORMAL")
        assertEquals(listOf("Welcome", "What is your goal?", "What do you have?"), seen.take(3))
        assertTrue(seen.containsAll(listOf("Push-up", "Squat", "Pull and row", "Core", "Hips and back", "Shoulders")))
        assertEquals("You are ready", seen.last())
        val levels = LevelStore.load(ctx)
        assertEquals(3, levels["pushup-standard"]); assertEquals(3, levels["split-squat"]); assertEquals(3, levels["pullup-band-assisted"])
        assertEquals(3, levels["plank"] ?: 3); assertEquals(3, levels["glute-bridge"]); assertEquals(3, levels["pike-pushup"])
        assertTrue(OnboardingStore.done(ctx))
    }

    @Test fun easyPushupSkipsTheShouldersPage() {
        val seen = runThroughWithGuesses("EASY")
        assertFalse("Shoulders" in seen)
        assertEquals(3, LevelStore.load(ctx)["pushup-knee"])
        assertEquals(1, LevelStore.load(ctx)["plank"])        // no easier core exercise -> step 1 of the anchor
    }

    @Test fun iDoThisStoresTheStepForTheTypedRepsAndTheRounds() {
        var done = false
        rule.setContent { CalisthenicsMemoryTheme(darkTheme = false) { OnboardingScreen(onBack = null, onDone = { done = true }) } }
        next(); next(); next() // welcome, goal, equipment -> push-up page
        assertEquals("Push-up", title())
        rule.onNodeWithTag("onb_carousel").performScrollToNode(hasTestTag("onb_ex_pushup-feet-elevated"))
        rule.onNodeWithTag("onb_ex_pushup-feet-elevated").performClick(); rule.waitForIdle(); rule.mainClock.advanceTimeBy(1000); rule.waitForIdle()
        rule.onNodeWithTag("onb_i_do_this").performSemanticsAction(SemanticsActions.OnClick); rule.waitForIdle(); rule.mainClock.advanceTimeBy(1000); rule.waitForIdle()
        rule.onNodeWithTag("onb_reps_value").assertTextEquals("8") // prefilled with the step-3 target
        repeat(2) { rule.onNodeWithTag("onb_reps_plus").performClick() } // 10 reps -> step 4
        rule.onNodeWithTag("onb_rounds_minus").performClick()           // 3 rounds
        next()
        assertEquals("Squat", title())
        rule.onNodeWithTag("onb_dont_know").performClick(); rule.onNodeWithTag("onb_guess_NORMAL").performClick(); next()
        rule.onNodeWithTag("onb_dont_know").performClick(); rule.onNodeWithTag("onb_guess_NORMAL").performClick(); next()
        rule.onNodeWithTag("onb_dont_know").performClick(); rule.onNodeWithTag("onb_guess_NORMAL").performClick(); next()
        rule.onNodeWithTag("onb_dont_know").performClick(); rule.onNodeWithTag("onb_guess_NORMAL").performClick(); next()
        assertEquals("Shoulders", title()) // push-up answer is above standard
        rule.onNodeWithTag("onb_dont_know").performClick(); rule.onNodeWithTag("onb_guess_NORMAL").performClick(); next()
        assertEquals("How do you like to train?", title())
        rule.onNodeWithTag("onb_default_rounds_value").assertTextEquals("3") // the rounds the person said they do
        next()
        rule.onNodeWithTag("onb_sum_pushup-feet-elevated").assertExists()
        rule.onNodeWithTag("onb_finish").performClick(); rule.waitForIdle()
        assertTrue(done)
        assertEquals(4, LevelStore.load(ctx)["pushup-feet-elevated"])
        assertEquals(3, TrainSettingsStore.load(ctx).rounds)
    }

    @Test fun styleAndEquipmentAnswersAreSaved() {
        rule.setContent { CalisthenicsMemoryTheme(darkTheme = false) { OnboardingScreen(onBack = null, onDone = {}) } }
        next(); next()
        rule.onNodeWithTag("onb_profile_name").performTextClearance(); rule.onNodeWithTag("onb_profile_name").performTextInput("Garage")
        rule.onNodeWithTag("eq_high-bar").performScrollTo().performClick()
        next()
        repeat(5) { rule.onNodeWithTag("onb_dont_know").performClick(); rule.onNodeWithTag("onb_guess_NORMAL").performClick(); next() }
        if (title() == "Shoulders") { rule.onNodeWithTag("onb_dont_know").performClick(); rule.onNodeWithTag("onb_guess_NORMAL").performClick(); next() }
        rule.onNodeWithTag("onb_style_timed").performClick()
        rule.onNodeWithTag("onb_stretch").performClick()
        next()
        rule.onNodeWithTag("onb_finish").performClick(); rule.waitForIdle()
        val p = ProfileStore.load(ctx).single { it.id == "home" }
        assertEquals("Garage", p.name); assertTrue(p.items.any { it.equipmentId == "high-bar" })
        val t = TrainSettingsStore.load(ctx)
        assertTrue(t.timed); assertFalse(t.stretchOn)
    }

    @Test fun withoutAPullUpBarNormalPullIsTheBandRow() {
        rule.setContent { CalisthenicsMemoryTheme(darkTheme = false) { OnboardingScreen(onBack = null, onDone = {}) } }
        next(); next()
        rule.onNodeWithTag("eq_pullup-bar").performScrollTo().performClick() // untick the bar Home had
        next()
        repeat(2) { rule.onNodeWithTag("onb_dont_know").performClick(); rule.onNodeWithTag("onb_guess_NORMAL").performClick(); next() }
        assertEquals("Pull and row", title())
        rule.onNodeWithTag("onb_dont_know").performClick()
        rule.onNodeWithTag("onb_guess_NORMAL").assertTextContains("Band Row", substring = true) // no bar: the band row
    }

    @Test fun redoFromSettingsAsksForConfirmationAndOverwritesLevels() {
        LevelStore.save(ctx, mapOf("pushup-standard" to 5))
        OnboardingStore.setDone(ctx)
        rule.setContent { CalisthenicsMemoryTheme(darkTheme = false) { AppNav() } }
        rule.onNodeWithTag("tab_settings").performClick()
        rule.onNodeWithTag("settings_redo").performClick()
        rule.onNodeWithText("Redo the starting questionnaire?").assertIsDisplayed()
        assertEquals(5, LevelStore.load(ctx)["pushup-standard"]) // nothing changed yet
        rule.onNodeWithTag("settings_redo_confirm").performClick(); rule.waitForIdle()
        rule.onNodeWithTag("onb_title").assertTextEquals("Welcome")
        rule.onNodeWithTag("onb_cancel").assertIsDisplayed() // redo can be cancelled
    }

    @Test fun catalogHasThePullAndShouldersFamiliesForTheQuestionnaire() {
        assertEquals(6, loadCatalog(ctx).onboardingFamilies.size)
        assertEquals(SeedProfiles.home.id, "home")
    }
}
