// R8.1-T05 (F6): what the person changed in the Preview survives going back to Train and returning. The draft belongs to the objective
// (goal, saved routine or program): it is dropped only when that changes, or when the person resets the Preview.
package io.github.gonbei774.calisthenicsmemory.ui.train

import android.content.Context
import app.calisthenics.domain.planner.PlanEdits
import app.calisthenics.domain.planner.TrainSettings
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class PreviewDraft(
    val key: String,
    /** Level, swap, add, remove, stretch picks, typed numbers, rule, cool-down changes. */
    val edits: PlanEdits = PlanEdits(),
    /** Rest per slot chosen with the rest steppers. */
    val rest: Map<String, Int> = emptyMap(),
)

/** The objective a draft belongs to: a goal or body focus, a saved routine, or a program. */
fun draftKey(s: TrainSettings): String = listOf(s.goalId, s.routineId ?: "-", s.fixedProgramId ?: "-").joinToString("|")

object PreviewDraftStore {
    private val json = Json { ignoreUnknownKeys = true }
    private fun p(ctx: Context) = ctx.getSharedPreferences("preview_draft", Context.MODE_PRIVATE)

    /** The draft saved for [key], or null when there is none or it belongs to another objective. */
    fun get(ctx: Context, key: String): PreviewDraft? {
        val d = runCatching { p(ctx).getString("draft", null)?.let { json.decodeFromString(PreviewDraft.serializer(), it) } }.getOrNull()
        return d?.takeIf { it.key == key }
    }

    fun put(ctx: Context, d: PreviewDraft) {
        runCatching { p(ctx).edit().putString("draft", json.encodeToString(PreviewDraft.serializer(), d)).apply() }
    }

    fun clear(ctx: Context) {
        runCatching { p(ctx).edit().remove("draft").apply() }
    }
}
