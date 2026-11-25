package app.tithimala.core.data

import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/**
 * Schedules or cancels the optional weekly static-JSON calendar refresh worker.
 */
interface CalendarSyncScheduler {
    fun syncWithPreference(enabled: Boolean)
}

internal class WorkManagerCalendarSyncScheduler @Inject constructor(private val workManager: WorkManager) :
    CalendarSyncScheduler {

    override fun syncWithPreference(enabled: Boolean) {
        if (!enabled) {
            workManager.cancelUniqueWork(DataLayerConfig.CALENDAR_SYNC_WORK_NAME)
            return
        }

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .setRequiresBatteryNotLow(true)
            .build()

        val request = PeriodicWorkRequestBuilder<CalendarRefreshWorker>(
            repeatInterval = DataLayerConfig.CALENDAR_SYNC_INTERVAL_DAYS,
            repeatIntervalTimeUnit = TimeUnit.DAYS
        )
            .setConstraints(constraints)
            .build()

        workManager.enqueueUniquePeriodicWork(
            DataLayerConfig.CALENDAR_SYNC_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }
}
