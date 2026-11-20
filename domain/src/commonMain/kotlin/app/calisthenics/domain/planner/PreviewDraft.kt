package app.calisthenics.domain.planner

import app.calisthenics.domain.model.BetweenSetMode
import kotlinx.serialization.Serializable

@Serializable
data class PreviewDraft(
    val key: String,
    /** Level, swap, add, remove, stretch picks, typed numbers, rule, cool-down changes. */
    val edits: PlanEdits = PlanEdits(),
    /** Rest per slot chosen with the rest steppers. */
    val rest: Map<String, Int> = emptyMap(),
    /** R8.1-T08 (F9): the items of a program being edited in the Preview (the catalog program itself never changes). */
    val program: ProgramContent? = null,
    val betweenSetMode: BetweenSetMode? = null,
    val preparationSeconds: Int = 5,
    /** Revisions explicit Flow/Rest/Stretch edits across Train and Preview. */
    val modeRevision: Long = 0L,
    /** 0 means a legacy draft whose rule provenance is unknown; current drafts are stamped 1. */
    val ruleDefaultVersion: Int = 0,
)

/** The objective a draft belongs to: a goal or body focus, a saved routine, or a program. */
fun draftKey(s: TrainSettings): String = listOf(s.goalId, s.routineId ?: "-", s.fixedProgramId ?: "-").joinToString("|")
