package app.tithimala.core.data

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
internal class CalendarRefreshWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val calendarRepository: CalendarRepository
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = when (val result = calendarRepository.refreshRemoteDatasetNow()) {
        is CalendarRefreshResult.Updated,
        is CalendarRefreshResult.UpToDate,
        CalendarRefreshResult.SkippedDisabled -> Result.success()
        is CalendarRefreshResult.FallbackToBundled -> {
            if (result.isTransientNetworkError && runAttemptCount < MAX_RETRY_ATTEMPTS) {
                Result.retry()
            } else {
                Result.success()
            }
        }
    }

    internal companion object {
        const val MAX_RETRY_ATTEMPTS = 3
    }
}
