package app.tithimala.core.calendarengine

import app.tithimala.core.model.BilingualText
import app.tithimala.core.model.BsDate
import app.tithimala.core.model.MoonPhase
import app.tithimala.core.model.PatroDayInfo
import app.tithimala.core.model.TithiSource
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.junit.Test

class GoldenPatroEngineTest {

    private val engine = BikramSambatEngine.createBundled()
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun patroSummaryMatchesGoldenJsonCountsExactly() {
        val summaryText = requireNotNull(
            javaClass.classLoader?.getResourceAsStream("vectors/patro-summary.json")
        ).bufferedReader(Charsets.UTF_8).use { it.readText() }
        val summary = json.decodeFromString<PatroSummaryVector>(summaryText)

        val coverage = engine.patroCoverage()
        assertEquals(BsDate.parse(summary.firstDay), coverage.firstDay)
        assertEquals(BsDate.parse(summary.lastDay), coverage.lastDay)
        assertEquals(BsDate.parse(summary.tithiFirstDay), coverage.tithiFirstDay)
        assertEquals(BsDate.parse(summary.tithiLastDay), coverage.tithiLastDay)

        val firstSerial = engine.bsToSerial(coverage.firstDay)
        val lastSerial = engine.bsToSerial(coverage.lastDay)
        val tithiFirstSerial = engine.bsToSerial(coverage.tithiFirstDay)
        val tithiLastSerial = engine.bsToSerial(coverage.tithiLastDay)

        assertEquals(summary.firstSerial, firstSerial)
        assertEquals(summary.lastSerial, lastSerial)
        assertEquals(summary.days, lastSerial - firstSerial + 1)
        assertEquals(summary.tithiFirstSerial, tithiFirstSerial)
        assertEquals(summary.tithiDays, tithiLastSerial - tithiFirstSerial + 1)

        var tithiDayCount = 0
        var holidayCount = 0
        var eventDayCount = 0
        var totalEvents = 0
        val distinctTithis = HashSet<BilingualText>()
        val distinctEvents = HashSet<BilingualText>()

        for (serial in firstSerial..lastSerial) {
            val bsDate = engine.serialToBs(serial)
            val info = engine.patroInfo(bsDate)
            if (serial in tithiFirstSerial..tithiLastSerial) {
                assertNotNull(info.tithi, "Gap in contiguous tithi coverage at $bsDate")
                assertEquals(TithiSource.VERIFIED, info.tithiSource)
            }
            info.tithi?.let {
                tithiDayCount++
                distinctTithis.add(it)
            }
            if (info.isPublicHoliday) {
                holidayCount++
            }
            if (info.events.isNotEmpty()) {
                eventDayCount++
                totalEvents += info.events.size
                distinctEvents.addAll(info.events)
            }
        }

        assertEquals(summary.tithiDays, tithiDayCount)
        assertEquals(summary.tithiNames, distinctTithis.size)
        assertEquals(summary.holidays, holidayCount)
        assertEquals(summary.eventDays, eventDayCount)
        assertEquals(summary.events, totalEvents)
        assertEquals(summary.eventNames, distinctEvents.size)

        val allHolidays = engine.allPublicHolidays()
        assertEquals(summary.holidays, allHolidays.size)
        assertEquals(allHolidays.sorted(), allHolidays)
        val holidaysByYearTotal = (summary.firstYear..summary.lastYear).sumOf { year ->
            engine.publicHolidaysInYear(year).size
        }
        assertEquals(summary.holidays, holidaysByYearTotal)
    }

    @Test
    fun patroMdSection4TestAnchorsMatchExactly() {
        val newYear2081 = engine.patroInfo(BsDate(2081, 1, 1))
        assertEquals("Panchami", newYear2081.tithi?.en)
        assertEquals(TithiSource.VERIFIED, newYear2081.tithiSource)
        assertTrue(newYear2081.isPublicHoliday)
        assertEquals(
            listOf("New Year", "Mesh Sankranti", "Bisket Jatra"),
            newYear2081.events.map { it.en }
        )

        val baishakh2 = engine.patroInfo(BsDate(2081, 1, 2))
        assertEquals("Shashthi", baishakh2.tithi?.en)
        assertFalse(baishakh2.isPublicHoliday)
        assertTrue(baishakh2.events.isEmpty())

        val republicDay = engine.patroInfo(BsDate(2081, 2, 15))
        assertEquals("Panchami", republicDay.tithi?.en)
        assertTrue(republicDay.isPublicHoliday)
        assertEquals(listOf("Republic Day"), republicDay.events.map { it.en })

        val falgun1 = engine.patroInfo(BsDate(2080, 11, 1))
        assertEquals("Chaturthi", falgun1.tithi?.en)
        assertFalse(falgun1.isPublicHoliday)
        assertEquals(
            listOf(
                "Mangala Chauthi Brata",
                "People's War Day",
                "Tilakund Chauthi",
                "Kumbha Sankranti",
                "World Radio Day"
            ),
            falgun1.events.map { it.en }
        )

        val shrawan15 = engine.patroInfo(BsDate(2081, 4, 15))
        assertEquals("Dashami", shrawan15.tithi?.en)
        assertFalse(shrawan15.isPublicHoliday)
        assertEquals(
            listOf(
                "Kheer Khane Din",
                "World Day Against Human Trafficking",
                "International Friendship Day"
            ),
            shrawan15.events.map { it.en }
        )

        assertEquals(PatroDayInfo.EMPTY, engine.patroInfo(BsDate(2070, 6, 15)))
        assertEquals(PatroDayInfo.EMPTY, engine.patroInfo(BsDate(2090, 1, 1)))
    }

