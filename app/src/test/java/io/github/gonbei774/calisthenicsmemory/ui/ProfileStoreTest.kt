package io.github.gonbei774.calisthenicsmemory.ui

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo

import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ApplicationProvider
import app.calisthenics.domain.equipment.*
import io.github.gonbei774.calisthenicsmemory.ui.screens.ModeStore
import io.github.gonbei774.calisthenicsmemory.ui.screens.PrefsStore
import io.github.gonbei774.calisthenicsmemory.ui.screens.ProfileEditScreen
import io.github.gonbei774.calisthenicsmemory.ui.screens.ProfileStore
import io.github.gonbei774.calisthenicsmemory.ui.theme.CalisthenicsMemoryTheme
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ProfileStoreTest {
    @get:Rule val rule = createComposeRule()
    private val ctx: Context get() = ApplicationProvider.getApplicationContext()

    @Before fun clean() {
        for (n in listOf("profiles", "mode", "planner_prefs")) ctx.getSharedPreferences(n, Context.MODE_PRIVATE).edit().clear().commit()
    }

    @Test fun firstRunSeedsHomeAndTravel() {
        assertEquals(listOf("home", "travel"), ProfileStore.load(ctx).map { it.id })
        assertEquals("home", ProfileStore.selected(ctx).id)
    }

    @Test fun createUpdateDeleteArePersisted() {
        val gym = buildProfile("gym", "Gym", listOf(EquipmentSelection("high-bar")))
        ProfileStore.upsert(ctx, gym)
        assertEquals(listOf("home", "travel", "gym"), ProfileStore.load(ctx).map { it.id })
        ProfileStore.upsert(ctx, gym.copy(name = "Big gym"))
        assertEquals("Big gym", ProfileStore.load(ctx).single { it.id == "gym" }.name)
        ProfileStore.select(ctx, "gym")
        assertEquals("gym", ProfileStore.selected(ctx).id)
        assertTrue(ProfileStore.delete(ctx, "gym") is ProfileEditResult.Ok)
        assertEquals(listOf("home", "travel"), ProfileStore.load(ctx).map { it.id })
        assertEquals("home", ProfileStore.selected(ctx).id) // selection moved off the deleted profile
    }

    @Test fun theLastProfileCannotBeDeleted() {
        ProfileStore.delete(ctx, "travel")
        assertTrue(ProfileStore.delete(ctx, "home") is ProfileEditResult.Refused)
        assertEquals(listOf("home"), ProfileStore.load(ctx).map { it.id })
    }

    @Test fun oldHighBarToggleBecomesAnItemOnHome() {
        ctx.getSharedPreferences("mode", Context.MODE_PRIVATE).edit().putBoolean("highBar", true).commit()
        val home = ProfileStore.load(ctx).single { it.id == "home" }
        assertTrue(home.items.any { it.equipmentId == "high-bar" })
        assertFalse(ProfileStore.load(ctx).single { it.id == "travel" }.items.any { it.equipmentId == "high-bar" })
        assertFalse(ModeStore.takeLegacyHighBar(ctx)) // consumed once
    }

    @Test fun editorCreatesAGymProfileWithAHighBarThatUnlocksMuscleUp() {
        var done = false
        rule.setContent { CalisthenicsMemoryTheme(darkTheme = false) { ProfileEditScreen(profileId = null, onDone = { done = true }) } }
        rule.onNodeWithTag("profile_name").performTextInput("Gym")
        rule.onNodeWithTag("eq_pullup-bar").performClick()
        rule.onNodeWithTag("eq_high-bar").performClick()
        rule.onNodeWithTag("profile_save").performClick()
        rule.waitForIdle()
        assertTrue(done)
        val gym = ProfileStore.load(ctx).single { it.id == "gym" }
        assertEquals("gym", ProfileStore.selected(ctx).id)
        val catalog = app.calisthenics.domain.content.parseCatalog(java.io.File(System.getProperty("user.dir"), "src/main/assets/catalog.json").readText())
        assertTrue(isAvailable(catalog.variation("muscle-up-bar")!!, gym))
        assertFalse(isAvailable(catalog.variation("muscle-up-bar")!!, ProfileStore.load(ctx).single { it.id == "home" }))
    }

    @Test fun editorRefusesAnEmptyOrDuplicateName() {
        var done = false
        rule.setContent { CalisthenicsMemoryTheme(darkTheme = false) { ProfileEditScreen(profileId = null, onDone = { done = true }) } }
        rule.onNodeWithTag("profile_save").performClick()
        rule.onNodeWithText("Give the profile a name.").assertIsDisplayed()
        rule.onNodeWithTag("profile_name").performTextInput("home")
        rule.onNodeWithTag("profile_save").performClick()
        rule.onNodeWithText("You already have a profile with this name.").assertIsDisplayed()
        assertFalse(done)
        assertEquals(2, ProfileStore.load(ctx).size)
    }
}

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ProfileLeaveDialogTest {
    @get:Rule val rule = createComposeRule()
    private val ctx: Context get() = ApplicationProvider.getApplicationContext()
    private var done = false
    private var dispatcher: androidx.activity.OnBackPressedDispatcher? = null

    @Before fun clean() {
        for (n in listOf("profiles", "mode", "planner_prefs")) ctx.getSharedPreferences(n, Context.MODE_PRIVATE).edit().clear().commit()
        done = false
        rule.setContent {
            dispatcher = androidx.activity.compose.LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
            CalisthenicsMemoryTheme(darkTheme = false) { ProfileEditScreen(profileId = "home", onDone = { done = true }) }
        }
    }

    private fun back() = rule.onNodeWithContentDescription("Back").performClick()

    @Test fun unchangedGoesBackWithoutDialog() {
        back(); rule.waitForIdle()
        assertTrue(done)
    }

    @Test fun changeShowsDialogAndDismissStays() {
        rule.onNodeWithTag("profile_name").performTextInput("x")
        back(); rule.waitForIdle()
        rule.onNodeWithText("Discard").assertIsDisplayed()
        assertFalse(done)
    }

    @Test fun savePersistsAndReturns() {
        rule.onNodeWithTag("profile_name").performTextInput("x")
        back(); rule.onNodeWithTag("profile_leave_save").performClick(); rule.waitForIdle()
        assertTrue(done)
        assertTrue(ProfileStore.load(ctx).any { it.id == "home" && it.name.contains("x") })
    }

    @Test fun discardReturnsWithoutPersisting() {
        val before = ProfileStore.load(ctx)
        rule.onNodeWithTag("eq_kettlebell").performScrollTo().performClick()
        back(); rule.onNodeWithTag("profile_leave_discard").performClick(); rule.waitForIdle()
        assertTrue(done)
        assertEquals(before, ProfileStore.load(ctx))
    }

    @Test fun systemBackShowsDialogOnlyWhenDirty() {
        rule.onNodeWithTag("profile_name").performTextInput("x")
        rule.waitForIdle(); rule.runOnUiThread { dispatcher!!.onBackPressed() }; rule.waitForIdle()
        rule.onNodeWithTag("profile_leave_discard").assertIsDisplayed()
        assertFalse(done)
    }
}
