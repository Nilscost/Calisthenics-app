package app.calisthenics.data.settings

import com.russhwolf.settings.StorageSettings
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

@Serializable
private data class Value(val type: String, val value: String = "", val set: Set<String> = emptySet())

/** One namespaced blob per settings file. Concurrent browser tabs are not synchronized. */
class WebSettingsProvider : SettingsProvider {
    private val storage = StorageSettings()
    override fun open(name: String): NamedSettings = WebNamedSettings(storage, "calisthenics.settings.$name")
}
private class WebNamedSettings(private val storage: StorageSettings, private val name: String) : NamedSettings {
    private fun read(): Map<String, Value> = storage.getStringOrNull(name)?.let { Json.decodeFromString<Map<String, Value>>(it) } ?: emptyMap()
    private fun value(key: String, type: String): Value? = read()[key]?.also { require(it.type == type) { "Wrong settings type for $key" } }
    override fun contains(key: String) = key in read()
    override fun stringOrNull(key: String) = value(key, "string")?.value
    override fun intOrNull(key: String) = value(key, "int")?.value?.toInt()
    override fun longOrNull(key: String) = value(key, "long")?.value?.toLong()
    override fun booleanOrNull(key: String) = value(key, "boolean")?.value?.toBooleanStrict()
    override fun stringSetOrNull(key: String) = value(key, "set")?.set?.toSet()
    override fun edit(commit: Boolean, changes: Editor.() -> Unit) {
        val pending = mutableMapOf<String, Value?>()
        var clear = false
        val e = object : Editor {
            override fun putString(key: String, value: String?) { pending[key] = value?.let { Value("string", it) } }
            override fun putInt(key: String, value: Int) { pending[key] = Value("int", value.toString()) }
            override fun putLong(key: String, value: Long) { pending[key] = Value("long", value.toString()) }
            override fun putBoolean(key: String, value: Boolean) { pending[key] = Value("boolean", value.toString()) }
            override fun putStringSet(key: String, value: Set<String>?) { pending[key] = value?.let { Value("set", set = it.toSet()) } }
            override fun remove(key: String) { pending[key] = null }
            override fun clear() { clear = true }
        }
        e.changes()
        val result = if (clear) mutableMapOf() else read().toMutableMap()
        pending.forEach { (key, value) -> if (value == null) result.remove(key) else result[key] = value }
        storage.putString(name, Json.encodeToString(result))
    }
}
