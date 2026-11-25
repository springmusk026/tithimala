package app.tithimala.core.model

import java.time.DayOfWeek
import java.time.LocalDate
import java.util.Locale
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.serialization.json.Json
import org.junit.Test

class CoreModelTest {

    private val json = Json { encodeDefaults = true }

    @Test
    fun bsDateIsoFormattingAndParsingRoundTrip() {
        val date = BsDate(year = 2083, month = 1, day = 9)
        assertEquals("2083-01-09", date.toIsoString())
        assertEquals(date, BsDate.parse("2083-01-09"))
        assertEquals(BsYearMonth(year = 2083, month = 1), date.toYearMonth())
    }

    @Test
    fun bsDateRejectsInvalidMonthAndDayAndMalformedIso() {
        assertFailsWith<IllegalArgumentException> { BsDate(2083, 0, 1) }
        assertFailsWith<IllegalArgumentException> { BsDate(2083, 13, 1) }
        assertFailsWith<IllegalArgumentException> { BsDate(2083, 1, 0) }
        assertFailsWith<IllegalArgumentException> { BsDate(2083, 1, 33) }
        assertFailsWith<IllegalArgumentException> { BsDate.parse("2083/01/01") }
        assertFailsWith<IllegalArgumentException> { BsDate.parse("2083-1-1") }
        assertFailsWith<IllegalArgumentException> { BsDate.parse("2083-13-01") }
    }

    @Test
    fun bsYearMonthArithmeticAndOrdering() {
        val baishakh = BsYearMonth(2083, 1)
        val chaitraPrev = BsYearMonth(2082, 12)

        assertEquals(chaitraPrev, baishakh.minusMonths(1))
        assertEquals(baishakh, chaitraPrev.plusMonths(1))
        assertEquals(BsYearMonth(2085, 2), baishakh.plusMonths(25))
        assertEquals(BsYearMonth(2080, 12), baishakh.minusMonths(25))
        assertEquals(baishakh, baishakh.plusMonths(0))
        assertTrue(chaitraPrev < baishakh)
        assertFailsWith<IllegalArgumentException> { BsYearMonth(2083, 13) }
    }

    @Test
    fun bilingualTextLocalizesForExplicitAndSystemLanguages() {
        val label = BilingualText(np = "तृतिया*", en = "Tritiya (K)")

        assertEquals("तृतिया*", label.localized(AppLanguage.NEPALI))
        assertEquals("Tritiya (K)", label.localized(AppLanguage.ENGLISH))
        assertEquals("तृतिया*", label.localized(AppLanguage.SYSTEM, Locale.forLanguageTag("ne-NP")))
        assertEquals("Tritiya (K)", label.localized(AppLanguage.SYSTEM, Locale.ENGLISH))

        val encoded = json.encodeToString(BilingualText.serializer(), label)
        assertEquals(label, json.decodeFromString(BilingualText.serializer(), encoded))

        val ym = BsYearMonth(2083, 1)
        val encodedYm = json.encodeToString(BsYearMonth.serializer(), ym)
        assertEquals(ym, json.decodeFromString(BsYearMonth.serializer(), encodedYm))
    }

    @Test
    fun patroCoverageRangeChecksAndEmptyPatroDay() {
        val coverage = PatroCoverage(
            firstDay = BsDate(2077, 1, 1),
            lastDay = BsDate(2084, 8, 20),
            tithiFirstDay = BsDate(2078, 1, 2),
            tithiLastDay = BsDate(2083, 12, 30)
        )

        assertTrue(BsDate(2081, 1, 1) in coverage)
        assertFalse(BsDate(2076, 12, 30) in coverage)
        assertTrue(coverage.hasTithiCoverage(BsDate(2078, 1, 2)))
        assertFalse(coverage.hasTithiCoverage(BsDate(2078, 1, 1)))
        assertEquals(TithiSource.UNAVAILABLE, PatroDayInfo.EMPTY.tithiSource)
        assertFalse(PatroDayInfo.EMPTY.isPublicHoliday)
        assertTrue(PatroDayInfo.EMPTY.events.isEmpty())
    }

    @Test
    fun calendarDayExposesAdComponentsAndWeekdayValue() {
        val day = CalendarDay(
            bsDate = BsDate(2081, 1, 1),
            adDate = LocalDate.of(2024, 4, 13),
            dayOfWeek = DayOfWeek.SATURDAY,
            isCurrentMonth = true,
            isToday = false,
            patro = PatroDayInfo.EMPTY
        )

        assertEquals(2024, day.adYear)
        assertEquals(4, day.adMonth)
        assertEquals(13, day.adDay)
        assertEquals(6, day.dayOfWeekValue)
        assertTrue(day.isSaturday)
    }

    @Test
    fun calendarReminderValidatesFieldsAndSerializesCleanly() {
        val reminder = CalendarReminder(
            id = 42L,
            title = "Dashain Tika",
            notes = "Family gathering",
            bsYear = 2083,
            bsMonth = 6,
            bsDay = 25,
            hour = 10,
            minute = 30,
            recurrence = ReminderRecurrence.YEARLY_BS,
            isEnabled = true
        )
        assertEquals(BsDate(2083, 6, 25), reminder.bsDate)

        val encoded = json.encodeToString(CalendarReminder.serializer(), reminder)
        val decoded = json.decodeFromString(CalendarReminder.serializer(), encoded)
        assertEquals(reminder, decoded)

        assertFailsWith<IllegalArgumentException> {
            reminder.copy(hour = 24)
        }
        assertFailsWith<IllegalArgumentException> {
            reminder.copy(minute = 60)
        }
        assertFailsWith<IllegalArgumentException> {
            reminder.copy(bsMonth = 0)
        }
        assertFailsWith<IllegalArgumentException> {
            reminder.copy(bsDay = 33)
        }
    }

    @Test
    fun preferencesAndDatasetMetadataSerializeCleanly() {
        val prefs = UserPreferences()
        val encodedPrefs = json.encodeToString(UserPreferences.serializer(), prefs)
        assertEquals(prefs, json.decodeFromString(UserPreferences.serializer(), encodedPrefs))

        val metadata = DatasetMetadata(
            version = "2026.10.30",
            upstreamRepo = "https://github.com/sanamhub/nepdate",
            upstreamCommit = "77adb449dd50f54222330096203c2cd16463c4c6",
            license = "MIT",
            patroYears = (2077..2084).toList()
        )
        val encodedMeta = json.encodeToString(DatasetMetadata.serializer(), metadata)
        assertEquals(metadata, json.decodeFromString(DatasetMetadata.serializer(), encodedMeta))
    }
}
