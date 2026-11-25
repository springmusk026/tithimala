package app.tithimala.core.designsystem.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import kotlin.math.max
import kotlin.math.min
import kotlin.test.assertTrue
import org.junit.Test

class HimalayanDawnContrastTest {

    @Test
    fun lightPaletteMeetsWcagAaContrast() {
        val scheme = TithimalaLightColorScheme
        val ext = LightExtendedColors

        assertContrastAtLeast("onBackground / background", scheme.onBackground, scheme.background, AA_NORMAL)
        assertContrastAtLeast("onSurfaceVariant / surface", scheme.onSurfaceVariant, scheme.surface, AA_NORMAL)
        assertContrastAtLeast("textMuted / background", ext.textMuted, scheme.background, AA_NORMAL)
        assertContrastAtLeast("primary / background", scheme.primary, scheme.background, AA_NORMAL)
        assertContrastAtLeast("onPrimary / primary", scheme.onPrimary, scheme.primary, AA_NORMAL)
        assertContrastAtLeast(
            "onPrimaryContainer / primaryContainer",
            scheme.onPrimaryContainer,
            scheme.primaryContainer,
            AA_NORMAL
        )
        assertContrastAtLeast("secondary / background", scheme.secondary, scheme.background, AA_NORMAL)
        assertContrastAtLeast("onSecondary / secondary", scheme.onSecondary, scheme.secondary, AA_NORMAL)
        assertContrastAtLeast("tertiary / background", scheme.tertiary, scheme.background, AA_NORMAL)
        assertContrastAtLeast("onTertiary / tertiary", scheme.onTertiary, scheme.tertiary, AA_NORMAL)
        assertContrastAtLeast(
            "onHolidayContainer / holidayContainer",
            ext.onHolidayContainer,
            ext.holidayContainer,
            AA_NORMAL
        )
        assertContrastAtLeast(
            "onTithiContainer / tithiContainer",
            ext.onTithiContainer,
            ext.tithiContainer,
            AA_NORMAL
        )
        assertContrastAtLeast(
            "estimatedBadge / estimatedBadgeContainer",
            ext.estimatedBadge,
            ext.estimatedBadgeContainer,
            AA_NORMAL
        )
    }

    @Test
    fun darkPaletteMeetsWcagAaContrast() {
        val scheme = TithimalaDarkColorScheme
        val ext = DarkExtendedColors

        assertContrastAtLeast("onBackground / background", scheme.onBackground, scheme.background, AA_NORMAL)
        assertContrastAtLeast("onSurfaceVariant / surface", scheme.onSurfaceVariant, scheme.surface, AA_NORMAL)
        assertContrastAtLeast("textMuted / background", ext.textMuted, scheme.background, AA_NORMAL)
        assertContrastAtLeast("primary / background", scheme.primary, scheme.background, AA_NORMAL)
        assertContrastAtLeast("onPrimary / primary", scheme.onPrimary, scheme.primary, AA_NORMAL)
        assertContrastAtLeast(
            "onPrimaryContainer / primaryContainer",
            scheme.onPrimaryContainer,
            scheme.primaryContainer,
            AA_NORMAL
        )
        assertContrastAtLeast("secondary / background", scheme.secondary, scheme.background, AA_NORMAL)
        assertContrastAtLeast("onSecondary / secondary", scheme.onSecondary, scheme.secondary, AA_NORMAL)
        assertContrastAtLeast("tertiary / background", scheme.tertiary, scheme.background, AA_NORMAL)
        assertContrastAtLeast("onTertiary / tertiary", scheme.onTertiary, scheme.tertiary, AA_NORMAL)
        assertContrastAtLeast(
            "onHolidayContainer / holidayContainer",
            ext.onHolidayContainer,
            ext.holidayContainer,
            AA_NORMAL
        )
        assertContrastAtLeast(
            "onTithiContainer / tithiContainer",
            ext.onTithiContainer,
            ext.tithiContainer,
            AA_NORMAL
        )
        assertContrastAtLeast(
            "estimatedBadge / estimatedBadgeContainer",
            ext.estimatedBadge,
            ext.estimatedBadgeContainer,
            AA_NORMAL
        )
    }

    private fun assertContrastAtLeast(label: String, foreground: Color, background: Color, minimumRatio: Double) {
        val ratio = contrastRatio(foreground, background)
        assertTrue(
            ratio >= minimumRatio,
            "Expected contrast >= $minimumRatio for $label, but got ${"%.2f".format(ratio)}"
        )
    }

    private fun contrastRatio(first: Color, second: Color): Double {
        val l1 = first.luminance().toDouble()
        val l2 = second.luminance().toDouble()
        val lighter = max(l1, l2)
        val darker = min(l1, l2)
        return (lighter + LUMINANCE_OFFSET) / (darker + LUMINANCE_OFFSET)
    }

    private companion object {
        const val AA_NORMAL = 4.5
        const val LUMINANCE_OFFSET = 0.05
    }
}
