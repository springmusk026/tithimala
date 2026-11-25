package app.tithimala.core.ui.calendar

import androidx.annotation.StringRes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.tithimala.core.designsystem.preview.TithimalaThemePreviews
import app.tithimala.core.designsystem.theme.TithimalaTheme
import app.tithimala.core.ui.R

private val LUNAR_GLYPH_SIZE = 16.dp
private val LUNAR_STROKE_WIDTH = 1.25.dp
private const val TERMINATOR_X_FRACTION = 0.46f
private const val TOP_ANGLE_DEG = -90f
private const val BOTTOM_ANGLE_DEG = 90f
private const val HALF_SWEEP_DEG = 180f
private const val MAX_BADGE_LINES = 2

/**
 * Eight canonical lunar phase visuals aligned with Northern Hemisphere (Nepal) illumination.
 */
@Immutable
enum class LunarPhaseVisual {
    NEW_MOON,
    WAXING_CRESCENT,
    FIRST_QUARTER,
    WAXING_GIBBOUS,
    FULL_MOON,
    WANING_GIBBOUS,
    LAST_QUARTER,
    WANING_CRESCENT
}

/**
 * Quiet tonal badge pairing a custom 16dp lunar phase glyph with the Tithi label.
 * Switches to the estimated badge palette and appends a localized estimated indicator when [isEstimated] is true.
 */
