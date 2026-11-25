package app.tithimala.core.data

import android.app.Application
import android.content.Context
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.work.Configuration
import androidx.work.ListenableWorker
import androidx.work.NetworkType
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import androidx.work.testing.SynchronousExecutor
import androidx.work.testing.TestListenableWorkerBuilder
import androidx.work.testing.WorkManagerTestInitHelper
import app.tithimala.core.calendarengine.BikramSambatEngine
import app.tithimala.core.database.ReminderDao
import app.tithimala.core.database.ReminderEntity
import app.tithimala.core.datastore.CalendarSyncMetadata
import app.tithimala.core.datastore.UserPreferencesDataSource
import app.tithimala.core.model.AppLanguage
import app.tithimala.core.model.BsDate
import app.tithimala.core.model.BsMonthGrid
import app.tithimala.core.model.BsYearMonth
import app.tithimala.core.model.CalendarReminder
import app.tithimala.core.model.DatasetMetadata
import app.tithimala.core.model.NumeralScript
import app.tithimala.core.model.PatroDayInfo
import app.tithimala.core.model.ReminderRecurrence
import app.tithimala.core.model.ThemeMode
import app.tithimala.core.model.UserPreferences
import app.tithimala.core.model.WeekStartDay
import app.tithimala.core.notifications.ReminderAlarmScheduler
import app.tithimala.core.notifications.ReminderRescheduleReason
import app.tithimala.core.notifications.ReminderTriggerRequest
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [36])
class DataRepositoriesAndWorkersTest {

    private lateinit var application: Application
    private lateinit var workManager: WorkManager

    @Before
    fun setUp() {
        application = ApplicationProvider.getApplicationContext()
        WorkManagerTestInitHelper.initializeTestWorkManager(application)
        workManager = WorkManager.getInstance(application)
    }

    @After
    fun tearDown() {
        shadowOf(Looper.getMainLooper()).idle()
        WorkManagerTestInitHelper.closeWorkDatabase()
    }

    @Test
    fun syncSchedulerEnqueuesPeriodicConnectedBatteryFriendlyWorkAndCancelsWhenDisabled() {
        val scheduler = WorkManagerCalendarSyncScheduler(workManager)

        scheduler.syncWithPreference(enabled = true)
        val enqueued = workManager.getWorkInfosForUniqueWork(DataLayerConfig.CALENDAR_SYNC_WORK_NAME).get()
        assertEquals(1, enqueued.size)
        val workInfo = enqueued.single()
        assertEquals(WorkInfo.State.ENQUEUED, workInfo.state)
        assertEquals(NetworkType.CONNECTED, workInfo.constraints.requiredNetworkType)
        assertTrue(workInfo.constraints.requiresBatteryNotLow())

        scheduler.syncWithPreference(enabled = false)
        val afterCancel = workManager.getWorkInfosForUniqueWork(DataLayerConfig.CALENDAR_SYNC_WORK_NAME).get()
        assertTrue(afterCancel.all { it.state == WorkInfo.State.CANCELLED })
    }

    @Test
    fun userPreferencesRepositoryPersistsAndUpdatesSyncScheduler() = runTest {
        val dataSource = InMemoryPreferencesDataSource()
        val syncCalls = mutableListOf<Boolean>()
        val scheduler = object : CalendarSyncScheduler {
            override fun syncWithPreference(enabled: Boolean) {
                syncCalls += enabled
            }
        }
        val repository = DefaultUserPreferencesRepository(dataSource, scheduler)

        repository.setBackgroundSyncEnabled(false)
        repository.setBackgroundSyncEnabled(true)

        assertEquals(listOf(false, true), syncCalls)
        assertTrue(repository.userPreferences.first().backgroundSyncEnabled)
    }

    @Test
    fun calendarRefreshWorkerHandlesSuccessTransientRetryAndMaxAttemptsSilently() = runTest {
        val updatedWorker = buildRefreshWorker(CalendarRefreshResult.Updated("2026.11.01"), runAttemptCount = 0)
        assertEquals(ListenableWorker.Result.success(), updatedWorker.doWork())

        val validationFallbackWorker = buildRefreshWorker(
            CalendarRefreshResult.FallbackToBundled(reason = "SHA-256 mismatch", isTransientNetworkError = false),
            runAttemptCount = 0
        )
        assertEquals(ListenableWorker.Result.success(), validationFallbackWorker.doWork())

        val transientAttempt0 = buildRefreshWorker(
            CalendarRefreshResult.FallbackToBundled(reason = "HTTP 503", isTransientNetworkError = true),
            runAttemptCount = 0
        )
        assertEquals(ListenableWorker.Result.retry(), transientAttempt0.doWork())

        val transientAttemptMax = buildRefreshWorker(
            CalendarRefreshResult.FallbackToBundled(reason = "HTTP 503", isTransientNetworkError = true),
            runAttemptCount = CalendarRefreshWorker.MAX_RETRY_ATTEMPTS
        )
        assertEquals(ListenableWorker.Result.success(), transientAttemptMax.doWork())
    }

