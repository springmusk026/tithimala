package app.tithimala.feature.converter

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.tithimala.core.calendarengine.BsDateFormatter
import app.tithimala.core.designsystem.component.TithimalaSurfaceCard
import app.tithimala.core.designsystem.theme.TithimalaMotion
import app.tithimala.core.designsystem.theme.TithimalaTheme
import app.tithimala.core.designsystem.theme.rememberReducedMotionEnabled
import app.tithimala.core.ui.calendar.TithiMoonBadge

private const val STACKED_BUTTONS_FONT_SCALE_THRESHOLD = 1.35f
private const val MAX_EVENT_LINES = 2
private const val MAX_BUTTON_LINES = 2
private val ACTION_ICON_SIZE = 18.dp
private val EVENT_DOT_SIZE = 6.dp

@Composable
internal fun ConverterResultCard(
    state: ConverterUiState,
    onAction: (ConverterAction) -> Unit,
    modifier: Modifier = Modifier
) {
    val spacing = TithimalaTheme.spacing
    val extendedColors = TithimalaTheme.colors
    val reducedMotion = rememberReducedMotionEnabled()
    val targetHeading = when (state.direction) {
        ConversionDirection.BS_TO_AD -> stringResource(R.string.converter_result_ad_heading)
        ConversionDirection.AD_TO_BS -> stringResource(R.string.converter_result_bs_heading)
    }

    TithimalaSurfaceCard(
        modifier = modifier.fillMaxWidth(),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
        borderColor = extendedColors.hairline
    ) {
        Text(
            text = targetHeading,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.semantics { heading() }
        )

        Spacer(modifier = Modifier.height(spacing.xs))

        AnimatedContent(
            targetState = state.result != null,
            transitionSpec = {
                fadeIn(TithimalaMotion.standardSpring(reducedMotion)) togetherWith
                    fadeOut(TithimalaMotion.standardSpring(reducedMotion))
            },
            label = "converter_result_presence"
        ) { hasResult ->
            val currentResult = state.result
            if (hasResult && currentResult != null) {
                ResultContent(
                    result = currentResult,
                    state = state,
                    onCopyClick = { onAction(ConverterAction.CopyResult) },
                    onOpenDayClick = { onAction(ConverterAction.OpenInCalendar) }
                )
            } else {
                Text(
                    text = stringResource(R.string.converter_result_empty_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ResultContent(
    result: ConvertedDateResult,
    state: ConverterUiState,
    onCopyClick: () -> Unit,
    onOpenDayClick: () -> Unit
) {
    val spacing = TithimalaTheme.spacing
    val extendedColors = TithimalaTheme.colors

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = result.primaryDateText,
            style = MaterialTheme.typography.displaySmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }
        )

        Spacer(modifier = Modifier.height(spacing.micro))

        Text(
            text = stringResource(
                R.string.converter_result_weekday_source_format,
                result.weekdayText,
                result.secondaryDateText
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        ResultMetadataSection(result = result, state = state)

        Spacer(modifier = Modifier.height(spacing.md))
        HorizontalDivider(thickness = spacing.hairline, color = extendedColors.hairline)
        Spacer(modifier = Modifier.height(spacing.sm))

        ResultActionsRow(
            onCopyClick = onCopyClick,
            onOpenDayClick = onOpenDayClick
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ResultMetadataSection(result: ConvertedDateResult, state: ConverterUiState) {
    val spacing = TithimalaTheme.spacing
    val extendedColors = TithimalaTheme.colors

    if (result.tithiLabel != null || result.isPublicHoliday) {
        Spacer(modifier = Modifier.height(spacing.sm))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(spacing.xs),
            verticalArrangement = Arrangement.spacedBy(spacing.xs)
        ) {
            if (result.tithiLabel != null) {
                TithiMoonBadge(
                    tithiLabel = result.tithiLabel,
                    phase = result.lunarPhase,
                    isEstimated = result.isEstimatedTithi
                )
            }
            if (result.isPublicHoliday) {
                HolidayPillBadge()
            }
        }
    }

    if (result.primaryEvent != null) {
        Spacer(modifier = Modifier.height(spacing.sm))
        PrimaryEventRow(
            eventName = result.primaryEvent,
            isPublicHoliday = result.isPublicHoliday
        )
    } else if (result.tithiLabel == null && state.patroFirstYear != null && state.patroLastYear != null) {
        Spacer(modifier = Modifier.height(spacing.sm))
        Text(
            text = stringResource(
                R.string.converter_patro_coverage_note_format,
                BsDateFormatter.formatNumber(state.patroFirstYear, state.numeralScript),
                BsDateFormatter.formatNumber(state.patroLastYear, state.numeralScript)
            ),
            style = MaterialTheme.typography.bodySmall,
            color = extendedColors.textMuted
        )
    }
}

@Composable
private fun ResultActionsRow(onCopyClick: () -> Unit, onOpenDayClick: () -> Unit, modifier: Modifier = Modifier) {
    val spacing = TithimalaTheme.spacing
    val stackVertically = LocalDensity.current.fontScale >= STACKED_BUTTONS_FONT_SCALE_THRESHOLD

    if (stackVertically) {
        Column(
            modifier = modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(spacing.xs)
        ) {
            CopyDateButton(onClick = onCopyClick, modifier = Modifier.fillMaxWidth())
            ViewDayDetailsButton(onClick = onOpenDayClick, modifier = Modifier.fillMaxWidth())
        }
    } else {
        Row(
            modifier = modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(spacing.xs),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CopyDateButton(onClick = onCopyClick, modifier = Modifier.weight(1f))
            ViewDayDetailsButton(onClick = onOpenDayClick, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun CopyDateButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val spacing = TithimalaTheme.spacing
    FilledTonalButton(
        onClick = onClick,
        modifier = modifier.heightIn(min = spacing.minTouchTarget),
        shape = MaterialTheme.shapes.small,
        contentPadding = PaddingValues(horizontal = spacing.sm, vertical = spacing.xs)
    ) {
        Icon(
            imageVector = Icons.Rounded.ContentCopy,
            contentDescription = null,
            modifier = Modifier.size(ACTION_ICON_SIZE)
        )
        Spacer(modifier = Modifier.width(spacing.xs))
        Text(
            text = stringResource(R.string.converter_action_copy_date),
            maxLines = MAX_BUTTON_LINES,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun ViewDayDetailsButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val spacing = TithimalaTheme.spacing
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.heightIn(min = spacing.minTouchTarget),
        shape = MaterialTheme.shapes.small,
        border = BorderStroke(width = spacing.hairline, color = TithimalaTheme.colors.hairline),
        contentPadding = PaddingValues(horizontal = spacing.sm, vertical = spacing.xs)
    ) {
        Icon(
            imageVector = Icons.Rounded.CalendarToday,
            contentDescription = null,
            modifier = Modifier.size(ACTION_ICON_SIZE)
        )
        Spacer(modifier = Modifier.width(spacing.xs))
        Text(
            text = stringResource(R.string.converter_action_view_day),
            maxLines = MAX_BUTTON_LINES,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun HolidayPillBadge(modifier: Modifier = Modifier) {
    val spacing = TithimalaTheme.spacing
    val extendedColors = TithimalaTheme.colors
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.small,
        color = extendedColors.holidayContainer
    ) {
        Text(
            text = stringResource(R.string.converter_holiday_badge),
            style = MaterialTheme.typography.labelMedium,
            color = extendedColors.onHolidayContainer,
            modifier = Modifier.padding(horizontal = spacing.xs, vertical = spacing.micro)
        )
    }
}

@Composable
private fun PrimaryEventRow(eventName: String, isPublicHoliday: Boolean, modifier: Modifier = Modifier) {
    val spacing = TithimalaTheme.spacing
    val extendedColors = TithimalaTheme.colors
    val dotColor = if (isPublicHoliday) extendedColors.holiday else extendedColors.festivalDot
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(spacing.xs)
    ) {
        Box(
            modifier = Modifier
                .size(EVENT_DOT_SIZE)
                .clip(CircleShape)
                .background(dotColor)
        )
        Text(
            text = eventName,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = MAX_EVENT_LINES,
            overflow = TextOverflow.Ellipsis
        )
    }
}
