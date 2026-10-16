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
    /** R7-T09 (D2): exercises of the supporting chains that help the skill (their current step at the person's level is trained too). DRAFT. */
    val supports: List<String> = emptyList(),
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
        Goal("hspu", "Handstand push-up", "Pike push-up to a freestanding handstand push-up.", "pike-pushup", "hspu-free", supports = listOf("plank", "dip-support-hold")),
        Goal("muscle-up", "Pull-up to muscle-up", "Band-assisted pull-up to bar muscle-up.", "pullup-band-assisted", "muscle-up-bar", supports = listOf("dip-support-hold", "hollow-hold")),
        Goal("pistol", "Pistol squat", "Bulgarian split squat to pistol squat.", "split-squat-bulgarian", "squat-pistol", supports = listOf("rdl-bodyweight", "calf-raise")),
        Goal("one-arm-pushup", "One-arm push-up", "Standard push-up to one-arm push-up.", "pushup-standard", "pushup-one-arm", supports = listOf("plank", "side-plank")),
        Goal("l-sit", "L-sit and V-sit", "Hollow hold to V-sit.", "hollow-hold", "v-sit-floor", supports = listOf("dip-support-hold", "plank")),
        Goal("front-lever", "Front lever", "Build from a tuck toward a full front lever.", "front-lever-tuck", "front-lever", supports = listOf("hollow-hold", "front-lever-row-tuck")),
        Goal("planche", "Planche", "Build from a lean toward a full planche. Hard skill.", "planche-lean", "planche-full", supports = listOf("hollow-hold", "dip-support-hold")),
        Goal("back-bridge", "Back bridge", "Glute bridge to full back bridge.", "glute-bridge", "bridge-back", supports = listOf("superman-hold", "plank")),
        Goal("handstand", "Handstand", "Build wall control, then balance freely.", "handstand-wall-plank", "handstand-free", supports = listOf("hollow-hold", "pike-pushup")),
        Goal("back-lever", "Back lever", "Progress from a tuck to a full back lever.", "back-lever-tuck", "back-lever", supports = listOf("pullup-false-grip-hang", "skin-the-cat")),
        Goal("dragon-flag", "Dragon flag", "Build a controlled full-body dragon flag.", "dragon-flag-candlestick", "dragon-flag", supports = listOf("hollow-hold", "hanging-knee-raise")),
        Goal("shrimp-squat", "Shrimp squat", "Build to a single-leg shrimp squat.", "squat-shrimp", "squat-shrimp-advanced", supports = listOf("calf-raise", "wall-sit")),
        Goal("flag", "Clutch flag", "Build toward a clutch flag on a vertical support.", "flag-clutch-hang", "flag-clutch", focus = setOf(StrengthFocus.UPPER_BODY, StrengthFocus.CORE), supports = listOf("side-plank", "pullup-full")),
        Goal("body-upper", "Focus: upper body", "Only upper-body exercises.", focus = setOf(StrengthFocus.UPPER_BODY)),
        Goal("body-lower", "Focus: lower body", "Only lower-body exercises.", focus = setOf(StrengthFocus.LOWER_BODY)),
        Goal("body-core", "Focus: core", "Only core exercises.", focus = setOf(StrengthFocus.CORE)),
    )
    fun byId(id: String?): Goal? = all.firstOrNull { it.id == id }
    fun focusFor(id: String?): Set<StrengthFocus> = byId(id)?.focus ?: setOf(StrengthFocus.FULL_BODY)

    /** Entry -> ... -> target, following the first successor each time. */
    fun chain(c: Catalog, g: Goal): List<String> {
        val start = g.entryVariationId ?: return emptyList()
        val explicit = c.progressionRoutes.firstOrNull { g.id in it.goalIds && g.targetVariationId in it.mainPath }
        if (explicit != null) return explicit.mainPath.take(explicit.mainPath.indexOf(g.targetVariationId) + 1)
        val out = mutableListOf(start); var cur = start
        while (cur != g.targetVariationId && out.size < 20) {
            cur = c.policyForVariation(cur)?.progressionSuccessors()?.firstOrNull() ?: break
            out += cur
        }
        return out
    }

    /** Use the furthest started main rung so a newly inserted earlier rung never resets an existing trainee. */
    private fun currentMainIndex(c: Catalog, g: Goal, chain: List<String>, p: ProgressSnapshot): Int {
        val route = c.progressionRoutes.firstOrNull { g.id in it.goalIds && g.targetVariationId in it.mainPath }
        val path = route?.mainPath ?: chain
        val startedIndex = path.indexOfLast { p.variations.containsKey(it) }
        if (startedIndex >= 0) {
            val started = path[startedIndex]
            if (!mastered(started, p)) return chain.indexOf(started).takeIf { it >= 0 } ?: 0
            val next = path.drop(startedIndex + 1).firstOrNull { !mastered(it, p) }
            if (next != null) return chain.indexOf(next).takeIf { it >= 0 } ?: 0
        }
        return chain.indexOfFirst { !mastered(it, p) }.takeIf { it >= 0 } ?: chain.lastIndex
    }

    private fun met(vt: VariationTier, p: ProgressSnapshot) =
        p.variations[vt.variationId]?.let { vt.tier in it.achievedTiers || it.tier > vt.tier } ?: false

    /** L02: someone who has started a harder exercise that follows [id] has moved past it, so a step inserted below their level (the wall push-up) never locks them out. */
    /** L12: has the person reached [id]: started it, or started something harder that follows it. */
    fun reached(c: Catalog, id: String, p: ProgressSnapshot): Boolean = p.variations.containsKey(id) || movedPast(c, id, p)

    private fun movedPast(c: Catalog, id: String, p: ProgressSnapshot): Boolean {
        val seen = mutableSetOf(id); val queue = ArrayDeque(listOf(id))
        while (queue.isNotEmpty()) {
            val current = queue.removeFirst()
            val successors = if (c.routesFor(current).isNotEmpty()) c.routesFor(current).flatMap { route ->
                val index = route.mainPath.indexOf(current)
                if (index >= 0) route.mainPath.drop(index + 1) else emptyList()
            }.distinct() else c.policyForVariation(current)?.nextVariationIds.orEmpty()
            for (n in successors) {
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
        var v = ch[currentMainIndex(c, g, ch, p)]
        repeat(10) {
            if (p.variations.containsKey(v)) return v
            val u = unmet(c, v, p).firstOrNull() ?: return v
            v = u.variationId
        }
        return v
    }

    /** R7-T09 (B11, D2): the exercise to train for [v] at the person's level: itself if started or unlocked, else the first prerequisite that unlocks it (recursively). */
    private fun trainable(c: Catalog, v: String, p: ProgressSnapshot): String {
        var cur = v
        repeat(10) {
            if (p.variations.containsKey(cur)) return cur
            cur = unmet(c, cur, p).firstOrNull()?.variationId ?: return cur
        }
        return cur
    }

    /**
     * R7-T09 (D2): what a session for the skill [g] is made of, at the person's level and in this order: the chain's current step (once its
     * prerequisites are met or it was started), else its unmet prerequisites (also from other tabs), then the current step of each supporting chain.
     * With [profile] set, exercises the equipment cannot do are left out.
     */
    fun skillExercises(c: Catalog, g: Goal, p: ProgressSnapshot, profile: EquipmentProfile? = null): List<String> {
        val ch = chain(c, g); if (ch.isEmpty()) return emptyList()
        val step = ch[currentMainIndex(c, g, ch, p)]
        val out = LinkedHashSet<String>()
        val missing = unmet(c, step, p)
        if (missing.isEmpty() || p.variations.containsKey(step)) out += step
        if (missing.isNotEmpty()) missing.forEach { out += trainable(c, it.variationId, p) }
        g.supports.forEach { out += trainable(c, it, p) }
        return out.filter { id -> c.variation(id)?.let { !it.demonstrationHold && (it.kind == Kind.REPS || it.kind == Kind.HOLD) && c.policyFor(it) != null && (profile == null || app.calisthenics.domain.equipment.isAvailable(it, profile)) } == true }
    }

    /** Slot ids of the exercises a skill brought in: the first is "goal", the rest "skill-n". */
    fun isSkillSlot(id: String) = id == "goal" || id.startsWith("skill-")

    /**
     * A body-part goal keeps the usual plan with its focus. A skill goal (B11) builds the session from the exercises that make the skill
     * possible ([skillExercises]); the planner and the exercises-per-round default then fill it up to the session length.
     */
    fun routineFor(c: Catalog, base: Routine, goalId: String?, p: ProgressSnapshot, profile: EquipmentProfile? = null): Routine {
        val g = byId(goalId) ?: return base
        val focused = base.copy(goalId = g.id, defaultFocus = g.focus, slots = base.slots.map { slot ->
            if (slot.progressionRouteId != null) slot else slot.copy(progressionRouteId = c.mainRouteFor(slot.preferredVariationId, g.id)?.id)
        })
        if (g.entryVariationId == null) return focused
        val ex = skillExercises(c, g, p, profile).mapNotNull { c.variation(it) }
        if (ex.isEmpty()) return focused
        return focused.copy(slots = ex.mapIndexed { i, v ->
            val route = c.mainRouteFor(v.id, g.id)
            RoutineSlot(if (i == 0) "goal" else "skill-${i + 1}", v.patterns.first(), v.areas.firstOrNull() ?: Area.UPPER_BODY, v.id,
                progressionRouteId = route?.id)
        })
    }
}
