package app.tithimala.core.data

import app.tithimala.core.calendarengine.BikramSambatEngine
import app.tithimala.core.model.BsDate
import app.tithimala.core.model.BsMonthGrid
import app.tithimala.core.model.DatasetMetadata
import app.tithimala.core.model.PatroDayInfo
import app.tithimala.core.model.WeekStartDay
import java.time.LocalDate
import kotlinx.coroutines.flow.StateFlow

/**
 * Outcome of an optional static-JSON remote calendar dataset refresh attempt.
 */
sealed interface CalendarRefreshResult {
    data class Updated(val version: String) : CalendarRefreshResult
    data class UpToDate(val version: String) : CalendarRefreshResult
    data object SkippedDisabled : CalendarRefreshResult
    data class FallbackToBundled(val reason: String, val isTransientNetworkError: Boolean = false) :
        CalendarRefreshResult
}

/**
 * Offline-first repository providing verified Bikram Sambat calendar conversions, Patro lookups,
 * and optional static-JSON background updates with silent fallback to bundled data.
 */
interface CalendarRepository {
    val engine: StateFlow<BikramSambatEngine>
    val datasetMetadata: StateFlow<DatasetMetadata>

    fun todayBs(now: LocalDate = LocalDate.now()): BsDate
    fun bsToAd(bsDate: BsDate): LocalDate
    fun adToBs(adDate: LocalDate): BsDate
    fun buildMonthGrid(year: Int, month: Int, weekStart: WeekStartDay, todayBs: BsDate = todayBs()): BsMonthGrid
    fun patroInfo(bsDate: BsDate): PatroDayInfo
    fun noticeText(): String
    suspend fun refreshRemoteDatasetNow(): CalendarRefreshResult
}
