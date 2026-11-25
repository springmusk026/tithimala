package app.tithimala.core.data

import app.tithimala.core.database.ReminderDao
import app.tithimala.core.database.ReminderEntity
import app.tithimala.core.model.BsDate
import app.tithimala.core.model.BsYearMonth
import app.tithimala.core.model.CalendarReminder
import app.tithimala.core.model.ReminderRecurrence
import app.tithimala.core.notifications.ReminderAlarmScheduler
import app.tithimala.core.notifications.ReminderTriggerRequest
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/**
 * Offline repository managing Bikram Sambat reminders and keeping local alarms synchronized.
 */
interface ReminderRepository {
    fun observeAll(): Flow<List<CalendarReminder>>
    fun observeForDate(date: BsDate): Flow<List<CalendarReminder>>
    fun observeForMonth(yearMonth: BsYearMonth): Flow<List<CalendarReminder>>
    suspend fun getById(id: Long): CalendarReminder?
    suspend fun upsert(reminder: CalendarReminder): Long
    suspend fun setEnabled(id: Long, enabled: Boolean)
    suspend fun deleteById(id: Long)
    suspend fun rescheduleAllEnabled(nowMillis: Long = System.currentTimeMillis())
}

internal class DefaultReminderRepository @Inject constructor(
    private val reminderDao: ReminderDao,
    private val calendarRepository: CalendarRepository,
    private val alarmScheduler: ReminderAlarmScheduler,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val epochClock: EpochClock,
    private val zoneIdProvider: ZoneIdProvider
) : ReminderRepository {

    override fun observeAll(): Flow<List<CalendarReminder>> = reminderDao.observeAll()
        .map { entities -> entities.map(ReminderEntity::toDomain) }

    override fun observeForDate(date: BsDate): Flow<List<CalendarReminder>> = reminderDao.observeForDate(
        bsYear = date.year,
        bsMonth = date.month,
        bsDay = date.day
    ).map { entities -> entities.map(ReminderEntity::toDomain) }

    override fun observeForMonth(yearMonth: BsYearMonth): Flow<List<CalendarReminder>> = reminderDao.observeForMonth(
        bsYear = yearMonth.year,
        bsMonth = yearMonth.month
    ).map { entities -> entities.map(ReminderEntity::toDomain) }

    override suspend fun getById(id: Long): CalendarReminder? = withContext(ioDispatcher) {
        reminderDao.getById(id)?.toDomain()
    }

    override suspend fun upsert(reminder: CalendarReminder): Long = withContext(ioDispatcher) {
        val nowMillis = epochClock.nowMillis()
        val createdAt = if (reminder.id > 0L) {
            reminderDao.getById(reminder.id)?.createdAtEpochMillis ?: nowMillis
        } else {
            nowMillis
        }
        val persistedId = reminderDao.upsert(reminder.toEntity(createdAtEpochMillis = createdAt))
        val persisted = reminder.copy(id = persistedId)
        syncAlarm(persisted, nowMillis)
        persistedId
    }

    override suspend fun setEnabled(id: Long, enabled: Boolean) = withContext(ioDispatcher) {
        reminderDao.setEnabled(id, enabled)
        if (!enabled) {
            alarmScheduler.cancel(id)
        } else {
            val reminder = reminderDao.getById(id)?.toDomain()
            if (reminder != null) {
                syncAlarm(reminder, epochClock.nowMillis())
            } else {
                alarmScheduler.cancel(id)
            }
        }
    }

    override suspend fun deleteById(id: Long) = withContext(ioDispatcher) {
        alarmScheduler.cancel(id)
        reminderDao.deleteById(id)
    }

    override suspend fun rescheduleAllEnabled(nowMillis: Long) = withContext(ioDispatcher) {
        val enabledReminders = reminderDao.getEnabledReminders().map(ReminderEntity::toDomain)
        enabledReminders.forEach { reminder ->
            syncAlarm(reminder, nowMillis)
        }
    }

    private fun syncAlarm(reminder: CalendarReminder, nowMillis: Long) {
        if (!reminder.isEnabled) {
            alarmScheduler.cancel(reminder.id)
            return
        }
        val computed = NextReminderTriggerCalculator.computeNextTrigger(
            reminder = reminder,
            engine = calendarRepository.engine.value,
            nowMillis = nowMillis,
            zoneId = zoneIdProvider.currentZoneId()
        )
        if (computed == null) {
            alarmScheduler.cancel(reminder.id)
            return
        }
        alarmScheduler.schedule(
            ReminderTriggerRequest(
                reminderId = reminder.id,
                title = reminder.title,
                notes = reminder.notes,
                triggerAtEpochMillis = computed.triggerAtEpochMillis,
                isYearlyBs = reminder.recurrence == ReminderRecurrence.YEARLY_BS,
                bsYear = computed.effectiveBsDate.year,
                bsMonth = computed.effectiveBsDate.month,
                bsDay = computed.effectiveBsDate.day,
                hour = reminder.hour,
                minute = reminder.minute
            )
        )
    }
}

private fun ReminderEntity.toDomain(): CalendarReminder = CalendarReminder(
    id = id,
    title = title,
    notes = notes,
    bsYear = bsYear,
    bsMonth = bsMonth,
    bsDay = bsDay,
    hour = hour,
    minute = minute,
    recurrence = when (recurrenceType) {
        ReminderEntity.RECURRENCE_YEARLY_BS -> ReminderRecurrence.YEARLY_BS
        else -> ReminderRecurrence.ONE_TIME
    },
    isEnabled = isEnabled
)

private fun CalendarReminder.toEntity(createdAtEpochMillis: Long): ReminderEntity = ReminderEntity(
    id = id,
    title = title,
    notes = notes,
    bsYear = bsYear,
    bsMonth = bsMonth,
    bsDay = bsDay,
    hour = hour,
    minute = minute,
    recurrenceType = when (recurrence) {
        ReminderRecurrence.ONE_TIME -> ReminderEntity.RECURRENCE_ONE_TIME
        ReminderRecurrence.YEARLY_BS -> ReminderEntity.RECURRENCE_YEARLY_BS
    },
    isEnabled = isEnabled,
    createdAtEpochMillis = createdAtEpochMillis
)
