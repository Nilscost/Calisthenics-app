package io.github.gonbei774.calisthenicsmemory

import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
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

    @OptIn(ExperimentalComposeUiApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        systemDarkMode.value = isNight(resources.configuration)
        val themePrefs = ThemePreferences(this)
        setContent {
            val isSystemDark by systemDarkMode
            var theme by remember { mutableStateOf(themePrefs.getTheme()) }
            val dark = when (theme) {
                AppTheme.SYSTEM -> isSystemDark
                AppTheme.LIGHT -> false
                AppTheme.DARK -> true
            }
            // testTagsAsResourceId: the UI screenshot flows (maestro/) find controls by their test tags.
            CalisthenicsMemoryTheme(darkTheme = dark) {
                Box(Modifier.fillMaxSize().semantics { testTagsAsResourceId = true }) { AppNav(theme) { theme = it; themePrefs.setTheme(it) } }
            }
        }
    }

    private fun isNight(c: Configuration) = (c.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
}
