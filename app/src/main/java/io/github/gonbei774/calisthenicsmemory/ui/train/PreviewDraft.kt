// R8.1-T05 (F6): what the person changed in the Preview survives going back to Train and returning. The draft belongs to the objective
// (goal, saved routine or program). A changed exercise count also starts a fresh automatic selection.
package io.github.gonbei774.calisthenicsmemory.ui.train

import app.calisthenics.domain.planner.expireLevelOverrides
import android.content.Context
import app.calisthenics.domain.planner.PlanEdits
import app.calisthenics.domain.planner.ProgramContent
import app.calisthenics.domain.planner.TrainSettings
import app.calisthenics.domain.model.BetweenSetMode
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

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

object PreviewDraftStore {
    private val json = Json { ignoreUnknownKeys = true }
    private fun p(ctx: Context) = ctx.getSharedPreferences("preview_draft", Context.MODE_PRIVATE)

    /** The draft saved for [key], or null when there is none or it belongs to another objective. */
    fun get(ctx: Context, key: String): PreviewDraft? {
        val d = runCatching { p(ctx).getString("draft", null)?.let { json.decodeFromString(PreviewDraft.serializer(), it) } }.getOrNull()
        // A manual level or typed number is for the next workout only: older ones are dropped
        return d?.takeIf { it.key == key }?.let { it.copy(edits = it.edits.expireLevelOverrides(java.time.LocalDate.now().toEpochDay())) }
    }

    fun put(ctx: Context, d0: PreviewDraft) {
        // a draft written now is today's: levels it carries (typed by hand, or a saved routine's own) count from today
        val d = if (d0.edits.hasLevelOverrides() && d0.edits.overrideEpochDay == 0L) d0.copy(edits = d0.edits.copy(overrideEpochDay = java.time.LocalDate.now().toEpochDay())) else d0
        runCatching { p(ctx).edit().putString("draft", json.encodeToString(PreviewDraft.serializer(), d)).apply() }
    }

    fun clear(ctx: Context) {
        runCatching { p(ctx).edit().remove("draft").apply() }
    }
}
