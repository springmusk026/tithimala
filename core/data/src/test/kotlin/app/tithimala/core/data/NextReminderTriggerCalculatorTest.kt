package app.tithimala.core.data

import app.tithimala.core.calendarengine.BikramSambatEngine
import app.tithimala.core.model.BsDate
import app.tithimala.core.model.CalendarReminder
import app.tithimala.core.model.ReminderRecurrence
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import org.junit.Test

class NextReminderTriggerCalculatorTest {

    private val engine: BikramSambatEngine = BikramSambatEngine.createBundled()
    private val kathmanduZone: ZoneId = ZoneId.of("Asia/Kathmandu")

    @Test
    fun oneTimeReminderInFutureReturnsExactTriggerMillis() {
        val reminder = CalendarReminder(
            id = 1L,
            title = "Baisakh Sankranti",
            bsYear = 2083,
            bsMonth = 1,
            bsDay = 1,
            hour = 9,
            minute = 30,
            recurrence = ReminderRecurrence.ONE_TIME
        )
        val expectedMillis = epochMillisFor(BsDate(2083, 1, 1), hour = 9, minute = 30)
        val nowMillis = expectedMillis - 60_000L

        val computed = NextReminderTriggerCalculator.computeNextTrigger(
            reminder = reminder,
            engine = engine,
            nowMillis = nowMillis,
            zoneId = kathmanduZone
        )

        assertNotNull(computed)
        assertEquals(expectedMillis, computed.triggerAtEpochMillis)
        assertEquals(BsDate(2083, 1, 1), computed.effectiveBsDate)
        assertEquals(
            expectedMillis,
            NextReminderTriggerCalculator.computeNextTriggerMillis(reminder, engine, nowMillis, kathmanduZone)
        )
    }

    @Test
    fun oneTimeReminderAtOrBeforeNowReturnsNull() {
        val reminder = CalendarReminder(
            id = 2L,
            title = "Past event",
            bsYear = 2083,
            bsMonth = 1,
            bsDay = 1,
            hour = 9,
            minute = 0,
            recurrence = ReminderRecurrence.ONE_TIME
        )
        val triggerMillis = epochMillisFor(BsDate(2083, 1, 1), hour = 9, minute = 0)

        assertNull(
            NextReminderTriggerCalculator.computeNextTriggerMillis(
                reminder = reminder,
                engine = engine,
                nowMillis = triggerMillis,
                zoneId = kathmanduZone
            )
        )
        assertNull(
            NextReminderTriggerCalculator.computeNextTriggerMillis(
                reminder = reminder,
                engine = engine,
                nowMillis = triggerMillis + 1_000L,
                zoneId = kathmanduZone
            )
        )
    }

    @Test
    fun oneTimeReminderWithNonexistentDayInShortMonthReturnsNull() {
        // Jestha (month 2) in BS 2082 has 31 days, not 32.
        val invalidReminder = CalendarReminder(
            id = 3L,
            title = "Invalid day",
            bsYear = 2082,
            bsMonth = 2,
            bsDay = 32,
            hour = 8,
            minute = 0,
            recurrence = ReminderRecurrence.ONE_TIME
        )
        val nowMillis = epochMillisFor(BsDate(2082, 1, 1), hour = 0, minute = 0)

        assertNull(
            NextReminderTriggerCalculator.computeNextTrigger(
                reminder = invalidReminder,
                engine = engine,
                nowMillis = nowMillis,
                zoneId = kathmanduZone
            )
        )
    }

    @Test
    fun yearlyBsReminderTriggersInSameYearWhenTimeIsStillUpcoming() {
        val reminder = CalendarReminder(
            id = 4L,
            title = "Birthday",
            bsYear = 2075,
            bsMonth = 5,
            bsDay = 15,
            hour = 10,
            minute = 0,
            recurrence = ReminderRecurrence.YEARLY_BS
        )
        val nowMillis = epochMillisFor(BsDate(2083, 5, 15), hour = 9, minute = 59)
        val expectedMillis = epochMillisFor(BsDate(2083, 5, 15), hour = 10, minute = 0)

        val computed = NextReminderTriggerCalculator.computeNextTrigger(
            reminder = reminder,
            engine = engine,
            nowMillis = nowMillis,
            zoneId = kathmanduZone
        )

        assertNotNull(computed)
        assertEquals(BsDate(2083, 5, 15), computed.effectiveBsDate)
        assertEquals(expectedMillis, computed.triggerAtEpochMillis)
    }

