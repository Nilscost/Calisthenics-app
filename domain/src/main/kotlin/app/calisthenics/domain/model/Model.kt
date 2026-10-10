// T05 — immutable domain contracts (docs/02-coding-specification.md §2–§3).
// Pure Kotlin: no Android, no wall clock, no Room. All values immutable.
package app.calisthenics.domain.model

import kotlinx.serialization.Serializable

// ---------- enums ----------

@Serializable enum class Muscle {
    CHEST, FRONT_DELTS, SIDE_DELTS, REAR_DELTS, TRICEPS, BICEPS, FOREARMS, LATS, UPPER_BACK, LOWER_BACK,
    ABS, OBLIQUES, GLUTES, QUADS, HAMSTRINGS, CALVES, HIP_FLEXORS, ADDUCTORS,
}

@Serializable enum class Kind { REPS, HOLD, STRETCH, MOBILITY }

/** Strength areas used by focus selection. */
@Serializable enum class Area { UPPER_BODY, LOWER_BODY, CORE }

/** Strength focus. FULL_BODY is mutually exclusive with the specific ones (spec §4.2). */
@Serializable enum class StrengthFocus { FULL_BODY, UPPER_BODY, LOWER_BODY, CORE }

@Serializable enum class StretchArea { FULL_BODY, CALF, ANKLE, HIP, HAMSTRING, QUAD, BACK, CHEST, SHOULDER, WRIST }

@Serializable enum class Pattern {
    PUSH_HORIZONTAL, PUSH_VERTICAL, PULL_HORIZONTAL, PULL_VERTICAL,
    SQUAT, LUNGE, HINGE, CALF_RAISE,
    CORE_ANTI_EXTENSION, CORE_ANTI_ROTATION, CORE_ANTI_LATERAL,
    STRETCH, MOBILITY,
}

@Serializable enum class ReviewState { DRAFT, REVIEWED }

@Serializable enum class PolicyKind { REVIEWED_GUIDANCE, PRODUCT_HEURISTIC }

@Serializable enum class Side { NONE, LEFT, RIGHT, BOTH }

@Serializable enum class BlockType { WORK, STRETCH, PASSIVE_RECOVERY, WARMUP, COOLDOWN, TRANSITION }

@Serializable enum class TargetType { REPS, HOLD_SECONDS }

/** V19 (D11, O1): how the exercises of a workout are ordered. Circuit = all exercises, then the next round; Pairs = two exercises alternate for all their sets; Straight sets = all sets of one exercise, then the next. */
@Serializable enum class WorkoutFormat { CIRCUIT, PAIRS, STRAIGHT }

// ---------- equipment ----------

/** One required item. Mass range only where relevant; band strength is a label, never kg. */
@Serializable
data class EquipmentNeed(
    val equipmentId: String,
    val quantity: Int = 1,
    val minMassGrams: Int? = null,
    val maxMassGrams: Int? = null,
    val suitability: Set<String> = emptySet(),
) {
    init { require(quantity >= 1) { "quantity must be >= 1" } }
}

/** AND inside a set: every need and every environment capability. Empty = bodyweight. */
@Serializable
data class RequirementSet(
    val needs: List<EquipmentNeed> = emptyList(),
    val capabilities: Set<String> = emptySet(),
)

@Serializable
data class EquipmentItem(
    val equipmentId: String,
    val quantity: Int = 1,
    val massGrams: Int? = null,
    val strengthLabel: String? = null,
    val suitability: Set<String> = emptySet(),
)

@Serializable
data class EquipmentProfile(
    val id: String,
    val name: String,
    val items: List<EquipmentItem>,
    val capabilities: Set<String>,
)

// ---------- content ----------

@Serializable
data class Target(val type: TargetType, val value: Int, /** V21: the top of a rep range (5-8): [value] is the bottom, [max] the top that moves you on. Null = a single number. */ val max: Int? = null) {
    init { require(value > 0) { "target must be positive" }; require(max == null || max >= value) { "range top below its bottom" } }
    override fun toString(): String = (if (max != null && max != value) "$value-$max" else "$value") + if (type == TargetType.REPS) " reps" else "s hold"
}

