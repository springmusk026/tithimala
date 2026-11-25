package app.tithimala.core.data

import app.tithimala.core.calendarengine.BikramSambatEngine
import app.tithimala.core.calendarengine.data.BundledCalendarDataLoader
import app.tithimala.core.calendarengine.data.CalendarDataValidationException
import app.tithimala.core.calendarengine.data.CalendarDataValidator
import app.tithimala.core.calendarengine.data.CalendarManifest
import app.tithimala.core.calendarengine.data.VerifiedCalendarDataset
import app.tithimala.core.datastore.UserPreferencesDataSource
import app.tithimala.core.model.BsDate
import app.tithimala.core.model.BsMonthGrid
import app.tithimala.core.model.DatasetMetadata
import app.tithimala.core.model.PatroDayInfo
import app.tithimala.core.model.WeekStartDay
import java.io.IOException
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.runInterruptible
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

internal class DefaultCalendarRepository @Inject constructor(
    private val userPreferencesDataSource: UserPreferencesDataSource,
    private val endpoint: CalendarUpdateEndpoint,
    private val storage: CalendarDatasetStorage,
    private val fetcher: HttpUrlConnectionCalendarFetcher,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val epochClock: EpochClock
) : CalendarRepository {

    private val refreshMutex = Mutex()
    private val engineState: Lazy<EngineState> = lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        val initialEngine = storage.loadCachedEngineOrNull() ?: BikramSambatEngine.createBundled()
        EngineState(initialEngine)
    }

    override val engine: StateFlow<BikramSambatEngine>
        get() = engineState.value.engine

    override val datasetMetadata: StateFlow<DatasetMetadata>
        get() = engineState.value.datasetMetadata

    override fun todayBs(now: LocalDate): BsDate = engine.value.adToBs(now)

    override fun bsToAd(bsDate: BsDate): LocalDate = engine.value.bsToAd(bsDate)

    override fun adToBs(adDate: LocalDate): BsDate = engine.value.adToBs(adDate)

    override fun buildMonthGrid(year: Int, month: Int, weekStart: WeekStartDay, todayBs: BsDate): BsMonthGrid =
        engine.value.buildMonthGrid(
            year = year,
            month = month,
            todayBs = todayBs,
            weekStart = weekStart
        )

    override fun patroInfo(bsDate: BsDate): PatroDayInfo = engine.value.patroInfo(bsDate)

    override fun noticeText(): String = engine.value.noticeText

    override suspend fun refreshRemoteDatasetNow(): CalendarRefreshResult {
        if (!userPreferencesDataSource.userPreferences.first().backgroundSyncEnabled) {
            return CalendarRefreshResult.SkippedDisabled
        }

        return refreshMutex.withLock {
            if (!userPreferencesDataSource.userPreferences.first().backgroundSyncEnabled) {
                CalendarRefreshResult.SkippedDisabled
            } else {
                executeRefreshLocked()
            }
        }
    }

    private suspend fun executeRefreshLocked(): CalendarRefreshResult = try {
        val activeVersion = engine.value.datasetMetadata.version
        val outcome = runInterruptible(ioDispatcher) {
            fetchAndVerifyRemoteDataset(activeVersion)
        }
        applyRefreshOutcome(outcome)
    } catch (e: CalendarHttpException) {
        CalendarRefreshResult.FallbackToBundled(
            reason = e.message ?: "HTTP ${e.statusCode}",
            isTransientNetworkError = e.isTransient
        )
    } catch (e: CalendarPayloadTooLargeException) {
        CalendarRefreshResult.FallbackToBundled(
            reason = e.message ?: "Remote calendar payload exceeded size limit",
            isTransientNetworkError = false
        )
    } catch (e: CalendarDataValidationException) {
        CalendarRefreshResult.FallbackToBundled(
            reason = e.message ?: "Calendar dataset validation failed",
            isTransientNetworkError = false
        )
    } catch (e: SerializationException) {
        CalendarRefreshResult.FallbackToBundled(
            reason = e.message ?: "Invalid calendar JSON payload",
            isTransientNetworkError = false
        )
    } catch (e: IOException) {
        CalendarRefreshResult.FallbackToBundled(
            reason = e.message ?: "Network or storage I/O error",
            isTransientNetworkError = true
        )
    } catch (e: IllegalArgumentException) {
        CalendarRefreshResult.FallbackToBundled(
            reason = e.message ?: "Invalid calendar update input",
            isTransientNetworkError = false
        )
    }

    private suspend fun applyRefreshOutcome(outcome: RemoteFetchOutcome): CalendarRefreshResult = when (outcome) {
        is RemoteFetchOutcome.UpToDate -> {
            val syncedAt = epochClock.nowMillis()
            userPreferencesDataSource.recordSyncResult(outcome.version, syncedAt)
            engineState.value.updateLastSyncTimestamp(syncedAt)
            CalendarRefreshResult.UpToDate(outcome.version)
        }
        is RemoteFetchOutcome.Updated -> {
            val syncedAt = epochClock.nowMillis()
            runInterruptible(ioDispatcher) {
                storage.commitVerifiedDataset(
                    manifestBytes = outcome.manifestBytes,
                    relativeFiles = outcome.downloadedFiles,
                    syncedAtEpochMillis = syncedAt
                )
            }
            val updatedEngine = BikramSambatEngine(
                dataset = outcome.dataset,
                isRemoteUpdateActive = true,
                lastSyncEpochMillis = syncedAt
            )
            engineState.value.updateEngine(updatedEngine)
            userPreferencesDataSource.recordSyncResult(outcome.dataset.manifest.datasetVersion, syncedAt)
            CalendarRefreshResult.Updated(outcome.dataset.manifest.datasetVersion)
        }
    }

    private fun fetchAndVerifyRemoteDataset(activeVersion: String): RemoteFetchOutcome {
        val manifestBytes = fetcher.fetchBytes(endpoint.baseUrl, BundledCalendarDataLoader.MANIFEST_FILE)
        val manifest = STRICT_JSON.decodeFromString<CalendarManifest>(manifestBytes.decodeToString())
        CalendarDataValidator.validateManifest(manifest)
        CalendarDatasetStorage.validateSafeRelativePath(manifest.calendarFile)
        CalendarDatasetStorage.validateSafeRelativePath(manifest.noticeFile)

        if (CalendarDatasetVersionComparator.compare(manifest.datasetVersion, activeVersion) <= 0) {
            return RemoteFetchOutcome.UpToDate(activeVersion)
        }

        val downloadedFiles = linkedMapOf<String, ByteArray>()
        val verifiedDataset = BundledCalendarDataLoader.loadAndVerify(manifestBytes) { relativePath ->
            if (Thread.currentThread().isInterrupted) {
                throw InterruptedException("Calendar refresh interrupted")
            }
            CalendarDatasetStorage.validateSafeRelativePath(relativePath)
            val bytes = fetcher.fetchBytes(endpoint.baseUrl, relativePath)
            downloadedFiles[relativePath] = bytes
            bytes
        }
        return RemoteFetchOutcome.Updated(
            dataset = verifiedDataset,
            manifestBytes = manifestBytes,
            downloadedFiles = downloadedFiles
        )
    }

    private sealed interface RemoteFetchOutcome {
        data class UpToDate(val version: String) : RemoteFetchOutcome
        data class Updated(
            val dataset: VerifiedCalendarDataset,
            val manifestBytes: ByteArray,
            val downloadedFiles: Map<String, ByteArray>
        ) : RemoteFetchOutcome
    }

    private class EngineState(initialEngine: BikramSambatEngine) {
        private val _engine = MutableStateFlow(initialEngine)
        private val _datasetMetadata = MutableStateFlow(initialEngine.datasetMetadata)

        val engine: StateFlow<BikramSambatEngine> = _engine.asStateFlow()
        val datasetMetadata: StateFlow<DatasetMetadata> = _datasetMetadata.asStateFlow()

        fun updateEngine(updated: BikramSambatEngine) {
            _engine.value = updated
            _datasetMetadata.value = updated.datasetMetadata
        }

        fun updateLastSyncTimestamp(syncedAtEpochMillis: Long) {
            _datasetMetadata.update { current -> current.copy(lastSyncEpochMillis = syncedAtEpochMillis) }
        }
    }

    private companion object {
        val STRICT_JSON = Json {
            ignoreUnknownKeys = false
            isLenient = false
            explicitNulls = false
        }
    }
}
