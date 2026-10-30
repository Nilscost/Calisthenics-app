// Self-assessed starting steps (ONB-01): variationId -> tier, app-private. Turned into SelfAssessment actions and replayed.
package app.calisthenics.data.settings


import app.calisthenics.domain.model.Catalog
import app.calisthenics.domain.progression.ProgressSnapshot
import app.calisthenics.domain.progression.ProgressionEngine
import app.calisthenics.domain.progression.UserAction
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

object LevelStore {
    private val ser = MapSerializer(String.serializer(), Int.serializer())
    fun load(ctx: SettingsProvider): Map<String, Int> = try {
        ctx.legacy("levels").getString("v", null)
            ?.let { Json.decodeFromString(ser, it) } ?: emptyMap()
    } catch (_: Exception) { emptyMap() }
    /** Day (epoch day) each level was last set; entries without one (older installs, seeds) count from day 0. */
    fun days(ctx: SettingsProvider): Map<String, Int> = try {
        ctx.legacy("levels").getString("days", null)?.let { Json.decodeFromString(ser, it) } ?: emptyMap()
    } catch (_: Exception) { emptyMap() }

    /**
     * W2: a level that is new or changed is dated [today] (it takes effect from that day: older sessions cannot undo it); unchanged ones keep their day.
     * [restart] dates every level (the questionnaire); [undated] stores plain day-0 levels (debug seed).
     */
    fun save(ctx: SettingsProvider, m: Map<String, Int>, today: Int, restart: Boolean = false, undated: Boolean = false) {
        val old = load(ctx); val oldDays = days(ctx)
        val d = if (undated) emptyMap() else m.mapNotNull { (k, v) -> if (restart || old[k] != v) k to today else oldDays[k]?.let { k to it } }.toMap()
        ctx.legacy("levels").edit().putString("v", Json.encodeToString(ser, m)).putString("days", Json.encodeToString(ser, d)).apply()
    }
    /** The levels as dated self-assessments for the replay. */
    fun actions(ctx: SettingsProvider): List<UserAction> { val d = days(ctx); return load(ctx).map { (v, t) -> app.calisthenics.domain.progression.selfAssessmentFor(v, t, d[v] ?: 0) } }
    /**
     * W2 (owner): redoing the starting questionnaire restarts the levels from the new answers. This is the moment (epoch ms) of the last save of the
     * questionnaire: the history before it stays visible in History, but no longer raises levels or counts toward streaks. 0 = never reset.
     */
    fun resetAtMs(ctx: SettingsProvider): Long = try { ctx.legacy("levels").getLong("resetAtMs", 0L) } catch (_: Exception) { 0L }
    fun markReset(ctx: SettingsProvider, atMs: Long) { ctx.legacy("levels").edit().putLong("resetAtMs", atMs).commit() }
    fun snapshot(catalog: Catalog, levels: Map<String, Int>): ProgressSnapshot =
        ProgressionEngine(catalog).replay(emptyList(), levels.map { (v, t) -> app.calisthenics.domain.progression.selfAssessmentFor(v, t) })
}