@Composable
fun TithiMoonBadge(
    tithiLabel: String,
    phase: LunarPhaseVisual?,
    modifier: Modifier = Modifier,
    isEstimated: Boolean = false
) {
    val spacing = TithimalaTheme.spacing
    val extendedColors = TithimalaTheme.colors
    val containerColor = if (isEstimated) {
        extendedColors.estimatedBadgeContainer
    } else {
        extendedColors.tithiContainer
    }
    val textColor = if (isEstimated) {
        extendedColors.estimatedBadge
    } else {
        extendedColors.onTithiContainer
    }
    val glyphTint = if (isEstimated) {
        extendedColors.estimatedBadge
    } else {
        extendedColors.tithiLunar
    }
    val displayText = if (isEstimated) {
        stringResource(R.string.core_ui_tithi_estimated_format, tithiLabel)
    } else {
        tithiLabel
    }

    Surface(
        modifier = modifier.semantics(mergeDescendants = true) {},
        shape = MaterialTheme.shapes.small,
        color = containerColor
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = spacing.xs,
                vertical = spacing.micro
            ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(spacing.micro)
        ) {
            if (phase != null) {
                LunarPhaseGlyph(
                    phase = phase,
                    tint = glyphTint,
                    contentDescription = stringResource(phase.descriptionRes())
                )
            }
            Text(
                text = displayText,
                style = MaterialTheme.typography.labelMedium,
                color = textColor,
                maxLines = MAX_BADGE_LINES,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun LunarPhaseGlyph(
    phase: LunarPhaseVisual,
    tint: Color,
    contentDescription: String,
    modifier: Modifier = Modifier
) {
    val litPath = remember { Path() }
    Canvas(
        modifier = modifier
            .size(LUNAR_GLYPH_SIZE)
            .semantics {
                this.contentDescription = contentDescription
                this.role = Role.Image
            }
    ) {
        drawLunarPhase(
            phase = phase,
            tint = tint,
            litPath = litPath
        )
    }
}

private fun DrawScope.drawLunarPhase(phase: LunarPhaseVisual, tint: Color, litPath: Path) {
    val diameter = size.minDimension
    val center = Offset(x = size.width / 2f, y = size.height / 2f)
    if (phase == LunarPhaseVisual.FULL_MOON) {
        drawCircle(color = tint, radius = diameter / 2f, center = center)
        return
    }

    val strokeWidth = LUNAR_STROKE_WIDTH.toPx()
    val radius = (diameter - strokeWidth) / 2f
    if (phase != LunarPhaseVisual.NEW_MOON) {
        buildIlluminatedLunarPath(
            phase = phase,
            center = center,
            radius = radius,
            targetPath = litPath
        )
        drawPath(path = litPath, color = tint)
    }
    drawCircle(
        color = tint,
        radius = radius,
        center = center,
        style = Stroke(width = strokeWidth)
    )
}

private fun buildIlluminatedLunarPath(phase: LunarPhaseVisual, center: Offset, radius: Float, targetPath: Path) {
    val circleRect = Rect(
        left = center.x - radius,
        top = center.y - radius,
        right = center.x + radius,
        bottom = center.y + radius
    )
    val terminatorHalfWidth = radius * TERMINATOR_X_FRACTION
    val terminatorRect = Rect(
        left = center.x - terminatorHalfWidth,
        top = center.y - radius,
        right = center.x + terminatorHalfWidth,
        bottom = center.y + radius
    )

    targetPath.reset()
    when (phase) {
        LunarPhaseVisual.WAXING_CRESCENT -> {
            targetPath.arcTo(circleRect, TOP_ANGLE_DEG, HALF_SWEEP_DEG, forceMoveTo = true)
            targetPath.arcTo(terminatorRect, BOTTOM_ANGLE_DEG, -HALF_SWEEP_DEG, forceMoveTo = false)
        }
        LunarPhaseVisual.FIRST_QUARTER -> {
            targetPath.arcTo(circleRect, TOP_ANGLE_DEG, HALF_SWEEP_DEG, forceMoveTo = true)
        }
        LunarPhaseVisual.WAXING_GIBBOUS -> {
            targetPath.arcTo(circleRect, TOP_ANGLE_DEG, HALF_SWEEP_DEG, forceMoveTo = true)
            targetPath.arcTo(terminatorRect, BOTTOM_ANGLE_DEG, HALF_SWEEP_DEG, forceMoveTo = false)
        }
        LunarPhaseVisual.WANING_GIBBOUS -> {
            targetPath.arcTo(circleRect, BOTTOM_ANGLE_DEG, HALF_SWEEP_DEG, forceMoveTo = true)
            targetPath.arcTo(terminatorRect, TOP_ANGLE_DEG, HALF_SWEEP_DEG, forceMoveTo = false)
        }
        LunarPhaseVisual.LAST_QUARTER -> {
            targetPath.arcTo(circleRect, BOTTOM_ANGLE_DEG, HALF_SWEEP_DEG, forceMoveTo = true)
        }
        LunarPhaseVisual.WANING_CRESCENT -> {
            targetPath.arcTo(circleRect, BOTTOM_ANGLE_DEG, HALF_SWEEP_DEG, forceMoveTo = true)
            targetPath.arcTo(terminatorRect, TOP_ANGLE_DEG, -HALF_SWEEP_DEG, forceMoveTo = false)
        }
        LunarPhaseVisual.NEW_MOON, LunarPhaseVisual.FULL_MOON -> Unit
    }
    targetPath.close()
}

@StringRes
private fun LunarPhaseVisual.descriptionRes(): Int = when (this) {
    LunarPhaseVisual.NEW_MOON -> R.string.core_ui_lunar_phase_new_moon
    LunarPhaseVisual.WAXING_CRESCENT -> R.string.core_ui_lunar_phase_waxing_crescent
    LunarPhaseVisual.FIRST_QUARTER -> R.string.core_ui_lunar_phase_first_quarter
    LunarPhaseVisual.WAXING_GIBBOUS -> R.string.core_ui_lunar_phase_waxing_gibbous
    LunarPhaseVisual.FULL_MOON -> R.string.core_ui_lunar_phase_full_moon
    LunarPhaseVisual.WANING_GIBBOUS -> R.string.core_ui_lunar_phase_waning_gibbous
    LunarPhaseVisual.LAST_QUARTER -> R.string.core_ui_lunar_phase_last_quarter
    LunarPhaseVisual.WANING_CRESCENT -> R.string.core_ui_lunar_phase_waning_crescent
}

@OptIn(ExperimentalLayoutApi::class)
@TithimalaThemePreviews
@Composable
private fun TithiMoonBadgePreview() {
    TithimalaTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            FlowRow(
                modifier = Modifier.padding(TithimalaTheme.spacing.md),
                horizontalArrangement = Arrangement.spacedBy(TithimalaTheme.spacing.xs),
                verticalArrangement = Arrangement.spacedBy(TithimalaTheme.spacing.xs)
            ) {
                TithiMoonBadge(
                    tithiLabel = stringResource(R.string.core_ui_preview_tithi_purnima),
                    phase = LunarPhaseVisual.FULL_MOON
                )
                TithiMoonBadge(
                    tithiLabel = stringResource(R.string.core_ui_preview_tithi_tritiya),
                    phase = LunarPhaseVisual.WAXING_CRESCENT,
                    isEstimated = true
                )
                TithiMoonBadge(
                    tithiLabel = stringResource(R.string.core_ui_preview_tithi_ashtami),
                    phase = LunarPhaseVisual.FIRST_QUARTER
                )
                TithiMoonBadge(
                    tithiLabel = stringResource(R.string.core_ui_preview_tithi_amavasya),
                    phase = LunarPhaseVisual.NEW_MOON
                )
            }
        }
    }
}
