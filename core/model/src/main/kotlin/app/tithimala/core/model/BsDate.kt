package app.tithimala.core.model

import kotlinx.serialization.Serializable

/**
 * Immutable representation of a Bikram Sambat calendar date.
 */
@Serializable
data class BsDate(val year: Int, val month: Int, val day: Int) : Comparable<BsDate> {
    init {
        require(month in MIN_MONTH..MAX_MONTH) { "BS month must be between $MIN_MONTH and $MAX_MONTH, got $month" }
        require(day in MIN_DAY..MAX_DAY) { "BS day must be between $MIN_DAY and $MAX_DAY, got $day" }
    }

    override fun compareTo(other: BsDate): Int = compareValuesBy(this, other, BsDate::year, BsDate::month, BsDate::day)

    fun toIsoString(): String = buildString(ISO_LENGTH) {
        append(year.toString().padStart(YEAR_WIDTH, '0'))
        append('-')
        append(month.toString().padStart(TWO_DIGIT_WIDTH, '0'))
        append('-')
        append(day.toString().padStart(TWO_DIGIT_WIDTH, '0'))
    }

    fun toYearMonth(): BsYearMonth = BsYearMonth(year = year, month = month)

    companion object {
        const val MIN_MONTH = 1
        const val MAX_MONTH = 12
        const val MIN_DAY = 1
        const val MAX_DAY = 32

        private const val ISO_LENGTH = 10
        private const val YEAR_WIDTH = 4
        private const val TWO_DIGIT_WIDTH = 2
        private val ISO_REGEX = Regex("^(\\d{4})-(\\d{2})-(\\d{2})$")

        fun parse(iso: String): BsDate {
            val match = ISO_REGEX.matchEntire(iso)
                ?: throw IllegalArgumentException("Expected BS ISO date YYYY-MM-DD, got '$iso'")
            val (yearStr, monthStr, dayStr) = match.destructured
            return BsDate(
                year = yearStr.toInt(),
                month = monthStr.toInt(),
                day = dayStr.toInt()
            )
        }
    }
}
