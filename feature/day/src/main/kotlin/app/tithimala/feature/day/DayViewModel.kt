package app.tithimala.feature.day

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.tithimala.core.calendarengine.BikramSambatEngine
import app.tithimala.core.calendarengine.BsDateFormatter
import app.tithimala.core.data.CalendarRepository
import app.tithimala.core.data.ReminderRepository
import app.tithimala.core.data.UserPreferencesRepository
import app.tithimala.core.model.AppLanguage
import app.tithimala.core.model.BsDate
import app.tithimala.core.model.CalendarReminder
import app.tithimala.core.model.MoonPhase
import app.tithimala.core.model.NumeralScript
import app.tithimala.core.model.UserPreferences
import app.tithimala.core.ui.calendar.LunarPhaseVisual
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * State holder for the Day Detail screen.
 *
 * Combines the active Bikram Sambat date with the verified calendar engine, persisted user
 * preferences, and reminders anchored to that date.
 */
@HiltViewModel
class DayViewModel @Inject constructor(
    private val calendarRepository: CalendarRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val reminderRepository: ReminderRepository
) : ViewModel() {

    private val selectedDate = MutableStateFlow<BsDate?>(null)
    private var lastLoadedDateArg: BsDate? = null

    private val effectChannel = Channel<DayEffect>(Channel.BUFFERED)
    internal val effects: Flow<DayEffect> = effectChannel.receiveAsFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    internal val uiState: StateFlow<DayUiState> = selectedDate
        .filterNotNull()
        .flatMapLatest { date ->
            combine(
                calendarRepository.engine,
                userPreferencesRepository.userPreferences,
                reminderRepository.observeForDate(date)
            ) { engine, preferences, reminders ->
                buildDayUiState(
                    date = date,
                    engine = engine,
                    preferences = preferences,
                    reminders = reminders
                )
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = DayUiState.LOADING
        )

    internal fun loadDate(bsDate: BsDate) {
        if (lastLoadedDateArg == bsDate && selectedDate.value != null) return
        lastLoadedDateArg = bsDate
        selectedDate.value = bsDate
    }

    internal fun onAction(action: DayAction) {
        when (action) {
            DayAction.PreviousDay -> navigateByDays(PREVIOUS_DAY_OFFSET)
            DayAction.NextDay -> navigateByDays(NEXT_DAY_OFFSET)
            is DayAction.ToggleReminder -> toggleReminder(id = action.id, enabled = action.enabled)
            is DayAction.DeleteReminder -> deleteReminder(id = action.id)
            DayAction.AddReminderClicked -> emitOpenAddReminder()
            DayAction.CopyDateClicked -> emitCopyDate()
        }
    }

    private fun navigateByDays(offset: Long) {
        val current = selectedDate.value
        val engine = calendarRepository.engine.value
        val target = current
            ?.takeIf { engine.isValidBsDate(it.year, it.month, it.day) }
            ?.let { engine.addDaysOrNull(it, offset) }
        if (target != null) {
            selectedDate.value = target
        }
    }

    private fun toggleReminder(id: Long, enabled: Boolean) {
        viewModelScope.launch {
            reminderRepository.setEnabled(id = id, enabled = enabled)
        }
    }

    private fun deleteReminder(id: Long) {
        viewModelScope.launch {
            reminderRepository.deleteById(id)
        }
    }

    private fun emitOpenAddReminder() {
        val current = selectedDate.value ?: return
        viewModelScope.launch {
            effectChannel.send(DayEffect.OpenAddReminder(current))
        }
    }

    private fun emitCopyDate() {
        val currentText = uiState.value.copyableDateText
        if (currentText.isNotEmpty()) {
            viewModelScope.launch {
                effectChannel.send(DayEffect.CopyToClipboard(currentText))
            }
            return
        }
        val currentDate = selectedDate.value ?: return
        viewModelScope.launch {
            val preferences = userPreferencesRepository.userPreferences.first()
            val computed = buildDayUiState(
                date = currentDate,
                engine = calendarRepository.engine.value,
                preferences = preferences,
                reminders = emptyList()
            )
            if (computed.copyableDateText.isNotEmpty()) {
                effectChannel.send(DayEffect.CopyToClipboard(computed.copyableDateText))
            }
        }
    }

    private fun buildDayUiState(
        date: BsDate,
        engine: BikramSambatEngine,
        preferences: UserPreferences,
        reminders: List<CalendarReminder>
    ): DayUiState {
        val language = preferences.language
        val script = preferences.numeralScript
        val isValid = engine.isValidBsDate(date.year, date.month, date.day)
        val patroInfo = calendarRepository.patroInfo(date)
        val adDate = if (isValid) engine.bsToAd(date) else null
        val dayOfWeek = if (isValid) engine.dayOfWeek(date) else null
        val todayBs = runCatching { calendarRepository.todayBs() }.getOrNull()

        val bsMonthYear = "${BsDateFormatter.monthName(date.month, language)} " +
            BsDateFormatter.formatNumber(date.year, script)
        val bsFullDateLabel = BsDateFormatter.formatLongDate(
            date = date,
            language = language,
            script = script,
            dayOfWeek = dayOfWeek,
            zeroPadDay = false
        )
        val adDateLabel = adDate?.let { formatAdDate(it, language, script) }.orEmpty()

        // Preserve trailing `*` and ` (K)` markers verbatim from upstream Patro data.
        val tithiLabel = patroInfo.tithi?.localized(language)
        val lunarPhase = tithiLabel?.let { engine.inferMoonPhase(patroInfo.tithi)?.toLunarPhaseVisual() }

        return DayUiState(
            isLoading = false,
            bsDate = date,
            bsDayNumber = BsDateFormatter.formatNumber(date.day, script),
            bsMonthYear = bsMonthYear,
            bsFullDateLabel = bsFullDateLabel,
            weekdayLabel = dayOfWeek?.let { BsDateFormatter.weekdayName(it).localized(language) }.orEmpty(),
            adDateLabel = adDateLabel,
            tithiLabel = tithiLabel,
            tithiSource = patroInfo.tithiSource,
            lunarPhase = lunarPhase,
            isPublicHoliday = patroInfo.isPublicHoliday,
            isSaturday = dayOfWeek == DayOfWeek.SATURDAY,
            isToday = todayBs == date,
            events = patroInfo.events.map { it.localized(language) },
            reminders = reminders.map { it.toUiModel(script) },
            canNavigatePrevious = isValid && engine.addDaysOrNull(date, PREVIOUS_DAY_OFFSET) != null,
            canNavigateNext = isValid && engine.addDaysOrNull(date, NEXT_DAY_OFFSET) != null,
            copyableDateText = buildCopyableDateText(bsFullDateLabel, adDateLabel, tithiLabel)
        )
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
        const val PREVIOUS_DAY_OFFSET = -1L
        const val NEXT_DAY_OFFSET = 1L
        const val TWO_DIGIT_WIDTH = 2
        const val TIME_STRING_LENGTH = 5
        const val NEPALI_LANGUAGE_CODE = "ne"
        val NEPALI_LOCALE: Locale = Locale.forLanguageTag(NEPALI_LANGUAGE_CODE)

        fun formatAdDate(adDate: LocalDate, language: AppLanguage, script: NumeralScript): String {
            val displayLocale = resolveDisplayLocale(language)
            val monthName = adDate.month.getDisplayName(TextStyle.FULL, displayLocale)
            val dayText = BsDateFormatter.formatNumber(adDate.dayOfMonth, script)
            val yearText = BsDateFormatter.formatNumber(adDate.year, script)
            return "$dayText $monthName $yearText"
        }

        fun resolveDisplayLocale(language: AppLanguage): Locale = when (language) {
            AppLanguage.NEPALI -> NEPALI_LOCALE
            AppLanguage.ENGLISH -> Locale.ENGLISH
            AppLanguage.SYSTEM -> {
                if (Locale.getDefault().language.equals(NEPALI_LANGUAGE_CODE, ignoreCase = true)) {
                    NEPALI_LOCALE
                } else {
                    Locale.ENGLISH
                }
            }
        }

        fun CalendarReminder.toUiModel(script: NumeralScript): DayReminderUiModel = DayReminderUiModel(
            id = id,
            title = title,
            notes = notes,
            formattedTime = formatReminderTime(hour, minute, script),
            recurrence = recurrence,
            isEnabled = isEnabled
        )

        fun formatReminderTime(hour: Int, minute: Int, script: NumeralScript): String {
            val raw = buildString(TIME_STRING_LENGTH) {
                append(hour.toString().padStart(TWO_DIGIT_WIDTH, '0'))
                append(':')
                append(minute.toString().padStart(TWO_DIGIT_WIDTH, '0'))
            }
            return BsDateFormatter.formatDigits(raw, script)
        }

        fun buildCopyableDateText(bsFullDateLabel: String, adDateLabel: String, tithiLabel: String?): String =
            buildString {
                append(bsFullDateLabel)
                if (adDateLabel.isNotEmpty()) {
                    append(" (")
                    append(adDateLabel)
                    append(')')
                }
                if (!tithiLabel.isNullOrEmpty()) {
                    append(" · ")
                    append(tithiLabel)
                }
            }
    }
}

private fun MoonPhase.toLunarPhaseVisual(): LunarPhaseVisual = when (this) {
    MoonPhase.NEW_MOON -> LunarPhaseVisual.NEW_MOON
    MoonPhase.WAXING_CRESCENT -> LunarPhaseVisual.WAXING_CRESCENT
    MoonPhase.FIRST_QUARTER -> LunarPhaseVisual.FIRST_QUARTER
    MoonPhase.WAXING_GIBBOUS -> LunarPhaseVisual.WAXING_GIBBOUS
    MoonPhase.FULL_MOON -> LunarPhaseVisual.FULL_MOON
    MoonPhase.WANING_GIBBOUS -> LunarPhaseVisual.WANING_GIBBOUS
    MoonPhase.LAST_QUARTER -> LunarPhaseVisual.LAST_QUARTER
    MoonPhase.WANING_CRESCENT -> LunarPhaseVisual.WANING_CRESCENT
}
