package app.tithimala.core.calendarengine.data

import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import org.junit.Test

class BundledCalendarDataTest {

    @Test
    fun bundledDatasetLoadsAndPassesSha256AndSchemaValidation() {
        val dataset = BundledCalendarDataLoader.loadBundled()

        assertEquals(1, dataset.manifest.schema)
        assertEquals("77adb449dd50f54222330096203c2cd16463c4c6", dataset.manifest.upstreamCommit)
        assertEquals(1901, dataset.calendarTable.minYear)
        assertEquals(2199, dataset.calendarTable.maxYear)
        assertEquals("1901-01-01", dataset.calendarTable.epoch.bs)
        assertEquals("1844-04-11", dataset.calendarTable.epoch.ad)
        assertEquals((2077..2084).toList(), dataset.patroByYear.keys.sorted())
        assertTrue(dataset.noticeText.contains("MIT License"))
        assertTrue(dataset.noticeText.contains("NepDate"))
    }

    @Test
    fun bundledPatroPreservesVerbatimTithiMarkers() {
        val dataset = BundledCalendarDataLoader.loadBundled()
        val allTithis = dataset.patroByYear.values.flatMap { year ->
            year.days.values.mapNotNull { it.tithi }
        }
        assertTrue(allTithis.any { it.np.endsWith("*") && it.en.endsWith(" (K)") })

        val sampleDay = dataset.patroByYear[2083]?.days?.get("2083-01-01")
        assertNotNull(sampleDay)
        assertEquals("द्वादशी", sampleDay.tithi?.np)
        assertEquals("Dwadashi", sampleDay.tithi?.en)
        assertTrue(sampleDay.holiday)
        assertEquals(3, sampleDay.events.size)
    }

    @Test
    fun tamperedBytesFailSha256Verification() {
        val classLoader = BundledCalendarDataLoader::class.java.classLoader
        val manifestBytes = classLoader
            .getResourceAsStream("${BundledCalendarDataLoader.BUNDLED_RESOURCE_ROOT}/manifest.json")!!
            .readBytes()

        assertFailsWith<CalendarDataValidationException> {
            BundledCalendarDataLoader.loadAndVerify(manifestBytes) { path ->
                val raw = classLoader
                    .getResourceAsStream("${BundledCalendarDataLoader.BUNDLED_RESOURCE_ROOT}/$path")!!
                    .readBytes()
                if (path == "patro/2083.json") raw + byteArrayOf(' '.code.toByte()) else raw
            }
        }
    }

    @Test
    fun sha256VerifierMatchesKnownDigest() {
        val bytes = "tithimala".toByteArray(Charsets.UTF_8)
        val hex = Sha256Verifier.sha256Hex(bytes)
        assertEquals(64, hex.length)
        assertTrue(Sha256Verifier.verify(bytes, hex))
        assertTrue(Sha256Verifier.verify(bytes, hex.uppercase()))
        assertFalse(Sha256Verifier.verify(bytes, "0".repeat(64)))
    }

    @Test
    fun invalidSchemaAndOutOfRangeDayFailValidation() {
        val dataset = BundledCalendarDataLoader.loadBundled()

        assertFailsWith<CalendarDataValidationException> {
            CalendarDataValidator.validateBsCalendar(dataset.calendarTable.copy(schema = 99))
        }

        val badPatro = dataset.patroByYear.getValue(2084).copy(
            days = mapOf("2084-01-32" to PatroDayRecord(holiday = true))
        )
        assertFailsWith<CalendarDataValidationException> {
            CalendarDataValidator.validatePatroYear(badPatro, dataset.calendarTable)
        }
    }
}
