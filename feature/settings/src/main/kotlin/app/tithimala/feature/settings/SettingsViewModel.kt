package app.tithimala.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.tithimala.core.data.CalendarRefreshResult
import app.tithimala.core.data.CalendarRepository
import app.tithimala.core.data.UserPreferencesRepository
import app.tithimala.core.model.AppLanguage
import app.tithimala.core.model.DatasetMetadata
import app.tithimala.core.model.NumeralScript
import app.tithimala.core.model.ThemeMode
import app.tithimala.core.model.WeekStartDay
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Instant
import java.time.ZoneId
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository,
    private val calendarRepository: CalendarRepository
) : ViewModel() {

    private val isCheckingForUpdates = MutableStateFlow(false)
    private val isLicensesDialogVisible = MutableStateFlow(false)

    private val _effects = Channel<SettingsEffect>(Channel.BUFFERED)
    internal val effects: Flow<SettingsEffect> = _effects.receiveAsFlow()

    internal val uiState: StateFlow<SettingsUiState> = combine(
        userPreferencesRepository.userPreferences,
        calendarRepository.datasetMetadata,
        isCheckingForUpdates,
        isLicensesDialogVisible
    ) { preferences, metadata, checking, showLicenses ->
        SettingsUiState(
            preferences = preferences,
            dataset = metadata.toSummaryUiModel(calendarRepository),
            isCheckingForUpdates = checking,
            isLicensesDialogVisible = showLicenses,
            noticeText = calendarRepository.noticeText()
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = SettingsUiState(
            dataset = calendarRepository.datasetMetadata.value.toSummaryUiModel(calendarRepository),
            noticeText = calendarRepository.noticeText()
        )
    )

    internal fun onAction(action: SettingsAction) {
        when (action) {
            is SettingsAction.SelectLanguage -> selectLanguage(action.language)
            is SettingsAction.SelectNumeralScript -> selectNumeralScript(action.script)
            is SettingsAction.SelectThemeMode -> selectThemeMode(action.themeMode)
            is SettingsAction.SelectWeekStartDay -> selectWeekStartDay(action.weekStartDay)
            is SettingsAction.SetBackgroundSyncEnabled -> setBackgroundSyncEnabled(action.enabled)
            SettingsAction.CheckForUpdates -> checkForUpdates()
            SettingsAction.OpenLicensesDialog -> isLicensesDialogVisible.value = true
            SettingsAction.DismissLicensesDialog -> isLicensesDialogVisible.value = false
        }
    }

    private fun selectLanguage(language: AppLanguage) {
        viewModelScope.launch {
            userPreferencesRepository.setLanguage(language)
            _effects.send(SettingsEffect.ApplyAppLocale(language))
        }
    }

    private fun selectNumeralScript(script: NumeralScript) {
        viewModelScope.launch {
            userPreferencesRepository.setNumeralScript(script)
            _effects.send(SettingsEffect.NotifyPreferencesChanged)
        }
    }

    private fun selectThemeMode(themeMode: ThemeMode) {
        viewModelScope.launch {
            userPreferencesRepository.setThemeMode(themeMode)
            _effects.send(SettingsEffect.NotifyPreferencesChanged)
        }
    }

    private fun selectWeekStartDay(weekStartDay: WeekStartDay) {
        viewModelScope.launch {
            userPreferencesRepository.setWeekStartDay(weekStartDay)
            _effects.send(SettingsEffect.NotifyPreferencesChanged)
        }
    }

    private fun setBackgroundSyncEnabled(enabled: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.setBackgroundSyncEnabled(enabled)
            _effects.send(SettingsEffect.NotifyPreferencesChanged)
        }
    }

    private fun checkForUpdates() {
        if (isCheckingForUpdates.value) return
        viewModelScope.launch {
            isCheckingForUpdates.value = true
            try {
                val result = calendarRepository.refreshRemoteDatasetNow()
                emitRefreshResult(result)
            } finally {
                isCheckingForUpdates.value = false
            }
        }
    }

    private suspend fun emitRefreshResult(result: CalendarRefreshResult) {
        when (result) {
            is CalendarRefreshResult.Updated -> {
                _effects.send(
                    SettingsEffect.ShowRefreshFeedback(
                        SettingsRefreshFeedback.Updated(result.version)
                    )
                )
                _effects.send(SettingsEffect.NotifyPreferencesChanged)
            }
            is CalendarRefreshResult.UpToDate -> {
                _effects.send(
                    SettingsEffect.ShowRefreshFeedback(
                        SettingsRefreshFeedback.UpToDate(result.version)
                    )
                )
            }
            CalendarRefreshResult.SkippedDisabled -> {
                _effects.send(
                    SettingsEffect.ShowRefreshFeedback(SettingsRefreshFeedback.ChecksDisabled)
                )
            }
            is CalendarRefreshResult.FallbackToBundled -> {
                _effects.send(
                    SettingsEffect.ShowRefreshFeedback(SettingsRefreshFeedback.OfflineFallback)
                )
            }
        }
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
        const val COMMIT_SHORT_LENGTH = 7

        fun DatasetMetadata.toSummaryUiModel(calendarRepository: CalendarRepository): DatasetSummaryUiModel {
            val sortedYears = patroYears.sorted()
            val lastSyncBsDate = lastSyncEpochMillis?.let { epochMillis ->
                val adDate = Instant.ofEpochMilli(epochMillis)
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate()
                runCatching { calendarRepository.adToBs(adDate) }.getOrNull()
            }
            return DatasetSummaryUiModel(
                version = version,
                upstreamRepo = upstreamRepo,
                upstreamCommitShort = upstreamCommit.take(COMMIT_SHORT_LENGTH),
                license = license,
                patroStartYear = sortedYears.firstOrNull(),
                patroEndYear = sortedYears.lastOrNull(),
                isRemoteUpdateActive = isRemoteUpdateActive,
                lastSyncBsDate = lastSyncBsDate,
                lastSyncEpochMillis = lastSyncEpochMillis
            )
        }
    }
}
