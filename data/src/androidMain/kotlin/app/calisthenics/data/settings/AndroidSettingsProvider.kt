package app.calisthenics.data.settings

import android.content.Context
import android.content.SharedPreferences
import com.russhwolf.settings.SharedPreferencesSettings

class AndroidSettingsProvider(private val context: Context) : SettingsProvider {
    override fun open(name: String): NamedSettings = AndroidNamedSettings(context.getSharedPreferences(name, Context.MODE_PRIVATE))
}
private class AndroidNamedSettings(private val prefs: SharedPreferences) : NamedSettings {
    private val primitives = SharedPreferencesSettings(prefs)
    override fun contains(key: String) = prefs.contains(key)
    override fun stringOrNull(key: String) = primitives.getStringOrNull(key)
    override fun intOrNull(key: String) = primitives.getIntOrNull(key)
    override fun longOrNull(key: String) = primitives.getLongOrNull(key)
    override fun booleanOrNull(key: String) = primitives.getBooleanOrNull(key)
    override fun stringSetOrNull(key: String) = prefs.getStringSet(key, null)?.toSet()
    override fun edit(commit: Boolean, changes: Editor.() -> Unit) {
        val native = prefs.edit()
        val editor = object : Editor {
            override fun putString(key: String, value: String?) { native.putString(key, value) }
            override fun putInt(key: String, value: Int) { native.putInt(key, value) }
            override fun putLong(key: String, value: Long) { native.putLong(key, value) }
            override fun putBoolean(key: String, value: Boolean) { native.putBoolean(key, value) }
            override fun putStringSet(key: String, value: Set<String>?) { native.putStringSet(key, value?.toSet()) }
            override fun remove(key: String) { native.remove(key) }
            override fun clear() { native.clear() }
        }
        editor.changes()
        if (commit) native.commit() else native.apply()
    }
}
