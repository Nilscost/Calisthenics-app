package io.github.gonbei774.calisthenicsmemory.ui

import android.content.Context
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ApplicationProvider
import app.calisthenics.domain.feedback.Rating
import app.calisthenics.domain.history.BlockRecord
import app.calisthenics.domain.history.SessionRecord
import app.calisthenics.domain.model.*
import app.calisthenics.domain.model.Target
import io.github.gonbei774.calisthenicsmemory.data.AppTheme
import io.github.gonbei774.calisthenicsmemory.ui.history.*
import io.github.gonbei774.calisthenicsmemory.ui.screens.PrefsStore
import io.github.gonbei774.calisthenicsmemory.ui.screens.PrivacyScreen
import io.github.gonbei774.calisthenicsmemory.ui.screens.SettingsScreen
import io.github.gonbei774.calisthenicsmemory.ui.theme.CalisthenicsMemoryTheme
import io.github.gonbei774.calisthenicsmemory.ui.train.TrainSettingsStore
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import app.calisthenics.domain.feedback.FeedbackRow
import io.github.gonbei774.calisthenicsmemory.ui.history.RoundCorrection
import java.time.LocalDate
import java.time.ZoneId

class FakeHistory(var list: List<StoredSession>) : HistorySource {
    val revisions = mutableListOf<List<Any?>>()
    override suspend fun sessions() = list
    override suspend fun revise(sessionId: String, variationId: String, rating: Rating, discomfort: Boolean, reps: Int?) { revisions += listOf(sessionId, variationId, rating, discomfort, reps) }
    val roundRevisions = mutableListOf<Pair<String, List<RoundCorrection>>>()
    override suspend fun reviseRounds(sessionId: String, variationId: String, rounds: List<RoundCorrection>) { roundRevisions += variationId to rounds }
}

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w411dp-h891dp-xxhdpi")
class HistorySettingsTest {
    @get:Rule val rule = createComposeRule()
    private val ctx: Context get() = ApplicationProvider.getApplicationContext()
    private val today = LocalDate.of(2026, 10, 7)   // a Wednesday; the week starts 2026-10-05
    private fun ms(d: LocalDate) = d.atTime(9, 0).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

    private fun work(r: Int, t: Int = 8) = TimelineBlock("r$r-push-work", BlockType.WORK, 60, r, "push", "pushup-standard", target = Target(TargetType.REPS, t), prescriptionTier = 3)
    private val blocks = listOf(work(1), work(2), work(3))
    private val plan = WorkoutPlan("p1", "r", 1, 1, 0, "home", 600, 180, setOf(StrengthFocus.FULL_BODY), true, null, 3, emptyList(), emptyList(), false, false, blocks)
    private fun session(id: String, d: LocalDate, vararg reps: Int?) = StoredSession(
        SessionRecord(id, "p1", ms(d), ms(d) + 40 * 60_000L, "COMPLETED", reps.mapIndexed { i, v -> BlockRecord("r${i + 1}-push-work", "pushup-standard", "WORK", "MET", 60, v) }),
        plan, emptyMap())

    @Before fun clean() { for (n in listOf("profiles", "planner_prefs", "train", "goal", "mode")) ctx.getSharedPreferences(n, Context.MODE_PRIVATE).edit().clear().commit() }

    private fun showHistory(src: HistorySource, open: (String) -> Unit = {}) = rule.setContent {
        CalisthenicsMemoryTheme(darkTheme = false) { HistoryScreen(source = src, today = today, onOpen = open) }
    }

    @Test fun weekStripMarksTheDaysYouTrainedAndListsTheWeeksWorkouts() {
        showHistory(FakeHistory(listOf(session("a", LocalDate.of(2026, 10, 5), 8, 8, 8), session("b", LocalDate.of(2026, 10, 7), 8, 6, 8), session("old", LocalDate.of(2026, 9, 20), 8))))
        rule.waitForIdle()
        rule.onNodeWithTag("dot_2026-10-05", useUnmergedTree = true).assertExists(); rule.onNodeWithTag("dot_2026-10-07", useUnmergedTree = true).assertExists(); rule.onNodeWithTag("nodot_2026-10-06", useUnmergedTree = true).assertExists()
        rule.onNodeWithTag("session_a").assertExists(); rule.onNodeWithTag("session_b").assertExists(); rule.onNodeWithTag("session_old").assertDoesNotExist()
        rule.onNodeWithTag("session_a").assertTextContains("40 min · 3 rounds · 1 exercises", substring = true)
        rule.onNodeWithTag("week_summary").assertTextContains("2 workouts", substring = true)
    }

