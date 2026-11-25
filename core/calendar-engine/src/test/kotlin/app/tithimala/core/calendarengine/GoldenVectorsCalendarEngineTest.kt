package app.tithimala.core.calendarengine

import app.tithimala.core.model.AppLanguage
import app.tithimala.core.model.BsDate
import app.tithimala.core.model.NumeralScript
import java.time.DayOfWeek
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.junit.Test

class GoldenVectorsCalendarEngineTest {

    private val engine = BikramSambatEngine.createBundled()

    @Test
    fun allMonthBoundariesMatchGoldenCsv() {
        val lines = readVectorLines("vectors/month-boundaries.csv").drop(1)
        assertEquals(EXPECTED_MONTH_BOUNDARIES, lines.size)

        for (line in lines) {
            val cols = line.split(",")
            val bsDate = BsDate.parse(cols[0])
            val expectedAd = LocalDate.parse(cols[1])
            val expectedWeekday = DayOfWeek.valueOf(cols[2].uppercase())
            val expectedMonthLength = cols[3].toInt()

            assertEquals(expectedAd, engine.bsToAd(bsDate), "bsToAd mismatch for $bsDate")
            assertEquals(bsDate, engine.adToBs(expectedAd), "adToBs mismatch for $expectedAd")
            assertEquals(expectedWeekday, engine.dayOfWeek(bsDate), "weekday mismatch for $bsDate")
            assertEquals(
                expectedMonthLength,
                engine.daysInMonth(bsDate.year, bsDate.month),
                "monthLength mismatch for $bsDate"
            )
        }
    }

    @Test
    fun allCsharpGoldenDatesMatchConversionsAndFormatting() {
        val lines = readVectorLines("vectors/csharp-golden/dates.tsv").drop(1)
        assertEquals(EXPECTED_CSHARP_DATES, lines.size)

        for (line in lines) {
            val cols = line.split("\t")
            val bsDate = BsDate.parse(cols[0])
            val expectedAd = LocalDate.parse(cols[1])
            val expectedWeekday = DayOfWeek.valueOf(cols[2].uppercase())
            val expectedDayOfYear = cols[3].toInt()
            val expectedMonthLength = cols[4].toInt()
            val expectedSlashEn = cols[5]
            val expectedSlashNe = cols[6]
            val expectedLongEn = cols[7]
            val expectedLongEnWeekday = cols[8]
            val expectedLongNe = cols[9]
            val expectedLongNeWeekday = cols[10]

            assertEquals(expectedAd, engine.bsToAd(bsDate), "bsToAd mismatch for $bsDate")
            assertEquals(bsDate, engine.adToBs(expectedAd), "adToBs mismatch for $expectedAd")
            assertEquals(expectedWeekday, engine.dayOfWeek(bsDate), "dayOfWeek mismatch for $bsDate")
            assertEquals(expectedDayOfYear, engine.dayOfYear(bsDate), "dayOfYear mismatch for $bsDate")
            assertEquals(expectedMonthLength, engine.daysInMonth(bsDate.year, bsDate.month))
            assertEquals(expectedSlashEn, BsDateFormatter.formatSlashDate(bsDate, NumeralScript.LATIN))
            assertEquals(expectedSlashNe, BsDateFormatter.formatSlashDate(bsDate, NumeralScript.DEVANAGARI))
            assertEquals(
                expectedLongEn,
                BsDateFormatter.formatLongDate(bsDate, AppLanguage.ENGLISH, NumeralScript.LATIN)
            )
            assertEquals(
                expectedLongEnWeekday,
                BsDateFormatter.formatLongDate(bsDate, AppLanguage.ENGLISH, NumeralScript.LATIN, expectedWeekday)
            )
            assertEquals(
                expectedLongNe,
                BsDateFormatter.formatLongDate(bsDate, AppLanguage.NEPALI, NumeralScript.DEVANAGARI)
            )
            assertEquals(
                expectedLongNeWeekday,
                BsDateFormatter.formatLongDate(bsDate, AppLanguage.NEPALI, NumeralScript.DEVANAGARI, expectedWeekday)
            )
        }
    }

    @Test
    fun allCsharpGoldenAddDaysMatchIncludingErrors() {
        val lines = readVectorLines("vectors/csharp-golden/add-days.tsv").drop(1)
        assertEquals(EXPECTED_ADD_DAYS_ROWS, lines.size)

        for (line in lines) {
            val cols = line.split("\t")
            val bsDate = BsDate.parse(cols[0])
            val delta = cols[1].toLong()
            val expected = cols[2]

            if (expected == ERR_MARKER) {
                assertNull(engine.addDaysOrNull(bsDate, delta), "Expected null for $bsDate + $delta days")
                assertFailsWith<IllegalArgumentException>("Expected throw for $bsDate + $delta days") {
                    engine.addDays(bsDate, delta)
                }
            } else {
                val expectedDate = BsDate.parse(expected)
                assertEquals(expectedDate, engine.addDaysOrNull(bsDate, delta))
                assertEquals(expectedDate, engine.addDays(bsDate, delta), "addDays mismatch for $bsDate + $delta")
            }
        }
    }

