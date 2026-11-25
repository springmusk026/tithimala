package app.tithimala.feature.calendar

import app.tithimala.core.calendarengine.BsDateFormatter
import app.tithimala.core.model.AppLanguage
import app.tithimala.core.model.BsYearMonth
import app.tithimala.core.model.NumeralScript
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

private const val NEPALI_LANGUAGE_CODE = "ne"

internal object CalendarDateFormatting {

    fun resolveEffectiveLanguage(language: AppLanguage, systemLocale: Locale): AppLanguage = when (language) {
        AppLanguage.ENGLISH -> AppLanguage.ENGLISH
        AppLanguage.NEPALI -> AppLanguage.NEPALI
        AppLanguage.SYSTEM -> {
            if (systemLocale.language.equals(NEPALI_LANGUAGE_CODE, ignoreCase = true)) {
                AppLanguage.NEPALI
            } else {
                AppLanguage.ENGLISH
            }
        }
    }

    fun resolveLocale(effectiveLanguage: AppLanguage): Locale = when (effectiveLanguage) {
        AppLanguage.NEPALI -> Locale.forLanguageTag(NEPALI_LANGUAGE_CODE)
        AppLanguage.ENGLISH, AppLanguage.SYSTEM -> Locale.ENGLISH
    }

    fun formatBsMonthYear(yearMonth: BsYearMonth, language: AppLanguage, script: NumeralScript): String {
        val monthName = BsDateFormatter.monthName(yearMonth.month, language)
        val yearLabel = BsDateFormatter.formatNumber(yearMonth.year, script)
        return "$monthName $yearLabel"
    }

    fun formatAdFullDate(date: LocalDate, locale: Locale, script: NumeralScript): String {
        val dayLabel = BsDateFormatter.formatNumber(date.dayOfMonth, script)
        val monthLabel = date.month.getDisplayName(TextStyle.FULL, locale)
        val yearLabel = BsDateFormatter.formatNumber(date.year, script)
        return "$dayLabel $monthLabel $yearLabel"
    }

    fun formatAdShortDate(date: LocalDate, locale: Locale, script: NumeralScript): String {
        val monthLabel = date.month.getDisplayName(TextStyle.SHORT, locale)
        val dayLabel = BsDateFormatter.formatNumber(date.dayOfMonth, script)
        return "$monthLabel $dayLabel"
    }

    fun formatAdMonthRange(start: LocalDate, end: LocalDate, locale: Locale, script: NumeralScript): String {
        if (locale.language == Locale.ENGLISH.language) {
            return BsDateFormatter.formatDigits(BsDateFormatter.formatAdMonthRange(start, end), script)
        }
        val startMonth = start.month.getDisplayName(TextStyle.SHORT, locale)
        val endMonth = end.month.getDisplayName(TextStyle.SHORT, locale)
        val startYear = BsDateFormatter.formatNumber(start.year, script)
        val endYear = BsDateFormatter.formatNumber(end.year, script)
        return when {
            start.year == end.year && start.month == end.month -> "$startMonth $startYear"
            start.year == end.year -> "$startMonth/$endMonth $startYear"
            else -> "$startMonth $startYear/$endMonth $endYear"
        }
    }
}
