// Kettlebell load (owner 2026-10-08): levels are earned per weight. Same bell -> harder exercise;
// heavier bell -> the same exercise starts again at tier 1 (stars at the lighter weight are kept).
package app.calisthenics.domain.load

import app.calisthenics.domain.model.EquipmentProfile
import app.calisthenics.domain.model.ExerciseVariation

/** The only loaded equipment so far. Dumbbells ("weight") are not used by any exercise yet. */
const val LOAD_EQUIPMENT_ID = "kettlebell"

/** True when the exercise is done with a kettlebell (its weight matters for progression). */
fun ExerciseVariation.isLoaded(): Boolean =
    equipmentAlternatives.any { alt -> alt.needs.any { it.equipmentId == LOAD_EQUIPMENT_ID } }

/** The kettlebell weight available in this profile (heaviest if several), or null when there is none or no weight is set. */
fun EquipmentProfile.loadGrams(): Int? =
    items.filter { it.equipmentId == LOAD_EQUIPMENT_ID }.mapNotNull { it.massGrams }.maxOrNull()

/** 12000 -> "12", 12500 -> "12.5" (locale-independent: plan texts are English for now). */
fun formatKg(grams: Int): String =
    if (grams % 1000 == 0) (grams / 1000).toString() else (grams / 100 / 10.0).toString()
