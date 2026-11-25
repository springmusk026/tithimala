package app.tithimala.core.designsystem.preview

import android.content.res.Configuration
import androidx.compose.ui.tooling.preview.Preview

/**
 * Multi-preview suite required across Tithimala components:
 * light/dark themes, English/Nepali locales, and 200% font scale.
 */
@Preview(
    name = "Light - English",
    locale = "en",
    uiMode = Configuration.UI_MODE_NIGHT_NO,
    showBackground = true
)
@Preview(
    name = "Dark - English",
    locale = "en",
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    showBackground = true
)
@Preview(
    name = "Light - Nepali",
    locale = "ne",
    uiMode = Configuration.UI_MODE_NIGHT_NO,
    showBackground = true
)
@Preview(
    name = "Dark - Nepali",
    locale = "ne",
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    showBackground = true
)
@Preview(
    name = "Light - English 200% Font",
    locale = "en",
    fontScale = 2.0f,
    uiMode = Configuration.UI_MODE_NIGHT_NO,
    showBackground = true
)
annotation class TithimalaThemePreviews
