package app.tithimala.feature.converter

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.tithimala.core.calendarengine.BikramSambatEngine
import app.tithimala.core.data.CalendarRepository
import app.tithimala.core.data.UserPreferencesRepository
import app.tithimala.core.model.UserPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class ConverterViewModel private constructor(
    private val calendarRepository: CalendarRepository,
    userPreferencesRepository: UserPreferencesRepository,
    private val todayProvider: () -> LocalDate
) : ViewModel() {

    @Inject
    constructor(
        calendarRepository: CalendarRepository,
        userPreferencesRepository: UserPreferencesRepository
    ) : this(
        calendarRepository = calendarRepository,
        userPreferencesRepository = userPreferencesRepository,
        todayProvider = LocalDate::now
    )

    @Volatile
    private var latestPreferences: UserPreferences = UserPreferences()

    private val inputDraft = MutableStateFlow(createTodayDraft(ConversionDirection.BS_TO_AD))

    private val _effects = Channel<ConverterEffect>(Channel.BUFFERED)
    internal val effects: Flow<ConverterEffect> = _effects.receiveAsFlow()

    internal val uiState: StateFlow<ConverterUiState> = combine(
        inputDraft,
        calendarRepository.engine,
        userPreferencesRepository.userPreferences.onEach { latestPreferences = it }
    ) { draft, engine, preferences ->
        ConverterEvaluator.evaluate(draft = draft, engine = engine, preferences = preferences)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = ConverterEvaluator.evaluate(
            draft = inputDraft.value,
            engine = calendarRepository.engine.value,
            preferences = latestPreferences
        )
    )

    internal fun onAction(action: ConverterAction) {
        when (action) {
            is ConverterAction.SetDirection -> switchDirection(action.direction)
            ConverterAction.SwapDirection -> switchDirection(inputDraft.value.direction.opposite())
            is ConverterAction.UpdateYear -> {
                val sanitized = ConverterEvaluator.sanitizeDigits(action.value, ConverterEvaluator.MAX_YEAR_LENGTH)
                inputDraft.update { it.copy(yearInput = sanitized) }
            }
            is ConverterAction.SelectMonth -> {
                val clamped = action.month.coerceIn(ConverterEvaluator.MIN_MONTH, ConverterEvaluator.MAX_MONTH)
                inputDraft.update { it.copy(monthIndex = clamped) }
            }
            is ConverterAction.UpdateDay -> {
                val sanitized = ConverterEvaluator.sanitizeDigits(action.value, ConverterEvaluator.MAX_DAY_LENGTH)
                inputDraft.update { it.copy(dayInput = sanitized) }
            }
            is ConverterAction.StepDay -> stepDay(action.delta)
            ConverterAction.ResetToToday -> {
                inputDraft.value = createTodayDraft(inputDraft.value.direction)
            }
            ConverterAction.CopyResult -> {
                val copyText = currentEvaluatedState().result?.copyText ?: return
                emitEffect(ConverterEffect.CopyToClipboard(copyText))
            }
            ConverterAction.OpenInCalendar -> {
                val bsDate = currentEvaluatedState().result?.bsDate ?: return
                emitEffect(ConverterEffect.OpenDay(bsDate))
            }
        }
    }

    private fun switchDirection(targetDirection: ConversionDirection) {
        if (targetDirection == inputDraft.value.direction) return
        val currentResult = currentEvaluatedState().result
        inputDraft.value = if (currentResult != null) {
            when (targetDirection) {
                ConversionDirection.BS_TO_AD -> ConverterInputDraft(
                    direction = targetDirection,
                    yearInput = currentResult.bsDate.year.toString(),
                    monthIndex = currentResult.bsDate.month,
                    dayInput = currentResult.bsDate.day.toString()
                )
                ConversionDirection.AD_TO_BS -> ConverterInputDraft(
                    direction = targetDirection,
                    yearInput = currentResult.adDate.year.toString(),
                    monthIndex = currentResult.adDate.monthValue,
                    dayInput = currentResult.adDate.dayOfMonth.toString()
                )
            }
        } else {
            createTodayDraft(targetDirection)
        }
    }

    private fun stepDay(delta: Int) {
        if (delta == 0) return
        val current = currentEvaluatedState()
        val maxDay = current.maxDayForCurrentMonth
        val parsedDay = ConverterEvaluator.parseDigits(inputDraft.value.dayInput)
        val nextDay = if (parsedDay == null) {
            if (delta > 0) ConverterEvaluator.MIN_DAY else maxDay
        } else {
            (parsedDay + delta).coerceIn(ConverterEvaluator.MIN_DAY, maxDay)
        }
        inputDraft.update { it.copy(dayInput = nextDay.toString()) }
    }

    private fun currentEvaluatedState(): ConverterUiState = ConverterEvaluator.evaluate(
        draft = inputDraft.value,
        engine = calendarRepository.engine.value,
        preferences = latestPreferences
    )

    private fun createTodayDraft(direction: ConversionDirection): ConverterInputDraft {
        val engine = calendarRepository.engine.value
        val todayAd = todayProvider().coerceInRange(engine)
        return when (direction) {
            ConversionDirection.BS_TO_AD -> {
                val todayBs = calendarRepository.todayBs(todayAd)
                ConverterInputDraft(
                    direction = direction,
                    yearInput = todayBs.year.toString(),
                    monthIndex = todayBs.month,
                    dayInput = todayBs.day.toString()
                )
            }
            ConversionDirection.AD_TO_BS -> ConverterInputDraft(
                direction = direction,
                yearInput = todayAd.year.toString(),
                monthIndex = todayAd.monthValue,
                dayInput = todayAd.dayOfMonth.toString()
            )
        }
    }

    private fun emitEffect(effect: ConverterEffect) {
        if (_effects.trySend(effect).isFailure) {
            viewModelScope.launch { _effects.send(effect) }
        }
    }

    internal companion object {
        private const val STOP_TIMEOUT_MILLIS = 5_000L

        internal fun createForTest(
            calendarRepository: CalendarRepository,
            userPreferencesRepository: UserPreferencesRepository,
            todayProvider: () -> LocalDate
        ): ConverterViewModel = ConverterViewModel(
            calendarRepository = calendarRepository,
            userPreferencesRepository = userPreferencesRepository,
            todayProvider = todayProvider
        )
    }
}

private fun LocalDate.coerceInRange(engine: BikramSambatEngine): LocalDate = when {
    isBefore(engine.minAdDate) -> engine.minAdDate
    isAfter(engine.maxAdDate) -> engine.maxAdDate
    else -> this
}
