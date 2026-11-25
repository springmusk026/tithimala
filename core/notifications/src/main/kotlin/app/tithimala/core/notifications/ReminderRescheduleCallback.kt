package app.tithimala.core.notifications

import android.content.Context
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

/**
 * Trigger reason describing why active reminder alarms need to be re-evaluated and rescheduled.
 */
enum class ReminderRescheduleReason {
    BOOT_COMPLETED,
    TIME_CHANGED,
    TIMEZONE_CHANGED,
    YEARLY_REMINDER_FIRED
}

/**
 * Decoupled callback invoked when device boot, clock/timezone changes, or a fired yearly BS
 * reminder requires rescheduling enabled reminders.
 *
 * Higher modules (such as `:core:data` or `:app`) can bind implementations with Hilt `@IntoSet`.
 */
fun interface ReminderRescheduleCallback {
    fun onRescheduleRequested(context: Context, reason: ReminderRescheduleReason)
}

@EntryPoint
@InstallIn(SingletonComponent::class)
internal interface NotificationsEntryPoint {
    fun rescheduleCallbacks(): Set<ReminderRescheduleCallback>
}

internal object ReminderRescheduleDispatcher {
    fun dispatch(context: Context, reason: ReminderRescheduleReason) {
        val appContext = context.applicationContext ?: context
        val callbacks = try {
            EntryPointAccessors.fromApplication(
                appContext,
                NotificationsEntryPoint::class.java
            ).rescheduleCallbacks()
        } catch (_: IllegalStateException) {
            return
        }
        callbacks.forEach { callback ->
            callback.onRescheduleRequested(appContext, reason)
        }
    }
}
