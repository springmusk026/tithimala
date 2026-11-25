package app.tithimala.feature.converter

import app.tithimala.core.calendarengine.BikramSambatEngine
import app.tithimala.core.calendarengine.BsDateFormatter
import app.tithimala.core.model.AppLanguage
import app.tithimala.core.model.BsDate
import app.tithimala.core.model.MoonPhase
import app.tithimala.core.model.NumeralScript
import app.tithimala.core.model.TithiSource
import app.tithimala.core.model.UserPreferences
import app.tithimala.core.ui.calendar.LunarPhaseVisual
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.Month
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

internal data class ConverterInputDraft(
    val direction: ConversionDirection,
    val yearInput: String,
    val monthIndex: Int,
    val dayInput: String
)

private data class EvaluationContext(
    val draft: ConverterInputDraft,
    val engine: BikramSambatEngine,
    val preferences: UserPreferences,
    val systemLocale: Locale
)

private data class DateBounds(
    val month: Int,
    val parsedYear: Int?,
    val parsedDay: Int?,
    val minYear: Int,
    val maxYear: Int,
    val maxDay: Int
)

internal object ConverterEvaluator {
    const val MIN_MONTH = BsDate.MIN_MONTH
    const val MAX_MONTH = BsDate.MAX_MONTH
    const val MIN_DAY = BsDate.MIN_DAY
    const val MAX_YEAR_LENGTH = 4
    const val MAX_DAY_LENGTH = 2

    private const val DEVANAGARI_ZERO = '\u0966'
    private const val DEVANAGARI_NINE = '\u096F'
    private const val NEPALI_LANGUAGE_CODE = "ne"
    private val NEPALI_LOCALE: Locale = Locale.forLanguageTag(NEPALI_LANGUAGE_CODE)

    fun sanitizeDigits(input: String, maxLength: Int): String = buildString(maxLength) {
        for (ch in input) {
            if (length >= maxLength) break
            if (ch in '0'..'9' || ch in DEVANAGARI_ZERO..DEVANAGARI_NINE) {
                append(ch)
            }
        }
    }

    fun parseDigits(input: String): Int? {
        var allDigits = input.isNotBlank()
        val normalized = buildString(input.length) {
            for (ch in input) {
                when (ch) {
                    in '0'..'9' -> append(ch)
                    in DEVANAGARI_ZERO..DEVANAGARI_NINE -> append('0' + (ch - DEVANAGARI_ZERO))
                    else -> allDigits = false
                }
            }
        }
        return if (allDigits) normalized.toIntOrNull() else null
    }

    fun evaluate(
        draft: ConverterInputDraft,
        engine: BikramSambatEngine,
        preferences: UserPreferences,
        systemLocale: Locale = Locale.getDefault()
    ): ConverterUiState {
        val context = EvaluationContext(
            draft = draft,
            engine = engine,
            preferences = preferences,
            systemLocale = systemLocale
        )
        return when (draft.direction) {
            ConversionDirection.BS_TO_AD -> evaluateBsToAd(context)
            ConversionDirection.AD_TO_BS -> evaluateAdToBs(context)
        }
    }

    private fun evaluateBsToAd(context: EvaluationContext): ConverterUiState {
        val month = context.draft.monthIndex.coerceIn(MIN_MONTH, MAX_MONTH)
        val parsedYear = parseDigits(context.draft.yearInput)
        val parsedDay = parseDigits(context.draft.dayInput)
        val maxDay = if (parsedYear != null && context.engine.isSupportedYear(parsedYear)) {
            context.engine.daysInMonth(parsedYear, month)
        } else {
            BsDate.MAX_DAY
        }
        val bounds = DateBounds(
            month = month,
            parsedYear = parsedYear,
            parsedDay = parsedDay,
            minYear = context.engine.minBsDate.year,
            maxYear = context.engine.maxBsDate.year,
            maxDay = maxDay
        )
        val validationError = validateYearAndDay(context.draft, bounds)
        val result = if (validationError == null && parsedYear != null && parsedDay != null) {
            val bsDate = BsDate(year = parsedYear, month = month, day = parsedDay)
            buildResult(
                context = context,
                bsDate = bsDate,
                adDate = context.engine.bsToAd(bsDate),
                dayOfWeek = context.engine.dayOfWeek(bsDate)
            )
        } else {
            null
        }
        return buildState(context, bounds, validationError, result)
    }

