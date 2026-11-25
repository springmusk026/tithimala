package app.tithimala.core.datastore

import app.tithimala.core.model.AppLanguage
import app.tithimala.core.model.NumeralScript
import app.tithimala.core.model.ThemeMode
import app.tithimala.core.model.UserPreferences
import app.tithimala.core.model.WeekStartDay
import kotlinx.coroutines.flow.Flow

/**
 * Local preference and calendar sync metadata data source backed by Jetpack DataStore.
 */
interface UserPreferencesDataSource {
    val userPreferences: Flow<UserPreferences>
    val syncMetadata: Flow<CalendarSyncMetadata>

    suspend fun setLanguage(language: AppLanguage)
    suspend fun setNumeralScript(script: NumeralScript)
    suspend fun setThemeMode(themeMode: ThemeMode)
    suspend fun setWeekStartDay(weekStartDay: WeekStartDay)
    suspend fun setBackgroundSyncEnabled(enabled: Boolean)
    suspend fun recordSyncResult(datasetVersion: String, syncedAtEpochMillis: Long)
}
