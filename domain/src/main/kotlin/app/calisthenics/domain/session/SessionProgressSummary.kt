// What the end-of-workout screen shows about progress: the snapshot BEFORE the session compared with the one AFTER it.
// Pure: the caller replays history without and with the finished session (the same replay the Progress tab uses); nothing here parses event text.
package app.calisthenics.domain.session

import app.calisthenics.domain.LEVEL_UP_SESSIONS
import app.calisthenics.domain.LEVEL_UP_SLOW_SESSIONS
import app.calisthenics.domain.LEVEL_UP_SLOW_WINDOW_DAYS
import app.calisthenics.domain.LEVEL_UP_WINDOW_DAYS
import app.calisthenics.domain.feedback.Execution
import app.calisthenics.domain.goals.Goals
import app.calisthenics.domain.model.BlockType
import app.calisthenics.domain.model.Catalog
import app.calisthenics.domain.model.TargetType
import app.calisthenics.domain.model.WorkoutPlan
import app.calisthenics.domain.progression.ProgressSnapshot
import app.calisthenics.domain.progression.ProgressionEngine
import app.calisthenics.domain.tree.displayStars

/** Good sessions so far towards the next level: [have] of [need], on the 14-day path or (when [weekly]) the 4-in-30-days path. [toLevel] 6 = mastery. */
data class LevelProgress(val have: Int, val need: Int, val weekly: Boolean, val toLevel: Int)

data class ExerciseProgress(
    val variationId: String,
    /** False for exercises without levels (stretches, timed extras, programs): the screen shows a plain row. */
    val hasProgression: Boolean,
    val tier: Int = 1,
    val stars: Int = 0,
    val starsBefore: Int = 0,
    /** The level reached by this session (null = no new level). */
    val leveledUpTo: Int? = null,
    /** All five levels were completed by this session. */
    val mastered: Boolean = false,
    /** Mastered before (or now): no dots are shown. */
    val masteredAlready: Boolean = false,
    val progress: LevelProgress? = null,
    val easierNextTime: Boolean = false,
    val nextTarget: Int? = null,
    val nextTargetIsHold: Boolean = false,
    val levelKept: Boolean = false,
)

data class NewExercise(val variationId: String, val unlockedBy: String?)

data class SessionProgressSummary(val exercises: List<ExerciseProgress>, val newExercises: List<NewExercise>) {
    val levelUps: List<ExerciseProgress> get() = exercises.filter { it.leveledUpTo != null && !it.mastered }
    val mastered: List<ExerciseProgress> get() = exercises.filter { it.mastered }
    val hasUnlocks: Boolean get() = levelUps.isNotEmpty() || mastered.isNotEmpty() || newExercises.isNotEmpty()
}

/** The exercises with work that counts (MET or PARTIAL, the same rule as History), in plan order. */
fun doneVariationIds(plan: WorkoutPlan, executions: Map<String, Execution>): List<String> =
    plan.blocks.filter { it.type == BlockType.WORK && it.variationId != null && executions[it.id].let { e -> e == Execution.COMPLETED || e == Execution.PARTIAL } }
        .mapNotNull { it.variationId }.distinct()

private const val MAX_NEW_EXERCISES = 3

