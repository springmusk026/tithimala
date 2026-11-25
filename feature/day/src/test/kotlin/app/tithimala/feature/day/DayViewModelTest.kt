package app.tithimala.feature.day

import app.cash.turbine.test
import app.tithimala.core.calendarengine.BikramSambatEngine
import app.tithimala.core.data.CalendarRefreshResult
import app.tithimala.core.data.CalendarRepository
import app.tithimala.core.data.ReminderRepository
import app.tithimala.core.data.UserPreferencesRepository
import app.tithimala.core.model.AppLanguage
import app.tithimala.core.model.BilingualText
import app.tithimala.core.model.BsDate
import app.tithimala.core.model.BsMonthGrid
import app.tithimala.core.model.BsYearMonth
import app.tithimala.core.model.CalendarReminder
import app.tithimala.core.model.DatasetMetadata
import app.tithimala.core.model.NumeralScript
import app.tithimala.core.model.PatroDayInfo
import app.tithimala.core.model.ReminderRecurrence
import app.tithimala.core.model.ThemeMode
import app.tithimala.core.model.TithiSource
import app.tithimala.core.model.UserPreferences
import app.tithimala.core.model.WeekStartDay
import app.tithimala.core.ui.calendar.LunarPhaseVisual
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestWatcher
import org.junit.runner.Description

@OptIn(ExperimentalCoroutinesApi::class)
class DayViewModelTest {

    @get:Rule
    val mainDispatcherRule: TestWatcher = MainDispatcherRule()

    private val bundledEngine = BikramSambatEngine.createBundled()

