package io.github.gonbei774.calisthenicsmemory.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createComposeRule
import io.github.gonbei774.calisthenicsmemory.ui.screens.DemoPlayer
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

/** V01 / K1: when the block changes (stretch -> Split Squat), the player must load the new clip. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w411dp-h891dp-xxhdpi")
class DemoPlayerTest {
    @get:Rule val rule = createComposeRule()

    @Test fun changingTheFileLoadsTheNewClip() {
        val dir = createTempDir("clips")
        val a = File(dir, "calf-stretch.mp4").apply { writeBytes(ByteArray(8)) }
        val b = File(dir, "split-squat.mp4").apply { writeBytes(ByteArray(8)) }
        val loaded = mutableListOf<String>()
        var file by mutableStateOf(a)
        rule.setContent { DemoPlayer(file, controls = false, onLoad = { loaded += it }) }
        rule.waitForIdle()
        assertEquals(listOf(a.absolutePath), loaded)
        file = b
        rule.waitForIdle()
        assertEquals(listOf(a.absolutePath, b.absolutePath), loaded)
        file = a
        rule.waitForIdle()
        assertEquals(listOf(a.absolutePath, b.absolutePath, a.absolutePath), loaded)
    }

    @Test fun sameFileDoesNotReload() {
        val a = File(createTempDir("clips"), "x.mp4").apply { writeBytes(ByteArray(8)) }
        val loaded = mutableListOf<String>()
        var tick by mutableStateOf(0)
        rule.setContent { tick; DemoPlayer(a, controls = false, onLoad = { loaded += it }) }
        rule.waitForIdle(); tick++; rule.waitForIdle()
        assertEquals(1, loaded.size)
    }
}