fun summarizeSessionProgress(catalog: Catalog, before: ProgressSnapshot, after: ProgressSnapshot, done: List<String>): SessionProgressSummary {
    val engine = ProgressionEngine(catalog)
    val exercises = done.map { vid ->
        val a = after.variations[vid]
        val pol = a?.effectivePolicySnapshot ?: catalog.policyForVariation(vid)
        if (a == null || pol == null) return@map ExerciseProgress(vid, hasProgression = false)
        val b = before.variations[vid]
        val fam = a.familyId
        val tierBefore = b?.tier ?: 1
        val masteredNow = 5 in a.achievedTiers && (b == null || 5 !in b.achievedTiers)
        val starsAfter = displayStars(catalog, after, vid)
        // an exercise already moved past (a harder one of its route was started) shows all five stars, like the Progress tab: no level or dots then
        val masteredAlready = 5 in a.achievedTiers || starsAfter >= 5
        val leveledUp = !masteredNow && a.tier > tierBefore && (b != null || a.demonstratedTier != null)
        val hold = after.holds[fam]
        val easier = hold != null && hold.steps > (before.holds[fam]?.steps ?: 0)
        val dots = if (masteredAlready || hold != null || a.autoPaused) null else levelProgress(engine.qualifyingDays(after, vid), a.tier)
        val tierTarget = pol.tiers.firstOrNull { it.index == a.tier }?.target
        val newlyReduced = a.reducedTarget != null && a.reducedTargetTier == a.tier && a.reducedTarget != b?.reducedTarget
        val kept = !easier && !leveledUp && !masteredNow && a.tier == tierBefore && a.consecutiveBelow > (b?.consecutiveBelow ?: 0)
        ExerciseProgress(
            variationId = vid, hasProgression = true, tier = a.tier.coerceIn(1, 5),
            stars = starsAfter, starsBefore = displayStars(catalog, before, vid),
            leveledUpTo = if (leveledUp) a.tier else null, mastered = masteredNow, masteredAlready = masteredAlready,
            progress = dots, easierNextTime = easier,
            nextTarget = if (newlyReduced && !easier) a.reducedTarget else null,
            nextTargetIsHold = tierTarget?.type == TargetType.HOLD_SECONDS,
            levelKept = kept,
        )
    }
    return SessionProgressSummary(exercises, newExercises(catalog, before, after, done))
}

/** Good sessions on the closer of the two paths: 3 inside 14 days, or 4 inside 30 days (the weekly path). */
internal fun levelProgress(days: List<Int>, tier: Int): LevelProgress {
    val d = days.distinct().sorted()
    fun best(window: Int) = d.indices.maxOfOrNull { i -> d.count { it >= d[i] && it - d[i] < window } } ?: 0
    val fast = minOf(best(LEVEL_UP_WINDOW_DAYS), LEVEL_UP_SESSIONS)
    val slow = minOf(best(LEVEL_UP_SLOW_WINDOW_DAYS), LEVEL_UP_SLOW_SESSIONS)
    // compare fast/3 with slow/4 without fractions; a tie stays on the faster path
    return if (fast * LEVEL_UP_SLOW_SESSIONS >= slow * LEVEL_UP_SESSIONS) LevelProgress(fast, LEVEL_UP_SESSIONS, false, tier + 1)
    else LevelProgress(slow, LEVEL_UP_SLOW_SESSIONS, true, tier + 1)
}

private fun newExercises(catalog: Catalog, before: ProgressSnapshot, after: ProgressSnapshot, done: List<String>): List<NewExercise> {
    val out = linkedMapOf<String, NewExercise>()
    fun ok(vid: String) = vid !in done && vid !in before.variations && catalog.variation(vid) != null
    // the route moved on: the exercise the person now trains in a family is a different one
    for ((fam, now) in after.activeInFamily) {
        if (before.activeInFamily[fam] == now || !ok(now)) continue
        val from = done.firstOrNull { catalog.variation(it)?.familyId == fam }
        out.putIfAbsent(now, NewExercise(now, from))
    }
    // prerequisites that this session newly met
    for (v in catalog.variations) {
        if (out.size >= MAX_NEW_EXERCISES) break
        if (v.id in out || !ok(v.id)) continue
        val pol = catalog.policyForVariation(v.id) ?: continue
        if (Goals.unmet(catalog, v.id, before).isEmpty() || Goals.unmet(catalog, v.id, after).isNotEmpty()) continue
        val by = done.firstOrNull { d -> pol.prerequisiteRule?.allOf.orEmpty().any { g -> g.any { it.variationTierMet?.variationId == d } } }
        out[v.id] = NewExercise(v.id, by)
    }
    return out.values.take(MAX_NEW_EXERCISES)
}