    @Test
    fun loadDate_populatesBsAdDatesTithiHolidayAndOrderedEvents() = runTest {
        val calendarRepository = FakeCalendarRepository(
            bundledEngine = bundledEngine,
            fixedToday = BsDate(2081, 1, 1)
        )
        val preferencesRepository = FakeUserPreferencesRepository(
            UserPreferences(language = AppLanguage.ENGLISH, numeralScript = NumeralScript.LATIN)
        )
        val reminderRepository = FakeReminderRepository(
            listOf(
                CalendarReminder(
                    id = 1L,
                    title = "Morning puja",
                    notes = "Bring flowers",
                    bsYear = 2081,
                    bsMonth = 1,
                    bsDay = 1,
                    hour = 7,
                    minute = 30,
                    recurrence = ReminderRecurrence.YEARLY_BS,
                    isEnabled = true
                )
            )
        )
        val viewModel = DayViewModel(calendarRepository, preferencesRepository, reminderRepository)

        viewModel.uiState.test {
            assertEquals(DayUiState.LOADING, awaitItem())

            viewModel.loadDate(BsDate(2081, 1, 1))
            val state = awaitItem()

            assertFalse(state.isLoading)
            assertEquals(BsDate(2081, 1, 1), state.bsDate)
            assertEquals("1", state.bsDayNumber)
            assertEquals("Baishakh 2081", state.bsMonthYear)
            assertEquals("Saturday", state.weekdayLabel)
            assertEquals("13 April 2024", state.adDateLabel)
            assertEquals("Panchami", state.tithiLabel)
            assertEquals(TithiSource.VERIFIED, state.tithiSource)
            assertEquals(LunarPhaseVisual.FIRST_QUARTER, state.lunarPhase)
            assertTrue(state.isPublicHoliday)
            assertTrue(state.isSaturday)
            assertTrue(state.isToday)
            assertEquals(listOf("New Year", "Mesh Sankranti", "Bisket Jatra"), state.events)
            assertEquals(1, state.reminders.size)
            assertEquals("07:30", state.reminders.single().formattedTime)
            assertEquals(ReminderRecurrence.YEARLY_BS, state.reminders.single().recurrence)
            assertTrue(state.canNavigatePrevious)
            assertTrue(state.canNavigateNext)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun previousAndNextDayActions_navigateAcrossDatesAndUpdateReminders() = runTest {
        val calendarRepository = FakeCalendarRepository(bundledEngine)
        val preferencesRepository = FakeUserPreferencesRepository(
            UserPreferences(language = AppLanguage.ENGLISH, numeralScript = NumeralScript.LATIN)
        )
        val reminderRepository = FakeReminderRepository(
            listOf(
                CalendarReminder(
                    id = 10L,
                    title = "Baishakh 2 errand",
                    bsYear = 2081,
                    bsMonth = 1,
                    bsDay = 2,
                    hour = 10,
                    minute = 0
                )
            )
        )
        val viewModel = DayViewModel(calendarRepository, preferencesRepository, reminderRepository)

        viewModel.uiState.test {
            assertEquals(DayUiState.LOADING, awaitItem())

            viewModel.loadDate(BsDate(2081, 1, 1))
            val initial = awaitItem()
            assertEquals(BsDate(2081, 1, 1), initial.bsDate)
            assertTrue(initial.reminders.isEmpty())

            viewModel.onAction(DayAction.NextDay)
            val next = awaitItem()
            assertEquals(BsDate(2081, 1, 2), next.bsDate)
            assertEquals("Shashthi", next.tithiLabel)
            assertFalse(next.isPublicHoliday)
            assertTrue(next.events.isEmpty())
            assertEquals(listOf("Baishakh 2 errand"), next.reminders.map(DayReminderUiModel::title))

            viewModel.onAction(DayAction.PreviousDay)
            val backToNewYear = awaitItem()
            assertEquals(BsDate(2081, 1, 1), backToNewYear.bsDate)

            viewModel.onAction(DayAction.PreviousDay)
            val lastDayOf2080 = awaitItem()
            assertEquals(BsDate(2080, 12, 30), lastDayOf2080.bsDate)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun toggleAndDeleteReminderActions_updateRepositoryAndUiState() = runTest {
        val calendarRepository = FakeCalendarRepository(bundledEngine)
        val preferencesRepository = FakeUserPreferencesRepository(
            UserPreferences(language = AppLanguage.ENGLISH, numeralScript = NumeralScript.LATIN)
        )
        val reminderRepository = FakeReminderRepository(
            listOf(
                CalendarReminder(
                    id = 42L,
                    title = "Submit forms",
                    bsYear = 2081,
                    bsMonth = 2,
                    bsDay = 15,
                    hour = 9,
                    minute = 15,
                    isEnabled = true
                )
            )
        )
        val viewModel = DayViewModel(calendarRepository, preferencesRepository, reminderRepository)

        viewModel.uiState.test {
            assertEquals(DayUiState.LOADING, awaitItem())

            viewModel.loadDate(BsDate(2081, 2, 15))
            val loaded = awaitItem()
            assertTrue(loaded.reminders.single().isEnabled)

            viewModel.onAction(DayAction.ToggleReminder(id = 42L, enabled = false))
            val disabled = awaitItem()
            assertFalse(disabled.reminders.single().isEnabled)

            viewModel.onAction(DayAction.DeleteReminder(id = 42L))
            val deleted = awaitItem()
            assertTrue(deleted.reminders.isEmpty())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun outOfCoverageDate_exposesUnavailableTithiSourceAndValidAdConversion() = runTest {
        val calendarRepository = FakeCalendarRepository(bundledEngine)
        val preferencesRepository = FakeUserPreferencesRepository(
            UserPreferences(language = AppLanguage.ENGLISH, numeralScript = NumeralScript.LATIN)
        )
        val reminderRepository = FakeReminderRepository()
        val viewModel = DayViewModel(calendarRepository, preferencesRepository, reminderRepository)

        viewModel.uiState.test {
            assertEquals(DayUiState.LOADING, awaitItem())

            viewModel.loadDate(BsDate(2070, 6, 15))
            val state = awaitItem()

            assertEquals(BsDate(2070, 6, 15), state.bsDate)
            assertEquals("15", state.bsDayNumber)
            assertEquals("Ashoj 2070", state.bsMonthYear)
            assertTrue(state.adDateLabel.isNotEmpty())
            assertNull(state.tithiLabel)
            assertNull(state.lunarPhase)
            assertEquals(TithiSource.UNAVAILABLE, state.tithiSource)
            assertFalse(state.isPublicHoliday)
            assertTrue(state.events.isEmpty())
            assertTrue(state.canNavigatePrevious)
            assertTrue(state.canNavigateNext)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun estimatedTithiSource_preservesLabelPhaseAndEstimatedFlag() = runTest {
        val targetDate = BsDate(2085, 1, 1)
        val calendarRepository = FakeCalendarRepository(
            bundledEngine = bundledEngine,
            patroOverrides = mapOf(
                targetDate to PatroDayInfo(
                    tithi = BilingualText(np = "पूर्णिमा", en = "Purnima"),
                    tithiSource = TithiSource.ESTIMATED,
                    isPublicHoliday = false,
                    events = emptyList()
                )
            )
        )
        val preferencesRepository = FakeUserPreferencesRepository(
            UserPreferences(language = AppLanguage.ENGLISH, numeralScript = NumeralScript.LATIN)
        )
        val viewModel = DayViewModel(calendarRepository, preferencesRepository, FakeReminderRepository())

        viewModel.uiState.test {
            assertEquals(DayUiState.LOADING, awaitItem())
            viewModel.loadDate(targetDate)
            val state = awaitItem()

            assertEquals("Purnima", state.tithiLabel)
            assertEquals(TithiSource.ESTIMATED, state.tithiSource)
            assertEquals(LunarPhaseVisual.FULL_MOON, state.lunarPhase)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun verbatimTithiMarkers_areNeverStrippedInNepaliOrEnglish() = runTest {
        val markedDate = findFirstMarkedTithiDate(bundledEngine)
        val rawTithi = requireNotNull(bundledEngine.patroInfo(markedDate).tithi)
        assertTrue(rawTithi.np.endsWith("*"), "Expected trailing '*' marker in Nepali tithi: ${rawTithi.np}")
        assertTrue(rawTithi.en.endsWith(" (K)"), "Expected trailing ' (K)' marker in English tithi: ${rawTithi.en}")

        val calendarRepository = FakeCalendarRepository(bundledEngine)
        val preferencesRepository = FakeUserPreferencesRepository(
            UserPreferences(language = AppLanguage.ENGLISH, numeralScript = NumeralScript.LATIN)
        )
        val viewModel = DayViewModel(calendarRepository, preferencesRepository, FakeReminderRepository())

        viewModel.uiState.test {
            assertEquals(DayUiState.LOADING, awaitItem())

            viewModel.loadDate(markedDate)
            val englishState = awaitItem()
            assertEquals(rawTithi.en, englishState.tithiLabel)
            assertNotNull(englishState.lunarPhase)
            assertTrue(englishState.copyableDateText.contains(rawTithi.en))

            preferencesRepository.setLanguage(AppLanguage.NEPALI)
            preferencesRepository.setNumeralScript(NumeralScript.DEVANAGARI)
            val nepaliState = awaitItem()
            val finalNepaliState = if (nepaliState.tithiLabel == rawTithi.np) nepaliState else awaitItem()
            assertEquals(rawTithi.np, finalNepaliState.tithiLabel)
            assertNotNull(finalNepaliState.lunarPhase)
            assertTrue(finalNepaliState.copyableDateText.contains(rawTithi.np))
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun addReminderAndCopyDateActions_emitOneOffEffects() = runTest {
        val calendarRepository = FakeCalendarRepository(bundledEngine)
        val preferencesRepository = FakeUserPreferencesRepository(
            UserPreferences(language = AppLanguage.ENGLISH, numeralScript = NumeralScript.LATIN)
        )
        val viewModel = DayViewModel(calendarRepository, preferencesRepository, FakeReminderRepository())
        viewModel.loadDate(BsDate(2081, 1, 1))

        viewModel.effects.test {
            viewModel.onAction(DayAction.AddReminderClicked)
            assertEquals(DayEffect.OpenAddReminder(BsDate(2081, 1, 1)), awaitItem())

            viewModel.onAction(DayAction.CopyDateClicked)
            val copyEffect = awaitItem()
            assertTrue(copyEffect is DayEffect.CopyToClipboard)
            assertTrue(copyEffect.formattedText.contains("Baishakh 1, 2081"))
            assertTrue(copyEffect.formattedText.contains("13 April 2024"))
            assertTrue(copyEffect.formattedText.contains("Panchami"))
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun nepaliDevanagariPreferences_formatDatesEventsAndReminderTimes() = runTest {
        val calendarRepository = FakeCalendarRepository(bundledEngine)
        val preferencesRepository = FakeUserPreferencesRepository(
            UserPreferences(language = AppLanguage.NEPALI, numeralScript = NumeralScript.DEVANAGARI)
        )
        val reminderRepository = FakeReminderRepository(
            listOf(
                CalendarReminder(
                    id = 7L,
                    title = "बिहानको पूजा",
                    bsYear = 2080,
                    bsMonth = 11,
                    bsDay = 1,
                    hour = 6,
                    minute = 45
                )
            )
        )
        val viewModel = DayViewModel(calendarRepository, preferencesRepository, reminderRepository)

        viewModel.uiState.test {
            assertEquals(DayUiState.LOADING, awaitItem())
            viewModel.loadDate(BsDate(2080, 11, 1))
            advanceUntilIdle()
            val state = awaitItem()

            assertEquals("१", state.bsDayNumber)
            assertEquals("फागुन २०८०", state.bsMonthYear)
            assertEquals("०६:४५", state.reminders.single().formattedTime)
            assertEquals(5, state.events.size)
            cancelAndIgnoreRemainingEvents()
        }
    }

    private fun findFirstMarkedTithiDate(engine: BikramSambatEngine): BsDate {
        val coverage = engine.patroCoverage()
        val start = engine.bsToSerial(coverage.tithiFirstDay)
        val end = engine.bsToSerial(coverage.tithiLastDay)
        for (serial in start..end) {
            val candidate = engine.serialToBs(serial)
            val tithi = engine.patroInfo(candidate).tithi
            if (tithi != null && tithi.np.endsWith("*") && tithi.en.endsWith(" (K)")) {
                return candidate
            }
        }
        error("Expected bundled dataset to contain at least one marked tithi date")
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
private class MainDispatcherRule(private val testDispatcher: TestDispatcher = StandardTestDispatcher()) :
    TestWatcher() {
    override fun starting(description: Description) {
        Dispatchers.setMain(testDispatcher)
    }

    override fun finished(description: Description) {
        Dispatchers.resetMain()
    }
}

private class FakeCalendarRepository(
    bundledEngine: BikramSambatEngine,
    private val fixedToday: BsDate = BsDate(2083, 8, 10),
    private val patroOverrides: Map<BsDate, PatroDayInfo> = emptyMap()
) : CalendarRepository {
    private val _engine = MutableStateFlow(bundledEngine)
    private val _datasetMetadata = MutableStateFlow(bundledEngine.datasetMetadata)

    override val engine: StateFlow<BikramSambatEngine> = _engine.asStateFlow()
    override val datasetMetadata: StateFlow<DatasetMetadata> = _datasetMetadata.asStateFlow()

    override fun todayBs(now: LocalDate): BsDate = fixedToday

    override fun bsToAd(bsDate: BsDate): LocalDate = _engine.value.bsToAd(bsDate)

    override fun adToBs(adDate: LocalDate): BsDate = _engine.value.adToBs(adDate)

    override fun buildMonthGrid(year: Int, month: Int, weekStart: WeekStartDay, todayBs: BsDate): BsMonthGrid =
        _engine.value.buildMonthGrid(year = year, month = month, todayBs = todayBs, weekStart = weekStart)

    override fun patroInfo(bsDate: BsDate): PatroDayInfo = patroOverrides[bsDate] ?: _engine.value.patroInfo(bsDate)

    override fun noticeText(): String = _engine.value.noticeText

    override suspend fun refreshRemoteDatasetNow(): CalendarRefreshResult =
        CalendarRefreshResult.UpToDate(_engine.value.datasetMetadata.version)
}

private class FakeUserPreferencesRepository(initial: UserPreferences = UserPreferences()) : UserPreferencesRepository {
    private val state = MutableStateFlow(initial)
    override val userPreferences: Flow<UserPreferences> = state

    override suspend fun setLanguage(language: AppLanguage) {
        state.update { it.copy(language = language) }
    }

    override suspend fun setNumeralScript(script: NumeralScript) {
        state.update { it.copy(numeralScript = script) }
    }

    override suspend fun setThemeMode(themeMode: ThemeMode) {
        state.update { it.copy(themeMode = themeMode) }
    }

    override suspend fun setWeekStartDay(weekStartDay: WeekStartDay) {
        state.update { it.copy(weekStartDay = weekStartDay) }
    }

    override suspend fun setBackgroundSyncEnabled(enabled: Boolean) {
        state.update { it.copy(backgroundSyncEnabled = enabled) }
    }
}

private class FakeReminderRepository(initial: List<CalendarReminder> = emptyList()) : ReminderRepository {
    private val reminders = MutableStateFlow(initial)

    override fun observeAll(): Flow<List<CalendarReminder>> = reminders

    override fun observeForDate(date: BsDate): Flow<List<CalendarReminder>> = reminders.map { all ->
        all.filter { reminder ->
            when (reminder.recurrence) {
                ReminderRecurrence.ONE_TIME ->
                    reminder.bsYear == date.year && reminder.bsMonth == date.month && reminder.bsDay == date.day
                ReminderRecurrence.YEARLY_BS ->
                    reminder.bsYear <= date.year && reminder.bsMonth == date.month && reminder.bsDay == date.day
            }
        }.sortedWith(compareBy(CalendarReminder::hour, CalendarReminder::minute, CalendarReminder::id))
    }

    override fun observeForMonth(yearMonth: BsYearMonth): Flow<List<CalendarReminder>> = reminders.map { all ->
        all.filter { it.bsMonth == yearMonth.month }
    }

    override suspend fun getById(id: Long): CalendarReminder? = reminders.value.firstOrNull { it.id == id }

    override suspend fun upsert(reminder: CalendarReminder): Long {
        val assignedId = if (reminder.id > 0L) reminder.id else (reminders.value.maxOfOrNull { it.id } ?: 0L) + 1L
        val updated = reminder.copy(id = assignedId)
        reminders.update { current -> current.filterNot { it.id == assignedId } + updated }
        return assignedId
    }

    override suspend fun setEnabled(id: Long, enabled: Boolean) {
        reminders.update { current ->
            current.map { item -> if (item.id == id) item.copy(isEnabled = enabled) else item }
        }
    }

    override suspend fun deleteById(id: Long) {
        reminders.update { current -> current.filterNot { it.id == id } }
    }

    override suspend fun rescheduleAllEnabled(nowMillis: Long) = Unit
}
