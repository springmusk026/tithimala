package app.tithimala.core.designsystem.theme

import android.provider.Settings
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode

/**
 * Damped Compose spring specs for Tithimala that honor system reduced-motion settings.
 */
object TithimalaMotion {
    private const val DAMPING_RATIO = 0.88f
    private const val STIFFNESS_STANDARD = 420f
    private const val STIFFNESS_GENTLE = 280f

    fun <T> standardSpring(reducedMotion: Boolean = false, visibilityThreshold: T? = null): FiniteAnimationSpec<T> =
        if (reducedMotion) {
            snap()
        } else {
            spring(
                dampingRatio = DAMPING_RATIO,
                stiffness = STIFFNESS_STANDARD,
                visibilityThreshold = visibilityThreshold
            )
        }

    fun <T> gentleSpring(reducedMotion: Boolean = false, visibilityThreshold: T? = null): FiniteAnimationSpec<T> =
        if (reducedMotion) {
            snap()
        } else {
            spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = STIFFNESS_GENTLE,
                visibilityThreshold = visibilityThreshold
            )
        }
}

@Composable
fun rememberReducedMotionEnabled(): Boolean {
    if (LocalInspectionMode.current) return false
    val context = LocalContext.current
    return remember(context) {
        val scale = Settings.Global.getFloat(
            context.contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            1f
        )
        scale == 0f
    }
}