    @Test fun tappingADayFiltersAndTappingASessionOpensIt() {
        var opened = ""
        showHistory(FakeHistory(listOf(session("a", LocalDate.of(2026, 10, 5), 8), session("b", LocalDate.of(2026, 10, 7), 8))), { opened = it })
        rule.waitForIdle()
        rule.onNodeWithTag("day_2026-10-07").performClick(); rule.waitForIdle()
        rule.onNodeWithTag("session_a").assertDoesNotExist(); rule.onNodeWithTag("session_b").assertExists()
        rule.onNodeWithTag("session_b").performClick()
        assertEquals("b", opened)
    }

    @Test fun weeksCanBeBrowsedButNotTheFuture() {
        showHistory(FakeHistory(listOf(session("old", LocalDate.of(2026, 9, 30), 8))))
        rule.waitForIdle()
        rule.onNodeWithTag("week_next").assertIsNotEnabled()
        rule.onNodeWithTag("history_empty").assertExists()
        rule.onNodeWithTag("week_prev").performClick(); rule.waitForIdle()
        rule.onNodeWithTag("session_old").assertExists()
        rule.onNodeWithTag("week_next").assertIsEnabled()
    }

    @Test fun emptyHistoryNeverShamesAMissedWeek() {
        showHistory(FakeHistory(emptyList())); rule.waitForIdle()
        rule.onNodeWithTag("history_empty").assertTextContains("never penalised", substring = true)
    }

    @Test fun detailShowsEachRoundWithItsOwnNumberAndACorrectionAddsRevisionsForEveryRound() {
        val fake = FakeHistory(listOf(session("a", today, 8, 6, null)))
        rule.setContent { CalisthenicsMemoryTheme(darkTheme = false) { SessionDetailScreen("a", source = fake, onBack = {}) } }
        rule.waitForIdle()
        rule.onNodeWithTag("detail_summary").assertTextContains("3 rounds", substring = true)
        rule.onNodeWithTag("fix_pushup-standard_round1").assertTextContains("Round 1")
        rule.onNodeWithTag("fix_pushup-standard_round1_value").assertTextEquals("8")
        rule.onNodeWithTag("fix_pushup-standard_round2_value").assertTextEquals("6")
        rule.onNodeWithTag("fix_pushup-standard_round3").assertTextContains("as planned", substring = true)
        rule.onNodeWithTag("fix_pushup-standard_round3_value").assertTextEquals("8") // untyped = the target
        // correct round 2 only: 6 -> 8; the others keep what they had, and ALL rounds are written (see Corrections.kt)
        rule.onNodeWithTag("fix_pushup-standard_round2_plus").performClick(); rule.waitForIdle()
        var (vid, list) = fake.roundRevisions.last()
        assertEquals("pushup-standard", vid)
        assertEquals(listOf("r1-push-work", "r2-push-work", "r3-push-work"), list.map { it.blockId })
        assertEquals(listOf<Int?>(8, 7, null), list.map { it.reps })
        assertEquals(listOf(Rating.MET, Rating.BELOW, Rating.MET), list.map { it.rating }) // 7 < target 8
        rule.onNodeWithTag("fix_pushup-standard_round2_plus").performClick(); rule.waitForIdle()
        list = fake.roundRevisions.last().second
        assertEquals(listOf(Rating.MET, Rating.MET, Rating.MET), list.map { it.rating }); assertEquals(8, list[1].reps)
        rule.onNodeWithTag("fix_pushup-standard_pain").performClick(); rule.waitForIdle()
        assertTrue(fake.roundRevisions.last().second.all { it.discomfort })
        rule.onNodeWithTag("fix_pushup-standard_too_easy").performClick(); rule.waitForIdle()
        assertTrue(fake.roundRevisions.last().second.all { it.rating == Rating.ABOVE })
        rule.onNodeWithTag("fix_pushup-standard_too_hard").performClick(); rule.waitForIdle() // exclusive with "too easy"
        assertTrue(fake.roundRevisions.last().second.all { it.rating == Rating.BELOW })
        assertEquals(5, fake.roundRevisions.size) // every change is new revisions; nothing is overwritten
        rule.onNodeWithTag("corrected_note").assertExists()
    }

