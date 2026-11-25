package app.tithimala.feature.day

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import app.tithimala.core.designsystem.component.TithimalaSurfaceCard
import app.tithimala.core.designsystem.theme.TithimalaTheme
import app.tithimala.core.model.TithiSource
import app.tithimala.core.ui.calendar.LunarPhaseVisual
import app.tithimala.core.ui.calendar.TithiMoonBadge

private val INDICATOR_DOT_SIZE = 6.dp
private val INFO_ICON_SIZE = 16.dp
private const val HOLIDAY_BORDER_ALPHA = 0.28f

@Composable
internal fun DayHeroCard(state: DayUiState, modifier: Modifier = Modifier) {
    val spacing = TithimalaTheme.spacing
    val extendedColors = TithimalaTheme.colors
    val cardBorderColor = if (state.isPublicHoliday) {
        extendedColors.holiday.copy(alpha = HOLIDAY_BORDER_ALPHA)
    } else {
        extendedColors.hairline
    }

    TithimalaSurfaceCard(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        borderColor = cardBorderColor,
        contentPadding = PaddingValues(spacing.md)
    ) {
        HeroDateTitleRow(
            bsDayNumber = state.bsDayNumber,
            bsMonthYear = state.bsMonthYear,
            isPublicHoliday = state.isPublicHoliday,
            isSaturday = state.isSaturday,
            isToday = state.isToday
        )
        HeroAdDateSubtitle(
            weekdayLabel = state.weekdayLabel,
            adDateLabel = state.adDateLabel
        )
        HeroStatusBadgesRow(
            tithiLabel = state.tithiLabel,
            lunarPhase = state.lunarPhase,
            isEstimated = state.tithiSource == TithiSource.ESTIMATED,
            isPublicHoliday = state.isPublicHoliday,
            isSaturday = state.isSaturday
        )
        if (state.tithiSource == TithiSource.UNAVAILABLE) {
            AlmanacUnavailableNote()
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun HeroDateTitleRow(
    bsDayNumber: String,
    bsMonthYear: String,
    isPublicHoliday: Boolean,
    isSaturday: Boolean,
    isToday: Boolean
) {
    val spacing = TithimalaTheme.spacing
    val extendedColors = TithimalaTheme.colors
    val dayNumberColor = when {
        isPublicHoliday -> extendedColors.holiday
        isSaturday -> extendedColors.saturday
        else -> MaterialTheme.colorScheme.onSurface
    }

    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(spacing.xs),
        verticalArrangement = Arrangement.spacedBy(spacing.micro),
        itemVerticalAlignment = Alignment.CenterVertically
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(spacing.xs)) {
            Text(
                text = bsDayNumber,
                style = MaterialTheme.typography.displayLarge,
                color = dayNumberColor,
                modifier = Modifier.alignByBaseline()
            )
            Text(
                text = bsMonthYear,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.alignByBaseline()
            )
        }
        if (isToday) {
            QuietStatusPill(
                label = stringResource(R.string.day_badge_today),
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

@Composable
private fun HeroAdDateSubtitle(weekdayLabel: String, adDateLabel: String) {
    if (adDateLabel.isEmpty()) return
    Spacer(modifier = Modifier.height(TithimalaTheme.spacing.micro))
    val adLine = if (weekdayLabel.isNotEmpty()) {
        stringResource(R.string.day_ad_date_format, weekdayLabel, adDateLabel)
    } else {
        adDateLabel
    }
    Text(
        text = adLine,
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun HeroStatusBadgesRow(
    tithiLabel: String?,
    lunarPhase: LunarPhaseVisual?,
    isEstimated: Boolean,
    isPublicHoliday: Boolean,
    isSaturday: Boolean
) {
    if (tithiLabel == null && !isPublicHoliday && !isSaturday) return
    val spacing = TithimalaTheme.spacing
    val extendedColors = TithimalaTheme.colors

    Spacer(modifier = Modifier.height(spacing.sm))
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(spacing.xs),
        verticalArrangement = Arrangement.spacedBy(spacing.xs)
    ) {
        if (tithiLabel != null) {
            TithiMoonBadge(
                tithiLabel = tithiLabel,
                phase = lunarPhase,
                isEstimated = isEstimated
            )
        }
        if (isPublicHoliday) {
            DayHolidayPill(
                label = stringResource(R.string.day_badge_public_holiday),
                dotColor = extendedColors.holiday
            )
        } else if (isSaturday) {
            DayHolidayPill(
                label = stringResource(R.string.day_badge_saturday_holiday),
                dotColor = extendedColors.saturday
            )
        }
    }
}

@Composable
private fun AlmanacUnavailableNote() {
    val spacing = TithimalaTheme.spacing
    val extendedColors = TithimalaTheme.colors
    Spacer(modifier = Modifier.height(spacing.sm))
    HorizontalDivider(thickness = spacing.hairline, color = extendedColors.hairline)
    Spacer(modifier = Modifier.height(spacing.xs))
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(spacing.xs)
    ) {
        Icon(
            imageVector = Icons.Outlined.Info,
            contentDescription = null,
            tint = extendedColors.textMuted,
            modifier = Modifier.size(INFO_ICON_SIZE)
        )
        Text(
            text = stringResource(R.string.day_almanac_unavailable),
            style = MaterialTheme.typography.bodySmall,
            color = extendedColors.textMuted
        )
    }
}

@Composable
private fun DayHolidayPill(label: String, dotColor: Color, modifier: Modifier = Modifier) {
    val spacing = TithimalaTheme.spacing
    val extendedColors = TithimalaTheme.colors
    Surface(
        modifier = modifier.semantics(mergeDescendants = true) {},
        shape = MaterialTheme.shapes.small,
        color = extendedColors.holidayContainer
    ) {
        Row(
            modifier = Modifier.padding(horizontal = spacing.xs, vertical = spacing.micro),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(spacing.micro)
        ) {
            Box(
                modifier = Modifier
                    .size(INDICATOR_DOT_SIZE)
                    .clip(CircleShape)
                    .background(dotColor)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = extendedColors.onHolidayContainer
            )
        }
    }
}

@Composable
internal fun QuietStatusPill(label: String, containerColor: Color, contentColor: Color, modifier: Modifier = Modifier) {
    val spacing = TithimalaTheme.spacing
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.small,
        color = containerColor
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = contentColor,
            modifier = Modifier.padding(horizontal = spacing.xs, vertical = spacing.micro)
        )
    }
}
