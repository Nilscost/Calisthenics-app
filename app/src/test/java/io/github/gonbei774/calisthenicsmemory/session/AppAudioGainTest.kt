package io.github.gonbei774.calisthenicsmemory.session

import org.junit.Assert.assertEquals
import org.junit.Test

class AppAudioGainTest {
    @Test fun gainMapsTheSavedRangeAndClampsOutOfRangeValues() {
        assertEquals(0f, appAudioGain(0))
        assertEquals(0.3f, appAudioGain(30))
        assertEquals(1f, appAudioGain(100))
        assertEquals(0f, appAudioGain(-1))
        assertEquals(1f, appAudioGain(101))
    }
}
