package io.github.gonbei774.calisthenicsmemory.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import io.github.gonbei774.calisthenicsmemory.ui.components.Stepper
import io.github.gonbei774.calisthenicsmemory.ui.theme.AppAccentTheme
import io.github.gonbei774.calisthenicsmemory.ui.theme.CalisthenicsMemoryTheme
import io.github.gonbei774.calisthenicsmemory.ui.theme.Teal40
import io.github.gonbei774.calisthenicsmemory.ui.theme.Teal80
import io.github.gonbei774.calisthenicsmemory.ui.theme.appColorScheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DesignSystemTest {
    @get:Rule val rule = createComposeRule()

    @Test fun fallbackPaletteIsTealAndDiffersByTheme() {
        assertEquals(Teal40, appColorScheme(false, null).primary)
        assertEquals(Teal80, appColorScheme(true, null).primary)
    }

    @Test fun lightAndDarkThemesCompose() {
        var primary: Color? = null
        var accent: Color? = null
        var dark by mutableStateOf(false)
        rule.setContent { CalisthenicsMemoryTheme(darkTheme = dark, dynamicColor = false) { primary = MaterialTheme.colorScheme.primary; accent = AppAccentTheme.colors.accent; Text("x") } }
        rule.waitForIdle()
        assertEquals(Teal40, primary)
        assertNotEquals(primary, accent)
        dark = true
        rule.waitForIdle()
        assertEquals(Teal80, primary)
        assertNotEquals(primary, accent)
    }

    @Test fun stepperClampsAndShowsValue() {
        rule.setContent {
            CalisthenicsMemoryTheme(darkTheme = false) {
                var n by remember { mutableIntStateOf(3) }
                Stepper(n, { n = it }, 1..4, valueText = { "$it rounds" })
            }
        }
        rule.onNodeWithTag("stepper_value").assertTextEquals("3 rounds")
        rule.onNodeWithTag("stepper_plus").performClick()
        rule.onNodeWithTag("stepper_value").assertTextEquals("4 rounds")
        rule.onNodeWithTag("stepper_plus").assertIsNotEnabled()
        rule.onNodeWithTag("stepper_minus").assertIsEnabled()
    }

    /** Screens being rewritten in U05–U12 still carry literals; the list only ever shrinks. Everything else must use string resources. */
    private val legacyUntilRewritten = setOf("TodayScreen.kt", "LibraryScreen.kt", "OnboardingScreen.kt", "SessionScreen.kt", "HistoryScreen.kt", "BackupScreen2.kt", "SkillTree.kt")

    @Test fun noHardCodedUiStringsOutsideLegacyScreens() {
        val src = File(System.getProperty("user.dir"), "src/main/java")
        val literal = Regex("""(Text\(\s*(text\s*=\s*)?"[^"$]|contentDescription\s*=\s*"[^"])""")
        val hits = src.walkTopDown().filter { it.extension == "kt" && it.name !in legacyUntilRewritten }
            .filter { literal.containsMatchIn(it.readText()) }.map { it.name }.toList()
        assertTrue("hard-coded UI text (use strings.xml): $hits", hits.isEmpty())
    }
}
