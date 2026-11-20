package app.calisthenics.data.settings

internal fun SettingsProvider.legacy(name: String) = LegacyAccess(open(name))
internal class LegacyAccess(private val settings: NamedSettings) {
    fun contains(k: String) = settings.contains(k)
    fun getString(k: String, default: String?) = settings.stringOrNull(k) ?: default
    fun getInt(k: String, default: Int) = settings.intOrNull(k) ?: default
    fun getLong(k: String, default: Long) = settings.longOrNull(k) ?: default
    fun getBoolean(k: String, default: Boolean) = settings.booleanOrNull(k) ?: default
    fun getStringSet(k: String, default: Set<String>?) = settings.stringSetOrNull(k) ?: default
    fun edit() = LegacyEdit(settings)
}
internal class LegacyEdit(private val settings: NamedSettings) {
    private val changes = mutableListOf<Editor.() -> Unit>()
    fun putString(k: String, v: String?) = apply { changes.add { putString(k, v) } }
    fun putInt(k: String, v: Int) = apply { changes.add { putInt(k, v) } }
    fun putLong(k: String, v: Long) = apply { changes.add { putLong(k, v) } }
    fun putBoolean(k: String, v: Boolean) = apply { changes.add { putBoolean(k, v) } }
    fun putStringSet(k: String, v: Set<String>?) = apply { val copy = v?.toSet(); changes.add { putStringSet(k, copy) } }
    fun remove(k: String) = apply { changes.add { remove(k) } }
    fun clear() = apply { changes.add { clear() } }
    fun apply() { settings.edit { changes.forEach { it() } } }
    fun commit(): Boolean { settings.edit(true) { changes.forEach { it() } }; return true }
}
