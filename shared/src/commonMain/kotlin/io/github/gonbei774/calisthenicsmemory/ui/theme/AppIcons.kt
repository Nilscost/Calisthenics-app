package io.github.gonbei774.calisthenicsmemory.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/** Icons that are not in material-icons-core. Paths from Material Symbols (Apache-2.0, see THIRD_PARTY_NOTICES.md). */
object AppIcons {
    val Remove: ImageVector by lazy {
        ImageVector.Builder("Remove", 24.dp, 24.dp, 24f, 24f).path(fill = SolidColor(Color.Black)) {
            moveTo(19f, 13f); horizontalLineTo(5f); verticalLineTo(11f); horizontalLineTo(19f); verticalLineTo(13f); close()
        }.build()
    }
}
