package app.tithimala.core.calendarengine

import app.tithimala.core.model.BsDate
import app.tithimala.core.model.BsMonthGrid
import app.tithimala.core.model.BsYearMonth
import app.tithimala.core.model.CalendarDay
import app.tithimala.core.model.PatroDayInfo
import app.tithimala.core.model.WeekStartDay
import java.time.DayOfWeek

/**
 * Builds 35- or 42-cell week-aligned [BsMonthGrid] snapshots from [CalendarTableIndex] and [PatroIndex].
 *
 * At the `1901-01` and `2199-12` dataset edges, leading/trailing overflow cells stay aligned to the
 * 7-column week structure with synthesized adjacent-edge [BsDate] placeholders and exact Gregorian dates.
 */
internal class MonthGridBuilder(private val tableIndex: CalendarTableIndex, private val patroIndex: PatroIndex) {
    fun build(year: Int, month: Int, todayBs: BsDate, weekStart: WeekStartDay): BsMonthGrid {
        val monthLength = tableIndex.daysInMonth(year, month)
        val firstDay = BsDate(year = year, month = month, day = BsDate.MIN_DAY)
        val lastDay = BsDate(year = year, month = month, day = monthLength)
        val firstSerial = tableIndex.bsToSerial(firstDay)
        val firstDayOfWeek = tableIndex.dayOfWeekForSerial(firstSerial)

        val leadingCount = when (weekStart) {
            WeekStartDay.SUNDAY -> firstDayOfWeek.value % DAYS_PER_WEEK
            WeekStartDay.MONDAY -> firstDayOfWeek.value - 1
        }
        val totalCells = ((leadingCount + monthLength + DAYS_PER_WEEK - 1) / DAYS_PER_WEEK) * DAYS_PER_WEEK
        val gridStartSerial = firstSerial - leadingCount

        val days = List(totalCells) { offset ->
            val serial = gridStartSerial + offset
            buildDayCell(serial = serial, targetYear = year, targetMonth = month, todayBs = todayBs)
        }

        val adStartDate = tableIndex.adDateForSerial(firstSerial)
        val adEndDate = tableIndex.adDateForSerial(tableIndex.bsToSerial(lastDay))
        return BsMonthGrid(
            yearMonth = BsYearMonth(year = year, month = month),
            days = days,
            adMonthRangeLabel = BsDateFormatter.formatAdMonthRange(adStartDate, adEndDate),
            adStartDate = adStartDate,
            adEndDate = adEndDate
        )
    }

    private fun buildDayCell(serial: Int, targetYear: Int, targetMonth: Int, todayBs: BsDate): CalendarDay {
        val adDate = tableIndex.adDateForSerial(serial)
        val dayOfWeek = tableIndex.dayOfWeekForSerial(serial)
        val bsDate = when {
            serial < 0 -> BsDate(
                year = tableIndex.minYear - 1,
                month = BsDate.MAX_MONTH,
                day = EDGE_FALLBACK_MONTH_LENGTH + serial + 1
            )
            serial > tableIndex.maxSerial -> BsDate(
                year = tableIndex.maxYear + 1,
                month = BsDate.MIN_MONTH,
                day = serial - tableIndex.maxSerial
            )
            else -> tableIndex.serialToBs(serial)
        }
        val isCurrentMonth = bsDate.year == targetYear && bsDate.month == targetMonth
        val patro = if (serial in 0..tableIndex.maxSerial) patroIndex.info(bsDate) else PatroDayInfo.EMPTY
        return CalendarDay(
            bsDate = bsDate,
            adDate = adDate,
            dayOfWeek = dayOfWeek,
            isCurrentMonth = isCurrentMonth,
            isToday = bsDate == todayBs,
            isSaturday = dayOfWeek == DayOfWeek.SATURDAY,
            patro = patro
        )
    }

    private companion object {
        const val DAYS_PER_WEEK = 7
        const val EDGE_FALLBACK_MONTH_LENGTH = 30
    }
}
