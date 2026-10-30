package app.calisthenics.data.settings

import app.calisthenics.domain.model.ProgressionRule
import app.calisthenics.domain.model.RuleKind
import app.calisthenics.domain.planner.PlanEdits
import app.calisthenics.domain.planner.PreviewDraft
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

private class MemoryProvider : SettingsProvider {
    private val files = mutableMapOf<String, MemoryFile>()
    override fun open(name: String): NamedSettings = files.getOrPut(name) { MemoryFile() }
}
private class MemoryFile : NamedSettings {
    private val data = mutableMapOf<String, Any>()
    override fun contains(key: String) = key in data
    override fun stringOrNull(key: String) = data[key] as String?
    override fun intOrNull(key: String) = data[key] as Int?
    override fun longOrNull(key: String) = data[key] as Long?
    override fun booleanOrNull(key: String) = data[key] as Boolean?
    @Suppress("UNCHECKED_CAST") override fun stringSetOrNull(key: String) = (data[key] as Set<String>?)?.toSet()
    override fun edit(commit: Boolean, changes: Editor.() -> Unit) {
        val next = data.toMutableMap()
        val editor = object : Editor {
            override fun putString(key: String, value: String?) { if (value == null) next.remove(key) else next[key] = value }
            override fun putInt(key: String, value: Int) { next[key] = value }
            override fun putLong(key: String, value: Long) { next[key] = value }
            override fun putBoolean(key: String, value: Boolean) { next[key] = value }
            override fun putStringSet(key: String, value: Set<String>?) { if (value == null) next.remove(key) else next[key] = value.toSet() }
            override fun remove(key: String) { next.remove(key) }
            override fun clear() { next.clear() }
        }
        editor.changes(); data.clear(); data.putAll(next)
    }
}

class SettingsCompatibilityTest {
    @Test fun legacyHighBarIsConsumedOnlyOnce() {
        val p = MemoryProvider(); p.open("mode").edit { putBoolean("highBar", true); putBoolean("timed", true) }
        assertTrue(ModeStore.takeLegacyHighBar(p)); assertEquals(false, ModeStore.takeLegacyHighBar(p))
        assertEquals(true, ModeStore.timed(p)); assertEquals(false, p.open("mode").contains("highBar"))
    }

    @Test fun suggestionNativeSetAndDelimitedActionsSurviveWrites() {
        val p = MemoryProvider(); p.open("suggestions").edit { putStringSet("dismissed", setOf("a", "b")); putString("accepted", "pushup-standard|3|19000") }
        SuggestionStore.dismiss(p, "c"); SuggestionStore.acceptNext(p, "pushup-standard", "pushup-diamond", 19001)
        assertEquals(setOf("a", "b", "c"), p.open("suggestions").stringSetOrNull("dismissed"))
        assertEquals(2, SuggestionStore.accepted(p).size)
    }

    @Test fun draftKeyAndExplicitRuleSurviveAndUnrelatedKeyDoesNotLoad() {
        val p = MemoryProvider(); val draft = PreviewDraft("general|-|-", edits = PlanEdits(rule = ProgressionRule(kind = RuleKind.REP_RANGE)), rest = mapOf("slot" to 73), ruleDefaultVersion = 1)
        PreviewDraftStore.put(p, draft, 20000)
        assertEquals(draft, PreviewDraftStore.get(p, draft.key, 20000)); assertNull(PreviewDraftStore.get(p, "other", 20000))
        TrainSettingsStore.load(p, 20000)
        assertEquals(draft, PreviewDraftStore.get(p, draft.key, 20000))
    }

    @Test fun trainLegacyStringSetIsClearedAndVersionMarkersAreIdempotent() {
        val p = MemoryProvider(); p.open("train").edit { putStringSet("additional_pools", setOf("power")) }
        val first = TrainSettingsStore.load(p, 20000); val second = TrainSettingsStore.load(p, 20000)
        assertEquals(first, second); assertEquals(emptySet(), p.open("train").stringSetOrNull("additional_pools"))
        assertEquals(1, p.open("train").intOrNull("ordinary_rule_default_version"))
        assertEquals(1, p.open("train").intOrNull("ordinary_draft_rule_default_version"))
    }

    @Test fun onlyLegacyOrdinaryDraftRuleIsMigratedOnce() {
        val p = MemoryProvider()
        val draft = PreviewDraft("general|-|-", edits = PlanEdits(rule = ProgressionRule(kind = RuleKind.REP_RANGE)), rest = mapOf("slot" to 73), ruleDefaultVersion = 0)
        PreviewDraftStore.put(p, draft, 20000)
        TrainSettingsStore.load(p, 20000)
        val migrated = PreviewDraftStore.get(p, draft.key, 20000)!!
        assertEquals(RuleKind.APP_LEVELS, migrated.edits.rule?.kind)
        assertEquals(draft.rest, migrated.rest); assertEquals(1, migrated.ruleDefaultVersion)
        TrainSettingsStore.load(p, 20000)
        assertEquals(migrated, PreviewDraftStore.get(p, draft.key, 20000))
    }
}
