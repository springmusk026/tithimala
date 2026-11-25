package app.tithimala.core.model

import kotlinx.serialization.Serializable

@Serializable
enum class ReminderRecurrence {
    ONE_TIME,
    YEARLY_BS
}

/**
 * User-created reminder anchored to a Bikram Sambat date.
 */
@Serializable
data class CalendarReminder(
    val id: Long = 0L,
    val title: String,
    val notes: String = "",
    val bsYear: Int,
    val bsMonth: Int,
    val bsDay: Int,
    val hour: Int = DEFAULT_HOUR,
    val minute: Int = 0,
    val recurrence: ReminderRecurrence = ReminderRecurrence.ONE_TIME,
    val isEnabled: Boolean = true
) {
    init {
        require(bsMonth in BsDate.MIN_MONTH..BsDate.MAX_MONTH) {
            "Reminder BS month must be between ${BsDate.MIN_MONTH} and ${BsDate.MAX_MONTH}, got $bsMonth"
        }
        require(bsDay in BsDate.MIN_DAY..BsDate.MAX_DAY) {
            "Reminder BS day must be between ${BsDate.MIN_DAY} and ${BsDate.MAX_DAY}, got $bsDay"
        }
        require(hour in MIN_HOUR..MAX_HOUR) {
            "Reminder hour must be between $MIN_HOUR and $MAX_HOUR, got $hour"
        }
        require(minute in MIN_MINUTE..MAX_MINUTE) {
            "Reminder minute must be between $MIN_MINUTE and $MAX_MINUTE, got $minute"
        }
    }

    val bsDate: BsDate
        get() = BsDate(year = bsYear, month = bsMonth, day = bsDay)

    companion object {
        private const val DEFAULT_HOUR = 9
        private const val MIN_HOUR = 0
        private const val MAX_HOUR = 23
        private const val MIN_MINUTE = 0
        private const val MAX_MINUTE = 59
    }
}
