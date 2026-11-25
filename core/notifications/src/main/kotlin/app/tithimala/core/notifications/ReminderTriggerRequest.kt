package app.tithimala.core.notifications

/**
 * Immutable trigger payload for scheduling a Bikram Sambat reminder alarm.
 */
data class ReminderTriggerRequest(
    val reminderId: Long,
    val title: String,
    val notes: String,
    val triggerAtEpochMillis: Long,
    val isYearlyBs: Boolean,
    val bsYear: Int,
    val bsMonth: Int,
    val bsDay: Int,
    val hour: Int,
    val minute: Int
)
