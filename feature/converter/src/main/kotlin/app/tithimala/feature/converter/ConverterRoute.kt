package app.tithimala.feature.converter

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun ConverterRoute(modifier: Modifier = Modifier) {
    ConverterScreen(modifier = modifier)
}

@Composable
internal fun ConverterScreen(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize())
}
