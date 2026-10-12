package app.calisthenics.domain

import app.calisthenics.domain.model.Target
import app.calisthenics.domain.model.TargetType
import org.junit.Assert.assertEquals
import org.junit.Test

/** R7-T01 (B2, B3): one helper decides what a target shows and what the logger starts from. */
class TargetShownValueTest {
    @Test fun aRangeShowsItsTop() {
        assertEquals(30, Target(TargetType.HOLD_SECONDS, 10, 30).shownValue())
        assertEquals(8, Target(TargetType.REPS, 5, 8).shownValue())
    }
    @Test fun aSingleNumberShowsItself() {
        assertEquals(12, Target(TargetType.REPS, 12).shownValue())
        assertEquals(20, Target(TargetType.HOLD_SECONDS, 20, 20).shownValue())
    }
}
