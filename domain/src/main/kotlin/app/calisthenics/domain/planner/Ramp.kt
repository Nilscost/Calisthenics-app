// L12 (docs/15, RR warm-up): the warm-up as RAMP blocks: Raise, Activate, Mobilise, Potentiate, in that order. Items that belong to a later stage of a
// progression unlock from the ladder state (arch hang after the negative pull-ups, support hold after the negative dips, an easier squat after the Bulgarian
// split squat, an easier hinge after the banded Nordic curl); the wrist prep is only there before push and handstand work.
package app.calisthenics.domain.planner

import app.calisthenics.domain.equipment.isAvailable
import app.calisthenics.domain.goals.Goals
import app.calisthenics.domain.model.*
import app.calisthenics.domain.progression.ProgressSnapshot

enum class RampPhase(val label: String) { RAISE("Raise"), ACTIVATE("Activate"), MOBILISE("Mobilise"), POTENTIATE("Potentiate") }

/** [gate] = the exercises of which one must be reached (started, or something harder started) before the item appears; empty = always. */
data class RampItem(val id: String, val phase: RampPhase, val gate: List<String> = emptyList(), val pushOnly: Boolean = false)

object Ramp {
    /** The RR's eight warm-up items (plus the towel variant of the shoulder warm-up and the stick/T-shirt-free alternatives as they come). */
    val items = listOf(
        RampItem("warmup-squat-sky-reach", RampPhase.RAISE),
        RampItem("warmup-shoulder-band", RampPhase.MOBILISE),
        RampItem("warmup-shoulder-towel", RampPhase.MOBILISE),
        RampItem("warmup-wrist-prep", RampPhase.MOBILISE, pushOnly = true),
        RampItem("warmup-dead-bug", RampPhase.ACTIVATE),
        RampItem("warmup-arch-hang", RampPhase.ACTIVATE, gate = listOf("pullup-negative", "pullup-full")),
        RampItem("warmup-support-hold", RampPhase.POTENTIATE, gate = listOf("dip-negative", "dip-parallel")),
        RampItem("warmup-squat-easier", RampPhase.POTENTIATE, gate = listOf("split-squat-bulgarian")),
        RampItem("warmup-hinge-easier", RampPhase.POTENTIATE, gate = listOf("nordic-banded", "nordic-curl")),
    )
    private val byId = items.associateBy { it.id }
    private val order = RampPhase.entries

    fun phaseOf(id: String): RampPhase = byId[id]?.phase ?: RampPhase.MOBILISE

    /** Does the workout have push or handstand work (so the wrists need preparing)? */
    fun hasPushWork(patterns: Collection<Pattern>) = patterns.any { it == Pattern.PUSH_HORIZONTAL || it == Pattern.PUSH_VERTICAL }

    /**
     * The warm-up for [ids] (the routine's list or the catalog's), in RAMP order, without the items that are not unlocked yet, not usable with the
     * profile, or (wrist prep) not needed. An id that is not a RAMP item keeps its place among the mobilise items.
     */
    fun order(c: Catalog, profile: EquipmentProfile, progress: ProgressSnapshot, ids: List<String>, patterns: Collection<Pattern>): List<String> {
        val push = hasPushWork(patterns)
        return ids.filter { id ->
            val it = byId[id]
            val v = c.variation(id)
            v != null && isAvailable(v, profile) && (it == null || ((!it.pushOnly || push) && (it.gate.isEmpty() || it.gate.any { g -> Goals.reached(c, g, progress) })))
        }.withIndex().sortedWith(compareBy({ order.indexOf(phaseOf(it.value)) }, { it.index })).map { it.value }
    }
}
