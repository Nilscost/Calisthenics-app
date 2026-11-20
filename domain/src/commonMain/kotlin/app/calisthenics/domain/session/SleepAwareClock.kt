package app.calisthenics.domain.session

/**
 * A monotonic clock that keeps counting while the device sleeps. Browsers' `performance.now()` pauses while a phone is
 * locked; the wall clock does not. Whenever the wall clock moved noticeably more than the monotonic one between two
 * reads, the difference is added, so session timers (stored end times) are right again the moment the page returns.
 * A wall clock set backwards is ignored; one set forwards counts as sleep (acceptable for a workout timer).
 */
class SleepAwareClock(
    private val monotonicMs: () -> Double,
    private val wallMs: () -> Double,
    private val gapThresholdMs: Double = 1000.0,
) {
    private var adjust = 0.0
    private var lastMono = monotonicMs()
    private var lastWall = wallMs()

    fun nowMs(): Long {
        val mono = monotonicMs()
        val wall = wallMs()
        val gap = (wall - lastWall) - (mono - lastMono)
        if (gap > gapThresholdMs) adjust += gap
        lastMono = mono
        lastWall = wall
        return (mono + adjust).toLong()
    }
}
