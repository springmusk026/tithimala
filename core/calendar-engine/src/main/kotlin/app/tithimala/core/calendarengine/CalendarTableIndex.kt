package app.tithimala.core.calendarengine

import app.tithimala.core.calendarengine.data.BsCalendarTable
import app.tithimala.core.calendarengine.data.CalendarDataValidator
import app.tithimala.core.model.BsDate
import java.time.DayOfWeek
import java.time.LocalDate

/**
 * Precomputed O(1) index over a validated [BsCalendarTable] for serial, Gregorian, and weekday conversions.
 */
internal class CalendarTableIndex(table: BsCalendarTable) {
    val minYear: Int
    val maxYear: Int
    private val yearCount: Int
    private val monthLengthsByYear: Array<IntArray>
    private val monthStartOffsetInYear: Array<IntArray>
    private val yearStartSerial: IntArray

    val maxSerial: Int
    val minBsDate: BsDate
    val maxBsDate: BsDate
    val minAdDate: LocalDate
    val maxAdDate: LocalDate
    private val minAdEpochDay: Long
    private val minAdDayOfWeekZeroBased: Int

    init {
        CalendarDataValidator.validateBsCalendar(table)
        minYear = table.minYear
        maxYear = table.maxYear
        yearCount = maxYear - minYear + 1
        monthLengthsByYear = Array(yearCount) { IntArray(MONTHS_PER_YEAR) }
        monthStartOffsetInYear = Array(yearCount) { IntArray(MONTHS_PER_YEAR) }
        yearStartSerial = IntArray(yearCount + 1)

        var runningSerial = 0
        for (yearIndex in 0 until yearCount) {
            yearStartSerial[yearIndex] = runningSerial
            val year = minYear + yearIndex
            val lengths = table.monthLengths.getValue(year.toString())
            var monthOffset = 0
            for (monthIndex in 0 until MONTHS_PER_YEAR) {
                val monthDays = lengths[monthIndex]
                monthLengthsByYear[yearIndex][monthIndex] = monthDays
                monthStartOffsetInYear[yearIndex][monthIndex] = monthOffset
                monthOffset += monthDays
            }
            runningSerial += monthOffset
        }
        yearStartSerial[yearCount] = runningSerial
        maxSerial = runningSerial - 1

        minBsDate = BsDate(year = minYear, month = BsDate.MIN_MONTH, day = BsDate.MIN_DAY)
        maxBsDate = BsDate(
            year = maxYear,
            month = BsDate.MAX_MONTH,
            day = monthLengthsByYear[yearCount - 1][MONTHS_PER_YEAR - 1]
        )
        minAdDate = LocalDate.parse(table.epoch.ad)
        maxAdDate = minAdDate.plusDays(maxSerial.toLong())
        minAdEpochDay = minAdDate.toEpochDay()
        minAdDayOfWeekZeroBased = minAdDate.dayOfWeek.value - 1
    }

    fun isSupportedYear(year: Int): Boolean = year in minYear..maxYear

    fun daysInMonth(year: Int, month: Int): Int {
        require(isSupportedYear(year)) { "BS year $year is outside supported range $minYear..$maxYear" }
        require(month in BsDate.MIN_MONTH..BsDate.MAX_MONTH) {
            "BS month must be between ${BsDate.MIN_MONTH} and ${BsDate.MAX_MONTH}, got $month"
        }
        return monthLengthsByYear[year - minYear][month - 1]
    }

    fun isValidBsDate(year: Int, month: Int, day: Int): Boolean {
        if (!isSupportedYear(year) || month !in BsDate.MIN_MONTH..BsDate.MAX_MONTH) {
            return false
        }
        return day in BsDate.MIN_DAY..monthLengthsByYear[year - minYear][month - 1]
    }

    fun bsToSerial(bsDate: BsDate): Int {
        require(isValidBsDate(bsDate.year, bsDate.month, bsDate.day)) {
            "Invalid or out-of-range BS date: ${bsDate.toIsoString()} (supported $minBsDate..$maxBsDate)"
        }
        val yearIndex = bsDate.year - minYear
        return yearStartSerial[yearIndex] + monthStartOffsetInYear[yearIndex][bsDate.month - 1] + (bsDate.day - 1)
    }

    fun serialToBs(serial: Int): BsDate {
        require(serial in 0..maxSerial) {
            "Serial day $serial is outside supported range 0..$maxSerial"
        }
        var yearIndex = (serial / DAYS_PER_APPROX_YEAR).coerceIn(0, yearCount - 1)
        while (yearIndex + 1 < yearCount && yearStartSerial[yearIndex + 1] <= serial) {
            yearIndex++
        }
        while (yearIndex > 0 && yearStartSerial[yearIndex] > serial) {
            yearIndex--
        }

        val dayInYear = serial - yearStartSerial[yearIndex]
        val monthOffsets = monthStartOffsetInYear[yearIndex]
        var monthIndex = (dayInYear / DAYS_PER_APPROX_MONTH).coerceIn(0, MONTHS_PER_YEAR - 1)
        while (monthIndex + 1 < MONTHS_PER_YEAR && monthOffsets[monthIndex + 1] <= dayInYear) {
            monthIndex++
        }
        while (monthIndex > 0 && monthOffsets[monthIndex] > dayInYear) {
            monthIndex--
        }

        return BsDate(
            year = minYear + yearIndex,
            month = monthIndex + 1,
            day = dayInYear - monthOffsets[monthIndex] + 1
        )
    }

    fun adToBs(adDate: LocalDate): BsDate {
        val offset = adDate.toEpochDay() - minAdEpochDay
        require(offset in 0L..maxSerial.toLong()) {
            "AD date $adDate is outside supported range $minAdDate..$maxAdDate"
        }
        return serialToBs(offset.toInt())
    }

    fun dayOfYear(bsDate: BsDate): Int {
        require(isValidBsDate(bsDate.year, bsDate.month, bsDate.day)) {
            "Invalid or out-of-range BS date: ${bsDate.toIsoString()}"
        }
        return monthStartOffsetInYear[bsDate.year - minYear][bsDate.month - 1] + bsDate.day
    }

    fun adDateForSerial(serial: Int): LocalDate = minAdDate.plusDays(serial.toLong())

    fun dayOfWeekForSerial(serial: Int): DayOfWeek {
        val isoZeroBased = Math.floorMod(minAdDayOfWeekZeroBased + serial, DAYS_PER_WEEK)
        return DayOfWeek.of(isoZeroBased + 1)
    }

    private companion object {
        const val MONTHS_PER_YEAR = 12
        const val DAYS_PER_WEEK = 7
        const val DAYS_PER_APPROX_YEAR = 365
        const val DAYS_PER_APPROX_MONTH = 30
    }
}
