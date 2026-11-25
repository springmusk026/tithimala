package app.tithimala.feature.calendar

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.tithimala.core.designsystem.component.TithimalaBrandMark
import app.tithimala.core.designsystem.component.TithimalaSurfaceCard
import app.tithimala.core.designsystem.preview.TithimalaThemePreviews
import app.tithimala.core.designsystem.theme.TithimalaTheme

@Composable
fun CalendarRoute(modifier: Modifier = Modifier) {
    CalendarScreen(modifier = modifier)
}

@Composable
internal fun CalendarScreen(modifier: Modifier = Modifier) {
    val spacing = TithimalaTheme.spacing

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(spacing.md),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        TithimalaSurfaceCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                TithimalaBrandMark(
                    size = 72.dp,
                    contentDescription = stringResource(id = R.string.feature_calendar_brand_cd)
                )
                Spacer(modifier = Modifier.height(spacing.md))
                Text(
                    text = stringResource(id = R.string.feature_calendar_title),
                    style = MaterialTheme.typography.displaySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(spacing.xs))
                Text(
                    text = stringResource(id = R.string.feature_calendar_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@TithimalaThemePreviews
@Composable
private fun CalendarScreenPreview() {
    TithimalaTheme {
        CalendarScreen()
    }
}
