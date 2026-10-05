package io.github.gonbei774.calisthenicsmemory.ui

import androidx.compose.ui.test.assertIsDisplayed
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
        listOf("Train", "Progress", "History", "Settings").forEach { rule.onNodeWithText(it).assertIsDisplayed() }
        listOf("train", "progress", "history", "settings").forEach { rule.onNodeWithTag("tab_$it").assertIsDisplayed() }
    }

    @Test fun tabsSwitchScreens() {
        launch(true)
        rule.onNodeWithTag("tab_settings").performClick()
        rule.onNodeWithText("Backup and restore").assertIsDisplayed()
        // The History tab reads Room; Robolectric's native SQLite is not available on linux-aarch64 (sandbox), so it is not opened here.
        rule.onNodeWithTag("tab_progress").performClick()
        rule.onNodeWithText("Exercise library").assertIsDisplayed()
    }

    @Test fun firstRunHidesTheTabBar() {
        launch(false)
        rule.onNodeWithText("Your starting level").assertIsDisplayed()
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
