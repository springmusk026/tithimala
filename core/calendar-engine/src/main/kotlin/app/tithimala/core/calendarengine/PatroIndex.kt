package app.tithimala.core.calendarengine

import app.tithimala.core.calendarengine.data.LocalizedLabel
import app.tithimala.core.calendarengine.data.PatroYearData
import app.tithimala.core.model.BilingualText
import app.tithimala.core.model.BsDate
import app.tithimala.core.model.PatroCoverage
import app.tithimala.core.model.PatroDayInfo
import app.tithimala.core.model.TithiSource

/**
 * Dense O(1) Patro lookup index derived from verified [PatroYearData] records.
 *
 * All tithi strings (including trailing `*` and ` (K)` markers) and event lists are preserved verbatim.
 */
internal class PatroIndex(patroByYear: Map<Int, PatroYearData>, private val tableIndex: CalendarTableIndex) {
    private val firstSerial: Int
    private val lastSerial: Int
    private val entries: Array<PatroDayInfo?>
    val coverage: PatroCoverage
    val allHolidays: List<BsDate>
    private val holidaysByYear: Map<Int, List<BsDate>>

    init {
        val internedText = HashMap<LocalizedLabel, BilingualText>()
        fun intern(label: LocalizedLabel): BilingualText =
            internedText.getOrPut(label) { BilingualText(np = label.np, en = label.en) }

        val parsedEntries = ArrayList<Pair<Int, PatroDayInfo>>()
        val holidaySerials = ArrayList<Int>()
        var minDaySerial = Int.MAX_VALUE
        var maxDaySerial = Int.MIN_VALUE
        var minTithiSerial = Int.MAX_VALUE
        var maxTithiSerial = Int.MIN_VALUE

        for (yearData in patroByYear.values) {
            for ((dateKey, record) in yearData.days) {
                val bsDate = BsDate.parse(dateKey)
                val serial = tableIndex.bsToSerial(bsDate)
                val tithiText = record.tithi?.let(::intern)
                val info = PatroDayInfo(
                    tithi = tithiText,
                    tithiSource = if (tithiText != null) TithiSource.VERIFIED else TithiSource.UNAVAILABLE,
                    isPublicHoliday = record.holiday,
                    events = record.events.map(::intern)
                )
                parsedEntries.add(serial to info)
                if (serial < minDaySerial) minDaySerial = serial
                if (serial > maxDaySerial) maxDaySerial = serial
                if (tithiText != null) {
                    if (serial < minTithiSerial) minTithiSerial = serial
                    if (serial > maxTithiSerial) maxTithiSerial = serial
                }
                if (record.holiday) {
                    holidaySerials.add(serial)
                }
            }
        }

        if (parsedEntries.isEmpty()) {
            firstSerial = 0
            lastSerial = -1
            entries = emptyArray()
            coverage = PatroCoverage(
                firstDay = tableIndex.minBsDate,
                lastDay = tableIndex.minBsDate,
                tithiFirstDay = tableIndex.minBsDate,
                tithiLastDay = tableIndex.minBsDate
            )
            allHolidays = emptyList()
            holidaysByYear = emptyMap()
        } else {
            firstSerial = minDaySerial
            lastSerial = maxDaySerial
            entries = arrayOfNulls(lastSerial - firstSerial + 1)
            for ((serial, info) in parsedEntries) {
                entries[serial - firstSerial] = info
            }
            val firstDay = tableIndex.serialToBs(firstSerial)
            val lastDay = tableIndex.serialToBs(lastSerial)
            val tithiFirst = if (minTithiSerial <= maxTithiSerial) tableIndex.serialToBs(minTithiSerial) else firstDay
            val tithiLast = if (minTithiSerial <= maxTithiSerial) tableIndex.serialToBs(maxTithiSerial) else lastDay
            coverage = PatroCoverage(
                firstDay = firstDay,
                lastDay = lastDay,
                tithiFirstDay = tithiFirst,
                tithiLastDay = tithiLast
            )
            holidaySerials.sort()
            allHolidays = holidaySerials.map(tableIndex::serialToBs)
            holidaysByYear = allHolidays.groupBy(BsDate::year)
        }
    }

    fun info(bsDate: BsDate): PatroDayInfo = if (!tableIndex.isValidBsDate(bsDate.year, bsDate.month, bsDate.day)) {
        PatroDayInfo.EMPTY
    } else {
        val serial = tableIndex.bsToSerial(bsDate)
        if (serial in firstSerial..lastSerial) {
            entries[serial - firstSerial] ?: PatroDayInfo.EMPTY
        } else {
            PatroDayInfo.EMPTY
        }
    }

    fun publicHolidaysInYear(bsYear: Int): List<BsDate> = holidaysByYear[bsYear].orEmpty()
}
