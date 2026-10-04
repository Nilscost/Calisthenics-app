package app.calisthenics.domain

import app.calisthenics.domain.model.*

/** Small in-test catalog (not the real starter content). */
object Fx {
    fun policy(vid: String, next: List<String> = emptyList(), rule: PrerequisiteRule = PrerequisiteRule(), type: TargetType = TargetType.REPS) =
        ProgressionPolicy(
            id = "pol-$vid", variationId = vid, version = 1,
            tiers = (1..5).map { Tier(it, Target(type, 5 * it), workWindowSeconds = 40, minRecoverySeconds = 20) },
            nextVariationIds = next, prerequisiteRule = rule,
        )

    fun strength(id: String, family: String = id, pattern: Pattern = Pattern.PUSH_HORIZONTAL, area: Area = Area.UPPER_BODY,
                 alts: List<RequirementSet> = listOf(RequirementSet()), stretches: List<String> = listOf("calf-stretch")) =
        ExerciseVariation(id = id, familyId = family, name = id, patterns = setOf(pattern), areas = setOf(area), kind = Kind.REPS,
            equipmentAlternatives = alts, progressionPolicyId = "pol-$id", compatibleStretchIds = stretches,
            instructions = listOf("do it"))

    val stretch = ExerciseVariation(id = "calf-stretch", familyId = "calf-stretch", name = "Calf stretch",
        patterns = setOf(Pattern.STRETCH), stretchAreas = setOf(StretchArea.CALF), kind = Kind.STRETCH,
        defaultSeconds = 30, unilateral = true, instructions = listOf("lean"))

    fun catalog(): Catalog {
        val vs = listOf(
            strength("pushup-knee", "pushup"),
            strength("pushup-std", "pushup"),
            strength("squat-air", "squat", Pattern.SQUAT, Area.LOWER_BODY),
            stretch,
        )
        val ps = listOf(
            policy("pushup-knee", next = listOf("pushup-std")),
            policy("pushup-std"),
            policy("squat-air"),
        )
        return Catalog(1, vs, ps)
    }

}
