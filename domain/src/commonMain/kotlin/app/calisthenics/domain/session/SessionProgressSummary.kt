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
import app.calisthenics.domain.progression.LevelUpBy
import app.calisthenics.domain.progression.ProgressSnapshot
import app.calisthenics.domain.progression.ProgressionEngine
import app.calisthenics.domain.progression.VariationProgress
import app.calisthenics.domain.planner.cappedTarget
import app.calisthenics.domain.progression.prescribedTarget
import app.calisthenics.domain.tree.displayStars

/**
 * Good sessions so far towards the next level: [have] of [need], on the 14-day path or (when [weekly]) the 4-in-30-days path. [toLevel] 6 = mastery.
 * [finished]: the streak that raised the level in this session, all filled; the level it was earned at is `toLevel - 1`.
 */
data class LevelProgress(val have: Int, val need: Int, val weekly: Boolean, val toLevel: Int, val finished: Boolean = false)

/** Why the card says something other than "unlocked" about the level (9.4 F4, F5). */
enum class LevelNote {
    NONE,
    /** The first session of the exercise: this is the level it started at, nothing was unlocked ("Starting at Level 3"). */
    STARTING,
    /** The level dropped after a break of 14 days or more and this session was done there ("Level 3 of 5 · back after a break"). */
    BACK_AFTER_BREAK,
    /** A level held before was earned again ("Level 4 again"). */
    AGAIN,
}

/** How [NextTime.value] relates to the level's own target: the usual climb, the same target once more after a first miss, or a lowered target that builds back up to [NextTime.backTo]. */
enum class NextTimeKind { NORMAL, AGAIN, THEN_BACK }

/** The per-set number the planner will prescribe next time (reps, or seconds when [isHold]). */
data class NextTime(val value: Int, val isHold: Boolean, val kind: NextTimeKind = NextTimeKind.NORMAL, val backTo: Int? = null)

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
    /** Why [leveledUpTo] was reached (null when no level was reached): three good sessions, or typed reps that already met the new level. */
    val levelUpBy: LevelUpBy? = null,
    /** Mastered (now or before) and the route has a next step that is now the one in training: the exercise the person moves on to. */
    val nextVariationId: String? = null,
    /** The level the next exercise is really at now (from the after-snapshot; 1 when it has no record yet). */
    val nextVariationTier: Int = 1,
    /** 9.4 F3: what the planner prescribes next time (null for mastered, easier-next-time, paused and level-5 top-target cards). */
    val nextTime: NextTime? = null,
    /** The level's own target and the next level's, for "good sessions at 5+ reps → Level 4 (8 reps)"; null when not a plain reps / seconds target. */
    val levelTarget: Int? = null,
    val nextLevelTarget: Int? = null,
    val levelNote: LevelNote = LevelNote.NONE,
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
        val firstSession = b == null || b.lastTrainedDay == null
        val backFromBreak = a.backFromBreakDay != null && a.backFromBreakDay != b?.backFromBreakDay
        // 9.4 F4/F5: not an unlock when it is the starting level, a level after a break, or a level held before
        val note = when {
            masteredNow || masteredAlready -> LevelNote.NONE
            firstSession -> LevelNote.STARTING
            backFromBreak -> LevelNote.BACK_AFTER_BREAK
            leveledUp && a.tier <= (b?.highestTierReached ?: 0) -> LevelNote.AGAIN
            else -> LevelNote.NONE
        }
        val unlock = leveledUp && note == LevelNote.NONE
        val hold = after.holds[fam]
        val easier = hold != null && hold.steps > (before.holds[fam]?.steps ?: 0)
        val streakLevelUp = leveledUp && a.levelUpBy == LevelUpBy.STREAK
        val dots = when {
            masteredAlready || hold != null || a.autoPaused -> null
            // reached by three good sessions: the finished streak at the old level (the engine clears it, so it is rebuilt from the days before + today)
            streakLevelUp -> a.lastAutoAdvanceDay?.let { day -> levelProgress(engine.qualifyingDays(before, vid) + day, tierBefore).let { it.copy(have = it.need, finished = true) } }
            else -> levelProgress(engine.qualifyingDays(after, vid), a.tier)
        }
        val tierTarget = pol.tiers.firstOrNull { it.index == a.tier }?.target
        val newlyReduced = a.reducedTarget != null && a.reducedTargetTier == a.tier && a.reducedTarget != b?.reducedTarget
        val kept = !easier && !leveledUp && !masteredNow && a.tier == tierBefore && a.consecutiveBelow > (b?.consecutiveBelow ?: 0)
        val routeNext = if (masteredAlready) routeNextStep(catalog, after, vid) else null
        val ownTarget = pol.tiers.firstOrNull { it.index == a.tier }?.target?.let { cappedTarget(it, pol) }
        val nextOwn = pol.tiers.firstOrNull { it.index == a.tier + 1 }?.target?.let { cappedTarget(it, pol) }
        ExerciseProgress(
            variationId = vid, hasProgression = true, tier = a.tier.coerceIn(1, 5),
            stars = starsAfter, starsBefore = displayStars(catalog, before, vid),
            leveledUpTo = if (unlock) a.tier else null, mastered = masteredNow, masteredAlready = masteredAlready,
            progress = dots, easierNextTime = easier,
            nextTarget = if (newlyReduced && !easier) a.reducedTarget
                else rampedNext(engine, pol, before, after, vid, b, a).takeIf { !easier && !leveledUp && !masteredAlready && a.tier == tierBefore },
            nextTargetIsHold = tierTarget?.type == TargetType.HOLD_SECONDS,
            levelKept = kept,
            levelUpBy = if (unlock) a.levelUpBy else null,
            nextVariationId = routeNext,
            nextVariationTier = routeNext?.let { after.variations[it]?.tier?.coerceIn(1, 5) } ?: 1,
            nextTime = if (masteredAlready || masteredNow || easier || a.autoPaused || hold != null) null else nextTime(engine, pol, after, vid, a),
            levelTarget = ownTarget?.takeIf { it.max == null }?.value,
            nextLevelTarget = nextOwn?.takeIf { it.max == null && it.type == ownTarget?.type }?.value,
            levelNote = note,
        )
    }
    return SessionProgressSummary(exercises, newExercises(catalog, before, after, done))
}