/** O2: how progress is judged for a routine. App levels = today's five-target tiers; Rep range = sets x from-to (a session where every set reaches the top moves you on); Custom = the same, with everything editable. */
/** L01: the shared tier vocabulary. R reps, RU reps per side, C core reps, H30 holds to 30 s, H60 accumulated holds to one minute, N negatives, E endurance (calves, tibialis), M McGill maintenance. */
@Serializable enum class TierProfile { R, RU, C, H30, H60, N, E, M }

@Serializable enum class RuleKind { APP_LEVELS, REP_RANGE, CUSTOM }

@Serializable
data class ProgressionRule(
    val kind: RuleKind = RuleKind.APP_LEVELS,
    val sets: Int = 3,
    val from: Int = 5,
    val to: Int = 8,
    /** Holds (seconds): they go on at the top, RR: 10-30 s. */
    val holdFrom: Int = 10,
    val holdTo: Int = 30,
    /** Qualifying sessions in a row before moving on (Rep range: 1; Custom: editable). */
    val sessions: Int = 1,
) {
    init { require(sets in 1..10 && from in 1..to && holdFrom in 1..holdTo && sessions in 1..10) { "rule values out of range" } }
    val isRange: Boolean get() = kind != RuleKind.APP_LEVELS
    fun withKind(k: RuleKind) = when (k) { RuleKind.REP_RANGE -> copy(kind = k, sessions = 1); else -> copy(kind = k) }
}

@Serializable
data class Tier(
    val index: Int,
    val target: Target,
    val workWindowSeconds: Int,
    val minRecoverySeconds: Int,
    val minQualifyingBlocks: Int = 2,
    val earlyCompletionStretchId: String? = null,
) {
    init {
        require(index in 1..5) { "tier index must be 1..5" }
        require(workWindowSeconds > 0) { "work window must be positive" }
        require(minRecoverySeconds >= 0) { "recovery must be >= 0" }
        require(minQualifyingBlocks >= 1) { "minQualifyingBlocks >= 1" }
        if (target.type == TargetType.HOLD_SECONDS) {
            require(target.value <= workWindowSeconds) { "hold target cannot exceed work window" }
        }
    }
}

/** Typed prerequisite predicates — no free-text evaluation (spec §3). */
@Serializable
data class Predicate(
    val variationTierMet: VariationTier? = null,
    val equipmentAvailable: String? = null,
) {
    init { require((variationTierMet == null) != (equipmentAvailable == null)) { "exactly one predicate kind" } }
}

@Serializable data class VariationTier(val variationId: String, val tier: Int)

/** AND of groups; each group is an OR of predicates. Empty = no prerequisite. */
@Serializable data class PrerequisiteRule(val allOf: List<List<Predicate>> = emptyList())

@Serializable
data class ProgressionPolicy(
    val id: String,
    val variationId: String,
    val version: Int,
    val tiers: List<Tier>,
    val nextVariationIds: List<String> = emptyList(),
    val prerequisiteRule: PrerequisiteRule = PrerequisiteRule(),
    val evidenceSourceIds: List<String> = emptyList(),
    val policyKind: PolicyKind = PolicyKind.PRODUCT_HEURISTIC,
    val approved: Boolean = false,
    /** L01: the shared tier vocabulary this policy's five tiers follow (null = hand-written numbers). Appended last; old catalogs decode. */
    val tierProfile: TierProfile? = null,
    /** L01: which of the tier numbers have a source and which are DRAFT (free text, our words). */
    val tierNote: String? = null,
) {
    fun tier(index: Int): Tier = tiers.first { it.index == index }
}

