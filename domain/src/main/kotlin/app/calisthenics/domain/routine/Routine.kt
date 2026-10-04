// T09 — familiar routines, focus and optional goals (PLAN-01, PLAN-04, PLAN-05, PLAN-06, PROG-01).
package app.calisthenics.domain.routine

import app.calisthenics.domain.model.Area
import app.calisthenics.domain.model.Preferences
import app.calisthenics.domain.model.Routine
import app.calisthenics.domain.model.RoutineSlot
import app.calisthenics.domain.model.StretchArea
import app.calisthenics.domain.model.StrengthFocus

/** Spec §4.2: FULL_BODY clears specific focuses; specific focuses combine by union; empty = FULL_BODY. */
fun normalizeFocus(selected: Set<StrengthFocus>): Set<StrengthFocus> =
    if (selected.isEmpty() || StrengthFocus.FULL_BODY in selected) setOf(StrengthFocus.FULL_BODY) else selected

/** Toggle helper for UI chips with the same normalization rule. */
fun toggleFocus(current: Set<StrengthFocus>, tapped: StrengthFocus): Set<StrengthFocus> = when {
    tapped == StrengthFocus.FULL_BODY -> setOf(StrengthFocus.FULL_BODY)
    tapped in current -> normalizeFocus(current - tapped)
    else -> current - StrengthFocus.FULL_BODY + tapped
}

fun areasFor(focus: Set<StrengthFocus>): Set<Area> {
    val f = normalizeFocus(focus)
    if (StrengthFocus.FULL_BODY in f) return Area.entries.toSet()
    return f.map {
        when (it) {
            StrengthFocus.UPPER_BODY -> Area.UPPER_BODY
            StrengthFocus.LOWER_BODY -> Area.LOWER_BODY
            StrengthFocus.CORE -> Area.CORE
            StrengthFocus.FULL_BODY -> error("unreachable")
        }
    }.toSet()
}

/** Same rule for stretch focus: FULL_BODY is exclusive. */
fun normalizeStretchAreas(selected: Set<StretchArea>): Set<StretchArea> =
    if (selected.isEmpty() || StretchArea.FULL_BODY in selected) setOf(StretchArea.FULL_BODY) else selected

/** Slots kept for a focus, preserving routine order (PLAN-01). */
fun slotsForFocus(routine: Routine, focus: Set<StrengthFocus>): List<RoutineSlot> {
    val areas = areasFor(focus)
    return routine.slots.filter { it.area in areas }
}

/**
 * Session draft = routine + today-only overlay (spec §3). Swaps and strength focus are
 * session-only unless explicitly saved; duration/toggles/stretch focus are remembered.
 */
data class SessionDraft(
    val durationSeconds: Int,
    val stretchOn: Boolean,
    val stretchAreas: Set<StretchArea>,
    val warmupOn: Boolean,
    val cooldownOn: Boolean,
    val focus: Set<StrengthFocus>,
    val profileId: String,
    val swaps: Map<String, String> = emptyMap(), // slotId -> variationId, today only
    val goalId: String? = null,
) {
    companion object {
        fun from(prefs: Preferences, routine: Routine) = SessionDraft(
            durationSeconds = prefs.defaultDurationSeconds,
            stretchOn = prefs.stretchOn,
            stretchAreas = normalizeStretchAreas(prefs.stretchAreas),
            warmupOn = prefs.warmupOn,
            cooldownOn = prefs.cooldownOn,
            focus = normalizeFocus(routine.defaultFocus),
            profileId = prefs.selectedProfileId,
            goalId = routine.goalId,
        )
    }
}

/** What is remembered after the user presses Start (spec §3). Swaps/focus are NOT. */
fun rememberOnStart(prefs: Preferences, draft: SessionDraft): Preferences = prefs.copy(
    defaultDurationSeconds = draft.durationSeconds,
    stretchOn = draft.stretchOn,
    stretchAreas = draft.stretchAreas,
    warmupOn = draft.warmupOn,
    cooldownOn = draft.cooldownOn,
    selectedProfileId = draft.profileId,
)

