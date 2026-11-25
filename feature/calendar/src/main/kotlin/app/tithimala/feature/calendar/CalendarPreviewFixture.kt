package app.tithimala.feature.calendar

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import app.tithimala.core.calendarengine.BsDateFormatter
import app.tithimala.core.model.AppLanguage
import app.tithimala.core.model.BsDate
import app.tithimala.core.model.BsYearMonth
import app.tithimala.core.model.NumeralScript
import app.tithimala.core.model.WeekStartDay
import app.tithimala.core.ui.calendar.DayCellUiModel
import app.tithimala.core.ui.calendar.LunarPhaseVisual
import app.tithimala.core.ui.calendar.MonthGridUiModel
import app.tithimala.core.ui.calendar.TodayHeroUiModel
import java.time.DayOfWeek
import java.time.LocalDate
import java.util.Locale

private const val PREVIEW_BS_YEAR = 2083
private const val PREVIEW_BS_MONTH = 8
private const val PREVIEW_MANGSIR_DAYS = 29
private const val PREVIEW_KARTIK_DAYS = 30
private const val PREVIEW_LEADING_DAYS = 2
private const val PREVIEW_TOTAL_CELLS = 35
private const val PREVIEW_TODAY_DAY = 8
private const val PREVIEW_SELECTED_DAY = 18
private const val PREVIEW_SATURDAY_COLUMN = 6
private const val PREVIEW_DAYS_PER_WEEK = 7
private val PREVIEW_FIRST_CELL_AD = LocalDate.of(2026, 11, 15)
private val PREVIEW_MONTH_START_AD = LocalDate.of(2026, 11, 17)
private val PREVIEW_MONTH_END_AD = LocalDate.of(2026, 12, 15)
private val PREVIEW_HOLIDAY_DAYS = setOf(8, 17, 18)
private val PREVIEW_EVENT_DAYS = setOf(1, 4, 8, 15, 17, 18, 21, 28)
private val PREVIEW_REMINDER_DAYS = setOf(8, 14, 18, 25)

@Composable
internal fun previewMangsir2083UiState(isShowingTodayMonth: Boolean = true): CalendarUiState {
    val primaryLocale = LocalConfiguration.current.locales.get(0)
    val effectiveLanguage = CalendarDateFormatting.resolveEffectiveLanguage(
        language = AppLanguage.SYSTEM,
        systemLocale = primaryLocale
    )
    val effectiveLocale = CalendarDateFormatting.resolveLocale(effectiveLanguage)
    val script = if (effectiveLanguage == AppLanguage.NEPALI) NumeralScript.DEVANAGARI else NumeralScript.LATIN

    val mangsirYearMonth = BsYearMonth(year = PREVIEW_BS_YEAR, month = PREVIEW_BS_MONTH)
    val todayBs = BsDate(year = PREVIEW_BS_YEAR, month = PREVIEW_BS_MONTH, day = PREVIEW_TODAY_DAY)
    val selectedDate = BsDate(year = PREVIEW_BS_YEAR, month = PREVIEW_BS_MONTH, day = PREVIEW_SELECTED_DAY)
    val monthGrid = buildPreviewMangsirGrid(effectiveLanguage, effectiveLocale, script)
    val monthName = BsDateFormatter.monthName(PREVIEW_BS_MONTH, effectiveLanguage)

    return CalendarUiState(
        todayHero = buildPreviewTodayHero(mangsirYearMonth, effectiveLanguage, effectiveLocale, script),
        todayBs = todayBs,
        currentYearMonth = mangsirYearMonth,
        selectedDate = selectedDate,
        minYearMonth = BsYearMonth(year = 1901, month = 1),
        maxYearMonth = BsYearMonth(year = 2199, month = 12),
        monthTitle = CalendarDateFormatting.formatBsMonthYear(mangsirYearMonth, effectiveLanguage, script),
        adRangeSubtitle = CalendarDateFormatting.formatAdMonthRange(
            start = PREVIEW_MONTH_START_AD,
            end = PREVIEW_MONTH_END_AD,
            locale = effectiveLocale,
            script = script
        ),
        isShowingTodayMonth = isShowingTodayMonth,
        monthGrid = monthGrid,
        visiblePages = mapOf(mangsirYearMonth to monthGrid),
        upcomingFestivals = buildPreviewFestivals(monthName, effectiveLocale, script),
        numeralScript = script,
        language = effectiveLanguage,
        weekStartDay = WeekStartDay.SUNDAY
    )
}

@Composable
private fun buildPreviewTodayHero(
    mangsirYearMonth: BsYearMonth,
    effectiveLanguage: AppLanguage,
    effectiveLocale: Locale,
    script: NumeralScript
): TodayHeroUiModel = TodayHeroUiModel(
    bsDayNumber = BsDateFormatter.formatNumber(PREVIEW_TODAY_DAY, script),
    bsMonthYear = CalendarDateFormatting.formatBsMonthYear(mangsirYearMonth, effectiveLanguage, script),
    adDateLabel = CalendarDateFormatting.formatAdFullDate(
        date = LocalDate.of(2026, 11, 24),
        locale = effectiveLocale,
        script = script
    ),
    weekdayLabel = BsDateFormatter.weekdayName(DayOfWeek.TUESDAY).localized(effectiveLanguage),
    tithiLabel = stringResource(R.string.feature_calendar_preview_tithi_purnima),
    lunarPhase = LunarPhaseVisual.FULL_MOON,
    primaryFestival = stringResource(R.string.feature_calendar_preview_event_sakimna),
    isHoliday = true
)

