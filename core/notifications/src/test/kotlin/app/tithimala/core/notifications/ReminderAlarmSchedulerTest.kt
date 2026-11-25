package app.tithimala.core.notifications

import android.Manifest
import android.app.AlarmManager
import android.app.Application
import android.app.Notification
import android.app.NotificationManager
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowAlarmManager

@RunWith(AndroidJUnit4::class)
@Config(sdk = [36])
class ReminderAlarmSchedulerTest {

    private lateinit var application: Application
    private lateinit var alarmManager: AlarmManager
    private lateinit var scheduler: AndroidReminderAlarmScheduler

    private val sampleRequest = ReminderTriggerRequest(
        reminderId = 17L,
        title = "Baisakh Sankranti",
        notes = "Call parents in the morning",
        triggerAtEpochMillis = 1_776_133_800_000L,
        isYearlyBs = true,
        bsYear = 2083,
        bsMonth = 1,
        bsDay = 1,
        hour = 8,
        minute = 15
    )

    @Before
    fun setUp() {
        application = ApplicationProvider.getApplicationContext()
        alarmManager = application.getSystemService(AlarmManager::class.java)
        scheduler = AndroidReminderAlarmScheduler(application)
    }

    @Test
    fun scheduleUsesExactAlarmWhenExactPermissionIsGranted() {
        ShadowAlarmManager.setCanScheduleExactAlarms(true)

        scheduler.schedule(sampleRequest)

        val shadowAlarmManager = shadowOf(alarmManager)
        assertEquals(1, shadowAlarmManager.scheduledAlarms.size)
        val scheduled = shadowAlarmManager.scheduledAlarms.single()
        assertEquals(AlarmManager.RTC_WAKEUP, scheduled.getType())
        assertEquals(sampleRequest.triggerAtEpochMillis, scheduled.triggerAtMs)
        assertTrue(scheduled.isAllowWhileIdle)
        assertEquals(EXACT_WINDOW_MS, scheduled.windowLengthMs)

        @Suppress("DEPRECATION")
        val intent = shadowOf(scheduled.operation).savedIntent
        assertEquals(ReminderAlarmReceiver.ACTION_REMINDER_TRIGGER, intent.action)
        assertEquals(sampleRequest.reminderId, intent.getLongExtra(ReminderAlarmReceiver.EXTRA_REMINDER_ID, -1L))
        assertEquals(sampleRequest.title, intent.getStringExtra(ReminderAlarmReceiver.EXTRA_TITLE))
        assertEquals(sampleRequest.notes, intent.getStringExtra(ReminderAlarmReceiver.EXTRA_NOTES))
        assertTrue(intent.getBooleanExtra(ReminderAlarmReceiver.EXTRA_IS_YEARLY_BS, false))
        assertEquals(sampleRequest.bsYear, intent.getIntExtra(ReminderAlarmReceiver.EXTRA_BS_YEAR, 0))
        assertEquals(sampleRequest.bsMonth, intent.getIntExtra(ReminderAlarmReceiver.EXTRA_BS_MONTH, 0))
        assertEquals(sampleRequest.bsDay, intent.getIntExtra(ReminderAlarmReceiver.EXTRA_BS_DAY, 0))
    }

    @Test
    fun scheduleFallsBackToInexactAlarmWhenExactPermissionIsDenied() {
        ShadowAlarmManager.setCanScheduleExactAlarms(false)

        scheduler.schedule(sampleRequest)

        val shadowAlarmManager = shadowOf(alarmManager)
        assertEquals(1, shadowAlarmManager.scheduledAlarms.size)
        val scheduled = shadowAlarmManager.scheduledAlarms.single()
        assertEquals(AlarmManager.RTC_WAKEUP, scheduled.getType())
        assertEquals(sampleRequest.triggerAtEpochMillis, scheduled.triggerAtMs)
        assertTrue(scheduled.isAllowWhileIdle)
        assertEquals(INEXACT_WINDOW_MS, scheduled.windowLengthMs)
    }

    @Test
    fun scheduleReplacesExistingAlarmForSameReminderId() {
        ShadowAlarmManager.setCanScheduleExactAlarms(true)
        scheduler.schedule(sampleRequest)

        val updated = sampleRequest.copy(
            title = "Updated title",
            triggerAtEpochMillis = sampleRequest.triggerAtEpochMillis + ONE_HOUR_MILLIS
        )
        scheduler.schedule(updated)

        val shadowAlarmManager = shadowOf(alarmManager)
        assertEquals(1, shadowAlarmManager.scheduledAlarms.size)
        val scheduled = shadowAlarmManager.scheduledAlarms.single()
        assertEquals(updated.triggerAtEpochMillis, scheduled.triggerAtMs)
        @Suppress("DEPRECATION")
        val savedIntent = shadowOf(scheduled.operation).savedIntent
        assertEquals("Updated title", savedIntent.getStringExtra(ReminderAlarmReceiver.EXTRA_TITLE))
    }

