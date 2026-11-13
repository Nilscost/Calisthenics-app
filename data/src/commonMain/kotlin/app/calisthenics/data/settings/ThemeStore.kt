package app.calisthenics.data.settings

enum class AppTheme(val code: String) {
    SYSTEM("system"), LIGHT("light"), DARK("dark");
    companion object { fun fromCode(code: String): AppTheme = entries.find { it.code == code } ?: SYSTEM }
}
class ThemeStore(private val provider: SettingsProvider) {
    fun getTheme(): AppTheme = AppTheme.fromCode(provider.open("theme_preferences").stringOrNull("app_theme") ?: AppTheme.LIGHT.code)
    fun setTheme(theme: AppTheme) = provider.open("theme_preferences").edit { putString("app_theme", theme.code) }
}
