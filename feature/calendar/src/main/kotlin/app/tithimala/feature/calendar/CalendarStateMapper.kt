package app.tithimala.feature.calendar

import app.tithimala.core.calendarengine.BikramSambatEngine
import app.tithimala.core.calendarengine.BsDateFormatter
import app.tithimala.core.model.AppLanguage
import app.tithimala.core.model.BsDate
import app.tithimala.core.model.BsMonthGrid
import app.tithimala.core.model.BsYearMonth
import app.tithimala.core.model.CalendarDay
import app.tithimala.core.model.CalendarReminder
import app.tithimala.core.model.MoonPhase
import app.tithimala.core.model.NumeralScript
import app.tithimala.core.model.ReminderRecurrence
import app.tithimala.core.model.TithiSource
import app.tithimala.core.model.UserPreferences
import app.tithimala.core.model.WeekStartDay
import app.tithimala.core.ui.calendar.DayCellUiModel
import app.tithimala.core.ui.calendar.LunarPhaseVisual
import app.tithimala.core.ui.calendar.MonthGridUiModel
import app.tithimala.core.ui.calendar.TodayHeroUiModel
import java.time.DayOfWeek
import java.util.Locale

private const val PRELOAD_MONTH_RADIUS = 2
private const val ADDITIONAL_EVENTS_SEPARATOR = " · "

internal data class CalendarRenderInput(
    val engine: BikramSambatEngine,
    val preferences: UserPreferences,
    val reminders: List<CalendarReminder>,
    val todayBs: BsDate,
    val currentYearMonth: BsYearMonth,
    val selectedDate: BsDate,
    val systemLocale: Locale
)

private data class MonthGridFormatContext(
    val engine: BikramSambatEngine,
    val effectiveLanguage: AppLanguage,
    val effectiveLocale: Locale,
    val numeralScript: NumeralScript,
    val selectedDate: BsDate,
    val reminderLookup: ReminderLookup
)

private class ReminderLookup(enabledReminders: List<CalendarReminder>) {
    private val oneTimeDates: Set<BsDate> = enabledReminders
        .asSequence()
        .filter { it.recurrence == ReminderRecurrence.ONE_TIME }
        .map(CalendarReminder::bsDate)
        .toSet()

    private val yearlyByMonth: Map<Int, List<CalendarReminder>> = enabledReminders
        .filter { it.recurrence == ReminderRecurrence.YEARLY_BS }
        .groupBy(CalendarReminder::bsMonth)

    fun hasReminderOn(bsDate: BsDate, engine: BikramSambatEngine): Boolean {
        val isValid = engine.isValidBsDate(bsDate.year, bsDate.month, bsDate.day)
        return isValid && (bsDate in oneTimeDates || hasYearlyMatch(bsDate, engine))
    }

    private fun hasYearlyMatch(bsDate: BsDate, engine: BikramSambatEngine): Boolean {
        val yearlyCandidates = yearlyByMonth[bsDate.month].orEmpty()
        if (yearlyCandidates.isEmpty()) return false
        val monthLength = engine.daysInMonth(bsDate.year, bsDate.month)
        return yearlyCandidates.any { reminder ->
            bsDate.year >= reminder.bsYear && minOf(reminder.bsDay, monthLength) == bsDate.day
        }
    }
}

internal object CalendarStateMapper {

    fun buildUiState(input: CalendarRenderInput): CalendarUiState {
        val minYearMonth = input.engine.minBsDate.toYearMonth()
        val maxYearMonth = input.engine.maxBsDate.toYearMonth()
        val clampedYearMonth = input.currentYearMonth.coerceIn(minYearMonth, maxYearMonth)
        val context = createFormatContext(input)
        val currentBsGrid = input.engine.buildMonthGrid(
            year = clampedYearMonth.year,
            month = clampedYearMonth.month,
            todayBs = input.todayBs,
            weekStart = input.preferences.weekStartDay
        )
        val visiblePages = buildVisiblePages(input, clampedYearMonth, currentBsGrid, context)

        return CalendarUiState(
            todayHero = buildTodayHero(input.todayBs, context),
            todayBs = input.todayBs,
            currentYearMonth = clampedYearMonth,
            selectedDate = input.selectedDate,
            minYearMonth = minYearMonth,
            maxYearMonth = maxYearMonth,
            monthTitle = CalendarDateFormatting.formatBsMonthYear(
                yearMonth = clampedYearMonth,
                language = context.effectiveLanguage,
                script = context.numeralScript
            ),
            adRangeSubtitle = CalendarDateFormatting.formatAdMonthRange(
                start = currentBsGrid.adStartDate,
                end = currentBsGrid.adEndDate,
                locale = context.effectiveLocale,
                script = context.numeralScript
            ),
            isShowingTodayMonth = clampedYearMonth == input.todayBs.toYearMonth(),
            monthGrid = visiblePages.getValue(clampedYearMonth),
            visiblePages = visiblePages,
            upcomingFestivals = buildFestivals(currentBsGrid, context),
            numeralScript = context.numeralScript,
            language = input.preferences.language,
            weekStartDay = input.preferences.weekStartDay
        )
    }

