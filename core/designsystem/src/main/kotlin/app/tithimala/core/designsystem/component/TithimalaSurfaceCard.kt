package app.tithimala.core.designsystem.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import app.tithimala.core.designsystem.theme.TithimalaTheme

/**
 * Tonal surface card separated by a quiet hairline border rather than drop shadows.
 */
@Composable
fun TithimalaSurfaceCard(
    modifier: Modifier = Modifier,
    shape: Shape = MaterialTheme.shapes.large,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainerLow,
    borderColor: Color = TithimalaTheme.colors.hairline,
    contentPadding: PaddingValues = PaddingValues(TithimalaTheme.spacing.md),
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier,
        shape = shape,
        color = containerColor,
        border = BorderStroke(width = TithimalaTheme.spacing.hairline, color = borderColor)
    ) {
        Column(modifier = Modifier.padding(contentPadding), content = content)
    }
}
