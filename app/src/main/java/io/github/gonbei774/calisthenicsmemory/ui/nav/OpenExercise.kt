// R7-T11 (D6): "open exercise X" as an app-level request. History (a workout's exercise rows, the Progressions list) asks for it;
// the Progress tab takes it when it shows and opens that exercise's detail page.
package io.github.gonbei774.calisthenicsmemory.ui.nav

object OpenExercise {
    @Volatile private var pending: String? = null
    fun request(variationId: String) { pending = variationId }
    /** The requested exercise, once. */
    fun take(): String? = pending.also { pending = null }
}
