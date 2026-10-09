// Maps "what I do today" (reps or seconds for one comfortable set) to a starting step. Conservative: highest step whose target
// the person already meets, never above it. Weighted or unlisted variants are NOT credited extra: the caller says so to the user.
package app.calisthenics.domain.intake

import app.calisthenics.domain.model.Catalog
import app.calisthenics.domain.model.ExerciseVariation
import app.calisthenics.domain.model.OnboardingFamily
import app.calisthenics.domain.model.Pattern
import app.calisthenics.domain.model.Routine
import app.calisthenics.domain.model.Tier

/** Highest tier index whose target value is <= [value]; 1 when below the first step. */
fun suggestStartingStep(tiers: List<Tier>, value: Int): Int =
    tiers.filter { it.target.value <= value }.maxOfOrNull { it.index } ?: tiers.minOf { it.index }

/** True when the person already exceeds the top step, so the app should say the ladder is too easy for them. */
fun exceedsLadder(tiers: List<Tier>, value: Int): Boolean = value > tiers.maxOf { it.target.value }

// ---------------------------------------------------------------------------------------------
// U08: the first-run questionnaire. One answer per family, not per exercise.

enum class Guess { EASY, NORMAL, HARD }

data class StartLevel(val variationId: String, val tier: Int)

/** What the person said for one family. */
sealed interface FamilyAnswer {
    /** "I do this": the exercise, reps (seconds for holds) in one comfortable set, and the rounds they usually do. */
    data class Does(val variationId: String, val value: Int, val rounds: Int) : FamilyAnswer
    /** "I don't know": easy / normal / hard. */
    data class NotSure(val guess: Guess) : FamilyAnswer
}

/** "I do this": the step whose target the person already meets (see [suggestStartingStep]). */
fun levelFromDoes(c: Catalog, a: FamilyAnswer.Does): StartLevel? {
    val pol = c.policyForVariation(a.variationId) ?: return null
    return StartLevel(a.variationId, suggestStartingStep(pol.tiers, a.value))
}

/**
 * "I don't know" (owner Q2). Normal = the anchor at step 3. Easy = one exercise easier at step 3; Hard = one harder at step 3.
 * With no neighbour: anchor step 1 (easy) / step 5 (hard). An anchor the equipment rules out (no pull-up bar) falls back
 * to the nearest usable exercise on the ladder, easier ones first.
 */
fun levelFor(c: Catalog, f: OnboardingFamily, guess: Guess, usable: (ExerciseVariation) -> Boolean = { true }): StartLevel {
    val ladder = f.ladder.mapNotNull { id -> c.variation(id)?.takeIf { usable(it) } }
    val fullAnchorIdx = f.ladder.indexOf(f.anchorVariationId)
    val anchor = ladder.firstOrNull { it.id == f.anchorVariationId }
        ?: f.ladder.take(fullAnchorIdx).reversed().firstNotNullOfOrNull { id -> ladder.firstOrNull { it.id == id } }
        ?: ladder.firstOrNull()
        ?: return StartLevel(f.anchorVariationId, 1)
    val i = ladder.indexOf(anchor)
    val steps = c.policyForVariation(anchor.id)?.tiers?.size ?: 5
    return when (guess) {
        Guess.NORMAL -> StartLevel(anchor.id, 3)
        Guess.EASY -> ladder.getOrNull(i - 1)?.let { StartLevel(it.id, 3) } ?: StartLevel(anchor.id, 1)
        Guess.HARD -> ladder.getOrNull(i + 1)?.let { StartLevel(it.id, 3) } ?: StartLevel(anchor.id, steps.coerceAtMost(5))
    }
}

fun levelFor(c: Catalog, f: OnboardingFamily, a: FamilyAnswer, usable: (ExerciseVariation) -> Boolean = { true }): StartLevel? = when (a) {
    is FamilyAnswer.Does -> levelFromDoes(c, a)
    is FamilyAnswer.NotSure -> levelFor(c, f, a.guess, usable)
}

/**
 * R3: every exercise of the family's progression for the level page, easier to harder: the entry ladder first, then everything
 * that follows it through `nextVariationIds`, ordered by difficulty. Each exercise appears once. Picking a harder one without its
 * prerequisites is allowed (self-assessment).
 */
fun fullProgression(c: Catalog, f: OnboardingFamily): List<ExerciseVariation> {
    val seen = LinkedHashSet<String>()
    f.ladder.forEach { seen += it }
    val queue = ArrayDeque(f.ladder)
    while (queue.isNotEmpty()) {
        val id = queue.removeFirst()
        c.policyForVariation(id)?.nextVariationIds.orEmpty().forEach { if (seen.add(it)) queue += it }
    }
    val ladderIds = f.ladder.toSet()
    val rest = seen.filter { it !in ladderIds }.mapNotNull { c.variation(it) }.sortedBy { it.difficultyRank }
    return f.ladder.mapNotNull { c.variation(it) } + rest
}

/** Shoulders are only asked when the push-up answer is at least the family's threshold (standard push-up). */
fun shouldAsk(c: Catalog, f: OnboardingFamily, pushup: FamilyAnswer?): Boolean {
    val min = f.requiresPushupAtLeast ?: return true
    val minRank = c.variation(min)?.difficultyRank ?: return true
    return when (pushup) {
        null -> false
        is FamilyAnswer.NotSure -> pushup.guess != Guess.EASY
        is FamilyAnswer.Does -> (c.variation(pushup.variationId)?.difficultyRank ?: 0) >= minRank
    }
}

/**
 * The usual plan follows the pull answer: a pull-up answer turns the pull slot into a vertical pull
 * (the starter routine only had a band row). Other families already follow the active exercise of their family.
 */
fun routineFromAnswers(c: Catalog, base: Routine, pull: StartLevel?): Routine {
    val v = pull?.let { c.variation(it.variationId) } ?: return base
    val slot = base.slots.firstOrNull { it.id == "pull" } ?: return base
    val intent = v.patterns.firstOrNull { it == Pattern.PULL_VERTICAL || it == Pattern.PULL_HORIZONTAL } ?: return base
    if (slot.intent == intent && slot.preferredVariationId == v.id) return base
    return base.copy(revision = base.revision + 1, slots = base.slots.map { if (it.id == "pull") it.copy(intent = intent, preferredVariationId = v.id) else it })
}

/** Rounds to suggest on the style page: the typical (rounded mean) of what the person said they do, within the planner's limits. */
fun suggestedRounds(answers: Collection<FamilyAnswer?>, default: Int = app.calisthenics.domain.planner.DEFAULT_TRAIN_ROUNDS): Int {
    val r = answers.filterIsInstance<FamilyAnswer.Does>().map { it.rounds }
    return if (r.isEmpty()) default else Math.round(r.average()).toInt().coerceIn(1, app.calisthenics.domain.planner.MAX_EXPLICIT_ROUNDS)
}
