// U01 placeholder: links to what exists today. U12 turns this into the full Settings tab.
package io.github.gonbei774.calisthenicsmemory.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.gonbei774.calisthenicsmemory.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(modifier: Modifier = Modifier, onLevels: () -> Unit, onBackup: () -> Unit, onLicenses: () -> Unit) {
    Scaffold(modifier = modifier, topBar = { TopAppBar(title = { Text(stringResource(R.string.tab_settings)) }) }) { pad ->
        Column(Modifier.padding(pad).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onLevels, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.settings_levels)) }
            OutlinedButton(onClick = onBackup, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.settings_backup)) }
            OutlinedButton(onClick = onLicenses, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.settings_licenses)) }
        }
    }
}
