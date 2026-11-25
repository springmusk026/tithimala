package app.tithimala.core.ui.calendar

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.tithimala.core.designsystem.preview.TithimalaThemePreviews
import app.tithimala.core.designsystem.theme.TithimalaMotion
import app.tithimala.core.designsystem.theme.TithimalaTheme
import app.tithimala.core.designsystem.theme.rememberReducedMotionEnabled
import app.tithimala.core.ui.R

private val SELECTION_BORDER_WIDTH = 1.5.dp
private val INDICATOR_DOT_SIZE = 4.dp
private val INDICATOR_DOT_GAP = 3.dp
private val CELL_HORIZONTAL_PADDING = 2.dp
private const val OVERFLOW_DAY_ALPHA = 0.55f
private const val TODAY_SECONDARY_ALPHA = 0.88f
private const val TODAY_REMINDER_DOT_ALPHA = 0.72f
private const val SELECTED_SECONDARY_ALPHA = 0.84f
private const val PREVIEW_TODAY_DAY_INDEX = 10

/**
 * Immutable UI model representing one cell in a Bikram Sambat month grid.
 */
@Immutable
data class DayCellUiModel(
    val id: String,
    val bsDayText: String,
    val adDayText: String,
    val isCurrentMonth: Boolean,
    val isToday: Boolean,
    val isSelected: Boolean,
    val isSaturday: Boolean,
    val isPublicHoliday: Boolean,
    val hasEvents: Boolean,
    val hasReminders: Boolean = false,
    val accessibilityDescription: String
)

@Immutable
private data class DayCellColors(
    val container: Color,
    val border: Color?,
    val bsText: Color,
    val adText: Color,
    val eventDot: Color,
    val reminderDot: Color
)

/**
 * Stateless calendar day cell showing dual BS and AD numerals, Sindoor holiday/Saturday accents,
 * Today fill, tonal selection ring, and subtle event/reminder indicator dots.
 */
@Composable
fun DayCell(day: DayCellUiModel, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val spacing = TithimalaTheme.spacing
    val cellShape = MaterialTheme.shapes.small
    val colors = resolveDayCellColors(day)
    val animatedContainerColor by animateColorAsState(
        targetValue = colors.container,
        animationSpec = TithimalaMotion.standardSpring(reducedMotion = rememberReducedMotionEnabled()),
        label = "dayCellContainerColor"
    )
    val borderModifier = colors.border?.let { color ->
        Modifier.border(width = SELECTION_BORDER_WIDTH, color = color, shape = cellShape)
    } ?: Modifier

    Column(
        modifier = modifier
            .defaultMinSize(minWidth = spacing.minTouchTarget, minHeight = spacing.minTouchTarget)
            .clip(cellShape)
            .drawBehind { drawRect(color = animatedContainerColor) }
            .then(borderModifier)
            .clickable(role = Role.Button, onClick = onClick)
            .clearAndSetSemantics {
                contentDescription = day.accessibilityDescription
                selected = day.isSelected
            }
            .padding(
                start = CELL_HORIZONTAL_PADDING,
                top = spacing.micro,
                end = CELL_HORIZONTAL_PADDING,
                bottom = spacing.xs
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = day.bsDayText,
            style = MaterialTheme.typography.titleMedium,
            color = colors.bsText,
            textAlign = TextAlign.Center,
            maxLines = 1,
            softWrap = false
        )
        Text(
            text = day.adDayText,
            style = MaterialTheme.typography.labelSmall,
            color = colors.adText,
            textAlign = TextAlign.Center,
            maxLines = 1,
            softWrap = false
        )
        Spacer(modifier = Modifier.height(spacing.micro / 2))
        DayIndicatorRow(
            hasEvents = day.hasEvents,
            hasReminders = day.hasReminders,
            eventDotColor = colors.eventDot,
            reminderDotColor = colors.reminderDot
        )
    }
}

@Composable
private fun DayIndicatorRow(hasEvents: Boolean, hasReminders: Boolean, eventDotColor: Color, reminderDotColor: Color) {
    Row(
        modifier = Modifier.height(INDICATOR_DOT_SIZE),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(INDICATOR_DOT_GAP)
    ) {
        if (hasEvents) {
            Box(
                modifier = Modifier
                    .size(INDICATOR_DOT_SIZE)
                    .clip(CircleShape)
                    .background(eventDotColor)
            )
        }
        if (hasReminders) {
            Box(
                modifier = Modifier
                    .size(INDICATOR_DOT_SIZE)
                    .clip(CircleShape)
                    .background(reminderDotColor)
            )
        }
    }
}

