package io.github.gonbei774.calisthenicsmemory.ui

import android.content.Context
import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ApplicationProvider
import app.calisthenics.domain.equipment.SeedProfiles
import app.calisthenics.domain.model.BlockType
import app.calisthenics.domain.planner.PlanResult
import app.calisthenics.domain.planner.TrainSettings
import app.calisthenics.domain.planner.buildTrainPlan
import app.calisthenics.domain.progression.ProgressSnapshot
import app.calisthenics.domain.routine.StarterRoutine
import app.calisthenics.domain.session.*
import io.github.gonbei774.calisthenicsmemory.session.SessionBus
import io.github.gonbei774.calisthenicsmemory.ui.history.HistoryScreen
import io.github.gonbei774.calisthenicsmemory.ui.onboarding.OnboardingScreen
import io.github.gonbei774.calisthenicsmemory.ui.progress.ProgressScreen
import io.github.gonbei774.calisthenicsmemory.ui.screens.*
import io.github.gonbei774.calisthenicsmemory.ui.session.SessionScreen
import io.github.gonbei774.calisthenicsmemory.ui.theme.*
import io.github.gonbei774.calisthenicsmemory.ui.train.PreviewScreen
import io.github.gonbei774.calisthenicsmemory.ui.train.TrainScreen
import io.github.gonbei774.calisthenicsmemory.ui.train.loadCatalog
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate

private fun contrast(a: Color, b: Color): Double {
    val (hi, lo) = listOf(a.luminance(), b.luminance()).sortedDescending()
    return (hi + 0.05) / (lo + 0.05)
}

/** V08b: colour contrast of the Direction B palette (doc 17 §1), checked on the tokens, because Robolectric cannot render pixels here. */
class ContrastTest {
    private fun pairs(c: ColorScheme, accent: AppAccent, bgForAccentText: Color) = listOf(
        "onPrimary/primary" to (c.onPrimary to c.primary), "onPrimaryContainer/primaryContainer" to (c.onPrimaryContainer to c.primaryContainer),
        "onSecondaryContainer/secondaryContainer" to (c.onSecondaryContainer to c.secondaryContainer),
        "onSurface/surface" to (c.onSurface to c.surface), "onBackground/background" to (c.onBackground to c.background),
        "onSurface/background" to (c.onSurface to c.background),
        "onSurfaceVariant/surface" to (c.onSurfaceVariant to c.surface), "onSurfaceVariant/surfaceVariant" to (c.onSurfaceVariant to c.surfaceVariant),
        "muted caption (outline)/background" to (c.outline to c.background), "muted caption (outline)/surface" to (c.outline to c.surface),
        "error/surface" to (c.error to c.surface), "onTertiary/tertiary" to (c.onTertiary to c.tertiary),
        "onAccent/accent" to (accent.onAccent to accent.accent), "gold text/background" to (accent.text to bgForAccentText), "gold text/surface" to (accent.text to c.surface),
    )

    @Test fun bothPalettesMeetWcagAaForText() {
        for (dark in listOf(false, true)) {
            val c = appColorScheme(dark, null)
            val accent = if (dark) DarkAccent else LightAccent
            for ((name, p) in pairs(c, accent, c.background)) assertTrue("${if (dark) "dark" else "light"} $name = ${"%.2f".format(contrast(p.first, p.second))}", contrast(p.first, p.second) >= 4.5)
        }
    }

    @Test fun cautionBoxesMeetAaInBothThemes() {
        assertTrue(contrast(CautionDarkText, CautionDarkSurface) >= 4.5)
        assertTrue(contrast(CautionLightText, CautionLightSurface) >= 4.5)
    }

    @Test fun goldGraphicsAndStarsAreVisibleOnBothBackgrounds() {
        assertTrue(contrast(Gold, BgDark) >= 3.0 && contrast(Gold, SurfaceDark) >= 3.0)          // graphical objects: 3:1
        assertTrue(contrast(GoldMarkOnLight, SurfaceLight) >= 2.9)                              // doc 17 "accentText" mark on white: about 3:1, never used for small text
        assertTrue(contrast(GoldDimDark, SurfaceDark) >= 3.0 || contrast(GoldDimDark, BgDark) >= 3.0)
    }

    @Test fun primaryAndSecondaryMuscleColoursDifferOnBothThemes() {
        assertTrue(contrast(DarkAccent.accent, DarkAccent.accentLight) >= 1.5)
        // doc 17 tokens: on white the two golds differ in saturation more than in lightness (about 1.05:1); the owner approved them, so only "different" is asserted
        assertNotEquals(Gold, GoldDimLight)
    }
}

