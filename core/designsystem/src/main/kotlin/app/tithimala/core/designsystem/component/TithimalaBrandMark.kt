package app.tithimala.core.designsystem.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private const val PHASE_BEAD_COUNT = 8
private const val NEW_MOON_INDEX = 4
private const val PHASE_ANGLE_STEP_DEG = 45.0
private const val TOP_ANGLE_DEG = -90.0
private const val DEG_TO_RAD = PI / 180.0
private const val GARLAND_RING_ALPHA = 0.55f
private const val GARLAND_RADIUS_FRACTION = 0.39f
private const val RING_STROKE_FRACTION = 0.024f
private const val CRESCENT_OUTER_FRACTION = 0.215f
private const val CRESCENT_CUTOUT_OFFSET_X = 0.125f
private const val CRESCENT_CUTOUT_RADIUS_FRACTION = 0.188f
private const val BINDU_OFFSET_X = 0.11f
private const val BINDU_RADIUS_FRACTION = 0.032f
private val BEAD_RADIUS_FACTORS = floatArrayOf(0.056f, 0.045f, 0.040f, 0.034f, 0.045f, 0.034f, 0.040f, 0.045f)

/**
 * Tithimala's Moon-Phase Garland brand mark: an 8-bead lunar cycle surrounding a waxing crescent.
 */
@Composable
fun TithimalaBrandMark(
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    tint: Color = MaterialTheme.colorScheme.primary,
    contentDescription: String? = null
) {
    val semanticsModifier = if (contentDescription != null) {
        Modifier.semantics {
            this.contentDescription = contentDescription
            this.role = Role.Image
        }
    } else {
        Modifier
    }

    Canvas(
        modifier = modifier
            .size(size)
            .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
            .then(semanticsModifier)
    ) {
        val diameter = this.size.minDimension
        val center = Offset(x = this.size.width / 2f, y = this.size.height / 2f)
        drawGarlandRing(center = center, diameter = diameter, tint = tint)
        drawLunarPhaseBeads(center = center, diameter = diameter, tint = tint)
        drawCentralCrescent(center = center, diameter = diameter, tint = tint)
    }
}

private fun DrawScope.drawGarlandRing(center: Offset, diameter: Float, tint: Color) {
    drawCircle(
        color = tint.copy(alpha = GARLAND_RING_ALPHA),
        radius = diameter * GARLAND_RADIUS_FRACTION,
        center = center,
        style = Stroke(width = diameter * RING_STROKE_FRACTION)
    )
}

private fun DrawScope.drawLunarPhaseBeads(center: Offset, diameter: Float, tint: Color) {
    val garlandRadius = diameter * GARLAND_RADIUS_FRACTION
    val strokeWidth = diameter * RING_STROKE_FRACTION
    for (index in 0 until PHASE_BEAD_COUNT) {
        val angleRad = (TOP_ANGLE_DEG + index * PHASE_ANGLE_STEP_DEG) * DEG_TO_RAD
        val beadCenter = Offset(
            x = center.x + (garlandRadius * cos(angleRad)).toFloat(),
            y = center.y + (garlandRadius * sin(angleRad)).toFloat()
        )
        val beadRadius = diameter * BEAD_RADIUS_FACTORS[index]
        if (index == NEW_MOON_INDEX) {
            drawCircle(
                color = Color.Transparent,
                radius = beadRadius,
                center = beadCenter,
                blendMode = BlendMode.Clear
            )
            drawCircle(color = tint, radius = beadRadius, center = beadCenter, style = Stroke(width = strokeWidth))
        } else {
            drawCircle(color = tint, radius = beadRadius, center = beadCenter)
        }
    }
}

private fun DrawScope.drawCentralCrescent(center: Offset, diameter: Float, tint: Color) {
    val outerRadius = diameter * CRESCENT_OUTER_FRACTION
    val cutoutRadius = diameter * CRESCENT_CUTOUT_RADIUS_FRACTION
    val outerCircle = Path().apply {
        addOval(
            Rect(
                left = center.x - outerRadius,
                top = center.y - outerRadius,
                right = center.x + outerRadius,
                bottom = center.y + outerRadius
            )
        )
    }
    val cutoutCenterX = center.x + diameter * CRESCENT_CUTOUT_OFFSET_X
    val innerCircle = Path().apply {
        addOval(
            Rect(
                left = cutoutCenterX - cutoutRadius,
                top = center.y - cutoutRadius,
                right = cutoutCenterX + cutoutRadius,
                bottom = center.y + cutoutRadius
            )
        )
    }
    val crescentPath = Path().apply {
        op(outerCircle, innerCircle, PathOperation.Difference)
    }
    drawPath(path = crescentPath, color = tint)
    drawCircle(
        color = tint,
        radius = diameter * BINDU_RADIUS_FRACTION,
        center = Offset(x = center.x + diameter * BINDU_OFFSET_X, y = center.y)
    )
}
