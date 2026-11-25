package app.tithimala.feature.day

import androidx.compose.runtime.Immutable
import app.tithimala.core.model.BsDate
import app.tithimala.core.model.ReminderRecurrence
import app.tithimala.core.model.TithiSource
import app.tithimala.core.ui.calendar.LunarPhaseVisual

/**
 * Immutable presentation model for the Day Detail screen.
 */
@Immutable
internal data class DayUiState(
    val isLoading: Boolean = false,
    val bsDate: BsDate = DEFAULT_BS_DATE,
    val bsDayNumber: String = "",
    val bsMonthYear: String = "",
    val bsFullDateLabel: String = "",
    val weekdayLabel: String = "",
    val adDateLabel: String = "",
    val tithiLabel: String? = null,
    val tithiSource: TithiSource = TithiSource.UNAVAILABLE,
    val lunarPhase: LunarPhaseVisual? = null,
    val isPublicHoliday: Boolean = false,
    val isSaturday: Boolean = false,
    val isToday: Boolean = false,
    val events: List<String> = emptyList(),
    val reminders: List<DayReminderUiModel> = emptyList(),
    val canNavigatePrevious: Boolean = false,
    val canNavigateNext: Boolean = false,
    val copyableDateText: String = ""
) {
    companion object {
        val DEFAULT_BS_DATE = BsDate(year = 2083, month = 8, day = 10)
        val LOADING = DayUiState(isLoading = true)
    }
}

/**
 * Immutable presentation model for a single Bikram Sambat reminder on the selected day.
 */
@Immutable
internal data class DayReminderUiModel(
    val id: Long,
    val title: String,
    val notes: String,
    val formattedTime: String,
    val recurrence: ReminderRecurrence,
    val isEnabled: Boolean
)

/**
 * User intents dispatched from [DayScreen] to [DayViewModel].
 */
internal sealed interface DayAction {
    data object PreviousDay : DayAction
    data object NextDay : DayAction
    data class ToggleReminder(val id: Long, val enabled: Boolean) : DayAction
    data class DeleteReminder(val id: Long) : DayAction
    data object AddReminderClicked : DayAction
    data object CopyDateClicked : DayAction
}

/**
 * One-off UI effects emitted by [DayViewModel].
 */
internal sealed interface DayEffect {
    data class OpenAddReminder(val bsDate: BsDate) : DayEffect
    data class CopyToClipboard(val formattedText: String) : DayEffect
}