    @Test
    fun patroInfoNeverThrowsForOutOfRangeOrInvalidDays() {
        val probes = listOf(
            BsDate(1850, 1, 1),
            BsDate(1901, 1, 1),
            BsDate(2070, 6, 15),
            BsDate(2090, 1, 1),
            BsDate(2199, 12, 30),
            BsDate(2500, 1, 1),
            BsDate(2081, 8, 32)
        )
        for (probe in probes) {
            assertEquals(PatroDayInfo.EMPTY, engine.patroInfo(probe), "Expected EMPTY for $probe")
        }
        assertTrue(engine.publicHolidaysInYear(1901).isEmpty())
        assertTrue(engine.publicHolidaysInYear(2099).isEmpty())
    }

    @Test
    fun tithiMarkersPreservedVerbatimAndMoonPhaseCoversAllBundledTithis() {
        val coverage = engine.patroCoverage()
        val firstSerial = engine.bsToSerial(coverage.tithiFirstDay)
        val lastSerial = engine.bsToSerial(coverage.tithiLastDay)

        val distinctTithis = HashSet<BilingualText>()
        for (serial in firstSerial..lastSerial) {
            val tithi = engine.patroInfo(engine.serialToBs(serial)).tithi
            assertNotNull(tithi)
            distinctTithis.add(tithi)
            assertNotNull(engine.inferMoonPhase(tithi), "Expected non-null moon phase for $tithi")
        }

        assertEquals(EXPECTED_DISTINCT_TITHIS, distinctTithis.size)
        assertTrue(distinctTithis.any { it.np == "प्रतिपदा*" && it.en == "Pratipadha (K)" })
        assertTrue(distinctTithis.any { it.np == "तृतिया*" && it.en == "Tritiya (K)" })

        assertEquals(MoonPhase.FULL_MOON, engine.inferMoonPhase(BilingualText("पूर्णिमा", "Purnima")))
        assertEquals(MoonPhase.FULL_MOON, engine.inferMoonPhase(BilingualText("पूर्णिमा*", "Purnima (K)")))
        assertEquals(MoonPhase.NEW_MOON, engine.inferMoonPhase(BilingualText("औंसी", "Aunsi")))
        assertEquals(MoonPhase.WAXING_CRESCENT, engine.inferMoonPhase(BilingualText("प्रतिपदा", "Pratipadha")))
        assertEquals(MoonPhase.WAXING_CRESCENT, engine.inferMoonPhase(BilingualText("प्रतिपदा", "Pratipada")))
        assertEquals(MoonPhase.FIRST_QUARTER, engine.inferMoonPhase(BilingualText("पञ्चमी", "Panchami")))
        assertEquals(MoonPhase.WAXING_GIBBOUS, engine.inferMoonPhase(BilingualText("अष्टमी", "Ashtami")))
        assertEquals(MoonPhase.WAXING_GIBBOUS, engine.inferMoonPhase(BilingualText("चतुर्दशी", "Chaturdashi")))
        assertEquals(
            MoonPhase.WANING_GIBBOUS,
            engine.inferMoonPhase(BilingualText("कृष्ण द्वितीया", "Krishna Dwitiya"))
        )
        assertEquals(MoonPhase.LAST_QUARTER, engine.inferMoonPhase(BilingualText("कृष्ण सप्तमी", "Krishna Saptami")))
        assertEquals(
            MoonPhase.WANING_CRESCENT,
            engine.inferMoonPhase(BilingualText("कृष्ण एकादशी", "Krishna Ekadashi"))
        )
        assertNull(engine.inferMoonPhase(null))
        assertNull(engine.inferMoonPhase(BilingualText("अज्ञात", "Unknown")))
    }

    @Test
    fun metadataAndNoticeExposeBundledDatasetProvenance() {
        val metadata = engine.datasetMetadata
        assertEquals("2026.10.30", metadata.version)
        assertEquals("https://github.com/sanamhub/nepdate", metadata.upstreamRepo)
        assertEquals("77adb449dd50f54222330096203c2cd16463c4c6", metadata.upstreamCommit)
        assertEquals("MIT", metadata.license)
        assertEquals((2077..2084).toList(), metadata.patroYears)
        assertFalse(metadata.isRemoteUpdateActive)
        assertNull(metadata.lastSyncEpochMillis)
        assertTrue(engine.noticeText.contains("MIT License"))
    }

    @Serializable
    private data class PatroSummaryVector(
        @SerialName("first_year") val firstYear: Int,
        @SerialName("last_year") val lastYear: Int,
        @SerialName("first_day") val firstDay: String,
        @SerialName("last_day") val lastDay: String,
        @SerialName("first_serial") val firstSerial: Int,
        @SerialName("last_serial") val lastSerial: Int,
        val days: Int,
        @SerialName("tithi_first_day") val tithiFirstDay: String,
        @SerialName("tithi_last_day") val tithiLastDay: String,
        @SerialName("tithi_first_serial") val tithiFirstSerial: Int,
        @SerialName("tithi_days") val tithiDays: Int,
        @SerialName("tithi_names") val tithiNames: Int,
        val holidays: Int,
        @SerialName("event_days") val eventDays: Int,
        val events: Int,
        @SerialName("event_names") val eventNames: Int
    )

    private companion object {
        const val EXPECTED_DISTINCT_TITHIS = 32
    }
}
