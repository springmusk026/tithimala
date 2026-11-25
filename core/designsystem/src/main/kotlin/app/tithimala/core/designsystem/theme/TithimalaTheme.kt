package app.tithimala.core.designsystem.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.platform.LocalConfiguration

private const val NEPALI_LANGUAGE_TAG = "ne"

object TithimalaTheme {
    val colors: TithimalaExtendedColors
        @Composable
        @ReadOnlyComposable
        get() = LocalTithimalaExtendedColors.current

    val spacing: TithimalaSpacing
        @Composable
        @ReadOnlyComposable
        get() = LocalTithimalaSpacing.current
}

@Composable
fun TithimalaTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    val colorScheme = if (darkTheme) TithimalaDarkColorScheme else TithimalaLightColorScheme
    val extendedColors = if (darkTheme) DarkExtendedColors else LightExtendedColors
    val primaryLocale = LocalConfiguration.current.locales.get(0)
    val typography = if (primaryLocale?.language == NEPALI_LANGUAGE_TAG) {
        TithimalaDevanagariTypography
    } else {
        TithimalaLatinTypography
    }

    CompositionLocalProvider(
        LocalTithimalaExtendedColors provides extendedColors,
        LocalTithimalaSpacing provides DefaultTithimalaSpacing
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = typography,
            shapes = TithimalaShapes,
            content = content
        )
    }
}
