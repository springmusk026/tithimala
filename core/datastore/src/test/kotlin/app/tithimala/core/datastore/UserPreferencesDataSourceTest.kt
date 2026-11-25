package app.tithimala.core.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import app.cash.turbine.test
import app.tithimala.core.model.AppLanguage
import app.tithimala.core.model.NumeralScript
import app.tithimala.core.model.ThemeMode
import app.tithimala.core.model.UserPreferences
import app.tithimala.core.model.WeekStartDay
import java.io.File
import java.io.IOException
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class UserPreferencesDataSourceTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun emitsDefaultsWhenStoreIsEmpty() = runTest {
        val dataSource = createDataSource(backgroundScope, "defaults.preferences_pb")

        assertEquals(UserPreferences(), dataSource.userPreferences.first())
        assertEquals(CalendarSyncMetadata(), dataSource.syncMetadata.first())
    }

    @Test
    fun updatesAndPersistsEveryUserPreference() = runTest {
        val dataSource = createDataSource(backgroundScope, "all_prefs.preferences_pb")

        dataSource.userPreferences.test {
            assertEquals(UserPreferences(), awaitItem())

            dataSource.setLanguage(AppLanguage.NEPALI)
            assertEquals(AppLanguage.NEPALI, awaitItem().language)

            dataSource.setNumeralScript(NumeralScript.LATIN)
            assertEquals(NumeralScript.LATIN, awaitItem().numeralScript)

            dataSource.setThemeMode(ThemeMode.DARK)
            assertEquals(ThemeMode.DARK, awaitItem().themeMode)

            dataSource.setWeekStartDay(WeekStartDay.MONDAY)
            assertEquals(WeekStartDay.MONDAY, awaitItem().weekStartDay)

            dataSource.setBackgroundSyncEnabled(false)
            val finalPrefs = awaitItem()
            assertEquals(
                UserPreferences(
                    language = AppLanguage.NEPALI,
                    numeralScript = NumeralScript.LATIN,
                    themeMode = ThemeMode.DARK,
                    weekStartDay = WeekStartDay.MONDAY,
                    backgroundSyncEnabled = false
                ),
                finalPrefs
            )
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun persistsSyncMetadataWithoutAlteringPreferences() = runTest {
        val dataSource = createDataSource(backgroundScope, "sync_meta.preferences_pb")

        dataSource.syncMetadata.test {
            assertEquals(CalendarSyncMetadata(), awaitItem())

            dataSource.recordSyncResult(
                datasetVersion = "2026.11.01",
                syncedAtEpochMillis = SAMPLE_SYNC_TIMESTAMP
            )
            assertEquals(
                CalendarSyncMetadata(
                    activeDatasetVersion = "2026.11.01",
                    lastSyncEpochMillis = SAMPLE_SYNC_TIMESTAMP
                ),
                awaitItem()
            )
            cancelAndIgnoreRemainingEvents()
        }

        assertEquals(UserPreferences(), dataSource.userPreferences.first())
    }

    @Test
    fun recoversToDefaultsOnIoExceptionAndRethrowsUnexpectedErrors() = runTest {
        val ioFailingStore = FailingReadDataStore(IOException("Simulated disk read error"))
        val ioDataSource = DataStoreUserPreferencesDataSource(ioFailingStore)

        assertEquals(UserPreferences(), ioDataSource.userPreferences.first())
        assertEquals(CalendarSyncMetadata(), ioDataSource.syncMetadata.first())

        val unexpectedFailingStore = FailingReadDataStore(IllegalStateException("Unexpected bug"))
        val unexpectedDataSource = DataStoreUserPreferencesDataSource(unexpectedFailingStore)

        assertFailsWith<IllegalStateException> {
            unexpectedDataSource.userPreferences.first()
        }
    }

    @Test
    fun replacesCorruptBackingFileWithDefaultsAndAllowsSubsequentEdits() = runTest {
        val corruptFile = File(tempFolder.root, "corrupt.preferences_pb").apply {
            writeBytes(byteArrayOf(0x00, 0xFF.toByte(), 0x7F, 0x13, 0x37))
        }
        val dataStore = PreferenceDataStoreFactory.create(
            corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() },
            scope = backgroundScope,
            produceFile = { corruptFile }
        )
        val dataSource = DataStoreUserPreferencesDataSource(dataStore)

        assertEquals(UserPreferences(), dataSource.userPreferences.first())

        dataSource.setThemeMode(ThemeMode.LIGHT)
        assertEquals(ThemeMode.LIGHT, dataSource.userPreferences.first().themeMode)
    }

    private fun createDataSource(scope: CoroutineScope, fileName: String): DataStoreUserPreferencesDataSource {
        val file = File(tempFolder.root, fileName)
        val store = PreferenceDataStoreFactory.create(
            corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() },
            scope = scope,
            produceFile = { file }
        )
        return DataStoreUserPreferencesDataSource(store)
    }

    private class FailingReadDataStore(private val failure: Throwable) : DataStore<Preferences> {
        override val data: Flow<Preferences> = flow { throw failure }

        override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences =
            throw UnsupportedOperationException("Not used in read-error test")
    }

    private companion object {
        const val SAMPLE_SYNC_TIMESTAMP = 1_776_133_800_000L
    }
}
