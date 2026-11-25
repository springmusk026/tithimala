package app.tithimala.core.notifications

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.Multibinds
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal abstract class NotificationsModule {
    @Binds
    @Singleton
    internal abstract fun bindReminderAlarmScheduler(impl: AndroidReminderAlarmScheduler): ReminderAlarmScheduler

    @Multibinds
    internal abstract fun bindReminderRescheduleCallbacks(): Set<ReminderRescheduleCallback>
}
