package io.github.gonbei774.calisthenicsmemory.platform

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

actual fun formatClockText(minutes: Int, seconds: Int): String = "%d:%02d".format(minutes, seconds)

actual fun formatOneDecimal(value: Double): String = "%.1f".format(value)

@OptIn(ExperimentalComposeUiApi::class)
actual fun Modifier.exposeTestTagsAsResourceId(): Modifier = this.semantics { testTagsAsResourceId = true }

actual val IoDispatcher: CoroutineDispatcher = Dispatchers.IO
