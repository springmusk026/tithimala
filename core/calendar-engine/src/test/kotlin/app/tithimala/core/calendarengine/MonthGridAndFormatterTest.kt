package app.tithimala.core.calendarengine

import app.tithimala.core.model.AppLanguage
import app.tithimala.core.model.BsDate
import app.tithimala.core.model.BsMonthGrid
import app.tithimala.core.model.BsYearMonth
import app.tithimala.core.model.NumeralScript
import app.tithimala.core.model.WeekStartDay
import java.time.DayOfWeek
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.Test

class MonthGridAndFormatterTest {

    private val engine = BikramSambatEngine.createBundled()

    @Test
    fun monthGridSupportsSundayAndMondayWeekStartsAcrossMonthLengths() {
        // 32-day month: Ashad 2083
        val ashad32Sunday = engine.buildMonthGrid(
            year = 2083,
            month = 3,
            todayBs = BsDate(2083, 3, 15),
            weekStart = WeekStartDay.SUNDAY
        )
        assertValidGridInvariants(ashad32Sunday, WeekStartDay.SUNDAY, expectedCurrentMonthDays = 32)
        assertEquals(BsYearMonth(2083, 3), ashad32Sunday.yearMonth)
        assertEquals(1, ashad32Sunday.days.count { it.isToday })
        assertEquals(BsDate(2083, 3, 15), ashad32Sunday.days.single { it.isToday }.bsDate)

        val ashad32Monday = engine.buildMonthGrid(
            year = 2083,
            month = 3,
            todayBs = BsDate(2083, 3, 15),
            weekStart = WeekStartDay.MONDAY
        )
        assertValidGridInvariants(ashad32Monday, WeekStartDay.MONDAY, expectedCurrentMonthDays = 32)

        // 29-day month: Mangsir 2083
        val mangsir29 = engine.buildMonthGrid(
            year = 2083,
            month = 8,
            todayBs = BsDate(2083, 8, 10),
            weekStart = WeekStartDay.SUNDAY
        )
        assertValidGridInvariants(mangsir29, WeekStartDay.SUNDAY, expectedCurrentMonthDays = 29)
        assertEquals(35, mangsir29.days.size)
    }

    @Test
    fun monthGridHandlesYearBoundariesAndAdYearSpans() {
        // Baishakh 2081 starts on Saturday 2024-04-13 (6 leading days from Chaitra 2080 + 31 days -> 42 cells)
        val baishakh2081 = engine.buildMonthGrid(
            year = 2081,
            month = 1,
            todayBs = BsDate(2081, 1, 1),
            weekStart = WeekStartDay.SUNDAY
        )
        assertValidGridInvariants(baishakh2081, WeekStartDay.SUNDAY, expectedCurrentMonthDays = 31)
        assertEquals(42, baishakh2081.days.size)
        assertEquals("Apr/May 2024", baishakh2081.adMonthRangeLabel)
        assertEquals(LocalDate.of(2024, 4, 13), baishakh2081.adStartDate)
        assertEquals(LocalDate.of(2024, 5, 13), baishakh2081.adEndDate)
        assertEquals(2080, baishakh2081.days.first().bsDate.year)
        assertEquals(12, baishakh2081.days.first().bsDate.month)
        assertFalse(baishakh2081.days.first().isCurrentMonth)

        val newYearCell = baishakh2081.days.single { it.bsDate == BsDate(2081, 1, 1) }
        assertTrue(newYearCell.isToday)
        assertTrue(newYearCell.isCurrentMonth)
        assertTrue(newYearCell.isSaturday)
        assertTrue(newYearCell.patro.isPublicHoliday)
        assertEquals("Panchami", newYearCell.patro.tithi?.en)

        // Poush 2081 has 29 days and spans Dec 2024 / Jan 2025
        val poush2081 = engine.buildMonthGrid(
            year = 2081,
            month = 9,
            todayBs = BsDate(2081, 1, 1),
            weekStart = WeekStartDay.MONDAY
        )
        assertValidGridInvariants(poush2081, WeekStartDay.MONDAY, expectedCurrentMonthDays = 29)
        assertEquals("Dec 2024/Jan 2025", poush2081.adMonthRangeLabel)
        assertEquals(0, poush2081.days.count { it.isToday })
    }

    @Test
    fun monthGridHandlesDatasetStartAndEndEdgesGracefully() {
        val startSunday = engine.buildMonthGrid(1901, 1, BsDate(1901, 1, 1), WeekStartDay.SUNDAY)
        assertValidGridInvariants(startSunday, WeekStartDay.SUNDAY, expectedCurrentMonthDays = 31)
        assertEquals(LocalDate.of(1844, 4, 11), startSunday.adStartDate)
        assertEquals(LocalDate.of(1844, 5, 11), startSunday.adEndDate)
        assertEquals("Apr/May 1844", startSunday.adMonthRangeLabel)

        val startMonday = engine.buildMonthGrid(1901, 1, BsDate(1901, 1, 1), WeekStartDay.MONDAY)
        assertValidGridInvariants(startMonday, WeekStartDay.MONDAY, expectedCurrentMonthDays = 31)

        val endSunday = engine.buildMonthGrid(2199, 12, BsDate(2199, 12, 30), WeekStartDay.SUNDAY)
        assertValidGridInvariants(endSunday, WeekStartDay.SUNDAY, expectedCurrentMonthDays = 30)
        assertEquals(engine.maxAdDate, endSunday.adEndDate)

        val endMonday = engine.buildMonthGrid(2199, 12, BsDate(2199, 12, 30), WeekStartDay.MONDAY)
        assertValidGridInvariants(endMonday, WeekStartDay.MONDAY, expectedCurrentMonthDays = 30)
        assertEquals(42, endMonday.days.size)
    }

