package app.tithimala.core.notifications

import android.Manifest
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

/**
 * Receives scheduled reminder alarms and posts a Himalayan Dawn styled notification.
 */
class ReminderAlarmReceiver internal constructor(
    private val onReschedule: (Context, ReminderRescheduleReason) -> Unit
) : BroadcastReceiver() {

    constructor() : this(ReminderRescheduleDispatcher::dispatch)

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent == null) return
        val reminderId = intent.getLongExtra(EXTRA_REMINDER_ID, INVALID_REMINDER_ID)
        if (reminderId < 0L) return

        val title = intent.getStringExtra(EXTRA_TITLE).orEmpty()
        val notes = intent.getStringExtra(EXTRA_NOTES).orEmpty()
        val isYearlyBs = intent.getBooleanExtra(EXTRA_IS_YEARLY_BS, false)
        val bsYear = intent.getIntExtra(EXTRA_BS_YEAR, UNSET_DATE_COMPONENT)
        val bsMonth = intent.getIntExtra(EXTRA_BS_MONTH, UNSET_DATE_COMPONENT)
        val bsDay = intent.getIntExtra(EXTRA_BS_DAY, UNSET_DATE_COMPONENT)

        NotificationChannels.ensureReminderChannel(context)
        postNotification(
            context = context,
            payload = NotificationPayload(
                reminderId = reminderId,
                title = title,
                notes = notes,
                isYearlyBs = isYearlyBs,
                bsYear = bsYear,
                bsMonth = bsMonth,
                bsDay = bsDay
            )
        )

        if (isYearlyBs) {
            onReschedule(
                context.applicationContext ?: context,
                ReminderRescheduleReason.YEARLY_REMINDER_FIRED
            )
        }
    }

    private fun postNotification(context: Context, payload: NotificationPayload) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val notificationManager = NotificationManagerCompat.from(context)
        if (!notificationManager.areNotificationsEnabled()) {
            return
        }

        val resolvedTitle = payload.title.ifBlank {
            context.getString(R.string.notification_reminder_default_title)
        }
        val resolvedBody = when {
            payload.notes.isNotBlank() -> payload.notes
            payload.isYearlyBs -> context.getString(R.string.notification_reminder_yearly_subtitle)
            else -> context.getString(R.string.notification_reminder_default_body)
        }

        val builder = NotificationCompat.Builder(context, NotificationChannels.REMINDERS_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_reminder)
            .setColor(ContextCompat.getColor(context, R.color.notification_accent_sindoor))
            .setContentTitle(resolvedTitle)
            .setContentText(resolvedBody)
            .setStyle(NotificationCompat.BigTextStyle().bigText(resolvedBody))
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)

        if (payload.isYearlyBs) {
            builder.setSubText(context.getString(R.string.notification_reminder_yearly_tag))
        }

        buildLaunchPendingIntent(context, payload)?.let(builder::setContentIntent)

        notificationManager.notify(payload.reminderId.toInt(), builder.build())
    }

    private fun buildLaunchPendingIntent(context: Context, payload: NotificationPayload): PendingIntent? {
        val launchIntent = context.packageManager.getLaunchIntentForPackage(context.packageName) ?: return null
        launchIntent.apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_REMINDER_ID, payload.reminderId)
            putExtra(EXTRA_BS_YEAR, payload.bsYear)
            putExtra(EXTRA_BS_MONTH, payload.bsMonth)
            putExtra(EXTRA_BS_DAY, payload.bsDay)
        }
        return PendingIntent.getActivity(
            context,
            payload.reminderId.toInt(),
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private data class NotificationPayload(
        val reminderId: Long,
        val title: String,
        val notes: String,
        val isYearlyBs: Boolean,
        val bsYear: Int,
        val bsMonth: Int,
        val bsDay: Int
    )

    companion object {
        const val EXTRA_REMINDER_ID = "app.tithimala.extra.REMINDER_ID"
        const val EXTRA_BS_YEAR = "app.tithimala.extra.BS_YEAR"
        const val EXTRA_BS_MONTH = "app.tithimala.extra.BS_MONTH"
        const val EXTRA_BS_DAY = "app.tithimala.extra.BS_DAY"

        internal const val ACTION_REMINDER_TRIGGER = "app.tithimala.action.REMINDER_TRIGGER"
        internal const val EXTRA_TITLE = "app.tithimala.extra.REMINDER_TITLE"
        internal const val EXTRA_NOTES = "app.tithimala.extra.REMINDER_NOTES"
        internal const val EXTRA_IS_YEARLY_BS = "app.tithimala.extra.REMINDER_IS_YEARLY_BS"
        internal const val EXTRA_HOUR = "app.tithimala.extra.REMINDER_HOUR"
        internal const val EXTRA_MINUTE = "app.tithimala.extra.REMINDER_MINUTE"

        private const val INVALID_REMINDER_ID = -1L
        private const val UNSET_DATE_COMPONENT = 0

        internal fun createIntent(context: Context, request: ReminderTriggerRequest): Intent =
            Intent(context, ReminderAlarmReceiver::class.java).apply {
                action = ACTION_REMINDER_TRIGGER
                putExtra(EXTRA_REMINDER_ID, request.reminderId)
                putExtra(EXTRA_TITLE, request.title)
                putExtra(EXTRA_NOTES, request.notes)
                putExtra(EXTRA_IS_YEARLY_BS, request.isYearlyBs)
                putExtra(EXTRA_BS_YEAR, request.bsYear)
                putExtra(EXTRA_BS_MONTH, request.bsMonth)
                putExtra(EXTRA_BS_DAY, request.bsDay)
                putExtra(EXTRA_HOUR, request.hour)
                putExtra(EXTRA_MINUTE, request.minute)
            }

        internal fun createCancelIntent(context: Context): Intent =
            Intent(context, ReminderAlarmReceiver::class.java).apply {
                action = ACTION_REMINDER_TRIGGER
            }
    }
}
