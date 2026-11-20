package app.calisthenics.domain.planner

import app.calisthenics.domain.equipment.isAvailable
import app.calisthenics.domain.model.*

data class ProgramItemUnavailable(val exerciseId: String, val missing: String)

sealed interface FixedProgramPlanResult {
    data class Ready(val plan: WorkoutPlan) : FixedProgramPlanResult
    data class Unavailable(val programId: String, val items: List<ProgramItemUnavailable>) : FixedProgramPlanResult
}

/** Builds exactly the catalog item order and prescriptions (one set, no rest between items unless flow is off). It never substitutes or drops an item. */
fun buildFixedProgramPlan(
    catalog: Catalog,
    program: FixedProgram,
    profile: EquipmentProfile,
    flow: Boolean = program.flow,
    nowEpochMs: Long = 0L,
): FixedProgramPlanResult = buildProgramPlan(catalog, programContentOf(program), profile, flow, 1, WorkoutFormat.CIRCUIT, nowEpochMs)
