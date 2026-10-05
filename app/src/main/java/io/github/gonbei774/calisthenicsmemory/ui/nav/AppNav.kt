// U01: navigation shell. One activity, four bottom tabs (Train, Progress, History, Settings) and a few full-screen routes.
// androidx.navigation is not available offline, so routes are a small sealed class kept in rememberSaveable.
package io.github.gonbei774.calisthenicsmemory.ui.nav

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import io.github.gonbei774.calisthenicsmemory.R
import io.github.gonbei774.calisthenicsmemory.ui.screens.BackupScreen2
import io.github.gonbei774.calisthenicsmemory.ui.screens.HistoryScreen
import io.github.gonbei774.calisthenicsmemory.ui.screens.LibraryScreen
import io.github.gonbei774.calisthenicsmemory.ui.screens.LicensesScreen
import io.github.gonbei774.calisthenicsmemory.ui.onboarding.OnboardingScreen
import io.github.gonbei774.calisthenicsmemory.ui.screens.OnboardingStore
import io.github.gonbei774.calisthenicsmemory.ui.screens.ProfileEditScreen
import io.github.gonbei774.calisthenicsmemory.ui.screens.ProfilesScreen
import io.github.gonbei774.calisthenicsmemory.ui.session.SessionScreen
import io.github.gonbei774.calisthenicsmemory.ui.screens.SettingsScreen
import io.github.gonbei774.calisthenicsmemory.ui.train.PreviewScreen
import io.github.gonbei774.calisthenicsmemory.ui.train.TrainScreen

enum class Tab(val route: String, val labelRes: Int, val icon: ImageVector) {
    TRAIN("train", R.string.tab_train, Icons.Filled.PlayArrow),
    PROGRESS("progress", R.string.tab_progress, Icons.Filled.Star),
    HISTORY("history", R.string.tab_history, Icons.Filled.DateRange),
    SETTINGS("settings", R.string.tab_settings, Icons.Filled.Settings),
}

/** Full-screen routes. Tabs are routes too; [route] is the saved form. */
enum class Route(val route: String) {
    TRAIN("train"), PROGRESS("progress"), HISTORY("history"), SETTINGS("settings"),
    SESSION("session"), ONBOARDING("onboarding"), FIRST_RUN("first_run"), BACKUP("backup"), LICENSES("licenses"),
    PROFILES("profiles"), PROFILE_EDIT("profile_edit"), PREVIEW("preview");

    val tab: Tab? get() = Tab.entries.firstOrNull { it.route == route }

    companion object {
        fun of(route: String?) = entries.firstOrNull { it.route == route }
        fun of(tab: Tab) = entries.first { it.route == tab.route }
    }
}

@Composable
fun AppNav() {
    val ctx = LocalContext.current
    var route by rememberSaveable {
        mutableStateOf((if (OnboardingStore.done(ctx)) Route.TRAIN else Route.FIRST_RUN).route)
    }
    // Where the profile editor returns to, and which profile it edits (null = new).
    var editId by rememberSaveable { mutableStateOf<String?>(null) }
    var editFrom by rememberSaveable { mutableStateOf(Route.TRAIN.route) }
    val current = Route.of(route) ?: Route.TRAIN
    fun go(r: Route) { route = r.route }
    val tab = current.tab

    Scaffold(
        bottomBar = {
            if (tab != null) {
                NavigationBar(Modifier.testTag("bottom_bar")) {
                    Tab.entries.forEach { t ->
                        NavigationBarItem(
                            selected = t == tab,
                            onClick = { go(Route.of(t)) },
                            icon = { Icon(t.icon, contentDescription = null) },
                            label = { Text(stringResource(t.labelRes)) },
                            modifier = Modifier.testTag("tab_${t.route}"),
                        )
                    }
                }
            }
        },
    ) { pad ->
        val m = Modifier.padding(pad)
        when (current) {
            Route.TRAIN -> TrainScreen(m, onPreview = { go(Route.PREVIEW) }, onStarted = { go(Route.SESSION) },
                onEditProfile = { id -> editId = id; editFrom = Route.TRAIN.route; go(Route.PROFILE_EDIT) })
            Route.PREVIEW -> { BackHandler { go(Route.TRAIN) }; PreviewScreen(m, onBack = { go(Route.TRAIN) }, onStarted = { go(Route.SESSION) }) }
            Route.PROGRESS -> LibraryScreen(m)
            Route.HISTORY -> HistoryScreen(m)
            Route.SETTINGS -> SettingsScreen(m, onLevels = { go(Route.ONBOARDING) }, onBackup = { go(Route.BACKUP) }, onLicenses = { go(Route.LICENSES) }, onProfiles = { go(Route.PROFILES) })
            Route.SESSION -> { BackHandler { go(Route.TRAIN) }; SessionScreen(m, onExit = { go(Route.TRAIN) }) }
            Route.FIRST_RUN -> OnboardingScreen(m, onBack = null, onDone = { OnboardingStore.setDone(ctx); go(Route.TRAIN) })
            Route.ONBOARDING -> { BackHandler { go(Route.SETTINGS) }; OnboardingScreen(m, onBack = { go(Route.SETTINGS) }, onDone = { OnboardingStore.setDone(ctx); go(Route.SETTINGS) }) }
            Route.BACKUP -> { BackHandler { go(Route.SETTINGS) }; BackupScreen2(m, onBack = { go(Route.SETTINGS) }) }
            Route.PROFILES -> { BackHandler { go(Route.SETTINGS) }
                ProfilesScreen(m, onBack = { go(Route.SETTINGS) }, onEdit = { id -> editId = id; editFrom = Route.PROFILES.route; go(Route.PROFILE_EDIT) }) }
            Route.PROFILE_EDIT -> { val back = Route.of(editFrom) ?: Route.TRAIN; BackHandler { go(back) }
                ProfileEditScreen(m, profileId = editId, onDone = { go(back) }) }
            Route.LICENSES -> { BackHandler { go(Route.SETTINGS) }; LicensesScreen(onNavigateBack = { go(Route.SETTINGS) }) }
        }
    }
}
