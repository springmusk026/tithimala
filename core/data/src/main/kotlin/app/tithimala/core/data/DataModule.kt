package app.tithimala.core.data

import android.content.Context
import androidx.work.WorkManager
import app.tithimala.core.notifications.ReminderRescheduleCallback
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import java.io.File
import java.time.ZoneId
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

@Module
@InstallIn(SingletonComponent::class)
internal object DataModule {

    @Provides
    @IoDispatcher
    fun provideIoDispatcher(): CoroutineDispatcher = Dispatchers.IO

    @Provides
    @Singleton
    fun provideCalendarUpdateEndpoint(): CalendarUpdateEndpoint =
        CalendarUpdateEndpoint(DataLayerConfig.DEFAULT_CALENDAR_UPDATE_BASE_URL)

    @Provides
    @Singleton
    @CalendarUpdatesDir
    fun provideCalendarUpdatesDir(@ApplicationContext context: Context): File =
        File(context.filesDir, DataLayerConfig.CALENDAR_UPDATES_DIR_NAME)

    @Provides
    @Singleton
    fun provideWorkManager(@ApplicationContext context: Context): WorkManager = WorkManager.getInstance(context)

    @Provides
    @Singleton
    fun provideEpochClock(): EpochClock = EpochClock(System::currentTimeMillis)

    @Provides
    @Singleton
    fun provideZoneIdProvider(): ZoneIdProvider = ZoneIdProvider(ZoneId::systemDefault)
}

@Module
@InstallIn(SingletonComponent::class)
internal abstract class DataBindingsModule {

    @Binds
    @Singleton
    internal abstract fun bindCalendarRepository(impl: DefaultCalendarRepository): CalendarRepository

    @Binds
    @Singleton
    internal abstract fun bindCalendarSyncScheduler(impl: WorkManagerCalendarSyncScheduler): CalendarSyncScheduler

    @Binds
    @Singleton
    internal abstract fun bindUserPreferencesRepository(
        impl: DefaultUserPreferencesRepository
    ): UserPreferencesRepository

    @Binds
    @Singleton
    internal abstract fun bindReminderRepository(impl: DefaultReminderRepository): ReminderRepository

    @Binds
    @IntoSet
    internal abstract fun bindReminderRescheduleCallback(
        impl: DataReminderRescheduleCallback
    ): ReminderRescheduleCallback
}
