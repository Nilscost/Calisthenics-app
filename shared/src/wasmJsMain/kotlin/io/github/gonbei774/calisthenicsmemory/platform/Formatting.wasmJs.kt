package io.github.gonbei774.calisthenicsmemory.platform

import androidx.compose.ui.Modifier
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

actual fun formatClockText(minutes: Int, seconds: Int): String = "$minutes:${seconds.toString().padStart(2, '0')}"

actual fun formatOneDecimal(value: Double): String {
    val tenths = kotlin.math.round(value * 10).toLong()
    return "${tenths / 10}.${kotlin.math.abs(tenths % 10)}"
}

actual fun Modifier.exposeTestTagsAsResourceId(): Modifier = this

actual val IoDispatcher: CoroutineDispatcher = Dispatchers.Default
