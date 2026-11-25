package app.tithimala.core.model

import kotlinx.serialization.Serializable

@Serializable
enum class TithiSource {
    VERIFIED,
    ESTIMATED,
    UNAVAILABLE
}

@Serializable
enum class MoonPhase {
    NEW_MOON,
    WAXING_CRESCENT,
    FIRST_QUARTER,
    WAXING_GIBBOUS,
    FULL_MOON,
    WANING_GIBBOUS,
    LAST_QUARTER,
    WANING_CRESCENT
}

/**
 * Patro metadata for a single Bikram Sambat day.
 *
 * Trailing `*` and ` (K)` markers in [tithi] are preserved verbatim from upstream data.
 */
@Serializable
data class PatroDayInfo(
    val tithi: BilingualText?,
    val tithiSource: TithiSource,
    val isPublicHoliday: Boolean,
    val events: List<BilingualText>
) {
    companion object {
        val EMPTY = PatroDayInfo(
            tithi = null,
            tithiSource = TithiSource.UNAVAILABLE,
            isPublicHoliday = false,
            events = emptyList()
        )
    }
}

/**
 * Date bounds covered by the loaded Patro dataset.
 */
@Serializable
data class PatroCoverage(
    val firstDay: BsDate,
    val lastDay: BsDate,
    val tithiFirstDay: BsDate,
    val tithiLastDay: BsDate
) {
    operator fun contains(date: BsDate): Boolean = date in firstDay..lastDay

    fun hasTithiCoverage(date: BsDate): Boolean = date in tithiFirstDay..tithiLastDay
}