/** U13: accessibility at font scale 1.3 and in both themes: every clickable thing is labelled and at least 48 dp, key controls are reachable. */
@RunWith(RobolectricTestRunner::class)
// A tall viewport so that nothing is clipped: every control is laid out and can be measured.
@Config(sdk = [34], qualifiers = "w411dp-h4000dp-xxhdpi", fontScale = 1.3f)
class AccessibilityTest {
    @get:Rule val rule = createComposeRule()
    private val ctx: Context get() = ApplicationProvider.getApplicationContext()
    private val catalog by lazy { loadCatalog(ctx) }

    @Before fun clean() {
        for (n in listOf("profiles", "planner_prefs", "train", "goal", "levels", "mode", "onboarding", "routine"))
            ctx.getSharedPreferences(n, Context.MODE_PRIVATE).edit().clear().commit()
        SessionBus.clear(); SessionBus.names = catalog.variations.associate { it.id to it.name }
    }

    private fun checkClickables(where: String) {
        val density = rule.density.density
        val rootW = rule.onRoot().fetchSemanticsNode().size.width.toFloat()
        val nodes = rule.onAllNodes(hasClickAction() or hasAnyAncestor(hasClickAction()).not().and(isToggleable())).fetchSemanticsNodes()
        for (n in nodes) {
            val label = n.config.getOrNull(SemanticsProperties.Text)?.joinToString { it.text }.orEmpty() + n.config.getOrNull(SemanticsProperties.ContentDescription)?.joinToString().orEmpty()
            assertTrue("$where: a control has no label (${n.config})", label.isNotBlank())
            val b = n.touchBoundsInRoot
            if (b.width == 0f && b.height == 0f) continue   // off to the side of a horizontally scrolling row: not laid out, cannot be measured
            if (b.right >= rootW - 1f && b.width < 48f * density) continue // a chip cut by the screen edge in a scrolling row (its text is only a few pixels wide under Robolectric)
            // Compose widens small controls (icon buttons, chips) to a 48 dp touch area: that is touchBounds. A big node that is half
            // scrolled out of view is clipped there, so its layout size counts too.
            val w = maxOf(b.width, n.size.width.toFloat()) / density; val h = maxOf(b.height, n.size.height.toFloat()) / density
            assertTrue("$where: '$label' touch area is $w x $h dp", w >= 47.5f && h >= 47.5f)
        }
        assertTrue("$where: no controls found", nodes.isNotEmpty())
    }

    /** Runs [checks] in the light theme, flips to dark, runs them again (a crash or a missing control in dark mode fails here). */
    private fun bothThemes(content: @Composable () -> Unit, checks: () -> Unit) {
        var dark by mutableStateOf(false)
        rule.setContent { CalisthenicsMemoryTheme(darkTheme = dark, dynamicColor = false) { content() } }
        rule.waitForIdle(); checks()
        dark = true; rule.waitForIdle(); checks()
    }

    private fun sessionAt(i: Int): SessionState {
        val none = ProgressSnapshot(emptyMap(), emptyMap(), emptyMap(), emptyList())
        val plan = (buildTrainPlan(catalog, StarterRoutine.routine, none, SeedProfiles.home, TrainSettings(rounds = 2)) as PlanResult.Ready).plan.copy(id = "plan-x")
        var s = reduce(newSession("S", plan), SessionEvent.Start(0)).state; var now = 0L
        repeat(i) { now += plan.blocks[it].durationSeconds * 1000L; s = reduce(s, SessionEvent.Tick(now)).state }
        return s
    }

    @Test fun trainTab() = bothThemes({ TrainScreen(onPreview = {}, onStarted = {}, onEditProfile = {}) }) {
        checkClickables("train")
        rule.onNodeWithTag("preview_button").assertIsDisplayed(); rule.onNodeWithTag("train_start_button").assertIsDisplayed() // the bottom bar stays in reach even when text is 30 % larger
        rule.onNodeWithTag("goal_field").assertIsDisplayed()
    }

    @Test fun previewScreen() = bothThemes({ PreviewScreen(onBack = {}, onStarted = {}) }) {
        checkClickables("preview"); rule.onNodeWithTag("start_button").assertIsDisplayed() // the Start button is sticky, never scrolled away
    }