    @Test
    fun cancelRemovesScheduledAlarmAndIgnoresUnknownReminderId() {
        ShadowAlarmManager.setCanScheduleExactAlarms(true)
        scheduler.schedule(sampleRequest)
        assertEquals(1, shadowOf(alarmManager).scheduledAlarms.size)

        scheduler.cancel(sampleRequest.reminderId)
        assertTrue(shadowOf(alarmManager).scheduledAlarms.isEmpty())

        scheduler.cancel(reminderId = 999L)
        assertTrue(shadowOf(alarmManager).scheduledAlarms.isEmpty())
    }

    @Test
    fun ensureReminderChannelCreatesConfiguredEnglishChannel() {
        NotificationChannels.ensureReminderChannel(application)

        val notificationManager = application.getSystemService(NotificationManager::class.java)
        val channel = notificationManager.getNotificationChannel(NotificationChannels.REMINDERS_CHANNEL_ID)
        assertNotNull(channel)
        assertEquals(
            application.getString(R.string.notification_channel_reminders_name),
            channel.name.toString()
        )
        assertEquals(
            application.getString(R.string.notification_channel_reminders_description),
            channel.description
        )
        assertEquals(NotificationManager.IMPORTANCE_HIGH, channel.importance)
        assertTrue(channel.shouldVibrate())
        assertTrue(channel.canShowBadge())
    }

    @Test
    @Config(qualifiers = "ne")
    fun ensureReminderChannelUsesNepaliCopyUnderNepaliLocale() {
        NotificationChannels.ensureReminderChannel(application)

        val notificationManager = application.getSystemService(NotificationManager::class.java)
        val channel = notificationManager.getNotificationChannel(NotificationChannels.REMINDERS_CHANNEL_ID)
        assertNotNull(channel)
        assertEquals("सम्झनाहरू", channel.name.toString())
        assertEquals("विक्रम संवत् मिति सम्झना र वार्षिक अवसरहरू", channel.description)
    }

    @Test
    fun reminderAlarmReceiverPostsNotificationAndTriggersYearlyReschedule() {
        shadowOf(application).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
        val dispatchedReasons = mutableListOf<ReminderRescheduleReason>()
        val receiver = ReminderAlarmReceiver { _, reason -> dispatchedReasons += reason }

        receiver.onReceive(application, ReminderAlarmReceiver.createIntent(application, sampleRequest))

        val notificationManager = application.getSystemService(NotificationManager::class.java)
        val posted = shadowOf(notificationManager).getNotification(sampleRequest.reminderId.toInt())
        assertNotNull(posted)
        assertEquals(NotificationChannels.REMINDERS_CHANNEL_ID, posted.channelId)
        assertEquals(sampleRequest.title, posted.extras.getString(Notification.EXTRA_TITLE))
        assertEquals(sampleRequest.notes, posted.extras.getString(Notification.EXTRA_TEXT))
        assertEquals(
            application.getString(R.string.notification_reminder_yearly_tag),
            posted.extras.getString(Notification.EXTRA_SUB_TEXT)
        )
        assertEquals(listOf(ReminderRescheduleReason.YEARLY_REMINDER_FIRED), dispatchedReasons)
    }

    @Test
    fun reminderAlarmReceiverSkipsPostingWhenPermissionDeniedButReschedulesYearly() {
        shadowOf(application).denyPermissions(Manifest.permission.POST_NOTIFICATIONS)
        val dispatchedReasons = mutableListOf<ReminderRescheduleReason>()
        val receiver = ReminderAlarmReceiver { _, reason -> dispatchedReasons += reason }

        receiver.onReceive(application, ReminderAlarmReceiver.createIntent(application, sampleRequest))

        val notificationManager = application.getSystemService(NotificationManager::class.java)
        assertTrue(shadowOf(notificationManager).allNotifications.isEmpty())
        assertEquals(listOf(ReminderRescheduleReason.YEARLY_REMINDER_FIRED), dispatchedReasons)
    }

    @Test
    fun bootRescheduleReceiverDispatchesBootTimeAndTimezoneEventsOnly() {
        val dispatchedReasons = mutableListOf<ReminderRescheduleReason>()
        val receiver = BootRescheduleReceiver { _, reason -> dispatchedReasons += reason }

        receiver.onReceive(application, Intent(Intent.ACTION_BOOT_COMPLETED))
        receiver.onReceive(application, Intent(Intent.ACTION_TIME_CHANGED))
        receiver.onReceive(application, Intent(Intent.ACTION_TIMEZONE_CHANGED))
        receiver.onReceive(application, Intent(Intent.ACTION_BATTERY_LOW))
        receiver.onReceive(application, null)

        assertEquals(
            listOf(
                ReminderRescheduleReason.BOOT_COMPLETED,
                ReminderRescheduleReason.TIME_CHANGED,
                ReminderRescheduleReason.TIMEZONE_CHANGED
            ),
            dispatchedReasons
        )
    }

    private companion object {
        const val EXACT_WINDOW_MS = 0L
        const val INEXACT_WINDOW_MS = -1L
        const val ONE_HOUR_MILLIS = 3_600_000L
    }
}
