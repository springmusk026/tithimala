package app.tithimala.feature.calendar

import app.cash.turbine.test
import app.tithimala.core.calendarengine.BikramSambatEngine
import app.tithimala.core.data.CalendarRefreshResult
import app.tithimala.core.data.CalendarRepository
import app.tithimala.core.data.ReminderRepository
import app.tithimala.core.data.UserPreferencesRepository
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
import app.tithimala.core.ui.calendar.LunarPhaseVisual
import java.time.LocalDate
import java.util.Locale
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestWatcher
import org.junit.runner.Description

@OptIn(ExperimentalCoroutinesApi::class)
class CalendarViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val sharedEngine: BikramSambatEngine by lazy { BikramSambatEngine.createBundled() }

    @Test
    fun initialState_loadsTodayHeroCurrentMonthVisiblePagesAndFestivals() = runTest {
        val harness = createHarness(
            initialPreferences = UserPreferences(
                language = AppLanguage.ENGLISH,
                numeralScript = NumeralScript.LATIN,
                weekStartDay = WeekStartDay.SUNDAY
            )
        )
        val collectJob = startCollectingUiState(harness.viewModel)
        advanceUntilIdle()

        val state = harness.viewModel.uiState.value
        assertEquals(TODAY_BS, state.todayBs)
        assertEquals(BsYearMonth(2083, 8), state.currentYearMonth)
        assertEquals(TODAY_BS, state.selectedDate)
        assertTrue(state.isShowingTodayMonth)
        assertEquals(BsYearMonth(1901, 1), state.minYearMonth)
        assertEquals(BsYearMonth(2199, 12), state.maxYearMonth)
        assertEquals(TOTAL_SUPPORTED_MONTHS, state.pageCount)
        assertEquals(
            BsYearMonth(2083, 8),
            state.yearMonthForPage(state.pageIndexFor(BsYearMonth(2083, 8)))
        )
        assertEquals("Mangsir 2083", state.monthTitle)
        assertEquals("Nov/Dec 2026", state.adRangeSubtitle)
        assertEquals("8", state.todayHero.bsDayNumber)
        assertEquals("Mangsir 2083", state.todayHero.bsMonthYear)
        assertEquals("24 November 2026", state.todayHero.adDateLabel)
        assertEquals("Tuesday", state.todayHero.weekdayLabel)
        assertEquals("Purnima", state.todayHero.tithiLabel)
        assertEquals(LunarPhaseVisual.FULL_MOON, state.todayHero.lunarPhase)
        assertTrue(state.todayHero.isHoliday)
        assertEquals(
            setOf(
                BsYearMonth(2083, 6),
                BsYearMonth(2083, 7),
                BsYearMonth(2083, 8),
                BsYearMonth(2083, 9),
                BsYearMonth(2083, 10)
            ),
            state.visiblePages.keys
        )
        assertTrue(state.upcomingFestivals.any { it.bsDate == BsDate(2083, 8, 18) && it.isPublicHoliday })

        collectJob.cancel()
    }

    @Test
    fun nextAndPreviousMonth_updateStateAndEmitScrollEffects() = runTest {
        val harness = createHarness()
        val collectJob = startCollectingUiState(harness.viewModel)
        advanceUntilIdle()

        harness.viewModel.effects.test {
            harness.viewModel.onAction(CalendarAction.NextMonth)
            advanceUntilIdle()

            assertEquals(CalendarEffect.ScrollToMonth(BsYearMonth(2083, 9)), awaitItem())
            val nextState = harness.viewModel.uiState.value
            assertEquals(BsYearMonth(2083, 9), nextState.currentYearMonth)
            assertFalse(nextState.isShowingTodayMonth)
            assertTrue(BsYearMonth(2083, 9) in nextState.visiblePages)

            harness.viewModel.onAction(CalendarAction.PreviousMonth)
            advanceUntilIdle()

            assertEquals(CalendarEffect.ScrollToMonth(BsYearMonth(2083, 8)), awaitItem())
            val returnedState = harness.viewModel.uiState.value
            assertEquals(BsYearMonth(2083, 8), returnedState.currentYearMonth)
            assertTrue(returnedState.isShowingTodayMonth)
            cancelAndIgnoreRemainingEvents()
        }

        collectJob.cancel()
    }

    @Test
    fun monthNavigation_clampsAtDatasetBoundsWithoutEmittingEffect() = runTest {
        val harness = createHarness()
        val collectJob = startCollectingUiState(harness.viewModel)
        advanceUntilIdle()

        harness.viewModel.effects.test {
            harness.viewModel.onAction(CalendarAction.SelectMonth(BsYearMonth(1901, 1)))
            advanceUntilIdle()
            assertFalse(harness.viewModel.uiState.value.canGoPrevious)
            assertTrue(harness.viewModel.uiState.value.canGoNext)

            harness.viewModel.onAction(CalendarAction.PreviousMonth)
            advanceUntilIdle()
            assertEquals(BsYearMonth(1901, 1), harness.viewModel.uiState.value.currentYearMonth)
            expectNoEvents()

            harness.viewModel.onAction(CalendarAction.SelectMonth(BsYearMonth(2199, 12)))
            advanceUntilIdle()
            assertTrue(harness.viewModel.uiState.value.canGoPrevious)
            assertFalse(harness.viewModel.uiState.value.canGoNext)

            harness.viewModel.onAction(CalendarAction.NextMonth)
            advanceUntilIdle()
            assertEquals(BsYearMonth(2199, 12), harness.viewModel.uiState.value.currentYearMonth)
            expectNoEvents()
        }

        collectJob.cancel()
    }

    @Test
    fun jumpToToday_resetsCurrentMonthAndSelectedDateAndEmitsScrollEffect() = runTest {
        val harness = createHarness()
        val collectJob = startCollectingUiState(harness.viewModel)
        advanceUntilIdle()

        harness.viewModel.onAction(CalendarAction.SelectDay(BsDate(2084, 1, 15)))
        advanceUntilIdle()
        assertEquals(BsYearMonth(2084, 1), harness.viewModel.uiState.value.currentYearMonth)
        assertEquals(BsDate(2084, 1, 15), harness.viewModel.uiState.value.selectedDate)
        assertFalse(harness.viewModel.uiState.value.isShowingTodayMonth)

        harness.viewModel.effects.test {
            assertEquals(CalendarEffect.ScrollToMonth(BsYearMonth(2084, 1)), awaitItem())

            harness.viewModel.onAction(CalendarAction.JumpToToday)
            advanceUntilIdle()

            assertEquals(CalendarEffect.ScrollToMonth(BsYearMonth(2083, 8)), awaitItem())
            val state = harness.viewModel.uiState.value
            assertEquals(BsYearMonth(2083, 8), state.currentYearMonth)
            assertEquals(TODAY_BS, state.selectedDate)
            assertTrue(state.isShowingTodayMonth)
            cancelAndIgnoreRemainingEvents()
        }

        collectJob.cancel()
    }

    @Test
    fun openDayDetail_updatesSelectedDateAndEmitsNavigateEffect() = runTest {
        val harness = createHarness()
        val collectJob = startCollectingUiState(harness.viewModel)
        advanceUntilIdle()

        val targetDate = BsDate(2083, 8, 18)
        harness.viewModel.effects.test {
            harness.viewModel.onAction(CalendarAction.OpenDayDetail(targetDate))
            advanceUntilIdle()

            assertEquals(CalendarEffect.NavigateToDay(targetDate), awaitItem())
            val state = harness.viewModel.uiState.value
            assertEquals(targetDate, state.selectedDate)
            assertTrue(
                state.monthGrid.days.first { it.id == targetDate.toIsoString() }.isSelected
            )

            // Out-of-range edge overflow placeholder must be safely ignored.
            harness.viewModel.onAction(CalendarAction.OpenDayDetail(BsDate(1900, 12, 30)))
            advanceUntilIdle()
            assertEquals(targetDate, harness.viewModel.uiState.value.selectedDate)
            expectNoEvents()
        }

        collectJob.cancel()
    }

    @Test
    fun preferencesChange_weekStartAndNumeralScriptUpdateGridAndHeaders() = runTest {
        val harness = createHarness(
            initialPreferences = UserPreferences(
                language = AppLanguage.ENGLISH,
                numeralScript = NumeralScript.LATIN,
                weekStartDay = WeekStartDay.SUNDAY
            )
        )
        val collectJob = startCollectingUiState(harness.viewModel)
        advanceUntilIdle()

        val initialGrid = harness.viewModel.uiState.value.monthGrid
        assertEquals(listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"), initialGrid.weekdayHeaders)
        assertEquals(6, initialGrid.saturdayColumnIndex)
        assertEquals("8", harness.viewModel.uiState.value.todayHero.bsDayNumber)
        assertEquals("Mangsir 2083", harness.viewModel.uiState.value.monthTitle)

        harness.preferencesRepository.setWeekStartDay(WeekStartDay.MONDAY)
        advanceUntilIdle()

        val mondayGrid = harness.viewModel.uiState.value.monthGrid
        assertEquals(listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"), mondayGrid.weekdayHeaders)
        assertEquals(5, mondayGrid.saturdayColumnIndex)

        harness.preferencesRepository.setNumeralScript(NumeralScript.DEVANAGARI)
        harness.preferencesRepository.setLanguage(AppLanguage.NEPALI)
        advanceUntilIdle()

        val nepaliState = harness.viewModel.uiState.value
        assertEquals("८", nepaliState.todayHero.bsDayNumber)
        assertEquals("मंसिर २०८३", nepaliState.monthTitle)
        assertEquals("सोम", nepaliState.monthGrid.weekdayHeaders.first())
        assertEquals(
            "१८",
            nepaliState.monthGrid.days.first { it.id == "2083-08-18" }.bsDayText
        )

        collectJob.cancel()
    }

    @Test
    fun reminders_markEnabledOneTimeAndYearlyBsIndicatorsOnMatchingDates() = runTest {
        val harness = createHarness()
        val collectJob = startCollectingUiState(harness.viewModel)
        advanceUntilIdle()

        harness.reminderRepository.setReminders(
            listOf(
                CalendarReminder(
                    id = 1L,
                    title = "Doctor visit",
                    bsYear = 2083,
                    bsMonth = 8,
                    bsDay = 14,
                    recurrence = ReminderRecurrence.ONE_TIME,
                    isEnabled = true
                ),
                CalendarReminder(
                    id = 2L,
                    title = "Disabled reminder",
                    bsYear = 2083,
                    bsMonth = 8,
                    bsDay = 15,
                    recurrence = ReminderRecurrence.ONE_TIME,
                    isEnabled = false
                ),
                CalendarReminder(
                    id = 3L,
                    title = "Annual family gathering",
                    bsYear = 2080,
                    bsMonth = 8,
                    bsDay = 21,
                    recurrence = ReminderRecurrence.YEARLY_BS,
                    isEnabled = true
                )
            )
        )
        advanceUntilIdle()

        val mangsir2083Days = harness.viewModel.uiState.value.monthGrid.days.associateBy { it.id }
        assertTrue(mangsir2083Days.getValue("2083-08-14").hasReminders)
        assertFalse(mangsir2083Days.getValue("2083-08-15").hasReminders)
        assertTrue(mangsir2083Days.getValue("2083-08-21").hasReminders)

        harness.viewModel.onAction(CalendarAction.SelectMonth(BsYearMonth(2084, 8)))
        advanceUntilIdle()

        val mangsir2084Days = harness.viewModel.uiState.value.monthGrid.days.associateBy { it.id }
        assertFalse(mangsir2084Days.getValue("2084-08-14").hasReminders)
        assertTrue(mangsir2084Days.getValue("2084-08-21").hasReminders)

        collectJob.cancel()
    }

    private fun TestScope.startCollectingUiState(viewModel: CalendarViewModel) = backgroundScope.launch {
        viewModel.uiState.collect {}
    }

    private fun createHarness(
        initialPreferences: UserPreferences = UserPreferences(
            language = AppLanguage.ENGLISH,
            numeralScript = NumeralScript.LATIN,
            weekStartDay = WeekStartDay.SUNDAY
        ),
        systemLocale: Locale = Locale.ENGLISH
    ): TestHarness {
        val calendarRepository = FakeCalendarRepository(sharedEngine)
        val preferencesRepository = FakeUserPreferencesRepository(initialPreferences)
        val reminderRepository = FakeReminderRepository()
        val viewModel = CalendarViewModel(
            calendarRepository = calendarRepository,
            userPreferencesRepository = preferencesRepository,
            reminderRepository = reminderRepository,
            todayProvider = { TODAY_AD },
            systemLocaleProvider = { systemLocale }
        )
        return TestHarness(
            viewModel = viewModel,
            preferencesRepository = preferencesRepository,
            reminderRepository = reminderRepository
        )
    }

    private data class TestHarness(
        val viewModel: CalendarViewModel,
        val preferencesRepository: FakeUserPreferencesRepository,
        val reminderRepository: FakeReminderRepository
    )

    private class FakeCalendarRepository(engine: BikramSambatEngine) : CalendarRepository {
        private val engineFlow = MutableStateFlow(engine)
        private val metadataFlow = MutableStateFlow(engine.datasetMetadata)

        override val engine: StateFlow<BikramSambatEngine> = engineFlow.asStateFlow()
        override val datasetMetadata: StateFlow<DatasetMetadata> = metadataFlow.asStateFlow()

        override fun todayBs(now: LocalDate): BsDate = engineFlow.value.adToBs(now)
        override fun bsToAd(bsDate: BsDate): LocalDate = engineFlow.value.bsToAd(bsDate)
        override fun adToBs(adDate: LocalDate): BsDate = engineFlow.value.adToBs(adDate)
        override fun buildMonthGrid(year: Int, month: Int, weekStart: WeekStartDay, todayBs: BsDate): BsMonthGrid =
            engineFlow.value.buildMonthGrid(year = year, month = month, todayBs = todayBs, weekStart = weekStart)

        override fun patroInfo(bsDate: BsDate): PatroDayInfo = engineFlow.value.patroInfo(bsDate)
        override fun noticeText(): String = engineFlow.value.noticeText
        override suspend fun refreshRemoteDatasetNow(): CalendarRefreshResult = CalendarRefreshResult.SkippedDisabled
    }

    private class FakeUserPreferencesRepository(initial: UserPreferences) : UserPreferencesRepository {
        private val state = MutableStateFlow(initial)
        override val userPreferences: Flow<UserPreferences> = state.asStateFlow()

        override suspend fun setLanguage(language: AppLanguage) {
            state.value = state.value.copy(language = language)
        }

        override suspend fun setNumeralScript(script: NumeralScript) {
            state.value = state.value.copy(numeralScript = script)
        }

        override suspend fun setThemeMode(themeMode: ThemeMode) {
            state.value = state.value.copy(themeMode = themeMode)
        }

        override suspend fun setWeekStartDay(weekStartDay: WeekStartDay) {
            state.value = state.value.copy(weekStartDay = weekStartDay)
        }

        override suspend fun setBackgroundSyncEnabled(enabled: Boolean) {
            state.value = state.value.copy(backgroundSyncEnabled = enabled)
        }
    }

    private class FakeReminderRepository : ReminderRepository {
        private val state = MutableStateFlow<List<CalendarReminder>>(emptyList())

        fun setReminders(reminders: List<CalendarReminder>) {
            state.value = reminders
        }

        override fun observeAll(): Flow<List<CalendarReminder>> = state.asStateFlow()
        override fun observeForDate(date: BsDate): Flow<List<CalendarReminder>> =
            state.map { list -> list.filter { it.bsDate == date } }

        override fun observeForMonth(yearMonth: BsYearMonth): Flow<List<CalendarReminder>> =
            state.map { list -> list.filter { it.bsYear == yearMonth.year && it.bsMonth == yearMonth.month } }

        override suspend fun getById(id: Long): CalendarReminder? = state.value.firstOrNull { it.id == id }
        override suspend fun upsert(reminder: CalendarReminder): Long = reminder.id
        override suspend fun setEnabled(id: Long, enabled: Boolean) = Unit
        override suspend fun deleteById(id: Long) = Unit
        override suspend fun rescheduleAllEnabled(nowMillis: Long) = Unit
    }

    private companion object {
        val TODAY_AD: LocalDate = LocalDate.of(2026, 11, 24)
        val TODAY_BS: BsDate = BsDate(year = 2083, month = 8, day = 8)
        const val TOTAL_SUPPORTED_MONTHS = 3_588
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class MainDispatcherRule(private val testDispatcher: TestDispatcher = StandardTestDispatcher()) : TestWatcher() {
    override fun starting(description: Description) {
        Dispatchers.setMain(testDispatcher)
    }

    override fun finished(description: Description) {
        Dispatchers.resetMain()
    }
}
