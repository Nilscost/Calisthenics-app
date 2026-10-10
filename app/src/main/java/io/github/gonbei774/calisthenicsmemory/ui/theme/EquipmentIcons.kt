package io.github.gonbei774.calisthenicsmemory.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/** Simple one-colour pictograms for the equipment checklist (original drawings, 24 x 24). */
object EquipmentIcons {
    private fun PathBuilder.rect(x1: Float, y1: Float, x2: Float, y2: Float) {
        moveTo(x1, y1); lineTo(x2, y1); lineTo(x2, y2); lineTo(x1, y2); close()
    }
    private fun PathBuilder.circle(cx: Float, cy: Float, r: Float) {
        moveTo(cx - r, cy); arcTo(r, r, 0f, true, true, cx + r, cy); arcTo(r, r, 0f, true, true, cx - r, cy); close()
    }
    private fun icon(name: String, evenOdd: Boolean = false, body: PathBuilder.() -> Unit): ImageVector =
        ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f)
            .path(fill = SolidColor(Color.Black), pathFillType = if (evenOdd) PathFillType.EvenOdd else PathFillType.NonZero, pathBuilder = body).build()

    private fun barOn(name: String, barY: Float) = icon(name) {
        rect(2f, barY, 22f, barY + 2.5f); rect(3f, barY, 5f, 22f); rect(19f, barY, 21f, 22f)
    }

    val PullUpBar by lazy { barOn("PullUpBar", 5f) }
    val HighBar by lazy { icon("HighBar") { rect(2f, 2f, 22f, 4.5f); rect(3f, 2f, 5f, 22f); rect(19f, 2f, 21f, 22f); rect(8f, 15f, 16f, 17f) } }
    val LowBar by lazy { barOn("LowBar", 14f) }
    val Band by lazy { icon("Band", evenOdd = true) { circle(12f, 12f, 9f); circle(12f, 12f, 5.5f) } }
    val Kettlebell by lazy {
        icon("Kettlebell", evenOdd = true) {
            circle(12f, 15f, 6.5f)
            rect(7f, 3f, 17f, 11f); rect(9f, 5f, 15f, 11f)
        }
    }
    val Dumbbell by lazy { icon("Dumbbell") { rect(7f, 11f, 17f, 13f); rect(3f, 7f, 7f, 17f); rect(17f, 7f, 21f, 17f); rect(1f, 9f, 3f, 15f); rect(21f, 9f, 23f, 15f) } }
    val Chair by lazy { icon("Chair") { rect(6f, 3f, 8.5f, 12f); rect(6f, 11f, 18f, 13.5f); rect(6f, 13f, 8.5f, 21f); rect(15.5f, 13f, 18f, 21f) } }
    val Wall by lazy {
        icon("Wall") {
            for (row in 0..3) {
                val y = 3f + row * 4.5f
                if (row % 2 == 0) { rect(3f, y, 11f, y + 3.5f); rect(12f, y, 21f, y + 3.5f) }
                else { rect(3f, y, 7f, y + 3.5f); rect(8f, y, 16f, y + 3.5f); rect(17f, y, 21f, y + 3.5f) }
            }
        }
    }
    val Mat by lazy { icon("Mat") { rect(2f, 14f, 22f, 19f) } }
    val Parallettes by lazy { icon("Parallettes") { rect(3f, 9f, 10f, 11f); rect(4f, 11f, 6f, 19f); rect(8f, 11f, 10f, 19f); rect(14f, 9f, 21f, 11f); rect(15f, 11f, 17f, 19f); rect(19f, 11f, 21f, 19f) } }

    val DipSupport by lazy { icon("DipSupport") { rect(3f, 9f, 21f, 11f); rect(5f, 11f, 7f, 19f); rect(17f, 11f, 19f, 19f); rect(2f, 19f, 22f, 20.5f) } }
    val FootAnchor by lazy { icon("FootAnchor") { rect(3f, 8f, 21f, 15f); rect(5f, 15f, 8f, 19f); rect(16f, 15f, 19f, 19f); rect(2f, 19f, 22f, 20.5f) } }

    fun forId(id: String): ImageVector? = when (id) {
        "pullup-bar" -> PullUpBar; "high-bar" -> HighBar; "low-bar" -> LowBar; "resistance-band" -> Band
        "kettlebell" -> Kettlebell; "weight" -> Dumbbell; "chair" -> Chair; "wall" -> Wall; "mat" -> Mat; "parallettes" -> Parallettes
        "dip-support" -> DipSupport; "foot-anchor" -> FootAnchor
        else -> null
    }
}
