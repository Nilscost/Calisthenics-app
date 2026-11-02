// Web licence list (Phase 4b): THIRD_PARTY_NOTICES.md is the single source; the Gradle `copyWebAssets` task copies it into
// the build as data/licenses.md and the web entry point loads it before the first frame.
package io.github.gonbei774.calisthenicsmemory.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import io.github.gonbei774.calisthenicsmemory.platform.PlatformContext
import io.github.gonbei774.calisthenicsmemory.shared.resources.*
import io.github.gonbei774.calisthenicsmemory.ui.components.AppTextButton
import io.github.gonbei774.calisthenicsmemory.ui.theme.Spacing
import org.jetbrains.compose.resources.stringResource

private enum class Kind { H1, H2, H3, ROW, TEXT }
private class Line(val kind: Kind, val text: String)

private fun clean(s: String) = s.replace("**", "").replace("`", "").trim()

private fun parseNotices(md: String): List<Line> = buildList {
    for (raw in md.lines()) {
        val l = raw.trim()
        when {
            l.isEmpty() -> Unit
            l.startsWith("### ") -> add(Line(Kind.H3, clean(l.removePrefix("### "))))
            l.startsWith("## ") -> add(Line(Kind.H2, clean(l.removePrefix("## "))))
            l.startsWith("# ") -> add(Line(Kind.H1, clean(l.removePrefix("# "))))
            l.startsWith("|") -> {
                val cells = l.trim('|').split('|').map { clean(it) }
                if (cells.all { c -> c.all { it == '-' || it == ':' } } || cells.firstOrNull() == "Group") continue
                add(Line(Kind.ROW, cells.joinToString(" · ")))
            }
            else -> add(Line(Kind.TEXT, clean(l.removePrefix("- "))))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
actual fun LicensesScreen(onNavigateBack: () -> Unit) {
    val lines = remember { parseNotices(PlatformContext.licensesMarkdown.orEmpty()) }
    Scaffold(topBar = {
        TopAppBar(
            title = { Text(stringResource(Res.string.open_source_licenses)) },
            navigationIcon = { AppTextButton(onClick = onNavigateBack) { Text(stringResource(Res.string.back)) } },
        )
    }) { pad ->
        Column(
            Modifier.padding(pad).verticalScroll(rememberScrollState()).padding(Spacing.l).testTag("licenses_list"),
            verticalArrangement = Arrangement.spacedBy(Spacing.s),
        ) {
            lines.forEach { l ->
                when (l.kind) {
                    Kind.H1, Kind.H2 -> Text(l.text, style = MaterialTheme.typography.titleLarge)
                    Kind.H3 -> Text(l.text, style = MaterialTheme.typography.titleMedium)
                    Kind.ROW, Kind.TEXT -> Text(l.text, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}
