package app.tithimala.feature.day

import android.annotation.SuppressLint
import android.content.ClipData
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.tithimala.core.model.BsDate

/**
 * Stateful Navigation 3 entry point for the Day Detail screen.
 */
@SuppressLint("ModifierParameter")
@Composable
fun DayRoute(
    bsDate: BsDate = BsDate(year = 2083, month = 8, day = 10),
    onBack: () -> Unit = {},
    onAddReminder: (BsDate) -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: DayViewModel = hiltViewModel(),
    sharedHeaderModifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val clipboard = LocalClipboard.current
    val currentOnAddReminder by rememberUpdatedState(onAddReminder)
    val clipboardLabel = stringResource(R.string.day_clipboard_label)
    val copiedMessage = stringResource(R.string.day_date_copied_snackbar)
    val currentClipboardLabel by rememberUpdatedState(clipboardLabel)
    val currentCopiedMessage by rememberUpdatedState(copiedMessage)

    LaunchedEffect(bsDate) {
        viewModel.loadDate(bsDate)
    }

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is DayEffect.OpenAddReminder -> currentOnAddReminder(effect.bsDate)
                is DayEffect.CopyToClipboard -> {
                    clipboard.setClipEntry(
                        ClipEntry(ClipData.newPlainText(currentClipboardLabel, effect.formattedText))
                    )
                    snackbarHostState.showSnackbar(
                        message = currentCopiedMessage,
                        withDismissAction = true
                    )
                }
            }
        }
    }

    DayScreen(
        state = uiState,
        onAction = viewModel::onAction,
        onBack = onBack,
        modifier = modifier,
        sharedHeaderModifier = sharedHeaderModifier,
        snackbarHostState = snackbarHostState
    )
}