@Composable
private fun resolveDayCellColors(day: DayCellUiModel): DayCellColors = DayCellColors(
    container = when {
        day.isToday -> TithimalaTheme.colors.todayFill
        day.isSelected -> MaterialTheme.colorScheme.primaryContainer
        else -> Color.Transparent
    },
    border = when {
        day.isSelected && day.isToday -> MaterialTheme.colorScheme.onSurface
        day.isSelected -> MaterialTheme.colorScheme.primary
        else -> null
    },
    bsText = resolveBsTextColor(day),
    adText = resolveAdTextColor(day),
    eventDot = resolveEventDotColor(day),
    reminderDot = resolveReminderDotColor(day)
)

@Composable
private fun resolveBsTextColor(day: DayCellUiModel): Color {
    val extendedColors = TithimalaTheme.colors
    val isAccentDay = day.isPublicHoliday || day.isSaturday
    return when {
        day.isToday -> extendedColors.onTodayFill
        day.isSelected && isAccentDay -> extendedColors.holiday
        day.isSelected -> MaterialTheme.colorScheme.onPrimaryContainer
        !day.isCurrentMonth && isAccentDay -> extendedColors.saturday.copy(alpha = OVERFLOW_DAY_ALPHA)
        !day.isCurrentMonth -> extendedColors.textMuted.copy(alpha = OVERFLOW_DAY_ALPHA)
        day.isPublicHoliday -> extendedColors.holiday
        day.isSaturday -> extendedColors.saturday
        else -> MaterialTheme.colorScheme.onSurface
    }
}

@Composable
private fun resolveAdTextColor(day: DayCellUiModel): Color {
    val extendedColors = TithimalaTheme.colors
    return when {
        day.isToday -> extendedColors.onTodayFill.copy(alpha = TODAY_SECONDARY_ALPHA)
        day.isSelected -> MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = SELECTED_SECONDARY_ALPHA)
        !day.isCurrentMonth -> extendedColors.textMuted.copy(alpha = OVERFLOW_DAY_ALPHA)
        else -> extendedColors.textMuted
    }
}

@Composable
private fun resolveEventDotColor(day: DayCellUiModel): Color {
    val extendedColors = TithimalaTheme.colors
    return when {
        day.isToday -> extendedColors.onTodayFill
        !day.isCurrentMonth -> extendedColors.festivalDot.copy(alpha = OVERFLOW_DAY_ALPHA)
        else -> extendedColors.festivalDot
    }
}

@Composable
private fun resolveReminderDotColor(day: DayCellUiModel): Color {
    val extendedColors = TithimalaTheme.colors
    return when {
        day.isToday -> extendedColors.onTodayFill.copy(alpha = TODAY_REMINDER_DOT_ALPHA)
        !day.isCurrentMonth -> extendedColors.tithiLunar.copy(alpha = OVERFLOW_DAY_ALPHA)
        else -> extendedColors.tithiLunar
    }
}

@Composable
private fun previewDayCellModel(
    id: String,
    isCurrentMonth: Boolean = true,
    isToday: Boolean = false,
    isSelected: Boolean = false,
    isSaturday: Boolean = false,
    isPublicHoliday: Boolean = false,
    hasEvents: Boolean = false,
    hasReminders: Boolean = false
): DayCellUiModel {
    val sampleBsDay = stringResource(R.string.core_ui_preview_bs_day)
    val sampleAdDay = stringArrayResource(R.array.core_ui_preview_ad_days)[PREVIEW_TODAY_DAY_INDEX]
    return DayCellUiModel(
        id = id,
        bsDayText = sampleBsDay,
        adDayText = sampleAdDay,
        isCurrentMonth = isCurrentMonth,
        isToday = isToday,
        isSelected = isSelected,
        isSaturday = isSaturday,
        isPublicHoliday = isPublicHoliday,
        hasEvents = hasEvents,
        hasReminders = hasReminders,
        accessibilityDescription = stringResource(
            R.string.core_ui_preview_day_cd_format,
            sampleBsDay,
            sampleAdDay
        )
    )
}

@TithimalaThemePreviews
@Composable
private fun DayCellStatesPreview() {
    TithimalaTheme {
        Surface(color = MaterialTheme.colorScheme.surfaceContainerLow) {
            val spacing = TithimalaTheme.spacing
            Row(
                modifier = Modifier.padding(spacing.sm),
                horizontalArrangement = Arrangement.spacedBy(spacing.xs)
            ) {
                DayCell(
                    day = previewDayCellModel(
                        id = "today",
                        isToday = true,
                        isPublicHoliday = true,
                        hasEvents = true,
                        hasReminders = true
                    ),
                    onClick = {}
                )
                DayCell(
                    day = previewDayCellModel(id = "selected", isSelected = true, hasReminders = true),
                    onClick = {}
                )
                DayCell(
                    day = previewDayCellModel(id = "saturday", isSaturday = true, hasEvents = true),
                    onClick = {}
                )
                DayCell(
                    day = previewDayCellModel(id = "overflow", isCurrentMonth = false),
                    onClick = {}
                )
            }
        }
    }
}
