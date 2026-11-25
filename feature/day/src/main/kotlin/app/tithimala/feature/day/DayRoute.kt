package app.tithimala.feature.day

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun DayRoute(modifier: Modifier = Modifier) {
    DayScreen(modifier = modifier)
}

@Composable
internal fun DayScreen(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize())
}
