package io.github.gonbei774.calisthenicsmemory

import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import io.github.gonbei774.calisthenicsmemory.data.AppTheme
import io.github.gonbei774.calisthenicsmemory.data.ThemePreferences
import io.github.gonbei774.calisthenicsmemory.ui.nav.AppNav
import io.github.gonbei774.calisthenicsmemory.ui.theme.CalisthenicsMemoryTheme

class MainActivity : ComponentActivity() {
    private val systemDarkMode = mutableStateOf(false)

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        systemDarkMode.value = isNight(newConfig)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        systemDarkMode.value = isNight(resources.configuration)
        val themePrefs = ThemePreferences(this)
        setContent {
            val isSystemDark by systemDarkMode
            val theme = remember { themePrefs.getTheme() }
            val dark = when (theme) {
                AppTheme.SYSTEM -> isSystemDark
                AppTheme.LIGHT -> false
                AppTheme.DARK -> true
            }
            CalisthenicsMemoryTheme(darkTheme = dark) { AppNav() }
        }
    }

    private fun isNight(c: Configuration) = (c.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
}