    @Test fun correctedRoundsAreShownInsteadOfTheLoggedNumber() {
        val s = session("a", today, 8, 6, 8).let { it.copy(rows = listOf(
            FeedbackRow("pushup-standard", "r2-push-work", 2, Rating.MET, false, 9),
            FeedbackRow("pushup-standard", "r1-push-work", 3, Rating.MET, false, 8),
            FeedbackRow("pushup-standard", "r3-push-work", 4, Rating.MET, false, 8))) }
        rule.setContent { CalisthenicsMemoryTheme(darkTheme = false) { SessionDetailScreen("a", source = FakeHistory(listOf(s)), onBack = {}) } }
        rule.waitForIdle()
        rule.onNodeWithTag("fix_pushup-standard_round2_value").assertTextEquals("9")
        rule.onNodeWithTag("fix_pushup-standard_round2").assertTextContains("corrected", substring = true)
    }

    @Test fun theAutomaticProgressionSwitchIsOnByDefaultAndPersists() {
        ctx.getSharedPreferences("planner_prefs", Context.MODE_PRIVATE).edit().clear().commit()
        showSettings()
        rule.onNodeWithTag("set_auto_progress").performScrollTo().assertIsOn()
        rule.onNodeWithTag("auto_progress_hint").assertTextContains("levels rise", substring = true)
        rule.onNodeWithTag("set_auto_progress").performClick(); rule.waitForIdle()
        rule.onNodeWithTag("set_auto_progress").assertIsOff()
        rule.onNodeWithTag("auto_progress_hint").assertTextContains("stay as they are", substring = true)
        assertFalse(io.github.gonbei774.calisthenicsmemory.ui.screens.PrefsStore.load(ctx).autoProgression)
    }

    @Test fun anUnknownWorkoutSaysSo() {
        rule.setContent { CalisthenicsMemoryTheme(darkTheme = false) { SessionDetailScreen("zzz", source = FakeHistory(emptyList()), onBack = {}) } }
        rule.waitForIdle()
        rule.onNodeWithText("This workout is not available.").assertIsDisplayed()
    }

    // ---------------------------------------------------------------- settings

    private fun showSettings(theme: AppTheme = AppTheme.SYSTEM, onTheme: (AppTheme) -> Unit = {}, onLevels: () -> Unit = {}) = rule.setContent {
        CalisthenicsMemoryTheme(darkTheme = false) { SettingsScreen(onLevels = onLevels, onBackup = {}, onLicenses = {}, theme = theme, onTheme = onTheme) }
    }

    @Test fun settingsHasEverySectionTheSpecAsks() {
        showSettings()
        for (tag in listOf("settings_profiles", "set_rounds_value", "set_reps", "set_stretch", "set_voice", "settings_redo", "theme_system", "settings_backup")) rule.onNodeWithTag(tag).performScrollTo().assertExists()
        for (tag in listOf("settings_privacy", "settings_licenses", "settings_about")) rule.onNodeWithTag(tag).performScrollTo().assertExists()
    }

    @Test fun workoutDefaultsAreSavedForTheTrainTab() {
        showSettings()
        rule.onNodeWithTag("set_rounds_plus").performScrollTo().performClick()
        rule.onNodeWithTag("set_timed").performScrollTo().performClick()
        rule.onNodeWithTag("set_stretch").performScrollTo().performClick()
        rule.waitForIdle()
        val t = TrainSettingsStore.load(ctx)
        assertEquals(5, t.rounds); assertTrue(t.timed); assertFalse(t.stretchOn)
    }

    @Test fun voiceCuesCanBeSwitchedOff() {
        showSettings()
        assertTrue(PrefsStore.load(ctx).audioEnabled)
        rule.onNodeWithTag("set_voice").performScrollTo().performClick(); rule.waitForIdle()
        assertFalse(PrefsStore.load(ctx).audioEnabled)
    }

    @Test fun themeChoiceIsReported() {
        var picked: AppTheme? = null
        showSettings(onTheme = { picked = it })
        rule.onNodeWithTag("theme_dark").performScrollTo().performClick()
        assertEquals(AppTheme.DARK, picked)
    }

    @Test fun redoAsksForConfirmationFirst() {
        var redone = false
        showSettings(onLevels = { redone = true })
        rule.onNodeWithTag("settings_redo").performScrollTo().performClick()
        rule.onNodeWithText("Redo the starting questionnaire?").assertIsDisplayed()
        assertFalse(redone)
        rule.onNodeWithTag("settings_redo_confirm").performClick()
        assertTrue(redone)
    }

    @Test fun privacyPolicyIsShownInTheApp() {
        rule.setContent { CalisthenicsMemoryTheme(darkTheme = false) { PrivacyScreen(onBack = {}) } }
        rule.onNodeWithText("Privacy policy: Calisthenics Personal").assertIsDisplayed()
        rule.onNodeWithText("No account, no tracking.", substring = true).assertExists()
    }
}
