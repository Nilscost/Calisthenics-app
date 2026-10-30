package app.calisthenics.data.settings

interface SettingsProvider { fun open(name: String): NamedSettings }
interface NamedSettings {
    fun contains(key: String): Boolean
    fun stringOrNull(key: String): String?
    fun intOrNull(key: String): Int?
    fun longOrNull(key: String): Long?
    fun booleanOrNull(key: String): Boolean?
    fun stringSetOrNull(key: String): Set<String>?
    fun edit(commit: Boolean = false, changes: Editor.() -> Unit)
}
interface Editor {
    fun putString(key: String, value: String?)
    fun putInt(key: String, value: Int)
    fun putLong(key: String, value: Long)
    fun putBoolean(key: String, value: Boolean)
    fun putStringSet(key: String, value: Set<String>?)
    fun remove(key: String)
    fun clear()
}
