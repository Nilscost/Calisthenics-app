package io.github.gonbei774.calisthenicsmemory.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.gonbei774.calisthenicsmemory.shared.resources.*
import org.jetbrains.compose.resources.stringResource

// Compile-only for Phase 3b: a heading and back button; the real licence list is a Phase 4 item.
@Composable
actual fun LicensesScreen(onNavigateBack: () -> Unit) {
    Column(Modifier.padding(16.dp)) {
        TextButton(onClick = onNavigateBack) { Text(stringResource(Res.string.back)) }
        Text(stringResource(Res.string.open_source_licenses))
    }
}
