// M6c: goals ("objectives") over the progression graph. A goal is a chain of exercises in the catalog; the plan
// trains the first step you have not mastered, or the prerequisite that unlocks it. DRAFT content (see catalog).
package app.calisthenics.domain.goals

import app.calisthenics.domain.model.*
import app.calisthenics.domain.progression.ProgressSnapshot

enum class NodeState { LOCKED, AVAILABLE, MASTERED }

data class Goal(
    val id: String, val name: String, val description: String,
    val entryVariationId: String? = null, val targetVariationId: String? = null,
    val focus: Set<StrengthFocus> = setOf(StrengthFocus.FULL_BODY),
)

/** R1 / doc 17: the three top-level kinds of objective. ROUTINE (ready-made and saved routines) only appears once presets exist (V22). */
enum class ObjectiveType { BODY_PART, SKILL, ROUTINE }

object Goals {
    fun typeOf(g: Goal): ObjectiveType = if (g.entryVariationId != null) ObjectiveType.SKILL else ObjectiveType.BODY_PART
    fun goalsOf(type: ObjectiveType): List<Goal> = all.filter { typeOf(it) == type }
    /** The types to offer: ROUTINE is hidden until there is something to put in it. */
    fun availableTypes(presetsExist: Boolean): List<ObjectiveType> =
        listOf(ObjectiveType.BODY_PART, ObjectiveType.SKILL) + if (presetsExist) listOf(ObjectiveType.ROUTINE) else emptyList()

    val all = listOf(
        Goal("general", "General strength", "Balanced full-body work; no skill target.", focus = setOf(StrengthFocus.FULL_BODY)),
        Goal("hspu", "Handstand push-up", "Pike push-up to wall handstand push-up.", "pike-pushup", "hspu-wall"),
        Goal("muscle-up", "Pull-up to muscle-up", "Band-assisted pull-up to bar muscle-up.", "pullup-band-assisted", "muscle-up-bar"),
        Goal("pistol", "Pistol squat", "Bulgarian split squat to pistol squat.", "split-squat-bulgarian", "squat-pistol"),
        Goal("one-arm-pushup", "One-arm push-up", "Standard push-up to one-arm push-up.", "pushup-standard", "pushup-one-arm"),
        Goal("l-sit", "L-sit and V-sit", "Hollow hold to V-sit.", "hollow-hold", "v-sit-floor"),
        Goal("front-lever", "Front lever", "Tuck lever to straddle lever. Hard skill.", "front-lever-tuck", "front-lever-straddle"),
        Goal("planche", "Planche", "Planche lean to advanced tuck planche. Hard skill.", "planche-lean", "planche-adv-tuck"),
        Goal("back-bridge", "Back bridge", "Glute bridge to full back bridge.", "glute-bridge", "bridge-back"),
        Goal("body-upper", "Focus: upper body", "Only upper-body exercises.", focus = setOf(StrengthFocus.UPPER_BODY)),
        Goal("body-lower", "Focus: lower body", "Only lower-body exercises.", focus = setOf(StrengthFocus.LOWER_BODY)),
        Goal("body-core", "Focus: core", "Only core exercises.", focus = setOf(StrengthFocus.CORE)),
    )
    fun byId(id: String?): Goal? = all.firstOrNull { it.id == id }
    fun focusFor(id: String?): Set<StrengthFocus> = byId(id)?.focus ?: setOf(StrengthFocus.FULL_BODY)

    /** Entry -> ... -> target, following the first successor each time. */
    fun chain(c: Catalog, g: Goal): List<String> {
        val start = g.entryVariationId ?: return emptyList()
        val out = mutableListOf(start); var cur = start
        while (cur != g.targetVariationId && out.size < 20) {
            cur = c.policyForVariation(cur)?.nextVariationIds?.firstOrNull() ?: break
            out += cur
        }
        return out
    }

    private fun met(vt: VariationTier, p: ProgressSnapshot) =
        p.variations[vt.variationId]?.let { vt.tier in it.achievedTiers || it.tier > vt.tier } ?: false

    /** L02: someone who has started a harder exercise that follows [id] has moved past it, so a step inserted below their level (the wall push-up) never locks them out. */
    private fun movedPast(c: Catalog, id: String, p: ProgressSnapshot): Boolean {
        val seen = mutableSetOf(id); val queue = ArrayDeque(listOf(id))
        while (queue.isNotEmpty()) {
            for (n in c.policyForVariation(queue.removeFirst())?.nextVariationIds.orEmpty()) {
                if (!seen.add(n)) continue
                if (p.variations.containsKey(n)) return true
                queue += n
            }
        }
        return false
    }

    fun unmet(c: Catalog, v: String, p: ProgressSnapshot): List<VariationTier> =
        (c.policyForVariation(v)?.prerequisiteRule?.allOf ?: emptyList()).mapNotNull { group ->
            val preds = group.mapNotNull { it.variationTierMet }
            if (preds.isEmpty() || preds.any { met(it, p) || movedPast(c, it.variationId, p) }) null else preds.first()
        }

    private fun mastered(v: String, p: ProgressSnapshot) = 5 in (p.variations[v]?.achievedTiers ?: emptySet())

    fun treeStates(c: Catalog, g: Goal, p: ProgressSnapshot): Map<String, NodeState> =
        chain(c, g).associateWith { v ->
            when {
                mastered(v, p) -> NodeState.MASTERED
                unmet(c, v, p).isEmpty() -> NodeState.AVAILABLE
                else -> NodeState.LOCKED
            }
        }

    /** The exercise to train now toward the goal: first unmastered step, or the prerequisite that unlocks it. */
    fun nextToTrain(c: Catalog, g: Goal, p: ProgressSnapshot): String? {
        val ch = chain(c, g); if (ch.isEmpty()) return null
        var v = ch.firstOrNull { !mastered(it, p) } ?: ch.last()
        repeat(10) {
            if (p.variations.containsKey(v)) return v
            val u = unmet(c, v, p).firstOrNull() ?: return v
            v = u.variationId
        }
        return v
    }

    fun routineFor(c: Catalog, base: Routine, goalId: String?, p: ProgressSnapshot): Routine {
        val g = byId(goalId) ?: return base
        val focused = base.copy(goalId = g.id, defaultFocus = g.focus)
        val target = nextToTrain(c, g, p)?.let { c.variation(it) } ?: return focused
        val area = target.areas.firstOrNull() ?: return focused
        val intent = target.patterns.first()
        val drop = base.slots.firstOrNull { it.area == area && it.intent == intent }
            ?: base.slots.firstOrNull { it.area == area } ?: return focused
        val slot = RoutineSlot("goal", intent, area, target.id)
        return focused.copy(slots = listOf(slot) + base.slots.filter { it !== drop })
    }
}
