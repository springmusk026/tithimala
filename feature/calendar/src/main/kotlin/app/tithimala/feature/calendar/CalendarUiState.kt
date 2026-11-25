package app.tithimala.feature.calendar

import androidx.compose.runtime.Immutable
import app.tithimala.core.model.AppLanguage
import app.tithimala.core.model.BsDate
import app.tithimala.core.model.BsYearMonth
import app.tithimala.core.model.NumeralScript
import app.tithimala.core.model.WeekStartDay
import app.tithimala.core.ui.calendar.MonthGridUiModel
import app.tithimala.core.ui.calendar.TodayHeroUiModel

@Immutable
internal data class FestivalSummaryUiModel(
    val bsDate: BsDate,
    val bsDayText: String,
    val bsMonthText: String,
    val adShortDateText: String,
    val primaryTitle: String?,
    val additionalEventsText: String?,
    val isPublicHoliday: Boolean,
    val isToday: Boolean
)

@Immutable
internal data class CalendarUiState(
    val todayHero: TodayHeroUiModel,
    val todayBs: BsDate,
    val currentYearMonth: BsYearMonth,
    val selectedDate: BsDate,
    val minYearMonth: BsYearMonth,
    val maxYearMonth: BsYearMonth,
    val monthTitle: String,
    val adRangeSubtitle: String,
    val isShowingTodayMonth: Boolean,
    val monthGrid: MonthGridUiModel,
    val visiblePages: Map<BsYearMonth, MonthGridUiModel>,
    val upcomingFestivals: List<FestivalSummaryUiModel>,
    val numeralScript: NumeralScript,
    val language: AppLanguage,
    val weekStartDay: WeekStartDay
) {
    val pageCount: Int
        get() = (monthOffset(minYearMonth, maxYearMonth) + 1).coerceAtLeast(1)

    val canGoPrevious: Boolean
        get() = currentYearMonth > minYearMonth

    val canGoNext: Boolean
        get() = currentYearMonth < maxYearMonth

    fun pageIndexFor(yearMonth: BsYearMonth): Int = monthOffset(minYearMonth, yearMonth).coerceIn(0, pageCount - 1)

    fun yearMonthForPage(pageIndex: Int): BsYearMonth = minYearMonth.plusMonths(pageIndex.coerceIn(0, pageCount - 1))

    private companion object {
        const val MONTHS_PER_YEAR = 12

        fun monthOffset(from: BsYearMonth, to: BsYearMonth): Int =
            (to.year - from.year) * MONTHS_PER_YEAR + (to.month - from.month)
    }
}

internal sealed interface CalendarAction {
    data class SelectMonth(val yearMonth: BsYearMonth) : CalendarAction
    data object PreviousMonth : CalendarAction
    data object NextMonth : CalendarAction
    data object JumpToToday : CalendarAction
    data class SelectDay(val bsDate: BsDate) : CalendarAction
    data class OpenDayDetail(val bsDate: BsDate) : CalendarAction
}

internal sealed interface CalendarEffect {
    data class ScrollToMonth(val yearMonth: BsYearMonth) : CalendarEffect
    data class NavigateToDay(val bsDate: BsDate) : CalendarEffect
}
