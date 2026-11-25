package app.tithimala.feature.settings

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.core.os.LocaleListCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import app.tithimala.core.calendarengine.BsDateFormatter
import app.tithimala.core.designsystem.preview.TithimalaThemePreviews
import app.tithimala.core.designsystem.theme.TithimalaTheme
import app.tithimala.core.model.AppLanguage
import app.tithimala.core.model.BsDate
import app.tithimala.core.model.NumeralScript
import app.tithimala.core.model.UserPreferences
import kotlinx.coroutines.launch

private const val KEY_HEADER = "settings_header"
private const val KEY_APPEARANCE = "settings_appearance"
private const val KEY_DATA_UPDATES = "settings_data_updates"
private const val KEY_ABOUT = "settings_about"

@Composable
fun SettingsRoute(
    modifier: Modifier = Modifier,
    onPreferencesChanged: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current
    val currentOnPreferencesChanged by rememberUpdatedState(onPreferencesChanged)
    val currentNumeralScript by rememberUpdatedState(uiState.preferences.numeralScript)

    LaunchedEffect(viewModel, lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.effects.collect { effect ->
                when (effect) {
                    is SettingsEffect.ApplyAppLocale -> {
                        applyApplicationLocale(effect.language)
                        currentOnPreferencesChanged()
                    }
                    SettingsEffect.NotifyPreferencesChanged -> {
                        currentOnPreferencesChanged()
                    }
                    is SettingsEffect.ShowRefreshFeedback -> {
                        val message = resolveRefreshFeedbackMessage(
                            context = context,
                            feedback = effect.feedback,
                            numeralScript = currentNumeralScript
                        )
                        launch {
                            snackbarHostState.currentSnackbarData?.dismiss()
                            snackbarHostState.showSnackbar(message = message)
                        }
                    }
                }
            }
        }
    }

    SettingsScreen(
        state = uiState,
        onAction = viewModel::onAction,
        modifier = modifier,
        snackbarHostState = snackbarHostState
    )
}

@Composable
internal fun SettingsScreen(
    state: SettingsUiState,
    onAction: (SettingsAction) -> Unit,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() }
) {
    val spacing = TithimalaTheme.spacing

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(
                horizontal = spacing.md,
                vertical = spacing.md
            ),
            verticalArrangement = Arrangement.spacedBy(spacing.lg)
        ) {
            item(key = KEY_HEADER) {
                SettingsScreenHeader()
            }
            item(key = KEY_APPEARANCE) {
                AppearanceAndLanguageSection(
                    preferences = state.preferences,
                    onAction = onAction
                )
            }
            item(key = KEY_DATA_UPDATES) {
                CalendarDataAndUpdatesSection(
                    dataset = state.dataset,
                    language = state.preferences.language,
                    numeralScript = state.preferences.numeralScript,
                    backgroundSyncEnabled = state.preferences.backgroundSyncEnabled,
                    isCheckingForUpdates = state.isCheckingForUpdates,
                    onAction = onAction
                )
            }
            item(key = KEY_ABOUT) {
                AboutAndSourcesSection(
                    onOpenLicenses = { onAction(SettingsAction.OpenLicensesDialog) }
                )
            }
        }
    }

    if (state.isLicensesDialogVisible) {
        DataSourcesLicensesDialog(
            dataset = state.dataset,
            noticeText = state.noticeText,
            onDismiss = { onAction(SettingsAction.DismissLicensesDialog) }
        )
    }
}

@Composable
private fun SettingsScreenHeader(modifier: Modifier = Modifier) {
    val spacing = TithimalaTheme.spacing

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(spacing.micro)
    ) {
        Text(
            text = stringResource(R.string.settings_screen_title),
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.semantics { heading() }
        )
        Text(
            text = stringResource(R.string.settings_screen_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun applyApplicationLocale(language: AppLanguage) {
    AppCompatDelegate.setApplicationLocales(
        LocaleListCompat.forLanguageTags(language.toLanguageTag())
    )
}

internal fun resolveRefreshFeedbackMessage(
    context: Context,
    feedback: SettingsRefreshFeedback,
    numeralScript: NumeralScript
): String = when (feedback) {
    is SettingsRefreshFeedback.Updated -> context.getString(
        R.string.settings_feedback_updated,
        BsDateFormatter.formatDigits(feedback.version, numeralScript)
    )
    is SettingsRefreshFeedback.UpToDate -> context.getString(R.string.settings_feedback_up_to_date)
    SettingsRefreshFeedback.OfflineFallback -> context.getString(R.string.settings_feedback_offline_fallback)
    SettingsRefreshFeedback.ChecksDisabled -> context.getString(R.string.settings_feedback_checks_disabled)
}

internal fun sampleSettingsUiState(
    preferences: UserPreferences = UserPreferences(),
    isCheckingForUpdates: Boolean = false,
    isLicensesDialogVisible: Boolean = false,
    noticeText: String = ""
): SettingsUiState = SettingsUiState(
    preferences = preferences,
    dataset = DatasetSummaryUiModel(
        version = "2026.10.30",
        upstreamRepo = "https://github.com/sanamhub/nepdate",
        upstreamCommitShort = "77adb44",
        license = "MIT",
        patroStartYear = 2077,
        patroEndYear = 2084,
        isRemoteUpdateActive = false,
        lastSyncBsDate = BsDate(year = 2083, month = 8, day = 10)
    ),
    isCheckingForUpdates = isCheckingForUpdates,
    isLicensesDialogVisible = isLicensesDialogVisible,
    noticeText = noticeText
)

@TithimalaThemePreviews
@Composable
private fun SettingsScreenPreview() {
    TithimalaTheme {
        SettingsScreen(
            state = sampleSettingsUiState(),
            onAction = {}
        )
    }
}
