package app.tithimala.feature.converter

import android.content.ClipData
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import app.tithimala.core.designsystem.theme.TithimalaTheme
import app.tithimala.core.model.BsDate
import kotlinx.coroutines.launch

@Composable
fun ConverterRoute(
    modifier: Modifier = Modifier,
    onOpenDay: (BsDate) -> Unit = {},
    viewModel: ConverterViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val clipboard = LocalClipboard.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentOnOpenDay by rememberUpdatedState(onOpenDay)
    val clipboardLabel by rememberUpdatedState(stringResource(R.string.converter_clipboard_label))
    val copiedMessage by rememberUpdatedState(stringResource(R.string.converter_copied_snackbar))

    LaunchedEffect(viewModel, lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.effects.collect { effect ->
                when (effect) {
                    is ConverterEffect.CopyToClipboard -> {
                        val clipEntry = ClipEntry(ClipData.newPlainText(clipboardLabel, effect.text))
                        clipboard.setClipEntry(clipEntry)
                        launch {
                            snackbarHostState.currentSnackbarData?.dismiss()
                            snackbarHostState.showSnackbar(
                                message = copiedMessage,
                                duration = SnackbarDuration.Short
                            )
                        }
                    }
                    is ConverterEffect.OpenDay -> currentOnOpenDay(effect.bsDate)
                }
            }
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        ConverterScreen(
            state = uiState,
            onAction = viewModel::onAction,
            modifier = Modifier.fillMaxSize()
        )
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(TithimalaTheme.spacing.md)
        )
    }
}
