package io.github.gonbei774.calisthenicsmemory.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import io.github.gonbei774.calisthenicsmemory.ui.nav.AppNav
import io.github.gonbei774.calisthenicsmemory.ui.screens.OnboardingStore
import io.github.gonbei774.calisthenicsmemory.ui.theme.CalisthenicsMemoryTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class NavShellTest {
    @get:Rule val rule = createComposeRule()

    private fun launch(onboarded: Boolean) {
        OnboardingStore.setDone(ApplicationProvider.getApplicationContext(), onboarded)
        rule.setContent { CalisthenicsMemoryTheme(darkTheme = false) { AppNav() } }
    }

    @Test fun fourTabsExistAfterOnboarding() {
        launch(true)
        listOf("train" to "Train", "progress" to "Progress", "history" to "History", "settings" to "Settings").forEach { (tag, label) ->
            rule.onNodeWithTag("tab_$tag").assertIsDisplayed().assertTextContains(label)
        }
    }

    @Test fun tabsSwitchScreens() {
        launch(true)
        rule.onNodeWithTag("tab_settings").performClick()
        rule.onNodeWithText("Backup and restore").assertIsDisplayed()
        // The History tab reads Room; Robolectric's native SQLite is not available on linux-aarch64 (sandbox), so it is not opened here.
        rule.onNodeWithTag("tab_progress").performClick()
        rule.onNodeWithTag("tab_chip_push").assertIsDisplayed()
    }

    @Test fun firstRunHidesTheTabBar() {
        launch(false)
        rule.onNodeWithTag("onb_title").assertTextEquals("Welcome")
        rule.onNodeWithTag("bottom_bar").assertDoesNotExist()
    }

    @Test fun oldForkEntriesAreNotReachable() {
        launch(true)
        listOf("train", "progress", "settings").forEach { t ->
            rule.onNodeWithTag("tab_$t").performClick()
            listOf("S'entraîner", "À faire", "Enregistrer l'entraînement", "To Do", "Record", "Create").forEach {
                rule.onNodeWithText(it).assertDoesNotExist()
            }
        }
    }
}

/** U01b: the old fork's screens and strings are deleted from the source tree, not just hidden. */
class OldForkRemovedTest {
    private val root = java.io.File(System.getProperty("user.dir"))

    @org.junit.Test fun noOldForkStringsInResources() {
        val banned = listOf("S'entraîner", "S\\'entraîner", "À faire", "Enregistrer")
        val hits = java.io.File(root, "src/main/res").walkTopDown().filter { it.extension == "xml" }
            .flatMap { f -> banned.filter { it in f.readText() }.map { "${f.path}: $it" } }.toList()
        org.junit.Assert.assertTrue("old fork strings still present: $hits", hits.isEmpty())
        org.junit.Assert.assertFalse(java.io.File(root, "src/main/res/values-fr").exists())
    }

    @org.junit.Test fun noOldForkClassesInSource() {
        val banned = listOf("TrainingViewModel", "ToDoScreen", "RecordScreen", "WorkoutScreen", "CreateScreen", "ProgramListScreen",
            "IntervalListScreen", "CommunityShare", "CsvDataManagementScreen", "ShareHubScreen", "SettingsScreenNew")
        val hits = java.io.File(root, "src/main/java").walkTopDown().filter { it.extension == "kt" }
            .flatMap { f -> banned.filter { it in f.readText() }.map { "${f.name}: $it" } }.toList()
        org.junit.Assert.assertTrue("old fork code still referenced: $hits", hits.isEmpty())
    }
}
