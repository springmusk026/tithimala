package app.tithimala.feature.day

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import app.tithimala.core.designsystem.component.TithimalaSectionHeader
import app.tithimala.core.designsystem.preview.TithimalaThemePreviews
import app.tithimala.core.designsystem.theme.TithimalaMotion
import app.tithimala.core.designsystem.theme.TithimalaTheme
import app.tithimala.core.designsystem.theme.rememberReducedMotionEnabled
import app.tithimala.core.model.BsDate
import app.tithimala.core.model.ReminderRecurrence
import app.tithimala.core.model.TithiSource
import app.tithimala.core.ui.calendar.LunarPhaseVisual

private const val CONTENT_TYPE_HERO = "hero"
private const val CONTENT_TYPE_SECTION_HEADER = "section_header"
private const val CONTENT_TYPE_EVENTS = "events"
private const val CONTENT_TYPE_REMINDER = "reminder"
private const val CONTENT_TYPE_EMPTY = "empty"

/**
 * Stateless Day Detail screen following the Himalayan Dawn design system.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DayScreen(
    state: DayUiState,
    onAction: (DayAction) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    sharedHeaderModifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() }
) {
    val spacing = TithimalaTheme.spacing
    val reducedMotion = rememberReducedMotionEnabled()
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            DayTopAppBar(
                canNavigatePrevious = state.canNavigatePrevious,
                canNavigateNext = state.canNavigateNext,
                canCopyDate = state.copyableDateText.isNotEmpty(),
                onBack = onBack,
                onPreviousDay = { onAction(DayAction.PreviousDay) },
                onNextDay = { onAction(DayAction.NextDay) },
                onCopyDate = { onAction(DayAction.CopyDateClicked) },
                scrollBehavior = scrollBehavior
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { innerPadding ->
        if (state.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            )
        } else {
            DayDetailContent(
                state = state,
                onAction = onAction,
                reducedMotion = reducedMotion,
                contentPadding = PaddingValues(
                    start = spacing.md,
                    top = innerPadding.calculateTopPadding() + spacing.xs,
                    end = spacing.md,
                    bottom = innerPadding.calculateBottomPadding() + spacing.lg
                ),
                sharedHeaderModifier = sharedHeaderModifier
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DayTopAppBar(
    canNavigatePrevious: Boolean,
    canNavigateNext: Boolean,
    canCopyDate: Boolean,
    onBack: () -> Unit,
    onPreviousDay: () -> Unit,
    onNextDay: () -> Unit,
    onCopyDate: () -> Unit,
    scrollBehavior: TopAppBarScrollBehavior
) {
    TopAppBar(
        title = {
            Text(
                text = stringResource(R.string.day_screen_title),
                style = MaterialTheme.typography.titleLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = stringResource(R.string.day_action_back)
                )
            }
        },
        actions = {
            IconButton(onClick = onPreviousDay, enabled = canNavigatePrevious) {
                Icon(
                    imageVector = Icons.Rounded.ChevronLeft,
                    contentDescription = stringResource(R.string.day_action_previous_day)
                )
            }
            IconButton(onClick = onNextDay, enabled = canNavigateNext) {
                Icon(
                    imageVector = Icons.Rounded.ChevronRight,
                    contentDescription = stringResource(R.string.day_action_next_day)
                )
            }
            IconButton(onClick = onCopyDate, enabled = canCopyDate) {
                Icon(
                    imageVector = Icons.Outlined.ContentCopy,
                    contentDescription = stringResource(R.string.day_action_copy_date)
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background,
            scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            navigationIconContentColor = MaterialTheme.colorScheme.onBackground,
            titleContentColor = MaterialTheme.colorScheme.onBackground,
            actionIconContentColor = MaterialTheme.colorScheme.onSurfaceVariant
        ),
        scrollBehavior = scrollBehavior
    )
}

@Composable
private fun DayDetailContent(
    state: DayUiState,
    onAction: (DayAction) -> Unit,
    reducedMotion: Boolean,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
    sharedHeaderModifier: Modifier = Modifier
) {
    val spacing = TithimalaTheme.spacing

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(spacing.sm)
    ) {
        item(key = "hero", contentType = CONTENT_TYPE_HERO) {
            DayHeroCard(
                state = state,
                modifier = sharedHeaderModifier.fillMaxWidth()
            )
        }

        item(key = "events-header", contentType = CONTENT_TYPE_SECTION_HEADER) {
            TithimalaSectionHeader(
                title = stringResource(R.string.day_section_festivals_events),
                modifier = Modifier.padding(top = spacing.xs)
            )
        }

        item(key = "events-card", contentType = CONTENT_TYPE_EVENTS) {
            DayEventsCard(
                events = state.events,
                isPublicHoliday = state.isPublicHoliday,
                reducedMotion = reducedMotion
            )
        }

        item(key = "reminders-header", contentType = CONTENT_TYPE_SECTION_HEADER) {
            TithimalaSectionHeader(
                title = stringResource(R.string.day_section_reminders),
                modifier = Modifier.padding(top = spacing.xs),
                trailing = {
                    AddReminderButton(onClick = { onAction(DayAction.AddReminderClicked) })
                }
            )
        }

        if (state.reminders.isEmpty()) {
            item(key = "reminders-empty", contentType = CONTENT_TYPE_EMPTY) {
                DayRemindersEmptyCard()
            }
        } else {
            items(
                items = state.reminders,
                key = { reminder -> reminder.id },
                contentType = { CONTENT_TYPE_REMINDER }
            ) { reminder ->
                DayReminderCard(
                    reminder = reminder,
                    onToggle = { enabled -> onAction(DayAction.ToggleReminder(reminder.id, enabled)) },
                    onDelete = { onAction(DayAction.DeleteReminder(reminder.id)) },
                    modifier = Modifier.animateItem(
                        fadeInSpec = TithimalaMotion.standardSpring(reducedMotion),
                        placementSpec = TithimalaMotion.standardSpring(reducedMotion),
                        fadeOutSpec = TithimalaMotion.standardSpring(reducedMotion)
                    )
                )
            }
        }
    }
}

@Composable
internal fun sampleDayUiState(): DayUiState = DayUiState(
    isLoading = false,
    bsDate = BsDate(year = 2081, month = 1, day = 1),
    bsDayNumber = stringResource(R.string.day_preview_bs_day),
    bsMonthYear = stringResource(R.string.day_preview_bs_month_year),
    bsFullDateLabel = stringResource(R.string.day_preview_bs_full_date),
    weekdayLabel = stringResource(R.string.day_preview_weekday),
    adDateLabel = stringResource(R.string.day_preview_ad_date),
    tithiLabel = stringResource(R.string.day_preview_tithi),
    tithiSource = TithiSource.VERIFIED,
    lunarPhase = LunarPhaseVisual.FIRST_QUARTER,
    isPublicHoliday = true,
    isSaturday = true,
    isToday = false,
    events = listOf(
        stringResource(R.string.day_preview_event_new_year),
        stringResource(R.string.day_preview_event_sankranti),
        stringResource(R.string.day_preview_event_bisket)
    ),
    reminders = listOf(
        DayReminderUiModel(
            id = 1L,
            title = stringResource(R.string.day_preview_reminder_puja_title),
            notes = stringResource(R.string.day_preview_reminder_puja_notes),
            formattedTime = stringResource(R.string.day_preview_reminder_puja_time),
            recurrence = ReminderRecurrence.YEARLY_BS,
            isEnabled = true
        ),
        DayReminderUiModel(
            id = 2L,
            title = stringResource(R.string.day_preview_reminder_call_title),
            notes = "",
            formattedTime = stringResource(R.string.day_preview_reminder_call_time),
            recurrence = ReminderRecurrence.ONE_TIME,
            isEnabled = false
        )
    ),
    canNavigatePrevious = true,
    canNavigateNext = true,
    copyableDateText = stringResource(R.string.day_preview_bs_full_date)
)

@TithimalaThemePreviews
@Composable
private fun DayScreenPreview() {
    TithimalaTheme {
        DayScreen(
            state = sampleDayUiState(),
            onAction = {},
            onBack = {}
        )
    }
}
