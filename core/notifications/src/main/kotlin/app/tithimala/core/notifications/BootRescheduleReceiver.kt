package app.tithimala.core.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Listens for device boot and clock/timezone changes and notifies registered
 * [ReminderRescheduleCallback] hooks so active Bikram Sambat reminders are rescheduled.
 */
class BootRescheduleReceiver internal constructor(
    private val onReschedule: (Context, ReminderRescheduleReason) -> Unit
) : BroadcastReceiver() {

    constructor() : this(ReminderRescheduleDispatcher::dispatch)

    override fun onReceive(context: Context, intent: Intent?) {
        val reason = when (intent?.action) {
            Intent.ACTION_BOOT_COMPLETED -> ReminderRescheduleReason.BOOT_COMPLETED
            Intent.ACTION_TIME_CHANGED -> ReminderRescheduleReason.TIME_CHANGED
            Intent.ACTION_TIMEZONE_CHANGED -> ReminderRescheduleReason.TIMEZONE_CHANGED
            else -> return
        }
        onReschedule(context.applicationContext ?: context, reason)
    }
}
