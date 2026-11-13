package app.calisthenics.domain.model

import kotlinx.serialization.Serializable

/** A prospective definition, keyed by exercise rather than its potentially shared catalog policy. */
@Serializable
data class PersonalLevelTargets(val revision: Int, val values: List<Int>, val modifiedAtEpochMs: Long = 0L) {
    init {
        require(revision > 0) { "Target revision must be positive" }
        require(values.size == 5 && values.all { it > 0 }) { "Define five positive targets" }
        require(values.zipWithNext().all { (a, b) -> a <= b }) { "Targets must be nondecreasing" }
    }
}
