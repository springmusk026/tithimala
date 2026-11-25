package app.tithimala.feature.calendar

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.tithimala.core.calendarengine.BsDateFormatter
import app.tithimala.core.designsystem.component.TithimalaSectionHeader
import app.tithimala.core.designsystem.component.TithimalaSurfaceCard
import app.tithimala.core.designsystem.preview.TithimalaThemePreviews
import app.tithimala.core.designsystem.theme.TithimalaTheme
import app.tithimala.core.model.BsDate
import app.tithimala.core.ui.calendar.DayCellUiModel
import app.tithimala.core.ui.calendar.MonthGrid
import app.tithimala.core.ui.calendar.MonthGridUiModel
import app.tithimala.core.ui.calendar.TodayHeroHeader
import java.util.Locale
import kotlinx.coroutines.flow.distinctUntilChanged

private val PLACEHOLDER_GRID_BODY_HEIGHT = 264.dp
private const val BEYOND_VIEWPORT_PAGE_COUNT = 1

@Immutable
private data class DayAccessibilityLabels(
    val today: String,
    val holiday: String,
    val hasEvents: String,
    val hasReminders: String,
    val appendFormat: String
)

@Composable
internal fun CalendarScreen(
    state: CalendarUiState,
    onAction: (CalendarAction) -> Unit,
    modifier: Modifier = Modifier,
    sharedDayModifier: @Composable (BsDate) -> Modifier = { Modifier },
    pagerState: PagerState = rememberPagerState(
        initialPage = state.pageIndexFor(state.currentYearMonth),
        pageCount = { state.pageCount }
    )
) {
    val spacing = TithimalaTheme.spacing
    val accessibilityLabels = rememberDayAccessibilityLabels()

    CalendarPagerSyncEffect(pagerState = pagerState, state = state, onAction = onAction)

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(spacing.md),
            verticalArrangement = Arrangement.spacedBy(spacing.xs)
        ) {
            item(key = "today_hero") {
                TodayHeroHeader(
                    state = state.todayHero,
                    onClick = { onAction(CalendarAction.OpenDayDetail(state.todayBs)) },
                    modifier = sharedDayModifier(state.todayBs),
                    trailingAction = if (state.isShowingTodayMonth) {
                        null
                    } else {
                        { TodayJumpButton(onClick = { onAction(CalendarAction.JumpToToday) }) }
                    }
                )
            }
            item(key = "month_section") {
                CalendarMonthSection(
                    state = state,
                    pagerState = pagerState,
                    accessibilityLabels = accessibilityLabels,
                    onAction = onAction,
                    modifier = Modifier.padding(top = spacing.xs)
                )
            }
            calendarFestivalsSection(
                state = state,
                onAction = onAction,
                sharedDayModifier = sharedDayModifier
            )
        }
    }
}

@Composable
private fun CalendarPagerSyncEffect(
    pagerState: PagerState,
    state: CalendarUiState,
    onAction: (CalendarAction) -> Unit
) {
    val latestState by rememberUpdatedState(state)
    val latestOnAction by rememberUpdatedState(onAction)

    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.settledPage }
            .distinctUntilChanged()
            .collect { settledPage ->
                val settledYearMonth = latestState.yearMonthForPage(settledPage)
                if (settledYearMonth != latestState.currentYearMonth) {
                    latestOnAction(CalendarAction.SelectMonth(settledYearMonth))
                }
            }
    }
}

@Composable
private fun CalendarMonthSection(
    state: CalendarUiState,
    pagerState: PagerState,
    accessibilityLabels: DayAccessibilityLabels,
    onAction: (CalendarAction) -> Unit,
    modifier: Modifier = Modifier
) {
    val spacing = TithimalaTheme.spacing
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(spacing.xs)
    ) {
        MonthNavigationHeader(
            monthTitle = state.monthTitle,
            adRangeSubtitle = state.adRangeSubtitle,
            canGoPrevious = state.canGoPrevious,
            canGoNext = state.canGoNext,
            onPreviousClick = { onAction(CalendarAction.PreviousMonth) },
            onNextClick = { onAction(CalendarAction.NextMonth) }
        )
        CalendarMonthPager(
            state = state,
            pagerState = pagerState,
            accessibilityLabels = accessibilityLabels,
            onDayClick = { bsDate -> onAction(CalendarAction.OpenDayDetail(bsDate)) }
        )
        CalendarLegendRow()
    }
}

