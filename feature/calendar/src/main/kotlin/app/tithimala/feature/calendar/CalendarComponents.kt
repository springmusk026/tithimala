package app.tithimala.feature.calendar

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.tithimala.core.designsystem.component.TithimalaSurfaceCard
import app.tithimala.core.designsystem.theme.TithimalaTheme

private val LEGEND_DOT_SIZE = 6.dp
private val CHEVRON_ICON_SIZE = 18.dp
private val NAV_ICON_SIZE = 24.dp
private const val MAX_TITLE_LINES = 2
private const val DISABLED_ICON_ALPHA = 0.38f

@Composable
internal fun TodayJumpButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val spacing = TithimalaTheme.spacing
    OutlinedButton(
        onClick = onClick,
        shape = MaterialTheme.shapes.small,
        border = BorderStroke(spacing.hairline, TithimalaTheme.colors.hairline),
        contentPadding = PaddingValues(horizontal = spacing.sm, vertical = spacing.xs),
        modifier = modifier.heightIn(min = spacing.minTouchTarget)
    ) {
        Text(
            text = stringResource(R.string.feature_calendar_action_today),
            style = MaterialTheme.typography.labelLarge
        )
    }
}

@Composable
internal fun MonthNavigationHeader(
    monthTitle: String,
    adRangeSubtitle: String,
    canGoPrevious: Boolean,
    canGoNext: Boolean,
    onPreviousClick: () -> Unit,
    onNextClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val spacing = TithimalaTheme.spacing
    val onSurface = MaterialTheme.colorScheme.onSurface
    val disabledTint = onSurface.copy(alpha = DISABLED_ICON_ALPHA)

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(spacing.xs)
    ) {
        IconButton(
            onClick = onPreviousClick,
            enabled = canGoPrevious,
            modifier = Modifier.size(spacing.minTouchTarget)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
                contentDescription = stringResource(R.string.feature_calendar_previous_month_cd),
                tint = if (canGoPrevious) onSurface else disabledTint,
                modifier = Modifier.size(NAV_ICON_SIZE)
            )
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .semantics(mergeDescendants = true) { heading() },
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = monthTitle,
                style = MaterialTheme.typography.headlineSmall,
                color = onSurface,
                textAlign = TextAlign.Center,
                maxLines = MAX_TITLE_LINES,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = adRangeSubtitle,
                style = MaterialTheme.typography.bodySmall,
                color = TithimalaTheme.colors.textMuted,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        IconButton(
            onClick = onNextClick,
            enabled = canGoNext,
            modifier = Modifier.size(spacing.minTouchTarget)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                contentDescription = stringResource(R.string.feature_calendar_next_month_cd),
                tint = if (canGoNext) onSurface else disabledTint,
                modifier = Modifier.size(NAV_ICON_SIZE)
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun CalendarLegendRow(modifier: Modifier = Modifier) {
    val spacing = TithimalaTheme.spacing
    val extendedColors = TithimalaTheme.colors

    FlowRow(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = spacing.xs),
        horizontalArrangement = Arrangement.spacedBy(spacing.md),
        verticalArrangement = Arrangement.spacedBy(spacing.xs)
    ) {
        LegendDotItem(
            color = extendedColors.holiday,
            label = stringResource(R.string.feature_calendar_legend_holiday)
        )
        LegendDotItem(
            color = extendedColors.festivalDot,
            label = stringResource(R.string.feature_calendar_legend_event)
        )
        LegendDotItem(
            color = extendedColors.tithiLunar,
            label = stringResource(R.string.feature_calendar_legend_reminder)
        )
    }
}

@Composable
private fun LegendDotItem(color: Color, label: String, modifier: Modifier = Modifier) {
    val spacing = TithimalaTheme.spacing
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(spacing.micro)
    ) {
        Box(
            modifier = Modifier
                .size(LEGEND_DOT_SIZE)
                .clip(CircleShape)
                .background(color)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = TithimalaTheme.colors.textMuted
        )
    }
}

