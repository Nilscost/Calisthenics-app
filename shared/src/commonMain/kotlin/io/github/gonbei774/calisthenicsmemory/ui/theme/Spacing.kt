package io.github.gonbei774.calisthenicsmemory.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/** Spacing tokens (plan §6): 4 / 8 / 12 / 16 / 24 dp. */
object Spacing {
    val xs = 4.dp
    val s = 8.dp
    val m = 12.dp
    val l = 16.dp
    val xl = 24.dp
    /** Minimum touch target. */
    val touch = 48.dp
}

/** Corner radius 16 on cards, 28 on primary buttons (plan §6). */
object Radius {
    val card = 16.dp
    val button = 28.dp
}

val AppShapes = Shapes(
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(Radius.card),
    large = RoundedCornerShape(Radius.card),
    extraLarge = RoundedCornerShape(Radius.button),
)
