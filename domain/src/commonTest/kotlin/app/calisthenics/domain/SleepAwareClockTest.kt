package app.calisthenics.domain

import app.calisthenics.domain.session.SleepAwareClock
import kotlin.test.Test
import kotlin.test.assertEquals

class SleepAwareClockTest {
    private var mono = 1000.0
    private var wall = 5_000_000.0
    private val clock = SleepAwareClock({ mono }, { wall })

    private fun advance(ms: Double, sleptMs: Double = 0.0) { mono += ms; wall += ms + sleptMs }

    @Test fun followsMonotonicClockWhileAwake() {
        advance(200.0); assertEquals(1200L, clock.nowMs())
        advance(5000.0); assertEquals(6200L, clock.nowMs()) // a throttled background tab still advances both clocks
    }

    @Test fun addsTimeTheDeviceSlept() {
        advance(200.0); clock.nowMs()
        advance(100.0, sleptMs = 60_000.0)
        assertEquals(1300L + 60_000L, clock.nowMs())
        advance(200.0)
        assertEquals(1500L + 60_000L, clock.nowMs())
    }

    @Test fun ignoresWallClockMovedBackwardsAndSmallJitter() {
        advance(200.0); clock.nowMs()
        wall -= 3_600_000.0
        assertEquals(1200L, clock.nowMs())
        advance(200.0, sleptMs = 400.0)
        assertEquals(1400L, clock.nowMs())
    }
}
