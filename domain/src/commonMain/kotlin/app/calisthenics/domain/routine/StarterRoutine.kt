// Starter "usual plan": one slot per movement pattern group. Preferred variations are the ladder entry
// points the planner then moves along with progression. DRAFT: owner reviews at G2.
package app.calisthenics.domain.routine

import app.calisthenics.domain.model.Area
import app.calisthenics.domain.model.Pattern
import app.calisthenics.domain.model.Routine
import app.calisthenics.domain.model.RoutineSlot

object StarterRoutine {
    val routine = Routine(
        id = "usual", revision = 1, name = "My usual plan",
        slots = listOf(
            RoutineSlot("push", Pattern.PUSH_HORIZONTAL, Area.UPPER_BODY, "pushup-incline"),
            RoutineSlot("squat", Pattern.SQUAT, Area.LOWER_BODY, "squat-air"),
            RoutineSlot("pull", Pattern.PULL_HORIZONTAL, Area.UPPER_BODY, "row-band"),
            RoutineSlot("core", Pattern.CORE_ANTI_EXTENSION, Area.CORE, "plank"),
            RoutineSlot("hinge", Pattern.HINGE, Area.LOWER_BODY, "glute-bridge", optional = true),
            RoutineSlot("core2", Pattern.CORE_ANTI_LATERAL, Area.CORE, "side-plank", optional = true),
        ),
    )
}
