package app.tithimala.core.ui.calendar

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.CollectionInfo
import androidx.compose.ui.semantics.collectionInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import app.tithimala.core.designsystem.component.TithimalaSurfaceCard
import app.tithimala.core.designsystem.preview.TithimalaThemePreviews
import app.tithimala.core.designsystem.theme.TithimalaTheme

private const val DAYS_PER_WEEK = 7

/**
 * Immutable UI model representing a Bikram Sambat calendar month matrix.
 */
@Immutable
data class MonthGridUiModel(
    val weekdayHeaders: List<String>,
    val saturdayColumnIndex: Int,
    val days: List<DayCellUiModel>
)

/**
 * Seven-column month calendar card with sentence-case weekday headers (highlighting Saturday in
 * Sindoor) and nested 8dp [DayCell] tiles inside a 16dp [TithimalaSurfaceCard].
 */
@Composable
fun MonthGrid(grid: MonthGridUiModel, onDayClick: (DayCellUiModel) -> Unit, modifier: Modifier = Modifier) {
    val spacing = TithimalaTheme.spacing
    val extendedColors = TithimalaTheme.colors
    val weeks = remember(grid.days) { grid.days.chunked(DAYS_PER_WEEK) }

    TithimalaSurfaceCard(
        modifier = modifier
            .fillMaxWidth()
            .semantics {
                collectionInfo = CollectionInfo(
                    rowCount = weeks.size,
                    columnCount = DAYS_PER_WEEK
                )
            },
        shape = MaterialTheme.shapes.large,
        contentPadding = PaddingValues(spacing.xs)
    ) {
        WeekdayHeaderRow(
            weekdayHeaders = grid.weekdayHeaders,
            saturdayColumnIndex = grid.saturdayColumnIndex
        )
        HorizontalDivider(
            thickness = spacing.hairline,
            color = extendedColors.hairline
        )
        Spacer(modifier = Modifier.height(spacing.micro))
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(spacing.micro)
        ) {
            weeks.forEach { week ->
                WeekRow(
                    week = week,
                    onDayClick = onDayClick
                )
            }
        }
    }
}

@Composable
private fun WeekdayHeaderRow(weekdayHeaders: List<String>, saturdayColumnIndex: Int) {
    val spacing = TithimalaTheme.spacing
    val extendedColors = TithimalaTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = spacing.xs),
        horizontalArrangement = Arrangement.spacedBy(spacing.micro)
    ) {
        weekdayHeaders.forEachIndexed { index, label ->
            val labelColor = if (index == saturdayColumnIndex) {
                extendedColors.saturday
            } else {
                extendedColors.textMuted
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = labelColor,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun WeekRow(week: List<DayCellUiModel>, onDayClick: (DayCellUiModel) -> Unit) {
    val spacing = TithimalaTheme.spacing
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(spacing.micro)
    ) {
        week.forEach { day ->
            key(day.id) {
                DayCell(
                    day = day,
                    onClick = { onDayClick(day) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
        repeat(DAYS_PER_WEEK - week.size) {
            Spacer(modifier = Modifier.weight(1f))
        }
    }
}

@TithimalaThemePreviews
@Composable
private fun MonthGridPreview() {
    TithimalaTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            MonthGrid(
                grid = sampleMonthGridUiModel(),
                onDayClick = {},
                modifier = Modifier.padding(TithimalaTheme.spacing.md)
            )
        }
    }
}
