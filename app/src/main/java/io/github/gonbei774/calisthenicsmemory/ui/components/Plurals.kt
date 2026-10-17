package io.github.gonbei774.calisthenicsmemory.ui.components

import androidx.annotation.PluralsRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

/** R8.1-T11 (F15): "1 round", "2 rounds": the plurals resources with the count as the only argument. */
@Composable
fun countText(@PluralsRes id: Int, n: Int): String = LocalContext.current.resources.getQuantityString(id, n, n)
