package app.tithimala.core.calendarengine

import app.tithimala.core.model.AppLanguage
import app.tithimala.core.model.BilingualText
import app.tithimala.core.model.BsDate
import app.tithimala.core.model.NumeralScript
import app.tithimala.core.model.WeekStartDay
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

/**
 * Canonical formatter for Bikram Sambat months, weekdays, dates, and localized numerals.
 *
 * All Devanagari digit formatting delegates exclusively to [DevanagariNumerals].
 */
object BsDateFormatter {
    private const val YEAR_WIDTH = 4
    private const val TWO_DIGIT_WIDTH = 2

    private val MONTH_NAMES: List<BilingualText> = listOf(
        BilingualText(np = "बैशाख", en = "Baishakh"),
        BilingualText(np = "जेठ", en = "Jestha"),
        BilingualText(np = "असार", en = "Ashad"),
        BilingualText(np = "साउन", en = "Shrawan"),
        BilingualText(np = "भदौ", en = "Bhadra"),
        BilingualText(np = "असोज", en = "Ashoj"),
        BilingualText(np = "कार्तिक", en = "Kartik"),
        BilingualText(np = "मंसिर", en = "Mangsir"),
        BilingualText(np = "पुष", en = "Poush"),
        BilingualText(np = "माघ", en = "Magh"),
        BilingualText(np = "फागुन", en = "Falgun"),
        BilingualText(np = "चैत", en = "Chaitra")
    )

    private val WEEKDAY_FULL_NAMES: Map<DayOfWeek, BilingualText> = mapOf(
        DayOfWeek.SUNDAY to BilingualText(np = "आइतवार", en = "Sunday"),
        DayOfWeek.MONDAY to BilingualText(np = "सोमवार", en = "Monday"),
        DayOfWeek.TUESDAY to BilingualText(np = "मङ्गलवार", en = "Tuesday"),
        DayOfWeek.WEDNESDAY to BilingualText(np = "बुधवार", en = "Wednesday"),
        DayOfWeek.THURSDAY to BilingualText(np = "बिहिवार", en = "Thursday"),
        DayOfWeek.FRIDAY to BilingualText(np = "शुक्रवार", en = "Friday"),
        DayOfWeek.SATURDAY to BilingualText(np = "शनिवार", en = "Saturday")
    )

    private val WEEKDAY_SHORT_NAMES: Map<DayOfWeek, BilingualText> = mapOf(
        DayOfWeek.SUNDAY to BilingualText(np = "आइत", en = "Sun"),
        DayOfWeek.MONDAY to BilingualText(np = "सोम", en = "Mon"),
        DayOfWeek.TUESDAY to BilingualText(np = "मङ्गल", en = "Tue"),
        DayOfWeek.WEDNESDAY to BilingualText(np = "बुध", en = "Wed"),
        DayOfWeek.THURSDAY to BilingualText(np = "बिहि", en = "Thu"),
        DayOfWeek.FRIDAY to BilingualText(np = "शुक्र", en = "Fri"),
        DayOfWeek.SATURDAY to BilingualText(np = "शनि", en = "Sat")
    )

    private val SUNDAY_WEEK_ORDER: List<DayOfWeek> = listOf(
        DayOfWeek.SUNDAY,
        DayOfWeek.MONDAY,
        DayOfWeek.TUESDAY,
        DayOfWeek.WEDNESDAY,
        DayOfWeek.THURSDAY,
        DayOfWeek.FRIDAY,
        DayOfWeek.SATURDAY
    )

    private val MONDAY_WEEK_ORDER: List<DayOfWeek> = listOf(
        DayOfWeek.MONDAY,
        DayOfWeek.TUESDAY,
        DayOfWeek.WEDNESDAY,
        DayOfWeek.THURSDAY,
        DayOfWeek.FRIDAY,
        DayOfWeek.SATURDAY,
        DayOfWeek.SUNDAY
    )

    fun monthName(month: Int): BilingualText {
        require(month in BsDate.MIN_MONTH..BsDate.MAX_MONTH) {
            "BS month must be between ${BsDate.MIN_MONTH} and ${BsDate.MAX_MONTH}, got $month"
        }
        return MONTH_NAMES[month - 1]
    }

    fun monthName(month: Int, language: AppLanguage): String = monthName(month).localized(language)

    fun weekdayName(dayOfWeek: DayOfWeek): BilingualText = WEEKDAY_FULL_NAMES.getValue(dayOfWeek)

    fun weekdayShortName(dayOfWeek: DayOfWeek): BilingualText = WEEKDAY_SHORT_NAMES.getValue(dayOfWeek)

    fun weekdaysInOrder(weekStart: WeekStartDay): List<DayOfWeek> = when (weekStart) {
        WeekStartDay.SUNDAY -> SUNDAY_WEEK_ORDER
        WeekStartDay.MONDAY -> MONDAY_WEEK_ORDER
    }

    fun formatNumber(value: Int, script: NumeralScript): String = when (script) {
        NumeralScript.DEVANAGARI -> DevanagariNumerals.format(value)
        NumeralScript.LATIN -> value.toString()
    }

    fun formatDigits(input: String, script: NumeralScript): String = when (script) {
        NumeralScript.DEVANAGARI -> DevanagariNumerals.format(input)
        NumeralScript.LATIN -> input
    }

    fun formatSlashDate(date: BsDate, script: NumeralScript): String {
        val raw = buildString {
            append(date.year.toString().padStart(YEAR_WIDTH, '0'))
            append('/')
            append(date.month.toString().padStart(TWO_DIGIT_WIDTH, '0'))
            append('/')
            append(date.day.toString().padStart(TWO_DIGIT_WIDTH, '0'))
        }
        return formatDigits(raw, script)
    }

    fun formatLongDate(
        date: BsDate,
        language: AppLanguage,
        script: NumeralScript,
        dayOfWeek: DayOfWeek? = null,
        zeroPadDay: Boolean = true
    ): String {
        val monthLabel = monthName(date.month).localized(language)
        val rawDay = if (zeroPadDay) date.day.toString().padStart(TWO_DIGIT_WIDTH, '0') else date.day.toString()
        val dayLabel = formatDigits(rawDay, script)
        val yearLabel = formatDigits(date.year.toString().padStart(YEAR_WIDTH, '0'), script)
        val base = "$monthLabel $dayLabel, $yearLabel"
        return if (dayOfWeek == null) {
            base
        } else {
            "${weekdayName(dayOfWeek).localized(language)}, $base"
        }
    }

    fun formatAdMonthRange(start: LocalDate, end: LocalDate): String {
        val startMonth = start.month.getDisplayName(TextStyle.SHORT, Locale.ENGLISH)
        val endMonth = end.month.getDisplayName(TextStyle.SHORT, Locale.ENGLISH)
        return when {
            start.year == end.year && start.month == end.month -> "$startMonth ${start.year}"
            start.year == end.year -> "$startMonth/$endMonth ${start.year}"
            else -> "$startMonth ${start.year}/$endMonth ${end.year}"
        }
    }
}
