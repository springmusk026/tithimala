package app.tithimala.core.data

import app.tithimala.core.calendarengine.BikramSambatEngine
import app.tithimala.core.model.BsDate
import app.tithimala.core.model.CalendarReminder
import app.tithimala.core.model.ReminderRecurrence
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

internal data class ComputedReminderTrigger(val triggerAtEpochMillis: Long, val effectiveBsDate: BsDate)

internal object NextReminderTriggerCalculator {

    fun computeNextTriggerMillis(
        reminder: CalendarReminder,
        engine: BikramSambatEngine,
        nowMillis: Long,
        zoneId: ZoneId = ZoneId.systemDefault()
    ): Long? = computeNextTrigger(
        reminder = reminder,
        engine = engine,
        nowMillis = nowMillis,
        zoneId = zoneId
    )?.triggerAtEpochMillis

    fun computeNextTrigger(
        reminder: CalendarReminder,
        engine: BikramSambatEngine,
        nowMillis: Long,
        zoneId: ZoneId = ZoneId.systemDefault()
    ): ComputedReminderTrigger? = when (reminder.recurrence) {
        ReminderRecurrence.ONE_TIME -> computeOneTimeTrigger(reminder, engine, nowMillis, zoneId)
        ReminderRecurrence.YEARLY_BS -> computeYearlyBsTrigger(reminder, engine, nowMillis, zoneId)
    }

    private fun computeOneTimeTrigger(
        reminder: CalendarReminder,
        engine: BikramSambatEngine,
        nowMillis: Long,
        zoneId: ZoneId
    ): ComputedReminderTrigger? {
        if (!engine.isValidBsDate(reminder.bsYear, reminder.bsMonth, reminder.bsDay)) {
            return null
        }
        val targetDate = reminder.bsDate
        val triggerMillis = toEpochMillis(
            bsDate = targetDate,
            hour = reminder.hour,
            minute = reminder.minute,
            engine = engine,
            zoneId = zoneId
        )
        return if (triggerMillis > nowMillis) {
            ComputedReminderTrigger(triggerAtEpochMillis = triggerMillis, effectiveBsDate = targetDate)
        } else {
            null
        }
    }

    private fun computeYearlyBsTrigger(
        reminder: CalendarReminder,
        engine: BikramSambatEngine,
        nowMillis: Long,
        zoneId: ZoneId
    ): ComputedReminderTrigger? {
        val currentBsYear = resolveCurrentBsYear(engine, nowMillis, zoneId) ?: return null
        val startYear = maxOf(reminder.bsYear, currentBsYear)
        val maxYear = engine.maxBsDate.year
        var result: ComputedReminderTrigger? = null

        for (candidateYear in startYear..maxYear) {
            val clampedDay = minOf(reminder.bsDay, engine.daysInMonth(candidateYear, reminder.bsMonth))
            val effectiveDate = BsDate(year = candidateYear, month = reminder.bsMonth, day = clampedDay)
            val triggerMillis = toEpochMillis(
                bsDate = effectiveDate,
                hour = reminder.hour,
                minute = reminder.minute,
                engine = engine,
                zoneId = zoneId
            )
            if (triggerMillis > nowMillis) {
                result = ComputedReminderTrigger(
                    triggerAtEpochMillis = triggerMillis,
                    effectiveBsDate = effectiveDate
                )
                break
            }
        }
        return result
    }

    private fun resolveCurrentBsYear(engine: BikramSambatEngine, nowMillis: Long, zoneId: ZoneId): Int? {
        val nowAdDate = Instant.ofEpochMilli(nowMillis).atZone(zoneId).toLocalDate()
        return when {
            nowAdDate < engine.minAdDate -> engine.minBsDate.year
            nowAdDate > engine.maxAdDate -> null
            else -> engine.adToBs(nowAdDate).year
        }
    }

    private fun toEpochMillis(
        bsDate: BsDate,
        hour: Int,
        minute: Int,
        engine: BikramSambatEngine,
        zoneId: ZoneId
    ): Long {
        val adDate = engine.bsToAd(bsDate)
        return ZonedDateTime.of(adDate, LocalTime.of(hour, minute), zoneId).toInstant().toEpochMilli()
    }
}
