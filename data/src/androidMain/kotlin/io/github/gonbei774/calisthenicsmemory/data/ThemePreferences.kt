package io.github.gonbei774.calisthenicsmemory.data

import android.content.Context
import app.calisthenics.data.settings.AndroidSettingsProvider
import app.calisthenics.data.settings.ThemeStore

typealias AppTheme = app.calisthenics.data.settings.AppTheme
class ThemePreferences(context: Context) {
    private val store = ThemeStore(AndroidSettingsProvider(context))
    fun getTheme(): AppTheme = store.getTheme()
    fun setTheme(theme: AppTheme) = store.setTheme(theme)
}
