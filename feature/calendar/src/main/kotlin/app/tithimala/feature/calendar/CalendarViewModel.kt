package app.tithimala.feature.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.tithimala.core.calendarengine.BikramSambatEngine
import app.tithimala.core.data.CalendarRepository
import app.tithimala.core.data.ReminderRepository
import app.tithimala.core.data.UserPreferencesRepository
import app.tithimala.core.model.BsDate
import app.tithimala.core.model.BsYearMonth
import app.tithimala.core.model.CalendarReminder
import app.tithimala.core.model.UserPreferences
import app.tithimala.core.ui.calendar.MonthGridUiModel
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn

private const val STOP_TIMEOUT_MILLIS = 5_000L

@HiltViewModel
class CalendarViewModel internal constructor(
    private val calendarRepository: CalendarRepository,
    userPreferencesRepository: UserPreferencesRepository,
    reminderRepository: ReminderRepository,
    private val todayProvider: () -> LocalDate,
    private val systemLocaleProvider: () -> Locale
) : ViewModel() {

    @Inject
    constructor(
        calendarRepository: CalendarRepository,
        userPreferencesRepository: UserPreferencesRepository,
        reminderRepository: ReminderRepository
    ) : this(
        calendarRepository = calendarRepository,
        userPreferencesRepository = userPreferencesRepository,
        reminderRepository = reminderRepository,
        todayProvider = { LocalDate.now() },
        systemLocaleProvider = { Locale.getDefault() }
    )

    private val initialEngine = calendarRepository.engine.value
    private val initialTodayBs = resolveTodayBs(initialEngine)

    private val currentYearMonthFlow = MutableStateFlow(initialTodayBs.toYearMonth())
    private val selectedDateFlow = MutableStateFlow(initialTodayBs)
    private val latestPreferences = MutableStateFlow(UserPreferences())
    private val latestReminders = MutableStateFlow<List<CalendarReminder>>(emptyList())

    private val _effects = Channel<CalendarEffect>(Channel.BUFFERED)
    internal val effects: Flow<CalendarEffect> = _effects.receiveAsFlow()

    internal val uiState: StateFlow<CalendarUiState> = combine(
        calendarRepository.engine,
        userPreferencesRepository.userPreferences,
        reminderRepository.observeAll(),
        currentYearMonthFlow,
        selectedDateFlow
    ) { engine, preferences, reminders, currentYearMonth, selectedDate ->
        latestPreferences.value = preferences
        latestReminders.value = reminders
        CalendarStateMapper.buildUiState(
            CalendarRenderInput(
                engine = engine,
                preferences = preferences,
                reminders = reminders,
                todayBs = resolveTodayBs(engine),
                currentYearMonth = currentYearMonth,
                selectedDate = selectedDate,
                systemLocale = systemLocaleProvider()
            )
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = CalendarStateMapper.buildUiState(
            CalendarRenderInput(
                engine = initialEngine,
                preferences = UserPreferences(),
                reminders = emptyList(),
                todayBs = initialTodayBs,
                currentYearMonth = initialTodayBs.toYearMonth(),
                selectedDate = initialTodayBs,
                systemLocale = systemLocaleProvider()
            )
        )
    )

    internal fun onAction(action: CalendarAction) {
        when (action) {
            is CalendarAction.SelectMonth -> selectMonth(action.yearMonth)
            CalendarAction.PreviousMonth -> stepMonth(delta = -1)
            CalendarAction.NextMonth -> stepMonth(delta = 1)
            CalendarAction.JumpToToday -> jumpToToday()
            is CalendarAction.SelectDay -> selectDay(action.bsDate)
            is CalendarAction.OpenDayDetail -> openDayDetail(action.bsDate)
        }
    }

    internal fun buildMonthPage(yearMonth: BsYearMonth): MonthGridUiModel {
        val engine = calendarRepository.engine.value
        return CalendarStateMapper.buildMonthPage(
            input = CalendarRenderInput(
                engine = engine,
                preferences = latestPreferences.value,
                reminders = latestReminders.value,
                todayBs = resolveTodayBs(engine),
                currentYearMonth = currentYearMonthFlow.value,
                selectedDate = selectedDateFlow.value,
                systemLocale = systemLocaleProvider()
            ),
            yearMonth = yearMonth
        )
    }

    private fun selectMonth(yearMonth: BsYearMonth) {
        val engine = calendarRepository.engine.value
        val clamped = yearMonth.coerceIn(
            minimumValue = engine.minBsDate.toYearMonth(),
            maximumValue = engine.maxBsDate.toYearMonth()
        )
        currentYearMonthFlow.value = clamped
    }

    private fun stepMonth(delta: Int) {
        val engine = calendarRepository.engine.value
        val minYearMonth = engine.minBsDate.toYearMonth()
        val maxYearMonth = engine.maxBsDate.toYearMonth()
        val target = currentYearMonthFlow.value.plusMonths(delta)
        if (target in minYearMonth..maxYearMonth && target != currentYearMonthFlow.value) {
            currentYearMonthFlow.value = target
            _effects.trySend(CalendarEffect.ScrollToMonth(target))
        }
    }

    private fun jumpToToday() {
        val today = resolveTodayBs(calendarRepository.engine.value)
        val todayYearMonth = today.toYearMonth()
        selectedDateFlow.value = today
        currentYearMonthFlow.value = todayYearMonth
        _effects.trySend(CalendarEffect.ScrollToMonth(todayYearMonth))
    }

    private fun selectDay(bsDate: BsDate) {
        val engine = calendarRepository.engine.value
        if (!engine.isValidBsDate(bsDate.year, bsDate.month, bsDate.day)) {
            return
        }
        selectedDateFlow.value = bsDate
        val targetYearMonth = bsDate.toYearMonth()
        if (targetYearMonth != currentYearMonthFlow.value) {
            currentYearMonthFlow.value = targetYearMonth
            _effects.trySend(CalendarEffect.ScrollToMonth(targetYearMonth))
        }
    }

    private fun openDayDetail(bsDate: BsDate) {
        val engine = calendarRepository.engine.value
        if (!engine.isValidBsDate(bsDate.year, bsDate.month, bsDate.day)) {
            return
        }
        selectedDateFlow.value = bsDate
        _effects.trySend(CalendarEffect.NavigateToDay(bsDate))
    }

    private fun resolveTodayBs(engine: BikramSambatEngine): BsDate {
        val todayAd = todayProvider()
        return when {
            todayAd < engine.minAdDate -> engine.minBsDate
            todayAd > engine.maxAdDate -> engine.maxBsDate
            else -> engine.adToBs(todayAd)
        }
    }
}