@Serializable
data class ExerciseVariation(
    val id: String,
    val familyId: String,
    val name: String,
    val patterns: Set<Pattern>,
    val areas: Set<Area> = emptySet(),
    val stretchAreas: Set<StretchArea> = emptySet(),
    val kind: Kind,
    val equipmentAlternatives: List<RequirementSet> = listOf(RequirementSet()),
    val unilateral: Boolean = false,
    /** Body position; a change between consecutive blocks costs one setup transition. */
    val position: String = "floor",
    val instructions: List<String> = emptyList(),
    val formCues: List<String> = emptyList(),
    val cautions: List<String> = emptyList(),
    val difficultyRank: Int = 0,
    val mediaId: String? = null,
    val sourceIds: List<String> = emptyList(),
    val reviewState: ReviewState = ReviewState.DRAFT,
    val reviewer: String? = null,
    val reviewedAt: String? = null,
    val progressionPolicyId: String? = null,
    val compatibleStretchIds: List<String> = emptyList(),
    /** For STRETCH / MOBILITY: seconds per side (unilateral) or total. */
    val defaultSeconds: Int? = null,
    /** U09 (F9): muscles worked. Primary = the main movers (clips colour them saturated), secondary = helpers (light). */
    val primaryMuscles: List<Muscle> = emptyList(),
    val secondaryMuscles: List<Muscle> = emptyList(),
)

@Serializable enum class EdgeRelation { PREREQUISITE, RECOMMENDED_PREPARATION }

@Serializable
data class SkillNode(
    val id: String,
    val name: String,
    val description: String = "",
    val variationIds: List<String>,
    val sourceIds: List<String> = emptyList(),
    val reviewState: ReviewState = ReviewState.DRAFT,
)

@Serializable
data class SkillEdge(
    val id: String,
    val fromId: String,
    val toId: String,
    val relation: EdgeRelation,
    val criterion: String = "",
)

/**
 * One page of the questionnaire. [ladder] is the entry-level chain, easier to harder (never a hard skill, so "I don't know"
 * cannot land on one). [anchorVariationId] is the owner's "Normal" (Q2): Normal = anchor at step 3, Easy/Hard = the
 * neighbour on the ladder at step 3, or the anchor at step 1 / 5 when there is no neighbour.
 */
@Serializable
data class OnboardingFamily(
    val id: String,
    val title: String,
    val ladder: List<String>,
    val anchorVariationId: String,
    /** Only asked when the push-up answer is at least this exercise (shoulders). */
    val requiresPushupAtLeast: String? = null,
)

@Serializable
data class Catalog(
    val catalogVersion: Int,
    val variations: List<ExerciseVariation>,
    val policies: List<ProgressionPolicy>,
    val skillNodes: List<SkillNode> = emptyList(),
    val skillEdges: List<SkillEdge> = emptyList(),
    val warmupTemplate: List<String> = emptyList(),
    val cooldownTemplate: List<String> = emptyList(),
    /** U08: the families the first-run questionnaire asks about. Appended last with a default. */
    val onboardingFamilies: List<OnboardingFamily> = emptyList(),
) {
    private val byId: Map<String, ExerciseVariation> by lazy { variations.associateBy { it.id } }
    private val policyById: Map<String, ProgressionPolicy> by lazy { policies.associateBy { it.id } }
    fun variation(id: String): ExerciseVariation? = byId[id]
    fun policyFor(v: ExerciseVariation): ProgressionPolicy? = v.progressionPolicyId?.let { policyById[it] }
    fun policyForVariation(variationId: String): ProgressionPolicy? = variation(variationId)?.let { policyFor(it) }
}

// ---------- routine & preferences ----------

@Serializable
data class RoutineSlot(
    val id: String,
    val intent: Pattern,
    val area: Area,
    val preferredVariationId: String,
    /** Optional slots may be dropped to fit a short session; mandatory ones may not. */
    val optional: Boolean = false,
    /** V22: an explicit group for the Pairs format (a pair, or a triplet like the RR core); slots without one pair up in order. */
    val group: Int? = null,
    /** V22: this slot's own rep range under a Rep range rule (the RR core triplet is 8-12 while the pairs are 5-8). */
    val repFrom: Int? = null,
    val repTo: Int? = null,
    /** V22: this slot's rest after a set in seconds (RR pairs 90, core 60, Minimalist 0 = none); null = the exercise's own recovery. */
    val restSeconds: Int? = null,
)

