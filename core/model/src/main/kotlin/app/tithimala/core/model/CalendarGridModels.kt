package app.tithimala.core.model

import java.time.DayOfWeek
import java.time.LocalDate

/**
 * A single day cell rendered in a Bikram Sambat calendar month grid or day detail view.
 */
data class CalendarDay(
    val bsDate: BsDate,
    val adDate: LocalDate,
    val dayOfWeek: DayOfWeek,
    val isCurrentMonth: Boolean,
    val isToday: Boolean,
    val isSaturday: Boolean = dayOfWeek == DayOfWeek.SATURDAY,
    val patro: PatroDayInfo
) {
    val adYear: Int get() = adDate.year
    val adMonth: Int get() = adDate.monthValue
    val adDay: Int get() = adDate.dayOfMonth
    val dayOfWeekValue: Int get() = dayOfWeek.value
}

/**
 * Complete month grid for a Bikram Sambat month, aligned to full weeks.
 */
data class BsMonthGrid(
    val yearMonth: BsYearMonth,
    val days: List<CalendarDay>,
    val adMonthRangeLabel: String,
    val adStartDate: LocalDate,
    val adEndDate: LocalDate
)
