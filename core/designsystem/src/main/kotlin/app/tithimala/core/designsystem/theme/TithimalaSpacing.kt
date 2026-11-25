package app.tithimala.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * 8dp spacing grid with 4dp micro steps for the Himalayan Dawn design system.
 */
@Immutable
data class TithimalaSpacing(
    val hairline: Dp = 1.dp,
    val micro: Dp = 4.dp,
    val xs: Dp = 8.dp,
    val sm: Dp = 12.dp,
    val md: Dp = 16.dp,
    val lg: Dp = 24.dp,
    val xl: Dp = 32.dp,
    val minTouchTarget: Dp = 48.dp
)

internal val DefaultTithimalaSpacing = TithimalaSpacing()

internal val LocalTithimalaSpacing = staticCompositionLocalOf { DefaultTithimalaSpacing }
