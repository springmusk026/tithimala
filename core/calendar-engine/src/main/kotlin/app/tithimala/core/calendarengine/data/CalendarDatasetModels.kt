package app.tithimala.core.calendarengine.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CalendarManifest(
    val schema: Int,
    @SerialName("dataset_version") val datasetVersion: String,
    @SerialName("upstream_repo") val upstreamRepo: String,
    @SerialName("upstream_commit") val upstreamCommit: String,
    val license: String,
    @SerialName("calendar_file") val calendarFile: String,
    @SerialName("notice_file") val noticeFile: String,
    @SerialName("patro_years") val patroYears: List<Int>,
    val sha256: Map<String, String>
)

@Serializable
data class CalendarSourceMetadata(val repo: String, val commit: String, val license: String, val imported: String)

@Serializable
data class BsCalendarEpoch(val bs: String, val ad: String)

@Serializable
data class BsCalendarTable(
    val schema: Int,
    val source: CalendarSourceMetadata,
    @SerialName("min_year") val minYear: Int,
    @SerialName("max_year") val maxYear: Int,
    val epoch: BsCalendarEpoch,
    @SerialName("month_lengths") val monthLengths: Map<String, List<Int>>
)

@Serializable
data class LocalizedLabel(val np: String, val en: String)

@Serializable
data class PatroDayRecord(
    val tithi: LocalizedLabel? = null,
    val holiday: Boolean = false,
    val events: List<LocalizedLabel> = emptyList()
)

@Serializable
data class PatroYearData(
    val schema: Int,
    val source: CalendarSourceMetadata,
    val year: Int,
    val days: Map<String, PatroDayRecord>
)

/**
 * Verified in-memory representation of a complete calendar dataset (BS calendar table + patro years + NOTICE).
 */
data class VerifiedCalendarDataset(
    val manifest: CalendarManifest,
    val calendarTable: BsCalendarTable,
    val patroByYear: Map<Int, PatroYearData>,
    val noticeText: String
)