    private fun evaluateAdToBs(context: EvaluationContext): ConverterUiState {
        val month = context.draft.monthIndex.coerceIn(MIN_MONTH, MAX_MONTH)
        val parsedYear = parseDigits(context.draft.yearInput)
        val parsedDay = parseDigits(context.draft.dayInput)
        val minYear = context.engine.minAdDate.year
        val maxYear = context.engine.maxAdDate.year
        val maxDay = if (parsedYear != null && parsedYear in minYear..maxYear) {
            YearMonth.of(parsedYear, month).lengthOfMonth()
        } else {
            Month.of(month).maxLength()
        }
        val bounds = DateBounds(
            month = month,
            parsedYear = parsedYear,
            parsedDay = parsedDay,
            minYear = minYear,
            maxYear = maxYear,
            maxDay = maxDay
        )
        val baseError = validateYearAndDay(context.draft, bounds)
        val (validationError, result) = if (baseError == null && parsedYear != null && parsedDay != null) {
            resolveAdDate(context, LocalDate.of(parsedYear, month, parsedDay))
        } else {
            baseError to null
        }
        return buildState(context, bounds, validationError, result)
    }

    private fun resolveAdDate(
        context: EvaluationContext,
        adDate: LocalDate
    ): Pair<ConverterValidationError?, ConvertedDateResult?> = when {
        adDate.isBefore(context.engine.minAdDate) -> {
            val label = formatAdDate(context.engine.minAdDate, context.preferences, context.systemLocale)
            ConverterValidationError.DateBeforeSupportedRange(label) to null
        }
        adDate.isAfter(context.engine.maxAdDate) -> {
            val label = formatAdDate(context.engine.maxAdDate, context.preferences, context.systemLocale)
            ConverterValidationError.DateAfterSupportedRange(label) to null
        }
        else -> {
            val bsDate = context.engine.adToBs(adDate)
            null to buildResult(context, bsDate, adDate, adDate.dayOfWeek)
        }
    }

    private fun validateYearAndDay(draft: ConverterInputDraft, bounds: DateBounds): ConverterValidationError? = when {
        draft.yearInput.isBlank() && draft.dayInput.isBlank() -> ConverterValidationError.EmptyInput
        draft.yearInput.isBlank() -> ConverterValidationError.EmptyYear
        bounds.parsedYear == null || bounds.parsedYear !in bounds.minYear..bounds.maxYear ->
            ConverterValidationError.YearOutOfRange(minYear = bounds.minYear, maxYear = bounds.maxYear)
        draft.dayInput.isBlank() -> ConverterValidationError.EmptyDay(maxDay = bounds.maxDay)
        bounds.parsedDay == null || bounds.parsedDay !in MIN_DAY..bounds.maxDay ->
            ConverterValidationError.InvalidDayForMonth(
                month = bounds.month,
                year = bounds.parsedYear,
                maxDay = bounds.maxDay
            )
        else -> null
    }

    private fun buildState(
        context: EvaluationContext,
        bounds: DateBounds,
        validationError: ConverterValidationError?,
        result: ConvertedDateResult?
    ): ConverterUiState {
        val patroYears = context.engine.datasetMetadata.patroYears
        return ConverterUiState(
            direction = context.draft.direction,
            yearInput = context.draft.yearInput,
            monthIndex = bounds.month,
            dayInput = context.draft.dayInput,
            maxDayForCurrentMonth = bounds.maxDay,
            minSupportedYear = bounds.minYear,
            maxSupportedYear = bounds.maxYear,
            patroFirstYear = patroYears.minOrNull(),
            patroLastYear = patroYears.maxOrNull(),
            numeralScript = context.preferences.numeralScript,
            canDecrementDay = bounds.parsedDay == null || bounds.parsedDay > MIN_DAY,
            canIncrementDay = bounds.parsedDay == null || bounds.parsedDay < bounds.maxDay,
            validationError = validationError,
            result = result
        )
    }

