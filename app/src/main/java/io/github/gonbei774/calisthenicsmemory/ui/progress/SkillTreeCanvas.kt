// V13 (R23, doc 17 §2.6): draws a vertical TreeLayout. Each node is a horizontal tile (left third: the level number, a check when
// mastered, a lock when locked; right two thirds: the exercise picture) with only its name and stars under it. Edges run downwards
// and split where the paths branch. All geometry comes from the domain layout, so it is deterministic.
package io.github.gonbei774.calisthenicsmemory.ui.progress

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.calisthenics.domain.tree.EdgeKind
import app.calisthenics.domain.tree.TreeLayout
import app.calisthenics.domain.tree.TreeNodeState
import io.github.gonbei774.calisthenicsmemory.R
import io.github.gonbei774.calisthenicsmemory.ui.components.ExerciseThumb
import io.github.gonbei774.calisthenicsmemory.ui.components.StarRow
import io.github.gonbei774.calisthenicsmemory.ui.theme.AppAccentTheme

val CellWidth = 120.dp
val CellHeight = 138.dp
private val TileWidth = 108.dp
private val TileHeight = 52.dp
private val TopGap = 6.dp

/** [tier] is the level the person is at (1..5), shown in the tile's number cell. */
data class NodeUi(val id: String, val name: String, val familyId: String, val state: TreeNodeState, val stars: Int, val inGoal: Boolean, val missingEquipmentId: String?, val tier: Int = 1, /** R7-T11 (O2): a prerequisite from another tab that is still open: its id and name, shown as a tappable "needs X" note. */ val needs: Pair<String, String>? = null)

@Composable
fun stateLabel(s: TreeNodeState) = stringResource(
    when (s) {
        TreeNodeState.MASTERED -> R.string.node_mastered
        TreeNodeState.CURRENT -> R.string.node_current
        TreeNodeState.AVAILABLE -> R.string.node_available
        TreeNodeState.LOCKED -> R.string.node_locked
        TreeNodeState.NEEDS_EQUIPMENT -> R.string.node_needs_equipment
    },
)

@Composable
fun SkillTreeCanvas(layout: TreeLayout, nodes: Map<String, NodeUi>, mastered: Set<String>, selected: String?, onNode: (String) -> Unit, modifier: Modifier = Modifier, onNeed: (String) -> Unit = {}) {
    val edgeColor = MaterialTheme.colorScheme.outlineVariant
    val doneColor = AppAccentTheme.colors.accent
    val w: Dp = CellWidth * layout.cols
    val positionsByRow = remember(layout) { layout.nodes.groupBy { it.row } }
    val positionById = remember(layout) { layout.nodes.associateBy { it.variationId } }
    val rowIds = remember(layout) { (0 until layout.rows).toList() }
    Box(modifier) {
        Row(Modifier.fillMaxSize().horizontalScroll(rememberScrollState())) {
            Box(Modifier.width(w).fillMaxHeight()) {
                LazyColumn(Modifier.fillMaxSize().testTag("tree_rows")) {
            items(rowIds, key = { it }) { row ->
                Box(Modifier.width(w).height(CellHeight).testTag("tree_row_$row")) {
                    Canvas(Modifier.fillMaxSize()) {
                        clipRect {
                        val cw = CellWidth.toPx(); val ch = CellHeight.toPx(); val th = TileHeight.toPx(); val gap = TopGap.toPx()
                        val rowTop = row * ch
                        withTransform({ translate(top = -rowTop) }) {
                            for (e in layout.edges) {
                                val a = positionById[e.from] ?: continue
                                val b = positionById[e.to] ?: continue
                                val x1 = a.col * cw + cw / 2; val y1 = a.row * ch + gap + th
                                val x2 = b.col * cw + cw / 2; val y2 = b.row * ch + gap
                                if (maxOf(y1, y2) < rowTop || minOf(y1, y2) > rowTop + ch) continue
                                val mid = (y1 + y2) / 2
                                val p = Path().apply { moveTo(x1, y1); cubicTo(x1, mid, x2, mid, x2, y2) }
                                drawPath(p, if (e.from in mastered) doneColor else edgeColor, style = Stroke(width = (if (e.kind == EdgeKind.NEXT) 3.dp else 2.dp).toPx(), cap = StrokeCap.Round))
                            }
                        }
                        }
                    }
                    positionsByRow[row].orEmpty().forEach { pos ->
                        val n = nodes[pos.variationId] ?: return@forEach
                        Box(Modifier.offset(x = CellWidth * pos.col).size(CellWidth, CellHeight), contentAlignment = Alignment.TopCenter) {
                            TreeNode(n, n.id == selected, onNeed) { onNode(n.id) }
                        }
                    }
                }
            }
        }
            }
        }
    }
}

@Composable
private fun TreeNode(n: NodeUi, selected: Boolean, onNeed: (String) -> Unit, onClick: () -> Unit) {
    val locked = n.state == TreeNodeState.LOCKED || n.state == TreeNodeState.NEEDS_EQUIPMENT
    val gold = AppAccentTheme.colors.accent
    val label = stringResource(R.string.node_description, n.name, n.stars, 5, stateLabel(n.state))
    Column(
        Modifier.width(CellWidth - 8.dp).padding(top = TopGap).clickable(onClick = onClick, role = Role.Button).semantics(mergeDescendants = true) { contentDescription = label; role = Role.Button }.testTag("tree_node_${n.id}"),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        val border = when {
            selected -> BorderStroke(3.dp, MaterialTheme.colorScheme.onSurface)
            n.state == TreeNodeState.CURRENT -> BorderStroke(2.dp, gold)
            n.inGoal -> BorderStroke(2.dp, AppAccentTheme.colors.text)
            else -> BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        }
        Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surface, border = border, modifier = Modifier.size(TileWidth, TileHeight).alpha(if (locked) 0.55f else 1f)) {
            Row(Modifier.fillMaxSize()) {
                Box(Modifier.weight(1f).fillMaxHeight().background(if (n.state == TreeNodeState.MASTERED) gold else MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) {
                    val on = if (n.state == TreeNodeState.MASTERED) AppAccentTheme.colors.onAccent else MaterialTheme.colorScheme.onSurface
                    when (n.state) {
                        TreeNodeState.MASTERED -> Icon(Icons.Filled.Check, null, Modifier.size(22.dp), tint = on)
                        TreeNodeState.LOCKED -> Icon(Icons.Filled.Lock, null, Modifier.size(20.dp), tint = on)
                        TreeNodeState.NEEDS_EQUIPMENT -> Icon(Icons.Filled.Build, null, Modifier.size(20.dp), tint = on)
                        else -> Text(n.tier.toString(), style = MaterialTheme.typography.titleLarge, color = on, modifier = Modifier.testTag("tree_level_${n.id}"))
                    }
                }
                Box(Modifier.weight(2f).fillMaxHeight(), contentAlignment = Alignment.Center) { ExerciseThumb(n.id, n.name, 44.dp) }
            }
        }
        Text(n.name, style = MaterialTheme.typography.labelMedium, textAlign = TextAlign.Center, maxLines = 2, modifier = Modifier.padding(horizontal = 2.dp))
        StarRow(n.stars, size = 13.dp)
        n.needs?.let { (id, name) -> Text(stringResource(R.string.node_needs_note, name), style = MaterialTheme.typography.labelSmall, color = AppAccentTheme.colors.text, textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline, maxLines = 1,
            modifier = Modifier.heightIn(min = 24.dp).clickable(role = Role.Button) { onNeed(id) }.testTag("tree_need_${n.id}")) }
    }
}
