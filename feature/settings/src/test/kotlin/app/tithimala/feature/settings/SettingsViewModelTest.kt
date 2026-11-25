package app.tithimala.feature.settings

import app.cash.turbine.test
import app.cash.turbine.turbineScope
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
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var preferencesRepository: FakeUserPreferencesRepository
    private lateinit var calendarRepository: FakeCalendarRepository
    private lateinit var viewModel: SettingsViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        preferencesRepository = FakeUserPreferencesRepository()
        calendarRepository = FakeCalendarRepository()
        viewModel = SettingsViewModel(
            userPreferencesRepository = preferencesRepository,
            calendarRepository = calendarRepository
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun loadsInitialPreferencesAndDatasetMetadata() = runTest(testDispatcher) {
        val bundled = BikramSambatEngine.createBundled()
        val syncEpochMillis = ZonedDateTime.of(
            bundled.bsToAd(BsDate(year = 2083, month = 8, day = 10)),
            LocalTime.NOON,
            ZoneId.systemDefault()
        ).toInstant().toEpochMilli()

        preferencesRepository.emit(
            UserPreferences(
                language = AppLanguage.NEPALI,
                numeralScript = NumeralScript.LATIN,
                themeMode = ThemeMode.DARK,
                weekStartDay = WeekStartDay.MONDAY,
                backgroundSyncEnabled = false
            )
        )
        calendarRepository.updateMetadata(
            bundled.datasetMetadata.copy(
                isRemoteUpdateActive = true,
                lastSyncEpochMillis = syncEpochMillis
            )
        )

        viewModel.uiState.test {
            awaitItem()
            val state = awaitItem()
            assertEquals(AppLanguage.NEPALI, state.preferences.language)
            assertEquals(NumeralScript.LATIN, state.preferences.numeralScript)
            assertEquals(ThemeMode.DARK, state.preferences.themeMode)
            assertEquals(WeekStartDay.MONDAY, state.preferences.weekStartDay)
            assertFalse(state.preferences.backgroundSyncEnabled)
            assertEquals(bundled.datasetMetadata.version, state.dataset.version)
            assertEquals(bundled.datasetMetadata.patroYears.min(), state.dataset.patroStartYear)
            assertEquals(bundled.datasetMetadata.patroYears.max(), state.dataset.patroEndYear)
            assertEquals(bundled.datasetMetadata.upstreamCommit.take(7), state.dataset.upstreamCommitShort)
            assertTrue(state.dataset.isRemoteUpdateActive)
            assertEquals(BsDate(year = 2083, month = 8, day = 10), state.dataset.lastSyncBsDate)
            assertEquals(bundled.noticeText, state.noticeText)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun changingLanguagePersistsAndEmitsApplyAppLocaleEffect() = runTest(testDispatcher) {
        assertEquals("", AppLanguage.SYSTEM.toLanguageTag())
        assertEquals("en", AppLanguage.ENGLISH.toLanguageTag())
        assertEquals("ne", AppLanguage.NEPALI.toLanguageTag())

        viewModel.effects.test {
            viewModel.onAction(SettingsAction.SelectLanguage(AppLanguage.NEPALI))
            assertEquals(SettingsEffect.ApplyAppLocale(AppLanguage.NEPALI), awaitItem())
            assertEquals(AppLanguage.NEPALI, preferencesRepository.current.language)

            viewModel.onAction(SettingsAction.SelectLanguage(AppLanguage.ENGLISH))
            assertEquals(SettingsEffect.ApplyAppLocale(AppLanguage.ENGLISH), awaitItem())
            assertEquals(AppLanguage.ENGLISH, preferencesRepository.current.language)

            viewModel.onAction(SettingsAction.SelectLanguage(AppLanguage.SYSTEM))
            assertEquals(SettingsEffect.ApplyAppLocale(AppLanguage.SYSTEM), awaitItem())
            assertEquals(AppLanguage.SYSTEM, preferencesRepository.current.language)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun changingDisplayAndSyncPreferencesPersistsAndNotifiesListeners() = runTest(testDispatcher) {
        viewModel.effects.test {
            viewModel.onAction(SettingsAction.SelectNumeralScript(NumeralScript.LATIN))
            assertEquals(SettingsEffect.NotifyPreferencesChanged, awaitItem())
            assertEquals(NumeralScript.LATIN, preferencesRepository.current.numeralScript)

            viewModel.onAction(SettingsAction.SelectThemeMode(ThemeMode.DARK))
            assertEquals(SettingsEffect.NotifyPreferencesChanged, awaitItem())
            assertEquals(ThemeMode.DARK, preferencesRepository.current.themeMode)

            viewModel.onAction(SettingsAction.SelectWeekStartDay(WeekStartDay.MONDAY))
            assertEquals(SettingsEffect.NotifyPreferencesChanged, awaitItem())
            assertEquals(WeekStartDay.MONDAY, preferencesRepository.current.weekStartDay)

            viewModel.onAction(SettingsAction.SetBackgroundSyncEnabled(false))
            assertEquals(SettingsEffect.NotifyPreferencesChanged, awaitItem())
            assertFalse(preferencesRepository.current.backgroundSyncEnabled)

            viewModel.onAction(SettingsAction.SetBackgroundSyncEnabled(true))
            assertEquals(SettingsEffect.NotifyPreferencesChanged, awaitItem())
            assertTrue(preferencesRepository.current.backgroundSyncEnabled)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun checkForUpdatesEmitsUpdatedFeedbackAndTracksCheckingState() = runTest(testDispatcher) {
        val pendingRefresh = CompletableDeferred<CalendarRefreshResult>()
        calendarRepository.onRefresh = { pendingRefresh.await() }

        turbineScope {
            val stateTurbine = viewModel.uiState.testIn(backgroundScope)
            val effectsTurbine = viewModel.effects.testIn(backgroundScope)

            assertFalse(stateTurbine.awaitItem().isCheckingForUpdates)

            viewModel.onAction(SettingsAction.CheckForUpdates)
            assertTrue(stateTurbine.awaitItem().isCheckingForUpdates)

            pendingRefresh.complete(CalendarRefreshResult.Updated(version = "2026.12.01"))
            assertEquals(
                SettingsEffect.ShowRefreshFeedback(SettingsRefreshFeedback.Updated("2026.12.01")),
                effectsTurbine.awaitItem()
            )
            assertEquals(SettingsEffect.NotifyPreferencesChanged, effectsTurbine.awaitItem())
            assertFalse(stateTurbine.awaitItem().isCheckingForUpdates)

            stateTurbine.cancelAndIgnoreRemainingEvents()
            effectsTurbine.cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun checkForUpdatesEmitsUpToDateFeedback() = runTest(testDispatcher) {
        calendarRepository.onRefresh = { CalendarRefreshResult.UpToDate(version = "2026.10.30") }

        viewModel.effects.test {
            viewModel.onAction(SettingsAction.CheckForUpdates)
            assertEquals(
                SettingsEffect.ShowRefreshFeedback(SettingsRefreshFeedback.UpToDate("2026.10.30")),
                awaitItem()
            )
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun checkForUpdatesEmitsOfflineFallbackFeedback() = runTest(testDispatcher) {
        calendarRepository.onRefresh = {
            CalendarRefreshResult.FallbackToBundled(
                reason = "Network unreachable",
                isTransientNetworkError = true
            )
        }

        viewModel.effects.test {
            viewModel.onAction(SettingsAction.CheckForUpdates)
            assertEquals(
                SettingsEffect.ShowRefreshFeedback(SettingsRefreshFeedback.OfflineFallback),
                awaitItem()
            )
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun checkForUpdatesEmitsChecksDisabledFeedbackWhenSkipped() = runTest(testDispatcher) {
        calendarRepository.onRefresh = { CalendarRefreshResult.SkippedDisabled }

        viewModel.effects.test {
            viewModel.onAction(SettingsAction.CheckForUpdates)
            assertEquals(
                SettingsEffect.ShowRefreshFeedback(SettingsRefreshFeedback.ChecksDisabled),
                awaitItem()
            )
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun licensesDialogStateOpensAndDismisses() = runTest(testDispatcher) {
        viewModel.uiState.test {
            assertFalse(awaitItem().isLicensesDialogVisible)

            viewModel.onAction(SettingsAction.OpenLicensesDialog)
            advanceUntilIdle()
            assertTrue(awaitItem().isLicensesDialogVisible)

            viewModel.onAction(SettingsAction.DismissLicensesDialog)
            advanceUntilIdle()
            assertFalse(awaitItem().isLicensesDialogVisible)

            cancelAndIgnoreRemainingEvents()
        }
    }

    private class FakeUserPreferencesRepository : UserPreferencesRepository {
        private val state = MutableStateFlow(UserPreferences())
        override val userPreferences: Flow<UserPreferences> = state

        val current: UserPreferences
            get() = state.value

        fun emit(preferences: UserPreferences) {
            state.value = preferences
        }

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

    private class FakeCalendarRepository : CalendarRepository {
        private val bundled = BikramSambatEngine.createBundled()
        private val metadataFlow = MutableStateFlow(bundled.datasetMetadata)

        var onRefresh: suspend () -> CalendarRefreshResult = {
            CalendarRefreshResult.UpToDate(metadataFlow.value.version)
        }

        override val engine: StateFlow<BikramSambatEngine> = MutableStateFlow(bundled)
        override val datasetMetadata: StateFlow<DatasetMetadata> = metadataFlow

        fun updateMetadata(metadata: DatasetMetadata) {
            metadataFlow.value = metadata
        }

        override fun todayBs(now: LocalDate): BsDate = bundled.adToBs(now)
        override fun bsToAd(bsDate: BsDate): LocalDate = bundled.bsToAd(bsDate)
        override fun adToBs(adDate: LocalDate): BsDate = bundled.adToBs(adDate)
        override fun buildMonthGrid(year: Int, month: Int, weekStart: WeekStartDay, todayBs: BsDate): BsMonthGrid =
            bundled.buildMonthGrid(year = year, month = month, todayBs = todayBs, weekStart = weekStart)

        override fun patroInfo(bsDate: BsDate): PatroDayInfo = bundled.patroInfo(bsDate)
        override fun noticeText(): String = bundled.noticeText
        override suspend fun refreshRemoteDatasetNow(): CalendarRefreshResult = onRefresh()
    }
}
