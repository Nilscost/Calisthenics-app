// Kettlebell load (owner 2026-10-08): levels are earned per weight. Same bell -> harder exercise;
// heavier bell -> the same exercise starts again at tier 1 (stars at the lighter weight are kept).
package app.calisthenics.domain.load

import app.calisthenics.domain.model.EquipmentProfile
import app.calisthenics.domain.model.ExerciseVariation

/** The kettlebell. V26 adds the dumbbells ("weight", the weight of each) as a second loaded kind; no exercise needs them before V27. */
const val LOAD_EQUIPMENT_ID = "kettlebell"
const val DUMBBELL_EQUIPMENT_ID = "weight"
const val BARBELL_EQUIPMENT_ID = "barbell"
const val VEST_EQUIPMENT_ID = "weighted-vest"
val LOAD_EQUIPMENT_IDS = listOf(LOAD_EQUIPMENT_ID, DUMBBELL_EQUIPMENT_ID, BARBELL_EQUIPMENT_ID, VEST_EQUIPMENT_ID)

/** Which loaded equipment decides this exercise's weight (kettlebell first), or null for a bodyweight exercise. */
fun ExerciseVariation.loadEquipmentId(): String? =
    LOAD_EQUIPMENT_IDS.firstOrNull { id -> equipmentAlternatives.any { alt -> alt.needs.any { it.equipmentId == id } } }

/** True when the exercise is done with a kettlebell or dumbbells (its weight matters for progression). */
fun ExerciseVariation.isLoaded(): Boolean = loadEquipmentId() != null

/** The weight available in this profile for that equipment (heaviest if several), or null when there is none or no weight is set. */
fun EquipmentProfile.loadGrams(equipmentId: String = LOAD_EQUIPMENT_ID): Int? =
    items.filter { it.equipmentId == equipmentId }.mapNotNull { it.massGrams }.maxOrNull()

/** The weight of the equipment this exercise uses in this profile. */
fun EquipmentProfile.loadGramsFor(v: ExerciseVariation): Int? = v.loadEquipmentId()?.let { loadGrams(it) }

/** O5: the steps shown on a weight track. Kettlebell 8 / 12 / 16 / 20 / 24 kg; dumbbells (each) 2.5 / 5 / 7.5 / 10 / 15 / 20 kg. */
fun loadLadder(equipmentId: String): List<Int> = when (equipmentId) {
    DUMBBELL_EQUIPMENT_ID -> listOf(2_500, 5_000, 7_500, 10_000, 15_000, 20_000)
    BARBELL_EQUIPMENT_ID -> listOf(20_000, 30_000, 40_000, 50_000, 60_000, 80_000, 100_000)
    VEST_EQUIPMENT_ID -> listOf(2_500, 5_000, 7_500, 10_000, 15_000, 20_000)
    else -> listOf(8_000, 12_000, 16_000, 20_000, 24_000)
}

enum class WeightState { DONE, NOW, READY, LOCKED }

/**
 * One step of a weight track. DONE = a lighter weight the person trained at (its stars are kept), NOW = the weight the level is earned at,
 * READY = a weight the profile has but nothing is earned at yet (a heavier bell than the recorded one: the exercise starts again at level 1),
 * LOCKED = heavier than anything in the profile ("needs a bell of this weight").
 */
data class WeightStep(val grams: Int, val state: WeightState, val stars: Int)

/**
 * The track for a loaded exercise, from the lightest weight in use upward along the ladder ("from your current bell upward").
 * [recorded] is the weight the current level is earned at (null = not trained yet), [starsByLoad] the stars kept from other weights,
 * [currentStars] the stars at [recorded]. Same-weight progression is not touched: this only describes it.
 */
fun weightTrack(equipmentId: String, profileGrams: Int?, recorded: Int?, currentStars: Int, starsByLoad: Map<Int, Int>): List<WeightStep> {
    val now = recorded ?: profileGrams
    val inUse = listOfNotNull(now, profileGrams) + starsByLoad.keys
    if (inUse.isEmpty()) return emptyList()
    val start = inUse.min()
    val weights = (loadLadder(equipmentId).filter { it >= start } + inUse).distinct().sorted()
    return weights.map { g ->
        val state = when {
            g == now -> WeightState.NOW
            now != null && g < now -> WeightState.DONE
            profileGrams != null && g <= profileGrams -> WeightState.READY
            else -> WeightState.LOCKED
        }
        WeightStep(g, state, if (g == now) currentStars else starsByLoad[g] ?: 0)
    }
}

/** 12000 -> "12", 12500 -> "12.5" (locale-independent: plan texts are English for now). */
fun formatKg(grams: Int): String =
    if (grams % 1000 == 0) (grams / 1000).toString() else (grams / 100 / 10.0).toString()
