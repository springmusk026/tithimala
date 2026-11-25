package app.tithimala.core.ui.calendar

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.tithimala.core.designsystem.component.TithimalaSurfaceCard
import app.tithimala.core.designsystem.preview.TithimalaThemePreviews
import app.tithimala.core.designsystem.theme.TithimalaTheme
import app.tithimala.core.ui.R

private val FESTIVAL_INDICATOR_DOT_SIZE = 6.dp
private const val MAX_FESTIVAL_LINES = 2

/**
 * Immutable UI state for [TodayHeroHeader].
 */
@Immutable
data class TodayHeroUiModel(
    val bsDayNumber: String,
    val bsMonthYear: String,
    val adDateLabel: String,
    val weekdayLabel: String,
    val tithiLabel: String?,
    val lunarPhase: LunarPhaseVisual?,
    val isEstimatedTithi: Boolean = false,
    val primaryFestival: String? = null,
    val isHoliday: Boolean = false
)

/**
 * Editorial summary card showing the active Bikram Sambat date, secondary AD date, Tithi badge,
 * optional holiday/festival indicators, and an optional trailing action slot.
 */
@Composable
fun TodayHeroHeader(
    state: TodayHeroUiModel,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    trailingAction: (@Composable () -> Unit)? = null
) {
    val spacing = TithimalaTheme.spacing
    val extendedColors = TithimalaTheme.colors
    val cardShape = MaterialTheme.shapes.large

    TithimalaSurfaceCard(
        modifier = modifier
            .fillMaxWidth()
            .clip(cardShape)
            .clickable(
                onClickLabel = stringResource(R.string.core_ui_hero_open_day_details),
                role = Role.Button,
                onClick = onClick
            ),
        shape = cardShape,
        contentPadding = PaddingValues(spacing.md)
    ) {
        HeroDateHeaderRow(
            bsDayNumber = state.bsDayNumber,
            bsMonthYear = state.bsMonthYear,
            isHoliday = state.isHoliday,
            trailingAction = trailingAction
        )
        Spacer(modifier = Modifier.height(spacing.micro))
        Text(
            text = stringResource(
                R.string.core_ui_hero_ad_date_format,
                state.weekdayLabel,
                state.adDateLabel
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (state.tithiLabel != null || state.isHoliday) {
            Spacer(modifier = Modifier.height(spacing.sm))
            HeroStatusBadgeRow(
                tithiLabel = state.tithiLabel,
                lunarPhase = state.lunarPhase,
                isEstimatedTithi = state.isEstimatedTithi,
                isHoliday = state.isHoliday
            )
        }
        if (state.primaryFestival != null) {
            Spacer(modifier = Modifier.height(spacing.sm))
            HorizontalDivider(thickness = spacing.hairline, color = extendedColors.hairline)
            Spacer(modifier = Modifier.height(spacing.xs))
            PrimaryFestivalRow(festivalName = state.primaryFestival, isHoliday = state.isHoliday)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun HeroDateHeaderRow(
    bsDayNumber: String,
    bsMonthYear: String,
    isHoliday: Boolean,
    trailingAction: (@Composable () -> Unit)?
) {
    val spacing = TithimalaTheme.spacing
    val dayNumberColor = if (isHoliday) {
        TithimalaTheme.colors.holiday
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalArrangement = Arrangement.spacedBy(spacing.xs),
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
        if (trailingAction != null) {
            trailingAction()
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun HeroStatusBadgeRow(
    tithiLabel: String?,
    lunarPhase: LunarPhaseVisual?,
    isEstimatedTithi: Boolean,
    isHoliday: Boolean
) {
    val spacing = TithimalaTheme.spacing
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(spacing.xs),
        verticalArrangement = Arrangement.spacedBy(spacing.xs)
    ) {
        if (tithiLabel != null) {
            TithiMoonBadge(
                tithiLabel = tithiLabel,
                phase = lunarPhase,
                isEstimated = isEstimatedTithi
            )
        }
        if (isHoliday) {
            HolidayStatusBadge()
        }
    }
}

@Composable
private fun HolidayStatusBadge(modifier: Modifier = Modifier) {
    val spacing = TithimalaTheme.spacing
    val extendedColors = TithimalaTheme.colors
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.small,
        color = extendedColors.holidayContainer
    ) {
        Text(
            text = stringResource(R.string.core_ui_holiday_badge),
            style = MaterialTheme.typography.labelMedium,
            color = extendedColors.onHolidayContainer,
            modifier = Modifier.padding(
                horizontal = spacing.xs,
                vertical = spacing.micro
            )
        )
    }
}

@Composable
private fun PrimaryFestivalRow(festivalName: String, isHoliday: Boolean, modifier: Modifier = Modifier) {
    val spacing = TithimalaTheme.spacing
    val extendedColors = TithimalaTheme.colors
    val dotColor = if (isHoliday) extendedColors.holiday else extendedColors.festivalDot
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(spacing.xs)
    ) {
        Box(
            modifier = Modifier
                .size(FESTIVAL_INDICATOR_DOT_SIZE)
                .clip(CircleShape)
                .background(dotColor)
        )
        Text(
            text = festivalName,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = MAX_FESTIVAL_LINES,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@TithimalaThemePreviews
@Composable
private fun TodayHeroHeaderPreview() {
    TithimalaTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            val spacing = TithimalaTheme.spacing
            TodayHeroHeader(
                state = TodayHeroUiModel(
                    bsDayNumber = stringResource(R.string.core_ui_preview_bs_day),
                    bsMonthYear = stringResource(R.string.core_ui_preview_bs_month_year),
                    adDateLabel = stringResource(R.string.core_ui_preview_ad_date),
                    weekdayLabel = stringResource(R.string.core_ui_preview_weekday),
                    tithiLabel = stringResource(R.string.core_ui_preview_tithi_purnima),
                    lunarPhase = LunarPhaseVisual.FULL_MOON,
                    primaryFestival = stringResource(R.string.core_ui_preview_festival_udhauli),
                    isHoliday = true
                ),
                onClick = {},
                modifier = Modifier.padding(spacing.md),
                trailingAction = {
                    OutlinedButton(
                        onClick = {},
                        shape = MaterialTheme.shapes.small,
                        border = BorderStroke(spacing.hairline, TithimalaTheme.colors.hairline),
                        contentPadding = PaddingValues(horizontal = spacing.sm, vertical = spacing.xs),
                        modifier = Modifier.heightIn(min = spacing.minTouchTarget)
                    ) {
                        Text(text = stringResource(R.string.core_ui_action_today))
                    }
                }
            )
        }
    }
}
