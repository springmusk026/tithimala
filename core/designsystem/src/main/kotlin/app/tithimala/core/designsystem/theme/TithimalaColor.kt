package app.tithimala.core.designsystem.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// Himalayan Dawn — Light palette (Lokta parchment, sindoor vermilion, Himalayan slate, antique brass)
internal val DawnParchment = Color(0xFFFAF7F2)
internal val DawnSurfaceLowest = Color(0xFFFFFFFF)
internal val DawnSurfaceLow = Color(0xFFF4EFE7)
internal val DawnSurfaceContainer = Color(0xFFEEE8DE)
internal val DawnSurfaceHigh = Color(0xFFE7DFD3)
internal val DawnSurfaceHighest = Color(0xFFDFD6C7)

internal val DawnInkPrimary = Color(0xFF1C1917)
internal val DawnInkSecondary = Color(0xFF57504A)
internal val DawnInkMuted = Color(0xFF6E665E)
internal val DawnOutline = Color(0xFF7A726A)
internal val DawnHairline = Color(0xFFDDD5C9)

internal val SindoorPrimary = Color(0xFFA93226)
internal val SindoorOnPrimary = Color(0xFFFFFFFF)
internal val SindoorContainer = Color(0xFFFBE4E0)
internal val SindoorOnContainer = Color(0xFF5C140E)

internal val RidgeSlateSecondary = Color(0xFF3B526B)
internal val RidgeSlateOnSecondary = Color(0xFFFFFFFF)
internal val RidgeSlateContainer = Color(0xFFE1EAF2)
internal val RidgeSlateOnContainer = Color(0xFF1B2D40)

internal val BrassTertiary = Color(0xFF8A5A10)
internal val BrassOnTertiary = Color(0xFFFFFFFF)
internal val BrassContainer = Color(0xFFF6E9CE)
internal val BrassOnContainer = Color(0xFF3D2604)

internal val DawnError = Color(0xFFB3261E)
internal val DawnOnError = Color(0xFFFFFFFF)
internal val DawnErrorContainer = Color(0xFFF9DEDC)
internal val DawnOnErrorContainer = Color(0xFF410E0B)

// Himalayan Dusk — Dark palette (Deep night slate, dawn ember, moonlit glacier, warm brass)
internal val DuskNight = Color(0xFF121316)
internal val DuskSurfaceLowest = Color(0xFF0D0E11)
internal val DuskSurfaceLow = Color(0xFF181A1F)
internal val DuskSurfaceContainer = Color(0xFF1F2228)
internal val DuskSurfaceHigh = Color(0xFF282C34)
internal val DuskSurfaceHighest = Color(0xFF323741)

internal val DuskInkPrimary = Color(0xFFF2ECE4)
internal val DuskInkSecondary = Color(0xFFC1B9AE)
internal val DuskInkMuted = Color(0xFF9B948B)
internal val DuskOutline = Color(0xFF8E877E)
internal val DuskHairline = Color(0xFF2B2F37)

internal val EmberPrimary = Color(0xFFF08A7B)
internal val EmberOnPrimary = Color(0xFF3D0B07)
internal val EmberContainer = Color(0xFF5E1D17)
internal val EmberOnContainer = Color(0xFFFFDAD5)

internal val MoonlitSecondary = Color(0xFFA8C4E0)
internal val MoonlitOnSecondary = Color(0xFF102438)
internal val MoonlitContainer = Color(0xFF263B52)
internal val MoonlitOnContainer = Color(0xFFDCEAF7)

internal val WarmBrassTertiary = Color(0xFFE2B96F)
internal val WarmBrassOnTertiary = Color(0xFF382302)
internal val WarmBrassContainer = Color(0xFF4E350B)
internal val WarmBrassOnContainer = Color(0xFFFCE8C0)

internal val DuskError = Color(0xFFF2B8B5)
internal val DuskOnError = Color(0xFF601410)
internal val DuskErrorContainer = Color(0xFF8C1D18)
internal val DuskOnErrorContainer = Color(0xFFF9DEDC)