@Composable
internal fun FestivalSummaryRow(festival: FestivalSummaryUiModel, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val spacing = TithimalaTheme.spacing
    val cardShape = MaterialTheme.shapes.medium
    val titleText = festival.primaryTitle ?: stringResource(R.string.feature_calendar_holiday_label)
    val metaText = if (festival.isPublicHoliday) {
        stringResource(R.string.feature_calendar_festival_meta_holiday_format, festival.adShortDateText)
    } else {
        festival.adShortDateText
    }
    val accessibilityLabel = stringResource(
        R.string.feature_calendar_festival_cd_format,
        festival.bsDayText,
        festival.bsMonthText,
        titleText,
        metaText
    )

    TithimalaSurfaceCard(
        modifier = modifier
            .fillMaxWidth()
            .clip(cardShape)
            .clickable(
                onClickLabel = stringResource(R.string.feature_calendar_open_day_details),
                role = Role.Button,
                onClick = onClick
            )
            .semantics(mergeDescendants = true) {
                contentDescription = accessibilityLabel
            },
        shape = cardShape,
        contentPadding = PaddingValues(spacing.sm)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(spacing.sm)
        ) {
            FestivalDateBadge(
                bsDayText = festival.bsDayText,
                bsMonthText = festival.bsMonthText,
                isPublicHoliday = festival.isPublicHoliday,
                isToday = festival.isToday
            )
            FestivalRowDetails(
                titleText = titleText,
                additionalEventsText = festival.additionalEventsText,
                metaText = metaText,
                isPublicHoliday = festival.isPublicHoliday,
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                contentDescription = null,
                tint = TithimalaTheme.colors.textMuted,
                modifier = Modifier.size(CHEVRON_ICON_SIZE)
            )
        }
    }
}

@Composable
private fun FestivalRowDetails(
    titleText: String,
    additionalEventsText: String?,
    metaText: String,
    isPublicHoliday: Boolean,
    modifier: Modifier = Modifier
) {
    val spacing = TithimalaTheme.spacing
    val extendedColors = TithimalaTheme.colors
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(spacing.micro / 2)
    ) {
        Text(
            text = titleText,
            style = MaterialTheme.typography.titleSmall,
            color = if (isPublicHoliday) extendedColors.holiday else MaterialTheme.colorScheme.onSurface,
            maxLines = MAX_TITLE_LINES,
            overflow = TextOverflow.Ellipsis
        )
        if (additionalEventsText != null) {
            Text(
                text = additionalEventsText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = MAX_TITLE_LINES,
                overflow = TextOverflow.Ellipsis
            )
        }
        Text(
            text = metaText,
            style = MaterialTheme.typography.labelSmall,
            color = extendedColors.textMuted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun FestivalDateBadge(
    bsDayText: String,
    bsMonthText: String,
    isPublicHoliday: Boolean,
    isToday: Boolean,
    modifier: Modifier = Modifier
) {
    val spacing = TithimalaTheme.spacing
    val extendedColors = TithimalaTheme.colors
    val containerColor = when {
        isToday -> extendedColors.todayFill
        isPublicHoliday -> extendedColors.holidayContainer
        else -> MaterialTheme.colorScheme.surfaceContainer
    }
    val primaryColor = when {
        isToday -> extendedColors.onTodayFill
        isPublicHoliday -> extendedColors.onHolidayContainer
        else -> MaterialTheme.colorScheme.onSurface
    }
    val secondaryColor = when {
        isToday -> extendedColors.onTodayFill
        isPublicHoliday -> extendedColors.onHolidayContainer
        else -> extendedColors.textMuted
    }

    Surface(
        modifier = modifier.defaultMinSize(
            minWidth = spacing.minTouchTarget,
            minHeight = spacing.minTouchTarget
        ),
        shape = MaterialTheme.shapes.small,
        color = containerColor
    ) {
        Column(
            modifier = Modifier.padding(horizontal = spacing.xs, vertical = spacing.micro),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = bsDayText,
                style = MaterialTheme.typography.titleMedium,
                color = primaryColor,
                maxLines = 1,
                softWrap = false
            )
            Text(
                text = bsMonthText,
                style = MaterialTheme.typography.labelSmall,
                color = secondaryColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
internal fun FestivalsEmptyCard(modifier: Modifier = Modifier) {
    TithimalaSurfaceCard(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium
    ) {
        Text(
            text = stringResource(R.string.feature_calendar_no_festivals),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
