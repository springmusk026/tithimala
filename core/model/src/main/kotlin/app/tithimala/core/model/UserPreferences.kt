package app.tithimala.core.model

import kotlinx.serialization.Serializable

@Serializable
enum class AppLanguage {
    SYSTEM,
    ENGLISH,
    NEPALI
}

@Serializable
enum class NumeralScript {
    DEVANAGARI,
    LATIN
}

@Serializable
enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK
}

@Serializable
enum class WeekStartDay {
    SUNDAY,
    MONDAY
}

@Serializable
data class UserPreferences(
    val language: AppLanguage = AppLanguage.SYSTEM,
    val numeralScript: NumeralScript = NumeralScript.DEVANAGARI,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val weekStartDay: WeekStartDay = WeekStartDay.SUNDAY,
    val backgroundSyncEnabled: Boolean = true
)