internal val TithimalaLightColorScheme = lightColorScheme(
    primary = SindoorPrimary,
    onPrimary = SindoorOnPrimary,
    primaryContainer = SindoorContainer,
    onPrimaryContainer = SindoorOnContainer,
    secondary = RidgeSlateSecondary,
    onSecondary = RidgeSlateOnSecondary,
    secondaryContainer = RidgeSlateContainer,
    onSecondaryContainer = RidgeSlateOnContainer,
    tertiary = BrassTertiary,
    onTertiary = BrassOnTertiary,
    tertiaryContainer = BrassContainer,
    onTertiaryContainer = BrassOnContainer,
    error = DawnError,
    onError = DawnOnError,
    errorContainer = DawnErrorContainer,
    onErrorContainer = DawnOnErrorContainer,
    background = DawnParchment,
    onBackground = DawnInkPrimary,
    surface = DawnParchment,
    onSurface = DawnInkPrimary,
    surfaceVariant = DawnSurfaceContainer,
    onSurfaceVariant = DawnInkSecondary,
    surfaceContainerLowest = DawnSurfaceLowest,
    surfaceContainerLow = DawnSurfaceLow,
    surfaceContainer = DawnSurfaceContainer,
    surfaceContainerHigh = DawnSurfaceHigh,
    surfaceContainerHighest = DawnSurfaceHighest,
    outline = DawnOutline,
    outlineVariant = DawnHairline,
    inverseSurface = DuskSurfaceContainer,
    inverseOnSurface = DuskInkPrimary,
    inversePrimary = EmberPrimary
)

internal val TithimalaDarkColorScheme = darkColorScheme(
    primary = EmberPrimary,
    onPrimary = EmberOnPrimary,
    primaryContainer = EmberContainer,
    onPrimaryContainer = EmberOnContainer,
    secondary = MoonlitSecondary,
    onSecondary = MoonlitOnSecondary,
    secondaryContainer = MoonlitContainer,
    onSecondaryContainer = MoonlitOnContainer,
    tertiary = WarmBrassTertiary,
    onTertiary = WarmBrassOnTertiary,
    tertiaryContainer = WarmBrassContainer,
    onTertiaryContainer = WarmBrassOnContainer,
    error = DuskError,
    onError = DuskOnError,
    errorContainer = DuskErrorContainer,
    onErrorContainer = DuskOnErrorContainer,
    background = DuskNight,
    onBackground = DuskInkPrimary,
    surface = DuskNight,
    onSurface = DuskInkPrimary,
    surfaceVariant = DuskSurfaceContainer,
    onSurfaceVariant = DuskInkSecondary,
    surfaceContainerLowest = DuskSurfaceLowest,
    surfaceContainerLow = DuskSurfaceLow,
    surfaceContainer = DuskSurfaceContainer,
    surfaceContainerHigh = DuskSurfaceHigh,
    surfaceContainerHighest = DuskSurfaceHighest,
    outline = DuskOutline,
    outlineVariant = DuskHairline,
    inverseSurface = DawnSurfaceContainer,
    inverseOnSurface = DawnInkPrimary,
    inversePrimary = SindoorPrimary
)

/**
 * Semantic calendar color tokens beyond Material 3 core roles.
 */
@Immutable
data class TithimalaExtendedColors(
    val holiday: Color,
    val holidayContainer: Color,
    val onHolidayContainer: Color,
    val saturday: Color,
    val todayFill: Color,
    val onTodayFill: Color,
    val tithiLunar: Color,
    val tithiContainer: Color,
    val onTithiContainer: Color,
    val festivalDot: Color,
    val estimatedBadge: Color,
    val estimatedBadgeContainer: Color,
    val hairline: Color,
    val textMuted: Color
)

internal val LightExtendedColors = TithimalaExtendedColors(
    holiday = SindoorPrimary,
    holidayContainer = Color(0xFFFCE8E5),
    onHolidayContainer = Color(0xFF691811),
    saturday = SindoorPrimary,
    todayFill = SindoorPrimary,
    onTodayFill = SindoorOnPrimary,
    tithiLunar = RidgeSlateSecondary,
    tithiContainer = RidgeSlateContainer,
    onTithiContainer = RidgeSlateOnContainer,
    festivalDot = BrassTertiary,
    estimatedBadge = Color(0xFF6B4E16),
    estimatedBadgeContainer = Color(0xFFF4EBD6),
    hairline = DawnHairline,
    textMuted = DawnInkMuted
)

internal val DarkExtendedColors = TithimalaExtendedColors(
    holiday = EmberPrimary,
    holidayContainer = Color(0xFF3D1916),
    onHolidayContainer = EmberOnContainer,
    saturday = EmberPrimary,
    todayFill = EmberPrimary,
    onTodayFill = EmberOnPrimary,
    tithiLunar = MoonlitSecondary,
    tithiContainer = MoonlitContainer,
    onTithiContainer = MoonlitOnContainer,
    festivalDot = WarmBrassTertiary,
    estimatedBadge = WarmBrassTertiary,
    estimatedBadgeContainer = Color(0xFF3A2B0E),
    hairline = DuskHairline,
    textMuted = DuskInkMuted
)

internal val LocalTithimalaExtendedColors = staticCompositionLocalOf { LightExtendedColors }
