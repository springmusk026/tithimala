package app.tithimala.feature.converter

import app.cash.turbine.test
import app.tithimala.core.calendarengine.BikramSambatEngine
import app.tithimala.core.data.CalendarRefreshResult
import app.tithimala.core.data.CalendarRepository
import app.tithimala.core.data.UserPreferencesRepository
import app.tithimala.core.model.AppLanguage
import app.tithimala.core.model.BsDate
import app.tithimala.core.model.BsMonthGrid
import app.tithimala.core.model.DatasetMetadata
import app.tithimala.core.model.NumeralScript
import app.tithimala.core.model.PatroDayInfo
import app.tithimala.core.model.ThemeMode
import app.tithimala.core.model.UserPreferences
import app.tithimala.core.model.WeekStartDay
import app.tithimala.core.ui.calendar.LunarPhaseVisual
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestWatcher
import org.junit.runner.Description

@OptIn(ExperimentalCoroutinesApi::class)
class ConverterViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val engine = BikramSambatEngine.createBundled()

    @Test
    fun initializesToTodayBsDateWithLiveConversion() = runTest {
        val viewModel = createViewModel(todayAd = LocalDate.of(2026, 4, 14))
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }

        val state = viewModel.uiState.value
        assertEquals(ConversionDirection.BS_TO_AD, state.direction)
        assertEquals("2083", state.yearInput)
        assertEquals(1, state.monthIndex)
        assertEquals("1", state.dayInput)
        assertEquals(31, state.maxDayForCurrentMonth)
        assertNull(state.validationError)

        val result = assertNotNull(state.result)
        assertEquals(BsDate(2083, 1, 1), result.bsDate)
        assertEquals(LocalDate.of(2026, 4, 14), result.adDate)
        assertEquals("14 April 2026", result.primaryDateText)
        assertEquals("Baishakh 1, 2083", result.secondaryDateText)
        assertEquals("Tuesday", result.weekdayText)
        assertEquals("Dwadashi", result.tithiLabel)
        assertEquals(LunarPhaseVisual.WAXING_GIBBOUS, result.lunarPhase)
        assertFalse(result.isEstimatedTithi)
        assertTrue(result.isPublicHoliday)
        assertEquals("Mesh Sankranti", result.primaryEvent)
    }

    @Test
    fun convertsBsToAdForEpochNewYearAnd32DayMonth() = runTest {
        val viewModel = createViewModel()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }

        // 1901-01-01 BS -> 1844-04-11 AD (epoch start, outside Patro coverage)
        viewModel.onAction(ConverterAction.UpdateYear("1901"))
        viewModel.onAction(ConverterAction.SelectMonth(1))
        viewModel.onAction(ConverterAction.UpdateDay("1"))

        val epochState = viewModel.uiState.value
        assertNull(epochState.validationError)
        val epochResult = assertNotNull(epochState.result)
        assertEquals(BsDate(1901, 1, 1), epochResult.bsDate)
        assertEquals(LocalDate.of(1844, 4, 11), epochResult.adDate)
        assertEquals("11 April 1844", epochResult.primaryDateText)
        assertEquals("Thursday", epochResult.weekdayText)
        assertNull(epochResult.tithiLabel)
        assertNull(epochResult.lunarPhase)

        // 2083-01-01 BS -> 2026-04-14 AD
        viewModel.onAction(ConverterAction.UpdateYear("2083"))
        viewModel.onAction(ConverterAction.SelectMonth(1))
        viewModel.onAction(ConverterAction.UpdateDay("1"))

        val newYearResult = assertNotNull(viewModel.uiState.value.result)
        assertEquals(LocalDate.of(2026, 4, 14), newYearResult.adDate)
        assertTrue(newYearResult.isPublicHoliday)

        // 2081-04-32 BS (Shrawan has 32 days in 2081) -> 2024-08-16 AD
        viewModel.onAction(ConverterAction.UpdateYear("2081"))
        viewModel.onAction(ConverterAction.SelectMonth(4))
        viewModel.onAction(ConverterAction.UpdateDay("32"))

        val day32State = viewModel.uiState.value
        assertEquals(32, day32State.maxDayForCurrentMonth)
        assertNull(day32State.validationError)
        val day32Result = assertNotNull(day32State.result)
        assertEquals(BsDate(2081, 4, 32), day32Result.bsDate)
        assertEquals(LocalDate.of(2024, 8, 16), day32Result.adDate)
        assertEquals("16 August 2024", day32Result.primaryDateText)
    }

    @Test
    fun convertsAdToBsIncludingLeapDayAndRejectsNonLeapFeb29() = runTest {
        val viewModel = createViewModel()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }

        viewModel.onAction(ConverterAction.SetDirection(ConversionDirection.AD_TO_BS))

        // 2024-07-31 AD -> 2081-04-16 BS
        viewModel.onAction(ConverterAction.UpdateYear("2024"))
        viewModel.onAction(ConverterAction.SelectMonth(7))
        viewModel.onAction(ConverterAction.UpdateDay("31"))

        val julyState = viewModel.uiState.value
        assertNull(julyState.validationError)
        val julyResult = assertNotNull(julyState.result)
        assertEquals(BsDate(2081, 4, 16), julyResult.bsDate)
        assertEquals(LocalDate.of(2024, 7, 31), julyResult.adDate)
        assertEquals("Shrawan 16, 2081", julyResult.primaryDateText)
        assertEquals("31 July 2024", julyResult.secondaryDateText)
        assertEquals("Wednesday", julyResult.weekdayText)

        // 2024-02-29 AD (leap year) -> 2080-11-17 BS
        viewModel.onAction(ConverterAction.SelectMonth(2))
        viewModel.onAction(ConverterAction.UpdateDay("29"))

        val leapState = viewModel.uiState.value
        assertEquals(29, leapState.maxDayForCurrentMonth)
        assertNull(leapState.validationError)
        assertEquals(BsDate(2080, 11, 17), assertNotNull(leapState.result).bsDate)

        // 2023-02-29 AD (non-leap year) -> validation error
        viewModel.onAction(ConverterAction.UpdateYear("2023"))
        val nonLeapState = viewModel.uiState.value
        assertEquals(28, nonLeapState.maxDayForCurrentMonth)
        assertEquals(
            ConverterValidationError.InvalidDayForMonth(month = 2, year = 2023, maxDay = 28),
            nonLeapState.validationError
        )
        assertNull(nonLeapState.result)
    }

    @Test
    fun rejectsInvalidBsDayOutOfRangeYearsAndEmptyInputs() = runTest {
        val viewModel = createViewModel()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }

        // 2081-01-32 BS (Baishakh 2081 has 31 days)
        viewModel.onAction(ConverterAction.UpdateYear("2081"))
        viewModel.onAction(ConverterAction.SelectMonth(1))
        viewModel.onAction(ConverterAction.UpdateDay("32"))

        val invalidDayState = viewModel.uiState.value
        assertEquals(31, invalidDayState.maxDayForCurrentMonth)
        assertEquals(
            ConverterValidationError.InvalidDayForMonth(month = 1, year = 2081, maxDay = 31),
            invalidDayState.validationError
        )
        assertNull(invalidDayState.result)

        // Out-of-range year 1800
        viewModel.onAction(ConverterAction.UpdateYear("1800"))
        assertEquals(
            ConverterValidationError.YearOutOfRange(minYear = 1901, maxYear = 2199),
            viewModel.uiState.value.validationError
        )
        assertNull(viewModel.uiState.value.result)

        // Out-of-range year 2300
        viewModel.onAction(ConverterAction.UpdateYear("2300"))
        assertEquals(
            ConverterValidationError.YearOutOfRange(minYear = 1901, maxYear = 2199),
            viewModel.uiState.value.validationError
        )
        assertNull(viewModel.uiState.value.result)

        // Empty year and day
        viewModel.onAction(ConverterAction.UpdateYear(""))
        assertEquals(ConverterValidationError.EmptyYear, viewModel.uiState.value.validationError)

        viewModel.onAction(ConverterAction.UpdateDay(""))
        assertEquals(ConverterValidationError.EmptyInput, viewModel.uiState.value.validationError)

        // AD boundary year before minAdDate (1844-04-10 < 1844-04-11)
        viewModel.onAction(ConverterAction.SetDirection(ConversionDirection.AD_TO_BS))
        viewModel.onAction(ConverterAction.UpdateYear("1844"))
        viewModel.onAction(ConverterAction.SelectMonth(4))
        viewModel.onAction(ConverterAction.UpdateDay("10"))
        assertIs<ConverterValidationError.DateBeforeSupportedRange>(viewModel.uiState.value.validationError)
        assertNull(viewModel.uiState.value.result)
    }

    @Test
    fun swapDirectionPreservesConvertedDateAcrossRoundTrip() = runTest {
        val viewModel = createViewModel()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }

        viewModel.onAction(ConverterAction.UpdateYear("2081"))
        viewModel.onAction(ConverterAction.SelectMonth(4))
        viewModel.onAction(ConverterAction.UpdateDay("16"))
        assertEquals(LocalDate.of(2024, 7, 31), assertNotNull(viewModel.uiState.value.result).adDate)

        // Swap BS -> AD into AD -> BS: seeds 2024-07-31
        viewModel.onAction(ConverterAction.SwapDirection)
        val afterFirstSwap = viewModel.uiState.value
        assertEquals(ConversionDirection.AD_TO_BS, afterFirstSwap.direction)
        assertEquals("2024", afterFirstSwap.yearInput)
        assertEquals(7, afterFirstSwap.monthIndex)
        assertEquals("31", afterFirstSwap.dayInput)
        assertEquals(BsDate(2081, 4, 16), assertNotNull(afterFirstSwap.result).bsDate)

        // Swap back AD -> BS into BS -> AD: seeds 2081-04-16
        viewModel.onAction(ConverterAction.SwapDirection)
        val afterSecondSwap = viewModel.uiState.value
        assertEquals(ConversionDirection.BS_TO_AD, afterSecondSwap.direction)
        assertEquals("2081", afterSecondSwap.yearInput)
        assertEquals(4, afterSecondSwap.monthIndex)
        assertEquals("16", afterSecondSwap.dayInput)
        assertEquals(LocalDate.of(2024, 7, 31), assertNotNull(afterSecondSwap.result).adDate)
    }

    @Test
    fun stepDayResetToTodayAndDevanagariInputWorkCleanly() = runTest {
        val viewModel = createViewModel(todayAd = LocalDate.of(2026, 4, 14))
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }

        // Day 1 cannot decrement, can increment
        assertFalse(viewModel.uiState.value.canDecrementDay)
        assertTrue(viewModel.uiState.value.canIncrementDay)

        viewModel.onAction(ConverterAction.StepDay(1))
        assertEquals("2", viewModel.uiState.value.dayInput)
        assertTrue(viewModel.uiState.value.canDecrementDay)

        // Devanagari keyboard digits parse cleanly
        viewModel.onAction(ConverterAction.UpdateYear("२०८१"))
        viewModel.onAction(ConverterAction.SelectMonth(4))
        viewModel.onAction(ConverterAction.UpdateDay("३२"))
        assertEquals(LocalDate.of(2024, 8, 16), assertNotNull(viewModel.uiState.value.result).adDate)
        assertFalse(viewModel.uiState.value.canIncrementDay)

        // Reset to today restores 2083-01-01
        viewModel.onAction(ConverterAction.ResetToToday)
        assertEquals("2083", viewModel.uiState.value.yearInput)
        assertEquals(1, viewModel.uiState.value.monthIndex)
        assertEquals("1", viewModel.uiState.value.dayInput)
    }

    @Test
    fun copyAndOpenDayEffectsEmitOnlyWhenDateIsValid() = runTest {
        val viewModel = createViewModel(todayAd = LocalDate.of(2026, 4, 14))
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }

        viewModel.effects.test {
            viewModel.onAction(ConverterAction.CopyResult)
            val copyEffect = assertIs<ConverterEffect.CopyToClipboard>(awaitItem())
            assertEquals("Tuesday, 14 April 2026 (Baishakh 1, 2083)", copyEffect.text)

            viewModel.onAction(ConverterAction.OpenInCalendar)
            val openEffect = assertIs<ConverterEffect.OpenDay>(awaitItem())
            assertEquals(BsDate(2083, 1, 1), openEffect.bsDate)

            // Make input invalid: neither CopyResult nor OpenInCalendar should emit
            viewModel.onAction(ConverterAction.UpdateYear("1800"))
            viewModel.onAction(ConverterAction.CopyResult)
            viewModel.onAction(ConverterAction.OpenInCalendar)
            expectNoEvents()
        }
    }

    @Test
    fun nepaliPreferencesFormatDevanagariDatesAndLabels() = runTest {
        val prefsRepo = FakeUserPreferencesRepository(
            UserPreferences(
                language = AppLanguage.NEPALI,
                numeralScript = NumeralScript.DEVANAGARI
            )
        )
        val viewModel = createViewModel(
            todayAd = LocalDate.of(2026, 4, 14),
            preferencesRepository = prefsRepo
        )
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }

        val result = assertNotNull(viewModel.uiState.value.result)
        assertEquals("बैशाख १, २०८३", result.secondaryDateText)
        assertEquals("मङ्गलवार", result.weekdayText)
        assertEquals("द्वादशी", result.tithiLabel)
        assertEquals("मेष संक्रान्ति", result.primaryEvent)
        assertTrue(result.primaryDateText.contains("१४"))
        assertTrue(result.primaryDateText.contains("२०२६"))
    }

    private fun createViewModel(
        todayAd: LocalDate = LocalDate.of(2026, 4, 14),
        preferencesRepository: FakeUserPreferencesRepository = FakeUserPreferencesRepository(
            UserPreferences(
                language = AppLanguage.ENGLISH,
                numeralScript = NumeralScript.LATIN
            )
        )
    ): ConverterViewModel = ConverterViewModel.createForTest(
        calendarRepository = FakeCalendarRepository(engine),
        userPreferencesRepository = preferencesRepository,
        todayProvider = { todayAd }
    )

    private class FakeCalendarRepository(initialEngine: BikramSambatEngine) : CalendarRepository {
        private val _engine = MutableStateFlow(initialEngine)
        private val _datasetMetadata = MutableStateFlow(initialEngine.datasetMetadata)

        override val engine: StateFlow<BikramSambatEngine> = _engine.asStateFlow()
        override val datasetMetadata: StateFlow<DatasetMetadata> = _datasetMetadata.asStateFlow()

        override fun todayBs(now: LocalDate): BsDate = engine.value.adToBs(now)
        override fun bsToAd(bsDate: BsDate): LocalDate = engine.value.bsToAd(bsDate)
        override fun adToBs(adDate: LocalDate): BsDate = engine.value.adToBs(adDate)
        override fun buildMonthGrid(year: Int, month: Int, weekStart: WeekStartDay, todayBs: BsDate): BsMonthGrid =
            engine.value.buildMonthGrid(year = year, month = month, todayBs = todayBs, weekStart = weekStart)

        override fun patroInfo(bsDate: BsDate): PatroDayInfo = engine.value.patroInfo(bsDate)
        override fun noticeText(): String = engine.value.noticeText
        override suspend fun refreshRemoteDatasetNow(): CalendarRefreshResult =
            CalendarRefreshResult.UpToDate(engine.value.datasetMetadata.version)
    }

    private class FakeUserPreferencesRepository(initial: UserPreferences) : UserPreferencesRepository {
        private val _userPreferences = MutableStateFlow(initial)
        override val userPreferences: Flow<UserPreferences> = _userPreferences.asStateFlow()

        override suspend fun setLanguage(language: AppLanguage) {
            _userPreferences.value = _userPreferences.value.copy(language = language)
        }

        override suspend fun setNumeralScript(script: NumeralScript) {
            _userPreferences.value = _userPreferences.value.copy(numeralScript = script)
        }

        override suspend fun setThemeMode(themeMode: ThemeMode) {
            _userPreferences.value = _userPreferences.value.copy(themeMode = themeMode)
        }

        override suspend fun setWeekStartDay(weekStartDay: WeekStartDay) {
            _userPreferences.value = _userPreferences.value.copy(weekStartDay = weekStartDay)
        }

        override suspend fun setBackgroundSyncEnabled(enabled: Boolean) {
            _userPreferences.value = _userPreferences.value.copy(backgroundSyncEnabled = enabled)
        }
    }

    class MainDispatcherRule(private val dispatcher: TestDispatcher = UnconfinedTestDispatcher()) : TestWatcher() {
        override fun starting(description: Description) {
            Dispatchers.setMain(dispatcher)
        }

        override fun finished(description: Description) {
            Dispatchers.resetMain()
        }
    }
}
