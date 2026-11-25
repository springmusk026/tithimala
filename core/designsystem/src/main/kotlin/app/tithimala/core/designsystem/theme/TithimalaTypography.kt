package app.tithimala.core.designsystem.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import app.tithimala.core.designsystem.R

val MuktaFontFamily = FontFamily(
    Font(resId = R.font.mukta_regular, weight = FontWeight.Normal),
    Font(resId = R.font.mukta_medium, weight = FontWeight.Medium),
    Font(resId = R.font.mukta_semibold, weight = FontWeight.SemiBold)
)

val NotoSerifDevanagariFontFamily = FontFamily(
    Font(resId = R.font.noto_serif_devanagari_medium, weight = FontWeight.Medium),
    Font(resId = R.font.noto_serif_devanagari_semibold, weight = FontWeight.SemiBold)
)

private fun tabularStyle(
    fontFamily: FontFamily,
    weight: FontWeight,
    size: TextUnit,
    lineHeight: TextUnit,
    letterSpacing: TextUnit = 0.sp
): TextStyle = TextStyle(
    fontFamily = fontFamily,
    fontWeight = weight,
    fontSize = size,
    lineHeight = lineHeight,
    letterSpacing = letterSpacing,
    fontFeatureSettings = "tnum"
)

internal fun buildTithimalaTypography(editorialFamily: FontFamily): Typography = Typography(
    displayLarge = tabularStyle(
        fontFamily = editorialFamily,
        weight = FontWeight.SemiBold,
        size = 40.sp,
        lineHeight = 52.sp,
        letterSpacing = (-0.25).sp
    ),
    displayMedium = tabularStyle(editorialFamily, FontWeight.SemiBold, size = 32.sp, lineHeight = 42.sp),
    displaySmall = tabularStyle(editorialFamily, FontWeight.Medium, size = 26.sp, lineHeight = 36.sp),
    headlineLarge = tabularStyle(editorialFamily, FontWeight.SemiBold, size = 24.sp, lineHeight = 34.sp),
    headlineMedium = tabularStyle(editorialFamily, FontWeight.Medium, size = 20.sp, lineHeight = 28.sp),
    headlineSmall = tabularStyle(editorialFamily, FontWeight.Medium, size = 18.sp, lineHeight = 26.sp),
    titleLarge = tabularStyle(MuktaFontFamily, FontWeight.SemiBold, size = 20.sp, lineHeight = 28.sp),
    titleMedium = tabularStyle(MuktaFontFamily, FontWeight.SemiBold, size = 16.sp, lineHeight = 24.sp),
    titleSmall = tabularStyle(MuktaFontFamily, FontWeight.Medium, size = 14.sp, lineHeight = 20.sp),
    bodyLarge = tabularStyle(MuktaFontFamily, FontWeight.Normal, size = 16.sp, lineHeight = 24.sp),
    bodyMedium = tabularStyle(MuktaFontFamily, FontWeight.Normal, size = 14.sp, lineHeight = 20.sp),
    bodySmall = tabularStyle(
        fontFamily = MuktaFontFamily,
        weight = FontWeight.Normal,
        size = 12.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.1.sp
    ),
    labelLarge = tabularStyle(MuktaFontFamily, FontWeight.Medium, size = 14.sp, lineHeight = 20.sp),
    labelMedium = tabularStyle(
        fontFamily = MuktaFontFamily,
        weight = FontWeight.Medium,
        size = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.1.sp
    ),
    labelSmall = tabularStyle(
        fontFamily = MuktaFontFamily,
        weight = FontWeight.Normal,
        size = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.15.sp
    )
)

internal val TithimalaLatinTypography = buildTithimalaTypography(MuktaFontFamily)
internal val TithimalaDevanagariTypography = buildTithimalaTypography(NotoSerifDevanagariFontFamily)