    fun buildMonthPage(input: CalendarRenderInput, yearMonth: BsYearMonth): MonthGridUiModel {
        val minYearMonth = input.engine.minBsDate.toYearMonth()
        val maxYearMonth = input.engine.maxBsDate.toYearMonth()
        val clamped = yearMonth.coerceIn(minYearMonth, maxYearMonth)
        val bsGrid = input.engine.buildMonthGrid(
            year = clamped.year,
            month = clamped.month,
            todayBs = input.todayBs,
            weekStart = input.preferences.weekStartDay
        )
        return mapMonthGrid(
            bsGrid = bsGrid,
            weekStartDay = input.preferences.weekStartDay,
            context = createFormatContext(input)
        )
    }

    private fun createFormatContext(input: CalendarRenderInput): MonthGridFormatContext {
        val effectiveLanguage = CalendarDateFormatting.resolveEffectiveLanguage(
            language = input.preferences.language,
            systemLocale = input.systemLocale
        )
        return MonthGridFormatContext(
            engine = input.engine,
            effectiveLanguage = effectiveLanguage,
            effectiveLocale = CalendarDateFormatting.resolveLocale(effectiveLanguage),
            numeralScript = input.preferences.numeralScript,
            selectedDate = input.selectedDate,
            reminderLookup = ReminderLookup(input.reminders.filter(CalendarReminder::isEnabled))
        )
    }

    private fun buildVisiblePages(
        input: CalendarRenderInput,
        clampedYearMonth: BsYearMonth,
        currentBsGrid: BsMonthGrid,
        formatContext: MonthGridFormatContext
    ): Map<BsYearMonth, MonthGridUiModel> {
        val minYearMonth = input.engine.minBsDate.toYearMonth()
        val maxYearMonth = input.engine.maxBsDate.toYearMonth()
        val pages = LinkedHashMap<BsYearMonth, MonthGridUiModel>(PRELOAD_MONTH_RADIUS * 2 + 1)

        for (offset in -PRELOAD_MONTH_RADIUS..PRELOAD_MONTH_RADIUS) {
            val candidate = clampedYearMonth.plusMonths(offset)
            if (candidate in minYearMonth..maxYearMonth) {
                val bsGrid = if (candidate == clampedYearMonth) {
                    currentBsGrid
                } else {
                    input.engine.buildMonthGrid(
                        year = candidate.year,
                        month = candidate.month,
                        todayBs = input.todayBs,
                        weekStart = input.preferences.weekStartDay
                    )
                }
                pages[candidate] = mapMonthGrid(
                    bsGrid = bsGrid,
                    weekStartDay = input.preferences.weekStartDay,
                    context = formatContext
                )
            }
        }
        return pages
    }

    private fun mapMonthGrid(
        bsGrid: BsMonthGrid,
        weekStartDay: WeekStartDay,
        context: MonthGridFormatContext
    ): MonthGridUiModel {
        val orderedWeekdays = BsDateFormatter.weekdaysInOrder(weekStartDay)
        val weekdayHeaders = orderedWeekdays.map { dayOfWeek ->
            BsDateFormatter.weekdayShortName(dayOfWeek).localized(context.effectiveLanguage)
        }
        val saturdayColumnIndex = orderedWeekdays.indexOf(DayOfWeek.SATURDAY)
        val days = bsGrid.days.map { day -> mapDayCell(day, context) }
        return MonthGridUiModel(
            weekdayHeaders = weekdayHeaders,
            saturdayColumnIndex = saturdayColumnIndex,
            days = days
        )
    }

