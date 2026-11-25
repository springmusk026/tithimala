package app.tithimala.core.data

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import app.tithimala.core.notifications.ReminderRescheduleCallback
import app.tithimala.core.notifications.ReminderRescheduleReason
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import javax.inject.Inject

@HiltWorker
internal class RescheduleRemindersWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val reminderRepository: ReminderRepository
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        reminderRepository.rescheduleAllEnabled()
        return Result.success()
    }

    internal companion object {
        const val KEY_REASON = "reschedule_reason"
    }
}

internal class DataReminderRescheduleCallback @Inject constructor() : ReminderRescheduleCallback {
    override fun onRescheduleRequested(context: Context, reason: ReminderRescheduleReason) {
        val request = OneTimeWorkRequestBuilder<RescheduleRemindersWorker>()
            .setInputData(workDataOf(RescheduleRemindersWorker.KEY_REASON to reason.name))
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            DataLayerConfig.REMINDER_RESCHEDULE_WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            request
        )
    }
}