    @Test
    fun formatterProvidesCanonicalMonthsAndWeekdays() {
        val expectedEnMonths = listOf(
            "Baishakh",
            "Jestha",
            "Ashad",
            "Shrawan",
            "Bhadra",
            "Ashoj",
            "Kartik",
            "Mangsir",
            "Poush",
            "Magh",
            "Falgun",
            "Chaitra"
        )
        val expectedNeMonths = listOf(
            "बैशाख",
            "जेठ",
            "असार",
            "साउन",
            "भदौ",
            "असोज",
            "कार्तिक",
            "मंसिर",
            "पुष",
            "माघ",
            "फागुन",
            "चैत"
        )

        for (month in 1..12) {
            assertEquals(expectedEnMonths[month - 1], BsDateFormatter.monthName(month, AppLanguage.ENGLISH))
            assertEquals(expectedNeMonths[month - 1], BsDateFormatter.monthName(month, AppLanguage.NEPALI))
        }
        assertFailsWith<IllegalArgumentException> { BsDateFormatter.monthName(0) }
        assertFailsWith<IllegalArgumentException> { BsDateFormatter.monthName(13) }

        assertEquals(DayOfWeek.SUNDAY, BsDateFormatter.weekdaysInOrder(WeekStartDay.SUNDAY).first())
        assertEquals(DayOfWeek.SATURDAY, BsDateFormatter.weekdaysInOrder(WeekStartDay.SUNDAY).last())
        assertEquals(DayOfWeek.MONDAY, BsDateFormatter.weekdaysInOrder(WeekStartDay.MONDAY).first())
        assertEquals(DayOfWeek.SUNDAY, BsDateFormatter.weekdaysInOrder(WeekStartDay.MONDAY).last())

        assertEquals("शनिवार", BsDateFormatter.weekdayName(DayOfWeek.SATURDAY).np)
        assertEquals("Saturday", BsDateFormatter.weekdayName(DayOfWeek.SATURDAY).en)
        assertEquals("शनि", BsDateFormatter.weekdayShortName(DayOfWeek.SATURDAY).np)
        assertEquals("Sat", BsDateFormatter.weekdayShortName(DayOfWeek.SATURDAY).en)
    }

    @Test
    fun formatterFormatsNumeralsLongDatesAndAdMonthRanges() {
        assertEquals("२०८३", BsDateFormatter.formatNumber(2083, NumeralScript.DEVANAGARI))
        assertEquals("2083", BsDateFormatter.formatNumber(2083, NumeralScript.LATIN))
        assertEquals("२०८३-०१-०५", BsDateFormatter.formatDigits("2083-01-05", NumeralScript.DEVANAGARI))
        assertEquals("2083-01-05", BsDateFormatter.formatDigits("2083-01-05", NumeralScript.LATIN))

        assertEquals(
            "Baishakh 5, 2083",
            BsDateFormatter.formatLongDate(
                date = BsDate(2083, 1, 5),
                language = AppLanguage.ENGLISH,
                script = NumeralScript.LATIN,
                zeroPadDay = false
            )
        )
        assertEquals(
            "Apr 2026",
            BsDateFormatter.formatAdMonthRange(LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 30))
        )
    }

    private fun assertValidGridInvariants(grid: BsMonthGrid, weekStart: WeekStartDay, expectedCurrentMonthDays: Int) {
        assertTrue(grid.days.size == 35 || grid.days.size == 42, "Grid size must be 35 or 42, got ${grid.days.size}")
        val expectedOrder = BsDateFormatter.weekdaysInOrder(weekStart)
        assertEquals(expectedOrder.first(), grid.days.first().dayOfWeek)
        assertEquals(expectedOrder.last(), grid.days.last().dayOfWeek)
        assertEquals(expectedCurrentMonthDays, grid.days.count { it.isCurrentMonth })
        assertEquals(grid.days.size, grid.days.map { it.bsDate }.distinct().size, "Duplicate BsDate keys in grid")

        for (index in grid.days.indices) {
            val cell = grid.days[index]
            assertEquals(expectedOrder[index % DAYS_PER_WEEK], cell.dayOfWeek)
            assertEquals(cell.dayOfWeek == DayOfWeek.SATURDAY, cell.isSaturday)
            if (index > 0) {
                assertEquals(grid.days[index - 1].adDate.plusDays(1), cell.adDate)
            }
        }
    }

    private companion object {
        const val DAYS_PER_WEEK = 7
    }
}
