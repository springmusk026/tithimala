package app.tithimala.core.ui.calendar

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import app.tithimala.core.designsystem.component.TithimalaSectionHeader
import app.tithimala.core.designsystem.preview.TithimalaThemePreviews
import app.tithimala.core.designsystem.theme.TithimalaTheme
import app.tithimala.core.ui.R

private const val SATURDAY_COLUMN_INDEX = 6
private const val DAYS_PER_WEEK = 7
private const val CURRENT_MONTH_START_INDEX = 1
private const val CURRENT_MONTH_END_INDEX = 29
private const val TODAY_INDEX = 10
private const val SELECTED_INDEX = 14
private val HOLIDAY_INDICES = setOf(10, 17)
private val EVENT_INDICES = setOf(4, 10, 17, 25)
private val REMINDER_INDICES = setOf(10, 14, 22, 25)

@Composable
internal fun CalendarCompositesShowcase(modifier: Modifier = Modifier) {
    val spacing = TithimalaTheme.spacing

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(spacing.md),
            verticalArrangement = Arrangement.spacedBy(spacing.md)
        ) {
            TodayHeroHeader(
                state = sampleTodayHeroUiModel(),
                onClick = {},
                trailingAction = { TodayJumpButton(onClick = {}) }
            )
            TithimalaSectionHeader(title = stringResource(R.string.core_ui_preview_section_lunar))
            LunarPhaseShowcaseRow()
            TithimalaSectionHeader(title = stringResource(R.string.core_ui_preview_section_month))
            MonthGrid(grid = sampleMonthGridUiModel(), onDayClick = {})
        }
    }
}

@Composable
private fun TodayJumpButton(onClick: () -> Unit) {
    val spacing = TithimalaTheme.spacing
    OutlinedButton(
        onClick = onClick,
        shape = MaterialTheme.shapes.small,
        border = BorderStroke(spacing.hairline, TithimalaTheme.colors.hairline),
        contentPadding = PaddingValues(horizontal = spacing.sm, vertical = spacing.xs),
        modifier = Modifier.heightIn(min = spacing.minTouchTarget)
    ) {
        Text(text = stringResource(R.string.core_ui_action_today))
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LunarPhaseShowcaseRow() {
    val spacing = TithimalaTheme.spacing
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(spacing.xs),
        verticalArrangement = Arrangement.spacedBy(spacing.xs)
    ) {
        TithiMoonBadge(
            tithiLabel = stringResource(R.string.core_ui_preview_tithi_amavasya),
            phase = LunarPhaseVisual.NEW_MOON
        )
        TithiMoonBadge(
            tithiLabel = stringResource(R.string.core_ui_preview_tithi_tritiya),
            phase = LunarPhaseVisual.WAXING_CRESCENT,
            isEstimated = true
        )
        TithiMoonBadge(
            tithiLabel = stringResource(R.string.core_ui_preview_tithi_ashtami),
            phase = LunarPhaseVisual.FIRST_QUARTER
        )
        TithiMoonBadge(
            tithiLabel = stringResource(R.string.core_ui_preview_tithi_ekadashi),
            phase = LunarPhaseVisual.WAXING_GIBBOUS
        )
        TithiMoonBadge(
            tithiLabel = stringResource(R.string.core_ui_preview_tithi_purnima),
            phase = LunarPhaseVisual.FULL_MOON
        )
        TithiMoonBadge(
            tithiLabel = stringResource(R.string.core_ui_preview_tithi_ekadashi),
            phase = LunarPhaseVisual.WANING_GIBBOUS
        )
        TithiMoonBadge(
            tithiLabel = stringResource(R.string.core_ui_preview_tithi_ashtami),
            phase = LunarPhaseVisual.LAST_QUARTER
        )
        TithiMoonBadge(
            tithiLabel = stringResource(R.string.core_ui_preview_tithi_tritiya),
            phase = LunarPhaseVisual.WANING_CRESCENT
        )
    }
}

@Composable
internal fun sampleTodayHeroUiModel(): TodayHeroUiModel = TodayHeroUiModel(
    bsDayNumber = stringResource(R.string.core_ui_preview_bs_day),
    bsMonthYear = stringResource(R.string.core_ui_preview_bs_month_year),
    adDateLabel = stringResource(R.string.core_ui_preview_ad_date),
    weekdayLabel = stringResource(R.string.core_ui_preview_weekday),
    tithiLabel = stringResource(R.string.core_ui_preview_tithi_purnima),
    lunarPhase = LunarPhaseVisual.FULL_MOON,
    primaryFestival = stringResource(R.string.core_ui_preview_festival_udhauli),
    isHoliday = true
)

@Composable
internal fun sampleMonthGridUiModel(): MonthGridUiModel {
    val headers = listOf(
        stringResource(R.string.core_ui_weekday_sun_short),
        stringResource(R.string.core_ui_weekday_mon_short),
        stringResource(R.string.core_ui_weekday_tue_short),
        stringResource(R.string.core_ui_weekday_wed_short),
        stringResource(R.string.core_ui_weekday_thu_short),
        stringResource(R.string.core_ui_weekday_fri_short),
        stringResource(R.string.core_ui_weekday_sat_short)
    )
    val bsDays = stringArrayResource(R.array.core_ui_preview_bs_days)
    val adDays = stringArrayResource(R.array.core_ui_preview_ad_days)
    val days = bsDays.indices.map { index ->
        val bsText = bsDays[index]
        val adText = adDays[index]
        val isCurrentMonth = index in CURRENT_MONTH_START_INDEX..CURRENT_MONTH_END_INDEX
        val isToday = index == TODAY_INDEX
        val isHoliday = index in HOLIDAY_INDICES
        val baseDescription = stringResource(R.string.core_ui_preview_day_cd_format, bsText, adText)
        val withToday = if (isToday) {
            stringResource(R.string.core_ui_preview_day_cd_today_suffix, baseDescription)
        } else {
            baseDescription
        }
        val description = if (isHoliday) {
            stringResource(R.string.core_ui_preview_day_cd_holiday_suffix, withToday)
        } else {
            withToday
        }
        DayCellUiModel(
            id = "preview-day-$index",
            bsDayText = bsText,
            adDayText = adText,
            isCurrentMonth = isCurrentMonth,
            isToday = isToday,
            isSelected = index == SELECTED_INDEX,
            isSaturday = index % DAYS_PER_WEEK == SATURDAY_COLUMN_INDEX,
            isPublicHoliday = isHoliday,
            hasEvents = index in EVENT_INDICES,
            hasReminders = index in REMINDER_INDICES,
            accessibilityDescription = description
        )
    }
    return MonthGridUiModel(
        weekdayHeaders = headers,
        saturdayColumnIndex = SATURDAY_COLUMN_INDEX,
        days = days
    )
}

@TithimalaThemePreviews
@Composable
private fun CalendarCompositesShowcasePreview() {
    TithimalaTheme {
        CalendarCompositesShowcase()
    }
}
