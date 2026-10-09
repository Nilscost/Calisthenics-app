// V08 (R5): a front and a back silhouette with the worked muscles coloured: primary in the accent colour, secondary light.
// Drawn from vector shapes (no bitmaps), driven by the same Muscle enum as the clips. Muscle names are in the screen-reader
// description and appear as chips when the figure is tapped.
package io.github.gonbei774.calisthenicsmemory.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import io.github.gonbei774.calisthenicsmemory.ui.theme.AppAccentTheme
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import app.calisthenics.domain.model.Muscle
import io.github.gonbei774.calisthenicsmemory.R

enum class BodyView { FRONT, BACK }
enum class ShapeKind { OVAL, ROUND_RECT }

/** One painted area in a 100 x 210 box. [l], [t], [r], [b] are box coordinates. */
data class Region(val view: BodyView, val kind: ShapeKind, val l: Float, val t: Float, val r: Float, val b: Float)

/** Where each muscle sits on the figure. Every [Muscle] has at least one region (tested). Left and right are separate regions. */
object BodyRegions {
    const val W = 100f
    const val H = 210f
    private fun ov(v: BodyView, l: Float, t: Float, r: Float, b: Float) = Region(v, ShapeKind.OVAL, l, t, r, b)
    private fun rr(v: BodyView, l: Float, t: Float, r: Float, b: Float) = Region(v, ShapeKind.ROUND_RECT, l, t, r, b)
    private fun pair(f: (Float, Float) -> Region, l: Float, r: Float): List<Region> { val a = f(l, r); val mirror = f(W - r, W - l); return listOf(a, mirror) }
    private val F = BodyView.FRONT
    private val B = BodyView.BACK

    val byMuscle: Map<Muscle, List<Region>> = mapOf(
        Muscle.CHEST to pair({ l, r -> ov(F, l, 34f, r, 53f) }, 31f, 49f),
        Muscle.FRONT_DELTS to pair({ l, r -> ov(F, l, 29f, r, 45f) }, 17f, 29f),
        Muscle.SIDE_DELTS to pair({ l, r -> ov(F, l, 30f, r, 46f) }, 9f, 18f) + pair({ l, r -> ov(B, l, 30f, r, 46f) }, 9f, 18f),
        Muscle.REAR_DELTS to pair({ l, r -> ov(B, l, 29f, r, 45f) }, 17f, 29f),
        Muscle.TRICEPS to pair({ l, r -> rr(B, l, 47f, r, 70f) }, 17f, 27f),
        Muscle.BICEPS to pair({ l, r -> rr(F, l, 47f, r, 70f) }, 17f, 27f),
        Muscle.FOREARMS to pair({ l, r -> rr(F, l, 72f, r, 100f) }, 15f, 26f) + pair({ l, r -> rr(B, l, 72f, r, 100f) }, 15f, 26f),
        Muscle.LATS to pair({ l, r -> rr(B, l, 52f, r, 86f) }, 29f, 40f),
        Muscle.UPPER_BACK to listOf(rr(B, 36f, 30f, 64f, 58f)),
        Muscle.LOWER_BACK to listOf(rr(B, 40f, 86f, 60f, 108f)),
        Muscle.ABS to listOf(rr(F, 42f, 56f, 58f, 94f)),
        Muscle.OBLIQUES to pair({ l, r -> rr(F, l, 58f, r, 92f) }, 32f, 41f),
        Muscle.GLUTES to pair({ l, r -> ov(B, l, 108f, r, 132f) }, 32f, 50f),
        Muscle.QUADS to pair({ l, r -> rr(F, l, 114f, r, 164f) }, 33f, 47f),
        Muscle.HAMSTRINGS to pair({ l, r -> rr(B, l, 134f, r, 172f) }, 33f, 47f),
        Muscle.CALVES to pair({ l, r -> rr(B, l, 174f, r, 202f) }, 34f, 46f),
        Muscle.HIP_FLEXORS to pair({ l, r -> ov(F, l, 94f, r, 110f) }, 36f, 49f),
        Muscle.ADDUCTORS to pair({ l, r -> rr(F, l, 114f, r, 142f) }, 45f, 50f),
    )