    @Test
    fun reminderRepositorySchedulesCancelsAndReschedulesAlarms() = runTest {
        val dao = InMemoryReminderDao()
        val calendarRepository = StubCalendarRepository(CalendarRefreshResult.UpToDate("2026.10.30"))
        val alarmScheduler = RecordingAlarmScheduler()
        val zone = ZoneId.of("Asia/Kathmandu")
        val engine = calendarRepository.engine.value
        val nowMillis = ZonedDateTime.of(
            engine.bsToAd(BsDate(2083, 1, 1)),
            LocalTime.of(8, 0),
            zone
        ).toInstant().toEpochMilli()

        val repository = DefaultReminderRepository(
            reminderDao = dao,
            calendarRepository = calendarRepository,
            alarmScheduler = alarmScheduler,
            ioDispatcher = Dispatchers.Unconfined,
            epochClock = EpochClock { nowMillis },
            zoneIdProvider = ZoneIdProvider { zone }
        )

        val id = repository.upsert(
            CalendarReminder(
                title = "New Year Call",
                notes = "Call family",
                bsYear = 2083,
                bsMonth = 1,
                bsDay = 1,
                hour = 9,
                minute = 0,
                recurrence = ReminderRecurrence.YEARLY_BS,
                isEnabled = true
            )
        )

        val scheduled = alarmScheduler.scheduledById[id]
        assertNotNull(scheduled)
        assertTrue(scheduled.isYearlyBs)
        assertEquals(2083, scheduled.bsYear)

        repository.setEnabled(id, enabled = false)
        assertTrue(alarmScheduler.cancelledIds.contains(id))
        assertFalse(repository.getById(id)!!.isEnabled)

        repository.setEnabled(id, enabled = true)
        assertNotNull(alarmScheduler.scheduledById[id])

        // Reschedule after 09:00 on 2083-01-01 advances yearly reminder to 2084-01-01.
        val afterFireMillis = ZonedDateTime.of(
            engine.bsToAd(BsDate(2083, 1, 1)),
            LocalTime.of(9, 1),
            zone
        ).toInstant().toEpochMilli()
        repository.rescheduleAllEnabled(nowMillis = afterFireMillis)
        assertEquals(2084, alarmScheduler.scheduledById[id]?.bsYear)

        repository.deleteById(id)
        assertEquals(null, repository.getById(id))
        assertTrue(alarmScheduler.cancelledIds.count { it == id } >= 2)
    }

    @Test
    fun rescheduleCallbackEnqueuesUniqueOneTimeWorkerAndWorkerReschedulesReminders() = runTest {
        var rescheduleInvocations = 0
        val stubReminderRepo = object : ReminderRepository {
            override fun observeAll(): Flow<List<CalendarReminder>> = MutableStateFlow(emptyList())
            override fun observeForDate(date: BsDate): Flow<List<CalendarReminder>> = MutableStateFlow(emptyList())
            override fun observeForMonth(yearMonth: BsYearMonth): Flow<List<CalendarReminder>> =
                MutableStateFlow(emptyList())
            override suspend fun getById(id: Long): CalendarReminder? = null
            override suspend fun upsert(reminder: CalendarReminder): Long = 0L
            override suspend fun setEnabled(id: Long, enabled: Boolean) = Unit
            override suspend fun deleteById(id: Long) = Unit
            override suspend fun rescheduleAllEnabled(nowMillis: Long) {
                rescheduleInvocations += 1
            }
        }
        val factory = object : WorkerFactory() {
            override fun createWorker(
                appContext: Context,
                workerClassName: String,
                workerParameters: WorkerParameters
            ): ListenableWorker? = if (workerClassName == RescheduleRemindersWorker::class.java.name) {
                RescheduleRemindersWorker(appContext, workerParameters, stubReminderRepo)
            } else {
                null
            }
        }

        WorkManagerTestInitHelper.closeWorkDatabase()
        WorkManagerTestInitHelper.initializeTestWorkManager(
            application,
            Configuration.Builder()
                .setExecutor(SynchronousExecutor())
                .setWorkerFactory(factory)
                .build()
        )
        workManager = WorkManager.getInstance(application)

        val callback = DataReminderRescheduleCallback()
        callback.onRescheduleRequested(application, ReminderRescheduleReason.BOOT_COMPLETED)
        shadowOf(Looper.getMainLooper()).idle()

        val infos = workManager.getWorkInfosForUniqueWork(DataLayerConfig.REMINDER_RESCHEDULE_WORK_NAME).get()
        assertEquals(1, infos.size)
        assertEquals(WorkInfo.State.SUCCEEDED, infos.single().state)
        assertEquals(1, rescheduleInvocations)

        // Also verify worker execution directly via TestListenableWorkerBuilder.
        val worker = TestListenableWorkerBuilder<RescheduleRemindersWorker>(application)
            .setWorkerFactory(factory)
            .build()
        assertEquals(ListenableWorker.Result.success(), worker.doWork())
        assertEquals(2, rescheduleInvocations)
    }

