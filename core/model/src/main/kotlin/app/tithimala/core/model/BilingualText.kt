package app.tithimala.core.model

import java.util.Locale
import kotlinx.serialization.Serializable

/**
 * Paired Nepali and English label from the calendar dataset.
 */
@Serializable
data class BilingualText(val np: String, val en: String) {
    fun localized(language: AppLanguage): String = localized(language, Locale.getDefault())

    fun localized(language: AppLanguage, systemLocale: Locale): String = when (language) {
        AppLanguage.NEPALI -> np
        AppLanguage.ENGLISH -> en
        AppLanguage.SYSTEM -> if (systemLocale.language.equals(NEPALI_LANGUAGE_CODE, ignoreCase = true)) np else en
    }

    companion object {
        private const val NEPALI_LANGUAGE_CODE = "ne"
    }
}
