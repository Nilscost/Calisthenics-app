package io.github.gonbei774.calisthenicsmemory.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/**
 * Strings read during composition for use in plain (non-composable) code such as event handlers and `remember` blocks.
 * Compose Multiplatform resolves strings only in composition or suspending code; this carries the raw templates
 * and applies the same `%1$s` / `%1$d` / `%%` rules the resources use.
 */
class ResolvedStrings internal constructor(private val templates: Map<StringResource, String>) {
    fun format(res: StringResource, vararg args: Any?): String {
        val t = templates[res] ?: error("String ${res.key} was not requested from rememberResolvedStrings")
        return PLACEHOLDER.replace(t) { m -> if (m.value == "%%") "%" else args[m.groupValues[1].toInt() - 1].toString() }
    }
    private companion object { val PLACEHOLDER = Regex("%(\\d+)\\$[sd]|%%") }
}

@Composable
fun rememberResolvedStrings(resources: List<StringResource>): ResolvedStrings {
    val values = resources.map { stringResource(it) }
    return remember(values) { ResolvedStrings(resources.zip(values).toMap()) }
}