    private fun buildRefreshWorker(result: CalendarRefreshResult, runAttemptCount: Int): CalendarRefreshWorker {
        val repo = StubCalendarRepository(result)
        return TestListenableWorkerBuilder<CalendarRefreshWorker>(application)
            .setRunAttemptCount(runAttemptCount)
            .setWorkerFactory(
                object : WorkerFactory() {
                    override fun createWorker(
                        appContext: Context,
                        workerClassName: String,
                        workerParameters: WorkerParameters
                    ): ListenableWorker = CalendarRefreshWorker(appContext, workerParameters, repo)
                }
            )
            .build()
    }

    private class RecordingAlarmScheduler : ReminderAlarmScheduler {
        val scheduledById = linkedMapOf<Long, ReminderTriggerRequest>()
        val cancelledIds = mutableListOf<Long>()

        override fun schedule(request: ReminderTriggerRequest) {
            scheduledById[request.reminderId] = request
        }

        override fun cancel(reminderId: Long) {
            cancelledIds += reminderId
            scheduledById.remove(reminderId)
        }
    }

    private class InMemoryReminderDao : ReminderDao {
        private var nextId = 1L
        private val items = MutableStateFlow<List<ReminderEntity>>(emptyList())

        override fun observeAll(): Flow<List<ReminderEntity>> = items

        override fun observeForDate(bsYear: Int, bsMonth: Int, bsDay: Int): Flow<List<ReminderEntity>> =
            items.map { list ->
                list.filter { entity ->
                    val sameDay = entity.bsMonth == bsMonth && entity.bsDay == bsDay
                    val matchesYearly = entity.recurrenceType == ReminderEntity.RECURRENCE_YEARLY_BS &&
                        entity.bsYear <= bsYear
                    sameDay && (entity.bsYear == bsYear || matchesYearly)
                }
            }

        override fun observeForMonth(bsYear: Int, bsMonth: Int): Flow<List<ReminderEntity>> =
            items.map { list -> list.filter { it.bsMonth == bsMonth } }

        override suspend fun getEnabledReminders(): List<ReminderEntity> = items.value.filter { it.isEnabled }

        override suspend fun getById(id: Long): ReminderEntity? = items.value.firstOrNull { it.id == id }

        override suspend fun upsert(entity: ReminderEntity): Long {
            val assignedId = if (entity.id == 0L) nextId++ else entity.id
            val stored = entity.copy(id = assignedId)
            items.value = items.value.filterNot { it.id == assignedId } + stored
            return assignedId
        }

        override suspend fun setEnabled(id: Long, enabled: Boolean) {
            items.value = items.value.map { if (it.id == id) it.copy(isEnabled = enabled) else it }
        }

        override suspend fun deleteById(id: Long) {
            items.value = items.value.filterNot { it.id == id }
        }
    }

    private class InMemoryPreferencesDataSource : UserPreferencesDataSource {
        private val prefs = MutableStateFlow(UserPreferences())
        private val sync = MutableStateFlow(CalendarSyncMetadata())

        override val userPreferences: Flow<UserPreferences> = prefs
        override val syncMetadata: Flow<CalendarSyncMetadata> = sync

        override suspend fun setLanguage(language: AppLanguage) {
            prefs.value = prefs.value.copy(language = language)
        }

        override suspend fun setNumeralScript(script: NumeralScript) {
            prefs.value = prefs.value.copy(numeralScript = script)
        }

        override suspend fun setThemeMode(themeMode: ThemeMode) {
            prefs.value = prefs.value.copy(themeMode = themeMode)
        }

        override suspend fun setWeekStartDay(weekStartDay: WeekStartDay) {
            prefs.value = prefs.value.copy(weekStartDay = weekStartDay)
        }

        override suspend fun setBackgroundSyncEnabled(enabled: Boolean) {
            prefs.value = prefs.value.copy(backgroundSyncEnabled = enabled)
        }

        override suspend fun recordSyncResult(datasetVersion: String, syncedAtEpochMillis: Long) {
            sync.value = CalendarSyncMetadata(datasetVersion, syncedAtEpochMillis)
        }
    }

    private class StubCalendarRepository(private val refreshResult: CalendarRefreshResult) : CalendarRepository {
        private val bundled = BikramSambatEngine.createBundled()
        override val engine: StateFlow<BikramSambatEngine> = MutableStateFlow(bundled)
        override val datasetMetadata: StateFlow<DatasetMetadata> = MutableStateFlow(bundled.datasetMetadata)

        override fun todayBs(now: LocalDate): BsDate = bundled.adToBs(now)
        override fun bsToAd(bsDate: BsDate): LocalDate = bundled.bsToAd(bsDate)
        override fun adToBs(adDate: LocalDate): BsDate = bundled.adToBs(adDate)
        override fun buildMonthGrid(year: Int, month: Int, weekStart: WeekStartDay, todayBs: BsDate): BsMonthGrid =
            bundled.buildMonthGrid(year = year, month = month, todayBs = todayBs, weekStart = weekStart)

        override fun patroInfo(bsDate: BsDate): PatroDayInfo = bundled.patroInfo(bsDate)
        override fun noticeText(): String = bundled.noticeText
        override suspend fun refreshRemoteDatasetNow(): CalendarRefreshResult = refreshResult
    }
}