    @Test
    fun yearlyBsReminderRollsOverToNextBsYearOnceTodaysTimePasses() {
        val reminder = CalendarReminder(
            id = 5L,
            title = "Annual Puja",
            bsYear = 2080,
            bsMonth = 1,
            bsDay = 1,
            hour = 7,
            minute = 30,
            recurrence = ReminderRecurrence.YEARLY_BS
        )
        val exactFireTime2083 = epochMillisFor(BsDate(2083, 1, 1), hour = 7, minute = 30)
        val expectedNextYearMillis = epochMillisFor(BsDate(2084, 1, 1), hour = 7, minute = 30)

        val computedAtExactFire = NextReminderTriggerCalculator.computeNextTrigger(
            reminder = reminder,
            engine = engine,
            nowMillis = exactFireTime2083,
            zoneId = kathmanduZone
        )
        val computedAfterFire = NextReminderTriggerCalculator.computeNextTrigger(
            reminder = reminder,
            engine = engine,
            nowMillis = exactFireTime2083 + 60_000L,
            zoneId = kathmanduZone
        )

        assertNotNull(computedAtExactFire)
        assertEquals(BsDate(2084, 1, 1), computedAtExactFire.effectiveBsDate)
        assertEquals(expectedNextYearMillis, computedAtExactFire.triggerAtEpochMillis)

        assertNotNull(computedAfterFire)
        assertEquals(BsDate(2084, 1, 1), computedAfterFire.effectiveBsDate)
        assertEquals(expectedNextYearMillis, computedAfterFire.triggerAtEpochMillis)
    }

    @Test
    fun yearlyBsReminderRespectsFutureAnchorYear() {
        val reminder = CalendarReminder(
            id = 6L,
            title = "Future yearly milestone",
            bsYear = 2086,
            bsMonth = 4,
            bsDay = 10,
            hour = 8,
            minute = 0,
            recurrence = ReminderRecurrence.YEARLY_BS
        )
        val nowMillis = epochMillisFor(BsDate(2083, 1, 1), hour = 0, minute = 0)

        val computed = NextReminderTriggerCalculator.computeNextTrigger(
            reminder = reminder,
            engine = engine,
            nowMillis = nowMillis,
            zoneId = kathmanduZone
        )

        assertNotNull(computed)
        assertEquals(BsDate(2086, 4, 10), computed.effectiveBsDate)
        assertEquals(epochMillisFor(BsDate(2086, 4, 10), hour = 8, minute = 0), computed.triggerAtEpochMillis)
    }

    @Test
    fun yearlyBsReminderClamps32DayMonthInShorterFutureYearsAndRestoresIn32DayYears() {
        // Jestha (month 2) has 32 days in BS 2081 and 2085, and 31 days in BS 2082, 2083, 2084.
        assertEquals(32, engine.daysInMonth(2081, 2))
        assertEquals(31, engine.daysInMonth(2082, 2))
        assertEquals(32, engine.daysInMonth(2085, 2))

        val reminder = CalendarReminder(
            id = 7L,
            title = "Jestha 32 anniversary",
            bsYear = 2081,
            bsMonth = 2,
            bsDay = 32,
            hour = 9,
            minute = 0,
            recurrence = ReminderRecurrence.YEARLY_BS
        )

        val after2081 = epochMillisFor(BsDate(2081, 2, 32), hour = 9, minute = 1)
        val trigger2082 = NextReminderTriggerCalculator.computeNextTrigger(
            reminder = reminder,
            engine = engine,
            nowMillis = after2081,
            zoneId = kathmanduZone
        )
        assertNotNull(trigger2082)
        assertEquals(BsDate(2082, 2, 31), trigger2082.effectiveBsDate)
        assertEquals(epochMillisFor(BsDate(2082, 2, 31), hour = 9, minute = 0), trigger2082.triggerAtEpochMillis)

        // When rolling into BS 2085 (32-day Jestha), original day 32 is preserved.
        val after2084 = epochMillisFor(BsDate(2084, 2, 31), hour = 9, minute = 1)
        val trigger2085 = NextReminderTriggerCalculator.computeNextTrigger(
            reminder = reminder,
            engine = engine,
            nowMillis = after2084,
            zoneId = kathmanduZone
        )
        assertNotNull(trigger2085)
        assertEquals(BsDate(2085, 2, 32), trigger2085.effectiveBsDate)
        assertEquals(epochMillisFor(BsDate(2085, 2, 32), hour = 9, minute = 0), trigger2085.triggerAtEpochMillis)

        // Baisakh (month 1) drops from 31 days in BS 2085 to 30 days in BS 2086.
        assertEquals(31, engine.daysInMonth(2085, 1))
        assertEquals(30, engine.daysInMonth(2086, 1))
        val baisakh31Reminder = CalendarReminder(
            id = 8L,
            title = "Baisakh end",
            bsYear = 2085,
            bsMonth = 1,
            bsDay = 31,
            hour = 18,
            minute = 0,
            recurrence = ReminderRecurrence.YEARLY_BS
        )
        val afterBaisakh2085 = epochMillisFor(BsDate(2085, 1, 31), hour = 18, minute = 5)
        val trigger2086 = NextReminderTriggerCalculator.computeNextTrigger(
            reminder = baisakh31Reminder,
            engine = engine,
            nowMillis = afterBaisakh2085,
            zoneId = kathmanduZone
        )
        assertNotNull(trigger2086)
        assertEquals(BsDate(2086, 1, 30), trigger2086.effectiveBsDate)
        assertEquals(epochMillisFor(BsDate(2086, 1, 30), hour = 18, minute = 0), trigger2086.triggerAtEpochMillis)
    }

    private fun epochMillisFor(bsDate: BsDate, hour: Int, minute: Int): Long {
        val adDate = engine.bsToAd(bsDate)
        return ZonedDateTime.of(adDate, LocalTime.of(hour, minute), kathmanduZone).toInstant().toEpochMilli()
    }
}
