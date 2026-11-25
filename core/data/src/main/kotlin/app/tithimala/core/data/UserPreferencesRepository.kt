package app.tithimala.core.data

import app.tithimala.core.datastore.UserPreferencesDataSource
import app.tithimala.core.model.AppLanguage
import app.tithimala.core.model.NumeralScript
import app.tithimala.core.model.ThemeMode
import app.tithimala.core.model.UserPreferences
import app.tithimala.core.model.WeekStartDay
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

/**
 * Coordinates persisted user preferences and side effects such as weekly background sync scheduling.
 */
interface UserPreferencesRepository {
    val userPreferences: Flow<UserPreferences>

    suspend fun setLanguage(language: AppLanguage)
    suspend fun setNumeralScript(script: NumeralScript)
    suspend fun setThemeMode(themeMode: ThemeMode)
    suspend fun setWeekStartDay(weekStartDay: WeekStartDay)
    suspend fun setBackgroundSyncEnabled(enabled: Boolean)
}

internal class DefaultUserPreferencesRepository @Inject constructor(
    private val userPreferencesDataSource: UserPreferencesDataSource,
    private val calendarSyncScheduler: CalendarSyncScheduler
) : UserPreferencesRepository {

    override val userPreferences: Flow<UserPreferences> = userPreferencesDataSource.userPreferences

    override suspend fun setLanguage(language: AppLanguage) {
        userPreferencesDataSource.setLanguage(language)
    }

    override suspend fun setNumeralScript(script: NumeralScript) {
        userPreferencesDataSource.setNumeralScript(script)
    }

    override suspend fun setThemeMode(themeMode: ThemeMode) {
        userPreferencesDataSource.setThemeMode(themeMode)
    }

    override suspend fun setWeekStartDay(weekStartDay: WeekStartDay) {
        userPreferencesDataSource.setWeekStartDay(weekStartDay)
    }

    override suspend fun setBackgroundSyncEnabled(enabled: Boolean) {
        userPreferencesDataSource.setBackgroundSyncEnabled(enabled)
        calendarSyncScheduler.syncWithPreference(enabled)
    }
}
