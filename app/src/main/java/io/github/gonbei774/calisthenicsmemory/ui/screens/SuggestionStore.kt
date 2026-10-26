// V27 (R29): which suggestions the person closed, and the "too easy" raises they accepted (the engine replays those as AcceptHarder).
package io.github.gonbei774.calisthenicsmemory.ui.screens

import android.content.Context
import app.calisthenics.domain.progression.UserAction

object SuggestionStore {
    private fun p(ctx: Context) = ctx.getSharedPreferences("suggestions", Context.MODE_PRIVATE)

    fun dismissed(ctx: Context): Set<String> = p(ctx).getStringSet("dismissed", emptySet()).orEmpty().toSet()
    fun dismiss(ctx: Context, id: String) { p(ctx).edit().putStringSet("dismissed", dismissed(ctx) + id).commit() }

    /** "variationId|tier|day" per accepted raise, oldest first. */
    private fun rawAccepted(ctx: Context): List<String> = p(ctx).getString("accepted", "").orEmpty().split(';').filter { it.isNotBlank() }
    fun accept(ctx: Context, variationId: String, tier: Int, day: Int) { p(ctx).edit().putString("accepted", (rawAccepted(ctx) + "$variationId|$tier|$day").joinToString(";")).commit() }
    private fun rawNext(ctx: Context): List<String> = p(ctx).getString("acceptedNext", "").orEmpty().split(';').filter { it.isNotBlank() }
    /** R7-T05: "from|to|day" per accepted next exercise. */
    fun acceptNext(ctx: Context, variationId: String, toVariationId: String, day: Int) { p(ctx).edit().putString("acceptedNext", (rawNext(ctx) + "$variationId|$toVariationId|$day").joinToString(";")).commit() }
    /** W2: the questionnaire was redone: raises accepted and suggestions closed before it belong to the old levels. */
    fun clearAll(ctx: Context) { p(ctx).edit().clear().commit() }
    fun accepted(ctx: Context): List<UserAction> = rawAccepted(ctx).mapNotNull { r ->
        val f = r.split('|'); if (f.size != 3) null else runCatching { UserAction.AcceptHarder(f[2].toInt(), f[0], f[1].toInt()) }.getOrNull()
    } + rawNext(ctx).mapNotNull { r ->
        val f = r.split('|'); if (f.size != 3) null else runCatching { UserAction.AcceptNextExercise(f[2].toInt(), f[0], f[1]) }.getOrNull()
    }
}