private fun LazyListScope.calendarFestivalsSection(
    state: CalendarUiState,
    onAction: (CalendarAction) -> Unit,
    sharedDayModifier: @Composable (BsDate) -> Modifier
) {
    item(key = "festivals_header") {
        val spacing = TithimalaTheme.spacing
        val countLabel = if (state.upcomingFestivals.isNotEmpty()) {
            BsDateFormatter.formatNumber(state.upcomingFestivals.size, state.numeralScript)
        } else {
            null
        }
        TithimalaSectionHeader(
            title = stringResource(R.string.feature_calendar_section_festivals),
            modifier = Modifier.padding(top = spacing.md),
            trailing = countLabel?.let { count ->
                {
                    Text(
                        text = count,
                        style = MaterialTheme.typography.labelMedium,
                        color = TithimalaTheme.colors.textMuted
                    )
                }
            }
        )
    }
    if (state.upcomingFestivals.isEmpty()) {
        item(key = "festivals_empty") {
            FestivalsEmptyCard()
        }
    } else {
        items(
            items = state.upcomingFestivals,
            key = { festival -> "festival-${festival.bsDate.toIsoString()}" }
        ) { festival ->
            val rowSharedModifier = if (festival.bsDate != state.todayBs) {
                sharedDayModifier(festival.bsDate)
            } else {
                Modifier
            }
            FestivalSummaryRow(
                festival = festival,
                onClick = { onAction(CalendarAction.OpenDayDetail(festival.bsDate)) },
                modifier = rowSharedModifier
            )
        }
    }
}

@Composable
private fun CalendarMonthPager(
    state: CalendarUiState,
    pagerState: PagerState,
    accessibilityLabels: DayAccessibilityLabels,
    onDayClick: (BsDate) -> Unit,
    modifier: Modifier = Modifier
) {
    val spacing = TithimalaTheme.spacing

    HorizontalPager(
        state = pagerState,
        modifier = modifier.fillMaxWidth(),
        pageSpacing = spacing.sm,
        beyondViewportPageCount = BEYOND_VIEWPORT_PAGE_COUNT,
        verticalAlignment = Alignment.Top,
        key = { pageIndex ->
            val ym = state.yearMonthForPage(pageIndex)
            "${ym.year}-${ym.month}"
        }
    ) { pageIndex ->
        val pageYearMonth = state.yearMonthForPage(pageIndex)
        val rawGrid = state.visiblePages[pageYearMonth]
        if (rawGrid != null) {
            val localizedGrid = remember(rawGrid, accessibilityLabels) {
                rawGrid.withLocalizedAccessibility(accessibilityLabels)
            }
            MonthGrid(
                grid = localizedGrid,
                onDayClick = { dayCell ->
                    runCatching { BsDate.parse(dayCell.id) }
                        .onSuccess(onDayClick)
                }
            )
        } else {
            MonthGridPlaceholder(
                weekdayHeaders = state.monthGrid.weekdayHeaders,
                saturdayColumnIndex = state.monthGrid.saturdayColumnIndex
            )
        }
    }
}

@Composable
private fun MonthGridPlaceholder(
    weekdayHeaders: List<String>,
    saturdayColumnIndex: Int,
    modifier: Modifier = Modifier
) {
    val spacing = TithimalaTheme.spacing
    val extendedColors = TithimalaTheme.colors

    TithimalaSurfaceCard(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        contentPadding = PaddingValues(spacing.xs)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = spacing.xs),
            horizontalArrangement = Arrangement.spacedBy(spacing.micro)
        ) {
            weekdayHeaders.forEachIndexed { index, label ->
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (index == saturdayColumnIndex) extendedColors.saturday else extendedColors.textMuted,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
            }
        }
        HorizontalDivider(
            thickness = spacing.hairline,
            color = extendedColors.hairline
        )
        Spacer(modifier = Modifier.height(spacing.micro))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(PLACEHOLDER_GRID_BODY_HEIGHT)
        )
    }
}

@Composable
private fun rememberDayAccessibilityLabels(): DayAccessibilityLabels {
    val today = stringResource(R.string.feature_calendar_day_cd_today)
    val holiday = stringResource(R.string.feature_calendar_day_cd_holiday)
    val hasEvents = stringResource(R.string.feature_calendar_day_cd_has_events)
    val hasReminders = stringResource(R.string.feature_calendar_day_cd_has_reminders)
    val appendFormat = stringResource(R.string.feature_calendar_day_cd_append_format)
    return remember(today, holiday, hasEvents, hasReminders, appendFormat) {
        DayAccessibilityLabels(
            today = today,
            holiday = holiday,
            hasEvents = hasEvents,
            hasReminders = hasReminders,
            appendFormat = appendFormat
        )
    }
}

private fun MonthGridUiModel.withLocalizedAccessibility(labels: DayAccessibilityLabels): MonthGridUiModel =
    copy(days = days.map { day -> day.withLocalizedAccessibility(labels) })

private fun DayCellUiModel.withLocalizedAccessibility(labels: DayAccessibilityLabels): DayCellUiModel {
    var description = accessibilityDescription
    if (isToday) {
        description = String.format(Locale.getDefault(), labels.appendFormat, description, labels.today)
    }
    if (isPublicHoliday) {
        description = String.format(Locale.getDefault(), labels.appendFormat, description, labels.holiday)
    }
    if (hasEvents) {
        description = String.format(Locale.getDefault(), labels.appendFormat, description, labels.hasEvents)
    }
    if (hasReminders) {
        description = String.format(Locale.getDefault(), labels.appendFormat, description, labels.hasReminders)
    }
    return if (description == accessibilityDescription) this else copy(accessibilityDescription = description)
}

@TithimalaThemePreviews
@Composable
private fun CalendarScreenPreview() {
    TithimalaTheme {
        CalendarScreen(
            state = previewMangsir2083UiState(),
            onAction = {}
        )
    }
}
