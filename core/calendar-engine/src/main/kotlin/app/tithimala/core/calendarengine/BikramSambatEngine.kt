package app.tithimala.core.calendarengine

import app.tithimala.core.calendarengine.data.BundledCalendarDataLoader
import app.tithimala.core.calendarengine.data.VerifiedCalendarDataset
import app.tithimala.core.model.BilingualText
import app.tithimala.core.model.BsDate
import app.tithimala.core.model.BsMonthGrid
import app.tithimala.core.model.DatasetMetadata
import app.tithimala.core.model.MoonPhase
import app.tithimala.core.model.PatroCoverage
import app.tithimala.core.model.PatroDayInfo
import app.tithimala.core.model.WeekStartDay
import java.time.DayOfWeek
import java.time.LocalDate

/**
 * Pure-JVM Bikram Sambat calendar and Patro engine backed by a verified [VerifiedCalendarDataset].
 */
@Suppress("TooManyFunctions")
class BikramSambatEngine(
    private val dataset: VerifiedCalendarDataset,
    isRemoteUpdateActive: Boolean = false,
    lastSyncEpochMillis: Long? = null
) {
    private val tableIndex = CalendarTableIndex(dataset.calendarTable)
    private val patroIndex = PatroIndex(dataset.patroByYear, tableIndex)
    private val gridBuilder = MonthGridBuilder(tableIndex, patroIndex)

    val minBsDate: BsDate = tableIndex.minBsDate
    val maxBsDate: BsDate = tableIndex.maxBsDate
    val minAdDate: LocalDate = tableIndex.minAdDate
    val maxAdDate: LocalDate = tableIndex.maxAdDate
    val noticeText: String = dataset.noticeText
    val datasetMetadata: DatasetMetadata = DatasetMetadata(
        version = dataset.manifest.datasetVersion,
        upstreamRepo = dataset.manifest.upstreamRepo,
        upstreamCommit = dataset.manifest.upstreamCommit,
        license = dataset.manifest.license,
        patroYears = dataset.manifest.patroYears.toList(),
        isRemoteUpdateActive = isRemoteUpdateActive,
        lastSyncEpochMillis = lastSyncEpochMillis
    )

    fun isSupportedYear(year: Int): Boolean = tableIndex.isSupportedYear(year)

    fun daysInMonth(year: Int, month: Int): Int = tableIndex.daysInMonth(year, month)

    fun isValidBsDate(year: Int, month: Int, day: Int): Boolean = tableIndex.isValidBsDate(year, month, day)

    fun bsToSerial(bsDate: BsDate): Int = tableIndex.bsToSerial(bsDate)

    fun serialToBs(serial: Int): BsDate = tableIndex.serialToBs(serial)

    fun bsToAd(bsDate: BsDate): LocalDate = tableIndex.adDateForSerial(tableIndex.bsToSerial(bsDate))

    fun adToBs(adDate: LocalDate): BsDate = tableIndex.adToBs(adDate)

    fun dayOfWeek(bsDate: BsDate): DayOfWeek = tableIndex.dayOfWeekForSerial(tableIndex.bsToSerial(bsDate))

    fun dayOfYear(bsDate: BsDate): Int = tableIndex.dayOfYear(bsDate)

    fun addDaysOrNull(bsDate: BsDate, days: Long): BsDate? {
        val targetSerial = bsToSerial(bsDate).toLong() + days
        return if (targetSerial in 0L..tableIndex.maxSerial.toLong()) {
            serialToBs(targetSerial.toInt())
        } else {
            null
        }
    }

    fun addDays(bsDate: BsDate, days: Long): BsDate = addDaysOrNull(bsDate, days)
        ?: throw IllegalArgumentException(
            "Adding $days days to ${bsDate.toIsoString()} exceeds supported range $minBsDate..$maxBsDate"
        )

    fun addMonthsOrNull(bsDate: BsDate, months: Int, overflow: Boolean = false): BsDate? {
        bsToSerial(bsDate)
        val totalMonths = bsDate.year.toLong() * MONTHS_PER_YEAR + (bsDate.month - 1) + months.toLong()
        val targetYearLong = Math.floorDiv(totalMonths, MONTHS_PER_YEAR.toLong())
        val targetMonth = Math.floorMod(totalMonths, MONTHS_PER_YEAR.toLong()).toInt() + 1
        if (targetYearLong !in tableIndex.minYear.toLong()..tableIndex.maxYear.toLong()) {
            return null
        }

        val targetYear = targetYearLong.toInt()
        val targetMonthLength = daysInMonth(targetYear, targetMonth)
        return when {
            bsDate.day <= targetMonthLength -> BsDate(year = targetYear, month = targetMonth, day = bsDate.day)
            !overflow -> BsDate(year = targetYear, month = targetMonth, day = targetMonthLength)
            else -> addDaysOrNull(
                bsDate = BsDate(year = targetYear, month = targetMonth, day = targetMonthLength),
                days = (bsDate.day - targetMonthLength).toLong()
            )
        }
    }

    fun addMonths(bsDate: BsDate, months: Int, overflow: Boolean = false): BsDate =
        addMonthsOrNull(bsDate, months, overflow)
            ?: throw IllegalArgumentException(
                "Adding $months months (overflow=$overflow) to ${bsDate.toIsoString()} exceeds " +
                    "supported range $minBsDate..$maxBsDate"
            )

    fun daysUntil(from: BsDate, to: BsDate): Long = bsToSerial(to).toLong() - bsToSerial(from).toLong()

    fun patroInfo(bsDate: BsDate): PatroDayInfo = patroIndex.info(bsDate)

    fun patroCoverage(): PatroCoverage = patroIndex.coverage

    fun publicHolidaysInYear(bsYear: Int): List<BsDate> = patroIndex.publicHolidaysInYear(bsYear)

    fun allPublicHolidays(): List<BsDate> = patroIndex.allHolidays

    fun buildMonthGrid(
        year: Int,
        month: Int,
        todayBs: BsDate,
        weekStart: WeekStartDay = WeekStartDay.SUNDAY
    ): BsMonthGrid = gridBuilder.build(year = year, month = month, todayBs = todayBs, weekStart = weekStart)

    fun inferMoonPhase(tithi: BilingualText?): MoonPhase? = MoonPhaseClassifier.infer(tithi)

    companion object {
        private const val MONTHS_PER_YEAR = 12

        fun createBundled(): BikramSambatEngine = BikramSambatEngine(BundledCalendarDataLoader.loadBundled())
    }
}
