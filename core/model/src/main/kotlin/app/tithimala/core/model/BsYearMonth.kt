package app.tithimala.core.model

import kotlinx.serialization.Serializable

/**
 * Immutable year-month pair in the Bikram Sambat calendar.
 */
@Serializable
data class BsYearMonth(val year: Int, val month: Int) : Comparable<BsYearMonth> {
    init {
        require(month in BsDate.MIN_MONTH..BsDate.MAX_MONTH) {
            "BS month must be between ${BsDate.MIN_MONTH} and ${BsDate.MAX_MONTH}, got $month"
        }
    }

    override fun compareTo(other: BsYearMonth): Int =
        compareValuesBy(this, other, BsYearMonth::year, BsYearMonth::month)

    fun plusMonths(delta: Int): BsYearMonth = shiftMonths(delta.toLong())

    fun minusMonths(delta: Int): BsYearMonth = shiftMonths(-delta.toLong())

    private fun shiftMonths(delta: Long): BsYearMonth {
        val totalMonths = year.toLong() * MONTHS_PER_YEAR + (month - 1) + delta
        val targetYear = Math.floorDiv(totalMonths, MONTHS_PER_YEAR.toLong()).toInt()
        val targetMonth = Math.floorMod(totalMonths, MONTHS_PER_YEAR.toLong()).toInt() + 1
        return BsYearMonth(year = targetYear, month = targetMonth)
    }

    companion object {
        private const val MONTHS_PER_YEAR = 12
    }
}