    private fun buildResult(
        context: EvaluationContext,
        bsDate: BsDate,
        adDate: LocalDate,
        dayOfWeek: DayOfWeek
    ): ConvertedDateResult {
        val preferences = context.preferences
        val systemLocale = context.systemLocale
        val resolvedLanguage = when (preferences.language) {
            AppLanguage.NEPALI -> AppLanguage.NEPALI
            AppLanguage.ENGLISH -> AppLanguage.ENGLISH
            AppLanguage.SYSTEM -> if (systemLocale.language.equals(NEPALI_LANGUAGE_CODE, ignoreCase = true)) {
                AppLanguage.NEPALI
            } else {
                AppLanguage.ENGLISH
            }
        }
        val bsFormatted = BsDateFormatter.formatLongDate(
            date = bsDate,
            language = resolvedLanguage,
            script = preferences.numeralScript,
            zeroPadDay = false
        )
        val adFormatted = formatAdDate(adDate, preferences, systemLocale)
        val weekdayText = BsDateFormatter.weekdayName(dayOfWeek).localized(preferences.language, systemLocale)
        val patro = context.engine.patroInfo(bsDate)
        val primaryDateText = when (context.draft.direction) {
            ConversionDirection.BS_TO_AD -> adFormatted
            ConversionDirection.AD_TO_BS -> bsFormatted
        }
        val secondaryDateText = when (context.draft.direction) {
            ConversionDirection.BS_TO_AD -> bsFormatted
            ConversionDirection.AD_TO_BS -> adFormatted
        }
        return ConvertedDateResult(
            bsDate = bsDate,
            adDate = adDate,
            primaryDateText = primaryDateText,
            secondaryDateText = secondaryDateText,
            weekdayText = weekdayText,
            tithiLabel = patro.tithi?.localized(preferences.language, systemLocale),
            lunarPhase = context.engine.inferMoonPhase(patro.tithi)?.toLunarPhaseVisual(),
            isEstimatedTithi = patro.tithiSource == TithiSource.ESTIMATED,
            isPublicHoliday = patro.isPublicHoliday,
            primaryEvent = patro.events.firstOrNull()?.localized(preferences.language, systemLocale),
            copyText = "$weekdayText, $primaryDateText ($secondaryDateText)"
        )
    }

    private fun formatAdDate(adDate: LocalDate, preferences: UserPreferences, systemLocale: Locale): String {
        val isNepali = preferences.language == AppLanguage.NEPALI ||
            (preferences.language == AppLanguage.SYSTEM && systemLocale.language.equals(NEPALI_LANGUAGE_CODE, true))
        val locale = if (isNepali) NEPALI_LOCALE else Locale.ENGLISH
        val script = if (isNepali) preferences.numeralScript else NumeralScript.LATIN
        val dayText = BsDateFormatter.formatNumber(adDate.dayOfMonth, script)
        val monthName = adDate.month.getDisplayName(TextStyle.FULL_STANDALONE, locale)
        val yearText = BsDateFormatter.formatNumber(adDate.year, script)
        return "$dayText $monthName $yearText"
    }
}

private fun MoonPhase.toLunarPhaseVisual(): LunarPhaseVisual = when (this) {
    MoonPhase.NEW_MOON -> LunarPhaseVisual.NEW_MOON
    MoonPhase.WAXING_CRESCENT -> LunarPhaseVisual.WAXING_CRESCENT
    MoonPhase.FIRST_QUARTER -> LunarPhaseVisual.FIRST_QUARTER
    MoonPhase.WAXING_GIBBOUS -> LunarPhaseVisual.WAXING_GIBBOUS
    MoonPhase.FULL_MOON -> LunarPhaseVisual.FULL_MOON
    MoonPhase.WANING_GIBBOUS -> LunarPhaseVisual.WANING_GIBBOUS
    MoonPhase.LAST_QUARTER -> LunarPhaseVisual.LAST_QUARTER
    MoonPhase.WANING_CRESCENT -> LunarPhaseVisual.WANING_CRESCENT
}