    @Test
    fun allCsharpGoldenAddMonthsMatchClampAndOverflow() {
        val lines = readVectorLines("vectors/csharp-golden/add-months.tsv").drop(1)
        assertEquals(EXPECTED_ADD_MONTHS_ROWS, lines.size)

        for (line in lines) {
            val cols = line.split("\t")
            val bsDate = BsDate.parse(cols[0])
            val delta = cols[1].toInt()
            val expectedClamp = cols[2]
            val expectedOverflow = cols[3]

            verifyAddMonthsCase(bsDate, delta, overflow = false, expected = expectedClamp)
            verifyAddMonthsCase(bsDate, delta, overflow = true, expected = expectedOverflow)
        }
    }

    @Test
    fun allCsharpGoldenDayDiffRowsMatch() {
        val lines = readVectorLines("vectors/csharp-golden/day-diff.tsv")
            .filter { it.isNotBlank() && !it.startsWith("#") }
            .drop(1)
        assertEquals(EXPECTED_DAY_DIFF_ROWS, lines.size)

        for (line in lines) {
            val cols = line.split("\t")
            val from = BsDate.parse(cols[0])
            val to = BsDate.parse(cols[1])
            val expectedDaysUntil = cols[2].toLong()
            val expectedReverse = cols[3].toLong()

            assertEquals(expectedDaysUntil, engine.daysUntil(from, to), "daysUntil($from, $to)")
            assertEquals(expectedReverse, engine.daysUntil(to, from), "daysUntil($to, $from)")
        }
    }

    @Test
    fun exhaustiveSerialAndGregorianRoundTripAcrossAll299Years() {
        assertEquals(BsDate(1901, 1, 1), engine.minBsDate)
        assertEquals(BsDate(2199, 12, 30), engine.maxBsDate)
        assertEquals(LocalDate.of(1844, 4, 11), engine.minAdDate)

        val maxSerial = engine.bsToSerial(engine.maxBsDate)
        assertEquals(EXPECTED_MAX_SERIAL, maxSerial)
        assertEquals(engine.minAdDate.plusDays(maxSerial.toLong()), engine.maxAdDate)

        for (serial in 0..maxSerial) {
            val bs = engine.serialToBs(serial)
            assertEquals(serial, engine.bsToSerial(bs), "Serial round-trip failed at $serial")
            val ad = engine.bsToAd(bs)
            assertEquals(bs, engine.adToBs(ad), "AD round-trip failed at $serial ($bs)")
            assertEquals(ad.dayOfWeek, engine.dayOfWeek(bs), "Weekday mismatch at $serial ($bs)")
        }
    }

    @Test
    fun outOfRangeAndInvalidDatesAreRejected() {
        assertTrue(engine.isSupportedYear(1901))
        assertTrue(engine.isSupportedYear(2199))
        assertFalse(engine.isSupportedYear(1900))
        assertFalse(engine.isSupportedYear(2200))

        assertTrue(engine.isValidBsDate(1901, 3, 32))
        assertFalse(engine.isValidBsDate(1901, 1, 32))
        assertFalse(engine.isValidBsDate(1900, 1, 1))
        assertFalse(engine.isValidBsDate(2200, 1, 1))

        assertFailsWith<IllegalArgumentException> { engine.serialToBs(-1) }
        assertFailsWith<IllegalArgumentException> { engine.serialToBs(EXPECTED_MAX_SERIAL + 1) }
        assertFailsWith<IllegalArgumentException> { engine.bsToAd(BsDate(1900, 12, 30)) }
        assertFailsWith<IllegalArgumentException> { engine.bsToAd(BsDate(1901, 1, 32)) }
        assertFailsWith<IllegalArgumentException> { engine.adToBs(engine.minAdDate.minusDays(1)) }
        assertFailsWith<IllegalArgumentException> { engine.adToBs(engine.maxAdDate.plusDays(1)) }
        assertFailsWith<IllegalArgumentException> { engine.daysInMonth(1900, 1) }
        assertFailsWith<IllegalArgumentException> { engine.daysInMonth(2083, 13) }
    }

    private fun verifyAddMonthsCase(bsDate: BsDate, delta: Int, overflow: Boolean, expected: String) {
        if (expected == ERR_MARKER) {
            assertNull(
                engine.addMonthsOrNull(bsDate, delta, overflow),
                "Expected null for addMonths($bsDate, $delta, overflow=$overflow)"
            )
            assertFailsWith<IllegalArgumentException> {
                engine.addMonths(bsDate, delta, overflow)
            }
        } else {
            val expectedDate = BsDate.parse(expected)
            assertEquals(expectedDate, engine.addMonthsOrNull(bsDate, delta, overflow))
            assertEquals(
                expectedDate,
                engine.addMonths(bsDate, delta, overflow),
                "addMonths mismatch for $bsDate + $delta (overflow=$overflow)"
            )
        }
    }

    private fun readVectorLines(path: String): List<String> {
        val stream = requireNotNull(javaClass.classLoader?.getResourceAsStream(path)) {
            "Missing golden vector resource: $path"
        }
        return stream.bufferedReader(Charsets.UTF_8).use { reader ->
            reader.readLines().filter { it.isNotEmpty() }
        }
    }

    private companion object {
        const val ERR_MARKER = "ERR"
        const val EXPECTED_MONTH_BOUNDARIES = 7176
        const val EXPECTED_CSHARP_DATES = 1018
        const val EXPECTED_ADD_DAYS_ROWS = 9162
        const val EXPECTED_ADD_MONTHS_ROWS = 10180
        const val EXPECTED_DAY_DIFF_ROWS = 16
        const val EXPECTED_MAX_SERIAL = 109211
    }
}
