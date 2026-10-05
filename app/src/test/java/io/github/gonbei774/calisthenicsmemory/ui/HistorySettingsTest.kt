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
import java.time.LocalDate
import java.time.ZoneId

class FakeHistory(var list: List<StoredSession>) : HistorySource {
    val revisions = mutableListOf<List<Any?>>()
    override suspend fun sessions() = list
    override suspend fun revise(sessionId: String, variationId: String, rating: Rating, discomfort: Boolean, reps: Int?) { revisions += listOf(sessionId, variationId, rating, discomfort, reps) }
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

    @Test fun detailShowsEachRoundAndACorrectionAddsARevision() {
        val fake = FakeHistory(listOf(session("a", today, 8, 6, null)))
        rule.setContent { CalisthenicsMemoryTheme(darkTheme = false) { SessionDetailScreen("a", source = fake, onBack = {}) } }
        rule.waitForIdle()
        rule.onNodeWithTag("detail_summary").assertTextContains("3 rounds", substring = true)
        rule.onNodeWithTag("round_r1-push-work").assertTextContains("Round 1: 8 reps")
        rule.onNodeWithTag("round_r2-push-work").assertTextContains("Round 2: 6 reps")
        rule.onNodeWithTag("round_r3-push-work").assertTextContains("as planned", substring = true)
        rule.onNodeWithTag("fix_pushup-standard_reps_value").assertTextEquals("6") // the lowest round is the starting point
        rule.onNodeWithTag("fix_pushup-standard_reps_plus").performClick(); rule.waitForIdle()
        assertEquals(listOf<Any?>("a", "pushup-standard", Rating.BELOW.let { Rating.BELOW }, false, 7), fake.revisions.last().let { listOf(it[0], it[1], it[2], it[3], it[4]) }) // 7 < target 8
        rule.onNodeWithTag("fix_pushup-standard_reps_plus").performClick(); rule.waitForIdle()
        assertEquals(Rating.MET, fake.revisions.last()[2]); assertEquals(8, fake.revisions.last()[4])
        rule.onNodeWithTag("fix_pushup-standard_pain").performClick(); rule.waitForIdle()
        assertEquals(true, fake.revisions.last()[3])
        assertEquals(3, fake.revisions.size) // every change is a new revision; nothing is overwritten
        rule.onNodeWithTag("corrected_note").assertExists()
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
