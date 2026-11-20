package io.github.gonbei774.calisthenicsmemory.session

import android.media.AudioAttributes
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class CueAudioAttributesTest {
    @Test fun warningSamplesUseMediaRouteAndMediaVolumeClassification() {
        val attributes = cueAudioAttributes()
        assertEquals(AudioAttributes.USAGE_MEDIA, attributes.usage)
        assertEquals(AudioAttributes.CONTENT_TYPE_SONIFICATION, attributes.contentType)
    }
}
