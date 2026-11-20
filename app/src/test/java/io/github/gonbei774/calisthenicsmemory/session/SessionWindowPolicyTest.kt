package io.github.gonbei774.calisthenicsmemory.session

import android.app.Activity
import android.view.WindowManager
import app.calisthenics.domain.session.*
import app.calisthenics.domain.model.TimelineBlock
import app.calisthenics.domain.model.BlockType
import app.calisthenics.domain.model.WorkoutPlan
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SessionWindowPolicyTest {
    private fun plan() = WorkoutPlan(id = "p", createdAtEpochMs = 0, blocks = listOf(
        TimelineBlock(id = "rest", type = BlockType.PASSIVE_RECOVERY, durationSeconds = 40)
    ), routineId = "r", routineRevision = 1, catalogVersion = 1, profileId = "home",
        requestedDurationSeconds = 40, plannedDurationSeconds = 40, focus = emptySet(), stretchOn = false,
        rounds = 1, changesExplained = emptyList(), warnings = emptyList(), needsAcceptance = false, usesDraftContent = false)

    @Test fun visibleRunningAndPausedUnfinishedSessionsKeepTheWindowAwake() {
        val running = reduce(newSession("s", plan()), SessionEvent.Start(0)).state
        assertTrue(shouldKeepSessionScreenOn(running, windowVisible = true))
        val paused = reduce(running, SessionEvent.Pause(1_000)).state
        assertTrue(shouldKeepSessionScreenOn(paused, windowVisible = true))
        val recoveryPrompt = reduce(running, SessionEvent.ProcessRecovered).state
        assertFalse(shouldKeepSessionScreenOn(recoveryPrompt, windowVisible = true))
        val recoveredPaused = reduce(recoveryPrompt, SessionEvent.RecoveryChoice(true)).state
        assertTrue(shouldKeepSessionScreenOn(recoveredPaused, windowVisible = true))
    }

    @Test fun absentRecoveryPromptTerminalAndBackgroundSessionsClearTheFlag() {
        assertFalse(shouldKeepSessionScreenOn(null, windowVisible = true))
        val ready = newSession("s", plan()).copy(phase = Phase.RECOVERY_REQUIRED)
        assertFalse(shouldKeepSessionScreenOn(ready, windowVisible = true))
        val completed = newSession("s", plan()).copy(phase = Phase.COMPLETED)
        assertFalse(shouldKeepSessionScreenOn(completed, windowVisible = true))
        val running = reduce(newSession("s", plan()), SessionEvent.Start(0)).state
        assertFalse(shouldKeepSessionScreenOn(running, windowVisible = false))
    }

    @Test fun attachedWindowFlagFollowsVisibleRunningPausedAndTerminalStates() {
        val activity = Robolectric.buildActivity(Activity::class.java).setup().get()
        val flag = WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
        val running = reduce(newSession("s", plan()), SessionEvent.Start(0)).state
        applySessionScreenPolicy(activity.window, running, windowVisible = true)
        assertTrue((activity.window.attributes.flags and flag) != 0)
        val paused = reduce(running, SessionEvent.Pause(1_000)).state
        applySessionScreenPolicy(activity.window, paused, windowVisible = true)
        assertTrue((activity.window.attributes.flags and flag) != 0)
        applySessionScreenPolicy(activity.window, paused, windowVisible = false)
        assertFalse((activity.window.attributes.flags and flag) != 0)
        applySessionScreenPolicy(activity.window, null, windowVisible = true)
        assertFalse((activity.window.attributes.flags and flag) != 0)
        applySessionScreenPolicy(activity.window, running.copy(phase = Phase.COMPLETED), windowVisible = true)
        assertFalse((activity.window.attributes.flags and flag) != 0)
        activity.finish()
    }
}
