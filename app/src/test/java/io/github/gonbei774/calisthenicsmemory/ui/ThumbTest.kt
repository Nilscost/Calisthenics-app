package io.github.gonbei774.calisthenicsmemory.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.core.app.ApplicationProvider
import io.github.gonbei774.calisthenicsmemory.ui.components.ExerciseThumb
import io.github.gonbei774.calisthenicsmemory.ui.components.loadThumb
import io.github.gonbei774.calisthenicsmemory.ui.theme.CalisthenicsMemoryTheme
import io.github.gonbei774.calisthenicsmemory.ui.train.loadCatalog
import org.junit.Assert.assertNotNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** V09: every exercise has a thumbnail the app can decode, and the tile shows it. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ThumbTest {
    @get:Rule val rule = createComposeRule()

    @Test fun everyCatalogExerciseHasADecodableThumbnail() {
        val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
        for (v in loadCatalog(ctx).variations) assertNotNull("no thumbnail for ${v.id}", loadThumb(ctx, v.id))
    }

    @Test fun theTileIsShownAndFallsBackToTheInitialForAnUnknownId() {
        rule.setContent { CalisthenicsMemoryTheme(darkTheme = true) { ExerciseThumb("pushup-standard", "Standard Push-Up"); ExerciseThumb("no-such-exercise", "Zebra") } }
        rule.onNodeWithTag("thumb_pushup-standard").assertIsDisplayed()
        rule.onNodeWithTag("thumb_no-such-exercise").assertIsDisplayed()
    }
}
