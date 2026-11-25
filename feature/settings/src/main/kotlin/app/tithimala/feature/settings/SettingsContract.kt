package app.tithimala.feature.settings

import androidx.compose.runtime.Immutable
import app.tithimala.core.model.AppLanguage
import app.tithimala.core.model.BsDate
import app.tithimala.core.model.NumeralScript
import app.tithimala.core.model.ThemeMode
import app.tithimala.core.model.UserPreferences
import app.tithimala.core.model.WeekStartDay

@Immutable
internal data class DatasetSummaryUiModel(
    val version: String = "",
    val upstreamRepo: String = "",
    val upstreamCommitShort: String = "",
    val license: String = "",
    val patroStartYear: Int? = null,
    val patroEndYear: Int? = null,
    val isRemoteUpdateActive: Boolean = false,
    val lastSyncBsDate: BsDate? = null,
    val lastSyncEpochMillis: Long? = null
)

@Immutable
internal data class SettingsUiState(
    val preferences: UserPreferences = UserPreferences(),
    val dataset: DatasetSummaryUiModel = DatasetSummaryUiModel(),
    val isCheckingForUpdates: Boolean = false,
    val isLicensesDialogVisible: Boolean = false,
    val noticeText: String = ""
)

internal sealed interface SettingsAction {
    data class SelectLanguage(val language: AppLanguage) : SettingsAction
    data class SelectNumeralScript(val script: NumeralScript) : SettingsAction
    data class SelectThemeMode(val themeMode: ThemeMode) : SettingsAction
    data class SelectWeekStartDay(val weekStartDay: WeekStartDay) : SettingsAction
    data class SetBackgroundSyncEnabled(val enabled: Boolean) : SettingsAction
    data object CheckForUpdates : SettingsAction
    data object OpenLicensesDialog : SettingsAction
    data object DismissLicensesDialog : SettingsAction
}

internal sealed interface SettingsRefreshFeedback {
    data class Updated(val version: String) : SettingsRefreshFeedback
    data class UpToDate(val version: String) : SettingsRefreshFeedback
    data object OfflineFallback : SettingsRefreshFeedback
    data object ChecksDisabled : SettingsRefreshFeedback
}

internal sealed interface SettingsEffect {
    data class ApplyAppLocale(val language: AppLanguage) : SettingsEffect
    data object NotifyPreferencesChanged : SettingsEffect
    data class ShowRefreshFeedback(val feedback: SettingsRefreshFeedback) : SettingsEffect
}

internal fun AppLanguage.toLanguageTag(): String = when (this) {
    AppLanguage.SYSTEM -> ""
    AppLanguage.ENGLISH -> "en"
    AppLanguage.NEPALI -> "ne"
}
