package app.tithimala.core.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context

/**
 * Notification channels registered by Tithimala.
 */
object NotificationChannels {
    const val REMINDERS_CHANNEL_ID = "tithimala_reminders"

    /**
     * Ensures the localized reminder notification channel exists and reflects the active app locale.
     */
    fun ensureReminderChannel(context: Context) {
        val notificationManager = context.getSystemService(NotificationManager::class.java) ?: return
        val channel = NotificationChannel(
            REMINDERS_CHANNEL_ID,
            context.getString(R.string.notification_channel_reminders_name),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = context.getString(R.string.notification_channel_reminders_description)
            enableVibration(true)
            setShowBadge(true)
        }
        notificationManager.createNotificationChannel(channel)
    }
}
