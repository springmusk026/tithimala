package app.tithimala.core.notifications

/**
 * Schedules and cancels local reminder alarms on the device.
 */
interface ReminderAlarmScheduler {
    fun schedule(request: ReminderTriggerRequest)
    fun cancel(reminderId: Long)
}
