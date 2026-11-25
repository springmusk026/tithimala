package app.tithimala.core.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import app.tithimala.core.model.AppLanguage
import app.tithimala.core.model.NumeralScript
import app.tithimala.core.model.ThemeMode
import app.tithimala.core.model.UserPreferences
import app.tithimala.core.model.WeekStartDay
import java.io.IOException
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

internal class DataStoreUserPreferencesDataSource @Inject constructor(private val dataStore: DataStore<Preferences>) :
    UserPreferencesDataSource {

    private val safePreferences: Flow<Preferences> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }

    override val userPreferences: Flow<UserPreferences> = safePreferences
        .map { preferences ->
            UserPreferences(
                language = parseEnumOrDefault(preferences[Keys.LANGUAGE], DEFAULTS.language),
                numeralScript = parseEnumOrDefault(preferences[Keys.NUMERAL_SCRIPT], DEFAULTS.numeralScript),
                themeMode = parseEnumOrDefault(preferences[Keys.THEME_MODE], DEFAULTS.themeMode),
                weekStartDay = parseEnumOrDefault(preferences[Keys.WEEK_START_DAY], DEFAULTS.weekStartDay),
                backgroundSyncEnabled = preferences[Keys.BACKGROUND_SYNC_ENABLED] ?: DEFAULTS.backgroundSyncEnabled
            )
        }
        .distinctUntilChanged()

    override val syncMetadata: Flow<CalendarSyncMetadata> = safePreferences
        .map { preferences ->
            CalendarSyncMetadata(
                activeDatasetVersion = preferences[Keys.ACTIVE_DATASET_VERSION],
                lastSyncEpochMillis = preferences[Keys.LAST_SYNC_EPOCH_MILLIS]
            )
        }
        .distinctUntilChanged()

    override suspend fun setLanguage(language: AppLanguage) {
        dataStore.edit { preferences ->
            preferences[Keys.LANGUAGE] = language.name
        }
    }

    override suspend fun setNumeralScript(script: NumeralScript) {
        dataStore.edit { preferences ->
            preferences[Keys.NUMERAL_SCRIPT] = script.name
        }
    }

    override suspend fun setThemeMode(themeMode: ThemeMode) {
        dataStore.edit { preferences ->
            preferences[Keys.THEME_MODE] = themeMode.name
        }
    }

    override suspend fun setWeekStartDay(weekStartDay: WeekStartDay) {
        dataStore.edit { preferences ->
            preferences[Keys.WEEK_START_DAY] = weekStartDay.name
        }
    }

    override suspend fun setBackgroundSyncEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[Keys.BACKGROUND_SYNC_ENABLED] = enabled
        }
    }

    override suspend fun recordSyncResult(datasetVersion: String, syncedAtEpochMillis: Long) {
        dataStore.edit { preferences ->
            preferences[Keys.ACTIVE_DATASET_VERSION] = datasetVersion
            preferences[Keys.LAST_SYNC_EPOCH_MILLIS] = syncedAtEpochMillis
        }
    }

    private inline fun <reified T : Enum<T>> parseEnumOrDefault(raw: String?, default: T): T =
        raw?.let { name -> enumValues<T>().firstOrNull { it.name == name } } ?: default

    private object Keys {
        val LANGUAGE = stringPreferencesKey("language")
        val NUMERAL_SCRIPT = stringPreferencesKey("numeral_script")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val WEEK_START_DAY = stringPreferencesKey("week_start_day")
        val BACKGROUND_SYNC_ENABLED = booleanPreferencesKey("background_sync_enabled")
        val ACTIVE_DATASET_VERSION = stringPreferencesKey("active_dataset_version")
        val LAST_SYNC_EPOCH_MILLIS = longPreferencesKey("last_sync_epoch_millis")
    }

    private companion object {
        val DEFAULTS = UserPreferences()
    }
}