/** Explicit "save to usual plan" (PLAN-05): creates a NEW revision; old plans keep their snapshot. */
fun saveSwapsToRoutine(routine: Routine, swaps: Map<String, String>): Routine {
    if (swaps.isEmpty()) return routine
    return routine.copy(
        revision = routine.revision + 1,
        slots = routine.slots.map { s -> swaps[s.id]?.let { s.copy(preferredVariationId = it) } ?: s },
    )
}

fun saveFocusToRoutine(routine: Routine, focus: Set<StrengthFocus>): Routine =
    routine.copy(revision = routine.revision + 1, defaultFocus = normalizeFocus(focus))

/** Explicit, persistent goal change (PROG-01). History is untouched by design (separate store). */
fun setGoal(routine: Routine, goalId: String?): Routine =
    if (routine.goalId == goalId) routine else routine.copy(revision = routine.revision + 1, goalId = goalId)

/** "Avoid this exercise in future" (PLAN-05). */
fun avoidVariation(prefs: Preferences, variationId: String): Preferences =
    prefs.copy(excludedVariationIds = prefs.excludedVariationIds + variationId)

// ---------- weekly frequency (PLAN-06) ----------

const val SUGGESTED_SESSIONS_PER_WEEK = 3 // owner's current baseline, not a prescription

data class WeekSummary(val weekIndex: Int, val sessions: Int, val suggested: Int, val message: String)

/**
 * Weekly summary over local day numbers (caller converts with the saved time zone).
 * Weeks are Monday-based when [mondayOfWeek0] is a Monday day number.
 * No streaks, no penalties, no "missed" wording.
 */
fun weeklySummary(sessionDays: List<Int>, mondayOfWeek0: Int, suggested: Int = SUGGESTED_SESSIONS_PER_WEEK): List<WeekSummary> {
    if (sessionDays.isEmpty()) return emptyList()
    val byWeek = sessionDays.groupBy { Math.floorDiv(it - mondayOfWeek0, 7) }
    val first = byWeek.keys.min(); val last = byWeek.keys.max()
    return (first..last).map { w ->
        val n = byWeek[w]?.size ?: 0
        val msg = when {
            n == 0 -> "No sessions this week."
            n >= suggested -> "$n sessions — at or above the suggested $suggested."
            else -> "$n of a suggested $suggested sessions."
        }
        WeekSummary(w, n, suggested, msg)
    }
}

/** Seed routine matching the owner's familiar full-body circuit; preferred variations are starter-catalog ids. */
object SeedRoutine {
    val fullBody = Routine(
        id = "usual",
        revision = 1,
        name = "Usual full-body circuit",
        slots = listOf(
            RoutineSlot("slot-push", app.calisthenics.domain.model.Pattern.PUSH_HORIZONTAL, Area.UPPER_BODY, "pushup-knee"),
            RoutineSlot("slot-squat", app.calisthenics.domain.model.Pattern.SQUAT, Area.LOWER_BODY, "squat-air"),
            RoutineSlot("slot-pull", app.calisthenics.domain.model.Pattern.PULL_HORIZONTAL, Area.UPPER_BODY, "row-band"),
            RoutineSlot("slot-hinge", app.calisthenics.domain.model.Pattern.HINGE, Area.LOWER_BODY, "glute-bridge"),
            RoutineSlot("slot-core", app.calisthenics.domain.model.Pattern.CORE_ANTI_EXTENSION, Area.CORE, "plank-forearm"),
            RoutineSlot("slot-lunge", app.calisthenics.domain.model.Pattern.LUNGE, Area.LOWER_BODY, "split-squat", optional = true),
            RoutineSlot("slot-core2", app.calisthenics.domain.model.Pattern.CORE_ANTI_ROTATION, Area.CORE, "dead-bug", optional = true),
        ),
    )
}