private fun buildPreviewMangsirGrid(
    effectiveLanguage: AppLanguage,
    effectiveLocale: Locale,
    script: NumeralScript
): MonthGridUiModel {
    val weekdays = BsDateFormatter.weekdaysInOrder(WeekStartDay.SUNDAY)
    val headers = weekdays.map { BsDateFormatter.weekdayShortName(it).localized(effectiveLanguage) }
    val days = List(PREVIEW_TOTAL_CELLS) { index ->
        val adDate = PREVIEW_FIRST_CELL_AD.plusDays(index.toLong())
        val (bsDate, isCurrentMonth) = when {
            index < PREVIEW_LEADING_DAYS -> BsDate(
                year = PREVIEW_BS_YEAR,
                month = PREVIEW_BS_MONTH - 1,
                day = PREVIEW_KARTIK_DAYS - PREVIEW_LEADING_DAYS + index + 1
            ) to false
            index < PREVIEW_LEADING_DAYS + PREVIEW_MANGSIR_DAYS -> BsDate(
                year = PREVIEW_BS_YEAR,
                month = PREVIEW_BS_MONTH,
                day = index - PREVIEW_LEADING_DAYS + 1
            ) to true
            else -> BsDate(
                year = PREVIEW_BS_YEAR,
                month = PREVIEW_BS_MONTH + 1,
                day = index - PREVIEW_LEADING_DAYS - PREVIEW_MANGSIR_DAYS + 1
            ) to false
        }
        val bsFull = BsDateFormatter.formatLongDate(
            date = bsDate,
            language = effectiveLanguage,
            script = script,
            dayOfWeek = adDate.dayOfWeek,
            zeroPadDay = false
        )
        val adFull = CalendarDateFormatting.formatAdFullDate(adDate, effectiveLocale, script)
        DayCellUiModel(
            id = bsDate.toIsoString(),
            bsDayText = BsDateFormatter.formatNumber(bsDate.day, script),
            adDayText = BsDateFormatter.formatNumber(adDate.dayOfMonth, script),
            isCurrentMonth = isCurrentMonth,
            isToday = isCurrentMonth && bsDate.day == PREVIEW_TODAY_DAY,
            isSelected = isCurrentMonth && bsDate.day == PREVIEW_SELECTED_DAY,
            isSaturday = index % PREVIEW_DAYS_PER_WEEK == PREVIEW_SATURDAY_COLUMN,
            isPublicHoliday = isCurrentMonth && bsDate.day in PREVIEW_HOLIDAY_DAYS,
            hasEvents = isCurrentMonth && bsDate.day in PREVIEW_EVENT_DAYS,
            hasReminders = isCurrentMonth && bsDate.day in PREVIEW_REMINDER_DAYS,
            accessibilityDescription = "$bsFull, $adFull"
        )
    }
    return MonthGridUiModel(
        weekdayHeaders = headers,
        saturdayColumnIndex = PREVIEW_SATURDAY_COLUMN,
        days = days
    )
}

private data class PreviewFestivalSpec(
    val day: Int,
    val adDate: LocalDate,
    @StringRes val titleRes: Int,
    @StringRes val moreRes: Int,
    val isHoliday: Boolean = false,
    val isToday: Boolean = false
)

private val PREVIEW_FESTIVAL_SPECS = listOf(
    PreviewFestivalSpec(
        day = 1,
        adDate = LocalDate.of(2026, 11, 17),
        titleRes = R.string.feature_calendar_preview_event_vrischik,
        moreRes = R.string.feature_calendar_preview_event_vrischik_more
    ),
    PreviewFestivalSpec(
        day = PREVIEW_TODAY_DAY,
        adDate = LocalDate.of(2026, 11, 24),
        titleRes = R.string.feature_calendar_preview_event_sakimna,
        moreRes = R.string.feature_calendar_preview_event_guru_nanak,
        isHoliday = true,
        isToday = true
    ),
    PreviewFestivalSpec(
        day = PREVIEW_SELECTED_DAY,
        adDate = LocalDate.of(2026, 12, 4),
        titleRes = R.string.feature_calendar_preview_event_udhauli,
        moreRes = R.string.feature_calendar_preview_event_udhauli_more,
        isHoliday = true
    ),
    PreviewFestivalSpec(
        day = 21,
        adDate = LocalDate.of(2026, 12, 7),
        titleRes = R.string.feature_calendar_preview_event_bala,
        moreRes = R.string.feature_calendar_preview_event_bala_more
    ),
    PreviewFestivalSpec(
        day = 28,
        adDate = LocalDate.of(2026, 12, 14),
        titleRes = R.string.feature_calendar_preview_event_vivaha,
        moreRes = R.string.feature_calendar_preview_event_vivaha_more
    )
)

@Composable
private fun buildPreviewFestivals(
    monthName: String,
    effectiveLocale: Locale,
    script: NumeralScript
): List<FestivalSummaryUiModel> = PREVIEW_FESTIVAL_SPECS.map { spec ->
    FestivalSummaryUiModel(
        bsDate = BsDate(year = PREVIEW_BS_YEAR, month = PREVIEW_BS_MONTH, day = spec.day),
        bsDayText = BsDateFormatter.formatNumber(spec.day, script),
        bsMonthText = monthName,
        adShortDateText = CalendarDateFormatting.formatAdShortDate(spec.adDate, effectiveLocale, script),
        primaryTitle = stringResource(spec.titleRes),
        additionalEventsText = stringResource(spec.moreRes),
        isPublicHoliday = spec.isHoliday,
        isToday = spec.isToday
    )
}
