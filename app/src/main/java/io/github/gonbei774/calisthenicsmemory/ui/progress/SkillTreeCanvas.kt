// U10 (F16): draws a TreeLayout. Nodes are round family icons with a name and up to five stars; edges are curves that merge
// where an exercise has several prerequisites. All geometry comes from the domain layout, so it is deterministic.
package io.github.gonbei774.calisthenicsmemory.ui.progress

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
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
import io.github.gonbei774.calisthenicsmemory.ui.components.StarRow
import io.github.gonbei774.calisthenicsmemory.ui.theme.AppAccentTheme
import io.github.gonbei774.calisthenicsmemory.ui.theme.EquipmentIcons
import io.github.gonbei774.calisthenicsmemory.ui.theme.FamilyIcons

val CellWidth = 132.dp
val CellHeight = 152.dp
private val NodeSize = 64.dp

data class NodeUi(val id: String, val name: String, val familyId: String, val state: TreeNodeState, val stars: Int, val inGoal: Boolean, val missingEquipmentId: String?)

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
fun SkillTreeCanvas(layout: TreeLayout, nodes: Map<String, NodeUi>, mastered: Set<String>, onNode: (String) -> Unit, modifier: Modifier = Modifier) {
    val edgeColor = MaterialTheme.colorScheme.outlineVariant
    val doneColor = MaterialTheme.colorScheme.primary
    val w: Dp = CellWidth * layout.cols
    val h: Dp = CellHeight * layout.rows
    Box(modifier.size(w + 16.dp, h + 16.dp).padding(8.dp)) {
        Canvas(Modifier.size(w, h)) {
            val cw = CellWidth.toPx(); val ch = CellHeight.toPx(); val r = NodeSize.toPx() / 2
            for (e in layout.edges) {
                val a = layout.node(e.from) ?: continue
                val b = layout.node(e.to) ?: continue
                val x1 = a.col * cw + cw / 2 + r; val y1 = a.row * ch + 8.dp.toPx() + r
                val x2 = b.col * cw + cw / 2 - r; val y2 = b.row * ch + 8.dp.toPx() + r
                val p = Path().apply { moveTo(x1, y1); cubicTo((x1 + x2) / 2, y1, (x1 + x2) / 2, y2, x2, y2) }
                drawPath(p, if (e.from in mastered) doneColor else edgeColor, style = Stroke(width = (if (e.kind == EdgeKind.NEXT) 3.dp else 2.dp).toPx(), cap = StrokeCap.Round))
            }
        }
        layout.nodes.forEach { pos ->
            val n = nodes[pos.variationId] ?: return@forEach
            Box(Modifier.offset(CellWidth * pos.col, CellHeight * pos.row).size(CellWidth, CellHeight), contentAlignment = Alignment.TopCenter) {
                TreeNode(n) { onNode(n.id) }
            }
        }
    }
}

@Composable
private fun TreeNode(n: NodeUi, onClick: () -> Unit) {
    val locked = n.state == TreeNodeState.LOCKED || n.state == TreeNodeState.NEEDS_EQUIPMENT
    val accent = AppAccentTheme.colors.accent
    val label = stringResource(R.string.node_description, n.name, n.stars, 5, stateLabel(n.state))
    Column(
        Modifier.width(CellWidth - 8.dp).padding(top = 8.dp).clickable(onClick = onClick, role = Role.Button).semantics(mergeDescendants = true) { contentDescription = label; role = Role.Button }.testTag("tree_node_${n.id}"),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Box(contentAlignment = Alignment.Center) {
            val bg = when (n.state) {
                TreeNodeState.MASTERED -> MaterialTheme.colorScheme.primary
                TreeNodeState.CURRENT -> MaterialTheme.colorScheme.primaryContainer
                TreeNodeState.AVAILABLE -> MaterialTheme.colorScheme.surface
                else -> MaterialTheme.colorScheme.surfaceVariant
            }
            val ring = when {
                n.state == TreeNodeState.CURRENT -> BorderStroke(4.dp, MaterialTheme.colorScheme.primary)
                n.state == TreeNodeState.AVAILABLE -> BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                n.inGoal -> BorderStroke(3.dp, accent)
                else -> null
            }
            Surface(shape = CircleShape, color = bg, border = ring, modifier = Modifier.size(NodeSize).alpha(if (locked) 0.6f else 1f)) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        FamilyIcons.forFamily(n.familyId), contentDescription = null, modifier = Modifier.size(34.dp),
                        tint = if (n.state == TreeNodeState.MASTERED) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
            if (locked) Surface(shape = CircleShape, color = MaterialTheme.colorScheme.secondaryContainer, modifier = Modifier.align(Alignment.BottomEnd).size(24.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    if (n.state == TreeNodeState.LOCKED) Icon(Icons.Filled.Lock, null, Modifier.size(14.dp))
                    else Icon(n.missingEquipmentId?.let { EquipmentIcons.forId(it) } ?: Icons.Filled.Build, null, Modifier.size(14.dp))
                }
            }
        }
        Text(n.name, style = MaterialTheme.typography.labelMedium, textAlign = TextAlign.Center, maxLines = 2, modifier = Modifier.padding(horizontal = 2.dp))
        if (n.state == TreeNodeState.CURRENT) Text(stringResource(R.string.node_training_now), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
        StarRow(n.stars, size = 13.dp)
    }
}
