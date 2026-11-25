package app.tithimala.feature.day

import androidx.compose.animation.animateContentSize
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.tithimala.core.designsystem.component.TithimalaSurfaceCard
import app.tithimala.core.designsystem.theme.TithimalaMotion
import app.tithimala.core.designsystem.theme.TithimalaTheme
import app.tithimala.core.model.ReminderRecurrence

private val EVENT_DOT_SIZE = 6.dp
private val ADD_ICON_SIZE = 18.dp
private val EMPTY_ICON_SIZE = 20.dp
private const val MAX_REMINDER_NOTE_LINES = 2

@Composable
internal fun DayEventsCard(
    events: List<String>,
    isPublicHoliday: Boolean,
    reducedMotion: Boolean,
    modifier: Modifier = Modifier
) {
    val spacing = TithimalaTheme.spacing
    val extendedColors = TithimalaTheme.colors
    val dotColor = if (isPublicHoliday) extendedColors.holiday else extendedColors.festivalDot

    TithimalaSurfaceCard(
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize(animationSpec = TithimalaMotion.standardSpring(reducedMotion)),
        shape = MaterialTheme.shapes.medium,
        contentPadding = PaddingValues(spacing.md)
    ) {
        if (events.isEmpty()) {
            Text(
                text = stringResource(R.string.day_events_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = extendedColors.textMuted
            )
        } else {
            events.forEachIndexed { index, eventName ->
                if (index > 0) {
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = spacing.xs),
                        thickness = spacing.hairline,
                        color = extendedColors.hairline
                    )
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = spacing.micro),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(spacing.sm)
                ) {
                    Box(
                        modifier = Modifier
                            .size(EVENT_DOT_SIZE)
                            .clip(CircleShape)
                            .background(dotColor)
                    )
                    Text(
                        text = eventName,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Composable
internal fun AddReminderButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val spacing = TithimalaTheme.spacing
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.heightIn(min = spacing.minTouchTarget),
        shape = MaterialTheme.shapes.small,
        border = BorderStroke(spacing.hairline, TithimalaTheme.colors.hairline),
        contentPadding = PaddingValues(horizontal = spacing.sm, vertical = spacing.xs)
    ) {
        Icon(
            imageVector = Icons.Rounded.Add,
            contentDescription = null,
            modifier = Modifier.size(ADD_ICON_SIZE)
        )
        Spacer(modifier = Modifier.width(spacing.micro))
        Text(
            text = stringResource(R.string.day_action_add_reminder),
            style = MaterialTheme.typography.labelLarge
        )
    }
}

@Composable
internal fun DayRemindersEmptyCard(modifier: Modifier = Modifier) {
    val spacing = TithimalaTheme.spacing
    val extendedColors = TithimalaTheme.colors
    TithimalaSurfaceCard(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        contentPadding = PaddingValues(spacing.md)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(spacing.sm)
        ) {
            Icon(
                imageVector = Icons.Outlined.NotificationsNone,
                contentDescription = null,
                tint = extendedColors.textMuted,
                modifier = Modifier
                    .padding(top = spacing.micro)
                    .size(EMPTY_ICON_SIZE)
            )
            Column(verticalArrangement = Arrangement.spacedBy(spacing.micro)) {
                Text(
                    text = stringResource(R.string.day_reminders_empty_title),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = stringResource(R.string.day_reminders_empty_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = extendedColors.textMuted
                )
            }
        }
    }
}

@Composable
internal fun DayReminderCard(
    reminder: DayReminderUiModel,
    onToggle: (Boolean) -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val spacing = TithimalaTheme.spacing

    TithimalaSurfaceCard(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        contentPadding = PaddingValues(
            start = spacing.md,
            top = spacing.sm,
            end = spacing.xs,
            bottom = spacing.sm
        )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(spacing.xs)
        ) {
            ReminderMetadataColumn(
                reminder = reminder,
                modifier = Modifier.weight(1f)
            )
            ReminderTrailingControls(
                reminderTitle = reminder.title,
                isEnabled = reminder.isEnabled,
                onToggle = onToggle,
                onDelete = onDelete
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ReminderMetadataColumn(reminder: DayReminderUiModel, modifier: Modifier = Modifier) {
    val spacing = TithimalaTheme.spacing
    val titleColor = if (reminder.isEnabled) {
        MaterialTheme.colorScheme.onSurface
    } else {
        TithimalaTheme.colors.textMuted
    }
    val (recurrenceLabel, recurrenceContainer, recurrenceContent) = when (reminder.recurrence) {
        ReminderRecurrence.ONE_TIME -> Triple(
            stringResource(R.string.day_reminder_recurrence_one_time),
            MaterialTheme.colorScheme.secondaryContainer,
            MaterialTheme.colorScheme.onSecondaryContainer
        )
        ReminderRecurrence.YEARLY_BS -> Triple(
            stringResource(R.string.day_reminder_recurrence_yearly_bs),
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.onPrimaryContainer
        )
    }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(spacing.micro)
    ) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(spacing.xs),
            verticalArrangement = Arrangement.spacedBy(spacing.micro)
        ) {
            QuietStatusPill(
                label = reminder.formattedTime,
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                contentColor = MaterialTheme.colorScheme.onSurface
            )
            QuietStatusPill(
                label = recurrenceLabel,
                containerColor = recurrenceContainer,
                contentColor = recurrenceContent
            )
        }
        Text(
            text = reminder.title,
            style = MaterialTheme.typography.titleMedium,
            color = titleColor
        )
        if (reminder.notes.isNotBlank()) {
            Text(
                text = reminder.notes,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = MAX_REMINDER_NOTE_LINES,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun ReminderTrailingControls(
    reminderTitle: String,
    isEnabled: Boolean,
    onToggle: (Boolean) -> Unit,
    onDelete: () -> Unit
) {
    val toggleContentDescription = stringResource(R.string.day_reminder_toggle_cd, reminderTitle)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(TithimalaTheme.spacing.micro)
    ) {
        Switch(
            checked = isEnabled,
            onCheckedChange = onToggle,
            modifier = Modifier.semantics {
                contentDescription = toggleContentDescription
            }
        )
        IconButton(onClick = onDelete) {
            Icon(
                imageVector = Icons.Outlined.DeleteOutline,
                contentDescription = stringResource(R.string.day_reminder_delete_cd, reminderTitle),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
