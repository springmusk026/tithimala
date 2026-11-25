package app.tithimala.core.data

import app.tithimala.core.calendarengine.data.BundledCalendarDataLoader
import app.tithimala.core.calendarengine.data.CalendarManifest
import app.tithimala.core.calendarengine.data.Sha256Verifier
import app.tithimala.core.datastore.CalendarSyncMetadata
import app.tithimala.core.datastore.UserPreferencesDataSource
import app.tithimala.core.model.AppLanguage
import app.tithimala.core.model.NumeralScript
import app.tithimala.core.model.ThemeMode
import app.tithimala.core.model.UserPreferences
import app.tithimala.core.model.WeekStartDay
import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import java.io.File
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class RemoteCalendarUpdaterTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun validManifestAndFilesUpdatesActiveDatasetAndPersistsAcrossRestart() = runTest {
        val bundledFiles = loadBundledPayloadFiles()
        val updatedManifest = bumpManifestVersion(bundledFiles.getValue(MANIFEST_PATH), UPDATED_VERSION)
        val servedFiles = bundledFiles + (MANIFEST_PATH to updatedManifest)

        withLocalServer(servedFiles) { baseUrl, requestedPaths ->
            val prefs = FakeUserPreferencesDataSource(backgroundSyncEnabled = true)
            val updatesDir = File(tempFolder.root, "calendar_updates")
            val repository = createRepository(prefs, baseUrl, updatesDir, SYNC_TIME_MILLIS)

            assertFalse(repository.datasetMetadata.value.isRemoteUpdateActive)
            assertEquals(BUNDLED_VERSION, repository.datasetMetadata.value.version)

            val result = repository.refreshRemoteDatasetNow()

            assertEquals(CalendarRefreshResult.Updated(UPDATED_VERSION), result)
            assertEquals(UPDATED_VERSION, repository.datasetMetadata.value.version)
            assertTrue(repository.datasetMetadata.value.isRemoteUpdateActive)
            assertEquals(SYNC_TIME_MILLIS, repository.datasetMetadata.value.lastSyncEpochMillis)
            assertEquals(UPDATED_VERSION, repository.engine.value.datasetMetadata.version)
            assertEquals(
                CalendarSyncMetadata(
                    activeDatasetVersion = UPDATED_VERSION,
                    lastSyncEpochMillis = SYNC_TIME_MILLIS
                ),
                prefs.syncMetadata.first()
            )
            assertTrue(File(updatesDir, MANIFEST_PATH).isFile)
            assertTrue(requestedPaths.contains("/$MANIFEST_PATH"))

            // Simulate cold app restart reading the atomically cached dataset from disk.
            val restartedRepository = createRepository(prefs, baseUrl, updatesDir, SYNC_TIME_MILLIS + 1_000L)
            assertEquals(UPDATED_VERSION, restartedRepository.datasetMetadata.value.version)
            assertTrue(restartedRepository.datasetMetadata.value.isRemoteUpdateActive)
        }
    }

    @Test
    fun matchingManifestVersionReturnsUpToDateWithoutDownloadingDataFiles() = runTest {
        val bundledFiles = loadBundledPayloadFiles()

        withLocalServer(bundledFiles) { baseUrl, requestedPaths ->
            val prefs = FakeUserPreferencesDataSource(backgroundSyncEnabled = true)
            val updatesDir = File(tempFolder.root, "calendar_updates_uptodate")
            val repository = createRepository(prefs, baseUrl, updatesDir, SYNC_TIME_MILLIS)

            val result = repository.refreshRemoteDatasetNow()

            assertEquals(CalendarRefreshResult.UpToDate(BUNDLED_VERSION), result)
            assertEquals(listOf("/$MANIFEST_PATH"), requestedPaths)
            assertEquals(SYNC_TIME_MILLIS, repository.datasetMetadata.value.lastSyncEpochMillis)
            assertFalse(repository.datasetMetadata.value.isRemoteUpdateActive)
        }
    }

    @Test
    fun tamperedFileBytesAreRejectedBySha256AndSilentlyKeepBundledData() = runTest {
        val bundledFiles = loadBundledPayloadFiles()
        val updatedManifest = bumpManifestVersion(bundledFiles.getValue(MANIFEST_PATH), UPDATED_VERSION)
        val tamperedPatro = bundledFiles.getValue("patro/2083.json") + byteArrayOf(' '.code.toByte())
        val servedFiles = bundledFiles + mapOf(
            MANIFEST_PATH to updatedManifest,
            "patro/2083.json" to tamperedPatro
        )

        withLocalServer(servedFiles) { baseUrl, _ ->
            val prefs = FakeUserPreferencesDataSource(backgroundSyncEnabled = true)
            val updatesDir = File(tempFolder.root, "calendar_updates_tampered")
            val repository = createRepository(prefs, baseUrl, updatesDir, SYNC_TIME_MILLIS)

            val result = repository.refreshRemoteDatasetNow()

            val fallback = assertIs<CalendarRefreshResult.FallbackToBundled>(result)
            assertFalse(fallback.isTransientNetworkError)
            assertTrue(fallback.reason.contains("SHA-256", ignoreCase = true))
            assertEquals(BUNDLED_VERSION, repository.datasetMetadata.value.version)
            assertFalse(repository.datasetMetadata.value.isRemoteUpdateActive)
            assertFalse(updatesDir.exists())
        }
    }

    @Test
    fun invalidSchemaJsonIsRejectedByValidatorAndSilentlyKeepsBundledData() = runTest {
        val bundledFiles = loadBundledPayloadFiles()
        val badCalendarBytes = bundledFiles.getValue("calendar/bs-calendar.json")
            .decodeToString()
            .replaceFirst("\"schema\": 1", "\"schema\": 99")
            .encodeToByteArray()
        val manifest = JSON.decodeFromString<CalendarManifest>(
            bundledFiles.getValue(MANIFEST_PATH).decodeToString()
        )
        val updatedManifestWithValidSha = manifest.copy(
            datasetVersion = UPDATED_VERSION,
            sha256 = manifest.sha256 + ("calendar/bs-calendar.json" to Sha256Verifier.sha256Hex(badCalendarBytes))
        )
        val servedFiles = bundledFiles + mapOf(
            MANIFEST_PATH to JSON.encodeToString(CalendarManifest.serializer(), updatedManifestWithValidSha)
                .encodeToByteArray(),
            "calendar/bs-calendar.json" to badCalendarBytes
        )

        withLocalServer(servedFiles) { baseUrl, _ ->
            val prefs = FakeUserPreferencesDataSource(backgroundSyncEnabled = true)
            val updatesDir = File(tempFolder.root, "calendar_updates_bad_schema")
            val repository = createRepository(prefs, baseUrl, updatesDir, SYNC_TIME_MILLIS)

            val result = repository.refreshRemoteDatasetNow()

            val fallback = assertIs<CalendarRefreshResult.FallbackToBundled>(result)
            assertFalse(fallback.isTransientNetworkError)
            assertTrue(fallback.reason.contains("schema", ignoreCase = true))
            assertEquals(BUNDLED_VERSION, repository.datasetMetadata.value.version)
            assertFalse(repository.datasetMetadata.value.isRemoteUpdateActive)
            assertFalse(updatesDir.exists())
        }
    }

    @Test
    fun http500AndUnreachableEndpointSilentlyFallBackToBundledData() = runTest {
        withLocalServer(
            files = emptyMap(),
            customHandler = { exchange ->
                exchange.sendResponseHeaders(HTTP_INTERNAL_ERROR, -1L)
                exchange.close()
            }
        ) { baseUrl, _ ->
            val prefs = FakeUserPreferencesDataSource(backgroundSyncEnabled = true)
            val updatesDir = File(tempFolder.root, "calendar_updates_500")
            val repository = createRepository(prefs, baseUrl, updatesDir, SYNC_TIME_MILLIS)

            val result = repository.refreshRemoteDatasetNow()

            val fallback = assertIs<CalendarRefreshResult.FallbackToBundled>(result)
            assertTrue(fallback.isTransientNetworkError)
            assertEquals(BUNDLED_VERSION, repository.datasetMetadata.value.version)
            assertFalse(repository.datasetMetadata.value.isRemoteUpdateActive)
        }

        val unusedPort = ServerSocket(0).use { it.localPort }
        val prefs = FakeUserPreferencesDataSource(backgroundSyncEnabled = true)
        val updatesDir = File(tempFolder.root, "calendar_updates_unreachable")
        val repository = createRepository(prefs, "http://127.0.0.1:$unusedPort", updatesDir, SYNC_TIME_MILLIS)

        val result = repository.refreshRemoteDatasetNow()

        val fallback = assertIs<CalendarRefreshResult.FallbackToBundled>(result)
        assertTrue(fallback.isTransientNetworkError)
        assertEquals(BUNDLED_VERSION, repository.datasetMetadata.value.version)
    }

    @Test
    fun backgroundSyncDisabledReturnsSkippedDisabledWithoutMakingAnyHttpRequest() = runTest {
        val bundledFiles = loadBundledPayloadFiles()

        withLocalServer(bundledFiles) { baseUrl, requestedPaths ->
            val prefs = FakeUserPreferencesDataSource(backgroundSyncEnabled = false)
            val updatesDir = File(tempFolder.root, "calendar_updates_disabled")
            val repository = createRepository(prefs, baseUrl, updatesDir, SYNC_TIME_MILLIS)

            val result = repository.refreshRemoteDatasetNow()

            assertEquals(CalendarRefreshResult.SkippedDisabled, result)
            assertTrue(requestedPaths.isEmpty())
            assertEquals(BUNDLED_VERSION, repository.datasetMetadata.value.version)
        }
    }

    @Test
    fun corruptCachedDirectoryFallsBackSilentlyToBundledEngineOnStartup() {
        val updatesDir = File(tempFolder.root, "calendar_updates_corrupt").apply { mkdirs() }
        File(updatesDir, MANIFEST_PATH).writeText("{not valid json")
        val prefs = FakeUserPreferencesDataSource(backgroundSyncEnabled = true)

        val repository = createRepository(prefs, "http://127.0.0.1:1", updatesDir, SYNC_TIME_MILLIS)

        assertEquals(BUNDLED_VERSION, repository.datasetMetadata.value.version)
        assertFalse(repository.datasetMetadata.value.isRemoteUpdateActive)
    }

    @Test
    fun oversizedRemotePayloadIsRejectedSafelyWithoutUpdatingDataset() = runTest {
        val oversizedPayload = ByteArray(DataLayerConfig.MAX_REMOTE_FILE_BYTES + 1) { 'a'.code.toByte() }
        val servedFiles = mapOf(MANIFEST_PATH to oversizedPayload)

        withLocalServer(servedFiles) { baseUrl, _ ->
            val prefs = FakeUserPreferencesDataSource(backgroundSyncEnabled = true)
            val updatesDir = File(tempFolder.root, "calendar_updates_oversized")
            val repository = createRepository(prefs, baseUrl, updatesDir, SYNC_TIME_MILLIS)

            val result = repository.refreshRemoteDatasetNow()

            val fallback = assertIs<CalendarRefreshResult.FallbackToBundled>(result)
            assertFalse(fallback.isTransientNetworkError)
            assertEquals(BUNDLED_VERSION, repository.datasetMetadata.value.version)
        }
    }

    private fun createRepository(
        prefs: UserPreferencesDataSource,
        baseUrl: String,
        updatesDir: File,
        nowMillis: Long
    ): DefaultCalendarRepository = DefaultCalendarRepository(
        userPreferencesDataSource = prefs,
        endpoint = CalendarUpdateEndpoint(baseUrl),
        storage = CalendarDatasetStorage(updatesDir),
        fetcher = HttpUrlConnectionCalendarFetcher(),
        ioDispatcher = Dispatchers.IO,
        epochClock = EpochClock { nowMillis }
    )

    private suspend fun withLocalServer(
        files: Map<String, ByteArray>,
        customHandler: ((HttpExchange) -> Unit)? = null,
        block: suspend (baseUrl: String, requestedPaths: List<String>) -> Unit
    ) {
        val requestedPaths = CopyOnWriteArrayList<String>()
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/") { exchange ->
            requestedPaths += exchange.requestURI.path
            if (customHandler != null) {
                customHandler(exchange)
                return@createContext
            }
            val key = exchange.requestURI.path.removePrefix("/")
            val payload = files[key]
            if (payload == null) {
                exchange.sendResponseHeaders(HTTP_NOT_FOUND, -1L)
                exchange.close()
            } else {
                exchange.sendResponseHeaders(HTTP_OK, payload.size.toLong())
                exchange.responseBody.use { body -> body.write(payload) }
                exchange.close()
            }
        }
        server.start()
        try {
            val baseUrl = "http://127.0.0.1:${server.address.port}"
            block(baseUrl, requestedPaths)
        } finally {
            server.stop(0)
        }
    }

    private fun loadBundledPayloadFiles(): Map<String, ByteArray> {
        val classLoader = BundledCalendarDataLoader::class.java.classLoader!!
        val manifestBytes = readBundledResource(classLoader, MANIFEST_PATH)
        val manifest = JSON.decodeFromString<CalendarManifest>(manifestBytes.decodeToString())
        return buildMap {
            put(MANIFEST_PATH, manifestBytes)
            put(manifest.calendarFile, readBundledResource(classLoader, manifest.calendarFile))
            put(manifest.noticeFile, readBundledResource(classLoader, manifest.noticeFile))
            for (year in manifest.patroYears) {
                val path = "patro/$year.json"
                put(path, readBundledResource(classLoader, path))
            }
        }
    }

    private fun readBundledResource(classLoader: ClassLoader, relativePath: String): ByteArray =
        classLoader.getResourceAsStream("${BundledCalendarDataLoader.BUNDLED_RESOURCE_ROOT}/$relativePath")!!
            .use { it.readBytes() }

    private fun bumpManifestVersion(manifestBytes: ByteArray, newVersion: String): ByteArray {
        val manifest = JSON.decodeFromString<CalendarManifest>(manifestBytes.decodeToString())
        val updated = manifest.copy(datasetVersion = newVersion)
        return JSON.encodeToString(CalendarManifest.serializer(), updated).encodeToByteArray()
    }

    private class FakeUserPreferencesDataSource(backgroundSyncEnabled: Boolean) : UserPreferencesDataSource {
        private val _userPreferences = MutableStateFlow(
            UserPreferences(backgroundSyncEnabled = backgroundSyncEnabled)
        )
        private val _syncMetadata = MutableStateFlow(CalendarSyncMetadata())

        override val userPreferences: Flow<UserPreferences> = _userPreferences
        override val syncMetadata: Flow<CalendarSyncMetadata> = _syncMetadata

        override suspend fun setLanguage(language: AppLanguage) {
            _userPreferences.value = _userPreferences.value.copy(language = language)
        }

        override suspend fun setNumeralScript(script: NumeralScript) {
            _userPreferences.value = _userPreferences.value.copy(numeralScript = script)
        }

        override suspend fun setThemeMode(themeMode: ThemeMode) {
            _userPreferences.value = _userPreferences.value.copy(themeMode = themeMode)
        }

        override suspend fun setWeekStartDay(weekStartDay: WeekStartDay) {
            _userPreferences.value = _userPreferences.value.copy(weekStartDay = weekStartDay)
        }

        override suspend fun setBackgroundSyncEnabled(enabled: Boolean) {
            _userPreferences.value = _userPreferences.value.copy(backgroundSyncEnabled = enabled)
        }

        override suspend fun recordSyncResult(datasetVersion: String, syncedAtEpochMillis: Long) {
            _syncMetadata.value = CalendarSyncMetadata(
                activeDatasetVersion = datasetVersion,
                lastSyncEpochMillis = syncedAtEpochMillis
            )
        }
    }

    private companion object {
        const val MANIFEST_PATH = BundledCalendarDataLoader.MANIFEST_FILE
        const val BUNDLED_VERSION = "2026.10.30"
        const val UPDATED_VERSION = "2026.11.01"
        const val SYNC_TIME_MILLIS = 1_795_500_000_000L
        const val HTTP_OK = 200
        const val HTTP_NOT_FOUND = 404
        const val HTTP_INTERNAL_ERROR = 500
        val JSON = Json {
            ignoreUnknownKeys = false
            isLenient = false
            explicitNulls = false
        }
    }
}