@Serializable
data class Routine(
    val id: String,
    val revision: Int,
    val name: String,
    val slots: List<RoutineSlot>,
    val defaultFocus: Set<StrengthFocus> = setOf(StrengthFocus.FULL_BODY),
    val goalId: String? = null,
    /** V21: the progression rule is a property of the routine (O2). Appended last; older routines use App levels. */
    val rule: ProgressionRule = ProgressionRule(),
    /** V22: this routine's own warm-up (mobility ids, in order); empty = the catalog's warm-up template. Used when the warm-up switch is on. */
    val warmup: List<String> = emptyList(),
)

/** Remembered preferences (spec §3, ADR 0002 §A). */
@Serializable
data class Preferences(
    val defaultDurationSeconds: Int = 2700,
    val stretchOn: Boolean = true,
    val stretchAreas: Set<StretchArea> = setOf(StretchArea.FULL_BODY),
    val warmupOn: Boolean = false,
    val cooldownOn: Boolean = false,
    val techniqueCues: Boolean = false,
    val audioEnabled: Boolean = true,
    val selectedProfileId: String = "home",
    val excludedVariationIds: Set<String> = emptySet(),
    val dismissedSuggestionIds: Set<String> = emptySet(),
    /** D4 (V14): the global "Automatic progression" switch. Off freezes levels and exercises; the owner changes them by hand. Appended last. */
    val autoProgression: Boolean = true,
)

// ---------- plan ----------

@Serializable
data class TimelineBlock(
    val id: String,
    val type: BlockType,
    val durationSeconds: Int,
    val roundIndex: Int? = null,
    val slotId: String? = null,
    val variationId: String? = null,
    val side: Side = Side.NONE,
    val target: Target? = null,
    val prescriptionTier: Int? = null,
    val recoveryForBlockIds: List<String> = emptyList(),
    val mediaId: String? = null,
    val earlyCompletionStretchId: String? = null,
    /** Optional extra stretch appended to use remaining time; first to be trimmed. */
    val optionalExtra: Boolean = false,
    /** Kettlebell weight for this work block (null = bodyweight / unknown, e.g. plans saved before 0.3.1). */
    val loadGrams: Int? = null,
    /** V17 (D3): the number was typed freely in the Detailed edit. A session with a free number does not count towards progression. */
    val freeTarget: Boolean = false,
    /** V19: the pair (Pairs) or the exercise number (Straight sets) this block belongs to; null in a Circuit. */
    val groupIndex: Int? = null,
) {
    init { require(durationSeconds > 0) { "block $id must have positive duration" } }
}

@Serializable
data class WorkoutPlan(
    val id: String,
    val routineId: String,
    val routineRevision: Int,
    val catalogVersion: Int,
    val createdAtEpochMs: Long,
    val profileId: String,
    val requestedDurationSeconds: Int,
    val plannedDurationSeconds: Int,
    val focus: Set<StrengthFocus>,
    val stretchOn: Boolean,
    val goalId: String? = null,
    val rounds: Int,
    val changesExplained: List<String>,
    val warnings: List<String>,
    /** True when the user must accept something before starting (underfill, re-entry, non-equivalent swap). */
    val needsAcceptance: Boolean,
    val usesDraftContent: Boolean,
    val blocks: List<TimelineBlock>,
    /** Timed-rounds mode: 60 s work / 60 s rest per exercise. */
    val timed: Boolean = false,
    /** V19: how the blocks are ordered. Appended last; plans saved before V19 are circuits. */
    val format: WorkoutFormat = WorkoutFormat.CIRCUIT,
    /** V21: the rule the plan was made under (a snapshot, like everything in a plan). */
    val rule: ProgressionRule = ProgressionRule(),
)