    /** The silhouette (same for both views): head, neck, torso, arms, legs. */
    val silhouette: List<Region> = listOf(
        ov(F, 40f, 3f, 60f, 25f), rr(F, 45f, 24f, 55f, 31f), rr(F, 29f, 28f, 71f, 112f),
        rr(F, 14f, 29f, 28f, 102f), rr(F, 72f, 29f, 86f, 102f), rr(F, 31f, 110f, 49f, 204f), rr(F, 51f, 110f, 69f, 204f),
    )
}

/** Which muscles to colour; used by [BodyMap] and by the tests. */
fun musclesDescription(primary: List<Muscle>, secondary: List<Muscle>, name: (Muscle) -> String): String =
    listOfNotNull(
        primary.takeIf { it.isNotEmpty() }?.joinToString(", ", transform = name),
        secondary.takeIf { it.isNotEmpty() }?.joinToString(", ", prefix = "+ ", transform = name),
    ).joinToString("; ")

@Composable
fun BodyMap(primary: List<Muscle>, secondary: List<Muscle> = emptyList(), modifier: Modifier = Modifier, showNamesOnTap: Boolean = true, tag: String = "body_map") {
    // doc 17 tokens: primary muscles gold (#E9C046), secondary "accentDim" (#8A7A3E on dark, #D8C88A on light)
    val gold = AppAccentTheme.colors.accent
    val dim = AppAccentTheme.colors.accentLight
    val base = MaterialTheme.colorScheme.surfaceVariant
    val outline = MaterialTheme.colorScheme.outline
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val desc = musclesDescription(primary, secondary) { ctx.getString(muscleLabel(it)) }
    val description = if (desc.isEmpty()) stringResource(R.string.body_map_none) else stringResource(R.string.body_map_description, desc)
    var showNames by remember { mutableStateOf(false) }
    Column(modifier.then(if (showNamesOnTap) Modifier.clickable { showNames = !showNames } else Modifier).semantics(mergeDescendants = true) { contentDescription = description }.testTag(tag),
        horizontalAlignment = Alignment.CenterHorizontally) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            for (view in BodyView.entries) Canvas(Modifier.width(60.dp).aspectRatio(BodyRegions.W / BodyRegions.H)) {
                drawFigure(view, primary, secondary, base, outline, gold, dim)
            }
        }
        if (showNames && showNamesOnTap) MuscleChips(primary, secondary, Modifier.padding(top = 4.dp).testTag("${tag}_names"))
    }
}

private fun DrawScope.shape(r: Region, color: Color, stroke: Color? = null) {
    val sx = size.width / BodyRegions.W; val sy = size.height / BodyRegions.H
    val tl = Offset(r.l * sx, r.t * sy); val sz = Size((r.r - r.l) * sx, (r.b - r.t) * sy)
    if (r.kind == ShapeKind.OVAL) {
        drawOval(color, tl, sz); if (stroke != null) drawOval(stroke, tl, sz, style = Stroke(1f))
    } else {
        val cr = CornerRadius(minOf(sz.width, sz.height) * 0.35f)
        drawRoundRect(color, tl, sz, cr); if (stroke != null) drawRoundRect(stroke, tl, sz, cr, style = Stroke(1f))
    }
}

private fun DrawScope.drawFigure(view: BodyView, primary: List<Muscle>, secondary: List<Muscle>, base: Color, outline: Color, accent: Color, light: Color) {
    for (r in BodyRegions.silhouette) shape(r.copy(view = view), base, outline)
    for (m in secondary) if (m !in primary) BodyRegions.byMuscle[m].orEmpty().filter { it.view == view }.forEach { shape(it, light) }
    for (m in primary) BodyRegions.byMuscle[m].orEmpty().filter { it.view == view }.forEach { shape(it, accent) }
}
