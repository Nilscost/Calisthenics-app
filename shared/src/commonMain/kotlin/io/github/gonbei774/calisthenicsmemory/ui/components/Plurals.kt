package io.github.gonbei774.calisthenicsmemory.ui.components

import androidx.compose.runtime.Composable
import org.jetbrains.compose.resources.PluralStringResource
import org.jetbrains.compose.resources.pluralStringResource

/** R8.1-T11 (F15): "1 round", "2 rounds": the plurals resources with the count as the only argument. */
@Composable
fun countText(id: PluralStringResource, n: Int): String = pluralStringResource(id, n, n)
