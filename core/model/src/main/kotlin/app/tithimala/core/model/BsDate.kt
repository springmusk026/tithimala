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

    private companion object {
        const val MIN_MONTH = 1
        const val MAX_MONTH = 12
        const val MIN_DAY = 1
        const val MAX_DAY = 32
    }
}