/** The number the planner prescribes next time: a pending first miss holds the target, a lowered target is in force, else the ramp (the same [prescribedTarget] call as the planner). */
private fun nextTime(engine: ProgressionEngine, pol: app.calisthenics.domain.model.ProgressionPolicy, after: ProgressSnapshot, vid: String, a: VariationProgress): NextTime? {
    val own = pol.tiers.firstOrNull { it.index == a.tier }?.target?.let { cappedTarget(it, pol) } ?: return null
    val hold = own.type == TargetType.HOLD_SECONDS
    if (a.tier == 5 && (a.topTarget ?: 0) > 0) return null   // the raised level-5 target keeps its own behaviour
    if (a.missedTarget != null && a.missedTargetTier == a.tier) return NextTime(a.missedTarget!!, hold, NextTimeKind.AGAIN)
    if (a.reducedTarget != null && a.reducedTargetTier == a.tier) {
        val r = minOf(a.reducedTarget!!, own.value)
        return NextTime(r, hold, if (r < own.value) NextTimeKind.THEN_BACK else NextTimeKind.NORMAL, own.value)
    }
    return NextTime(prescribedTarget(pol, a.tier, engine.qualifyingDays(after, vid))?.target?.value ?: own.value, hold)
}

/** The main step after [vid] on its route, when [vid] is a main step itself and the route has actually moved on to it (prerequisites and equipment allowed it). */
private fun routeNextStep(catalog: Catalog, after: ProgressSnapshot, vid: String): String? {
    val route = catalog.mainRouteFor(vid) ?: return null
    if (vid !in route.mainPath) return null
    val next = catalog.nextMain(vid) ?: return null
    return next.takeIf { catalog.variation(it) != null && after.activeInFamily[catalog.variation(vid)?.familyId] == it }
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
        out.getOrPut(now) { NewExercise(now, from) }
    }
    // prerequisites that this session newly met
    for (v in catalog.variations) {
        if (out.size >= MAX_NEW_EXERCISES) break
        if (v.id in out || !ok(v.id)) continue
        // the next main step of an exercise the person is still training is not news: it is announced when the route moves on to it
        if (isNextStepOfExerciseStillInTraining(catalog, after, v.id, v.familyId)) continue
        val pol = catalog.policyForVariation(v.id) ?: continue
        if (Goals.unmet(catalog, v.id, before).isEmpty() || Goals.unmet(catalog, v.id, after).isNotEmpty()) continue
        val by = done.firstOrNull { d -> pol.prerequisiteRule?.allOf.orEmpty().any { g -> g.any { it.variationTierMet?.variationId == d } } }
        out[v.id] = NewExercise(v.id, by)
    }
    return out.values.take(MAX_NEW_EXERCISES)
}

/** True when [vid] is a `nextVariationIds` entry of the variation the person currently trains in its family and has not mastered yet. */
private fun isNextStepOfExerciseStillInTraining(catalog: Catalog, after: ProgressSnapshot, vid: String, familyId: String): Boolean {
    val activeId = after.activeInFamily[familyId] ?: return false
    if (activeId == vid) return false
    if (5 in (after.variations[activeId]?.achievedTiers ?: emptySet())) return false
    return vid in (catalog.policyForVariation(activeId)?.nextVariationIds.orEmpty())
}

/** L4: the climbing target inside a level, when this session moved it up (the same function the planner uses); null when it did not change or the lowered target is in force. */
private fun rampedNext(engine: ProgressionEngine, pol: app.calisthenics.domain.model.ProgressionPolicy, before: ProgressSnapshot, after: ProgressSnapshot, vid: String,
    b: VariationProgress?, a: VariationProgress): Int? {
    fun at(p: VariationProgress, s: ProgressSnapshot) = if (p.reducedTarget != null && p.reducedTargetTier == p.tier) null
        else prescribedTarget(pol, p.tier, engine.qualifyingDays(s, vid))?.takeIf { it.ramped }?.target?.value
    val now = at(a, after) ?: return null
    return now.takeIf { b == null || b.tier != a.tier || at(b, before) != now }
}