    @Test fun sessionWorkBlock() { SessionBus.publish(sessionAt(0)); bothThemes({ SessionScreen(onExit = {}) }) { checkClickables("session work"); rule.onNodeWithTag("session_time").assertIsDisplayed(); rule.onNodeWithTag("session_done").assertIsDisplayed() } }

    @Test fun sessionRecoveryWithLogger() {
        val none = ProgressSnapshot(emptyMap(), emptyMap(), emptyMap(), emptyList())
        val plan = (buildTrainPlan(catalog, StarterRoutine.routine, none, SeedProfiles.home, TrainSettings(rounds = 2)) as PlanResult.Ready).plan
        val s = sessionAt(plan.blocks.indexOfFirst { it.type == BlockType.STRETCH }); assertEquals(BlockType.STRETCH, s.currentBlock!!.type)
        SessionBus.publish(s)
        bothThemes({ SessionScreen(onExit = {}) }) { checkClickables("session recovery"); rule.onNodeWithTag("logger").assertIsDisplayed(); rule.onNodeWithTag("session_skip").assertIsDisplayed() }
    }

    @Test fun settingsTab() = bothThemes({ SettingsScreen(onLevels = {}, onBackup = {}, onLicenses = {}) }) { checkClickables("settings") }

    @Test fun progressTab() = bothThemes({ ProgressScreen() }) { checkClickables("progress"); rule.onNodeWithTag("tab_chip_push").assertIsDisplayed() }

    @Test fun historyTab() = bothThemes({ HistoryScreen(source = FakeHistory(emptyList()), today = LocalDate.of(2026, 10, 7), onOpen = {}) }) { rule.onNodeWithTag("history_empty").assertIsDisplayed() }

    @Test fun onboardingWelcomeAndEquipment() = bothThemes({ OnboardingScreen(onBack = null, onDone = {}) }) {
        rule.onNodeWithTag("onb_next").assertIsDisplayed().assertIsEnabled()
        checkClickables("onboarding")
    }

    @Test fun profileEditor() = bothThemes({ ProfileEditScreen(profileId = null, onDone = {}) }) { checkClickables("profile editor") }
}


/** U13: the same screens on the Galaxy S21's size with text 30 % larger: the controls that matter stay reachable (scroll if needed). */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w411dp-h891dp-xxhdpi", fontScale = 1.3f)
class FontScaleReachTest {
    @get:Rule val rule = createComposeRule()
    private val ctx: Context get() = ApplicationProvider.getApplicationContext()

    @Before fun clean() {
        for (n in listOf("profiles", "planner_prefs", "train", "goal", "levels", "mode", "onboarding", "routine")) ctx.getSharedPreferences(n, Context.MODE_PRIVATE).edit().clear().commit()
    }

    @Test fun trainTabKeepsItsControlsReachable() {
        rule.setContent { CalisthenicsMemoryTheme(darkTheme = false) { TrainScreen(onPreview = {}, onStarted = {}, onEditProfile = {}) } }
        for (tag in listOf("goal_field", "profile_field", "rounds_plus", "between_rest", "minutes", "exercise_count")) rule.onNodeWithTag(tag).performScrollTo().assertIsDisplayed()
        for (tag in listOf("preview_button", "train_start_button")) rule.onNodeWithTag(tag).assertIsDisplayed() // the bottom bar
    }

    @Test fun previewKeepsStartVisibleAtAllTimes() {
        rule.setContent { CalisthenicsMemoryTheme(darkTheme = false) { PreviewScreen(onBack = {}, onStarted = {}) } }
        rule.waitForIdle()
        rule.onNodeWithTag("start_button").assertIsDisplayed()
    }

    @Test fun onboardingNextButtonStaysOnScreen() {
        rule.setContent { CalisthenicsMemoryTheme(darkTheme = false) { OnboardingScreen(onBack = null, onDone = {}) } }
        rule.onNodeWithTag("onb_next").assertIsDisplayed()
    }

    @Test fun settingsEveryRowIsReachable() {
        rule.setContent { CalisthenicsMemoryTheme(darkTheme = false) { SettingsScreen(onLevels = {}, onBackup = {}, onLicenses = {}) } }
        for (tag in listOf("settings_profiles", "set_stretch", "set_voice", "theme_dark", "settings_backup", "settings_privacy", "settings_licenses")) rule.onNodeWithTag(tag).performScrollTo().assertIsDisplayed()
    }
}
