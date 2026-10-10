package io.github.gonbei774.calisthenicsmemory.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/** One key pose per family, drawn as a stick figure (original drawings, one colour, 24 x 24). Used by the skill tree and cards. */
object FamilyIcons {
    private fun PathBuilder.line(x1: Float, y1: Float, x2: Float, y2: Float) { moveTo(x1, y1); lineTo(x2, y2) }
    private fun PathBuilder.head(cx: Float, cy: Float, r: Float = 2f) {
        moveTo(cx - r, cy); arcTo(r, r, 0f, true, true, cx + r, cy); arcTo(r, r, 0f, true, true, cx - r, cy); close()
    }
    private fun icon(name: String, body: PathBuilder.() -> Unit): ImageVector =
        ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f).path(
            fill = null, stroke = SolidColor(Color.Black), strokeLineWidth = 1.9f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round, pathBuilder = body,
        ).build()

    val Push by lazy { icon("Push") { line(3f, 17f, 17f, 11.5f); line(15f, 12f, 15f, 19f); head(20f, 10f); line(1.5f, 20f, 22.5f, 20f) } }
    val Planche by lazy { icon("Planche") { line(4f, 11f, 18f, 11f); line(16f, 11f, 16f, 19f); head(21f, 10.5f); line(1.5f, 20f, 22.5f, 20f) } }
    val Handstand by lazy { icon("Handstand") { line(12f, 3f, 12f, 13f); line(12f, 13f, 8f, 20f); line(12f, 13f, 16f, 20f); head(12f, 16.5f, 1.6f) } }
    val Pull by lazy { icon("Pull") { line(3f, 3.5f, 21f, 3.5f); line(8f, 3.5f, 12f, 9f); line(16f, 3.5f, 12f, 9f); head(12f, 11.5f); line(12f, 13.5f, 12f, 21f) } }
    val Lever by lazy { icon("Lever") { line(4f, 3.5f, 20f, 3.5f); line(12f, 3.5f, 12f, 9f); line(3f, 10f, 19f, 10f); head(21.5f, 10f, 1.8f) } }
    val Row by lazy { icon("Row") { line(14f, 6f, 22f, 6f); line(17f, 6f, 15f, 12f); line(15f, 12f, 3f, 19f); head(17f, 14f, 1.8f) } }
    val Squat by lazy { icon("Squat") { head(12f, 4.5f); line(12f, 7f, 10f, 13f); line(10f, 13f, 15f, 14f); line(15f, 14f, 14f, 20f); line(11f, 9f, 18f, 9f) } }
    val Bridge by lazy { icon("Bridge") { line(3f, 19f, 9f, 12f); line(9f, 12f, 13f, 12f); line(13f, 12f, 17f, 14f); line(17f, 14f, 18f, 19f); head(2.5f, 15.5f, 1.8f); line(1.5f, 20f, 22.5f, 20f) } }
    val Superman by lazy { icon("Superman") { line(5f, 16f, 18f, 16f); line(18f, 16f, 22f, 12.5f); line(5f, 16f, 2f, 12.5f); head(19.5f, 13.5f, 1.6f); line(1.5f, 20f, 22.5f, 20f) } }
    val Hinge by lazy { icon("Hinge") { head(17.5f, 6.5f); line(15f, 8.5f, 8f, 13f); line(8f, 13f, 9f, 20f); line(14f, 10f, 13f, 15f); moveTo(10.5f, 17.5f); arcTo(2.2f, 2.2f, 0f, true, true, 14.9f, 17.5f); arcTo(2.2f, 2.2f, 0f, true, true, 10.5f, 17.5f) } }
    val Plank by lazy { icon("Plank") { line(4f, 16.5f, 19f, 13f); line(19f, 13f, 19f, 19f); head(21.5f, 11.5f, 1.8f); line(1.5f, 20f, 22.5f, 20f) } }
    val DeadBug by lazy { icon("DeadBug") { line(4f, 19f, 18f, 19f); line(8f, 19f, 8f, 11f); line(14f, 19f, 17f, 12f); head(20.5f, 18f, 1.8f) } }
    val SidePlank by lazy { icon("SidePlank") { line(4f, 19f, 17f, 10.5f); line(17f, 10.5f, 17f, 19f); head(19.5f, 8f, 1.8f); line(1.5f, 20f, 22.5f, 20f) } }
    val LegRaise by lazy { icon("LegRaise") { line(3f, 19f, 13f, 19f); line(13f, 19f, 20f, 9f); head(1.5f, 17f, 1.6f); line(1.5f, 20.5f, 22.5f, 20.5f) } }
    val Stretch by lazy { icon("Stretch") { head(12f, 4.5f); line(12f, 7f, 12f, 14f); line(12f, 9f, 6f, 5f); line(12f, 9f, 18f, 5f); line(12f, 14f, 8f, 20f); line(12f, 14f, 16f, 20f) } }

    fun forFamily(familyId: String): ImageVector = when (familyId) {
        "pushup" -> Push; "planche" -> Planche; "hspu" -> Handstand; "pullup" -> Pull; "lever" -> Lever; "row" -> Row
        "squat" -> Squat; "bridge" -> Bridge; "superman" -> Superman; "deadlift" -> Hinge; "plank" -> Plank
        "dip" -> Push; "lunge" -> Squat; "rdl", "nordic", "slide" -> Hinge; "reverse-hyper" -> Superman; "plank-tap", "pallof" -> Plank
        "dead-bug" -> DeadBug; "side-plank" -> SidePlank; "legraise" -> LegRaise
        else -> Stretch
    }
}