    private fun mapDayCell(day: CalendarDay, context: MonthGridFormatContext): DayCellUiModel {
        val bsFullDate = BsDateFormatter.formatLongDate(
            date = day.bsDate,
            language = context.effectiveLanguage,
            script = context.numeralScript,
            dayOfWeek = day.dayOfWeek,
            zeroPadDay = false
        )
        val adFullDate = CalendarDateFormatting.formatAdFullDate(
            date = day.adDate,
            locale = context.effectiveLocale,
            script = context.numeralScript
        )
        return DayCellUiModel(
            id = day.bsDate.toIsoString(),
            bsDayText = BsDateFormatter.formatNumber(day.bsDate.day, context.numeralScript),
            adDayText = BsDateFormatter.formatNumber(day.adDay, context.numeralScript),
            isCurrentMonth = day.isCurrentMonth,
            isToday = day.isToday,
            isSelected = day.bsDate == context.selectedDate,
            isSaturday = day.isSaturday,
            isPublicHoliday = day.patro.isPublicHoliday,
            hasEvents = day.patro.events.isNotEmpty(),
            hasReminders = context.reminderLookup.hasReminderOn(day.bsDate, context.engine),
            accessibilityDescription = "$bsFullDate, $adFullDate"
        )
    }

    private fun buildTodayHero(todayBs: BsDate, context: MonthGridFormatContext): TodayHeroUiModel {
        val todayAd = context.engine.bsToAd(todayBs)
        val dayOfWeek = context.engine.dayOfWeek(todayBs)
        val patro = context.engine.patroInfo(todayBs)
        return TodayHeroUiModel(
            bsDayNumber = BsDateFormatter.formatNumber(todayBs.day, context.numeralScript),
            bsMonthYear = CalendarDateFormatting.formatBsMonthYear(
                yearMonth = todayBs.toYearMonth(),
                language = context.effectiveLanguage,
                script = context.numeralScript
            ),
            adDateLabel = CalendarDateFormatting.formatAdFullDate(
                date = todayAd,
                locale = context.effectiveLocale,
                script = context.numeralScript
            ),
            weekdayLabel = BsDateFormatter.weekdayName(dayOfWeek).localized(context.effectiveLanguage),
            tithiLabel = patro.tithi?.localized(context.effectiveLanguage),
            lunarPhase = context.engine.inferMoonPhase(patro.tithi)?.toVisual(),
            isEstimatedTithi = patro.tithiSource == TithiSource.ESTIMATED,
            primaryFestival = patro.events.firstOrNull()?.localized(context.effectiveLanguage),
            isHoliday = patro.isPublicHoliday
        )
    }

    private fun buildFestivals(bsGrid: BsMonthGrid, context: MonthGridFormatContext): List<FestivalSummaryUiModel> {
        val monthName = BsDateFormatter.monthName(bsGrid.yearMonth.month, context.effectiveLanguage)
        return bsGrid.days
            .asSequence()
            .filter { day -> day.isCurrentMonth && (day.patro.isPublicHoliday || day.patro.events.isNotEmpty()) }
            .map { day ->
                val localizedEvents = day.patro.events.map { event -> event.localized(context.effectiveLanguage) }
                val additionalEvents = localizedEvents
                    .drop(1)
                    .takeIf(List<String>::isNotEmpty)
                    ?.joinToString(separator = ADDITIONAL_EVENTS_SEPARATOR)
                FestivalSummaryUiModel(
                    bsDate = day.bsDate,
                    bsDayText = BsDateFormatter.formatNumber(day.bsDate.day, context.numeralScript),
                    bsMonthText = monthName,
                    adShortDateText = CalendarDateFormatting.formatAdShortDate(
                        date = day.adDate,
                        locale = context.effectiveLocale,
                        script = context.numeralScript
                    ),
                    primaryTitle = localizedEvents.firstOrNull(),
                    additionalEventsText = additionalEvents,
                    isPublicHoliday = day.patro.isPublicHoliday,
                    isToday = day.isToday
                )
            }
            .toList()
    }

    private fun MoonPhase.toVisual(): LunarPhaseVisual = when (this) {
        MoonPhase.NEW_MOON -> LunarPhaseVisual.NEW_MOON
        MoonPhase.WAXING_CRESCENT -> LunarPhaseVisual.WAXING_CRESCENT
        MoonPhase.FIRST_QUARTER -> LunarPhaseVisual.FIRST_QUARTER
        MoonPhase.WAXING_GIBBOUS -> LunarPhaseVisual.WAXING_GIBBOUS
        MoonPhase.FULL_MOON -> LunarPhaseVisual.FULL_MOON
        MoonPhase.WANING_GIBBOUS -> LunarPhaseVisual.WANING_GIBBOUS
        MoonPhase.LAST_QUARTER -> LunarPhaseVisual.LAST_QUARTER
        MoonPhase.WANING_CRESCENT -> LunarPhaseVisual.WANING_CRESCENT
    }
}
