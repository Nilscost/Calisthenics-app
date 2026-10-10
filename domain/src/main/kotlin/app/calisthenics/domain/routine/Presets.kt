// V22 (O4, plan §3): ready-made routines. Structure and numbers only, our own wording, credited (docs/research/rr-live-check.md).
// A preset is a SavedRoutine built from the catalog and the equipment profile, so it plans, previews, runs and progresses like any saved routine.
// Each slot names the entry exercise of its path; the planner follows the person's own level in that family and substitutes by equipment.
package app.calisthenics.domain.routine

import app.calisthenics.domain.equipment.isAvailable
import app.calisthenics.domain.model.*

object Presets {
    const val RR = "preset-rr"
    const val MINIMALIST = "preset-minimalist"
    const val RR_CREDIT = "r/bodyweightfitness wiki, Recommended Routine (live check 2026-10-09)"
    const val MINIMALIST_CREDIT = "r/bodyweightfitness wiki, Minimalist Routine (from u/m092's Concept Wednesday post; live check 2026-10-09)"

    fun isPreset(id: String?) = id != null && id.startsWith("preset-")

    const val STRETCH = "preset-stretch"
    const val STRETCH_UPPER = "preset-stretch-upper"
    const val STRETCH_LOWER = "preset-stretch-lower"
    const val STRETCH_CREDIT = "r/flexibility wiki, Starting To Stretch (u/tykato; live check 2026-10-09)"

    fun all(c: Catalog, profile: EquipmentProfile): List<SavedRoutine> = listOf(recommended(c, profile), minimalist(c, profile)) + startingToStretch(c, profile)
    fun byId(id: String?, c: Catalog, profile: EquipmentProfile): SavedRoutine? = if (isPreset(id)) all(c, profile).firstOrNull { it.id == id } else null

    /** The first of [ids] the profile can do, else the first one (the planner then says what is missing or substitutes). */
    private fun pick(c: Catalog, profile: EquipmentProfile, vararg ids: String): String =
        ids.firstOrNull { id -> c.variation(id)?.let { isAvailable(it, profile) } == true } ?: ids.first()

    private fun slot(id: String, intent: Pattern, area: Area, variation: String, group: Int? = null, rest: Int? = null, from: Int? = null, to: Int? = null) =
        RoutineSlot(id, intent, area, variation, optional = false, group = group, repFrom = from, repTo = to, restSeconds = rest)

    /**
     * The Recommended Routine: three pairs (pull-up + squat, dip + hinge, row + push-up) of 3 sets of 5-8 with 90 s after every set,
     * then the core triplet (anti-extension, anti-rotation, extension) of 3 sets of 8-12 with 60 s. Plain rest (no stretch blocks), warm-up on.
     * Holds go from 10 to 30 s. The warm-up lists the RR items; the arch hang and the support hold only when the equipment exists.
     */
    fun recommended(c: Catalog, profile: EquipmentProfile): SavedRoutine {
        val rule = ProgressionRule(RuleKind.REP_RANGE, sets = 3, from = 5, to = 8, holdFrom = 10, holdTo = 30, sessions = 1)
        val rotation = pick(c, profile, "pallof-press", "side-plank")   // no band: the Copenhagen path (it starts with the side plank)
        val slots = listOf(
            slot("rr-pull", Pattern.PULL_VERTICAL, Area.UPPER_BODY, pick(c, profile, "pullup-band-assisted", "scapular-pull"), group = 1, rest = 90),
            slot("rr-squat", Pattern.SQUAT, Area.LOWER_BODY, "squat-air", group = 1, rest = 90),
            slot("rr-dip", Pattern.PUSH_VERTICAL, Area.UPPER_BODY, "dip-support-hold", group = 2, rest = 90),
            slot("rr-hinge", Pattern.HINGE, Area.LOWER_BODY, "rdl-bodyweight", group = 2, rest = 90),
            slot("rr-row", Pattern.PULL_HORIZONTAL, Area.UPPER_BODY, pick(c, profile, "inverted-row-bent-knees", "row-band"), group = 3, rest = 90),
            slot("rr-push", Pattern.PUSH_HORIZONTAL, Area.UPPER_BODY, "pushup-incline", group = 3, rest = 90),
            slot("rr-core-ext", Pattern.CORE_ANTI_EXTENSION, Area.CORE, "plank", group = 4, rest = 60, from = 8, to = 12),
            slot("rr-core-rot", if (rotation == "pallof-press") Pattern.CORE_ANTI_ROTATION else Pattern.CORE_ANTI_LATERAL, Area.CORE, rotation, group = 4, rest = 60, from = 8, to = 12),
            slot("rr-core-back", Pattern.HINGE, Area.CORE, "reverse-hyperextension", group = 4, rest = 60, from = 8, to = 12),
        )
        val warmup = listOf("warmup-shoulder-band", "warmup-squat-sky-reach", "warmup-wrist-prep", "warmup-dead-bug") +
            listOfNotNull("warmup-arch-hang".takeIf { c.variation(it)?.let { v -> isAvailable(v, profile) } == true },
                "warmup-support-hold".takeIf { c.variation(it)?.let { v -> isAvailable(v, profile) } == true })
        val name = "Recommended Routine"
        val routine = Routine(RR, 1, name, slots, rule = rule, warmup = warmup)
        return SavedRoutine(RR, name, 1, routine, PlanEdits0, WorkoutFormat.PAIRS, rule, stretchOn = false, sets = 3, warmupOn = true, credit = RR_CREDIT)
    }

