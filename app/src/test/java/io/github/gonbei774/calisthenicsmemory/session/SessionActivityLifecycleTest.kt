package io.github.gonbei774.calisthenicsmemory.session

import android.os.Looper
import android.view.WindowManager
import app.calisthenics.domain.model.BlockType
import app.calisthenics.domain.model.TimelineBlock
import app.calisthenics.domain.model.WorkoutPlan
import app.calisthenics.domain.session.SessionEvent
import app.calisthenics.domain.session.newSession
import app.calisthenics.domain.session.reduce
import androidx.activity.ComponentActivity
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SessionActivityLifecycleTest {
    private fun plan() = WorkoutPlan(
        id = "p", routineId = "r", routineRevision = 1, catalogVersion = 1, createdAtEpochMs = 0,
        profileId = "home", requestedDurationSeconds = 40, plannedDurationSeconds = 40,
        focus = emptySet(), stretchOn = false, rounds = 1, changesExplained = emptyList(), warnings = emptyList(),
        needsAcceptance = false, usesDraftContent = false,
        blocks = listOf(TimelineBlock("rest", BlockType.PASSIVE_RECOVERY, 40)),
    )

    private fun drainMainQueue() = shadowOf(Looper.getMainLooper()).idle()

    @After fun cleanUp() { SessionBus.clear() }

    @Test fun lifecycleAppliesSessionStateAndReappliesItAfterReturningToForeground() {
        val controller = Robolectric.buildActivity(ComponentActivity::class.java).setup()
        val activity = controller.get()
        activity.bindSessionScreenPolicy()
        val flag = WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
        val running = reduce(newSession("s", plan()), SessionEvent.Start(0)).state
        SessionBus.publish(running)
        drainMainQueue()
        assertTrue((activity.window.attributes.flags and flag) != 0)

        controller.pause().stop()
        assertFalse((activity.window.attributes.flags and flag) != 0)

        controller.restart().resume().visible()
        drainMainQueue()
        assertTrue((activity.window.attributes.flags and flag) != 0)

        activity.finish()
        controller.pause().stop().destroy()
        val recreatedController = Robolectric.buildActivity(ComponentActivity::class.java).setup()
        val recreatedActivity = recreatedController.get()
        recreatedActivity.bindSessionScreenPolicy()
        drainMainQueue()
        assertTrue((recreatedActivity.window.attributes.flags and flag) != 0)

        SessionBus.publish(running.copy(phase = app.calisthenics.domain.session.Phase.COMPLETED))
        drainMainQueue()
        assertFalse((recreatedActivity.window.attributes.flags and flag) != 0)
        recreatedActivity.finish()
        recreatedController.pause().stop().destroy()
    }
}
