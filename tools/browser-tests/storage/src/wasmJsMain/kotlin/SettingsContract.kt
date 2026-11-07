package app.calisthenics.storageprobe

import app.calisthenics.data.settings.*
import app.calisthenics.domain.planner.PreviewDraft
import app.calisthenics.domain.planner.TrainSettings
import app.calisthenics.domain.planner.draftKey

private const val DAY = 21000L
private const val LARGE = 9_007_199_254_740_993L
private const val FILE = "browser_contract"

fun settingsPrepare() {
    val provider = WebSettingsProvider()
    val raw = provider.open(FILE)
    raw.edit {
        clear()
        putString("text", "hello \u2603")
        putInt("number", -42)
        putBoolean("enabled", true)
        putLong("large", LARGE)
        putStringSet("tags", setOf("one", "two"))
    }
    // Reset the production stores used by this isolated browser test origin.
    listOf("profiles", "planner_prefs", "goal", "mode", "train", "preview_draft", "routine").forEach { provider.open(it).edit { clear() } }
    val profile = ProfileStore.load(provider).first().copy(name = "Browser profile")
    ProfileStore.save(provider, listOf(profile))
    val train = TrainSettings(goalId = "strength", profileId = profile.id, rounds = 4, routineId = "saved-browser", warmupOn = true, preparationSeconds = 12)
    TrainSettingsStore.save(provider, train)
    PreviewDraftStore.put(provider, PreviewDraft(draftKey(train), rest = mapOf("block" to 17), modeRevision = LARGE, ruleDefaultVersion = 1), DAY)
}

fun settingsVerify() {
    val provider = WebSettingsProvider()
    val raw = provider.open(FILE)
    check(raw.stringOrNull("text") == "hello \u2603")
    check(raw.intOrNull("number") == -42)
    check(raw.booleanOrNull("enabled") == true)
    check(raw.longOrNull("large") == LARGE)
    check(raw.stringSetOrNull("tags") == setOf("one", "two"))
    val profile = ProfileStore.load(provider).single()
    check(profile.name == "Browser profile")
    val train = TrainSettingsStore.load(provider, DAY)
    check(train.goalId == "strength" && train.profileId == profile.id && train.rounds == 4)
    check(train.routineId == "saved-browser" && train.warmupOn && train.preparationSeconds == 12)
    val draft = PreviewDraftStore.get(provider, draftKey(train), DAY)
    check(draft != null && draft.rest == mapOf("block" to 17) && draft.modeRevision == LARGE && draft.ruleDefaultVersion == 1)
}

fun settingsAtomic() {
    val settings = WebSettingsProvider().open("browser_atomic")
    settings.edit { clear(); putString("old", "old") }
    settings.edit { putString("before-clear", "kept"); clear(); putInt("after-clear", 9) }
    check(!settings.contains("old"))
    check(settings.stringOrNull("before-clear") == "kept" && settings.intOrNull("after-clear") == 9)
    val failed = runCatching { settings.edit { clear(); putString("bad", "must not persist"); error("abort") } }
    check(failed.isFailure)
    check(settings.stringOrNull("before-clear") == "kept" && settings.intOrNull("after-clear") == 9 && !settings.contains("bad"))
}