    /**
     * The Minimalist Routine: one circuit of walking lunges, push-ups, rows and plank shoulder taps with little or no rest, 2-6 circuits
     * (the Custom rule changes the number). Any rep range; the app uses about 3 sets of 8-10 before an exercise gets harder.
     */
    fun minimalist(c: Catalog, profile: EquipmentProfile): SavedRoutine {
        val rule = ProgressionRule(RuleKind.REP_RANGE, sets = 3, from = 8, to = 10, holdFrom = 10, holdTo = 30, sessions = 1)
        val slots = listOf(
            slot("min-lunge", Pattern.LUNGE, Area.LOWER_BODY, "walking-lunge", rest = 0),
            slot("min-push", Pattern.PUSH_HORIZONTAL, Area.UPPER_BODY, "pushup-incline", rest = 0),
            slot("min-row", Pattern.PULL_HORIZONTAL, Area.UPPER_BODY, pick(c, profile, "inverted-row-bent-knees", "row-band"), rest = 0),
            slot("min-tap", Pattern.CORE_ANTI_EXTENSION, Area.CORE, pick(c, profile, "plank-shoulder-tap"), rest = 0),
        )
        val name = "Minimalist Routine"
        val routine = Routine(MINIMALIST, 1, name, slots, rule = rule)
        return SavedRoutine(MINIMALIST, name, 1, routine, PlanEdits0, WorkoutFormat.CIRCUIT, rule, stretchOn = false, sets = 3, warmupOn = false, credit = MINIMALIST_CREDIT)
    }

    private val UPPER = listOf("stretch-shoulder-backbend", "stretch-cobra", "stretch-rear-clasp", "stretch-supine-twist", "stretch-wrist-biceps")
    private val LOWER = listOf("stretch-pike-one-leg", "stretch-hip-flexor", "stretch-pancake", "stretch-butterfly", "stretch-calf-wall")

    /**
     * "Starting To Stretch": ten stretches in two halves (upper body, lower body), each with the bump-and-hold protocol (about 25 minutes for both halves).
     * The full session and each half are listed; the halves are for when time is short (alternate them). A stretch the profile cannot do (no chair, no wall) is left out.
     */
    fun startingToStretch(c: Catalog, profile: EquipmentProfile): List<SavedRoutine> {
        fun make(id: String, name: String, ids: List<String>): SavedRoutine {
            val ok = ids.filter { s -> c.variation(s)?.let { isAvailable(it, profile) } == true }
            val routine = Routine(id, 1, name, emptyList(), stretchSession = ok)
            return SavedRoutine(id, name, 1, routine, PlanEdits0, WorkoutFormat.CIRCUIT, ProgressionRule(), stretchOn = true, sets = 1, credit = STRETCH_CREDIT)
        }
        return listOf(make(STRETCH, "Starting To Stretch", UPPER + LOWER), make(STRETCH_UPPER, "Starting To Stretch: upper body", UPPER), make(STRETCH_LOWER, "Starting To Stretch: lower body", LOWER))
    }

    private val PlanEdits0 = app.calisthenics.domain.planner.PlanEdits()
}
