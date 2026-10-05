package io.github.gonbei774.calisthenicsmemory.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.mikepenz.aboutlibraries.ui.compose.m3.LibrariesContainer
import com.mikepenz.aboutlibraries.ui.compose.m3.LibraryDefaults
import io.github.gonbei774.calisthenicsmemory.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LicensesScreen(
    onNavigateBack: () -> Unit
) {
    Scaffold(
        topBar = {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Text(
                        text = stringResource(R.string.open_source_licenses),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    ) { paddingValues ->
        LibrariesContainer(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            colors = LibraryDefaults.libraryColors(
                backgroundColor = MaterialTheme.colorScheme.background,
                contentColor = MaterialTheme.colorScheme.onBackground,
                badgeBackgroundColor = MaterialTheme.colorScheme.primaryContainer,
                badgeContentColor = MaterialTheme.colorScheme.onPrimaryContainer
            )
        )
    }
}