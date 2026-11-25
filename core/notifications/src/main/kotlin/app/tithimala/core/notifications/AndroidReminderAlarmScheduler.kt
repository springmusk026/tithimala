package app.tithimala.core.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.os.Build
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

internal class AndroidReminderAlarmScheduler @Inject constructor(@ApplicationContext private val context: Context) :
    ReminderAlarmScheduler {

    override fun schedule(request: ReminderTriggerRequest) {
        val alarmManager = context.getSystemService(AlarmManager::class.java) ?: return
        val pendingIntent = buildPendingIntent(request)

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()) {
            try {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    request.triggerAtEpochMillis,
                    pendingIntent
                )
                return
            } catch (_: SecurityException) {
                // Fall back to an inexact alarm if exact-alarm permission is revoked mid-call.
            }
        }

        alarmManager.setAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            request.triggerAtEpochMillis,
            pendingIntent
        )
    }

    override fun cancel(reminderId: Long) {
        val alarmManager = context.getSystemService(AlarmManager::class.java) ?: return
        val existingPendingIntent = PendingIntent.getBroadcast(
            context,
            requestCodeFor(reminderId),
            ReminderAlarmReceiver.createCancelIntent(context),
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        ) ?: return

        alarmManager.cancel(existingPendingIntent)
        existingPendingIntent.cancel()
    }

    private fun buildPendingIntent(request: ReminderTriggerRequest): PendingIntent = PendingIntent.getBroadcast(
        context,
        requestCodeFor(request.reminderId),
        ReminderAlarmReceiver.createIntent(context, request),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    private fun requestCodeFor(reminderId: Long): Int = reminderId.toInt()
}
